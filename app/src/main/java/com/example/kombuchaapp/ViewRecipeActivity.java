package com.example.kombuchaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ViewRecipeActivity extends AppCompatActivity {

    private static final String TAG = "ViewRecipeActivity";

    // UI Components
    private TextView tvRecipeName, tvStatus, tvTeaLeaf, tvWater, tvSugar, tvScoby,
            tvKombuchaStarter, tvFlavor, tvCreatedDate, tvBrewingStartDate,
            tvCompletionDate, tvNotes;
    private Button btnEdit, btnStartBrewing, btnMarkCompleted;
    private ProgressBar progressBar;
    private View notesSection, flavorSection;

    // Repository
    private RecipeRepository recipeRepository;
    private String recipeId;
    private Recipe currentRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        // Get recipe ID from intent
        recipeId = getIntent().getStringExtra("recipe_id");
        if (recipeId == null) {
            Toast.makeText(this, "Error: No recipe ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipeRepository = new RecipeRepository();

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        initViews();

        // Load recipe data
        loadRecipe();

        // Setup button listeners
        setupButtons();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        tvRecipeName = findViewById(R.id.tv_recipe_name);
        tvStatus = findViewById(R.id.tv_status);
        tvTeaLeaf = findViewById(R.id.tv_tea_leaf);
        tvWater = findViewById(R.id.tv_water);
        tvSugar = findViewById(R.id.tv_sugar);
        tvScoby = findViewById(R.id.tv_scoby);
        tvKombuchaStarter = findViewById(R.id.tv_kombucha_starter);
        tvFlavor = findViewById(R.id.tv_flavor);
        tvCreatedDate = findViewById(R.id.tv_created_date);
        tvBrewingStartDate = findViewById(R.id.tv_brewing_start_date);
        tvCompletionDate = findViewById(R.id.tv_completion_date);
        tvNotes = findViewById(R.id.tv_notes);

        btnEdit = findViewById(R.id.btn_edit);
        btnStartBrewing = findViewById(R.id.btn_start_brewing);
        btnMarkCompleted = findViewById(R.id.btn_mark_completed);

        notesSection = findViewById(R.id.notes_section);
        flavorSection = findViewById(R.id.flavor_section);
    }

    private void loadRecipe() {
        showLoading(true);

        recipeRepository.getRecipeById(recipeId, new RecipeRepository.OnRecipeLoadedListener() {
            @Override
            public void onSuccess(Recipe recipe) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentRecipe = recipe;
                    displayRecipe(recipe);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ViewRecipeActivity.this,
                            "Error loading recipe: " + error,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load recipe: " + error);
                    finish();
                });
            }
        });
    }

    private void displayRecipe(Recipe recipe) {
        // Recipe name
        tvRecipeName.setText(recipe.getRecipeName() != null ? recipe.getRecipeName() : "Unnamed Recipe");

        // Status
        String status = recipe.getStatus() != null ? recipe.getStatus() : "draft";
        tvStatus.setText(status.toUpperCase());

        // First fermentation ingredients
        tvTeaLeaf.setText(recipe.getTeaLeaf() != null ? recipe.getTeaLeaf() : "N/A");
        tvWater.setText(recipe.getWater() != null ? recipe.getWater() : "N/A");
        tvSugar.setText(recipe.getSugar() != null ? recipe.getSugar() : "N/A");
        tvScoby.setText(recipe.getScoby() != null ? recipe.getScoby() : "N/A");
        tvKombuchaStarter.setText(recipe.getKombuchaStarter() != null ? recipe.getKombuchaStarter() : "N/A");

        // Second fermentation (flavor)
        if (recipe.getFlavor() != null && !recipe.getFlavor().trim().isEmpty()) {
            flavorSection.setVisibility(View.VISIBLE);
            tvFlavor.setText(recipe.getFlavor());
        } else {
            flavorSection.setVisibility(View.GONE);
        }

        // Dates
        tvCreatedDate.setText(formatDate(recipe.getCreatedDate()));
        tvBrewingStartDate.setText(formatDate(recipe.getBrewingStartDate()));
        tvCompletionDate.setText(formatDate(recipe.getCompletionDate()));

        // Notes
        if (recipe.getNotes() != null && !recipe.getNotes().trim().isEmpty()) {
            notesSection.setVisibility(View.VISIBLE);
            tvNotes.setText(recipe.getNotes());
        } else {
            notesSection.setVisibility(View.GONE);
        }

        // Update button visibility based on status
        updateButtonsForStatus(status);
    }

    private void updateButtonsForStatus(String status) {
        switch (status.toLowerCase()) {
            case "draft":
                btnStartBrewing.setVisibility(View.VISIBLE);
                btnMarkCompleted.setVisibility(View.GONE);
                break;
            case "brewing":
                btnStartBrewing.setVisibility(View.GONE);
                btnMarkCompleted.setVisibility(View.VISIBLE);
                break;
            case "completed":
                btnStartBrewing.setVisibility(View.GONE);
                btnMarkCompleted.setVisibility(View.GONE);
                break;
        }
    }

    private void setupButtons() {
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(ViewRecipeActivity.this, EditRecipeActivity.class);
            intent.putExtra("recipe_id", recipeId);
            startActivity(intent);
        });

        btnStartBrewing.setOnClickListener(v -> updateRecipeStatus("brewing"));
        btnMarkCompleted.setOnClickListener(v -> updateRecipeStatus("completed"));
    }

    private void updateRecipeStatus(String newStatus) {
        showLoading(true);

        recipeRepository.updateRecipeStatus(recipeId, newStatus, new RecipeRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ViewRecipeActivity.this, message, Toast.LENGTH_SHORT).show();
                    loadRecipe(); // Reload to update UI
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ViewRecipeActivity.this,
                            "Failed to update status: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload recipe when returning from edit
        if (recipeId != null) {
            loadRecipe();
        }
    }
}
