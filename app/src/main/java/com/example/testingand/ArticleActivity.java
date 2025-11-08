package com.example.testingand;


import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.content.ContentResolver;
import android.content.pm.PackageManager;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;


import com.example.testingand.exceptions.ServerCommunicationError;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Properties;


public class ArticleActivity extends AppCompatActivity {


    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());


    private TextView title;
    private TextView meta;
    private TextView subtitle; //footer
    private TextView articleAbstract;
    private TextView body;
    private ImageView imageView;
    private View btnAlterImage;
    private Article article;


    private Uri selectedImageUri = null;
    private String selectedImageBase64 = null;


    private File cameraTempFile = null;
    private Uri cameraTempUri = null;


    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedImageUri = uri;
                encodeImageToBase64(uri);
                saveImageOnArticle();
            });


    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraTempUri != null) {
                    selectedImageUri = cameraTempUri;
                    encodeImageToBase64(cameraTempUri);
                    saveImageOnArticle();
                } else {
                    Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show();
                }
            });
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) launchCameraNow();
                else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            });




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);


        title = findViewById(R.id.article_title);
        meta = findViewById(R.id.article_meta);
        subtitle = findViewById(R.id.article_subtitle);
        articleAbstract = findViewById(R.id.article_abstract);
        body = findViewById(R.id.article_body);
        imageView = findViewById(R.id.article_image);
        btnAlterImage = findViewById(R.id.btnCreate); // din knapp i layouten




        article = (Article) getIntent().getSerializableExtra("article");

        title.setText(nz(article.getTitleText()));

        String category = nz(article.getCategory());
        String when = nz(article.getUpdatedAtDisplay());
        String who = nz(String.valueOf(article.getIdUser()));
        String metaText = "";
        if (!category.isEmpty()) metaText += category;
        if (!when.isEmpty() && !when.equals("–")) {
            metaText += (metaText.isEmpty() ? "" : " • ") + "Latest update: " + when + " by user with ID #" + who;
        }
        if (metaText.isEmpty()) {
            meta.setVisibility(View.GONE);
        } else {
            meta.setText(metaText);
            meta.setVisibility(View.VISIBLE);
        }

        // läst via "subtitle" i Article-konstruktorn, med fallback till "footer"
        String foot = nz(article.getFooterText());
        if (foot.isEmpty()) {
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(foot);
            subtitle.setVisibility(View.VISIBLE);
        }

        String absRaw = nz(article.getAbstractText());
        if (absRaw.isEmpty()) {
            articleAbstract.setVisibility(View.GONE);
        } else {
            articleAbstract.setText(toPlainText(absRaw));
            articleAbstract.setVisibility(View.VISIBLE);
        }

        body.setText(HtmlCompat.fromHtml(nz(article.getBodyText()), HtmlCompat.FROM_HTML_MODE_LEGACY));
        body.setMovementMethod(LinkMovementMethod.getInstance());

        imageView.setImageResource(R.drawable.ic_launcher_background);
        executor.execute(() -> {
            try {
                final Image image = article.getImage();
                if (image != null) {
                    final String b64Image = image.getImage();
                    if (b64Image != null && !b64Image.isEmpty()) {
                        byte[] decoded = Base64.decode(b64Image, Base64.DEFAULT);
                        final Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        handler.post(() -> imageView.setImageBitmap(bmp));
                    }
                }
            } catch (ServerCommunicationError e) {
                e.printStackTrace();
            }
        });
        btnAlterImage.setOnClickListener(v -> showImageSourceDialog());
    }

    private void showImageSourceDialog () {
        String[] options = new String[]{"Take photo", "Pick photo from gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select image source")
                .setItems(options, (d, which) -> {
                    if (which == 0) { //Take photo
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            launchCameraNow();
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else { //Välj bild
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void launchCameraNow() {
        try {
            cameraTempFile = File.createTempFile("camera_", ".jpg", getCacheDir());
            String authority = getPackageName() + ".fileprovider";
            cameraTempUri = FileProvider.getUriForFile(this, authority, cameraTempFile);
            takePictureLauncher.launch(cameraTempUri);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not open camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // -------- Bild → Base64 + preview --------
    private void encodeImageToBase64(Uri uri) {
        try {
            ContentResolver cr = getContentResolver();
            try (InputStream is = cr.openInputStream(uri)) {
                Bitmap original = BitmapFactory.decodeStream(is);
                if (original == null) {
                    Toast.makeText(this, "Could not decode image", Toast.LENGTH_SHORT).show();
                    return;
                }

                Bitmap scaled = scaleBitmapIfLarge(original, 1600);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // JNG 100% för mindre storlek
                scaled.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] bytes = bos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                imageView.setImageBitmap(scaled);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Image read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap scaleBitmapIfLarge(Bitmap src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= maxSide) return src;

        float scale = (float) maxSide / (float) max;
        int nw = Math.round(w * scale);
        int nh = Math.round(h * scale);
        return Bitmap.createScaledBitmap(src, nw, nh, true);
    }


    // -------- Spara bilden på artikeln --------
    private void saveImageOnArticle() {
        if (article == null) return;
        if (selectedImageBase64 == null || selectedImageBase64.isEmpty()) return;

        executor.execute(() -> {
            try {
                // Skapa (eller återanvänd) ModelManager – samma som i CreateArticleActivity
                Properties props = new Properties();
                props.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");
                props.setProperty(ModelManager.ATTR_LOGIN_USER, "DEV_TEAM_09");
                props.setProperty(ModelManager.ATTR_LOGIN_PASS, "654321@09");
                ModelManager mm = new ModelManager(props);

                // Se till att artikeln har en ModelManager kopplad
                article.setModelManager(mm);

                // Lägg till/ersätt bild
                article.addImage(selectedImageBase64, "altered image");

                // Spara artikeln
                article.save();

                handler.post(() -> {
                    Toast.makeText(this, "Image updated!", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() ->
                        Toast.makeText(this, "Could not update image: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    // Null-säkra strängar och filtrera bort bokstavlig "null".
    private String nz(String s) {
        if (s == null) return "";
        String t = s.trim();
        return "null".equalsIgnoreCase(t) ? "" : t;
    }

    //Hanterar ev. dubbel-escapad HTML.
    private String toPlainText(String raw) {
        if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) return "";
        CharSequence spanned = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY);
        String once = spanned.toString();
        if (once.contains("&lt;") || once.contains("&gt;")) {
            once = HtmlCompat.fromHtml(once, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
        }
        return once.replace('\u00A0', ' ').trim();
    }
}