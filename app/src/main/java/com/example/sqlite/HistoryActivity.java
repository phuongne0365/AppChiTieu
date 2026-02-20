package com.example.sqlite;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private TextView tvFilterDateRange;
    private RecyclerView rvHistoryList;
    private TabLayout tabLayoutFilter;
    private Toolbar toolbar;
    private EditText edtSearch;
    private TransactionAdapter adapter;
    private List<TransactionAdapter.Transaction> allTransactions;
    private DatabaseHelper dbHelper;
    
    // Mặc định là lọc tất cả thời gian
    private long startDate = 0, endDate = Long.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupToolbar();
        setupFilter();
        loadData();
        setupSwipeToDelete();
        setupSearch();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_history);
        tvFilterDateRange = findViewById(R.id.tv_filter_date_range);
        rvHistoryList = findViewById(R.id.rv_history_list);
        tabLayoutFilter = findViewById(R.id.tab_layout_filter);
        edtSearch = findViewById(R.id.edt_search);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvFilterDateRange.setOnClickListener(v -> {
            Intent intent = new Intent(this, DateRangeActivity.class);
            startActivityForResult(intent, 200);
        });
    }

    private void setupFilter() {
        tabLayoutFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilters();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        // Sử dụng hàm getFilteredTransactions đã có sẵn trong DatabaseHelper
        allTransactions = dbHelper.getFilteredTransactions(startDate, endDate);
        applyFilters();
    }

    private void applyFilters() {
        if (allTransactions == null) return;

        String query = edtSearch.getText().toString().toLowerCase().trim();
        int tabPosition = tabLayoutFilter.getSelectedTabPosition();
        
        List<TransactionAdapter.Transaction> filteredList = new ArrayList<>();
        for (TransactionAdapter.Transaction t : allTransactions) {
            // 1. Lọc theo Tab (Thu/Chi)
            boolean matchesTab = (tabPosition == 0) || 
                                 (tabPosition == 1 && !t.isExpense) || 
                                 (tabPosition == 2 && t.isExpense);
            
            // 2. Lọc theo Tìm kiếm
            boolean matchesSearch = t.title.toLowerCase().contains(query);
            
            if (matchesTab && matchesSearch) {
                filteredList.add(t);
            }
        }
        
        adapter = new TransactionAdapter(filteredList);
        adapter.setOnItemLongClickListener((transaction, position) -> {
            if (transaction.isHeader) return;
            String[] options = {"Sửa", "Xóa"};
            new AlertDialog.Builder(this)
                    .setItems(options, (dialog, which) -> {
                        if (which == 1) confirmDelete(transaction.id);
                    }).show();
        });

        rvHistoryList.setLayoutManager(new LinearLayoutManager(this));
        rvHistoryList.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            startDate = data.getLongExtra("startDate", 0);
            endDate = data.getLongExtra("endDate", Long.MAX_VALUE);
            String label = data.getStringExtra("label");
            tvFilterDateRange.setText(label + " ▾");
            loadData();
        }
    }

    private void confirmDelete(int transactionId) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn chắc chắn muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.deleteTransaction(transactionId);
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                TransactionAdapter.Transaction transaction = adapter.getTransactionList().get(position);
                if (transaction != null && !transaction.isHeader) {
                    confirmDelete(transaction.id);
                }
                adapter.notifyItemChanged(position);
            }
        };
        new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(rvHistoryList);
    }
}