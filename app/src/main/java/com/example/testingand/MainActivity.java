package com.example.testingand;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PlanetAdapter adapter;
    private ArrayList<Planet> planetsList;
    private static final String PLANETS_URL = "http://sanger.dia.fi.upm.es/pmd-task/public/list-example/planets.php";

    private final Handler dataHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == DownloadPlanetsThread.DATA_DOWNLOADED) {
                List<Planet> downloadedPlanets = (List<Planet>) msg.obj;
                planetsList.clear();
                planetsList.addAll(downloadedPlanets);
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_first);

        ListView listView = findViewById(R.id.listView);
        Button downloadButton = findViewById(R.id.downloadButton);

        planetsList = new ArrayList<>();
        adapter = new PlanetAdapter(this, planetsList);
        listView.setAdapter(adapter);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadPlanetsThread downloadRunnable = new DownloadPlanetsThread(PLANETS_URL, dataHandler);
                new Thread(downloadRunnable).start();
            }
        });
    }
}