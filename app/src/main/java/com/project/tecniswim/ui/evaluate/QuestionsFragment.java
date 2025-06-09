package com.project.tecniswim.ui.evaluate;

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

    private LinearLayout container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questions, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = view.findViewById(R.id.containerQuestions);

        String style = "crol";
        Bundle args = getArguments();
        if (args != null && args.containsKey("style")) {
            style = args.getString("style", "crol");
        }

        try {
            InputStream is = requireContext().getAssets().open("questions.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject rootAll = new JSONObject(jsonText);
            JSONObject root = rootAll.getJSONObject(style);
            String estiloJSON = root.optString("style", style.toUpperCase());
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(estiloJSON);
            tvTitle.setTextSize(24f);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setPadding(0, 0, 0, dpToPx(16));
            container.addView(tvTitle);

            JSONArray sectionsArray = root.getJSONArray("sections");
            for (int s = 0; s < sectionsArray.length(); s++) {
                JSONObject sectionObj = sectionsArray.getJSONObject(s);
                String sectionName = sectionObj.optString("name", "");
                TableLayout table = new TableLayout(requireContext());
                table.setStretchAllColumns(true);
                table.setShrinkAllColumns(true);
                table.setPadding(0, dpToPx(8), 0, dpToPx(24));

                ColorDrawable dividerDrawable = new ColorDrawable(Color.parseColor("#90CAF9"));
                table.setDividerDrawable(dividerDrawable);
                table.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);

                TableRow rowSection = new TableRow(requireContext());
                rowSection.setBackgroundColor(Color.parseColor("#90CAF9"));
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

                JSONArray subsectionsArr = sectionObj.getJSONArray("subsections");
                for (int ss = 0; ss < subsectionsArr.length(); ss++) {
                    JSONObject subObj = subsectionsArr.getJSONObject(ss);
                    String subName = subObj.optString("name", "");

                    TableRow rowSub = new TableRow(requireContext());
                    rowSub.setBackgroundColor(Color.parseColor("#BBDEFB"));
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

                    JSONArray criteriaArr = subObj.getJSONArray("criteria");
                    for (int c = 0; c < criteriaArr.length(); c++) {
                        String criterio = criteriaArr.getString(c);

                        TableRow rowCritText = new TableRow(requireContext());
                        if (c % 2 == 0) {
                            rowCritText.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        } else {
                            rowCritText.setBackgroundColor(Color.parseColor("#E3F2FD"));
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
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#1976D2")
                                )
                        );
                        rbApto.setId(View.generateViewId());
                        rg.addView(rbApto);

                        RadioButton rbNoApto = new RadioButton(requireContext());
                        rbNoApto.setText("No apto");
                        rbNoApto.setButtonTintList(
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#D32F2F")
                                )
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

                container.addView(table);
            }

        } catch (Exception e) {
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando criterios: " + e.getMessage());
            tvError.setTextSize(16f);
            tvError.setTextColor(Color.RED);
            tvError.setPadding(0, dpToPx(16), 0, 0);
            container.addView(tvError);
        }
    }

    /** Convierte DP a píxeles según densidad */
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}