package com.example.kombuchaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeDeletedListener {

    private static final String TAG = "MainActivity";

    Button newRecipeButton, logoutButton, sensorReadingsButton;
    ImageButton settingsButton;
    RecyclerView recipesRecyclerView;
    TextView emptyStateText;
    ProgressBar progressBar;

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth fAuth;
    private RecipeRepository recipeRepository;
    private RecipeAdapter recipeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        fAuth = FirebaseAuth.getInstance();
        recipeRepository = new RecipeRepository();

        // Initialize existing buttons
        newRecipeButton = findViewById(R.id.NewRecipeButton);
        settingsButton = findViewById(R.id.SettingsButton);
        logoutButton = findViewById(R.id.LogoutButton);


        // Initialize new recipe list components
        recipesRecyclerView = findViewById(R.id.recipesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        setupRecyclerView();

        // Load recipes
        loadRecipes();

        //Existing button listeners
        newRecipeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateANewRecipe.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            showSettingsMenu(v);
        });



        logoutButton.setOnClickListener(v -> {
            logout();
        });
    }

    private void showSettingsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.settings_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_settings) {
                // Navigate to settings
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.menu_logout) {
                // Logout
                logout();
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload recipes when returning to this activity
        loadRecipes();
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(this, this);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipesRecyclerView.setAdapter(recipeAdapter);
    }

    private void loadRecipes() {
        showLoading(true);

        recipeRepository.getAllRecipes(new RecipeRepository.OnRecipesLoadedListener() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                runOnUiThread(() -> {
                    showLoading(false);

                    if (recipes.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        recipeAdapter.setRecipes(recipes);
                    }

                    Log.d(TAG, "Loaded " + recipes.size() + " recipes");
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyState(true);
                    Toast.makeText(MainActivity.this,
                            "Error loading recipes: " + error,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load recipes: " + error);
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recipesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateText.setVisibility(View.VISIBLE);
            recipesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recipesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRecipeDeleted() {
        // Callback from adapter when a recipe is deleted
        loadRecipes(); // Reload the list
    }

    private void logout() {
        fAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
}