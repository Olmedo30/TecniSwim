package com.project.tecniswim.ui.evaluate;

import android.net.Uri;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.project.tecniswim.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Muestra la sección “VISIÓN FRONTAL” de la evaluación.
 * Contiene:
 *  - Un VideoView (con MediaController anclado sobre el propio VideoView).
 *  - ScrollView con Cards para cada criterio (Sí/No).
 *  - Botón “Finalizar y enviar” que se habilita al completar todos los criterios.
 */
public class FrontalFragment extends Fragment {

    private QuestionsViewModel viewModel;
    private LinearLayout container;    // LinearLayout dentro del ScrollView
    private Button btnFinalizar;       // Botón “Finalizar y enviar”
    private int totalCriterios = 0;    // Cuenta total de criterios de “Visión Frontal”

    private VideoView videoView;       // VideoView para reproducir un vídeo de ejemplo
    private FrameLayout frameVideoContainer; // Contenedor con padding (8dp)

    // Para elegir vídeo desde la galería
    private androidx.activity.result.ActivityResultLauncher<String> pickVideoLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        // Inflamos el layout: fragment_questions_frontal.xml
        return inflater.inflate(R.layout.fragment_questions_frontal, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) Referenciamos VideoView y su FrameLayout contenedor
        videoView = view.findViewById(R.id.videoViewFrontal);
        frameVideoContainer = view.findViewById(R.id.frameVideoContainerFrontal);

        /*
         * 2) Sólo usamos setZOrderMediaOverlay(true) para que el MediaController se dibuje
         * justo encima del VideoView, pero NO forcemos setZOrderOnTop(true), pues esto suele
         * hacer que el vídeo “salte” fuera de su marco y se desalineen los controles.
         */
        videoView.setZOrderMediaOverlay(true);

        /*
         * 3) Creamos el MediaController y lo anclamos AL VideoView:
         *    Así, el ancho del controlador coincidirá con el ancho real del video (200dp menos 16dp de padding).
         */
        MediaController mediaController = new MediaController(requireContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        /*
         * 4) Registramos el lanzador para elegir vídeo desde galería (“video/*”):
         *    Cuando el usuario seleccione un URI, lo cargamos en el VideoView y lo reproducimos en bucle.
         */
        pickVideoLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        videoView.setVideoURI(uri);
                        // Reanclar el MediaController
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

        // 5) Botón para abrir la galería y elegir vídeo
        Button btnPick = view.findViewById(R.id.btnPickVideoFrontal);
        btnPick.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        // 6) Inicializamos ViewModel y conectamos las vistas del formulario
        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);
        container = view.findViewById(R.id.containerFrontal);
        btnFinalizar = view.findViewById(R.id.btnFinalizar);

        // 7) Generamos dinámicamente la sección “VISIÓN FRONTAL” a partir del JSON
        renderFrontalSection();

