package com.example.sqlite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchDarkMode;
    private static final String DB_NAME = "SmartWallet.db";

    // Launcher để lưu file (Sao lưu)
    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleBackup(result.getData().getData());
                }
            }
    );

    // Launcher để chọn file (Khôi phục)
    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleRestore(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        handleEvents();
    }

    private void initViews() {
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            switchDarkMode.setChecked(true);
        } else {
            switchDarkMode.setChecked(false);
        }
    }

    private void handleEvents() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        findViewById(R.id.btn_profile).setOnClickListener(v -> 
            Toast.makeText(this, "Thông tin cá nhân", Toast.LENGTH_SHORT).show());
            
        // KÍCH HOẠT SAO LƯU
        findViewById(R.id.btn_backup).setOnClickListener(v -> startBackupProcess());

        // KÍCH HOẠT KHÔI PHỤC
        findViewById(R.id.btn_restore).setOnClickListener(v -> startRestoreProcess());
            
        findViewById(R.id.btn_about).setOnClickListener(v -> 
            Toast.makeText(this, "Smart Wallet v1.0 - Đồ án sinh viên", Toast.LENGTH_SHORT).show());
    }

    private void startBackupProcess() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "SmartWallet_Backup_" + timeStamp + ".db";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        backupLauncher.launch(intent);
    }

    private void handleBackup(Uri targetUri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                Toast.makeText(this, "Chưa có dữ liệu để sao lưu", Toast.LENGTH_SHORT).show();
                return;
            }

            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = getContentResolver().openOutputStream(targetUri)) {
                
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Toast.makeText(this, "Sao lưu thành công!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi sao lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startRestoreProcess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Cho phép chọn mọi file, ta sẽ check đuôi sau
        restoreLauncher.launch(intent);
    }

    private void handleRestore(Uri sourceUri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);
            
            // Đảm bảo thư mục databases tồn tại
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(dbFile)) {
                
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                
                Toast.makeText(this, "Khôi phục thành công! Hãy khởi động lại app.", Toast.LENGTH_LONG).show();
                
                // Đóng app để refresh database
                new android.os.Handler().postDelayed(() -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    Runtime.getRuntime().exit(0);
                }, 2000);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khôi phục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}