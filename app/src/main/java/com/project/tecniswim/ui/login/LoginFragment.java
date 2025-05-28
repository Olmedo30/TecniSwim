package com.project.tecniswim.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.project.tecniswim.MainActivity;
import com.project.tecniswim.R;

import java.util.Collections;
import java.util.List;

public class LoginFragment extends Fragment {
    private static final int RC_SIGN_IN = 123;

    private EditText etEmail, etPassword;
    private Button btnEmailLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvSwitchToRegister;
    private FirebaseAuth auth;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        auth = FirebaseAuth.getInstance();

        etEmail            = view.findViewById(R.id.etEmail);
        etPassword         = view.findViewById(R.id.etPassword);
        btnEmailLogin      = view.findViewById(R.id.btnEmailLogin);
        btnGoogleLogin     = view.findViewById(R.id.btnGoogleLogin);
        tvSwitchToRegister = view.findViewById(R.id.tvSwitchToRegister);

        // Cambiar texto del botón de Google
        if (btnGoogleLogin.getChildCount() > 0 &&
                btnGoogleLogin.getChildAt(0) instanceof TextView) {
            ((TextView) btnGoogleLogin.getChildAt(0))
                    .setText("Continuar con Google");
        }

        btnEmailLogin.setOnClickListener(v -> attemptEmailLogin());
        btnGoogleLogin.setOnClickListener(v -> startGoogleSignIn());
        tvSwitchToRegister.setOnClickListener(v ->
                ((LoginActivity) requireActivity())
                        .showFragment(new RegisterFragment())
        );

        return view;
    }

    private void attemptEmailLogin() {
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

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(getActivity(), MainActivity.class));
                        requireActivity().finish();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startGoogleSignIn() {
        List<AuthUI.IdpConfig> providers =
                Collections.singletonList(
                        new AuthUI.IdpConfig.GoogleBuilder().build()
                );

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse resp = IdpResponse.fromResultIntent(data);
            if (resultCode == requireActivity().RESULT_OK) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                requireActivity().finish();
            } else {
                String err = resp != null && resp.getError() != null
                        ? resp.getError().getMessage()
                        : "Google sign-in cancelado";
                Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
            }
        }
    }
}
