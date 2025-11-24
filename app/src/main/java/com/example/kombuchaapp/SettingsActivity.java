package com.example.kombuchaapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.kombuchaapp.models.UserSettings;
import com.example.kombuchaapp.repositories.SettingsRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI Components
    private EditText etUsername, etEmail, etCurrentPassword, etPassword;
    private CheckBox cbShowPassword;
    private Button btnSaveAccount;
    private RadioGroup groupUnits;
    private RadioButton optCelsius, optFahrenheit;
    private Toolbar toolbar;

    // Backend - Firebase Auth (same as login system)
    private FirebaseAuth fAuth;
    private SettingsRepository settingsRepo;
    private UserSettings currentSettings;

    // Local cache
    private SharedPreferences sharedPrefs;
    private static final String PREFS_NAME = "SettingsCache";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Initialize Firebase Auth (same as Login.java)
        fAuth = FirebaseAuth.getInstance();
        settingsRepo = new SettingsRepository();
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize UI
        initViews();
        setupListeners();

        // Handle back press with modern API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FizzTransitionUtil.play(SettingsActivity.this, SettingsActivity.this::finish);
            }
        });

        // Load settings
        loadSettings();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> FizzTransitionUtil.play(SettingsActivity.this, this::finish)
        );

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etCurrentPassword = findViewById(R.id.et_current_password);
        etPassword = findViewById(R.id.et_password);
        cbShowPassword = findViewById(R.id.cb_show_password);
        btnSaveAccount = findViewById(R.id.btn_save_account);

        groupUnits = findViewById(R.id.group_units);
        optCelsius = findViewById(R.id.opt_celsius);
        optFahrenheit = findViewById(R.id.opt_fahrenheit);
    }

    private void setupListeners() {
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Keep cursor at end of both fields
            etCurrentPassword.setSelection(etCurrentPassword.getText().length());
            etPassword.setSelection(etPassword.getText().length());
        });

        btnSaveAccount.setOnClickListener(v -> saveAccountInfo());

        groupUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.opt_celsius) ? "celsius" : "fahrenheit";
            saveTemperatureUnit(unit);
        });
    }

    /**
     * Load settings (username from Firestore, email from Firebase Auth)
     */
    private void loadSettings() {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        settingsRepo.getUserSettings(new SettingsRepository.OnSettingsLoadedListener() {
            @Override
            public void onSuccess(UserSettings settings) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentSettings = settings;
                    displaySettings(settings);
                    cacheSettingsLocally(settings);
                    Log.d(TAG, "Settings loaded: " + settings.toString());
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(SettingsActivity.this,
                            "Error loading settings: " + error,
                            Toast.LENGTH_SHORT).show();
                    loadCachedSettings();
                });
            }
        });
    }

    private void displaySettings(UserSettings settings) {
        etUsername.setText(settings.getfName());
        etEmail.setText(settings.getEmail());

        if ("celsius".equals(settings.getTemperatureUnit())) {
            optCelsius.setChecked(true);
        } else {
            optFahrenheit.setChecked(true);
        }
    }

    /**
     * Save account info
     * Username → Firestore (via repository)
     * Email → Firebase Auth directly (same as ForgotPassword.java)
     * Password → Firebase Auth directly (same as ForgotPassword.java)
     */
    private void saveAccountInfo() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) && TextUtils.isEmpty(email) && TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Please enter at least one field to update", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        // Validate password - require current password if changing password
        if (!TextUtils.isEmpty(newPassword)) {
            if (TextUtils.isEmpty(currentPassword)) {
                etCurrentPassword.setError("Current password is required");
                return;
            }
            if (newPassword.length() < 6) {
                etPassword.setError("New password must be at least 6 characters");
                return;
            }
        }

        showLoading(true);

        // Update username in Firestore (ONLY if changed)
        if (!TextUtils.isEmpty(username) && (currentSettings == null || !username.equals(currentSettings.getfName()))) {
            settingsRepo.updateUsername(username, new SettingsRepository.OnUpdateListener() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                        if (currentSettings != null) {
                            currentSettings.setfName(username);
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this,
                                "Failed to update username: " + error,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        // Update email in Firebase Auth directly (same pattern as ForgotPassword.java)
        if (!TextUtils.isEmpty(email)) {
            updateFirebaseAuthEmail(email);
        }

        // Update password - requires re-authentication with current password
        if (!TextUtils.isEmpty(newPassword) && !TextUtils.isEmpty(currentPassword)) {
            updateFirebaseAuthPassword(currentPassword, newPassword);
        }

        showLoading(false);
        etCurrentPassword.setText(""); // Clear current password field
        etPassword.setText(""); // Clear new password field
    }

    /**
     * Update email in Firebase Auth (same as login system)
     */
    private void updateFirebaseAuthEmail(String newEmail) {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        user.updateEmail(newEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Email updated in Firebase Auth");
                        Toast.makeText(SettingsActivity.this,
                                "Email updated successfully",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to update email: " + e.getMessage());
                        Toast.makeText(SettingsActivity.this,
                                "Email update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Update password in Firebase Auth
     * Re-authenticates with current password first, then updates to new password
     */
    private void updateFirebaseAuthPassword(String currentPassword, String newPassword) {
        FirebaseUser user = fAuth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create credential with current email and password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        // Re-authenticate the user first
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Re-authentication successful, now update password
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Password updated in Firebase Auth");
                                Toast.makeText(SettingsActivity.this,
                                        "Password updated successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update password: " + e.getMessage());
                                Toast.makeText(SettingsActivity.this,
                                        "Password update failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Re-authentication failed: " + e.getMessage());
                    Toast.makeText(SettingsActivity.this,
                            "Current password is incorrect",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Save temperature unit to Firestore
     */
    private void saveTemperatureUnit(String unit) {
        settingsRepo.updateTemperatureUnit(unit, new SettingsRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Temperature unit saved: " + unit);
                cachePreference("temperatureUnit", unit);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(SettingsActivity.this,
                        "Failed to save: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cacheSettingsLocally(UserSettings settings) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("fName", settings.getfName());
        editor.putString("email", settings.getEmail());
        editor.putString("temperatureUnit", settings.getTemperatureUnit());
        editor.apply();
    }

    private void cachePreference(String key, Object value) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        }
        editor.apply();
    }

    private void loadCachedSettings() {
        String name = sharedPrefs.getString("fName", "");
        String email = sharedPrefs.getString("email", "");
        String tempUnit = sharedPrefs.getString("temperatureUnit", "celsius");

        UserSettings cachedSettings = new UserSettings();
        cachedSettings.setfName(name);
        cachedSettings.setEmail(email);
        cachedSettings.setTemperatureUnit(tempUnit);

        displaySettings(cachedSettings);
    }

    private void showLoading(boolean show) {
        if (btnSaveAccount != null) {
            btnSaveAccount.setEnabled(!show);
        }
    }
}
