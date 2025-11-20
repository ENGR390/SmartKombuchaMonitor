package com.example.kombuchaapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kombuchaapp.models.Recipe;
import com.example.kombuchaapp.repositories.RecipeRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private static final String TAG = "RecipeAdapter";
    private Context context;
    private List<Recipe> recipes;
    private RecipeRepository recipeRepository;
    private OnRecipeDeletedListener deleteListener;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public interface OnRecipeDeletedListener {
        void onRecipeDeleted();
    }

    public RecipeAdapter(Context context, OnRecipeDeletedListener deleteListener) {
        this.context = context;
        this.recipes = new ArrayList<>();
        this.recipeRepository = new RecipeRepository();
        this.deleteListener = deleteListener;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (isDiscoverMode) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.discover_recipe_card, parent, false);
        } else {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_recipe_card, parent, false);
        }

        Haptics.attachToTree(view);
        return new RecipeViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    public void addRecipe(Recipe recipe) {
        recipes.add(0, recipe);
        notifyItemInserted(0);
    }

    public void removeRecipe(int position) {
        recipes.remove(position);
        notifyItemRemoved(position);
    }

    private boolean isDiscoverMode = false;

    public void setDiscoverMode(boolean discover) {
        this.isDiscoverMode = discover;
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        return isDiscoverMode ? 1 : 0;
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView recipeName, recipeStatus, recipeTea, recipeWater, recipeSugar, recipeDate, likeCount;
        Button btnView, btnEdit, btnDelete, btnPublish, btnLike;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeName = itemView.findViewById(R.id.recipe_name);
            recipeStatus = itemView.findViewById(R.id.recipe_status);
            recipeTea = itemView.findViewById(R.id.recipe_tea);
            recipeWater = itemView.findViewById(R.id.recipe_water);
            recipeSugar = itemView.findViewById(R.id.recipe_sugar);
            recipeDate = itemView.findViewById(R.id.recipe_date);
            btnView = itemView.findViewById(R.id.btn_view);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnPublish = itemView.findViewById(R.id.btn_publish);
            btnLike = itemView.findViewById(R.id.btn_like);
            likeCount = itemView.findViewById(R.id.like_count);


        }

        public void bind(Recipe recipe) {
            // Reset all buttons to default
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            if (btnView != null) btnView.setVisibility(View.VISIBLE);
            if (btnPublish != null) btnPublish.setVisibility(View.VISIBLE);
            if (btnLike != null) btnLike.setVisibility(View.GONE);

            if (isDiscoverMode) {
                // Hide owner-only buttons
                if (btnEdit != null) btnEdit.setVisibility(View.GONE);
                if (btnDelete != null) btnDelete.setVisibility(View.GONE);
                if (btnView != null) btnView.setVisibility(View.GONE);
                if (btnPublish != null) btnPublish.setVisibility(View.GONE);
                // Show Discover-only button
                if (btnLike != null) btnLike.setVisibility(View.VISIBLE);

            } else {
                // Show owner-only buttons
                if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
                if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
                if (btnView != null) btnView.setVisibility(View.VISIBLE);
                if (btnPublish != null) btnPublish.setVisibility(View.VISIBLE);

                // Hide Discover button
                if (btnLike != null) btnLike.setVisibility(View.GONE);
            }


            // Set recipe name
            recipeName.setText(recipe.getRecipeName() != null ? recipe.getRecipeName() : "Unnamed Recipe");
            //Set recipe like count
            if (likeCount != null) {
                likeCount.setText(recipe.getLikes() + " Likes");
            }
            // Set status with color
            if (recipeStatus != null){
                String status = recipe.getStatus() != null ? recipe.getStatus() : "draft";
                recipeStatus.setText(status.toUpperCase());
                setStatusColor(status);
            }

            // Set ingredients
            if (recipeTea != null)
                recipeTea.setText("Tea: " + (recipe.getTeaLeaf() != null ? recipe.getTeaLeaf() : "N/A"));
            if (recipeWater != null)
                recipeWater.setText("Water: " + (recipe.getWater() != null ? recipe.getWater() : "N/A"));
            if (recipeSugar != null)
                recipeSugar.setText("Sugar: " + (recipe.getSugar() != null ? recipe.getSugar() : "N/A"));

            // Set created date
            if (recipe.getCreatedDate() != null) {
                recipeDate.setText("Created: " + formatDate(recipe.getCreatedDate()));
            } else {
                recipeDate.setText("Created: Unknown");
            }

            if (!isDiscoverMode) {
                // View button - navigate to detailed view
                if (btnView != null) {
                    btnView.setOnClickListener(v -> {
                        Intent intent = new Intent(context, ViewRecipeActivity.class);
                        intent.putExtra("recipe_id", recipe.getRecipeId());
                        context.startActivity(intent);
                    });
                }

                // Edit button - navigate to edit activity
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> {
                        Intent intent = new Intent(context, EditRecipeActivity.class);
                        intent.putExtra("recipe_id", recipe.getRecipeId());
                        context.startActivity(intent);
                    });
                }
                // Delete button - show confirmation dialog
                if (btnDelete != null)
                    btnDelete.setOnClickListener(v -> showDeleteConfirmation(recipe, getAdapterPosition()));

                // Always read from recipe object directly
                if (btnPublish != null) {
                    if (recipe.getPublished() != null && recipe.getPublished()) {
                        btnPublish.setText("Unpublish");
                        btnPublish.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(Color.GRAY)
                        );
                    } else {
                        btnPublish.setText("Publish");
                        btnPublish.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(Color.parseColor("#14828C"))
                        );
                    }


                    // Handle Publish/Unpublish click
                    btnPublish.setOnClickListener(v -> {
                        boolean newState = !(recipe.getPublished() != null && recipe.getPublished());

                        recipeRepository.updateRecipePublished(recipe.getRecipeId(), newState, new RecipeRepository.OnUpdateListener() {
                            @Override
                            public void onSuccess(String message) {

                                // Update recipe state
                                recipe.setPublished(newState);

                                // Update UI right away
                                if (newState) {
                                    btnPublish.setText("Published");
                                    btnPublish.setBackgroundTintList(
                                            android.content.res.ColorStateList.valueOf(Color.GRAY)
                                    );
                                } else {
                                    btnPublish.setText("Publish");
                                    btnPublish.setBackgroundTintList(
                                            android.content.res.ColorStateList.valueOf(Color.parseColor("#14828C"))
                                    );
                                }

                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(context, "Failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }

            }

            if (isDiscoverMode && btnLike != null) {
                btnLike.setOnClickListener(v -> {
                    Toast.makeText(context, "Liked " + recipe.getRecipeName(), Toast.LENGTH_SHORT).show();
                });
            }

            if (btnLike != null && isDiscoverMode) {
                btnLike.setOnClickListener(v -> {

                    String currentUserId = FirebaseAuth.getInstance().getUid();
                    String ownerUserId = recipe.getUserId();
                    String recipeId = recipe.getRecipeId();

                    boolean userAlreadyLiked = recipe.getLikedBy().contains(currentUserId);

                    // Optimistic UI update
                    if (userAlreadyLiked) {
                        // unlike
                        recipe.getLikedBy().remove(currentUserId);
                        recipe.setLikes(recipe.getLikes() - 1);
                        likeCount.setText(recipe.getLikes() + " Likes");
                    } else {
                        // like
                        recipe.getLikedBy().add(currentUserId);
                        recipe.setLikes(recipe.getLikes() + 1);
                        likeCount.setText(recipe.getLikes() + " Likes");
                    }

                    // Apply toggle in Firestore
                    recipeRepository.toggleLike(recipeId, ownerUserId, currentUserId, new RecipeRepository.OnUpdateListener() {
                        @Override
                        public void onSuccess(String message) {
                            // ui already updated
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(context, "Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }



            // Card click - navigate to detailed view
            if (!isDiscoverMode) {
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ViewRecipeActivity.class);
                    intent.putExtra("recipe_id", recipe.getRecipeId());
                    context.startActivity(intent);
                });
            } else {
                // Disable click in Discover mode
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }
        }

        private void setStatusColor(String status) {
            switch (status.toLowerCase()) {
                case "draft":
                    recipeStatus.setBackgroundColor(Color.parseColor("#757575")); // Gray
                    break;
                case "brewing":
                    recipeStatus.setBackgroundColor(Color.parseColor("#FF9100")); // Orange
                    break;
                case "paused":
                    recipeStatus.setBackgroundColor(Color.parseColor("#FF9800")); // Amber
                    break;
                case "completed":
                    recipeStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                    break;
                default:
                    recipeStatus.setBackgroundColor(Color.parseColor("#4A148C")); // Purple
                    break;
            }
        }

        private String formatDate(Timestamp timestamp) {
            if (timestamp == null) return "Unknown";
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(date);
        }

        private void showDeleteConfirmation(Recipe recipe, int position) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Delete Recipe")
                    .setMessage("Are you sure you want to delete \"" + recipe.getRecipeName() + "\"? This action cannot be undone.")
                    .setPositiveButton("Delete", (d, which) -> deleteRecipe(recipe, position))
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create();

            dialog.show();
            View dialogView = dialog.getWindow().getDecorView();
            Haptics.attachToTree(dialogView);

        }

        private void deleteRecipe(Recipe recipe, int position) {
            // First, remove from sensor control to prevent new sensor writes during deletion
            removeRecipeForSensors(recipe.getRecipeId());
            
            // Small delay to ensure sensor sees the removal before we start deleting
            new android.os.Handler().postDelayed(() -> {
                // Now delete the recipe (which will delete all subcollections first)
                recipeRepository.deleteRecipe(recipe.getRecipeId(), new RecipeRepository.OnUpdateListener() {
                    @Override
                    public void onSuccess(String message) {
                        removeRecipe(position);
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        if (deleteListener != null) {
                            deleteListener.onRecipeDeleted();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(context, "Failed to delete: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }, 500); // 500ms delay to let sensor control update propagate
        }

        private void removeRecipeForSensors(String deletedRecipeId) {
            // First, read the current active_config document
            db.collection("sensor_control").document("active_config").get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String activeRecipeId = documentSnapshot.getString("active_recipe_id");

                            // Check if the deleted recipe is the one that's currently active
                            if (deletedRecipeId.equals(activeRecipeId)) {
                                // If it is, clear the active config by setting fields to null
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("active_recipe_id", null);
                                updates.put("active_user_id", null);

                                db.collection("sensor_control").document("active_config")
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Active recipe cleared from sensor_control because it was deleted.");
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to clear active recipe from sensor_control", e));
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to check active sensor config", e));
        }
    }
}
