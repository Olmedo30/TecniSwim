package com.project.tecniswim.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.SetOptions;
import com.project.tecniswim.R;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {
    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button   btnRegister;
    private TextView tvSwitchToLogin;

    private FirebaseAuth      auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Enlazamos solo los campos que quedan: nombre, apellidos, email y contraseña
        etFirstName     = view.findViewById(R.id.etFirstName);
        etLastName      = view.findViewById(R.id.etLastName);
        etEmail         = view.findViewById(R.id.etEmail);
        etPassword      = view.findViewById(R.id.etPassword);
        btnRegister     = view.findViewById(R.id.btnRegister);
        tvSwitchToLogin = view.findViewById(R.id.tvSwitchToLogin);

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvSwitchToLogin.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        return view;
    }

    private void attemptRegister() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String pass      = etPassword.getText().toString().trim();

        // Validaciones básicas
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("Introduce tu nombre");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Introduce tus apellidos");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Introduce tu correo");
            return;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            etPassword.setError("Contraseña de al menos 6 caracteres");
            return;
        }

        // Creamos el usuario con correo/contraseña
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (!task.isSuccessful()) {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser fbUser = auth.getCurrentUser();
                    if (fbUser == null) return;

                    // 1) Enviar correo de verificación
                    fbUser.sendEmailVerification()
                            .addOnSuccessListener(v ->
                                    Toast.makeText(getContext(),
                                            "Revisa tu correo para verificar tu cuenta",
                                            Toast.LENGTH_LONG).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error al enviar verificación: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );

                    // 2) Guardar datos en Firestore (sin navegar aún)
                    Map<String,Object> datos = new HashMap<>();
                    datos.put("uid",          fbUser.getUid());
                    datos.put("email",        email);
                    datos.put("displayName",  "");   // se completará en configuración de perfil
                    datos.put("photoUrl",     null);
                    datos.put("firstName",    firstName);
                    datos.put("lastName",     lastName);

                    db.collection("Usuarios")
                            .document(fbUser.getUid())
                            .set(datos, SetOptions.merge())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error guardando datos: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );

                    // No navegamos a MainActivity: esperamos a que el usuario verifique su correo
                });
    }
}
