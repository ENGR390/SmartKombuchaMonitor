package com.example.kombuchaapp.repositories;

import android.util.Log;
import com.example.kombuchaapp.models.UserSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings Repository
 * Uses Firebase Auth for credentials (email/password) - SAME AS LOGIN SYSTEM
 * Uses Firestore ONLY for username and app preferences (theme, font, temperature)
 */
public class SettingsRepository {

    private static final String TAG = "SettingsRepository";
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userID;

    public SettingsRepository() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    /**
     * Get current user's settings
     * Email comes from Firebase Auth (NOT Firestore)
     */
    public void getUserSettings(OnSettingsLoadedListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }

        userID = user.getUid();

        // Read username and preferences from Firestore
        DocumentReference docRef = fStore.collection("users").document(userID);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                UserSettings settings = parseSettings(documentSnapshot, user);
                listener.onSuccess(settings);
                Log.d(TAG, "Settings loaded: " + settings.toString());
            } else {
                listener.onFailure("User document not found");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading settings: " + e.getMessage());
            listener.onFailure(e.getMessage());
        });
    }

    /**
     * Parse Firestore document + Firebase Auth into UserSettings
     */
    private UserSettings parseSettings(DocumentSnapshot doc, FirebaseUser user) {
        UserSettings settings = new UserSettings();

        settings.setUserId(doc.getId());

        // Get username from Firestore
        settings.setfName(doc.getString("fName") != null ? doc.getString("fName") : "");

        // Get email from Firebase Auth (NOT Firestore)
        settings.setEmail(user.getEmail() != null ? user.getEmail() : "");

        // Get app preferences from Firestore
        settings.setTemperatureUnit(
                doc.getString("temperatureUnit") != null ?
                        doc.getString("temperatureUnit") : "celsius"
        );

        Long fontSizeLong = doc.getLong("fontSize");
        settings.setFontSize(fontSizeLong != null ? fontSizeLong.intValue() : 16);

        settings.setThemeColor(
                doc.getString("themeColor") != null ?
                        doc.getString("themeColor") : "purple"
        );

        return settings;
    }

    /**
     * Update username in Firestore ONLY
     * Email/password are handled by Firebase Auth directly
     */
    public void updateUsername(String newName, OnUpdateListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            listener.onFailure("Username cannot be empty");
            return;
        }

        userID = user.getUid();
        DocumentReference docRef = fStore.collection("users").document(userID);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fName", newName);

        // Use set with merge to create field if document doesn't exist
        docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Username updated in Firestore");
                    listener.onSuccess("Username updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update failed: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    /**
     * Update temperature unit in Firestore
     */
    public void updateTemperatureUnit(String unit, OnUpdateListener listener) {
        updatePreference("temperatureUnit", unit, listener);
    }

    /**
     * Update font size in Firestore
     */
    public void updateFontSize(int fontSize, OnUpdateListener listener) {
        updatePreference("fontSize", fontSize, listener);
    }

    /**
     * Update theme color in Firestore
     */
    public void updateThemeColor(String color, OnUpdateListener listener) {
        updatePreference("themeColor", color, listener);
    }

    /**
     * Generic method to update app preferences in Firestore
     */
    private void updatePreference(String fieldName, Object value, OnUpdateListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }

        userID = user.getUid();

        fStore.collection("users").document(userID)
                .update(fieldName, value)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, fieldName + " updated to: " + value);
                    listener.onSuccess("Setting updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update " + fieldName + ": " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // Callback interfaces
    public interface OnSettingsLoadedListener {
        void onSuccess(UserSettings settings);
        void onFailure(String error);
    }

    public interface OnUpdateListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}