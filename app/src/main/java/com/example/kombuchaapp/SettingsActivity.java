package com.example.kombuchaapp;

import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.kombuchaapp.models.UserSettings;
import com.example.kombuchaapp.repositories.SettingsRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private EditText etUsername, etEmail, etPassword;
    private CheckBox cbShowPassword;
    private Button btnSaveAccount;
    private RadioGroup groupUnits, groupColors;
    private RadioButton optCelsius, optFahrenheit;
    private RadioButton optPurple, optGray, optBlue, optGreen;
    private SeekBar seekFont;
    private TextView txtFontPreview;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    // Backend - Firebase Auth (same as login system)
    private FirebaseAuth fAuth;
    private SettingsRepository settingsRepo;
    private UserSettings currentSettings;

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

        initViews();
        setupListeners();

        loadSettings();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        cbShowPassword = findViewById(R.id.cb_show_password);
        btnSaveAccount = findViewById(R.id.btn_save_account);

        groupUnits = findViewById(R.id.group_units);
        optCelsius = findViewById(R.id.opt_celsius);
        optFahrenheit = findViewById(R.id.opt_fahrenheit);

        seekFont = findViewById(R.id.seek_font);
        txtFontPreview = findViewById(R.id.txt_font_preview);

        groupColors = findViewById(R.id.group_colors);
        optPurple = findViewById(R.id.opt_purple);
        optGray = findViewById(R.id.opt_gray);
        optBlue = findViewById(R.id.opt_blue);
        optGreen = findViewById(R.id.opt_green);
    }

    private void setupListeners() {
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnSaveAccount.setOnClickListener(v -> saveAccountInfo());

        groupUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.opt_celsius) ? "celsius" : "fahrenheit";
            saveTemperatureUnit(unit);
        });

        seekFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFontPreview(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveFontSize(seekBar.getProgress());
            }
        });

        groupColors.setOnCheckedChangeListener((group, checkedId) -> {
            String color = getColorFromRadioId(checkedId);
            saveThemeColor(color);
        });
    }

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

        seekFont.setProgress(settings.getFontSize());
        updateFontPreview(settings.getFontSize());

        setColorRadioButton(settings.getThemeColor());
        applyThemeColor(settings.getThemeColor());
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
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) && TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter at least one field to update", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (!TextUtils.isEmpty(password) && password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        showLoading(true);

        if (!TextUtils.isEmpty(username) && !username.equals(currentSettings.getfName())) {
            settingsRepo.updateUsername(username, new SettingsRepository.OnUpdateListener() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                        currentSettings.setfName(username);
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

        if (!TextUtils.isEmpty(email)) {
            updateFirebaseAuthEmail(email);
        }

        if (!TextUtils.isEmpty(password)) {
            updateFirebaseAuthPassword(password);
        }

        showLoading(false);
        etPassword.setText(""); 
    }

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


    private void updateFirebaseAuthPassword(String newPassword) {
        FirebaseUser user = fAuth.getCurrentUser();
        
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        user.updatePassword(newPassword)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Password updated in Firebase Auth");
                    Toast.makeText(SettingsActivity.this, 
                        "Password updated successfully", 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to update password: " + e.getMessage());
                    Toast.makeText(SettingsActivity.this, 
                        "Password update failed: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
            });
    }


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


    private void saveFontSize(int fontSize) {
        settingsRepo.updateFontSize(fontSize, new SettingsRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Font size saved: " + fontSize);
                cachePreference("fontSize", fontSize);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(SettingsActivity.this, 
                    "Failed to save: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void saveThemeColor(String color) {
        settingsRepo.updateThemeColor(color, new SettingsRepository.OnUpdateListener() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Theme color saved: " + color);
                cachePreference("themeColor", color);
                applyThemeColor(color);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(SettingsActivity.this, 
                    "Failed to save: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFontPreview(int progress) {
        float fontSize = 12 + progress;
        txtFontPreview.setTextSize(fontSize);
        int percentage = (int) ((fontSize / 16.0f) * 100);
        txtFontPreview.setText("Preview • " + percentage + "%");
    }

    private String getColorFromRadioId(int id) {
        if (id == R.id.opt_purple) return "purple";
        if (id == R.id.opt_gray) return "gray";
        if (id == R.id.opt_blue) return "blue";
        if (id == R.id.opt_green) return "green";
        return "purple";
    }

    private void setColorRadioButton(String color) {
        switch (color) {
            case "purple": optPurple.setChecked(true); break;
            case "gray": optGray.setChecked(true); break;
            case "blue": optBlue.setChecked(true); break;
            case "green": optGreen.setChecked(true); break;
            default: optPurple.setChecked(true); break;
        }
    }

    private void applyThemeColor(String color) {
        int colorInt;
        switch (color) {
            case "purple": colorInt = Color.parseColor("#4A148C"); break;
            case "gray": colorInt = Color.parseColor("#424242"); break;
            case "blue": colorInt = Color.parseColor("#0D47A1"); break;
            case "green": colorInt = Color.parseColor("#1B5E20"); break;
            default: colorInt = Color.parseColor("#4A148C"); break;
        }
        toolbar.setBackgroundColor(colorInt);
    }

    private void cacheSettingsLocally(UserSettings settings) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("fName", settings.getfName());
        editor.putString("email", settings.getEmail());
        editor.putString("temperatureUnit", settings.getTemperatureUnit());
        editor.putInt("fontSize", settings.getFontSize());
        editor.putString("themeColor", settings.getThemeColor());
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
        int fontSize = sharedPrefs.getInt("fontSize", 16);
        String themeColor = sharedPrefs.getString("themeColor", "purple");

        UserSettings cachedSettings = new UserSettings();
        cachedSettings.setfName(name);
        cachedSettings.setEmail(email);
        cachedSettings.setTemperatureUnit(tempUnit);
        cachedSettings.setFontSize(fontSize);
        cachedSettings.setThemeColor(themeColor);

        displaySettings(cachedSettings);
    }

    private void showLoading(boolean show) {
        if (btnSaveAccount != null) {
            btnSaveAccount.setEnabled(!show);
        }
    }
}
