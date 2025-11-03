package com.example.testingand;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private ArticleAdapter adapter;
    private ArrayList<Article> articleList;

    private static final int DATA_DOWNLOADED = 1;

    private final Handler dataHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == DATA_DOWNLOADED) {
                List<Article> downloadedArticles = (List<Article>) msg.obj;
                articleList.clear();
                articleList.addAll(downloadedArticles);
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_first);

        ListView listView = findViewById(R.id.listView);
        //Button downloadButton = findViewById(R.id.downloadButton);

        articleList = new ArrayList<>();
        adapter = new ArticleAdapter(this, articleList);
        listView.setAdapter(adapter);

        //downloadArticles();
    }

    @Override
    protected void onStart() {
        super.onStart();
        downloadArticles(); // refresh each time activity is visible
    }


    private void downloadArticles() {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");
                // Add credentials if required
                props.setProperty(ModelManager.ATTR_LOGIN_USER, "DEV_TEAM_09");
                props.setProperty(ModelManager.ATTR_LOGIN_PASS, "654321@09");

                ModelManager modelManager = new ModelManager(props);
                List<Article> articles = modelManager.getArticles();

                Message msg = dataHandler.obtainMessage(DATA_DOWNLOADED, articles);
                msg.sendToTarget();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
