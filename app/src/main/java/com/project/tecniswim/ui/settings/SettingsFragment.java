package com.project.tecniswim.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.SetOptions;
import com.project.tecniswim.R;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class SettingsFragment extends Fragment {

    private ImageView ivProfile;
    private TextView  tvChangePhotoHint, tvEmail, tvDisplayName, tvFirstName, tvLastName;
    private Button    btnSave;

    private FirebaseAuth      auth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String[]> permLauncher;

    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private static final String TAG = "SettingsFragment";

    private Bitmap currentBitmap;
    private String externalPhotoUrl = "";

    @Override
    public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        registerLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup parent,
                             @Nullable Bundle state) {

        View v = inf.inflate(R.layout.fragment_settings, parent, false);
        bindViews(v);
        loadUserData();

        ivProfile.setOnClickListener(_v -> checkPermsAndDialog());
        btnSave   .setOnClickListener(_v -> savePhoto());
        return v;
    }

    private void registerLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode() == AppCompatActivity.RESULT_OK
                            && res.getData() != null
                            && res.getData().getExtras() != null) {
                        Bitmap bmp = (Bitmap) res.getData().getExtras().get("data");
                        if (bmp != null) {
                            showCircularBitmap(bmp);
                            externalPhotoUrl = "";
                        }
                    }
                }
        );

        // Galería
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) loadBitmapFromUri(uri);
                }
        );

        // Permisos
        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                res -> {
                    boolean granted = !res.values().contains(Boolean.FALSE);
                    if (granted) showImageDialog();
                    else Toast.makeText(requireContext(),
                            "Permisos necesarios", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void checkPermsAndDialog() {
        List<String> need = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                need.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (need.isEmpty()) {
            showImageDialog();
        } else {
            permLauncher.launch(need.toArray(new String[0]));
        }
    }

    private void showImageDialog() {
        String[] items = {"Cámara", "Galería", "URL externa"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar imagen")
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        Intent cam = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(cam);
                    } else if (w == 1) {
                        galleryLauncher.launch("image/*");
                    } else {
                        showUrlDialog();
                    }
                })
                .show();
    }

    private void showUrlDialog() {
        EditText et = new EditText(requireContext());
        et.setHint("https://ejemplo.com/foto.jpg");
        new AlertDialog.Builder(requireContext())
                .setTitle("Introduce URL de la imagen")
                .setView(et)
                .setPositiveButton("OK", (d, w) -> {
                    String url = et.getText().toString().trim();
                    if (url.startsWith("http")) {
                        externalPhotoUrl = url;
                        loadBitmapFromUrl(url);
                    } else {
                        Toast.makeText(requireContext(),
                                "URL no válida", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadBitmapFromUri(Uri uri) {
        exec.execute(() -> {
            Bitmap bmp = null;
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                if (in != null) bmp = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                Log.e(TAG, "Galería", e);
            }

            Bitmap f = bmp;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (f != null) showCircularBitmap(f);
                else showPlaceholder();
            });
        });
    }

    private void loadBitmapFromUrl(String url) {
        exec.execute(() -> {
            Bitmap bmp = null;
            try (InputStream in = new java.net.URL(url).openStream()) {
                bmp = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                Log.e(TAG, "URL", e);
            }

            Bitmap f = bmp;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (f != null) showCircularBitmap(f);
                else showPlaceholder();
            });
        });
    }

    private void loadUserData() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();

        db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        loadAuthOrDefault(u);
                        return;
                    }

                    String firestorePhotoUrl = snap.getString("photoUrl");
                    String base64 = snap.getString("photoBase64");

                    if (!TextUtils.isEmpty(firestorePhotoUrl)) {
                        externalPhotoUrl = firestorePhotoUrl;
                        Glide.with(this)
                                .load(firestorePhotoUrl)
                                .circleCrop()
                                .into(ivProfile);
                    } else if (!TextUtils.isEmpty(base64)) {
                        setBitmapFromBase64(base64);
                        externalPhotoUrl = "";
                    } else {
                        loadAuthOrDefault(u);
                    }

                    tvEmail.setText("Email: " + nullToEmpty(snap.getString("email")));

                    String googleName = u.getDisplayName();
                    if (!TextUtils.isEmpty(googleName)) {
                        tvDisplayName.setText("Nombre completo: " + googleName);
                        String[] p = googleName.split(" ", 2);
                        tvFirstName.setText("Nombre: "    + p[0]);
                        tvLastName .setText("Apellidos: " + (p.length > 1 ? p[1] : ""));
                    } else {
                        String fn = nullToEmpty(snap.getString("firstName"));
                        String ln = nullToEmpty(snap.getString("lastName"));
                        tvDisplayName.setText("Nombre completo: " + (fn + " " + ln).trim());
                        tvFirstName.setText("Nombre: " + fn);
                        tvLastName .setText("Apellidos: " + ln);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    FirebaseUser u2 = auth.getCurrentUser();
                    if (u2 != null) loadAuthOrDefault(u2);
                });
    }

    private void loadAuthOrDefault(FirebaseUser u) {
        Uri photoAuth = u.getPhotoUrl();
        if (photoAuth != null) {
            Glide.with(this)
                    .load(photoAuth)
                    .circleCrop()
                    .into(ivProfile);
            externalPhotoUrl = "";
        } else {
            showPlaceholder();
            externalPhotoUrl = "";
        }
    }

    private void setBitmapFromBase64(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            showCircularBitmap(bmp);
            externalPhotoUrl = "";
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Base64 inválido", e);
            showPlaceholder();
        }
    }

    private void savePhoto() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();

        if (currentBitmap == null && TextUtils.isEmpty(externalPhotoUrl)) {
            Toast.makeText(requireContext(),
                    "Selecciona una imagen primero", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String,Object> data = new HashMap<>();
        if (!TextUtils.isEmpty(externalPhotoUrl)) {
            data.put("photoUrl", externalPhotoUrl);
            data.put("photoBase64", "");
        } else {
            data.put("photoUrl", "");
            data.put("photoBase64", bitmapToBase64(currentBitmap));
        }

        db.collection("Usuarios").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Toast.makeText(requireContext(),
                                "Foto guardada correctamente", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error al guardar foto", Toast.LENGTH_SHORT).show()
                );
    }

    private void showCircularBitmap(@NonNull Bitmap bmp) {
        Glide.with(this)
                .load(bmp)
                .circleCrop()
                .into(ivProfile);
        currentBitmap = bmp;
    }

    private void showPlaceholder() {
        Glide.with(this)
                .load(R.mipmap.ic_tecniswim_launcher_round)
                .circleCrop()
                .into(ivProfile);
        currentBitmap = null;
    }

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void bindViews(View v) {
        ivProfile         = v.findViewById(R.id.ivProfile);
        tvChangePhotoHint = v.findViewById(R.id.tvChangePhotoHint);
        tvEmail           = v.findViewById(R.id.tvEmail);
        tvDisplayName     = v.findViewById(R.id.tvDisplayName);
        tvFirstName       = v.findViewById(R.id.tvFirstName);
        tvLastName        = v.findViewById(R.id.tvLastName);
        btnSave           = v.findViewById(R.id.btnSave);
    }

    private String nullToEmpty(@Nullable String s) {
        return s != null ? s : "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exec.shutdownNow();
    }
}