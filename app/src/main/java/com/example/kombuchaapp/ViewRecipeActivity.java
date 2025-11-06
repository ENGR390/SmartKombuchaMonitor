package com.example.kombuchaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;
import com.example.kombuchaapp.AlertAdapter;
import com.example.kombuchaapp.TemperatureAlert;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ViewRecipeActivity extends AppCompatActivity {

    private static final String TAG = "ViewRecipeActivity";

    // UI Components
    private TextView tvRecipeName, tvStatus, tvTeaLeaf, tvWater, tvSugar, tvScoby,
            tvKombuchaStarter, tvFlavor, tvCreatedDate, tvBrewingStartDate,
            tvCompletionDate, tvNotes, tvTempAlert;
    private Button btnEdit, btnStartBrewing, btnMarkCompleted, btnPauseBrewing,
            btnResumeBrewing, btnBackToDraft, btnRebrew;
    private ProgressBar progressBar;
    private View notesSection, flavorSection;

    // Repository
    private RecipeRepository recipeRepository;
    private FirebaseFirestore db;
    private String recipeId;
    private Recipe currentRecipe;

    private ListenerRegistration readingsListener;

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
        db = FirebaseFirestore.getInstance();

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
        tvTempAlert = findViewById(R.id.tv_temp_alert);
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
        btnPauseBrewing = findViewById(R.id.btn_pause_brewing);
        btnResumeBrewing = findViewById(R.id.btn_resume_brewing);
        btnBackToDraft = findViewById(R.id.btn_back_to_draft);
        btnRebrew = findViewById(R.id.btn_rebrew);

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

        if (!"brewing".equalsIgnoreCase(status)) {
            tvTempAlert.setVisibility(View.GONE);
        }
    }

    private void updateButtonsForStatus(String status) {
        // Hide all action buttons first
        btnStartBrewing.setVisibility(View.GONE);
        btnPauseBrewing.setVisibility(View.GONE);
        btnMarkCompleted.setVisibility(View.GONE);
        btnResumeBrewing.setVisibility(View.GONE);
        btnBackToDraft.setVisibility(View.GONE);
        btnRebrew.setVisibility(View.GONE);

        switch (status.toLowerCase()) {
            case "draft":
                // Draft: Can only start brewing
                btnStartBrewing.setVisibility(View.VISIBLE);
                break;
            case "brewing":
                // Brewing: Can pause, complete, or go back to draft
                btnPauseBrewing.setVisibility(View.VISIBLE);
                btnMarkCompleted.setVisibility(View.VISIBLE);
                btnBackToDraft.setVisibility(View.VISIBLE);
                break;
            case "paused":
                // Paused: Can resume or go back to draft
                btnResumeBrewing.setVisibility(View.VISIBLE);
                btnBackToDraft.setVisibility(View.VISIBLE);
                break;
            case "completed":
                // Completed: Can rebrew
                btnRebrew.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupButtons() {
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(ViewRecipeActivity.this, EditRecipeActivity.class);
            intent.putExtra("recipe_id", recipeId);
            startActivity(intent);
        });

        btnStartBrewing.setOnClickListener(v -> startBrewingProcess());
        btnMarkCompleted.setOnClickListener(v -> updateRecipeStatus("completed"));
        btnPauseBrewing.setOnClickListener(v -> pauseBrewing());
        btnResumeBrewing.setOnClickListener(v -> resumeBrewing());
        btnBackToDraft.setOnClickListener(v -> confirmBackToDraft());
        btnRebrew.setOnClickListener(v -> confirmRebrew());
    }

    private void startBrewingProcess() {
        showLoading(true);

        // Check the central sensor_control document
        db.collection("sensor_control").document("active_config").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check if another recipe is already active
                        String activeRecipeId = documentSnapshot.getString("active_recipe_id");

                        // If a recipe is active and it's not the current one, block the process
                        if (activeRecipeId != null && !activeRecipeId.equals(recipeId)) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(ViewRecipeActivity.this,
                                        "Another recipe is already brewing. Please complete it first.",
                                        Toast.LENGTH_LONG).show();
                            });
                            return; // Stop here
                        }
                    }
                    // If the document doesn't exist, or no recipe is active, proceed to start brewing.
                    runOnUiThread(() -> updateRecipeStatus("brewing"));

                })
                .addOnFailureListener(e -> {
                    // If we can't check the status, it's safer to block and show an error.
                    runOnUiThread(() -> {
                        showLoading(false);
                        Log.e(TAG, "Failed to check active brewing status", e);
                        Toast.makeText(ViewRecipeActivity.this,
                                "Failed to check brewing status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void pauseBrewing() {
        showLoading(true);

        // Remove from sensor control but keep the status as paused
        removeRecipeForSensors();

        // Update status to paused
        recipeRepository.updateRecipeStatus(recipeId, "paused", new RecipeRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ViewRecipeActivity.this,
                            "Brewing paused. Sensors deactivated.",
                            Toast.LENGTH_SHORT).show();
                    loadRecipe(); // Reload to update UI
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ViewRecipeActivity.this,
                            "Failed to pause: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resumeBrewing() {
        showLoading(true);

        // Check if another recipe is active
        db.collection("sensor_control").document("active_config").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String activeRecipeId = documentSnapshot.getString("active_recipe_id");

                        if (activeRecipeId != null && !activeRecipeId.equals(recipeId)) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(ViewRecipeActivity.this,
                                        "Another recipe is already brewing. Please complete it first.",
                                        Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                    }

                    // Resume brewing - change status and reactivate sensors
                    runOnUiThread(() -> updateRecipeStatus("brewing"));
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(ViewRecipeActivity.this,
                                "Failed to check brewing status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void confirmBackToDraft() {
        new AlertDialog.Builder(this)
                .setTitle("Back to Draft")
                .setMessage("This will remove all sensor readings for this recipe. Are you sure?")
                .setPositiveButton("Yes, Go Back", (dialog, which) -> backToDraft())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void backToDraft() {
        showLoading(true);

        // First, delete all temperature readings for this recipe
        deleteSensorReadings();

        // Remove from sensor control
        removeRecipeForSensors();

        // Clear brewing dates and update status to draft
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "draft");
        updates.put("brewingStartDate", null);
        updates.put("completionDate", null);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showLoading(false);
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("Recipes").document(recipeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(ViewRecipeActivity.this,
                                "Recipe moved back to draft. All sensor data deleted.",
                                Toast.LENGTH_SHORT).show();
                        loadRecipe();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(ViewRecipeActivity.this,
                                "Failed to update status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void confirmRebrew() {
        new AlertDialog.Builder(this)
                .setTitle("Rebrew Recipe")
                .setMessage("This will restart brewing for this recipe. Previous completion date will be cleared.")
                .setPositiveButton("Yes, Rebrew", (dialog, which) -> rebrew())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rebrew() {
        showLoading(true);

        // Check if another recipe is active
        db.collection("sensor_control").document("active_config").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String activeRecipeId = documentSnapshot.getString("active_recipe_id");

                        if (activeRecipeId != null && !activeRecipeId.equals(recipeId)) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(ViewRecipeActivity.this,
                                        "Another recipe is already brewing. Please complete it first.",
                                        Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                    }

                    // Clear completion date and restart brewing
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "brewing");
                    updates.put("brewingStartDate", Timestamp.now());
                    updates.put("completionDate", null);

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        showLoading(false);
                        Toast.makeText(ViewRecipeActivity.this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users").document(user.getUid())
                            .collection("Recipes").document(recipeId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                runOnUiThread(() -> {
                                    addRecipeForSensors();
                                    showLoading(false);
                                    Toast.makeText(ViewRecipeActivity.this,
                                            "Brewing restarted!",
                                            Toast.LENGTH_SHORT).show();
                                    loadRecipe();
                                });
                            })
                            .addOnFailureListener(e -> {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(ViewRecipeActivity.this,
                                            "Failed to restart brewing: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(ViewRecipeActivity.this,
                                "Failed to check brewing status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void deleteSensorReadings() {
        // Delete all temperature readings for this recipe
        db.collection("temperature_readings")
                .whereEqualTo("recipe_id", recipeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().delete();
                    }
                    Log.d(TAG, "Deleted sensor readings for recipe: " + recipeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete sensor readings", e);
                });
    }

    private void updateRecipeStatus(String newStatus) {
        showLoading(true);

        recipeRepository.updateRecipeStatus(recipeId, newStatus, new RecipeRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    // If starting brewing, also set as active recipe in sensor_control
                    if ("brewing".equals(newStatus)) {
                        addRecipeForSensors();
                        startReadingListener();
                    }

                    if ("completed".equals(newStatus)) {
                        removeRecipeForSensors();
                        stopReadingListener();
                        tvTempAlert.setVisibility(View.GONE);
                    }

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

    private void removeRecipeForSensors() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot remove recipe for sensors: No user logged in.");
            return;
        }

        // To "remove" the active recipe, we set the fields to null.
        Map<String, Object> updates = new HashMap<>();
        updates.put("active_recipe_id", null);
        updates.put("active_user_id", null);

        db.collection("sensor_control")
                .document("active_config")
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Active recipe and user removed from sensor_control.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove active recipe from sensor_control", e);
                });
    }

    private void addRecipeForSensors() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(ViewRecipeActivity.this, "Error: No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Create a map to hold the active recipe and user ID.
        Map<String, Object> activeConfig = new HashMap<>();
        activeConfig.put("active_recipe_id", recipeId);
        activeConfig.put("active_user_id", userId);

        // Set the data on the 'active_config' document, overwriting previous values.
        db.collection("sensor_control")
                .document("active_config")
                .set(activeConfig)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Active recipe and user set in sensor_control: " + recipeId);
                    Toast.makeText(ViewRecipeActivity.this,
                            "Recipe activated for sensor readings!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to set active recipe in sensor_control", e);
                    Toast.makeText(ViewRecipeActivity.this,
                            "Warning: Couldn't activate sensors: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
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
    protected void onStart() {
        super.onStart();
        if (currentRecipe != null && "brewing".equalsIgnoreCase(currentRecipe.getStatus())) {
            startReadingListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopReadingListener();
    }

    private void startReadingListener() {
        stopReadingListener();
        readingsListener = db.collection("sensor_readings")
                .whereEqualTo("recipe_id", recipeId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null || snap.isEmpty()) return;

                    Double tempFd = snap.getDocuments().get(0).getDouble("temperature_f");
                    if (tempFd == null) return;
                    float tempF = tempFd.floatValue();

                    AlertAdapter.handleNewReading(this, tempF, tvTempAlert);

                    TemperatureAlert.Result r = TemperatureAlert.evaluateF(tempF);
                    tvTempAlert.setVisibility(View.VISIBLE);
                    tvTempAlert.setContentDescription("Temperature status: " + r.title);
                });
    }

    private void stopReadingListener() {
        if (readingsListener != null) {
            readingsListener.remove();
            readingsListener = null;
        }
        AlertAdapter.resetDebounce();
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
