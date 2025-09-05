package com.example.gestipork_v3_sincronizacion.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.auth.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout tilEmail, tilPass, tilPass2, tilName;
    private TextInputEditText etEmail, etPass, etPass2, etName;
    private MaterialCheckBox cbTerms;
    private MaterialButton btnRegister;
    private CircularProgressIndicator progress;
    private TextView tvToLogin;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);

        toolbar = findViewById(R.id.toolbar_estandar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle("Registro");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );


        tilEmail = findViewById(R.id.tilEmail);
        tilPass  = findViewById(R.id.tilPassword);
        tilPass2 = findViewById(R.id.tilPassword2);
        etEmail  = findViewById(R.id.txtEmail);
        etPass   = findViewById(R.id.txtPassword);
        etPass2  = findViewById(R.id.txtPassword2);
        cbTerms  = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        progress    = findViewById(R.id.progress);
        tvToLogin   = findViewById(R.id.tvToLogin);
        tilName = findViewById(R.id.tilName);
        etName  = findViewById(R.id.txtName);


        btnRegister.setOnClickListener(v -> attemptRegister());
        tvToLogin.setOnClickListener(v -> finish()); // vuelve al login
    }

    private void attemptRegister() {
        clearErrors();
        String email = text(etEmail);
        String p1 = text(etPass);
        String p2 = text(etPass2);
        String nombre = etName.getText() == null ? "" : etName.getText().toString().trim();


        boolean ok = true;
        if (nombre.isEmpty()) { tilName.setError("El nombre es obligatorio"); ok = false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email no válido"); ok = false;
        }
        if (p1.length() < 6) { tilPass.setError("Mínimo 6 caracteres"); ok = false; }
        if (!p1.equals(p2)) { tilPass2.setError("Las contraseñas no coinciden"); ok = false; }
        if (!cbTerms.isChecked()) { Toast.makeText(this, "Debes aceptar los términos", Toast.LENGTH_SHORT).show(); ok = false; }
        if (!ok) return;

        setLoading(true);
        io.execute(() -> {
            AuthRepository.Result res = new AuthRepository(getApplicationContext()).signUp(email, p1, nombre);
            main.post(() -> {
                setLoading(false);
                if (res.ok) {
                    // → Ir a pantalla informativa post-registro
                    Intent i = new Intent(RegisterActivity.this, PostRegisterActivity.class);
                    i.putExtra(PostRegisterActivity.EXTRA_EMAIL, email);
                    startActivity(i);
                    finish(); // opcional: evita volver a esta pantalla
                } else {
                    Toast.makeText(this, res.message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private String text(TextInputEditText et) {
        return et.getText()==null ? "" : et.getText().toString().trim();
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPass.setError(null);
        tilPass2.setError(null);
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        progress.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
