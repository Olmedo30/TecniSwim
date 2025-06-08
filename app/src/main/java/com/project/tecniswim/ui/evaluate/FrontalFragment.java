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
import android.widget.Toast;
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
import java.util.Map;

public class FrontalFragment extends Fragment {

    private QuestionsViewModel viewModel;
    private LinearLayout container;    // Dentro de fragment_questions_frontal.xml
    private Button btnFinalizar;       // Botón “Finalizar y enviar”
    private int totalCriterios = 0;    // Cuenta total de criterios de “VISIÓN FRONTAL”

    // Video-related fields:
    private VideoView videoView;
    private FrameLayout frameVideoContainer;
    private ActivityResultLauncher<String> pickVideoLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questions_frontal, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoView = view.findViewById(R.id.videoViewFrontal);
        frameVideoContainer = view.findViewById(R.id.frameVideoContainerFrontal);

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

        Button btnPickVideo = view.findViewById(R.id.btnPickVideoFrontal);
        btnPickVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);
        container = view.findViewById(R.id.containerFrontal);
        btnFinalizar = view.findViewById(R.id.btnFinalizar);

        // 1) Desactivar “Finalizar” al inicio
        btnFinalizar.setEnabled(false);

        // 2) Mostrar dinámicamente la parte “VISIÓN FRONTAL”
        renderFrontalSection();

        // 3) Al pulsar “Finalizar y enviar” → UsersFragment
        btnFinalizar.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : viewModel.getTodasRespuestas().entrySet()) {
                sb.append(entry.getKey())
                        .append(" → ")
                        .append(entry.getValue() ? "Sí" : "No")
                        .append("\n");
            }

            // Navegar a UsersFragment
            Navigation.findNavController(v)
                    .navigate(R.id.action_frontal_to_users);
        });
    }

    private void renderFrontalSection() {
        try {
            // 1) Leer estilo del ViewModel
            String style = viewModel.getSelectedStyle();
            if (style == null) style = "crol";
            String assetFileName = "questions_" + style + ".json";

            // 2) Abrir JSON
            InputStream is = requireContext().getAssets().open(assetFileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);

            // 3) Buscar la sección “VISIÓN FRONTAL”
            if (!root.has("sections")) return;
            JSONArray sectionsArr = root.getJSONArray("sections");

            // 4) Contar cuántos criterios hay en “VISIÓN FRONTAL”
            totalCriterios = 0;
            for (int s = 0; s < sectionsArr.length(); s++) {
                JSONObject sectObj = sectionsArr.getJSONObject(s);
                String sectionName = sectObj.optString("name", "");
                if ("VISIÓN FRONTAL".equalsIgnoreCase(sectionName)) {
                    JSONArray subsectionsArr = sectObj.getJSONArray("subsections");
                    for (int i = 0; i < subsectionsArr.length(); i++) {
                        JSONArray criteriaArr = subsectionsArr.getJSONObject(i).getJSONArray("criteria");
                        totalCriterios += criteriaArr.length();
                    }
                    break;
                }
            }

            // 5) Título principal “VISIÓN FRONTAL”
            TextView tvSection = new TextView(requireContext());
            tvSection.setText("VISIÓN FRONTAL");
            tvSection.setTextSize(22f);
            tvSection.setTextColor(Color.WHITE);
            tvSection.setBackgroundColor(0xFF1976D2);
            tvSection.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            container.addView(tvSection);

            // 6) Iterar subsecciones y criterios
            for (int s = 0; s < sectionsArr.length(); s++) {
                JSONObject sectObj = sectionsArr.getJSONObject(s);
                String sectionName = sectObj.optString("name", "");
                if ("VISIÓN FRONTAL".equalsIgnoreCase(sectionName)) {
                    JSONArray subsectionsArr = sectObj.getJSONArray("subsections");

                    for (int i = 0; i < subsectionsArr.length(); i++) {
                        JSONObject subObj = subsectionsArr.getJSONObject(i);
                        String subName = subObj.optString("name", "");

                        // 6.a) Subtítulo de subsección
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
                            String clave = "FRONTAL|" + criterio;

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

                            // Restaurar selección previa
                            if (viewModel.tieneRespuesta(clave)) {
                                boolean fueSi = viewModel.getRespuesta(clave);
                                if (fueSi) rbSi.setChecked(true);
                                else rbNo.setChecked(true);
                            }

                            // Listener para guardar respuesta y activar botón
                            rg.setOnCheckedChangeListener((group, checkedId) -> {
                                boolean esSi = (checkedId == rbSi.getId());
                                viewModel.setRespuesta(clave, esSi);

                                int contestadas = viewModel.getNumContestadosEnSeccion("FRONTAL");
                                if (contestadas >= totalCriterios) {
                                    btnFinalizar.setEnabled(true);
                                }
                            });

                            inner.addView(rg);
                            container.addView(card);
                        }
                    }
                    break;
                }
            }

            // 7) Si ya estaban todas contestadas, habilitar “Finalizar” de inmediato
            int inicialContestadas = viewModel.getNumContestadosEnSeccion("FRONTAL");
            if (inicialContestadas >= totalCriterios) {
                btnFinalizar.setEnabled(true);
            }

        } catch (Exception e) {
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando sección FRONTAL:\n" + e.getMessage());
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