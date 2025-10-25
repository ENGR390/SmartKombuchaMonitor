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

    Button newRecipeButton, settingsButton, logoutButton, myRecipesButton, sensorReadingsButton;
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

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        fAuth = FirebaseAuth.getInstance();

        myRecipesButton = findViewById(R.id.myRecipesButton);
        newRecipeButton = findViewById(R.id.NewRecipeButton);
        settingsButton = findViewById(R.id.SettingsButton);
        logoutButton = findViewById(R.id.LogoutButton);
        sensorReadingsButton = findViewById(R.id.sensorReadingsButton);

        myRecipesButton.setOnClickListener(v -> {
            //Toast.makeText(this, "Button clicked! Event logged.", Toast.LENGTH_SHORT).show();
            //logButtonClickEvent();

            Intent i = new Intent(MainActivity.this, HomePage_activity.class);
            startActivity(i);
        });

        newRecipeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateANewRecipe.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        sensorReadingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllSensorReadingsActivity.class);
            startActivity(intent);
        });


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
