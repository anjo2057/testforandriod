package com.example.testingand;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testingand.exceptions.ServerCommunicationError;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleAdapter extends BaseAdapter {

    private final Context context;
    private final List<Article> articleList;

    private boolean isLoggedIn = false;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ArticleAdapter(Context context, List<Article> articleList) {
        this.context = context;
        this.articleList = articleList;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return articleList.size();
    }

    @Override
    public Object getItem(int position) {
        return articleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_article, parent, false);
        }

        Article article = articleList.get(position);

        TextView titleText = convertView.findViewById(R.id.articleTitle);
        TextView descText = convertView.findViewById(R.id.articleAbstract);
        TextView categoryText = convertView.findViewById(R.id.articleCategory);
        ImageView ivArticle = convertView.findViewById(R.id.articleImageView);
        Button editButton = convertView.findViewById(R.id.editButton);
        Button deleteButton = convertView.findViewById(R.id.deleteButton);

        // Set a placeholder image while the real one loads
        ivArticle.setImageResource(R.drawable.ic_launcher_background);

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
                            ivArticle.setImageBitmap(decodedByte);
                        });
                    }
                }
            } catch (ServerCommunicationError e) {
                e.printStackTrace();
            }
        });

        titleText.setText(article.getTitleText());
        descText.setText(article.getAbstractText());
        categoryText.setText(article.getCategory());

        if (isLoggedIn) {
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);

            // Trigger delete
            deleteButton.setOnClickListener(v -> {
                executor.execute(() -> {
                    try {
                        article.delete();
                        handler.post(() -> {
                            articleList.remove(article);
                            notifyDataSetChanged();
                            Toast.makeText(context, "Article deleted", Toast.LENGTH_SHORT).show();
                        });
                    } catch (ServerCommunicationError e) {
                        e.printStackTrace();
                        handler.post(() -> {
                            Toast.makeText(context, "Failed to delete article", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });

            // Trigger edit
            editButton.setOnClickListener(v -> {
                // Intent lagrar info för nästa komponent så ni vet
                Intent intent = new Intent(context, CreateArticleActivity.class);
                intent.putExtra("article", article);
                context.startActivity(intent);
            });

        } else {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }

        return convertView;
    }

}
