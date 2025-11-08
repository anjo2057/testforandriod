package com.example.testingand;

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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.example.testingand.exceptions.ServerCommunicationError;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView title;
    private TextView meta;
    private TextView subtitle; //footer
    private TextView articleAbstract;
    private TextView body;
    private ImageView imageView;

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

        Article article = (Article) getIntent().getSerializableExtra("article");
        if (article == null) { finish(); return; }

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
