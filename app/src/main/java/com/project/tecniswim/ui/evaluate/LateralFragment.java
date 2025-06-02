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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.project.tecniswim.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Fragmento que muestra la sección “VISIÓN LATERAL” de la evaluación.
 * Contiene:
 *  - VideoView (con MediaController anclado sobre el propio VideoView).
 *  - Un ScrollView con Cards para cada criterio (Apto/No apto).
 *  - Botón “Continuar” que se habilita al responder todos los criterios.
 */
public class LateralFragment extends Fragment {

    private QuestionsViewModel viewModel;
    private LinearLayout container;      // LinearLayout dentro del ScrollView
    private Button btnContinuar;         // Botón “Continuar”
    private int totalCriterios = 0;      // Cuenta de criterios de “Visión Lateral”
    private VideoView videoView;         // Para reproducir vídeo de demostración
    private FrameLayout frameVideoContainer; // Contenedor con padding

    private ActivityResultLauncher<String> pickVideoLauncher;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup containerParent,
            Bundle savedInstanceState) {
        // Inflamos el layout que acabamos de ver en fragment_questions_lateral.xml
        return inflater.inflate(R.layout.fragment_questions_lateral, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) Referencia a VideoView y su FrameLayout contenedor
        videoView = view.findViewById(R.id.videoViewLateral);
        frameVideoContainer = view.findViewById(R.id.frameVideoContainer);

        /*
         * 2) Solo dejamos setZOrderMediaOverlay(true). NO usamos setZOrderOnTop(true)
         *    para evitar que el vídeo se “salga” de su contenedor.
         */
        videoView.setZOrderMediaOverlay(true);

        /*
         * 3) Crear MediaController y anclarlo AL VideoView.
         *    De este modo, el ancho del controlador coincide con el ancho real del vídeo
         *    (que respeta los 8dp de padding del FrameLayout).
         */
        MediaController mediaController = new MediaController(requireContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        /*
         * 4) Registrar el lanzador para elegir vídeo desde galería:
         *    Usamos “GetContent” con MIME type “video/*”.
         */
        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            // Cargar el vídeo en el VideoView
                            videoView.setVideoURI(uri);

                            // Reanclar el MediaController (por si se reinicia el VideoView)
                            mediaController.setAnchorView(videoView);
                            videoView.setMediaController(mediaController);

                            // Preparar el vídeo y reproducir en bucle
                            videoView.setOnPreparedListener(mp -> {
                                mp.setLooping(true);
                                videoView.seekTo(100);
                                videoView.start();
                            });
                        }
                    }
                }
        );

        // 5) Botón para abrir la galería y elegir vídeo
        Button btnPick = view.findViewById(R.id.btnPickVideoLateral);
        btnPick.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        // 6) Inicializar ViewModel y conectar vistas del formulario
        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);
        container = view.findViewById(R.id.containerLateral);
        btnContinuar = view.findViewById(R.id.btnContinuar);

        // 7) Renderizar dinámicamente la sección “VISIÓN LATERAL” desde JSON
        renderLateralSection();

        // 8) Al presionar “Continuar”, navegar a FrontalFragment
        btnContinuar.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_lateralFragment_to_frontalFragment)
        );
    }

    /**
     * Abre el asset “questions_crol.json” y genera todas las tarjetas de criterios:
     * - Título principal “VISIÓN LATERAL”
     * - Cada subsección con su nombre y luego cada criterio como Card con RadioButtons.
     * - Lleva cuenta de cuántos criterios hay y habilita "Continuar" solo cuando se marquen todos.
     */
    private void renderLateralSection() {
        try {
            // 1) Leer JSON
            InputStream is = requireContext().getAssets().open("questions_crol.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonText = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonText);

            // 2) Título grande “VISIÓN LATERAL”
            TextView tvSection = new TextView(requireContext());
            tvSection.setText("VISIÓN LATERAL");
            tvSection.setTextSize(22f);
            tvSection.setTextColor(0xFFFFFFFF);
            tvSection.setBackgroundColor(0xFF1976D2);
            tvSection.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            container.addView(tvSection);

            // 3) Buscar la sección correspondiente en el JSON y contar criterios
            if (root.has("sections")) {
                JSONArray sectionsArr = root.getJSONArray("sections");
                totalCriterios = 0;

                // 3.1) Sumar todos los criterios para “VISIÓN LATERAL”
                for (int s = 0; s < sectionsArr.length(); s++) {
                    JSONObject sectObj = sectionsArr.getJSONObject(s);
                    String sectionName = sectObj.optString("name", "");
                    if ("VISIÓN LATERAL".equalsIgnoreCase(sectionName)) {
                        JSONArray subsectionsArr = sectObj.getJSONArray("subsections");
                        for (int i = 0; i < subsectionsArr.length(); i++) {
                            JSONArray criteriaArr = subsectionsArr
                                    .getJSONObject(i)
                                    .getJSONArray("criteria");
                            totalCriterios += criteriaArr.length();
                        }
                        break;
                    }
                }

                // 3.2) Renderizar cada subsección y sus criterios
                for (int s = 0; s < sectionsArr.length(); s++) {
                    JSONObject sectObj = sectionsArr.getJSONObject(s);
                    String sectionName = sectObj.optString("name", "");
                    if ("VISIÓN LATERAL".equalsIgnoreCase(sectionName)) {
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

                            // Iterar criterios en esta subsección
                            JSONArray criteriaArr = subObj.getJSONArray("criteria");
                            for (int c = 0; c < criteriaArr.length(); c++) {
                                String criterio = criteriaArr.getString(c);
                                String clave = "LATERAL|" + criterio;

                                // Crear CardView para cada criterio
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

                                // Contenedor interno vertical para texto + RadioGroup
                                LinearLayout inner = new LinearLayout(requireContext());
                                inner.setOrientation(LinearLayout.VERTICAL);
                                inner.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                                card.addView(inner);

                                // Mostrar texto del criterio
                                TextView tvCrit = new TextView(requireContext());
                                tvCrit.setText(criterio);
                                tvCrit.setTextSize(16f);
                                tvCrit.setTextColor(0xFF000000);
                                inner.addView(tvCrit);

                                // RadioGroup horizontal con dos opciones
                                RadioGroup rg = new RadioGroup(requireContext());
                                rg.setOrientation(RadioGroup.HORIZONTAL);
                                rg.setPadding(0, dpToPx(8), 0, 0);

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

                                // Restaurar selección anterior (si existe)
                                if (viewModel.tieneRespuesta(clave)) {
                                    boolean fueApto = viewModel.getRespuesta(clave);
                                    if (fueApto) rbApto.setChecked(true);
                                    else rbNoApto.setChecked(true);
                                }

                                // Listener que guarda la respuesta en ViewModel
                                rg.setOnCheckedChangeListener((group, checkedId) -> {
                                    boolean esApto = (checkedId == rbApto.getId());
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

                // 3.3) Si ya había respuestas marcadas al iniciar, habilitar “Continuar” de inmediato
                int inicialContestadas = viewModel.getNumContestadosEnSeccion("LATERAL");
                if (inicialContestadas >= totalCriterios) {
                    btnContinuar.setEnabled(true);
                }
            }
        } catch (Exception e) {
            // En caso de error, mostrar un TextView rojo
            TextView tvError = new TextView(requireContext());
            tvError.setText("Error cargando sección LATERAL:\n" + e.getMessage());
            tvError.setTextColor(0xFFFF0000);
            tvError.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            container.addView(tvError);
        }
    }

    /** Convierte densidad dp a píxeles internos */
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
