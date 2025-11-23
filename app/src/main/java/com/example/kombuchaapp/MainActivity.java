package com.example.kombuchaapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeDeletedListener {

    private static final String TAG = "MainActivity";

    Button newRecipeButton, logoutButton, myBrewButton, discoverButton;
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        fAuth = FirebaseAuth.getInstance();
        recipeRepository = new RecipeRepository();

        // Initialize existing buttons
        newRecipeButton = findViewById(R.id.NewRecipeButton);
        settingsButton = findViewById(R.id.SettingsButton);
        logoutButton = findViewById(R.id.LogoutButton);
        myBrewButton = findViewById(R.id.myBrewButton);
        discoverButton = findViewById(R.id.discoverButton);

        // Initialize new recipe list components
        recipesRecyclerView = findViewById(R.id.recipesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        setupRecyclerView();

        // Load recipes
        loadMyRecipes();

        //Existing button listeners
        newRecipeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateANewRecipe.class);
            FizzTransitionUtil.play(MainActivity.this, () -> startActivity(intent));
        });

        settingsButton.setOnClickListener(v -> {
            showSettingsMenu(v);
        });

        logoutButton.setOnClickListener(v -> {
            logout();
        });

        myBrewButton.setOnClickListener(v -> {
            recipeAdapter.setDiscoverMode(false);
            loadMyRecipes();
            myBrewButton.setTypeface(null, android.graphics.Typeface.BOLD);
            discoverButton.setTypeface(null, android.graphics.Typeface.NORMAL);
            // Show create button in My Brews mode
            newRecipeButton.setVisibility(View.VISIBLE);
        });
        discoverButton.setOnClickListener(v -> {
            recipeAdapter.setDiscoverMode(true);
            loadDiscoverRecipes();
            myBrewButton.setTypeface(null, android.graphics.Typeface.NORMAL);
            discoverButton.setTypeface(null, android.graphics.Typeface.BOLD);
            // Hide create button in Discover mode
            newRecipeButton.setVisibility(View.GONE);
        });
    }

    private void showSettingsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.settings_menu, popup.getMenu());

        // Make delete account item red
        MenuItem deleteItem = popup.getMenu().findItem(R.id.menu_delete_account);
        SpannableString redTitle = new SpannableString(deleteItem.getTitle());
        redTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.delete_red)), 0, redTitle.length(), 0);
        deleteItem.setTitle(redTitle);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_settings) {
                // Navigate to settings
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                FizzTransitionUtil.play(MainActivity.this, () -> startActivity(intent));
                return true;
            } else if (itemId == R.id.menu_insights) {
                // Navigate to insights
                Intent intent = new Intent(MainActivity.this, DataInsightsActivity.class);
                FizzTransitionUtil.play(MainActivity.this, () -> startActivity(intent));
                return true;
            } else if (itemId == R.id.menu_logout) {
                // Logout
                logout();
                return true;
            } else if (itemId == R.id.menu_delete_account) {
                // Delete account
                showDeleteAccountDialog();
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
        loadMyRecipes();
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(this, this);
        recipesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipesRecyclerView.setAdapter(recipeAdapter);
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
        loadMyRecipes(); // Reload the list
    }

    private void logout() {
        fAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, Login.class);
        FizzTransitionUtil.play(this, () -> {
            startActivity(intent);
            finish();
        });
    }

    private void loadMyRecipes() {
        showLoading(true);
        recipeRepository.getAllRecipes(new RecipeRepository.OnRecipesLoadedListener() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                showLoading(false);
                recipeAdapter.setRecipes(recipes);
                toggleEmptyState(recipes.isEmpty());
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDiscoverRecipes() {
        showLoading(true);
        recipeRepository.getAllPublishedRecipes(new RecipeRepository.OnRecipesLoadedListener() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                showLoading(false);
                recipeAdapter.setRecipes(recipes);
                toggleEmptyState(recipes.isEmpty());
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void toggleEmptyState(boolean isEmpty) {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        RecyclerView recyclerView = findViewById(R.id.recipesRecyclerView);

        if (isEmpty) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteAccountDialog() {
        // Create password input field
        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        passwordInput.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This action is permanent and cannot be undone. All your recipes, data, and account information will be permanently deleted.\n\nPlease enter your password to confirm:")
                .setView(passwordInput)
                .setPositiveButton("Delete", null) // Set to null to override later
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            deleteButton.setTextColor(ContextCompat.getColor(this, R.color.delete_red));
            deleteButton.setOnClickListener(v -> {
                String password = passwordInput.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                deleteAccount(password);
            });
        });

        dialog.show();
    }

    private void deleteAccount(String password) {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Authentication successful, proceed with deletion
                    deleteUserData(user);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteUserData(FirebaseUser user) {
        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);

        // First, delete all recipes and their subcollections
        userDocRef.collection("Recipes").get()
                .addOnSuccessListener(recipesSnapshot -> {
                    if (recipesSnapshot.isEmpty()) {
                        // No recipes, just delete user document and auth account
                        deleteUserDocumentAndAuth(userDocRef, user);
                    } else {
                        // Delete each recipe's subcollections, then the recipes, then user doc
                        deleteRecipesWithSubcollections(userDocRef, recipesSnapshot, user);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error deleting data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteRecipesWithSubcollections(DocumentReference userDocRef,
                                                 com.google.firebase.firestore.QuerySnapshot recipesSnapshot,
                                                 FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] pendingDeletes = {recipesSnapshot.size()};

        for (QueryDocumentSnapshot recipeDoc : recipesSnapshot) {
            DocumentReference recipeRef = recipeDoc.getReference();

            // Delete temperature_readings subcollection
            deleteSubcollection(recipeRef.collection("temperature_readings"), () -> {
                // Delete ph_readings subcollection
                deleteSubcollection(recipeRef.collection("ph_readings"), () -> {
                    // Delete the recipe document itself
                    recipeRef.delete().addOnCompleteListener(task -> {
                        pendingDeletes[0]--;
                        if (pendingDeletes[0] == 0) {
                            // All recipes deleted, now delete user document and auth
                            deleteUserDocumentAndAuth(userDocRef, user);
                        }
                    });
                });
            });
        }
    }

    private void deleteSubcollection(com.google.firebase.firestore.CollectionReference collectionRef,
                                     Runnable onComplete) {
        collectionRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                onComplete.run();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : snapshot) {
                batch.delete(doc.getReference());
            }
            batch.commit().addOnCompleteListener(task -> onComplete.run());
        }).addOnFailureListener(e -> {
            // Continue even if subcollection doesn't exist
            onComplete.run();
        });
    }

    private void deleteUserDocumentAndAuth(DocumentReference userDocRef, FirebaseUser user) {
        // Delete the user document
        userDocRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Finally, delete the Firebase Auth account
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                // Navigate to login screen
                                Intent intent = new Intent(MainActivity.this, Login.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Error deleting auth account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error deleting user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}
