/*
 *     Gaso
 *
 *     Copyright (C) 2016  Eric Guimarães
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ericmguimaraes.gaso.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ericmguimaraes.gaso.R;
import com.ericmguimaraes.gaso.activities.ExpensesListActivity;
import com.ericmguimaraes.gaso.model.MonthSpent;
import com.ericmguimaraes.gaso.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MyMonthlyExpensesRecyclerViewAdapter extends RecyclerView.Adapter<MyMonthlyExpensesRecyclerViewAdapter.ViewHolder> {

    private List<MonthSpent> monthExpensesList;

    Activity activity;
    RecyclerView recyclerView;

    String[] monthNames = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    public MyMonthlyExpensesRecyclerViewAdapter(List<MonthSpent> items, Activity activity, RecyclerView recyclerView) {
        monthExpensesList = items;
        if(items==null)
            monthExpensesList = new ArrayList<>();
        this.activity = activity;
        this.recyclerView = recyclerView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.month_item_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = monthExpensesList.get(position);
        Calendar cal = Calendar.getInstance();
        cal.setTime(monthExpensesList.get(position).getMonth());
        holder.monthText.setText(monthNames[cal.get(Calendar.MONTH)].subSequence(0,3));
        holder.valueText.setText(StringUtils.formatMoney(monthExpensesList.get(position).getValue()));
    }

    @Override
    public int getItemCount() {
        return monthExpensesList.size();
    }

    public void resetList(List<MonthSpent> monthExpensesList) {
        this.monthExpensesList = monthExpensesList;
        if(monthExpensesList==null)
            this.monthExpensesList = new ArrayList<>();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final View view;

        @Bind(R.id.monthText)
        TextView monthText;

        @Bind(R.id.valueText)
        TextView valueText;

        public MonthSpent mItem;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
                int itemPosition = recyclerView.getChildAdapterPosition(v);
                Intent intent = new Intent(activity, ExpensesListActivity.class);
                Calendar car = Calendar.getInstance();
                car.setTime(monthExpensesList.get(itemPosition).getMonth());
                intent.putExtra("month", car.get(Calendar.MONTH));
                intent.putExtra("year", car.get(Calendar.YEAR));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.getApplicationContext().startActivity(intent);
        }
    }
}
