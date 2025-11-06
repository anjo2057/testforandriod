package com.example.testingand;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

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

    @Override public int getCount() { return articleList.size(); }
    @Override public Object getItem(int position) { return articleList.get(position); }
    @Override public long getItemId(int position) { return position; }

    static class ViewHolder {
        ImageView ivArticle;
        TextView titleText;
        TextView descText;
        TextView categoryText;
        TextView updatedText;
        View adminBar;
        Button editButton;
        Button deleteButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_article, parent, false);
            h = new ViewHolder();
            h.titleText = convertView.findViewById(R.id.articleTitle);
            h.descText = convertView.findViewById(R.id.articleAbstract);
            h.categoryText = convertView.findViewById(R.id.articleCategory);
            h.updatedText = convertView.findViewById(R.id.articleUpdated);
            h.ivArticle = convertView.findViewById(R.id.articleImageView);
            h.adminBar    = convertView.findViewById(R.id.adminBar);
            h.editButton = convertView.findViewById(R.id.editButton);
            h.deleteButton = convertView.findViewById(R.id.deleteButton);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        final Article article = articleList.get(position);

        // ====== TEXT ======
        h.titleText.setText(article.getTitleText());
        h.categoryText.setText(article.getCategory());

        // HTML ➜ läsbar text (hanterar även dubbel-escape)
        String raw = article.getAbstractText();
        CharSequence spanned = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY);
        String once = spanned.toString();
        if (once.contains("&lt;") || once.contains("&gt;")) {
            spanned = HtmlCompat.fromHtml(once, HtmlCompat.FROM_HTML_MODE_LEGACY);
        }
        h.descText.setText(spanned);
        // Om du vill ha klickbara länkar i abstract:
        // h.descText.setMovementMethod(LinkMovementMethod.getInstance());

        // Visa "senast uppdaterad" (eller skapad) i list-raden
        String when = article.getUpdatedAtDisplay(); // "yyyy-MM-dd HH:mm" eller "–"
        if (h.updatedText != null) {
            if (when == null || when.trim().isEmpty() || "–".equals(when)) {
                h.updatedText.setVisibility(View.GONE); // dölj om vi inte har datum
            } else {
                h.updatedText.setText(when);
                h.updatedText.setVisibility(View.VISIBLE);
            }
        }


        // ====== BILD ======
        // Placeholder direkt och tagga imageView med aktuell artikel
        h.ivArticle.setImageResource(R.drawable.ic_launcher_background);
        h.ivArticle.setTag(article);

        executor.execute(() -> {
            try {
                Image image = article.getImage();
                if (image != null) {
                    String b64Image = image.getImage();
                    if (b64Image != null && !b64Image.isEmpty()) {
                        byte[] decoded = Base64.decode(b64Image, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        handler.post(() -> {
                            // Sätt endast om raden fortfarande visar samma artikel
                            Object tag = h.ivArticle.getTag();
                            if (tag == article) {
                                h.ivArticle.setImageBitmap(bmp);
                            }
                        });
                    }
                }
            } catch (ServerCommunicationError e) {
                e.printStackTrace();
            }
        });

        // ====== ADMIN-KNAPPAR ======
        if (isLoggedIn) {
            h.adminBar.setVisibility(View.VISIBLE);
            h.editButton.setVisibility(View.VISIBLE);
            h.deleteButton.setVisibility(View.VISIBLE);

            h.deleteButton.setOnClickListener(v -> {
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
                        handler.post(() ->
                                Toast.makeText(context, "Failed to delete article", Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            });

            h.editButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, CreateArticleActivity.class);
                intent.putExtra("article", article);
                context.startActivity(intent);
            });

        } else {
            h.editButton.setVisibility(View.GONE);
            h.deleteButton.setVisibility(View.GONE);
            h.editButton.setOnClickListener(null);
            h.deleteButton.setOnClickListener(null);
        }

        return convertView;
    }
}
