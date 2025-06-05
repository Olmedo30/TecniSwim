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

public class IntermediateFragment extends Fragment {

    private QuestionsViewModel viewModel;
    private LinearLayout container;    // LinearLayout dentro de fragment_questions_lateral.xml
    private Button btnContinuar;       // Botón “Continuar”
    private int totalCriterios = 0;    // Cuenta total de criterios de “VISIÓN POSTERIOR”

    // Video-related fields:
    private VideoView videoView;
    private FrameLayout frameVideoContainer;
    private ActivityResultLauncher<String> pickVideoLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        // Reutilizamos el mismo layout que LateralFragment (fragment_questions_lateral.xml)
        return inflater.inflate(R.layout.fragment_questions_lateral, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ─── VIDEO SETUP ────────────────────────────────────────────────────────────────
        videoView = view.findViewById(R.id.videoViewLateral);
        frameVideoContainer = view.findViewById(R.id.frameVideoContainer);

        videoView.setZOrderMediaOverlay(true);
        MediaController mediaController = new MediaController(requireContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

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

        Button btnPickVideo = view.findViewById(R.id.btnPickVideoLateral);
        btnPickVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        // ────────────────────────────────────────────────────────────────────────────────

        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);
        container = view.findViewById(R.id.containerLateral);
        btnContinuar = view.findViewById(R.id.btnContinuar);
        btnContinuar.setEnabled(false);

        renderPosteriorSection();

        // Al pulsar “Continuar” → FrontalFragment
        btnContinuar.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_intermediate_to_frontal)
        );
    }

    private void renderPosteriorSection() {
        try {
            // 1) Leer el estilo (debe ser “braza” si llegamos aquí)
            String style = viewModel.getSelectedStyle();
            if (style == null) style = "braza";
            String assetFileName = "questions_" + style + ".json";

            // 2) Abrir JSON
            InputStream is = requireContext().getAssets().open(assetFileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);

            // 3) Buscar la sección “VISIÓN POSTERIOR”
            if (!root.has("sections")) return;
            JSONArray sectionsArr = root.getJSONArray("sections");

            // 4) Contar cuántos criterios hay en “VISIÓN POSTERIOR”
            totalCriterios = 0;
            for (int s = 0; s < sectionsArr.length(); s++) {
                JSONObject sectObj = sectionsArr.getJSONObject(s);
                String sectionName = sectObj.optString("name", "");
                if ("VISIÓN POSTERIOR".equalsIgnoreCase(sectionName)) {
                    JSONArray subsectionsArr = sectObj.getJSONArray("subsections");
                    for (int i = 0; i < subsectionsArr.length(); i++) {
                        JSONArray criteriaArr = subsectionsArr.getJSONObject(i).getJSONArray("criteria");
                        totalCriterios += criteriaArr.length();
                    }
                    break;
                }
            }

            // 5) Título principal “VISIÓN POSTERIOR”
            TextView tvSection = new TextView(requireContext());
            tvSection.setText("VISIÓN POSTERIOR");
            tvSection.setTextSize(22f);
            tvSection.setTextColor(Color.WHITE);
            tvSection.setBackgroundColor(0xFF1976D2);
            tvSection.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            container.addView(tvSection);

            // 6) Iterar subsecciones y criterios
            for (int s = 0; s < sectionsArr.length(); s++) {
                JSONObject sectObj = sectionsArr.getJSONObject(s);
                String sectionName = sectObj.optString("name", "");
                if ("VISIÓN POSTERIOR".equalsIgnoreCase(sectionName)) {
                    JSONArray subsectionsArr = sectObj.getJSONArray("subsections");

                    for (int i = 0; i < subsectionsArr.length(); i++) {
                        JSONObject subObj = subsectionsArr.getJSONObject(i);
                        String subName = subObj.optString("name", "");

                        // 6.a) Subtítulo de subsección
                        TextView tvSub = new TextView(requireContext());
                        tvSub.setText(subName);
                        tvSub.setTextSize(18f);
                        tvSub.setTextColor(0xFF0D47A1);
                        tvSub.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(4));
                        container.addView(tvSub);

                        // 6.b) Iterar criterios
                        JSONArray criteriaArr = subObj.getJSONArray("criteria");
                        for (int c = 0; c < criteriaArr.length(); c++) {
                            String criterio = criteriaArr.getString(c);
                            String clave = "POSTERIOR|" + criterio;

                            CardView card = new CardView(requireContext());
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
                            tvCrit.setTextColor(Color.BLACK);
                            inner.addView(tvCrit);

                            RadioGroup rg = new RadioGroup(requireContext());
                            rg.setOrientation(RadioGroup.HORIZONTAL);
                            rg.setPadding(0, dpToPx(8), 0, dpToPx(8));

                            RadioButton rbApto = new RadioButton(requireContext());
                            rbApto.setText("Apto");
                            rbApto.setButtonTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF1976D2)
                            );
                            rbApto.setId(View.generateViewId());
                            rg.addView(rbApto);

                            RadioButton rbNoApto = new RadioButton(requireContext());
                            rbNoApto.setText("No apto");
                            rbNoApto.setButtonTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFD32F2F)
                            );
                            rbNoApto.setId(View.generateViewId());
                            rg.addView(rbNoApto);

                            // Restaurar selección previa
                            if (viewModel.tieneRespuesta(clave)) {
                                boolean fueApto = viewModel.getRespuesta(clave);
                                if (fueApto) rbApto.setChecked(true);
                                else rbNoApto.setChecked(true);
                            }

                            // Listener
                            rg.setOnCheckedChangeListener((group, checkedId) -> {
                                boolean esApto = (checkedId == rbApto.getId());
                                viewModel.setRespuesta(clave, esApto);

                                int contestadas = viewModel.getNumContestadosEnSeccion("POSTERIOR");
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

            // 7) Habilitar “Continuar” si ya estaban todas contestadas
            int inicialContestadas = viewModel.getNumContestadosEnSeccion("POSTERIOR");
            if (inicialContestadas >= totalCriterios) {
                btnContinuar.setEnabled(true);
            }

        } catch (Exception e) {
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando sección POSTERIOR:\n" + e.getMessage());
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