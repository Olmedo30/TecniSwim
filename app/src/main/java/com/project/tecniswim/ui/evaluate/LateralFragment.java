package com.project.tecniswim.ui.evaluate;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.project.tecniswim.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LateralFragment extends Fragment {

    private QuestionsViewModel viewModel;
    private LinearLayout container;   // Dentro del ScrollView en fragment_questions_lateral.xml
    private Button btnContinuar;      // Botón “Continuar”
    private int totalCriterios = 0;   // Número total de criterios en la sección “VISIÓN LATERAL”

    // Video-related fields:
    private VideoView videoView;
    private FrameLayout frameVideoContainer;
    private ActivityResultLauncher<String> pickVideoLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questions_lateral, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoView = view.findViewById(R.id.videoViewLateral);
        frameVideoContainer = view.findViewById(R.id.frameVideoContainer);

        // 1) Let the MediaController overlay the VideoView
        videoView.setZOrderMediaOverlay(true);
        MediaController mediaController = new MediaController(requireContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // 2) Register a launcher to pick “video/*” from gallery
        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        videoView.setVideoURI(uri);
                        mediaController.setAnchorView(videoView);
                        videoView.setMediaController(mediaController);
                        videoView.setOnPreparedListener(mp -> {
                            mp.setLooping(true);
                            videoView.seekTo(100);
                            videoView.start();
                        });
                    }
                }
        );

        // 3) “Seleccionar video” button in the XML
        Button btnPickVideo = view.findViewById(R.id.btnPickVideoLateral);
        btnPickVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);
        container = view.findViewById(R.id.containerLateral);
        btnContinuar = view.findViewById(R.id.btnContinuar);

        btnContinuar.setEnabled(false);

        renderLateralSection();

        btnContinuar.setOnClickListener(v -> {
            String style = viewModel.getSelectedStyle();
            if ("braza".equals(style)) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_lateral_to_intermediate);
            } else {
                Navigation.findNavController(v)
                        .navigate(R.id.action_lateral_to_frontal);
            }
        });
    }

    private void renderLateralSection() {
        try {
            // 1) Leer el estilo actual
            String style = viewModel.getSelectedStyle();
            if (style == null) style = "crol"; // Por defecto, en caso de error
            String assetFileName = "questions_" + style + ".json";

            // 2) Abrir ese JSON desde /assets
            InputStream is = requireContext().getAssets().open(assetFileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);

            // 3) Obtener la sección “VISIÓN LATERAL” dentro del array “sections”
            if (!root.has("sections")) return;
            JSONArray sectionsArr = root.getJSONArray("sections");

            // 4) Contar cuántos criterios hay en “VISIÓN LATERAL”
            totalCriterios = 0;
            for (int s = 0; s < sectionsArr.length(); s++) {
                JSONObject sectObj = sectionsArr.getJSONObject(s);
                String sectionName = sectObj.optString("name", "");
                if ("VISIÓN LATERAL".equalsIgnoreCase(sectionName)) {
                    JSONArray subsectionsArr = sectObj.getJSONArray("subsections");
                    for (int i = 0; i < subsectionsArr.length(); i++) {
                        JSONArray criteriaArr = subsectionsArr.getJSONObject(i).getJSONArray("criteria");
                        totalCriterios += criteriaArr.length();
                    }
                    break;
                }
            }

            // 5) Título principal (“VISIÓN LATERAL”)
            TextView tvSection = new TextView(requireContext());
            tvSection.setText("VISIÓN LATERAL");
            tvSection.setTextSize(22f);
            tvSection.setTextColor(Color.WHITE);
            tvSection.setBackgroundColor(0xFF1976D2);
            tvSection.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            container.addView(tvSection);

            // 6) Iterar para renderizar subsecciones y criterios
            for (int s = 0; s < sectionsArr.length(); s++) {
                JSONObject sectObj = sectionsArr.getJSONObject(s);
                String sectionName = sectObj.optString("name", "");
                if ("VISIÓN LATERAL".equalsIgnoreCase(sectionName)) {
                    JSONArray subsectionsArr = sectObj.getJSONArray("subsections");

                    for (int i = 0; i < subsectionsArr.length(); i++) {
                        JSONObject subObj = subsectionsArr.getJSONObject(i);
                        String subName = subObj.optString("name", "");

                        // 6.a) Subtítulo de la subsección
                        TextView tvSub = new TextView(requireContext());
                        tvSub.setText(subName);
                        tvSub.setTextSize(20f);
                        tvSub.setTextColor(0xFF0D47C4);
                        tvSub.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(4));
                        container.addView(tvSub);

                        // 6.b) Iterar criterios
                        JSONArray criteriaArr = subObj.getJSONArray("criteria");
                        for (int c = 0; c < criteriaArr.length(); c++) {
                            String criterio = criteriaArr.getString(c);
                            String clave = "LATERAL|" + criterio;

                            // Crear CardView por cada criterio
                            CardView card = new CardView(requireContext());
                            card.setCardBackgroundColor(0xFF090909);

                            LinearLayout.LayoutParams cardParams =
                                    new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                    );
                            cardParams.setMargins(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                            card.setLayoutParams(cardParams);
                            card.setRadius(dpToPx(8));
                            card.setCardElevation(dpToPx(4));
                            card.setUseCompatPadding(true);

                            LinearLayout inner = new LinearLayout(requireContext());
                            inner.setOrientation(LinearLayout.VERTICAL);
                            inner.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                            card.addView(inner);

                            TextView tvCrit = new TextView(requireContext());
                            tvCrit.setText(criterio);
                            tvCrit.setTextSize(16f);
                            tvCrit.setTextColor(Color.WHITE);
                            inner.addView(tvCrit);

                            RadioGroup rg = new RadioGroup(requireContext());
                            rg.setOrientation(RadioGroup.HORIZONTAL);
                            rg.setPadding(0, dpToPx(8), 0, dpToPx(8));

                            RadioButton rbSi = new RadioButton(requireContext());
                            rbSi.setText("Sí");
                            rbSi.setTextColor(Color.WHITE);
                            rbSi.setButtonTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF1976D2)
                            );
                            rbSi.setId(View.generateViewId());
                            rg.addView(rbSi);

                            RadioButton rbNo = new RadioButton(requireContext());
                            rbNo.setText("No");
                            rbNo.setTextColor(Color.WHITE);
                            rbNo.setButtonTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFD32F2F)
                            );
                            rbNo.setId(View.generateViewId());
                            rg.addView(rbNo);

                            // Restaurar selección previa (si existe)
                            if (viewModel.tieneRespuesta(clave)) {
                                boolean fueApto = viewModel.getRespuesta(clave);
                                if (fueApto) rbSi.setChecked(true);
                                else rbNo.setChecked(true);
                            }

                            // Listener para guardar la respuesta y verificar si todas contestadas
                            rg.setOnCheckedChangeListener((group, checkedId) -> {
                                boolean esApto = (checkedId == rbSi.getId());
                                viewModel.setRespuesta(clave, esApto);

                                int contestadas = viewModel.getNumContestadosEnSeccion("LATERAL");
                                if (contestadas >= totalCriterios) {
                                    btnContinuar.setEnabled(true);
                                }
                            });

                            inner.addView(rg);
                            container.addView(card);
                        }
                    }
                    break;
                }
            }

            // 7) Si en el ViewModel ya había respuestas previas, habilitar “Continuar” si están todas
            int inicialContestadas = viewModel.getNumContestadosEnSeccion("LATERAL");
            if (inicialContestadas >= totalCriterios) {
                btnContinuar.setEnabled(true);
            }

        } catch (Exception e) {
            // Mostrar mensaje de error en caso de fallo al parsear JSON
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando sección LATERAL:\n" + e.getMessage());
            tvError.setTextColor(0xFFFF0000);
            tvError.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            container.addView(tvError);
        }
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}