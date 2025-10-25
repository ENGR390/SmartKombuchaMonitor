package com.example.kombuchaapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kombuchaapp.models.SensorReadings;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AllSensorReadingsActivity extends AppCompatActivity {

    private static final String TAG = "AllSensorReadings";
    private ListView listView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private FirebaseFirestore db;
    private List<SensorReadings> sensorReadingsList;
    private ArrayAdapter<SensorReadings> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_sensor_readings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        sensorReadingsList = new ArrayList<>();

        // Initialize views
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);

        // Setup ListView with adapter that uses SensorReadings objects directly
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2,
                android.R.id.text1, sensorReadingsList);
        listView.setAdapter(adapter);

        // Load data from Firestore
        loadSensorReadings();
    }

    private void loadSensorReadings() {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        db.collection("temperature_readings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    sensorReadingsList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String sensorId = document.getString("sensor_id");
                            float tempC = document.getDouble("temperature_c") != null
                                    ? document.getDouble("temperature_c").floatValue() : 0f;
                            float tempF = document.getDouble("temperature_f") != null
                                    ? document.getDouble("temperature_f").floatValue() : 0f;

                            SensorReadings reading = new SensorReadings(sensorId, tempC, tempF);
                            sensorReadingsList.add(reading);

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document: " + document.getId(), e);
                        }
                    }

                    if (sensorReadingsList.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                    } else {
                        listView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading sensor readings", e);
                    Toast.makeText(this, "Error loading data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}