package com.project.tecniswim.ui.evaluate;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.tecniswim.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * UsersFragment: muestra un buscador de usuarios, permite seleccionar uno y, al pulsar
 * “Confirmar y enviar”, genera un PDF con las respuestas del QuestionsViewModel y lo envía por
 * correo al usuario seleccionado.
 */
public class UsersFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvUsers;
    private Button btnConfirmSend;

    private UsersAdapter adapter;
    private List<UserItem> allUsers = new ArrayList<>();

    // Usuario actualmente seleccionado
    private UserItem selectedUser = null;

    // Firestore
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // ViewModel que contiene las respuestas y el estilo
    private QuestionsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup containerParent,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_users, containerParent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch        = view.findViewById(R.id.etSearchUsers);
        rvUsers         = view.findViewById(R.id.rvUsersList);
        btnConfirmSend  = view.findViewById(R.id.btnConfirmSend);

        // 1) Configurar RecyclerView
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UsersAdapter(new ArrayList<>());
        rvUsers.setAdapter(adapter);

        // 2) Obtener el ViewModel compartido
        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);

        // 3) Cargar todos los usuarios desde Firestore
        loadAllUsersFromFirestore();

        // 4) Configurar filtrado en tiempo real
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString().trim());
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        // 5) Listener en adapter: cuando pulsan un usuario, lo marcamos y mostramos el botón
        adapter.setOnItemClickListener(user -> {
            selectedUser = user;
            btnConfirmSend.setVisibility(View.VISIBLE);
        });

        // 6) Al pulsar “Confirmar y enviar”:
        //    - Si no hay usuario seleccionado: mostrar Toast.
        //    - Si hay uno, generar el PDF y enviar correo.
        btnConfirmSend.setOnClickListener(v -> {
            if (selectedUser == null) {
                Toast.makeText(requireContext(), "Selecciona primero un usuario", Toast.LENGTH_SHORT).show();
                return;
            }
            // Generar y enviar PDF al email del usuario seleccionado
            String destinatarioEmail = selectedUser.getEmail();
            String destinatarioNombre = selectedUser.getFirstName() + " " + selectedUser.getLastName();
            File pdfFile = generarPDFParaUsuario(destinatarioEmail, destinatarioNombre);
            if (pdfFile != null) {
                enviarCorreoConAdjunto(destinatarioEmail, pdfFile);
            } else {
                Toast.makeText(requireContext(), "Error generando PDF", Toast.LENGTH_SHORT).show();
            }
        });

        // Por defecto, ocultamos el botón
        btnConfirmSend.setVisibility(View.GONE);
    }

    /**
     * Carga todos los usuarios desde Firestore (colección “Usuarios”),
     * y actualiza el adapter.
     */
    private void loadAllUsersFromFirestore() {
        firestore.collection("Usuarios")
                .get()
                .addOnSuccessListener((QuerySnapshot snapshots) -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String firstName = doc.getString("firstName");
                        String lastName  = doc.getString("lastName");
                        String email     = doc.getString("email");
                        String uid       = doc.getId();
                        if (firstName == null) firstName = "";
                        if (lastName == null)  lastName = "";
                        if (email == null)     email = "";
                        allUsers.add(new UserItem(uid, firstName, lastName, email));
                    }
                    adapter.updateList(allUsers);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Error cargando usuarios: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Genera un PDF en /storage/emulated/0/Android/data/[paquete]/files/informe_evaluacion.pdf
     * que incluye:
     *   - Estilo seleccionado (crol/espalda/braza/mariposa)
     *   - Todas las secciones (Lateral, Posterior si corresponde, Frontal) y sus respuestas
     *   - Nombre y correo del destinatario en el encabezado
     *
     * @param destinatarioEmail El email del usuario (solo para mostrar en el PDF)
     * @param destinatarioNombre El nombre completo del usuario (ídem)
     * @return El File generado (o null si hubo error)
     */
    private File generarPDFParaUsuario(String destinatarioEmail, String destinatarioNombre) {
        try {
            // 1) Leer JSON para el estilo seleccionado
            String estilo = viewModel.getSelectedStyle(); // "crol", "espalda", "braza" o "mariposa"
            if (estilo == null) estilo = "crol"; // fallback
            String jsonFileName = "questions_" + estilo.toLowerCase() + ".json";
            InputStream is = requireContext().getAssets().open(jsonFileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject root = new JSONObject(new String(buffer, StandardCharsets.UTF_8));

            // 2) Crear PdfDocument tamaño A4 (595×842)
            PdfDocument documento = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page pagina = documento.startPage(pageInfo);
            Canvas canvas = pagina.getCanvas();

            // 3) Preparar Paints y TextPaints
            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            titlePaint.setTextSize(20);
            titlePaint.setTextAlign(Paint.Align.CENTER);

            Paint infoPaint = new Paint();
            infoPaint.setColor(Color.BLACK);
            infoPaint.setTextSize(14);
            infoPaint.setTextAlign(Paint.Align.LEFT);

            Paint sectBgPaint = new Paint();
            sectBgPaint.setColor(0xFF1976D2);

            Paint sectTextPaint = new Paint();
            sectTextPaint.setColor(Color.WHITE);
            sectTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            sectTextPaint.setTextSize(16);
            sectTextPaint.setTextAlign(Paint.Align.LEFT);

            Paint subBgPaint = new Paint();
            subBgPaint.setColor(0xFFBBDEFB);

            Paint subTextPaint = new Paint();
            subTextPaint.setColor(0xFF0D47A1);
            subTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            subTextPaint.setTextSize(14);
            subTextPaint.setTextAlign(Paint.Align.LEFT);

            // Usamos TextPaint para StaticLayout (line wrapping)
            TextPaint critTextPaint = new TextPaint();
            critTextPaint.setColor(Color.BLACK);
            critTextPaint.setTextSize(12);
            critTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            critTextPaint.setTextAlign(Paint.Align.LEFT);

            Paint markPaint = new Paint();
            markPaint.setColor(Color.BLACK);
            markPaint.setTextSize(12);
            markPaint.setTextAlign(Paint.Align.CENTER);

            // 4) Posiciones iniciales
            int xMargin = 40;
            int y = 40;

            // 4.a) Dibujar título centrado
            String titulo = "Informe de Evaluación - " + estilo.toUpperCase();
            canvas.drawText(titulo, pageInfo.getPageWidth() / 2f, y, titlePaint);
            y += 30;

            // 4.b) Dibujar información de destinatario
            String lineTecnico = "Técnico: ________________________";
            canvas.drawText(lineTecnico, xMargin, y, infoPaint);
            y += 20;
            String lineNadador = "Nadador: " + destinatarioNombre;
            canvas.drawText(lineNadador, xMargin, y, infoPaint);
            y += 20;
            String lineEmail = "Correo: " + destinatarioEmail;
            canvas.drawText(lineEmail, xMargin, y, infoPaint);
            y += 30;

            // Pre-cálculos para columnas de "Sí" / "No"
            float pageWidthF = pageInfo.getPageWidth();
            float availableWidth = pageWidthF - 2 * xMargin;
            float textAreaWidth = availableWidth * 0.8f;
            float markAreaWidth = availableWidth * 0.2f;
            float markColumnWidth = markAreaWidth / 2f;
            float markAreaStartX = xMargin + textAreaWidth;
            float yesColumnX = markAreaStartX + (markColumnWidth / 2f);
            float noColumnX  = markAreaStartX + (markColumnWidth * 1.5f);

            // 5) Recorrer secciones del JSON
            if (root.has("sections")) {
                JSONArray sectionsArr = root.getJSONArray("sections");

                for (int s = 0; s < sectionsArr.length(); s++) {
                    JSONObject sectObj = sectionsArr.getJSONObject(s);
                    // Normalizamos el nombre de sección y calculamos la clave de ViewModel
                    String sectionNameRaw = sectObj.getString("name").trim();            // e.g. "VISIÓN LATERAL"
                    String sectionKey = sectionNameRaw.toUpperCase().replace("VISIÓN ", ""); // "LATERAL", "FRONTAL" o "POSTERIOR"

                    // 5.a) Dibujar franja de sección
                    canvas.drawRect(
                            xMargin - 10,
                            y - 18,
                            pageInfo.getPageWidth() - xMargin + 10,
                            y + 6,
                            sectBgPaint
                    );
                    canvas.drawText(sectionNameRaw, xMargin, y, sectTextPaint);
                    y += 28;

                    // 5.b) Recorrer subsecciones
                    JSONArray subsArr = sectObj.getJSONArray("subsections");
                    for (int ss = 0; ss < subsArr.length(); ss++) {
                        JSONObject subObj = subsArr.getJSONObject(ss);
                        // Normalizamos el nombre de subsección
                        String subName = subObj.getString("name").trim();

                        // Dibujar franja de subsección
                        canvas.drawRect(
                                xMargin - 10,
                                y - 14,
                                pageInfo.getPageWidth() - xMargin + 10,
                                y + 4,
                                subBgPaint
                        );
                        canvas.drawText(subName, xMargin, y, subTextPaint);
                        y += 20;

                        // Dibujar encabezados de columnas "Sí" y "No"
                        canvas.drawText("Sí", yesColumnX, y, markPaint);
                        canvas.drawText("No", noColumnX, y, markPaint);
                        y += 18;

                        // 5.b.1) Iterar criterios y sus respuestas
                        JSONArray critArr = subObj.getJSONArray("criteria");
                        for (int cidx = 0; cidx < critArr.length(); cidx++) {
                            // Normalizamos el texto del criterio
                            String criterio = critArr.getString(cidx).trim();
                            // Ahora construimos la clave tal como lo hace el ViewModel:
                            //     "<SECCION>|< criterio >"
                            String clave = sectionKey + "|" + criterio;

                            // Depuración: verificar que coincide con las claves del ViewModel
                            boolean tieneResp = viewModel.tieneRespuesta(clave);
                            boolean respuesta = false;
                            if (tieneResp) {
                                respuesta = viewModel.getRespuesta(clave);
                            }
                            Log.d("PDF_DEBUG", "Clave='" + clave + "' | tieneRespuesta=" + tieneResp + " | respuesta=" + respuesta);

                            // Preparar texto con viñeta
                            String bullet = "• " + criterio;

                            // Ajustar texto con StaticLayout
                            StaticLayout layout = new StaticLayout(
                                    bullet,
                                    critTextPaint,
                                    (int) textAreaWidth,
                                    Layout.Alignment.ALIGN_NORMAL,
                                    1.0f,
                                    0.0f,
                                    false
                            );

                            // Dibujar el texto adaptado
                            canvas.save();
                            canvas.translate(xMargin + 10, y);
                            layout.draw(canvas);
                            canvas.restore();

                            int textHeight = layout.getHeight();

                            // Calcular posición vertical centrada para la "X"
                            float markY = y + (textHeight / 2f) + 4;

                            // Dibujar la marca en la columna "Sí" o "No"
                            if (respuesta) {
                                canvas.drawText("X", yesColumnX, markY, markPaint);
                            } else {
                                canvas.drawText("X", noColumnX, markY, markPaint);
                            }

                            y += textHeight + 8;

                            if (y > pageInfo.getPageHeight() - 60) {
                                break;
                            }
                        }

                        y += 10;
                        if (y > pageInfo.getPageHeight() - 80) break;
                    }

                    y += 10;
                    if (y > pageInfo.getPageHeight() - 80) break;
                }
            }

            // 6) Finalizar página y guardar PDF
            documento.finishPage(pagina);
            File folder = new File(requireContext().getExternalFilesDir(null), "reports");
            if (!folder.exists()) folder.mkdirs();
            File pdfFile = new File(folder, "informe_" + estilo + "_" + destinatarioNombre + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                documento.writeTo(fos);
            }
            documento.close();
            return pdfFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Envía un correo con SMTP utilizando la librería javax.mail. Esto se hace en un hilo aparte
     * para no bloquear la interfaz.
     *
     * Credenciales fijas: tecniswim@gmail.com / puwn jamf ckuf gfnl
     *
     * @param destinatarioEmail e-mail al que se envía el PDF
     * @param pdfFile           El fichero PDF que se adjunta
     */
    private void enviarCorreoConAdjunto(String destinatarioEmail, File pdfFile) {
        //  Para permitir la operación de correo desde el hilo de UI (solo para test),
        //  se emplea StrictMode; en producción no es recomendable.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new Thread(() -> {
            try {
                // 1) Propiedades SMTP
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.port", "465");

                // 2) Sesión autenticada
                Session session = Session.getDefaultInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                "tecniswim@gmail.com",
                                "puwn jamf ckuf gfnl"
                        );
                    }
                });

                // 3) Construir el mensaje
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("tecniswim@gmail.com"));
                message.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(destinatarioEmail)
                );
                message.setSubject("Informe de Evaluación - TecniSwim");

                // 4) Parte de texto
                MimeBodyPart cuerpo = new MimeBodyPart();
                cuerpo.setText("Hola,\n\nAdjunto encontrarás tu informe de evaluación generado por la app.\n\nSaludos,\nTecniSwim");

                // 5) Parte de adjunto
                MimeBodyPart adjunto = new MimeBodyPart();
                DataSource source = new FileDataSource(pdfFile.getAbsolutePath());
                adjunto.setDataHandler(new DataHandler(source));
                adjunto.setFileName("informe_evaluacion.pdf");

                // 6) Ensamblar multipart
                MimeMultipart multipart = new MimeMultipart();
                multipart.addBodyPart(cuerpo);
                multipart.addBodyPart(adjunto);
                message.setContent(multipart);

                // 7) Enviar
                Transport.send(message);

                // 8) Avisar en UI
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Correo enviado correctamente a " + destinatarioEmail,
                                Toast.LENGTH_LONG
                        ).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Error al enviar correo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
