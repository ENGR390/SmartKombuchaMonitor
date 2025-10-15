package com.example.kombuchaapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Get reference to your button
        Button myButton = findViewById(R.id.my_button);

        // Set click listener
        myButton.setOnClickListener(v -> {
            // Add visual confirmation
            Toast.makeText(this, "Button clicked! Event logged.", Toast.LENGTH_SHORT).show();

            // Log the button click event
            logButtonClickEvent();
        });
    }

    private void logButtonClickEvent() {
        // Create a bundle to add parameters
        Bundle params = new Bundle();
        params.putString("button_name", "my_button");
        params.putString("screen_name", "main_screen");

        // Add logging to verify it's working
        Log.d("FirebaseAnalytics", "Logging event: button_clicked");

        // Log the event
        mFirebaseAnalytics.logEvent("button_clicked", params);
    }
}
