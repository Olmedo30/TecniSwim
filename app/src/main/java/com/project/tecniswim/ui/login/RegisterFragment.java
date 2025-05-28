package com.project.tecniswim.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.project.tecniswim.MainActivity;
import com.project.tecniswim.R;

public class RegisterFragment extends Fragment {
    private EditText etEmail, etPassword;
    private Button btnRegister;
    private TextView tvSwitchToLogin;
    private FirebaseAuth auth;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        auth = FirebaseAuth.getInstance();

        etEmail         = view.findViewById(R.id.etEmail);
        etPassword      = view.findViewById(R.id.etPassword);
        btnRegister     = view.findViewById(R.id.btnRegister);
        tvSwitchToLogin = view.findViewById(R.id.tvSwitchToLogin);

        btnRegister.setOnClickListener(v -> attemptRegister());
        // Al pulsar volver, poppea el fragment para usar la animación inversa
        tvSwitchToLogin.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        return view;
    }

    private void attemptRegister() {
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

        auth.createUserWithEmailAndPassword(email, pass)
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
}