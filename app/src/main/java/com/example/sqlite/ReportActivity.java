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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private RecyclerView rvReportMain;
    private Toolbar toolbar;
    private Spinner spinnerFilter;
    private DatabaseHelper dbHelper;
    
    private long startDate = 0, endDate = Long.MAX_VALUE;
    private boolean isExpenseTab = true;
    private ReportAdapter mainAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupToolbar();
        setupSpinner();
        
        mainAdapter = new ReportAdapter();
        rvReportMain.setLayoutManager(new LinearLayoutManager(this));
        rvReportMain.setAdapter(mainAdapter);
        
        refreshData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_report);
        rvReportMain = findViewById(R.id.rv_report_main);
        spinnerFilter = findViewById(R.id.spinner_report_filter);
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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filters);
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

    private void refreshData() {
        double income = dbHelper.getTotalIncome(startDate, endDate);
        double expense = dbHelper.getTotalExpense(startDate, endDate);
        Map<String, Double> stats = isExpenseTab ? dbHelper.getSpendingStats(startDate, endDate) : dbHelper.getIncomeStats(startDate, endDate);
        
        mainAdapter.updateData(income, expense, stats, isExpenseTab);
    }

    // ADAPTER ĐA NĂNG ĐỂ GIẢI QUYẾT LỖI CUỘN
    class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_SUMMARY = 0;
        private static final int TYPE_CHART = 1;
        private static final int TYPE_HEADER = 2;
        private static final int TYPE_ITEM = 3;

        private double totalIncome, totalExpense;
        private Map<String, Double> stats;
        private List<String> categories = new ArrayList<>();
        private List<Double> amounts = new ArrayList<>();
        private boolean isExpense;

        public void updateData(double income, double expense, Map<String, Double> stats, boolean isExpense) {
            this.totalIncome = income;
            this.totalExpense = expense;
            this.stats = stats;
            this.isExpense = isExpense;
            this.categories.clear();
            this.amounts.clear();
            if (stats != null) {
                this.categories.addAll(stats.keySet());
                this.amounts.addAll(stats.values());
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_SUMMARY;
            if (position == 1) return TYPE_CHART;
            if (position == 2) return TYPE_HEADER;
            return TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_SUMMARY) return new SummaryVH(inflater.inflate(R.layout.item_report_summary, parent, false));
            if (viewType == TYPE_CHART) return new ChartVH(inflater.inflate(R.layout.item_report_chart, parent, false));
            if (viewType == TYPE_HEADER) return new HeaderVH(inflater.inflate(R.layout.item_report_section_header, parent, false));
            return new ItemVH(inflater.inflate(R.layout.item_category_stat, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SummaryVH) {
                SummaryVH h = (SummaryVH) holder;
                h.tvInc.setText(String.format(Locale.getDefault(), "%,.0f đ", totalIncome));
                h.tvExp.setText(String.format(Locale.getDefault(), "%,.0f đ", totalExpense));
            } else if (holder instanceof ChartVH) {
                setupChart(((ChartVH) holder));
            } else if (holder instanceof ItemVH) {
                int dataPos = position - 3;
                ItemVH h = (ItemVH) holder;
                String name = categories.get(dataPos);
                double amt = amounts.get(dataPos);
                double total = isExpense ? totalExpense : totalIncome;
                int percent = (total == 0) ? 0 : (int) Math.round((amt / total) * 100);

                h.tvName.setText(name);
                h.tvPercent.setText(percent + "%");
                h.tvAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", amt));
                h.tvAmount.setTextColor(isExpense ? Color.parseColor("#FF5252") : Color.parseColor("#4CAF50"));
                
                if (name.contains("Ăn")) h.ivIcon.setImageResource(android.R.drawable.ic_menu_today);
                else if (name.contains("nhà")) h.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
                else h.ivIcon.setImageResource(android.R.drawable.ic_menu_help);
            }
        }

        private void setupChart(ChartVH holder) {
            holder.tabLayout.removeAllTabs();
            holder.tabLayout.addTab(holder.tabLayout.newTab().setText("CHI TIÊU"), isExpense);
            holder.tabLayout.addTab(holder.tabLayout.newTab().setText("THU NHẬP"), !isExpense);
            
            holder.tabLayout.clearOnTabSelectedListeners();
            holder.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    isExpenseTab = (tab.getPosition() == 0);
                    refreshData();
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });

            if (stats == null || stats.isEmpty()) {
                holder.chart.clear();
                holder.chart.setNoDataText("Chưa có dữ liệu");
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

            holder.chart.setData(new PieData(dataSet));
            holder.chart.getDescription().setEnabled(false);
            holder.chart.getLegend().setEnabled(false);
            holder.chart.setDrawHoleEnabled(true);
            holder.chart.setHoleRadius(75f);
            holder.chart.setCenterText(String.format(Locale.getDefault(), "Tổng %s\n%,.0f đ", isExpense ? "chi" : "thu", isExpense ? totalExpense : totalIncome));
            holder.chart.setCenterTextSize(14f);
            holder.chart.invalidate();
        }

        @Override
        public int getItemCount() {
            return 3 + categories.size();
        }

        class SummaryVH extends RecyclerView.ViewHolder {
            TextView tvInc, tvExp;
            SummaryVH(View v) { super(v); tvInc = v.findViewById(R.id.tv_report_total_income); tvExp = v.findViewById(R.id.tv_report_total_expense); }
        }
        class ChartVH extends RecyclerView.ViewHolder {
            PieChart chart; TabLayout tabLayout;
            ChartVH(View v) { super(v); chart = v.findViewById(R.id.pie_chart_report); tabLayout = v.findViewById(R.id.tab_layout_report); }
        }
        class HeaderVH extends RecyclerView.ViewHolder { HeaderVH(View v) { super(v); } }
        class ItemVH extends RecyclerView.ViewHolder {
            TextView tvName, tvAmount, tvPercent; ImageView ivIcon;
            ItemVH(View v) { super(v); tvName = v.findViewById(R.id.tv_stat_category_name); tvAmount = v.findViewById(R.id.tv_stat_category_amount);
                tvPercent = v.findViewById(R.id.tv_stat_percent); ivIcon = v.findViewById(R.id.iv_stat_icon); }
        }
    }
}