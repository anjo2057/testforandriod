package com.example.testingand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlanetAdapter extends ArrayAdapter<Planet> {

    private final ExecutorService executor = Executors.newFixedThreadPool(5); // Thread pool for image loading
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PlanetAdapter(@NonNull Context context, @NonNull List<Planet> planets) {
        super(context, 0, planets);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Planet planet = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_planet, parent, false);
        }

        // Lookup view for data population
        TextView tvName = convertView.findViewById(R.id.planetNameTextView);
        TextView tvDistance = convertView.findViewById(R.id.planetDistanceTextView);
        TextView tvGravity = convertView.findViewById(R.id.planetGravityTextView);
        ImageView ivPlanet = convertView.findViewById(R.id.planetImageView);

        // Populate the data into the template view using the data object
        tvName.setText(planet.getName());
        tvDistance.setText("Distance from sun: " + planet.getDistance() + " million km");
        tvGravity.setText("Gravity: " + planet.getGravity() + " m/s²");

        // Set a placeholder image while the real one loads
        ivPlanet.setImageResource(R.drawable.ic_launcher_background); 

        // Load image in the background
        executor.execute(() -> {
            try {
                InputStream in = new URL(planet.getImage()).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                handler.post(() -> {
                    ivPlanet.setImageBitmap(bitmap);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Return the completed view to render on screen
        return convertView;
    }
}