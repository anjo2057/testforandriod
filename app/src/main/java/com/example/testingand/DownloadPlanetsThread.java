package com.example.testingand;

import android.os.Handler;
import android.os.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Arrays;
import java.util.List;

public class DownloadPlanetsThread implements Runnable {

    public static final int DATA_DOWNLOADED = 1;
    private final String url;
    private final Handler dataHandler;

    public DownloadPlanetsThread(String url, Handler dataHandler) {
        this.url = url;
        this.dataHandler = dataHandler;
    }

    @Override
    public void run() {
        try {
            String jsonResponse = NetUtils.getURLText(url);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            List<Planet> planets = Arrays.asList(gson.fromJson(jsonResponse, Planet[].class));

            Message message = new Message();
            message.what = DATA_DOWNLOADED;
            message.obj = planets;
            dataHandler.sendMessage(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}