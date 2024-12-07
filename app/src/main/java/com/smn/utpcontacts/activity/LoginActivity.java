package com.smn.utpcontacts.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smn.utpcontacts.R;
import com.smn.utpcontacts.database.AppDatabase;
import com.smn.utpcontacts.model.User;
import com.smn.utpcontacts.util.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister;
    private SessionManager sessionManager;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initComponents();
        checkLoggedInState();
        createTestUserIfNeeded();
    }

    private void initComponents() {
        sessionManager = new SessionManager(this);
        db = AppDatabase.getDatabase(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> startRegistration());
    }

    private void checkLoggedInState() {
        if (sessionManager.isLoggedIn()) {
            navigateToContactList();
        }
    }

    private void createTestUserIfNeeded() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (db.userDao().findByEmail("test@test.com") == null) {
                User testUser = new User("test@test.com", "123456", "Usuario Test");
                db.userDao().insert(testUser);
            }
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validateInput(email, password)) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                User user = db.userDao().authenticate(email, password);
                runOnUiThread(() -> handleLoginResult(user));
            });
        }
    }

    private void handleLoginResult(User user) {
        if (user != null) {
            sessionManager.login(user.getId(), user.getEmail());
            navigateToContactList();
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            etPassword.setText("");
        }
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingrese su correo electrónico");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingrese un correo válido");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingrese su contraseña");
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        return true;
    }

    private void navigateToContactList() {
        Intent intent = new Intent(this, ContactListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startRegistration() {
        Toast.makeText(this, "Registro próximamente", Toast.LENGTH_SHORT).show();
    }
}