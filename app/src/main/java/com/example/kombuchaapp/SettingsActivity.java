package com.example.kombuchaapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    // UI Components - Account Section
    private EditText etUsername, etEmail, etPassword;
    private CheckBox cbShowPassword;
    private Button btnSaveAccount;

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

    // SharedPreferences for local storage
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_THEME_COLOR = "theme_color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Load saved settings
        loadSettings();
    }

    private void initializeViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

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
            // Move cursor to end
            etPassword.setSelection(etPassword.getText().length());
        });

        // Save Account Button
        btnSaveAccount.setOnClickListener(v -> saveAccountInfo());

        // Temperature Unit Selection
        groupUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.opt_celsius) ? "celsius" : "fahrenheit";
            savePreference(KEY_TEMP_UNIT, unit);
            Toast.makeText(this, "Temperature unit updated", Toast.LENGTH_SHORT).show();
        });

        // Font Size SeekBar
        seekFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFontPreview(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // To be implemented if needed.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                savePreference(KEY_FONT_SIZE, seekBar.getProgress());
                Toast.makeText(SettingsActivity.this, "Font size saved", Toast.LENGTH_SHORT).show();
            }
        });

        // Color Theme Selection
        groupColors.setOnCheckedChangeListener((group, checkedId) -> {
            String colorTheme = getColorThemeFromId(checkedId);
            savePreference(KEY_THEME_COLOR, colorTheme);
            applyColorTheme(colorTheme);
            Toast.makeText(this, "Theme updated", Toast.LENGTH_SHORT).show();
        });
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

        // TODO: Firebase integration will go here
        // For now, it just show a success message

        Toast.makeText(this, "Account information saved (Firebase integration pending)", Toast.LENGTH_LONG).show();

        // Clear password field for security
        etPassword.setText("");
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void loadSettings() {
        // Load temperature unit
        String tempUnit = sharedPreferences.getString(KEY_TEMP_UNIT, "celsius");
        if (tempUnit.equals("celsius")) {
            optCelsius.setChecked(true);
        } else {
            optFahrenheit.setChecked(true);
        }

        // Load font size
        int fontSize = sharedPreferences.getInt(KEY_FONT_SIZE, 0);
        seekFont.setProgress(fontSize);
        updateFontPreview(fontSize);

        // Load color theme
        String colorTheme = sharedPreferences.getString(KEY_THEME_COLOR, "purple");
        setColorThemeRadioButton(colorTheme);
        applyColorTheme(colorTheme);

        // TODO: Load user information from Firebase when integrated
        // For now, fields remain empty
    }

    private void updateFontPreview(int progress) {
        // Base font size is 12sp, max is 67sp (12 + 55)
        float fontSize = 12 + progress;
        txtFontPreview.setTextSize(fontSize);

        // Calculate percentage (100% = 16sp, max ~420%)
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

    private void savePreference(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void savePreference(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    // ===== FIREBASE INTEGRATION METHODS (TO BE IMPLEMENTED) =====

    // This method should update user information in Firebase
    private void updateUserInfoInFirebase(String username, String email, String password) {
        // Firebase implementation to update the user info will go here
    }

    //This method should load user information from Firebase
    private void loadUserInfoFromFirebase() {
        // Firebase implementation to load the user info will go here
    }
}