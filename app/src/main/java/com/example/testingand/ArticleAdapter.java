package com.example.testingand;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ArticleAdapter extends BaseAdapter {

    private final Context context;
    private final List<Article> articleList;

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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_article, parent, false);
        }

        // TODO: make clickable to new page

        Article article = articleList.get(position);

        // TODO: add thumbnail
        // TODO: add category

        TextView titleText = convertView.findViewById(R.id.articleTitle);
        TextView descText = convertView.findViewById(R.id.articleDescription);

        titleText.setText(article.getTitleText());
        descText.setText(article.getAbstractText());

        return convertView;
    }
}
