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
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    Button newRecipeButton, settingsButton, logoutButton;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth fAuth;
    
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

        // Initialize Firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        fAuth = FirebaseAuth.getInstance();

        // Get reference to buttons
        Button myButton = findViewById(R.id.my_button);
        newRecipeButton = findViewById(R.id.NewRecipeButton);
        settingsButton = findViewById(R.id.SettingsButton);
        logoutButton = findViewById(R.id.LogoutButton);

        // Analytics test button
        myButton.setOnClickListener(v -> {
            Toast.makeText(this, "Button clicked! Event logged.", Toast.LENGTH_SHORT).show();
            logButtonClickEvent();
        });

        // New Recipe button
        newRecipeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateANewRecipe.class);
            startActivity(intent);
        });

        // Settings button - NEW
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Logout button - NEW
        logoutButton.setOnClickListener(v -> {
            logout();
        });
    }

    private void logButtonClickEvent() {
        Bundle params = new Bundle();
        params.putString("button_name", "my_button");
        params.putString("screen_name", "main_screen");
        Log.d("FirebaseAnalytics", "Logging event: button_clicked");
        mFirebaseAnalytics.logEvent("button_clicked", params);
    }

    private void logout() {
        fAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
}
