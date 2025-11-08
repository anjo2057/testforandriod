package com.example.testingand;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class CreateArticleActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText abstractEditText;
    private AutoCompleteTextView categoriesDropdown;
    private Article articleToEdit;

    private TextInputEditText inputTitle;
    private TextInputEditText inputSubtitle;
    private TextInputEditText inputAbstract;
    private TextInputEditText inputBody;
    private MaterialButton btnUpload;
    private MaterialButton btnCamera;
    private MaterialButton btnCreate;

    private ImageView imgPreview;

    // Image data
    private Uri selectedImageUri = null;
    private String selectedImageBase64 = null; // what we send to the API

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
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraTempUri != null) {
                    selectedImageUri = cameraTempUri;
                    encodeImageToBase64(cameraTempUri);
                } else {
                    Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraNow();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_article_form);

        titleEditText = findViewById(R.id.input_article_title);
        abstractEditText = findViewById(R.id.input_article_abstract);
        categoriesDropdown = findViewById(R.id.categoriesDropdown);
        inputTitle = findViewById(R.id.input_article_title);
        inputSubtitle = findViewById(R.id.input_article_subtitle);
        inputAbstract = findViewById(R.id.input_article_abstract);
        inputBody = findViewById(R.id.input_article_body);

        btnUpload = findViewById(R.id.btnUpload);
        btnCamera = findViewById(R.id.btnCamera);
        btnCreate = findViewById(R.id.btnCreate);

        imgPreview = findViewById(R.id.imagePreview);

        // Dropdown
        String[] categories = new String[]{"National", "Economy", "Sports", "Technology"};
        ArrayAdapter<String> ddAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categories
        );
        categoriesDropdown.setAdapter(ddAdapter);

        btnUpload.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
                launchCameraNow();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Check if we are editing an existing article
        if (getIntent().hasExtra("article")) {
            articleToEdit = (Article) getIntent().getSerializableExtra("article");
            if (articleToEdit != null) {
                // EDIT MODE
                setTitle("Edit Article");
                btnCreate.setText("Update article");
                titleEditText.setText(articleToEdit.getTitleText());
                inputSubtitle.setText(articleToEdit.getFooterText());
                abstractEditText.setText(articleToEdit.getAbstractText());
                inputBody.setText(articleToEdit.getBodyText());
                categoriesDropdown.setText(articleToEdit.getCategory(), false);
                // Set the listener for editing
                btnCreate.setOnClickListener(v -> editArticle(articleToEdit,
                        safeStr(inputTitle),
                        safeStr(inputSubtitle),
                        safeStr(inputAbstract),
                        safeStr(inputBody),
                        categoriesDropdown.getText().toString()));
            }
        } else {
            // CREATE MODE
            setTitle("Create Article");
            btnCreate.setText("Create article");
            categoriesDropdown.setText("National", false);
            // Set the listener for creating
            btnCreate.setOnClickListener(v -> createArticle());
        }
    }

    private void launchCameraNow () {
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
                scaled.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] bytes = bos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                imgPreview.setImageBitmap(scaled);
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

    private void editArticle(Article article, String title, String subtitle, String abs, String body, String category) {
        if (title.isEmpty() || subtitle.isEmpty() || abs.isEmpty() || category.isEmpty() ||  body.isEmpty()) {
            Toast.makeText(this, "Title, subtitle, abstract, body and category are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ny tråd annars krasch
        new Thread(() -> {
            try {
                // Återskapa ModelManager för annars nullpointer
                Properties props = new Properties();
                props.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");
                props.setProperty(ModelManager.ATTR_LOGIN_USER, "DEV_TEAM_09");
                props.setProperty(ModelManager.ATTR_LOGIN_PASS, "654321@09");
                ModelManager mm = new ModelManager(props);
                article.setModelManager(mm);

                // Auto-fyll-i formulär
                article.setTitleText(title);
                article.setFooterText(subtitle);
                article.setAbstractText(abs);
                article.setBodyText(body);
                article.setCategory(category);

                // Bildhantering
                if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
                    article.addImage(selectedImageBase64, "main image");
                }

                // Uppdatera!!!!!
                article.save();

                // Byte av tråd annars breakar toast
                runOnUiThread(() -> {
                    Toast.makeText(this, "Article updated!", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Could not update article: " + e.getMessage(), Toast.LENGTH_LONG).show());
                //Log.d("CreateArticleActivity", "Could not update article: " + e.getMessage());
            }
        }).start();
    }

    private void createArticle() {
        String title = safeStr(inputTitle);
        String subtitle = safeStr(inputSubtitle);
        String abs = safeStr(inputAbstract);
        String body = safeStr(inputBody);
        String category = categoriesDropdown.getText() == null ? "National" : categoriesDropdown.getText().toString();

        if (title.isEmpty() || abs.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Title, Abstract, and Body are required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");
                props.setProperty(ModelManager.ATTR_LOGIN_USER, "DEV_TEAM_09");
                props.setProperty(ModelManager.ATTR_LOGIN_PASS, "654321@09");
                ModelManager mm = new ModelManager(props);

                Article a = new Article(mm, category, title, abs, body, subtitle);

                if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
                    a.addImage(selectedImageBase64, "main image");
                }

                a.save();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Article created!", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Create failed: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private String safeStr(TextInputEditText t) {
        return t.getText() == null ? "" : t.getText().toString().trim();
    }
}
