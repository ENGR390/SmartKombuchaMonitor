package com.example.kombuchaapp.repositories;

import android.util.Log;

import com.example.kombuchaapp.models.DataInsights;
import com.example.kombuchaapp.models.Recipe;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DataInsightsRepository {

    private static final String TAG = "DataInsightsRepository";
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    public DataInsightsRepository() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    public void calculateInsights(OnInsightsLoadedListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();
        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }

        String userId = user.getUid();
        DataInsights insights = new DataInsights();

        // Load all recipes for the user
        fStore.collection("users")
                .document(userId)
                .collection("Recipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onSuccess(insights);
                        return;
                    }

                    List<Recipe> recipes = new ArrayList<>();
                    Map<String, Integer> statusCounts = new HashMap<>();
                    Map<String, Integer> teaCounts = new HashMap<>();

                    int completedCount = 0;
                    long totalFermentationTime = 0;
                    int fermentationCount = 0;

                    // Parse recipes and calculate basic stats
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Recipe recipe = parseRecipe(doc);
                        recipes.add(recipe);

                        // Count by status
                        String status = recipe.getStatus() != null ? recipe.getStatus() : "draft";
                        statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);

                        // Count completed
                        if ("completed".equalsIgnoreCase(status)) {
                            completedCount++;

                            // Calculate fermentation time
                            if (recipe.getBrewingStartDate() != null && recipe.getCompletionDate() != null) {
                                long startMillis = recipe.getBrewingStartDate().toDate().getTime();
                                long endMillis = recipe.getCompletionDate().toDate().getTime();
                                long durationMillis = endMillis - startMillis;
                                totalFermentationTime += durationMillis;
                                fermentationCount++;
                            }
                        }

                        // Count tea types - FIXED: Use full tea name instead of just first word
                        String teaLeaf = recipe.getTeaLeaf();
                        if (teaLeaf != null && !teaLeaf.trim().isEmpty()) {
                            String teaType = extractTeaType(teaLeaf);
                            teaCounts.put(teaType, teaCounts.getOrDefault(teaType, 0) + 1);
                        }
                    }

                    // Set overall statistics
                    insights.setTotalRecipes(recipes.size());
                    insights.setCompletedBrews(completedCount);
                    insights.setSuccessRate(recipes.size() > 0 ?
                            (completedCount * 100.0f / recipes.size()) : 0);

                    if (fermentationCount > 0) {
                        float avgMillis = (float) totalFermentationTime / fermentationCount;
                        float avgDays = avgMillis / (1000 * 60 * 60 * 24);
                        insights.setAvgFermentationDays(avgDays);
                    }

                    insights.setStatusCounts(statusCounts);
                    insights.setTeaTypeCounts(teaCounts);

                    // Find most used tea
                    String mostUsedTea = null;
                    int maxTeaCount = 0;
                    for (Map.Entry<String, Integer> entry : teaCounts.entrySet()) {
                        if (entry.getValue() > maxTeaCount) {
                            maxTeaCount = entry.getValue();
                            mostUsedTea = entry.getKey();
                        }
                    }
                    insights.setMostUsedTea(mostUsedTea);

                    // Find most successful recipe (most completed)
                    findMostSuccessfulRecipe(userId, insights, recipes, () -> {
                        // Calculate temperature and pH statistics
                        calculateTemperatureStats(userId, recipes, insights, () -> {
                            calculatePhStats(userId, recipes, insights, () -> {
                                listener.onSuccess(insights);
                            });
                        });
                    });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load recipes for insights", e);
                    listener.onFailure(e.getMessage());
                });
    }

    private void findMostSuccessfulRecipe(String userId, DataInsights insights,
                                          List<Recipe> recipes, Runnable onComplete) {
        if (recipes.isEmpty()) {
            onComplete.run();
            return;
        }

        // Count which recipe was brewed and completed most often
        Map<String, Integer> recipeCompletionCounts = new HashMap<>();

        for (Recipe recipe : recipes) {
            if ("completed".equalsIgnoreCase(recipe.getStatus()) &&
                    recipe.getRecipeName() != null) {
                String name = recipe.getRecipeName();
                recipeCompletionCounts.put(name,
                        recipeCompletionCounts.getOrDefault(name, 0) + 1);
            }
        }

        String mostSuccessful = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : recipeCompletionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostSuccessful = entry.getKey();
            }
        }

        insights.setMostSuccessfulRecipe(mostSuccessful);
        onComplete.run();
    }

    private void calculateTemperatureStats(String userId, List<Recipe> recipes,
                                           DataInsights insights, Runnable onComplete) {
        if (recipes.isEmpty()) {
            onComplete.run();
            return;
        }

        final AtomicInteger pendingReads = new AtomicInteger(recipes.size());
        final List<Float> allTemperatures = new ArrayList<>();
        final List<Float> optimalTemperatures = new ArrayList<>();
        final AtomicInteger alertCount = new AtomicInteger(0);

        for (Recipe recipe : recipes) {
            fStore.collection("users")
                    .document(userId)
                    .collection("Recipes")
                    .document(recipe.getRecipeId())
                    .collection("temperature_readings")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(tempSnapshots -> {
                        for (QueryDocumentSnapshot doc : tempSnapshots) {
                            Double tempC = doc.getDouble("temperature_c");
                            if (tempC != null) {
                                allTemperatures.add(tempC.floatValue());

                                // Check if in optimal range (23.9-26.7°C which is 75-80°F)
                                if (tempC >= 23.9f && tempC <= 26.7f &&
                                        "completed".equalsIgnoreCase(recipe.getStatus())) {
                                    optimalTemperatures.add(tempC.floatValue());
                                }

                                // Check if critical/lethal (outside 18.3-29.4°C which is 65-85°F)
                                if (tempC < 18.3f || tempC > 29.4f) {
                                    alertCount.incrementAndGet();
                                }
                            }
                        }

                        if (pendingReads.decrementAndGet() == 0) {
                            // All recipes processed
                            if (!allTemperatures.isEmpty()) {
                                float sum = 0;
                                for (Float temp : allTemperatures) {
                                    sum += temp;
                                }
                                insights.setAvgTemperatureC(sum / allTemperatures.size());
                            }

                            if (!optimalTemperatures.isEmpty()) {
                                float min = Float.MAX_VALUE;
                                float max = Float.MIN_VALUE;
                                for (Float temp : optimalTemperatures) {
                                    min = Math.min(min, temp);
                                    max = Math.max(max, temp);
                                }
                                insights.setOptimalTempRangeMin(min);
                                insights.setOptimalTempRangeMax(max);
                            }

                            insights.setTempAlertsTriggered(alertCount.get());
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load temperature readings for recipe: " +
                                recipe.getRecipeId(), e);
                        if (pendingReads.decrementAndGet() == 0) {
                            onComplete.run();
                        }
                    });
        }
    }

    private void calculatePhStats(String userId, List<Recipe> recipes,
                                  DataInsights insights, Runnable onComplete) {
        if (recipes.isEmpty()) {
            onComplete.run();
            return;
        }

        final AtomicInteger pendingReads = new AtomicInteger(recipes.size());
        final List<Float> harvestPhValues = new ArrayList<>();
        final List<Float> optimalPhValues = new ArrayList<>();
        final List<Long> timeToOptimalPh = new ArrayList<>();

        for (Recipe recipe : recipes) {
            fStore.collection("users")
                    .document(userId)
                    .collection("Recipes")
                    .document(recipe.getRecipeId())
                    .collection("ph_readings")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(phSnapshots -> {
                        Float lastPh = null;
                        Long firstOptimalTime = null;
                        Long startTime = recipe.getBrewingStartDate() != null ?
                                recipe.getBrewingStartDate().toDate().getTime() : null;

                        for (QueryDocumentSnapshot doc : phSnapshots) {
                            Double phVal = doc.getDouble("ph_value");
                            if (phVal != null) {
                                lastPh = phVal.floatValue();

                                // Check if in optimal range (2.5-3.5)
                                if (phVal >= 2.5f && phVal <= 3.5f) {
                                    optimalPhValues.add(phVal.floatValue());

                                    // Track time to reach optimal pH
                                    if (firstOptimalTime == null && startTime != null) {
                                        String timestamp = doc.getString("timestamp");
                                        if (timestamp != null) {
                                            try {
                                                // Parse timestamp (format: yyyy-MM-dd HH:mm:ss)
                                                java.text.SimpleDateFormat sdf =
                                                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                java.util.Date date = sdf.parse(timestamp);
                                                if (date != null) {
                                                    firstOptimalTime = date.getTime();
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Failed to parse pH timestamp", e);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Record pH at harvest for completed brews
                        if ("completed".equalsIgnoreCase(recipe.getStatus()) && lastPh != null) {
                            harvestPhValues.add(lastPh);
                        }

                        // Record time to optimal pH
                        if (firstOptimalTime != null && startTime != null) {
                            timeToOptimalPh.add(firstOptimalTime - startTime);
                        }

                        if (pendingReads.decrementAndGet() == 0) {
                            // All recipes processed
                            if (!harvestPhValues.isEmpty()) {
                                float sum = 0;
                                for (Float ph : harvestPhValues) {
                                    sum += ph;
                                }
                                insights.setAvgPhAtHarvest(sum / harvestPhValues.size());
                            }

                            if (!optimalPhValues.isEmpty()) {
                                float min = Float.MAX_VALUE;
                                float max = Float.MIN_VALUE;
                                for (Float ph : optimalPhValues) {
                                    min = Math.min(min, ph);
                                    max = Math.max(max, ph);
                                }
                                insights.setOptimalPhRangeMin(min);
                                insights.setOptimalPhRangeMax(max);
                            }

                            if (!timeToOptimalPh.isEmpty()) {
                                long sum = 0;
                                for (Long time : timeToOptimalPh) {
                                    sum += time;
                                }
                                float avgMillis = (float) sum / timeToOptimalPh.size();
                                float avgDays = avgMillis / (1000 * 60 * 60 * 24);
                                insights.setAvgTimeToOptimalPhDays(avgDays);
                            }

                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load pH readings for recipe: " +
                                recipe.getRecipeId(), e);
                        if (pendingReads.decrementAndGet() == 0) {
                            onComplete.run();
                        }
                    });
        }
    }

    private String extractTeaType(String teaLeaf) {
        // Return the full tea name for better readability in charts
        // Just trim whitespace
        return teaLeaf != null ? teaLeaf.trim() : "";
    }

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

    public interface OnInsightsLoadedListener {
        void onSuccess(DataInsights insights);
        void onFailure(String error);
    }
}
