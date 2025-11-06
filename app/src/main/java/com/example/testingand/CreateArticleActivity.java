package com.example.testingand;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testingand.exceptions.ServerCommunicationError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
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
    private MaterialButton btnCreate;

    private ImageView imgPreview;

    // Bilddata
    private Uri selectedImageUri = null;
    private String selectedImageBase64 = null; // det vi skickar till API:t
    private String selectedImageMime = "image/png";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedImageUri = uri;
                encodeImageToBase64(uri);
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

        //Dropdown innehåll
        btnUpload = findViewById(R.id.btnUpload);
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

        // Kolla om vi ska ändra en artikel istället för göra ny
        if (getIntent().hasExtra("article")) {
            articleToEdit = (Article) getIntent().getSerializableExtra("article");
            if (articleToEdit != null) {
                // Fyll i formuläret
                btnCreate.setText("Update article");
                titleEditText.setText(articleToEdit.getTitleText());
                inputSubtitle.setText(articleToEdit.getFooterText());
                abstractEditText.setText(articleToEdit.getAbstractText());
                inputBody.setText(articleToEdit.getBodyText());
                categoriesDropdown.setText(articleToEdit.getCategory(), false);
                btnCreate.setOnClickListener(v -> editArticle(articleToEdit,
                        titleEditText.getText().toString(),
                        inputSubtitle.getText().toString(),
                        abstractEditText.getText().toString(),
                        inputBody.getText().toString(),
                        categoriesDropdown.getText().toString()));
            }
        } else {
            btnCreate.setText("Create article");
        }
        categoriesDropdown.setText("National", false);

        btnUpload.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnCreate.setOnClickListener(v -> createArticle());
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

                // Nedskalning
                Bitmap scaled = scaleBitmapIfLarge(original, 1600);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] bytes = bos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                selectedImageMime = "image/png";


                imgPreview.setImageBitmap(scaled);

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Image read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Hjälpmetod för att nedskala stor bild */
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

    private void editArticle(Article article, String title, String subtitle, String abs, String body, String category){
       article.setTitleText(title);
       article.setFooterText(subtitle);
       article.setAbstractText(abs);
       article.setBodyText(body);
       article.setCategory(category);
        try {
            article.save();
            Toast.makeText(this, "Article updated!", Toast.LENGTH_LONG).show();
        } catch (ServerCommunicationError e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not save article: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createArticle() {
        String title = safeStr(inputTitle);
        String subtitle = safeStr(inputSubtitle);
        String abs = safeStr(inputAbstract);
        String body = safeStr(inputBody);
        String category = categoriesDropdown.getText() == null ? "National" : categoriesDropdown.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (abs.isEmpty()) {
            Toast.makeText(this, "Abstract is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (body.isEmpty()) {
            Toast.makeText(this, "Body is required", Toast.LENGTH_SHORT).show();
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

                // Koppla bild om vald
                if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
                    Image img = new Image(mm, 1, "main image", -1, selectedImageBase64);
                    a.setImage(img);
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
