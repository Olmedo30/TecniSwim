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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.tecniswim.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

public class UsersFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvUsers;
    private Button btnConfirmSend;

    private UsersAdapter adapter;
    private List<UserItem> allUsers = new ArrayList<>();
    private UserItem selectedUser = null;

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
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

        etSearch       = view.findViewById(R.id.etSearchUsers);
        rvUsers        = view.findViewById(R.id.rvUsersList);
        btnConfirmSend = view.findViewById(R.id.btnConfirmSend);

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UsersAdapter(new ArrayList<>());
        rvUsers.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(QuestionsViewModel.class);

        preloadTecnicoNombre();
        loadAllUsersFromFirestore();

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString().trim());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        adapter.setOnItemClickListener(user -> {
            selectedUser = user;
            btnConfirmSend.setVisibility(View.VISIBLE);
        });

        btnConfirmSend.setOnClickListener(v -> {
            if (selectedUser == null) {
                Toast.makeText(requireContext(),
                        "Selecciona primero un usuario",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String emailDest = selectedUser.getEmail();
            String nameDest  = selectedUser.getFirstName() + " " + selectedUser.getLastName();
            String techName  = viewModel.getTecnicoNombre();
            File pdf = generarPDFParaUsuario(emailDest, nameDest, techName);
            if (pdf != null) {
                enviarCorreoConAdjunto(emailDest, pdf);
            } else {
                Toast.makeText(requireContext(),
                        "Error generando PDF",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirmSend.setVisibility(View.GONE);
    }

    private void preloadTecnicoNombre() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String display = user.getDisplayName();
        if (display != null && !display.isEmpty()) {
            viewModel.setTecnicoNombre(display);
        } else {
            firestore.collection("Usuarios")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(ds -> {
                        String fn = ds.getString("firstName");
                        String ln = ds.getString("lastName");
                        String full = ((fn!=null?fn:"") + " " + (ln!=null?ln:"")).trim();
                        if (full.isEmpty()) full = user.getEmail();
                        viewModel.setTecnicoNombre(full);
                    })
                    .addOnFailureListener(e ->
                            viewModel.setTecnicoNombre(user.getEmail())
                    );
        }
    }

    private void loadAllUsersFromFirestore() {
        firestore.collection("Usuarios")
                .get()
                .addOnSuccessListener((QuerySnapshot snaps) -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        String fn = doc.getString("firstName");
                        String ln = doc.getString("lastName");
                        if ((fn == null || fn.isEmpty()) &&
                                (ln == null || ln.isEmpty())) {
                            String disp = doc.getString("displayName");
                            if (disp != null && !disp.isEmpty()) {
                                String[] parts = disp.split(" ",2);
                                fn = parts[0];
                                ln = parts.length>1 ? parts[1] : "";
                            }
                        }
                        String email = doc.getString("email");
                        allUsers.add(new UserItem(
                                doc.getId(),
                                fn != null ? fn : "",
                                ln != null ? ln : "",
                                email != null ? email : ""
                        ));
                    }
                    adapter.updateList(allUsers);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Error cargando usuarios: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private File generarPDFParaUsuario(
            String destinatarioEmail,
            String destinatarioNombre,
            String tecnicoNombre
    ) {
        try {
            InputStream ejIs = requireContext().getAssets().open("ejercicios.json");
            byte[] ejBuf = new byte[ejIs.available()];
            ejIs.read(ejBuf);
            ejIs.close();
            JSONObject ejerciciosRoot = new JSONObject(new String(ejBuf, StandardCharsets.UTF_8));

            String estilo = viewModel.getSelectedStyle();
            if (estilo == null) estilo = "crol";
            InputStream is = requireContext().getAssets().open("questions_" + estilo.toLowerCase() + ".json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            JSONObject root = new JSONObject(new String(buf, StandardCharsets.UTF_8));

            PdfDocument doc = new PdfDocument();
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = doc.startPage(info);
            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            titlePaint.setTextSize(20);
            titlePaint.setTextAlign(Paint.Align.CENTER);

            Paint infoPaint = new Paint();
            infoPaint.setColor(Color.BLACK);
            infoPaint.setTextSize(14);

            Paint sectBg = new Paint(); sectBg.setColor(0xFF1976D2);
            Paint sectText = new Paint(); sectText.setColor(Color.WHITE);
            sectText.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            sectText.setTextSize(16);

            Paint subBg = new Paint(); subBg.setColor(0xFFBBDEFB);
            Paint subText = new Paint(); subText.setColor(0xFF0D47A1);
            subText.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            subText.setTextSize(14);

            TextPaint critPaint = new TextPaint();
            critPaint.setColor(Color.BLACK);
            critPaint.setTextSize(12);

            Paint markPaint = new Paint();
            markPaint.setColor(Color.BLACK);
            markPaint.setTextSize(12);
            markPaint.setTextAlign(Paint.Align.CENTER);

            final int xM = 40, bottomM = 60;
            int y = 40;

            canvas.drawText("Informe de Evaluación - " + estilo.toUpperCase(),
                    info.getPageWidth() / 2f, y, titlePaint);
            y += 30;

            if (tecnicoNombre == null || tecnicoNombre.isEmpty()) {
                tecnicoNombre = "Técnico desconocido";
            }
            canvas.drawText("Técnico: " + tecnicoNombre, xM, y, infoPaint);
            y += 20;

            canvas.drawText("Nadador: " + destinatarioNombre, xM, y, infoPaint);
            y += 20;
            canvas.drawText("Correo: " + destinatarioEmail, xM, y, infoPaint);
            y += 30;

            int startY = y;
            float pageW = info.getPageWidth();
            float availW = pageW - 2 * xM;
            float columnWidth = 40f;
            float textW = availW - columnWidth * 2;
            float yesX = xM + textW + (columnWidth / 2f);
            float noX  = xM + textW + (columnWidth * 1.5f);

            JSONArray sections = root.optJSONArray("sections");
            if (sections != null) {
                for (int si = 0; si < sections.length(); si++) {
                    JSONObject sectObj = sections.getJSONObject(si);
                    String sectionNameRaw = sectObj.getString("name").trim();
                    String upper = sectionNameRaw.toUpperCase();
                    String sectionKey;
                    if (upper.contains("LATERAL")) sectionKey = "LATERAL";
                    else if (upper.contains("FRONTAL")) sectionKey = "FRONTAL";
                    else if (upper.contains("POSTERIOR")) sectionKey = "POSTERIOR";
                    else sectionKey = upper;

                    if (y > info.getPageHeight() - bottomM) {
                        doc.finishPage(page);
                        page = doc.startPage(info);
                        canvas = page.getCanvas();
                        y = startY;
                    }
                    canvas.drawRect(xM - 10, y - 18, pageW - xM + 10, y + 6, sectBg);
                    canvas.drawText(sectionNameRaw, xM, y, sectText);
                    y += 28;

                    JSONArray subs = sectObj.getJSONArray("subsections");
                    for (int ui = 0; ui < subs.length(); ui++) {
                        JSONObject subObj = subs.getJSONObject(ui);
                        String subName = subObj.getString("name").trim();

                        if (y > info.getPageHeight() - bottomM) {
                            doc.finishPage(page);
                            page = doc.startPage(info);
                            canvas = page.getCanvas();
                            y = startY;
                        }
                        canvas.drawRect(xM - 10, y - 14, pageW - xM + 10, y + 4, subBg);
                        canvas.drawText(subName, xM, y, subText);
                        y += 20;

                        canvas.drawText("Sí", yesX, y, markPaint);
                        canvas.drawText("No", noX, y, markPaint);
                        y += 18;

                        List<String> malos = new ArrayList<>();
                        JSONArray crits = subObj.getJSONArray("criteria");
                        for (int ci = 0; ci < crits.length(); ci++) {
                            if (y > info.getPageHeight() - bottomM) {
                                doc.finishPage(page);
                                page = doc.startPage(info);
                                canvas = page.getCanvas();
                                y = startY;
                            }
                            String crit = crits.getString(ci).trim();
                            String clave = sectionKey + "|" + crit;
                            boolean resp = viewModel.tieneRespuesta(clave) && viewModel.getRespuesta(clave);
                            if (!resp) malos.add(crit);

                            StaticLayout sl = new StaticLayout(
                                    "• " + crit, critPaint,
                                    (int) textW, Layout.Alignment.ALIGN_NORMAL,
                                    1f, 0f, false);
                            canvas.save();
                            canvas.translate(xM + 10, y);
                            sl.draw(canvas);
                            canvas.restore();
                            int h = sl.getHeight();
                            float my = y + h / 2f + 4;
                            canvas.drawText("X", resp ? yesX : noX, my, markPaint);
                            y += h + 8;
                        }

                        if (!malos.isEmpty()) {
                            y += 12;
                            Paint recTitlePaint = new Paint();
                            recTitlePaint.setColor(0xFF1976D2);
                            recTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                            recTitlePaint.setTextSize(14);
                            canvas.drawText("Vídeos recomendados:", xM, y, recTitlePaint);

                            y += 18;
                            Paint recLinkPaint = new Paint();
                            recLinkPaint.setColor(0xFF0D47A1);
                            recLinkPaint.setTextSize(12);
                            recLinkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

                            JSONArray stylesArr = ejerciciosRoot.getJSONArray("styles");
                            for (int i = 0; i < stylesArr.length(); i++) {
                                JSONObject st = stylesArr.getJSONObject(i);
                                if (estilo.equalsIgnoreCase(st.getString("style"))) {
                                    JSONArray secs = st.getJSONArray("sections");
                                    for (int j = 0; j < secs.length(); j++) {
                                        JSONObject sec = secs.getJSONObject(j);
                                        if (subName.equalsIgnoreCase(sec.getString("name"))) {
                                            JSONArray links = sec.getJSONArray("exercise_links");
                                            for (int l = 0; l < links.length(); l++) {
                                                String url = links.getString(l);
                                                if (y > info.getPageHeight() - bottomM) {
                                                    doc.finishPage(page);
                                                    page = doc.startPage(info);
                                                    canvas = page.getCanvas();
                                                    y = startY;
                                                }
                                                canvas.drawText("– " + url, xM + 10, y, recLinkPaint);
                                                y += 14;
                                            }
                                        }
                                    }
                                }
                            }
                            y += 10;
                        }
                        y += 10;
                    }
                    y += 10;
                }
            }

            doc.finishPage(page);
            File fdir = new File(requireContext().getExternalFilesDir(null), "reports");
            if (!fdir.exists()) fdir.mkdirs();
            File out = new File(fdir, "informe_" + estilo + "_" + destinatarioNombre + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                doc.writeTo(fos);
            }
            doc.close();
            return out;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void enviarCorreoConAdjunto(String destinatarioEmail, File pdfFile) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.port", "465");

                Session session = Session.getDefaultInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                "tecniswim@gmail.com",
                                "puwn jamf ckuf gfnl"
                        );
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("tecniswim@gmail.com"));
                message.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(destinatarioEmail)
                );
                message.setSubject("Informe de Evaluación - TecniSwim");

                MimeBodyPart cuerpo = new MimeBodyPart();
                cuerpo.setText("Hola,\n\nAdjunto tu informe de evaluación.\n\nSaludos,\nTecniSwim");

                MimeBodyPart adjunto = new MimeBodyPart();
                DataSource source = new FileDataSource(pdfFile.getAbsolutePath());
                adjunto.setDataHandler(new DataHandler(source));
                adjunto.setFileName("informe_evaluacion.pdf");

                MimeMultipart multipart = new MimeMultipart();
                multipart.addBodyPart(cuerpo);
                multipart.addBodyPart(adjunto);
                message.setContent(multipart);

                Transport.send(message);

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