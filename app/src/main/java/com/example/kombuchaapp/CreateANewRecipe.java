package com.example.kombuchaapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;

import java.util.Objects;

public class CreateANewRecipe extends AppCompatActivity {

    private static final String TAG = "CreateANewRecipe";

    // UI Components
    private EditText etTeaLeaf, etWater, etSugar, etScoby, etKombuchaStarter, etFlavor;
    private Button btnSaveRecipe;
    private ProgressBar progressBar;

    // Repository
    private RecipeRepository recipeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_anew_recipe);

        // Initialize repository
        recipeRepository = new RecipeRepository();

        // Initialize UI components
        initViews();

        // Enable toolbar and back button
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

        // Set up save button
        btnSaveRecipe.setOnClickListener(v -> saveRecipe());
    }

    private void initViews() {
        etTeaLeaf = findViewById(R.id.teaLeaf);
        etWater = findViewById(R.id.water);
        etSugar = findViewById(R.id.sugar);
        etScoby = findViewById(R.id.scoby);
        etKombuchaStarter = findViewById(R.id.kombuchaStarter);
        etFlavor = findViewById(R.id.flavor1);
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe);
        progressBar = findViewById(R.id.progressBar);
    }

    private void saveRecipe() {
        // Get values from input fields
        String teaLeaf = etTeaLeaf.getText().toString().trim();
        String water = etWater.getText().toString().trim();
        String sugar = etSugar.getText().toString().trim();
        String scoby = etScoby.getText().toString().trim();
        String kombuchaStarter = etKombuchaStarter.getText().toString().trim();
        String flavor = etFlavor.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(teaLeaf, water, sugar, scoby, kombuchaStarter)) {
            return;
        }

        // Generate recipe name from current date/time
        String recipeName = "Kombucha Recipe - " + new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());

        // Show loading
        showLoading(true);

        // Create Recipe object
        Recipe recipe = new Recipe(
                null, // userId will be set by repository
                recipeName,
                teaLeaf,
                water,
                sugar,
                scoby,
                kombuchaStarter,
                flavor
        );

        // Save to Firestore
        recipeRepository.createRecipe(recipe, new RecipeRepository.OnRecipeSavedListener() {
            @Override
            public void onSuccess(String message, String recipeId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(CreateANewRecipe.this, 
                            "Recipe saved successfully!", 
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Recipe saved with ID: " + recipeId);
                    
                    // Clear input fields
                    clearInputFields();
                    
                    // Optional: Navigate back to main activity
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(CreateANewRecipe.this, 
                            "Failed to save recipe: " + error, 
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save recipe: " + error);
                });
            }
        });
    }

    private boolean validateInputs(String teaLeaf, String water, String sugar, String scoby, String starter) {
        // Check for empty required fields
        if (TextUtils.isEmpty(teaLeaf)) {
            etTeaLeaf.setError("Tea leaf is required");
            etTeaLeaf.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(water)) {
            etWater.setError("Water amount is required");
            etWater.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(sugar)) {
            etSugar.setError("Sugar amount is required");
            etSugar.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(scoby)) {
            etScoby.setError("SCOBY information is required");
            etScoby.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(starter)) {
            etKombuchaStarter.setError("Kombucha starter is required");
            etKombuchaStarter.requestFocus();
            return false;
        }

        // Flavor is optional for first fermentation
        return true;
    }

    private void clearInputFields() {
        etTeaLeaf.setText("");
        etWater.setText("");
        etSugar.setText("");
        etScoby.setText("");
        etKombuchaStarter.setText("");
        etFlavor.setText("");
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveRecipe.setEnabled(!show);
    }
}
