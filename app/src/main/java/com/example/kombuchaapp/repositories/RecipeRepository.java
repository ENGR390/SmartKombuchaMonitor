package com.example.kombuchaapp.repositories;

import android.util.Log;
import com.example.kombuchaapp.models.Recipe;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Handles all Firestore operations for recipes

public class RecipeRepository {

    private static final String TAG = "RecipeRepository";
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userID;

    public RecipeRepository() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    private CollectionReference getRecipesCollection() {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            return null;
        }
        userID = user.getUid();
        return fStore.collection("users").document(userID).collection("Recipes");
    }


    public void createRecipe(Recipe recipe, OnRecipeSavedListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }

        userID = user.getUid();
        recipe.setUserId(userID);

        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        // Convert Recipe object to Map
        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("userId", recipe.getUserId());
        recipeData.put("recipeName", recipe.getRecipeName());
        recipeData.put("teaLeaf", recipe.getTeaLeaf());
        recipeData.put("water", recipe.getWater());
        recipeData.put("sugar", recipe.getSugar());
        recipeData.put("scoby", recipe.getScoby());
        recipeData.put("kombuchaStarter", recipe.getKombuchaStarter());
        recipeData.put("flavor", recipe.getFlavor());
        recipeData.put("status", recipe.getStatus());
        recipeData.put("createdDate", recipe.getCreatedDate());
        recipeData.put("notes", recipe.getNotes());

        // Add to Firestore (auto-generates document ID)
        recipesRef.add(recipeData)
                .addOnSuccessListener(documentReference -> {
                    String recipeId = documentReference.getId();
                    recipe.setRecipeId(recipeId);
                    Log.d(TAG, "Recipe created with ID: " + recipeId);
                    listener.onSuccess("Recipe saved successfully", recipeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create recipe: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

//recipe for current user
    public void getAllRecipes(OnRecipesLoadedListener listener) {
        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        recipesRef.orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Recipe recipe = parseRecipe(doc);
                        if (recipe != null) {
                            recipes.add(recipe);
                        }
                    }
                    Log.d(TAG, "Loaded " + recipes.size() + " recipes");
                    listener.onSuccess(recipes);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load recipes: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }


    public void getRecipeById(String recipeId, OnRecipeLoadedListener listener) {
        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        recipesRef.document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Recipe recipe = parseRecipe(documentSnapshot);
                        listener.onSuccess(recipe);
                    } else {
                        listener.onFailure("Recipe not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load recipe: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }


    public void updateRecipe(String recipeId, Recipe recipe, OnUpdateListener listener) {
        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("recipeName", recipe.getRecipeName());
        updates.put("teaLeaf", recipe.getTeaLeaf());
        updates.put("water", recipe.getWater());
        updates.put("sugar", recipe.getSugar());
        updates.put("scoby", recipe.getScoby());
        updates.put("kombuchaStarter", recipe.getKombuchaStarter());
        updates.put("flavor", recipe.getFlavor());
        updates.put("status", recipe.getStatus());
        updates.put("notes", recipe.getNotes());

        recipesRef.document(recipeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Recipe updated: " + recipeId);
                    listener.onSuccess("Recipe updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update recipe: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }


    public void updateRecipeStatus(String recipeId, String newStatus, OnUpdateListener listener) {
        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        if ("brewing".equals(newStatus)) {
            updates.put("brewingStartDate", Timestamp.now());
        } else if ("completed".equals(newStatus)) {
            updates.put("completionDate", Timestamp.now());
        }

        recipesRef.document(recipeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Recipe status updated to: " + newStatus);
                    listener.onSuccess("Status updated to " + newStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update status: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    public void deleteRecipe(String recipeId, OnUpdateListener listener) {
        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        // First, delete all subcollections (temperature_readings and ph_readings)
        deleteRecipeSubcollections(recipeId, new OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                // After subcollections are deleted, delete the recipe document
                recipesRef.document(recipeId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Recipe deleted: " + recipeId);
                            listener.onSuccess("Recipe deleted successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete recipe: " + e.getMessage());
                            listener.onFailure(e.getMessage());
                        });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to delete subcollections: " + error);
                listener.onFailure("Failed to delete recipe data: " + error);
            }
        });
    }

    private void deleteRecipeSubcollections(String recipeId, OnUpdateListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }

        String userId = user.getUid();
        
        // Counter to track deletion completion
        final int[] deletionCount = {0};
        final int[] failureCount = {0};
        
        // Delete temperature readings
        fStore.collection("users")
                .document(userId)
                .collection("Recipes")
                .document(recipeId)
                .collection("temperature_readings")
                .get()
                .addOnSuccessListener(tempSnapshots -> {
                    if (tempSnapshots.isEmpty()) {
                        deletionCount[0]++;
                        checkDeletionComplete(deletionCount, failureCount, listener);
                    } else {
                        for (DocumentSnapshot doc : tempSnapshots.getDocuments()) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted temp reading"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete temp reading", e));
                        }
                        deletionCount[0]++;
                        checkDeletionComplete(deletionCount, failureCount, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch temperature readings", e);
                    failureCount[0]++;
                    checkDeletionComplete(deletionCount, failureCount, listener);
                });

        // Delete pH readings
        fStore.collection("users")
                .document(userId)
                .collection("Recipes")
                .document(recipeId)
                .collection("ph_readings")
                .get()
                .addOnSuccessListener(phSnapshots -> {
                    if (phSnapshots.isEmpty()) {
                        deletionCount[0]++;
                        checkDeletionComplete(deletionCount, failureCount, listener);
                    } else {
                        for (DocumentSnapshot doc : phSnapshots.getDocuments()) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted pH reading"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete pH reading", e));
                        }
                        deletionCount[0]++;
                        checkDeletionComplete(deletionCount, failureCount, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch pH readings", e);
                    failureCount[0]++;
                    checkDeletionComplete(deletionCount, failureCount, listener);
                });
    }

    private void checkDeletionComplete(int[] deletionCount, int[] failureCount, OnUpdateListener listener) {
        // We're deleting 2 subcollections (temperature and pH)
        if (deletionCount[0] + failureCount[0] >= 2) {
            if (failureCount[0] > 0) {
                listener.onFailure("Some sensor data could not be deleted");
            } else {
                listener.onSuccess("Subcollections deleted");
            }
        }
    }

    // Parse Firestore document into Recipe object
    private Recipe parseRecipe(DocumentSnapshot doc) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(doc.getId());
        recipe.setUserId(doc.getString("userId"));
        recipe.setRecipeName(doc.getString("recipeName"));
        recipe.setTeaLeaf(doc.getString("teaLeaf"));
        recipe.setWater(doc.getString("water"));
        recipe.setSugar(doc.getString("sugar"));
        recipe.setScoby(doc.getString("scoby"));
        recipe.setKombuchaStarter(doc.getString("kombuchaStarter"));
        recipe.setFlavor(doc.getString("flavor"));
        recipe.setStatus(doc.getString("status") != null ? doc.getString("status") : "draft");
        recipe.setNotes(doc.getString("notes"));
        recipe.setCreatedDate(doc.getTimestamp("createdDate"));
        recipe.setBrewingStartDate(doc.getTimestamp("brewingStartDate"));
        recipe.setCompletionDate(doc.getTimestamp("completionDate"));
        return recipe;
    }

    public interface OnRecipeSavedListener {
        void onSuccess(String message, String recipeId);
        void onFailure(String error);
    }

    public interface OnRecipeLoadedListener {
        void onSuccess(Recipe recipe);
        void onFailure(String error);
    }

    public interface OnRecipesLoadedListener {
        void onSuccess(List<Recipe> recipes);
        void onFailure(String error);
    }

    public interface OnUpdateListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
