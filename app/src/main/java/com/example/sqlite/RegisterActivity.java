package com.example.sqlite;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullname, edtUser, edtPass;
    private Button btnRegister, btnBackLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);
        initViews();
        handleEvents();
    }

    private void initViews() {
        edtFullname = findViewById(R.id.edt_reg_fullname);
        edtUser = findViewById(R.id.edt_reg_user);
        edtPass = findViewById(R.id.edt_reg_pass);
        btnRegister = findViewById(R.id.btn_register_submit);
        btnBackLogin = findViewById(R.id.btn_back_to_login);
    }

    private void handleEvents() {
        btnRegister.setOnClickListener(v -> {
            String fullname = edtFullname.getText().toString();
            String user = edtUser.getText().toString();
            String pass = edtPass.getText().toString();

            if (fullname.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.registerUser(user, pass, fullname)) {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                finish(); // Quay lại màn hình Đăng nhập
            } else {
                Toast.makeText(this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show();
            }
        });

        btnBackLogin.setOnClickListener(v -> finish());
    }
}