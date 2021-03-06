package com.ericmguimaraes.gaso.evaluation.evaluators;

import com.ericmguimaraes.gaso.config.SessionSingleton;
import com.ericmguimaraes.gaso.evaluation.FeatureType;
import com.ericmguimaraes.gaso.evaluation.Milestone;
import com.ericmguimaraes.gaso.evaluation.evaluations.Evaluation;
import com.google.android.gms.cast.framework.Session;

/**
 * Created by ericmguimaraes on 07/05/17.
 */
public class FuelAmountEvaluator extends Evaluator {

    private static float ALLOWANCE = 1f;

    public FuelAmountEvaluator(Milestone milestone) {
        super(milestone);
    }

    @Override
    public Evaluation evaluate() {
        if(milestone.getExpense()==null || !milestone.isHasTankMax())
            return null;
        double diference = milestone.getExpense().getAmount() - ((milestone.getExpense().getAmountPercentageOBDRefil()*milestone.getTankMax())/100);
        Evaluation evaluation = new Evaluation();
        evaluation.setFeatureType(FeatureType.OBD_FUEL_AMOUNT);
        if(diference>0 && diference>ALLOWANCE) {
            evaluation.setRate(-1);
            evaluation.setMessage("A quantidade de combustível detectada pelo OBD foi menor do que o informado pelo usuário. Isso pode significar problema com o leitor do carro ou com a bomba de combustível do posto.");
        } else if (diference<0 && Math.abs(diference)>ALLOWANCE) {
            evaluation.setRate(1);
            evaluation.setMessage("A quantidade de combustível detectada pelo OBD foi maior do que o informado pelo usuário. Isso pode significar problema com o leitor do carro ou com a bomba de combustível do posto.");
        } else {
            evaluation.setRate(0);
            evaluation.setMessage("A quantidade de combustível detectada pelo OBD foi identica ao informado pelo usuário.");
        }
        return evaluation;
    }

}
