package com.example.kombuchaapp.repositories;

import android.util.Log;
import com.example.kombuchaapp.models.Recipe;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
        recipeData.put("minPh", recipe.getMinPh());
        recipeData.put("maxPh", recipe.getMaxPh());
        recipeData.put("published",recipe.getPublished());
        recipeData.put("likes", recipe.getLikes());

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
        updates.put("minPh", recipe.getMinPh());
        updates.put("maxPh", recipe.getMaxPh());

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

        // Track completion of both subcollection deletions
        final boolean[] tempComplete = {false};
        final boolean[] phComplete = {false};
        final boolean[] hasError = {false};

        // Delete temperature readings
        fStore.collection("users")
                .document(userId)
                .collection("Recipes")
                .document(recipeId)
                .collection("temperature_readings")
                .get()
                .addOnSuccessListener(tempSnapshots -> {
                    if (tempSnapshots.isEmpty()) {
                        Log.d(TAG, "No temperature readings to delete");
                        tempComplete[0] = true;
                        checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                    } else {
                        int totalDocs = tempSnapshots.size();
                        final int[] deletedCount = {0};

                        Log.d(TAG, "Deleting " + totalDocs + " temperature readings");

                        for (DocumentSnapshot doc : tempSnapshots.getDocuments()) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        deletedCount[0]++;
                                        Log.d(TAG, "Deleted temp reading " + deletedCount[0] + "/" + totalDocs);

                                        if (deletedCount[0] == totalDocs) {
                                            tempComplete[0] = true;
                                            Log.d(TAG, "All temperature readings deleted");
                                            checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to delete temp reading", e);
                                        hasError[0] = true;
                                        deletedCount[0]++;

                                        if (deletedCount[0] == totalDocs) {
                                            tempComplete[0] = true;
                                            checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch temperature readings", e);
                    hasError[0] = true;
                    tempComplete[0] = true;
                    checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
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
                        Log.d(TAG, "No pH readings to delete");
                        phComplete[0] = true;
                        checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                    } else {
                        int totalDocs = phSnapshots.size();
                        final int[] deletedCount = {0};

                        Log.d(TAG, "Deleting " + totalDocs + " pH readings");

                        for (DocumentSnapshot doc : phSnapshots.getDocuments()) {
                            doc.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        deletedCount[0]++;
                                        Log.d(TAG, "Deleted pH reading " + deletedCount[0] + "/" + totalDocs);

                                        if (deletedCount[0] == totalDocs) {
                                            phComplete[0] = true;
                                            Log.d(TAG, "All pH readings deleted");
                                            checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to delete pH reading", e);
                                        hasError[0] = true;
                                        deletedCount[0]++;

                                        if (deletedCount[0] == totalDocs) {
                                            phComplete[0] = true;
                                            checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch pH readings", e);
                    hasError[0] = true;
                    phComplete[0] = true;
                    checkSubcollectionDeletionComplete(tempComplete, phComplete, hasError, listener);
                });
    }

    private void checkSubcollectionDeletionComplete(boolean[] tempComplete, boolean[] phComplete,
                                                    boolean[] hasError, OnUpdateListener listener) {
        // Only proceed when BOTH subcollections are completely deleted
        if (tempComplete[0] && phComplete[0]) {
            if (hasError[0]) {
                Log.w(TAG, "Subcollections deleted with some errors");
                listener.onFailure("Some sensor data could not be deleted");
            } else {
                Log.d(TAG, "All subcollections deleted successfully");
                listener.onSuccess("Subcollections deleted");
            }
        }
    }

    public void updateRecipePublished(String recipeId, boolean published, OnUpdateListener listener) {
        CollectionReference recipesRef = getRecipesCollection();

        if (recipesRef == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("published", published);

        recipesRef.document(recipeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess(published ? "Recipe published" : "Recipe unpublished");
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getAllPublishedRecipes(OnRecipesLoadedListener listener) {
        FirebaseFirestore fStore = FirebaseFirestore.getInstance();

        fStore.collectionGroup("Recipes")
                .whereEqualTo("published", true)
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100) // Limit to prevent quota issues and improve performance
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Recipe recipe = parseRecipe(doc);
                        if (recipe != null) {
                            recipes.add(recipe);
                        }
                    }
                    listener.onSuccess(recipes);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void incrementLikes(String recipeId, String ownerUserId, OnUpdateListener listener) {

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(ownerUserId)
                .collection("Recipes")
                .document(recipeId)
                .update("likes", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> listener.onSuccess("Liked!"))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void toggleLike(String recipeId, String ownerUserId, String currentUserId, OnUpdateListener listener) {
        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection("users")
                .document(ownerUserId)
                .collection("Recipes")
                .document(recipeId);

        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref);

            // Safe type casting for likedBy list
            List<String> likedBy = new ArrayList<>();
            Object likedByObj = snapshot.get("likedBy");
            if (likedByObj instanceof List<?>) {
                for (Object item : (List<?>) likedByObj) {
                    if (item instanceof String) {
                        likedBy.add((String) item);
                    }
                }
            }

            Long likes = snapshot.getLong("likes");
            if (likes == null) likes = 0L;

            if (likedBy.contains(currentUserId)) {
                // Unlike
                likedBy.remove(currentUserId);
                likes -= 1;
            } else {
                // Like
                likedBy.add(currentUserId);
                likes += 1;
            }

            transaction.update(ref, "likedBy", likedBy);
            transaction.update(ref, "likes", likes);

            return null;
        }).addOnSuccessListener(aVoid -> {
            listener.onSuccess("Updated");
        }).addOnFailureListener(e -> {
            listener.onFailure(e.getMessage());
        });
    }

    // Parse Firestore document into Recipe object
// Replace the parseRecipe method in RecipeRepository.java with this:

    private Recipe parseRecipe(DocumentSnapshot doc) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(doc.getId());

        // Try to get userId from document field first
        String userId = doc.getString("userId");
        Log.d(TAG, "parseRecipe - userId from field: " + userId);

        // If userId is null, extract from document path: users/{userId}/Recipes/{recipeId}
        if (userId == null || userId.isEmpty()) {
            String path = doc.getReference().getPath();
            Log.d(TAG, "parseRecipe - document path: " + path);
            // Path format: users/{userId}/Recipes/{recipeId}
            String[] parts = path.split("/");
            Log.d(TAG, "parseRecipe - path parts length: " + parts.length);
            if (parts.length >= 2 && "users".equals(parts[0])) {
                userId = parts[1];
                Log.d(TAG, "parseRecipe - extracted userId from path: " + userId);
            }
        }

        recipe.setUserId(userId);
        Log.d(TAG, "parseRecipe - final userId set: " + userId);
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

        try {
            Object minPhObj = doc.get("minPh");
            if (minPhObj != null) {
                if (minPhObj instanceof Double) {
                    recipe.setMinPh((Double) minPhObj);
                } else if (minPhObj instanceof Long) {
                    recipe.setMinPh(((Long) minPhObj).doubleValue());
                }
            } else {
                recipe.setMinPh(0.0); // Default value
            }

            Object maxPhObj = doc.get("maxPh");
            if (maxPhObj != null) {
                if (maxPhObj instanceof Double) {
                    recipe.setMaxPh((Double) maxPhObj);
                } else if (maxPhObj instanceof Long) {
                    recipe.setMaxPh(((Long) maxPhObj).doubleValue());
                }
            } else {
                recipe.setMaxPh(0.0); // Default value
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing pH values: " + e.getMessage());
            recipe.setMinPh(0.0);
            recipe.setMaxPh(0.0);
        }
        recipe.setPublished(doc.getBoolean("published"));
        Long likesValue = doc.getLong("likes");
        recipe.setLikes(likesValue != null ? likesValue.intValue() : 0);
        // Safe type casting for likedBy list
        List<String> likedByList = new ArrayList<>();
        Object likedByObj = doc.get("likedBy");
        if (likedByObj instanceof List<?>) {
            for (Object item : (List<?>) likedByObj) {
                if (item instanceof String) {
                    likedByList.add((String) item);
                }
            }
        }
        recipe.setLikedBy(likedByList);

        // Parse review fields
        Double ratingValue = doc.getDouble("rating");
        recipe.setRating(ratingValue != null ? ratingValue.floatValue() : null);
        recipe.setReviewNotes(doc.getString("reviewNotes"));
        recipe.setReviewDate(doc.getTimestamp("reviewDate"));

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
