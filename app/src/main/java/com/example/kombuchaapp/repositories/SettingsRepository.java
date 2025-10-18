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


public class SettingsRepository {
    
    private static final String TAG = "SettingsRepository";
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userID;

    public SettingsRepository() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

 
    public void getUserSettings(OnSettingsLoadedListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();
        
        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }
        
        userID = user.getUid();
        

        DocumentReference docRef = fStore.collection("users").document(userID);
        
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                UserSettings settings = parseSettings(documentSnapshot);
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


    private UserSettings parseSettings(DocumentSnapshot doc) {
        UserSettings settings = new UserSettings();
        
        settings.setUserId(doc.getId());
        settings.setfName(doc.getString("fName") != null ? doc.getString("fName") : "");
        settings.setEmail(doc.getString("email") != null ? doc.getString("email") : "");
      
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

    public void updateAccountInfo(String newName, String newEmail, OnUpdateListener listener) {
        FirebaseUser user = fAuth.getCurrentUser();
        
        if (user == null) {
            listener.onFailure("User not logged in");
            return;
        }
        
        userID = user.getUid();
        DocumentReference docRef = fStore.collection("users").document(userID);
        
        Map<String, Object> updates = new HashMap<>();
        
        if (newName != null && !newName.trim().isEmpty()) {
            updates.put("fName", newName);
        }
        
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            updates.put("email", newEmail);
        }
        
        if (updates.isEmpty()) {
            listener.onFailure("No data to update");
            return;
        }
        
        docRef.update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Account info updated");
                listener.onSuccess("Account information updated successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Update failed: " + e.getMessage());
                listener.onFailure(e.getMessage());
            });
    }


    public void updateTemperatureUnit(String unit, OnUpdateListener listener) {
        updateSingleField("temperatureUnit", unit, listener);
    }


    public void updateFontSize(int fontSize, OnUpdateListener listener) {
        updateSingleField("fontSize", fontSize, listener);
    }


    public void updateThemeColor(String color, OnUpdateListener listener) {
        updateSingleField("themeColor", color, listener);
    }


    private void updateSingleField(String fieldName, Object value, OnUpdateListener listener) {
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


    public void initializeDefaultSettings(String userId, OnUpdateListener listener) {
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("temperatureUnit", "celsius");
        defaultSettings.put("fontSize", 16);
        defaultSettings.put("themeColor", "purple");
        
        fStore.collection("users").document(userId)
            .update(defaultSettings)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Default settings initialized for user: " + userId);
                listener.onSuccess("Settings initialized");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to initialize settings: " + e.getMessage());
                listener.onFailure(e.getMessage());
            });
    }


    public interface OnSettingsLoadedListener {
        void onSuccess(UserSettings settings);
        void onFailure(String error);
    }

    public interface OnUpdateListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
