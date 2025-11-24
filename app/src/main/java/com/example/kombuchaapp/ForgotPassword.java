package com.example.kombuchaapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPassword extends AppCompatActivity {
    private static final String TAG = "ForgotPassword";
    EditText mEmail;
    Button mResetBtn;
    TextView mBackBtn;
    FirebaseAuth fAuth;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mEmail = findViewById(R.id.emailForgot);
        mResetBtn = findViewById(R.id.resetBtn);
        mBackBtn = findViewById(R.id.backToLogin);
        progressBar = findViewById(R.id.progressBar);

        fAuth = FirebaseAuth.getInstance();

        mResetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();

                if(TextUtils.isEmpty(email)) {
                    mEmail.setError("Email is Required.");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                mResetBtn.setEnabled(false);

                Log.d(TAG, "Sending password reset email to: " + email);

                fAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Password reset email sent successfully");
                                Toast.makeText(ForgotPassword.this, "Reset Link Sent To Your Email. Check your inbox and spam folder.", Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.GONE);
                                mResetBtn.setEnabled(true);
                                mEmail.setText("");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to send password reset email: " + e.getMessage());
                                Log.e(TAG, "Error type: " + e.getClass().getSimpleName());

                                String userMessage;

                                // Use exception type checking instead of fragile string matching
                                if (e instanceof FirebaseAuthInvalidUserException) {
                                    userMessage = "No account found with this email address.";
                                } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("blocked")) {
                                    userMessage = "Too many reset requests. Please try again later.";
                                } else {
                                    userMessage = "Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                                }

                                Toast.makeText(ForgotPassword.this, userMessage, Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                mResetBtn.setEnabled(true);
                            }
                        });
            }
        });

        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FizzTransitionUtil.play(ForgotPassword.this, () -> {
                    startActivity(new Intent(getApplicationContext(), Login.class));
                    finish();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        FizzTransitionUtil.play(this, ForgotPassword.super::onBackPressed);
    }
}
