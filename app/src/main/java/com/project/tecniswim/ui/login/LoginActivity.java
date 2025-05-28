package com.project.tecniswim.ui.login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.project.tecniswim.MainActivity;
import com.project.tecniswim.R;

/**
 * Activity que aloja los fragments de Login y Register.
 * Comprueba si ya existe sesión y, de ser así, salta a MainActivity.
 */
public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1) Comprobar sesión persistente
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 2) Inflar layout y cargar el fragment de login
        setContentView(R.layout.activity_login);
        if (savedInstanceState == null) {
            showFragment(new LoginFragment());
        }
    }

    /**
     * Reemplaza el fragment en el contenedor con animaciones de deslizamiento.
     */
    public void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,  R.anim.slide_out_left,
                        R.anim.slide_in_left,   R.anim.slide_out_right
                )
                .replace(R.id.flContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
