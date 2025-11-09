package com.example.kombuchaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    EditText mEmail, mPassword;
    Button mLoginBtn;
    TextView mCreateBtn, forgotTextLink;
    FirebaseAuth fAuth;

    private static final long SPLASH_DURATION_MS = 650L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mEmail = findViewById(R.id.Email);
        mPassword = findViewById(R.id.password);
        mLoginBtn = findViewById(R.id.loginBtn);
        mCreateBtn = findViewById(R.id.createText);
        forgotTextLink = findViewById(R.id.forgotPassword);

        fAuth = FirebaseAuth.getInstance();

        // Ask notification permission once
        ensurePostNotificationsPermission();
        if (fAuth.getCurrentUser() != null) {
            showSplashThenGoToMain();
            return;
        }

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    mEmail.setError("Email is Required.");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    mPassword.setError("Password is Required.");
                    return;
                }

                if (password.length() < 6) {
                    mPassword.setError("Password Must be >= 6 Characters");
                    return;
                }

                fAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(Login.this, "Logged in Successfully", Toast.LENGTH_SHORT).show();
                                    showSplashThenGoToMain();
                                } else {
                                    Toast.makeText(Login.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Register.class));
            }
        });

        forgotTextLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ForgotPassword.class));
            }
        });
    }

    private void ensurePostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            SharedPreferences sp = getSharedPreferences("kombucha_prefs", MODE_PRIVATE);
            boolean alreadyAsked = sp.getBoolean("asked_post_notifications_v1", false);

            if (!alreadyAsked) {
                sp.edit().putBoolean("asked_post_notifications_v1", true).apply();

                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
                }
            }
        }
    }

    private void showSplashThenGoToMain() {
        FrameLayout splashRoot = new FrameLayout(this);
        splashRoot.setBackgroundColor(Color.parseColor("#F4B266"));

        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.app_logo);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            float radius = 30f;
            bg.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
        splashRoot.addView(bg);

        ImageView centerLogo = new ImageView(this);
        centerLogo.setImageResource(R.drawable.app_logo);
        int logoSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams logoLp = new FrameLayout.LayoutParams(logoSizePx, logoSizePx);
        logoLp.gravity = Gravity.CENTER;
        centerLogo.setLayoutParams(logoLp);
        splashRoot.addView(centerLogo);

        setContentView(splashRoot);

        splashRoot.postDelayed(() -> {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }
}