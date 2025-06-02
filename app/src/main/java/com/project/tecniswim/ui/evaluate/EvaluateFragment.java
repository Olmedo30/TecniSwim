package com.project.tecniswim.ui.evaluate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.project.tecniswim.R;

public class EvaluateFragment extends Fragment {
    private String selectedStyle = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_evaluate, container, false);

        // Ahora usamos ImageButton para los estilos
        ImageButton btnCrawl        = v.findViewById(R.id.btnCrawl);
        ImageButton btnBackstroke   = v.findViewById(R.id.btnBackstroke);
        ImageButton btnBreaststroke = v.findViewById(R.id.btnBreaststroke);
        ImageButton btnButterfly    = v.findViewById(R.id.btnButterfly);

        // El botón Continuar sigue siendo un Button normal
        Button btnContinue          = v.findViewById(R.id.btnContinue);

        View.OnClickListener styleClick = view -> {
            // Reactivar todos los ImageButton
            btnCrawl.setEnabled(true);
            btnBackstroke.setEnabled(true);
            btnBreaststroke.setEnabled(true);
            btnButterfly.setEnabled(true);

            // Desactivar solo el ImageButton seleccionado
            view.setEnabled(false);

            // Determinar estilo según el ID del ImageButton
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

            // Mostrar el botón Continuar
            btnContinue.setVisibility(View.VISIBLE);
        };

        btnCrawl.setOnClickListener(styleClick);
        btnBackstroke.setOnClickListener(styleClick);
        btnBreaststroke.setOnClickListener(styleClick);
        btnButterfly.setOnClickListener(styleClick);

        btnContinue.setOnClickListener(view -> {
            // Pasar el estilo seleccionado al siguiente destino
            Bundle args = new Bundle();
            args.putString("style", selectedStyle);
            Navigation.findNavController(view)
                    .navigate(R.id.action_home_to_questions, args);
        });

        return v;
    }
}
