package com.example.gestipork_v3_sincronizacion.ui.login;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.auth.AuthRepository;
import com.example.gestipork_v3_sincronizacion.auth.SessionManager;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.sync.workers.SyncWorker; // <-- cambia a .sync.SyncWorker si corresponde
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.material.progressindicator.CircularProgressIndicator;


public class LoginActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView txtRegistro, txtOlvidarContrasena;
    private ImageView iconoCerdo;
    private CircularProgressIndicator progressLogin;


    private ExecutorService ioExecutor;
    private Handler mainHandler;

    private static final String DASHBOARD_CLASS =
            "com.example.gestipork_v3_sincronizacion.ui.dashboard.DashboardActivity";

    private static final String PASSWORD_RESET_REDIRECT = "https://juanhornet.github.io/gestipork-auth-pages/reset.html"; // ej: "https://tu-dominio.com/auth/callback"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ApiClient.setAppContext(getApplicationContext());

        ioExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        bindViews();
        setupToolbar();
        setupClicks();

        if (new SessionManager(this).isLoggedIn()) {
            goToDashboardAndFinish();
        }
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar); // <-- coincide con tu XML
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.editTextUsername);
        etPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegistro = findViewById(R.id.txtRegistro);
        txtOlvidarContrasena = findViewById(R.id.txtOlvidarContrasena);
        iconoCerdo = findViewById(R.id.iconoCerdo);
        progressLogin = findViewById(R.id.progressLogin);

    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("GestiPork");
            // Login es pantalla raíz: sin botón atrás
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationIcon(null);
    }

    private void setupClicks() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        txtOlvidarContrasena.setOnClickListener(v -> showRecoverDialog());
        txtRegistro.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this,
                    com.example.gestipork_v3_sincronizacion.ui.login.RegisterActivity.class);
            startActivity(i);
        });

    }

    private void attemptLogin() {
        clearErrors();
        String email = safeText(etEmail);
        String pass  = safeText(etPassword);

        boolean ok = true;
        if (!isValidEmail(email)) { tilEmail.setError("Introduce un email válido"); ok = false; }
        if (pass.isEmpty())       { tilPassword.setError("La contraseña es obligatoria"); ok = false; }
        if (!ok) return;

        setLoading(true);

        ioExecutor.execute(() -> {
            AuthRepository repo = new AuthRepository(getApplicationContext());
            boolean success = repo.signIn(email, pass);

            if (!success) {
                mainHandler.post(() -> {
                    setLoading(false);
                    tilPassword.setError("Credenciales incorrectas o red no disponible");
                });
                return;
            }

            // Login OK → decidir si necesitamos bootstrap
            com.example.gestipork_v3_sincronizacion.sync.BootstrapSync bs =
                    new com.example.gestipork_v3_sincronizacion.sync.BootstrapSync(getApplicationContext());
            boolean needsBootstrap = !bs.hasLocalData();

            if (needsBootstrap) {
                // Primer login en este dispositivo → seedAll antes de navegar
                boolean seeded = bs.seedAll(); // mantiene el spinner
                // puedes loguear 'seeded' si quieres
            }

            // Ya con datos (o no hacían falta) → terminar en UI
            mainHandler.post(() -> {
                setLoading(false);
                Toast.makeText(this, "Inicio de sesión correcto", Toast.LENGTH_SHORT).show();
                enqueueInitialSync();           // WorkManager para sync normal
                goToDashboardAndFinish();       // ahora sí, cerramos la Activity
            });
        });
    }



    private void showRecoverDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Tu email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Restablecer contraseña")
                .setView(input)
                .setPositiveButton("Enviar", (d, w) -> {
                    String email = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!isValidEmail(email)) {
                        Toast.makeText(this, "Email no válido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    setLoading(true);
                    ioExecutor.execute(() -> {
                        AuthRepository.Result res =
                                new AuthRepository(getApplicationContext()).sendPasswordReset(email, PASSWORD_RESET_REDIRECT);
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(this, res.message, Toast.LENGTH_LONG).show();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Procesando..." : "Iniciar Sesión");
        tilEmail.setEnabled(!loading);
        tilPassword.setEnabled(!loading);
        txtRegistro.setEnabled(!loading);
        txtOlvidarContrasena.setEnabled(!loading);

        if (progressLogin != null) {
            progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }


    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void enqueueInitialSync() {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(req);
    }

    private void goToDashboardAndFinish() {
        try {
            Class<?> dashboard = Class.forName(DASHBOARD_CLASS);
            startActivity(new Intent(this, dashboard));
            finish();
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "Configura DASHBOARD_CLASS para navegar.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
