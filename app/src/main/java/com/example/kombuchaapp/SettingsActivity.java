package com.example.kombuchaapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.kombuchaapp.models.UserSettings;
import com.example.kombuchaapp.repositories.SettingsRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI Components - Account Section
    private EditText etUsername, etEmail, etPassword;
    private CheckBox cbShowPassword;
    private Button btnSaveAccount;
    private ProgressBar progressBar;

    // UI Components - Units Section
    private RadioGroup groupUnits;
    private RadioButton optCelsius, optFahrenheit;

    // UI Components - Font Section
    private SeekBar seekFont;
    private TextView txtFontPreview;

    // UI Components - Appearance Section
    private RadioGroup groupColors;
    private RadioButton optPurple, optGray, optBlue, optGreen;
    private Toolbar toolbar;

    // Backend components
    private SettingsRepository settingsRepository;
    private FirebaseAuth firebaseAuth;
    private UserSettings currentSettings;

    // SharedPreferences for local caching
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_THEME_COLOR = "theme_color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Initialize Firebase and Repository
        firebaseAuth = FirebaseAuth.getInstance();
        settingsRepository = new SettingsRepository();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Load settings from Firebase
        loadSettingsFromFirebase();
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Progress bar for loading states
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        // Account Section
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        cbShowPassword = findViewById(R.id.cb_show_password);
        btnSaveAccount = findViewById(R.id.btn_save_account);

        // Units Section
        groupUnits = findViewById(R.id.group_units);
        optCelsius = findViewById(R.id.opt_celsius);
        optFahrenheit = findViewById(R.id.opt_fahrenheit);

        // Font Section
        seekFont = findViewById(R.id.seek_font);
        txtFontPreview = findViewById(R.id.txt_font_preview);

        // Appearance Section
        groupColors = findViewById(R.id.group_colors);
        optPurple = findViewById(R.id.opt_purple);
        optGray = findViewById(R.id.opt_gray);
        optBlue = findViewById(R.id.opt_blue);
        optGreen = findViewById(R.id.opt_green);
    }

    private void setupListeners() {
        // Show/Hide Password
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Save Account Button
        btnSaveAccount.setOnClickListener(v -> saveAccountInfo());

        // Temperature Unit Selection
        groupUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.opt_celsius) ? "celsius" : "fahrenheit";
            updateTemperatureUnit(unit);
        });

        // Font Size SeekBar
        seekFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFontPreview(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateFontSize(seekBar.getProgress());
            }
        });

        // Color Theme Selection
        groupColors.setOnCheckedChangeListener((group, checkedId) -> {
            String colorTheme = getColorThemeFromId(checkedId);
            updateColorTheme(colorTheme);
        });
    }

    private void loadSettingsFromFirebase() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        
        if (user == null) {
            Toast.makeText(this, "Please sign in to access settings", Toast.LENGTH_LONG).show();
            loadLocalSettings(); // Fallback to local settings
            return;
        }

        showLoading(true);
        
        settingsRepository.loadSettings(new SettingsRepository.OnSettingsLoadListener() {
            @Override
            public void onSuccess(UserSettings settings) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentSettings = settings;
                    displaySettings(settings);
                    Log.d(TAG, "Settings loaded from Firebase: " + settings);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load settings: " + error);
                    Toast.makeText(SettingsActivity.this, 
                            "Using local settings. " + error, 
                            Toast.LENGTH_SHORT).show();
                    loadLocalSettings(); // Fallback
                });
            }
        });
    }

    private void displaySettings(UserSettings settings) {
        // Display account info
        etUsername.setText(settings.getUsername());
        etEmail.setText(settings.getEmail());

        // Display temperature unit
        if ("celsius".equals(settings.getTemperatureUnit())) {
            optCelsius.setChecked(true);
        } else {
            optFahrenheit.setChecked(true);
        }

        // Display font size
        seekFont.setProgress(settings.getFontSize());
        updateFontPreview(settings.getFontSize());

        // Display color theme
        setColorThemeRadioButton(settings.getThemeColor());
        applyColorTheme(settings.getThemeColor());

        // Save to local cache
        cacheSettingsLocally(settings);
    }

    private void saveAccountInfo() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic validation
        if (username.isEmpty() && email.isEmpty() && password.isEmpty()) {
            Toast.makeText(this, "Please enter at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.isEmpty() && !isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to save account info", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);

        // Update username and email in Firestore
        settingsRepository.updateAccountInfo(username, email, 
                new SettingsRepository.OnSettingsOperationListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    
                    // Update Firebase Auth email if changed
                    if (!email.isEmpty() && !email.equals(user.getEmail())) {
                        updateFirebaseAuthEmail(email);
                    }
                    
                    // Update password if provided
                    if (!password.isEmpty()) {
                        updateFirebaseAuthPassword(password);
                    }
                    
                    etPassword.setText(""); // Clear password field
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(SettingsActivity.this, 
                            "Failed to save: " + error, 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateTemperatureUnit(String unit) {
        savePreferenceLocally(KEY_TEMP_UNIT, unit);
        
        if (currentSettings != null) {
            settingsRepository.updateSetting("temperatureUnit", unit, 
                    new SettingsRepository.OnSettingsOperationListener() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> 
                        Toast.makeText(SettingsActivity.this, 
                                "Temperature unit updated", 
                                Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to update temperature unit: " + error);
                }
            });
        }
    }

    private void updateFontSize(int fontSize) {
        savePreferenceLocally(KEY_FONT_SIZE, fontSize);
        
        if (currentSettings != null) {
            settingsRepository.updateSetting("fontSize", fontSize, 
                    new SettingsRepository.OnSettingsOperationListener() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> 
                        Toast.makeText(SettingsActivity.this, 
                                "Font size saved", 
                                Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to update font size: " + error);
                }
            });
        }
    }

    private void updateColorTheme(String colorTheme) {
        savePreferenceLocally(KEY_THEME_COLOR, colorTheme);
        applyColorTheme(colorTheme);
        
        if (currentSettings != null) {
            settingsRepository.updateSetting("themeColor", colorTheme, 
                    new SettingsRepository.OnSettingsOperationListener() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> 
                        Toast.makeText(SettingsActivity.this, 
                                "Theme updated", 
                                Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to update theme: " + error);
                }
            });
        }
    }

    private void updateFirebaseAuthEmail(String newEmail) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.updateEmail(newEmail)
                    .addOnSuccessListener(aVoid -> 
                        Toast.makeText(this, "Email updated in Firebase Auth", 
                                Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to update email: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show()
                    );
        }
    }

    private void updateFirebaseAuthPassword(String newPassword) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid -> 
                        Toast.makeText(this, "Password updated successfully", 
                                Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to update password: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show()
                    );
        }
    }

    private void loadLocalSettings() {
        // Load from SharedPreferences as fallback
        String tempUnit = sharedPreferences.getString(KEY_TEMP_UNIT, "celsius");
        int fontSize = sharedPreferences.getInt(KEY_FONT_SIZE, 0);
        String colorTheme = sharedPreferences.getString(KEY_THEME_COLOR, "purple");

        if ("celsius".equals(tempUnit)) {
            optCelsius.setChecked(true);
        } else {
            optFahrenheit.setChecked(true);
        }

        seekFont.setProgress(fontSize);
        updateFontPreview(fontSize);

        setColorThemeRadioButton(colorTheme);
        applyColorTheme(colorTheme);
    }

    private void cacheSettingsLocally(UserSettings settings) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TEMP_UNIT, settings.getTemperatureUnit());
        editor.putInt(KEY_FONT_SIZE, settings.getFontSize());
        editor.putString(KEY_THEME_COLOR, settings.getThemeColor());
        editor.apply();
    }

    private void updateFontPreview(int progress) {
        float fontSize = 12 + progress;
        txtFontPreview.setTextSize(fontSize);
        int percentage = (int) ((fontSize / 16.0f) * 100);
        txtFontPreview.setText("Preview • " + percentage + "%");
    }

    private String getColorThemeFromId(int checkedId) {
        if (checkedId == R.id.opt_purple) return "purple";
        if (checkedId == R.id.opt_gray) return "gray";
        if (checkedId == R.id.opt_blue) return "blue";
        if (checkedId == R.id.opt_green) return "green";
        return "purple";
    }

    private void setColorThemeRadioButton(String colorTheme) {
        switch (colorTheme) {
            case "purple":
                optPurple.setChecked(true);
                break;
            case "gray":
                optGray.setChecked(true);
                break;
            case "blue":
                optBlue.setChecked(true);
                break;
            case "green":
                optGreen.setChecked(true);
                break;
            default:
                optPurple.setChecked(true);
        }
    }

    private void applyColorTheme(String colorTheme) {
        int color;
        switch (colorTheme) {
            case "purple":
                color = Color.parseColor("#4A148C");
                break;
            case "gray":
                color = Color.parseColor("#424242");
                break;
            case "blue":
                color = Color.parseColor("#0D47A1");
                break;
            case "green":
                color = Color.parseColor("#1B5E20");
                break;
            default:
                color = Color.parseColor("#4A148C");
        }
        toolbar.setBackgroundColor(color);
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void savePreferenceLocally(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void savePreferenceLocally(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    private void showLoading(boolean show) {
        if (btnSaveAccount != null) {
            btnSaveAccount.setEnabled(!show);
        }
    }
}
