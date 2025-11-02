package com.example.kombuchaapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private Context context;
    private List<Recipe> recipes;
    private RecipeRepository recipeRepository;
    private OnRecipeDeletedListener deleteListener;

    public interface OnRecipeDeletedListener {
        void onRecipeDeleted();
    }

    public RecipeAdapter(Context context, OnRecipeDeletedListener deleteListener) {
        this.context = context;
        this.recipes = new ArrayList<>();
        this.recipeRepository = new RecipeRepository();
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_card, parent, false);
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

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView recipeName, recipeStatus, recipeTea, recipeWater, recipeSugar, recipeDate;
        Button btnView, btnEdit, btnDelete;

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
        }

        public void bind(Recipe recipe) {
            // Set recipe name
            recipeName.setText(recipe.getRecipeName() != null ? recipe.getRecipeName() : "Unnamed Recipe");

            // Set status with color
            String status = recipe.getStatus() != null ? recipe.getStatus() : "draft";
            recipeStatus.setText(status.toUpperCase());
            setStatusColor(status);

            // Set ingredients
            recipeTea.setText("Tea: " + (recipe.getTeaLeaf() != null ? recipe.getTeaLeaf() : "N/A"));
            recipeWater.setText("Water: " + (recipe.getWater() != null ? recipe.getWater() : "N/A"));
            recipeSugar.setText("Sugar: " + (recipe.getSugar() != null ? recipe.getSugar() : "N/A"));

            // Set created date
            if (recipe.getCreatedDate() != null) {
                recipeDate.setText("Created: " + formatDate(recipe.getCreatedDate()));
            } else {
                recipeDate.setText("Created: Unknown");
            }

            // View button - navigate to detailed view
            btnView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ViewRecipeActivity.class);
                intent.putExtra("recipe_id", recipe.getRecipeId());
                context.startActivity(intent);
            });

            // Edit button - navigate to edit activity
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditRecipeActivity.class);
                intent.putExtra("recipe_id", recipe.getRecipeId());
                context.startActivity(intent);
            });

            // Delete button - show confirmation dialog
            btnDelete.setOnClickListener(v -> showDeleteConfirmation(recipe, getAdapterPosition()));

            // Card click - navigate to detailed view
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ViewRecipeActivity.class);
                intent.putExtra("recipe_id", recipe.getRecipeId());
                context.startActivity(intent);
            });
        }

        private void setStatusColor(String status) {
            switch (status.toLowerCase()) {
                case "draft":
                    recipeStatus.setBackgroundColor(Color.parseColor("#757575")); // Gray
                    break;
                case "brewing":
                    recipeStatus.setBackgroundColor(Color.parseColor("#FF9100")); // Orange
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
        }
    }
}
