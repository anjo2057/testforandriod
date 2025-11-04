package com.example.testingand;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testingand.exceptions.ServerCommunicationError;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        TextView title = findViewById(R.id.article_title);
        TextView articleAbstract = findViewById(R.id.article_abstract);
        TextView body = findViewById(R.id.article_body);
        ImageView imageView = findViewById(R.id.article_image);

        Article article = (Article) getIntent().getSerializableExtra("article");

        if (article != null) {
            title.setText(article.getTitleText());
            articleAbstract.setText(article.getAbstractText());
            body.setText(article.getBodyText());

            // Set a placeholder image while the real one loads
            imageView.setImageResource(R.drawable.ic_launcher_background);

            // Load image in the background
            executor.execute(() -> {
                try {
                    final Image image = article.getImage();
                    if (image != null) {
                        final String b64Image = image.getImage();
                        if (b64Image != null && !b64Image.isEmpty()) {
                            byte[] decodedString = Base64.decode(b64Image, Base64.DEFAULT);
                            final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            handler.post(() -> {
                                imageView.setImageBitmap(decodedByte);
                            });
                        }
                    }
                } catch (ServerCommunicationError e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
