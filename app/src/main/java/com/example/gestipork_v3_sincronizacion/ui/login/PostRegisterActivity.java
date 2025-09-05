package com.example.gestipork_v3_sincronizacion.ui.login;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.auth.AuthRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostRegisterActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    private static final String CONFIRM_REDIRECT = "https://TU_USUARIO.github.io/TU_REPO/confirm.html";

    private MaterialToolbar toolbar;
    private TextView tvEmail, tvBackToLogin;
    private MaterialButton btnOpenEmail, btnResend;
    private LinearProgressIndicator progress;

    private String email;
    private ExecutorService ioExec;
    private Handler main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_register);

        ioExec = Executors.newSingleThreadExecutor();
        main = new Handler(Looper.getMainLooper());

        toolbar = findViewById(R.id.toolbar_estandar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("GestiPork");
        }
        toolbar.setNavigationIcon(null);

        tvEmail = findViewById(R.id.tvEmail);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        btnOpenEmail = findViewById(R.id.btnOpenEmail);
        btnResend = findViewById(R.id.btnResend);
        progress = findViewById(R.id.progress);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null) email = "";

        tvEmail.setText(email);

        btnOpenEmail.setOnClickListener(v -> openEmailApp());
        btnResend.setOnClickListener(v -> resendEmail());
        tvBackToLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        btnResend.setEnabled(!loading);
        btnOpenEmail.setEnabled(!loading);
        tvBackToLogin.setEnabled(!loading);
    }

    private void openEmailApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + email)));
            } catch (Exception ignored) {
                Toast.makeText(this, "No encuentro una app de correo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resendEmail() {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        ioExec.execute(() -> {
            AuthRepository.Result res =
                    new AuthRepository(getApplicationContext())
                            .resendConfirmation(email, CONFIRM_REDIRECT);
            main.post(() -> {
                setLoading(false);
                Toast.makeText(this, res.message, Toast.LENGTH_LONG).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExec.shutdownNow();
    }
}
