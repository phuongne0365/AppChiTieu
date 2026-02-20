package com.example.sqlite;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerFilter;
    private TabLayout tabLayout;
    private PieChart pieChart;
    private RecyclerView rvCategoryStats;
    private TextView tvTotalIncome, tvTotalExpense, tvDailyAverage, tvComparePrevious;
    private DatabaseHelper dbHelper;
    
    private long startDate = 0, endDate = Long.MAX_VALUE;
    private boolean isExpenseTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupToolbar();
        setupSpinner();
        setupTabLayout();
        
        refreshData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_report);
        spinnerFilter = findViewById(R.id.spinner_report_filter);
        tabLayout = findViewById(R.id.tab_layout_report);
        pieChart = findViewById(R.id.pie_chart_report);
        rvCategoryStats = findViewById(R.id.rv_category_stats);
        tvTotalIncome = findViewById(R.id.tv_report_total_income);
        tvTotalExpense = findViewById(R.id.tv_report_total_expense);
        tvDailyAverage = findViewById(R.id.tv_daily_average);
        tvComparePrevious = findViewById(R.id.tv_compare_previous);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        List<String> filters = new ArrayList<>();
        filters.add("Tất cả ▾");
        filters.add("7 ngày qua ▾");
        filters.add("30 ngày qua ▾");
        for (int i = 1; i <= 12; i++) {
            filters.add("Tháng " + i + " ▾");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTimeRange(position);
                refreshData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTimeRange(int position) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);

        if (position == 0) { startDate = 0; endDate = Long.MAX_VALUE; }
        else if (position == 1) { start.add(Calendar.DAY_OF_YEAR, -7); startDate = start.getTimeInMillis(); endDate = end.getTimeInMillis(); }
        else if (position == 2) { start.add(Calendar.DAY_OF_YEAR, -30); startDate = start.getTimeInMillis(); endDate = end.getTimeInMillis(); }
        else { 
            int month = position - 3;
            start.set(Calendar.MONTH, month); start.set(Calendar.DAY_OF_MONTH, 1);
            end.set(Calendar.MONTH, month); end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            startDate = start.getTimeInMillis(); endDate = end.getTimeInMillis();
        }
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isExpenseTab = (tab.getPosition() == 0);
                refreshData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void refreshData() {
        double income = dbHelper.getTotalIncome(startDate, endDate);
        double expense = dbHelper.getTotalExpense(startDate, endDate);
        tvTotalIncome.setText(String.format(Locale.getDefault(), "%,.0f đ", income));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "%,.0f đ", expense));

        // Ý tưởng 1: Chi tiêu trung bình ngày
        long diff = (endDate == Long.MAX_VALUE) ? 30 : Math.max(1, (endDate - startDate) / (1000 * 60 * 60 * 24));
        tvDailyAverage.setText(String.format(Locale.getDefault(), "%,.0f đ/ngày", expense / diff));

        // Ý tưởng 2: So sánh kỳ trước (Giả lập hạn mức 3tr)
        double percent = (expense / 3000000) * 100;
        tvComparePrevious.setText(percent > 100 ? "Vượt mức!" : String.format(Locale.getDefault(), "Dùng %.0f%%", percent));
        tvComparePrevious.setTextColor(percent > 100 ? Color.RED : Color.parseColor("#4CAF50"));

        Map<String, Double> stats = isExpenseTab ? dbHelper.getSpendingStats(startDate, endDate) : dbHelper.getIncomeStats(startDate, endDate);
        updateChart(stats, isExpenseTab ? expense : income);
        updateList(stats, isExpenseTab ? expense : income);
    }

    private void updateChart(Map<String, Double> stats, double total) {
        if (stats == null || stats.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("Chưa có dữ liệu");
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {Color.parseColor("#FF7043"), Color.parseColor("#42A5F5"), Color.parseColor("#AB47BC"), 
                        Color.parseColor("#26A69A"), Color.parseColor("#FFCA28"), Color.parseColor("#EC407A")};
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setDrawValues(false);

        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(75f);
        pieChart.setCenterText(String.format(Locale.getDefault(), "Tổng %s\n%,.0f đ", isExpenseTab ? "chi" : "thu", total));
        pieChart.setCenterTextSize(14f);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void updateList(Map<String, Double> stats, double total) {
        StatsAdapter adapter = new StatsAdapter(stats, total, isExpenseTab);
        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryStats.setAdapter(adapter);
    }

    private static class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {
        private final List<String> categories;
        private final List<Double> amounts;
        private final double total;
        private final boolean isExpense;

        public StatsAdapter(Map<String, Double> stats, double total, boolean isExpense) {
            this.categories = stats != null ? new ArrayList<>(stats.keySet()) : new ArrayList<>();
            this.amounts = stats != null ? new ArrayList<>(stats.values()) : new ArrayList<>();
            this.total = total;
            this.isExpense = isExpense;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_stat, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = categories.get(position);
            double amt = amounts.get(position);
            int percent = (total == 0) ? 0 : (int) Math.round((amt / total) * 100);
            holder.tvName.setText(name);
            holder.tvPercent.setText(percent + "%");
            holder.tvAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", amt));
            holder.tvAmount.setTextColor(isExpense ? Color.parseColor("#FF5252") : Color.parseColor("#4CAF50"));
        }

        @Override public int getItemCount() { return categories.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAmount, tvPercent; ImageView ivIcon;
            public ViewHolder(View v) { super(v);
                tvName = v.findViewById(R.id.tv_stat_category_name); tvAmount = v.findViewById(R.id.tv_stat_category_amount);
                tvPercent = v.findViewById(R.id.tv_stat_percent); ivIcon = v.findViewById(R.id.iv_stat_icon);
            }
        }
    }
}