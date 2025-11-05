package com.example.testingand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.text.HtmlCompat;
import com.example.testingand.exceptions.ServerCommunicationError;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleAdapter extends BaseAdapter {

    private final Context context;
    private final List<Article> articleList;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ArticleAdapter(Context context, List<Article> articleList) {
        this.context = context;
        this.articleList = articleList;
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

    static class ViewHolder {
        ImageView ivArticle;
        TextView titleText;
        TextView descText;
        TextView categoryText;
        TextView updatedText;
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
            h.ivArticle = convertView.findViewById(R.id.articleImageView);
            h.updatedText = convertView.findViewById(R.id.articleUpdated);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        // TODO: make clickable to new page

        Article article = articleList.get(position);

        h.titleText.setText(article.getTitleText());
        h.categoryText.setText(article.getCategory());

        String raw = article.getAbstractText();
        CharSequence spanned = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY);
        String once = spanned.toString();
        if (once.contains("&lt;") || once.contains("&gt")) {
            spanned = HtmlCompat.fromHtml(once, HtmlCompat.FROM_HTML_MODE_LEGACY);
        }

        h.descText.setText(spanned);

        if (h.updatedText != null) {
            h.updatedText.setText(article.getUpdatedAtDisplay());
        }

        h.ivArticle.setImageResource(R.drawable.ic_launcher_background);
        h.ivArticle.setTag(article);


        //TextView titleText = convertView.findViewById(R.id.articleTitle);
        //TextView descText = convertView.findViewById(R.id.articleAbstract);
        //TextView categoryText = convertView.findViewById(R.id.articleCategory);
        //ImageView ivArticle = convertView.findViewById(R.id.articleImageView);


        // Set a placeholder image while the real one loads
        //ivArticle.setImageResource(R.drawable.ic_launcher_background);

        // Load image in the background
        executor.execute(() -> {
            try {
                Image image = article.getImage();
                if (image != null) {
                    String b64Image = image.getImage();
                    if (b64Image != null && !b64Image.isEmpty()) {
                        byte[] decoded = Base64.decode(b64Image, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        handler.post(() -> {
                            // Kontrollera att vyn fortfarande visar samma artikel
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

        return convertView;
    }
}

        //titleText.setText(article.getTitleText());
        //descText.setText(article.getAbstractText());
        //categoryText.setText(article.getCategory());
