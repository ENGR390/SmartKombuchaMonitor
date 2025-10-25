package com.example.kombuchaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    Button appButton;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Get reference to your button
        Button myButton = findViewById(R.id.my_button);

        // Set click listener
        myButton.setOnClickListener(v -> {
            // Visual confirmation
            Toast.makeText(this, "Button clicked! Event logged.", Toast.LENGTH_SHORT).show();

            // Log the button click event
            logButtonClickEvent();

            Intent i = new Intent(MainActivity.this, HomePage_activity.class);
            startActivity(i);
        });

        // Settings button
        appButton = findViewById(R.id.NewRecipeButton);
        appButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateANewRecipe.class);
            startActivity(intent);
        });
    }

    private void logButtonClickEvent() {
        Bundle params = new Bundle();
        params.putString("button_name", "my_button");
        params.putString("screen_name", "main_screen");
        Log.d("FirebaseAnalytics", "Logging event: button_clicked");
        mFirebaseAnalytics.logEvent("button_clicked", params);
    }
}
