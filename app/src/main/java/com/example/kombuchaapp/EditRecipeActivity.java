package com.example.kombuchaapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;

import java.util.Objects;

public class EditRecipeActivity extends AppCompatActivity {

    private static final String TAG = "EditRecipeActivity";

    // UI Components
    private EditText etTeaLeaf, etWater, etSugar, etScoby, etKombuchaStarter, etFlavor, etRecipeName;
    private Button btnUpdateRecipe;
    private ProgressBar progressBar;

    // Repository and data
    private RecipeRepository recipeRepository;
    private String recipeId;
    private Recipe currentRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        // Get recipe ID from intent
        recipeId = getIntent().getStringExtra("recipe_id");
        if (recipeId == null) {
            Toast.makeText(this, "Error: No recipe ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        // Load recipe data
        loadRecipe();

        // Set up update button
        btnUpdateRecipe.setOnClickListener(v -> updateRecipe());
    }

    private void initViews() {
        etTeaLeaf = findViewById(R.id.teaLeaf);
        etWater = findViewById(R.id.water);
        etSugar = findViewById(R.id.sugar);
        etScoby = findViewById(R.id.scoby);
        etKombuchaStarter = findViewById(R.id.kombuchaStarter);
        etFlavor = findViewById(R.id.flavor1);
        btnUpdateRecipe = findViewById(R.id.btnUpdateRecipe);
        progressBar = findViewById(R.id.progressBar);
        etRecipeName = findViewById(R.id.RecipeNameEditText);
    }

    private void loadRecipe() {
        showLoading(true);

        recipeRepository.getRecipeById(recipeId, new RecipeRepository.OnRecipeLoadedListener() {
            @Override
            public void onSuccess(Recipe recipe) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentRecipe = recipe;
                    populateFields(recipe);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(EditRecipeActivity.this,
                            "Error loading recipe: " + error,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load recipe: " + error);
                    finish();
                });
            }
        });
    }

    private void populateFields(Recipe recipe) {
        etRecipeName.setText(recipe.getRecipeName());
        etTeaLeaf.setText(recipe.getTeaLeaf());
        etWater.setText(recipe.getWater());
        etSugar.setText(recipe.getSugar());
        etScoby.setText(recipe.getScoby());
        etKombuchaStarter.setText(recipe.getKombuchaStarter());
        etFlavor.setText(recipe.getFlavor());
    }

    private void updateRecipe() {
        // Get values from input fields
        String recipeName = etRecipeName.getText().toString().trim();
        String teaLeaf = etTeaLeaf.getText().toString().trim();
        String water = etWater.getText().toString().trim();
        String sugar = etSugar.getText().toString().trim();
        String scoby = etScoby.getText().toString().trim();
        String kombuchaStarter = etKombuchaStarter.getText().toString().trim();
        String flavor = etFlavor.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(recipeName, teaLeaf, water, sugar, scoby, kombuchaStarter)) {
            return;
        }

        // Show loading
        showLoading(true);

        // Update the current recipe object
        currentRecipe.setRecipeName(recipeName);
        currentRecipe.setTeaLeaf(teaLeaf);
        currentRecipe.setWater(water);
        currentRecipe.setSugar(sugar);
        currentRecipe.setScoby(scoby);
        currentRecipe.setKombuchaStarter(kombuchaStarter);
        currentRecipe.setFlavor(flavor);

        // Update in Firestore
        recipeRepository.updateRecipe(recipeId, currentRecipe, new RecipeRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(EditRecipeActivity.this,
                            "Recipe updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Recipe updated: " + recipeId);
                    finish(); // Return to previous activity
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(EditRecipeActivity.this,
                            "Failed to update recipe: " + error,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update recipe: " + error);
                });
            }
        });
    }

    private boolean validateInputs(String recipeName, String teaLeaf, String water,
                                   String sugar, String scoby, String starter) {
        if (TextUtils.isEmpty(recipeName)) {
            etRecipeName.setError("Recipe name is required");
            etRecipeName.requestFocus();
            return false;
        }

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

        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnUpdateRecipe.setEnabled(!show);
    }
}
