package com.project.tecniswim.ui.evaluate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.project.tecniswim.R;

public class EvaluateFragment extends Fragment {
    private String selectedStyle = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_evaluate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ImageButton btnCrawl        = v.findViewById(R.id.btnCrawl);
        ImageButton btnBackstroke   = v.findViewById(R.id.btnBackstroke);
        ImageButton btnBreaststroke = v.findViewById(R.id.btnBreaststroke);
        ImageButton btnButterfly    = v.findViewById(R.id.btnButterfly);

        QuestionsViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(QuestionsViewModel.class);

        View.OnClickListener styleClick = view -> {
            // 1) Reactivar todos
            btnCrawl.setEnabled(true);
            btnBackstroke.setEnabled(true);
            btnBreaststroke.setEnabled(true);
            btnButterfly.setEnabled(true);

            // 2) Desactivar el pulsado
            view.setEnabled(false);

            // 3) Detectar cuál fue pulsado
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

            // 4) Guardar estilo en el ViewModel
            viewModel.setSelectedStyle(selectedStyle);

            // 5) Navegar siempre a LateralFragment (desde Lateral luego se decide si ir a Intermediate o Frontal)
            Navigation.findNavController(view)
                    .navigate(R.id.action_evaluate_to_lateral);
        };

        btnCrawl.setOnClickListener(styleClick);
        btnBackstroke.setOnClickListener(styleClick);
        btnBreaststroke.setOnClickListener(styleClick);
        btnButterfly.setOnClickListener(styleClick);
    }
}