package com.project.tecniswim.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.tecniswim.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class QuestionsFragment extends Fragment {
    private String style;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

            Bundle args = getArguments();
        style = (args != null ? args.getString("style") : null);

        LinearLayout container = view.findViewById(R.id.questionsContainer);

        try {
            // Leer JSON desde assets
            InputStream is = requireContext().getAssets().open("questions.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            String json = new String(buf, StandardCharsets.UTF_8);

            JSONObject root = new JSONObject(json);
            if (style != null && root.has(style)) {
                JSONArray arr = root.getJSONArray(style);

                for (int i = 0; i < arr.length(); i++) {
                    final int questionIndex = i;  // captura el índice

                    // Obtener texto de la pregunta
                    String pregunta = root
                            .getJSONArray(style)
                            .getJSONObject(questionIndex)
                            .optString("pregunta");

                    // 1) TextView con la pregunta
                    TextView tv = new TextView(requireContext());
                    tv.setText((questionIndex + 1) + ". " + pregunta);
                    tv.setTextSize(16f);
                    tv.setPadding(0, 16, 0, 8);
                    container.addView(tv);

                    // 2) RadioGroup horizontal
                    RadioGroup rg = new RadioGroup(requireContext());
                    rg.setOrientation(RadioGroup.HORIZONTAL);

                    // Opción A
                    RadioButton rb1 = new RadioButton(requireContext());
                    rb1.setText("Apto");
                    rg.addView(rb1);

                    // Opción B
                    RadioButton rb2 = new RadioButton(requireContext());
                    rb2.setText("No Apto");
                    rg.addView(rb2);
                    rg.setOnCheckedChangeListener((group, checkedId) -> {
                    });

                    container.addView(rg);
                }

            } else {
                TextView err = new TextView(requireContext());
                err.setText("No hay preguntas para: " + style);
                container.addView(err);
            }
        } catch (Exception e) {
            TextView err = new TextView(requireContext());
            err.setText("Error cargando preguntas.");
            container.addView(err);
        }
    }
}
