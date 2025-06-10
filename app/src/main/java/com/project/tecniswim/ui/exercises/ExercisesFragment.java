package com.project.tecniswim.ui.exercises;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.project.tecniswim.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExercisesFragment extends Fragment {

    private Spinner spinnerStyles;
    private LinearLayout containerExercises;
    private JSONArray stylesArray;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup parent, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_exercises, parent, false);

        spinnerStyles      = root.findViewById(R.id.spinnerStyles);
        containerExercises = root.findViewById(R.id.containerExercises);

        try {
            InputStream is = requireContext().getAssets().open("ejercicios.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            JSONObject rootObj = new JSONObject(new String(buf, StandardCharsets.UTF_8));
            stylesArray = rootObj.optJSONArray("styles");
        } catch (Exception e) {
            stylesArray = new JSONArray();
        }

        List<String> estilos = new ArrayList<>();
        estilos.add("TODOS");
        for (int i = 0; i < stylesArray.length(); i++) {
            estilos.add(stylesArray.optJSONObject(i).optString("style", "ESTILO_" + i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                estilos
        ) {
            @NonNull @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(18f);
                tv.setBackgroundColor(Color.TRANSPARENT);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(0xFF0D47A1);
                tv.setTextSize(16f);
                tv.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                tv.setBackgroundColor(0xFFBBDEFB);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStyles.setAdapter(adapter);
        spinnerStyles.setBackgroundResource(R.drawable.spinner_bg);
        spinnerStyles.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

        MarginLayoutParams mlp = (MarginLayoutParams) spinnerStyles.getLayoutParams();
        mlp.bottomMargin = dpToPx(4);
        spinnerStyles.setLayoutParams(mlp);

        spinnerStyles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String elegido = estilos.get(position);
                loadExercisesForStyle(elegido);
            }
        });

        return root;
    }

    private void loadExercisesForStyle(String styleKey) {
        containerExercises.removeAllViews();

        if ("TODOS".equalsIgnoreCase(styleKey)) {
            for (int i = 0; i < stylesArray.length(); i++) {
                JSONObject styleObj = stylesArray.optJSONObject(i);
                String sectStyle = styleObj.optString("style", "");
                JSONArray sections = styleObj.optJSONArray("sections");
                if (sections != null) {
                    for (int j = 0; j < sections.length(); j++) {
                        JSONObject sect = sections.optJSONObject(j);
                        String sectName = sect.optString("name", sectStyle + " Sección " + (j+1));
                        TextView tvSect = new TextView(requireContext());
                        tvSect.setText(sectStyle + " - " + sectName);
                        tvSect.setTextSize(22f);
                        tvSect.setTextColor(Color.WHITE);
                        tvSect.setBackgroundColor(0xFF1976D2);
                        tvSect.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                        containerExercises.addView(tvSect);
                        JSONArray links = sect.optJSONArray("exercise_links");
                        if (links != null) {
                            for (int k = 0; k < links.length(); k++) {
                                addVideoCard(links.optString(k));
                            }
                        }
                    }
                }
            }
            return;
        }

        JSONObject styleObj = null;
        for (int i = 0; i < stylesArray.length(); i++) {
            JSONObject obj = stylesArray.optJSONObject(i);
            if (styleKey.equalsIgnoreCase(obj.optString("style", ""))) {
                styleObj = obj;
                break;
            }
        }
        if (styleObj == null) {
            TextView tvNone = new TextView(requireContext());
            tvNone.setText("No hay ejercicios para " + styleKey);
            tvNone.setTextSize(18f);
            tvNone.setTextColor(Color.BLACK);
            containerExercises.addView(tvNone);
            return;
        }

        JSONArray sections = styleObj.optJSONArray("sections");
        if (sections != null) {
            for (int i = 0; i < sections.length(); i++) {
                JSONObject sect = sections.optJSONObject(i);
                String sectName = sect.optString("name", "Sección " + (i+1));

                TextView tvSect = new TextView(requireContext());
                tvSect.setText(sectName);
                tvSect.setTextSize(22f);
                tvSect.setTextColor(Color.WHITE);
                tvSect.setBackgroundColor(0xFF1976D2);
                tvSect.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                containerExercises.addView(tvSect);

                JSONArray links = sect.optJSONArray("exercise_links");
                if (links != null) {
                    for (int j = 0; j < links.length(); j++) {
                        addVideoCard(links.optString(j));
                    }
                }
            }
        }
    }

    private void addVideoCard(String url) {
        String videoId = extractYouTubeId(url);
        if (videoId.isEmpty()) return;

        String thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(200)
        );
        lp.setMargins(0, dpToPx(8), 0, dpToPx(8));
        card.setLayoutParams(lp);
        card.setRadius(dpToPx(8));
        card.setCardElevation(dpToPx(4));
        card.setUseCompatPadding(true);

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_video_card, card, false);
        ImageView iv = content.findViewById(R.id.ivThumbnail);

        Glide.with(this)
                .load(thumbUrl)
                .into(iv);

        card.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        card.addView(content);
        containerExercises.addView(card);
    }

    private String extractYouTubeId(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getHost().contains("youtu.be")) {
            return uri.getLastPathSegment();
        } else {
            return uri.getQueryParameter("v");
        }
    }

    private int dpToPx(int dp) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(d * dp);
    }
}