        // 8) Al pulsar “Finalizar y enviar”, mostramos (por ejemplo) un Toast con todas las respuestas
        btnFinalizar.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : viewModel.getTodasRespuestas().entrySet()) {
                sb.append(entry.getKey())
                        .append(" → ")
                        .append(entry.getValue() ? "Sí" : "No")
                        .append("\n");
            }
            Toast.makeText(requireContext(), sb.toString(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Lee el asset “questions_crol.json”, busca la sección “VISIÓN FRONTAL” y construye:
     *  - Un título principal “VISIÓN FRONTAL”
     *  - Cada subtítulo de subsección (p.ej. “POSICIÓN del CUERPO”)
     *  - Cada criterio dentro de esa subsección, metido en un CardView con un RadioGroup (Sí/No).
     *  - Cuenta el nº total de criterios; solo cuando el usuario marque todos se habilita “Finalizar”.
     */
    private void renderFrontalSection() {
        try {
            // 1) Leer JSON desde assets/questions_crol.json
            InputStream is = requireContext().getAssets().open("questions_crol.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);

            // 2) Título principal “VISIÓN FRONTAL”
            TextView tvSection = new TextView(requireContext());
            tvSection.setText("VISIÓN FRONTAL");
            tvSection.setTextSize(22f);
            tvSection.setTextColor(0xFFFFFFFF);
            tvSection.setBackgroundColor(0xFF1976D2);
            tvSection.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            container.addView(tvSection);

            // 3) Encontrar la sección “VISIÓN FRONTAL” y contar cuántos criterios incluye
            if (root.has("sections")) {
                JSONArray sectionsArr = root.getJSONArray("sections");

                // 3.1) Sumar todos los criterios de “VISIÓN FRONTAL”
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

                // 3.2) Renderizar cada subsección y sus criterios
                for (int s = 0; s < sectionsArr.length(); s++) {
                    JSONObject sectObj = sectionsArr.getJSONObject(s);
                    String sectionName = sectObj.optString("name", "");
                    if ("VISIÓN FRONTAL".equalsIgnoreCase(sectionName)) {
                        JSONArray subsectionsArr = sectObj.getJSONArray("subsections");

                        for (int i = 0; i < subsectionsArr.length(); i++) {
                            JSONObject subObj = subsectionsArr.getJSONObject(i);
                            String subName = subObj.optString("name", "");

                            // Subtítulo de la subsección
                            TextView tvSub = new TextView(requireContext());
                            tvSub.setText(subName);
                            tvSub.setTextSize(18f);
                            tvSub.setTextColor(0xFF0D47A1);
                            tvSub.setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(4));
                            container.addView(tvSub);

                            // Iterar criterios de esta subsección
                            JSONArray criteriaArr = subObj.getJSONArray("criteria");
                            for (int c = 0; c < criteriaArr.length(); c++) {
                                String criterio = criteriaArr.getString(c);
                                String clave = "FRONTAL|" + criterio;

                                // Crear un CardView para cada criterio
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

                                // Contenedor interno vertical
                                LinearLayout inner = new LinearLayout(requireContext());
                                inner.setOrientation(LinearLayout.VERTICAL);
                                inner.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                                card.addView(inner);

                                // Texto del criterio
                                TextView tvCrit = new TextView(requireContext());
                                tvCrit.setText(criterio);
                                tvCrit.setTextSize(16f);
                                tvCrit.setTextColor(0xFF000000);
                                inner.addView(tvCrit);

                                // RadioGroup horizontal (Sí / No)
                                RadioGroup rg = new RadioGroup(requireContext());
                                rg.setOrientation(RadioGroup.HORIZONTAL);
                                rg.setPadding(0, dpToPx(8), 0, 0);

                                RadioButton rbSi = new RadioButton(requireContext());
                                rbSi.setText("Sí");
                                rbSi.setButtonTintList(
                                        android.content.res.ColorStateList.valueOf(0xFF1976D2)
                                );
                                rbSi.setId(View.generateViewId());
                                rg.addView(rbSi);

                                RadioButton rbNo = new RadioButton(requireContext());
                                rbNo.setText("No");
                                rbNo.setButtonTintList(
                                        android.content.res.ColorStateList.valueOf(0xFFD32F2F)
                                );
                                rbNo.setId(View.generateViewId());
                                rg.addView(rbNo);

                                // Si ya había una respuesta guardada, restauramos la selección
                                if (viewModel.tieneRespuesta(clave)) {
                                    boolean fueSi = viewModel.getRespuesta(clave);
                                    if (fueSi) rbSi.setChecked(true);
                                    else rbNo.setChecked(true);
                                }

                                // Listener para guardar la respuesta en el ViewModel
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

                // 3.3) Si ya estaban todas contestadas, habilitar “Finalizar y enviar” al instante
                int inicialContestadas = viewModel.getNumContestadosEnSeccion("FRONTAL");
                if (inicialContestadas >= totalCriterios) {
                    btnFinalizar.setEnabled(true);
                }
            }
        } catch (Exception e) {
            // En caso de error, mostramos un TextView con el mensaje rojo
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando sección FRONTAL:\n" + e.getMessage());
            tvError.setTextColor(0xFFFF0000);
            tvError.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            container.addView(tvError);
        }
    }

    /** Convierte densidad dp a píxeles para aplicar paddings/margins */
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
