package com.example.kombuchaapp.repositories;

import android.util.Log;
import com.example.kombuchaapp.models.UserSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

// Repository for managing user settings in Firebase Firestore
public class SettingsRepository {
    
    private static final String TAG = "SettingsRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_SETTINGS = "settings";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    public SettingsRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    //Save+update
    public void saveSettings(UserSettings settings, OnSettingsOperationListener listener) {
        String userId = getCurrentUserId();
        
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            if (listener != null) {
                listener.onFailure("User not authenticated. Please sign in.");
            }
            return;
        }
        
        settings.setUserId(userId);
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("preferences")
                .set(settings.toMap(), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Settings saved successfully");
                    if (listener != null) {
                        listener.onSuccess("Settings saved successfully");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving settings", e);
                    if (listener != null) {
                        listener.onFailure("Failed to save settings: " + e.getMessage());
                    }
                });
    }
    
    //load user
    public void loadSettings(OnSettingsLoadListener listener) {
        String userId = getCurrentUserId();
        
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            if (listener != null) {
                listener.onFailure("User not authenticated. Please sign in.");
            }
            return;
        }
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("preferences")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserSettings settings = documentSnapshot.toObject(UserSettings.class);
                        Log.d(TAG, "Settings loaded: " + settings);
                        if (listener != null) {
                            listener.onSuccess(settings);
                        }
                    } else {
                        Log.d(TAG, "No settings found, creating default settings");
                        // Create default settings for new user
                        createDefaultSettings(listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading settings", e);
                    if (listener != null) {
                        listener.onFailure("Failed to load settings: " + e.getMessage());
                    }
                });
    }
    
    // uopdate settings
    public void updateSetting(String field, Object value, OnSettingsOperationListener listener) {
        String userId = getCurrentUserId();
        
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("preferences")
                .update(field, value, "updatedAt", com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Setting updated: " + field);
                    if (listener != null) {
                        listener.onSuccess("Setting updated");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating setting", e);
                    if (listener != null) {
                        listener.onFailure("Failed to update: " + e.getMessage());
                    }
                });
    }
    
    //Update User account
  public void updateAccountInfo(String username, String email, OnSettingsOperationListener listener) {
        String userId = getCurrentUserId();
        
        if (userId == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("preferences")
                .update(
                        "username", username,
                        "email", email,
                        "updatedAt", com.google.firebase.Timestamp.now()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Account info updated");
                    if (listener != null) {
                        listener.onSuccess("Account information updated");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating account info", e);
                    if (listener != null) {
                        listener.onFailure("Failed to update account: " + e.getMessage());
                    }
                });
    }
    
    //Default settings
    private void createDefaultSettings(OnSettingsLoadListener listener) {
        String userId = getCurrentUserId();
        FirebaseUser user = auth.getCurrentUser();
        
        if (userId == null || user == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }
        
        UserSettings defaultSettings = new UserSettings(
                userId,
                user.getDisplayName() != null ? user.getDisplayName() : "",
                user.getEmail() != null ? user.getEmail() : ""
        );
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("preferences")
                .set(defaultSettings.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Default settings created");
                    if (listener != null) {
                        listener.onSuccess(defaultSettings);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating default settings", e);
                    if (listener != null) {
                        listener.onFailure("Failed to create settings: " + e.getMessage());
                    }
                });
    }
    
    // Delete all user settings
    public void deleteSettings(OnSettingsOperationListener listener) {
        String userId = getCurrentUserId();
        
        if (userId == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }
        
        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("preferences")
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Settings deleted");
                    if (listener != null) {
                        listener.onSuccess("Settings deleted");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting settings", e);
                    if (listener != null) {
                        listener.onFailure("Failed to delete settings: " + e.getMessage());
                    }
                });
    }
    
    // Callback interfaces
    public interface OnSettingsLoadListener {
        void onSuccess(UserSettings settings);
        void onFailure(String error);
    }
    
    public interface OnSettingsOperationListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
