package com.project.tecniswim.ui.evaluate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
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

    private LinearLayout container;  // Contenedor principal dentro del ScrollView

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        // Inflamos el XML que contiene el ScrollView y el LinearLayout
        return inflater.inflate(R.layout.fragment_questions, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = view.findViewById(R.id.containerQuestions);

        try {
            // 1) Leemos el JSON de CROL desde /assets/questions_crol.json
            InputStream is = requireContext().getAssets().open("questions_crol.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);

            // 2) Mostramos el título principal ("CROL")
            String estilo = root.optString("style", "CROL");
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(estilo);
            tvTitle.setTextSize(24f);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setPadding(0, 0, 0, dpToPx(16));
            container.addView(tvTitle);

            // 3) Iteramos cada sección y la representamos en un TableLayout
            JSONArray sectionsArray = root.getJSONArray("sections");
            for (int s = 0; s < sectionsArray.length(); s++) {
                JSONObject sectionObj = sectionsArray.getJSONObject(s);
                String sectionName = sectionObj.optString("name", "");

                // --- CREAMOS LA TABLA PARA ESTA SECCIÓN ---
                TableLayout table = new TableLayout(requireContext());
                table.setStretchAllColumns(true);
                table.setShrinkAllColumns(true);
                table.setPadding(0, dpToPx(8), 0, dpToPx(24));

                // 3.a) Mostrar divisores horizontales
                ColorDrawable dividerDrawable = new ColorDrawable(Color.parseColor("#90CAF9"));
                table.setDividerDrawable(dividerDrawable);
                table.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);

                // 3.b) FILA: nombre de la sección (colspan = 2), fondo azul oscuro
                TableRow rowSection = new TableRow(requireContext());
                rowSection.setBackgroundColor(Color.parseColor("#90CAF9")); // azul oscuro
                TextView tvSection = new TextView(requireContext());
                tvSection.setText(sectionName);
                tvSection.setTextSize(20f);
                tvSection.setTextColor(Color.WHITE);
                tvSection.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

                TableRow.LayoutParams paramsSection = new TableRow.LayoutParams();
                paramsSection.span = 2;
                tvSection.setLayoutParams(paramsSection);
                rowSection.addView(tvSection);
                table.addView(rowSection);

                // 3.c) Iteramos cada subsección
                JSONArray subsectionsArr = sectionObj.getJSONArray("subsections");
                for (int ss = 0; ss < subsectionsArr.length(); ss++) {
                    JSONObject subObj = subsectionsArr.getJSONObject(ss);
                    String subName = subObj.optString("name", "");

                    // 3.c.1) FILA: nombre de la subsección (colspan = 2), fondo azul medio
                    TableRow rowSub = new TableRow(requireContext());
                    rowSub.setBackgroundColor(Color.parseColor("#BBDEFB")); // azul medio
                    TextView tvSub = new TextView(requireContext());
                    tvSub.setText(subName);
                    tvSub.setTextSize(18f);
                    tvSub.setTextColor(Color.DKGRAY);
                    tvSub.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(6));

                    TableRow.LayoutParams paramsSub = new TableRow.LayoutParams();
                    paramsSub.span = 2;
                    tvSub.setLayoutParams(paramsSub);
                    rowSub.addView(tvSub);
                    table.addView(rowSub);

                    // 3.c.2) Iteramos los criterios de esta subsección
                    JSONArray criteriaArr = subObj.getJSONArray("criteria");
                    for (int c = 0; c < criteriaArr.length(); c++) {
                        String criterio = criteriaArr.getString(c);

                        // 3.c.2.a) FILA #1: Texto del criterio (colspan = 2)
                        TableRow rowCritText = new TableRow(requireContext());
                        // Alternamos color de fondo cada fila de texto de criterio
                        if (c % 2 == 0) {
                            rowCritText.setBackgroundColor(Color.parseColor("#FFFFFF")); // blanco
                        } else {
                            rowCritText.setBackgroundColor(Color.parseColor("#E3F2FD")); // azul suave
                        }
                        TextView tvCrit = new TextView(requireContext());
                        tvCrit.setText("• " + criterio);
                        tvCrit.setTextSize(16f);
                        tvCrit.setTextColor(Color.BLACK);
                        tvCrit.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

                        TableRow.LayoutParams critParams = new TableRow.LayoutParams();
                        critParams.span = 2;
                        tvCrit.setLayoutParams(critParams);
                        rowCritText.addView(tvCrit);
                        table.addView(rowCritText);

                        // 3.c.2.b) FILA #2: RadioGroup (colspan = 2), fondo idéntico al anterior
                        TableRow rowCritOpts = new TableRow(requireContext());
                        if (c % 2 == 0) {
                            rowCritOpts.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        } else {
                            rowCritOpts.setBackgroundColor(Color.parseColor("#E3F2FD"));
                        }
                        RadioGroup rg = new RadioGroup(requireContext());
                        rg.setOrientation(RadioGroup.HORIZONTAL);
                        rg.setPadding(dpToPx(16), dpToPx(4), dpToPx(8), dpToPx(8));

                        RadioButton rbApto = new RadioButton(requireContext());
                        rbApto.setText("Apto");
                        rbApto.setButtonTintList(
                                ColorStateList.valueOf(Color.parseColor("#1976D2")) // azul oscuro
                        );
                        rbApto.setId(View.generateViewId());
                        rg.addView(rbApto);

                        RadioButton rbNoApto = new RadioButton(requireContext());
                        rbNoApto.setText("No apto");
                        rbNoApto.setButtonTintList(
                                ColorStateList.valueOf(Color.parseColor("#D32F2F")) // se mantiene rojo para contraste
                        );
                        rbNoApto.setId(View.generateViewId());
                        rg.addView(rbNoApto);

                        TableRow.LayoutParams rgParams = new TableRow.LayoutParams();
                        rgParams.span = 2;
                        rg.setLayoutParams(rgParams);
                        rowCritOpts.addView(rg);
                        table.addView(rowCritOpts);
                    }
                }

                // 4) Añadimos la tabla completa al contenedor principal
                container.addView(table);
            }

        } catch (Exception e) {
            // Si hay error al cargar/parsing JSON
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando criterios: " + e.getMessage());
            tvError.setTextSize(16f);
            tvError.setTextColor(Color.RED);
            tvError.setPadding(0, dpToPx(16), 0, 0);
            container.addView(tvError);
        }
    }

    /** Convierte DP a píxeles según densidad de pantalla */
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
