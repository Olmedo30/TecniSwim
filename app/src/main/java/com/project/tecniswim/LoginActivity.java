package com.project.tecniswim;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import java.util.Collections;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;

    private EditText etEmail, etPassword;
    private Button btnEmailLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvSwitchToRegister;
    private FirebaseAuth auth;
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        // Enlazar vistas
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        btnEmailLogin      = findViewById(R.id.btnEmailLogin);
        btnGoogleLogin     = findViewById(R.id.btnGoogleLogin);
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);

        // Cambiar texto interno del SignInButton
        // (hay un TextView como primer hijo en SignInButton)
        if (btnGoogleLogin.getChildCount() > 0
                && btnGoogleLogin.getChildAt(0) instanceof TextView) {
            ((TextView) btnGoogleLogin.getChildAt(0))
                    .setText("Continuar con Google");
        }

        // Listeners
        btnEmailLogin.setOnClickListener(v -> attemptEmailAuth());
        btnGoogleLogin.setOnClickListener(v -> startGoogleSignIn());
        tvSwitchToRegister.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        btnEmailLogin.setText(isRegisterMode ? "Regístrate" : "Iniciar sesión");
        tvSwitchToRegister.setText(isRegisterMode
                ? "¿Ya tienes cuenta? Inicia sesión"
                : "¿No tienes cuenta? Regístrate");
    }

    private void attemptEmailAuth() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Introduce tu correo");
            return;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            etPassword.setError("Contraseña de al menos 6 caracteres");
            return;
        }

        if (isRegisterMode) {
            // Registro
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, this::onEmailAuthComplete);
        } else {
            // Login
            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, this::onEmailAuthComplete);
        }
    }

    private void onEmailAuthComplete(@NonNull Task<AuthResult> task) {
        if (task.isSuccessful()) {
            goToMain();
        } else {
            String msg = task.getException() != null
                    ? task.getException().getMessage()
                    : "Error desconocido";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void startGoogleSignIn() {
        List<AuthUI.IdpConfig> providers = Collections.singletonList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse resp = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                goToMain();
            } else {
                Toast.makeText(this,
                        resp != null && resp.getError() != null
                                ? resp.getError().getMessage()
                                : "Google sign-in cancelado",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
