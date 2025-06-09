package com.project.tecniswim.ui.login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.project.tecniswim.MainActivity;
import com.project.tecniswim.R;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        if (savedInstanceState == null) {
            showFragment(new LoginFragment());
        }
    }

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