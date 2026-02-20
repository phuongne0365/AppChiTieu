package com.example.sqlite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUser, edtPass;
    private Button btnLogin, btnGoRegister;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        initViews();
        handleEvents();
    }

    private void initViews() {
        edtUser = findViewById(R.id.edt_login_user);
        edtPass = findViewById(R.id.edt_login_pass);
        btnLogin = findViewById(R.id.btn_login_submit);
        btnGoRegister = findViewById(R.id.btn_go_to_register);
    }

    private void handleEvents() {
        btnLogin.setOnClickListener(v -> {
            String user = edtUser.getText().toString();
            String pass = edtPass.getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullname = dbHelper.checkUser(user, pass);
            if (fullname != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putString("username", fullname);
                editor.apply();

                Toast.makeText(this, "Chào mừng " + fullname, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            }
        });

        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}