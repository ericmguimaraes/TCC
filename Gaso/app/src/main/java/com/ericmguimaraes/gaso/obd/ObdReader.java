package com.ericmguimaraes.gaso.obd;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.ericmguimaraes.gaso.config.SessionSingleton;
import com.ericmguimaraes.gaso.model.Car;
import com.ericmguimaraes.gaso.model.ObdLog;
import com.ericmguimaraes.gaso.model.ObdLogGroup;
import com.ericmguimaraes.gaso.persistence.ObdLogGroupDAO;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.engine.AbsoluteLoadCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.AirFuelRatioCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.commands.fuel.WidebandAirFuelRatioCommand;
import com.github.pires.obd.commands.pressure.BarometricPressureCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.protocol.CloseCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.ObdWarmstartCommand;
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObdReader {
    private static final String TAG = ObdReader.class.getSimpleName();
    private static final int MAX_COUNT_TO_TRY_AGAIN = 20;
    private final BluetoothSocket mBtSocket;
    private List<ObdCommand> mObdCommands = new ArrayList<>();
    private List<ObdCommand> setupObdCommands = new ArrayList<>();
    private List<ObdCommand> trySomeTimesObdCommands = new ArrayList<>();
    private List<ObdCommand> toDeleteCommands = new ArrayList<>();
    private List<ObdCommand> toSucessfullCommands = new ArrayList<>();
    public boolean hasGotDistanceFuel;
    private boolean isFirstTime = true;
    private int counter = 0;
    private ObdLog fuelLevelLog;
    private ObdLog distanceobdLog;
    private List<ObdCommand> toDeletePermanentlyCommands = new ArrayList<>();

    public ObdReader(BluetoothSocket btSocket) {
        mBtSocket = btSocket;

        hasGotDistanceFuel = false;

        setupObdCommands.add(new EchoOffCommand());
        setupObdCommands.add(new LineFeedOffCommand());
        setupObdCommands.add(new TimeoutCommand(125));
        setupObdCommands.add(new SelectProtocolCommand(ObdProtocols.AUTO));

        // speed
        mObdCommands.add(new SpeedCommand());
        mObdCommands.add(new RPMCommand());
        // engine

        mObdCommands.add(new AbsoluteLoadCommand());
        mObdCommands.add(new LoadCommand());
        mObdCommands.add(new MassAirFlowCommand());
        mObdCommands.add(new OilTempCommand());
        mObdCommands.add(new RuntimeCommand());
        mObdCommands.add(new ThrottlePositionCommand());

        // fuel
        mObdCommands.add(new AirFuelRatioCommand());
        mObdCommands.add(new ConsumptionRateCommand());
        mObdCommands.add(new FindFuelTypeCommand());
        mObdCommands.add(new FuelTrimCommand());
        mObdCommands.add(new WidebandAirFuelRatioCommand());

        // pressure
        mObdCommands.add(new BarometricPressureCommand());
        mObdCommands.add(new FuelPressureCommand());
        mObdCommands.add(new FuelRailPressureCommand());
        mObdCommands.add(new IntakeManifoldPressureCommand());

        // temperature
        mObdCommands.add(new AirIntakeTemperatureCommand());
        mObdCommands.add(new AmbientAirTemperatureCommand());
        mObdCommands.add(new EngineCoolantTemperatureCommand());

    }

    private boolean isLogOkay(ObdLog log){
        return ((log.getData()!=null && !log.getData().contains("NO_DATA") || log.getData()==null));
    }

    public List<ObdLog> readValues() throws BrokenPipeException {
        Log.d(TAG, "Reading values");

        if(isFirstTime)
            try {
                new ObdResetCommand().run(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        //setup
        List<ObdLog> obdValues = new ArrayList<>();
        if(isFirstTime)
            for (ObdCommand command: setupObdCommands) {
                boolean gotSetUpError = false;
                try {
                    command.run(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                    ObdLog obdLog = getLog(command);
                    if(!isLogOkay(obdLog))
                       gotSetUpError = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    gotSetUpError = true;
                }
                if(!gotSetUpError)
                    isFirstTime = false;
            }

        if(!hasGotDistanceFuel) {
                boolean gotError = false;
                try {
                    DistanceSinceCCCommand distancecommand = new DistanceSinceCCCommand();
                    distancecommand.run(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                    distanceobdLog = getLog(distancecommand);
                    FuelLevelCommand fuelLevelCommand = new FuelLevelCommand();
                    fuelLevelCommand.run(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                    fuelLevelLog = getLog(fuelLevelCommand);
                    if (!isLogOkay(distanceobdLog) || !isLogOkay(fuelLevelLog)) {
                        hasGotDistanceFuel = false;
                    }
                    obdValues.add(distanceobdLog);
                    obdValues.add(fuelLevelLog);
                } catch (Exception e) {
                    e.printStackTrace();
                    gotError = true;
                }
                if (!gotError)
                    hasGotDistanceFuel = true;
        }

        //run commands
        boolean gotSucessfullCall = false;
        for (ObdCommand command: mObdCommands) {
            try {
                command.run(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                ObdLog obdLog = getLog(command);
                obdValues.add(obdLog);
                if(!gotSucessfullCall)
                    gotSucessfullCall = isLogOkay(obdLog);
            }  catch (IOException e) {
                e.printStackTrace();
                if(e.getMessage().contains("Broken"))
                    throw new BrokenPipeException(e);
            } catch (Exception e) {
                e.printStackTrace();
                toDeleteCommands.add(command);
            }
        }

        //try commands that failed before
        if(isTimeToTryAgain())
            for (ObdCommand command: trySomeTimesObdCommands) {
                try {
                    command.run(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                    ObdLog obdLog = getLog(command);
                    if(!gotSucessfullCall)
                        gotSucessfullCall = isLogOkay(obdLog);
                    if(isLogOkay(obdLog))
                        toSucessfullCommands.add(command);
                } catch (com.github.pires.obd.exceptions.UnsupportedCommandException e) {
                    //toDeletePermanentlyCommands.add(command);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        if(gotSucessfullCall)
            for (ObdCommand command: toDeleteCommands) {
                mObdCommands.remove(command);
                trySomeTimesObdCommands.add(command);
            }
        for (ObdCommand command: toSucessfullCommands) {
            trySomeTimesObdCommands.remove(command);
            mObdCommands.add(command);
        }

        for (ObdCommand command: toDeletePermanentlyCommands)
            trySomeTimesObdCommands.remove(command);

        toDeletePermanentlyCommands.clear();
        toDeleteCommands.clear();
        toSucessfullCommands.clear();
        saveObdlog(obdValues);
        return obdValues;
    }

    private boolean isTimeToTryAgain() {
        counter++;
        if(counter>MAX_COUNT_TO_TRY_AGAIN){
            counter = 0;
            return true;
        }
        return false;
    }

    private void saveObdlog(List<ObdLog> logs) {
        ObdLogGroup obdLogGroup = new ObdLogGroup();
        obdLogGroup.setLogs(logs);
        ObdLogGroupDAO dao = new ObdLogGroupDAO();
        dao.add(obdLogGroup);
    }

    private ObdLog getLog(ObdCommand command) {
        ObdLog obdLog = new ObdLog();
        final String cmdName = command.getName();
        final String cmdID = LookUpCommand(cmdName);

        Car c = SessionSingleton.getInstance().currentCar;
        if(c!=null)
            obdLog.setCarId(c.getid());
        try {
            obdLog.setPid(command.getCommandPID());
        } catch (IndexOutOfBoundsException e){
            obdLog.setPid(cmdID);
        }
        obdLog.setName(cmdID);
        obdLog.setParsed(true);
        obdLog.setData(command.getCalculatedResult());
        return obdLog;
    }

    public static String LookUpCommand(String txt) {
        for (AvailableCommandNames item : AvailableCommandNames.values()) {
            if (item.getValue().equals(txt)) return item.name();
        }
        return txt;
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting bluetooth");

        try {
            mBtSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class BrokenPipeException extends IOException {

        public BrokenPipeException(IOException e) {
            super(e);
        }

    }

    public boolean isHasGotDistanceFuel() {
        return hasGotDistanceFuel;
    }

    public ObdLog getFuelLevelLog() {
        return fuelLevelLog;
    }

    public ObdLog getDistanceobdLog() {
        return distanceobdLog;
    }
}
