package com.example.testingand;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private ArticleAdapter adapter;

    //All data som hämtas
    private final ArrayList<Article> allArticles = new ArrayList<>();
    // Det som visas
    private final ArrayList<Article> visibleArticles = new ArrayList<>();

    private ListView listView;
    private Button loginButton;

    private Button createButton;

    private boolean isLoggedIn = false;

    private TextInputLayout categoriesLayout;
    private AutoCompleteTextView categoriesDropdown;

    private static final int DATA_DOWNLOADED = 1;

    private final Handler dataHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == DATA_DOWNLOADED) {
                List<Article> downloadedArticles = (List<Article>) msg.obj;
                onArticlesLoaded(downloadedArticles);
                setLoading(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_first);

        listView = findViewById(R.id.listView);
        loginButton = findViewById(R.id.loginButton);
        createButton = findViewById(R.id.createButton);
        categoriesLayout = findViewById(R.id.categoriesLayout);
        categoriesDropdown = findViewById(R.id.categoriesDropdown);

        adapter = new ArticleAdapter(this, visibleArticles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Article article = visibleArticles.get(position);
            Intent intent = new Intent(MainActivity.this, ArticleActivity.class);
            intent.putExtra("article", article);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            isLoggedIn = !isLoggedIn;
            updateUiForLoginState();
        });

        createButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateArticleActivity.class);
            startActivity(intent);
        });

        //Dropdown innehåll
        String[] categories = new String[]{"All", "National", "Economy", "Sports", "Technology"};
        ArrayAdapter<String> ddAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categories
        );
        categoriesDropdown.setAdapter(ddAdapter);
        categoriesDropdown.setText("All", false);

        categoriesDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            applyFilter(selected);
        });

        applyFilter("All");
        updateUiForLoginState();
    }

    private void updateUiForLoginState() {
        if (isLoggedIn) {
            loginButton.setText("Log out");
            createButton.setVisibility(View.VISIBLE);
        } else {
            loginButton.setText("Log in");
            createButton.setVisibility(View.GONE);
        }
        if (adapter != null) {
            adapter.setLoggedIn(isLoggedIn);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setLoading(true);
        downloadArticles();
    }

    private void downloadArticles() {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");

                props.setProperty(ModelManager.ATTR_LOGIN_USER, "DEV_TEAM_09");
                props.setProperty(ModelManager.ATTR_LOGIN_PASS, "654321@09");

                ModelManager modelManager = new ModelManager(props);
                List<Article> articles = modelManager.getArticles();

                Message msg = dataHandler.obtainMessage(DATA_DOWNLOADED, articles);
                msg.sendToTarget();

            } catch (Exception e) {
                e.printStackTrace();
                Message msg = dataHandler.obtainMessage(DATA_DOWNLOADED, null);
                msg.sendToTarget();
            }
        }).start();
    }

    private void onArticlesLoaded(List<Article> downloaded) {
        allArticles.clear();
        if (downloaded != null) allArticles.addAll(downloaded);

        String current = categoriesDropdown.getText() == null
                ? "All"
                : categoriesDropdown.getText().toString();

        if (current.isEmpty()) current = "All";
        applyFilter(current);
    }

    private void applyFilter(String category) {
        visibleArticles.clear();

        if ("All".equalsIgnoreCase(category)) {
            visibleArticles.addAll(allArticles);
        } else {
            for (Article a : allArticles) {
                String cat = a.getCategory();
                if (cat != null && cat.equalsIgnoreCase(category)) {
                    visibleArticles.add(a);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        if (loading) {
            loginButton.setText("Loading...");
        } else {
            loginButton.setText(isLoggedIn ? "Log out" : "Log in");
        }
    }
}
