package com.project.tecniswim.ui.home;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.project.tecniswim.R;

public class HomeFragment extends Fragment {
    private String selectedStyle = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        Button btnCrawl        = v.findViewById(R.id.btnCrawl);
        Button btnBackstroke   = v.findViewById(R.id.btnBackstroke);
        Button btnBreaststroke = v.findViewById(R.id.btnBreaststroke);
        Button btnButterfly    = v.findViewById(R.id.btnButterfly);
        Button btnContinue     = v.findViewById(R.id.btnContinue);

        View.OnClickListener styleClick = view -> {
            // Reactivar todos
            btnCrawl.setEnabled(true);
            btnBackstroke.setEnabled(true);
            btnBreaststroke.setEnabled(true);
            btnButterfly.setEnabled(true);

            // Desactivar el seleccionado
            view.setEnabled(false);

            // Determinar estilo con if/else
            int id = view.getId();
            if (id == R.id.btnCrawl) {
                selectedStyle = "crol";
            } else if (id == R.id.btnBackstroke) {
                selectedStyle = "espalda";
            } else if (id == R.id.btnBreaststroke) {
                selectedStyle = "braza";
            } else if (id == R.id.btnButterfly) {
                selectedStyle = "mariposa";
            }

            // Mostrar botón continuar
            btnContinue.setVisibility(View.VISIBLE);
        };

        btnCrawl.setOnClickListener(styleClick);
        btnBackstroke.setOnClickListener(styleClick);
        btnBreaststroke.setOnClickListener(styleClick);
        btnButterfly.setOnClickListener(styleClick);

        btnContinue.setOnClickListener(view -> {
            // Construir manualmente el bundle
            Bundle args = new Bundle();
            args.putString("style", selectedStyle);
            Navigation.findNavController(view).navigate(R.id.action_home_to_questions, args);
        });

        return v;
    }
}
