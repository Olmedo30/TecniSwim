package com.project.tecniswim.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.project.tecniswim.MainActivity;
import com.project.tecniswim.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginFragment extends Fragment {
    private static final int RC_SIGN_IN = 123;

    private EditText etEmail, etPassword;
    private Button btnEmailLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvSwitchToRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

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

        btnEmailLogin.setOnClickListener(v -> attemptEmailAuth());
        btnGoogleLogin.setOnClickListener(v -> startGoogleSignIn());
        tvSwitchToRegister.setOnClickListener(v ->
                ((LoginActivity) requireActivity()).showFragment(new RegisterFragment())
        );

        return view;
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

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // No añadimos a Firestore aquí (solo Google)
                            goToMain();
                        } else {
                            Toast.makeText(getContext(),
                                    "Verifica tu correo antes de iniciar sesión",
                                    Toast.LENGTH_LONG).show();
                            if (user != null) { user.sendEmailVerification(); }
                        }
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
                Collections.singletonList(new AuthUI.IdpConfig.GoogleBuilder().build());

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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN && resultCode == requireActivity().RESULT_OK) {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;

            // Construimos el Map con todos los campos (vacíos los extras)
            Map<String,Object> usuario = new HashMap<>();
            usuario.put("uid",         user.getUid());
            usuario.put("email",       user.getEmail());
            usuario.put("displayName", user.getDisplayName());
            usuario.put("photoUrl",    user.getPhotoUrl() != null
                    ? user.getPhotoUrl().toString()
                    : null);
            // Campos extra a rellenar más adelante
            usuario.put("firstName",  "");
            usuario.put("lastName",   "");
            usuario.put("nickname",   "");
            usuario.put("clase",      "");

            // Guardamos en la colección "Usuarios"
            db.collection("Usuarios")
                    .document(user.getUid())
                    .set(usuario, SetOptions.merge())  // merge para no machacar si ya existe
                    .addOnSuccessListener(aVoid -> goToMain())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Error guardando usuario: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
        }
    }

    private void goToMain() {
        startActivity(new Intent(getActivity(), MainActivity.class));
        requireActivity().finish();
    }
}
