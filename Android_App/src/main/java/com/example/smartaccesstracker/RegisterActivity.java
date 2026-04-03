package com.example.smartaccesstracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;

    // Firebase Authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase initialize
        mAuth = FirebaseAuth.getInstance();

        // Views initialize
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);

        // Register button click listener
        btnRegister.setOnClickListener(v -> registerUser());

        // Login link click listener
        tvLoginLink.setOnClickListener(v -> {
            // LoginActivity pe wapas jao
            finish();
        });
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        Log.d(TAG, "Starting registration for email: " + email);

        // ============ VALIDATION CHECKS ============

        // 1. Full Name check
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Name is required");
            etFullName.requestFocus();
            return;
        }

        if (fullName.length() < 3) {
            etFullName.setError("Name must be at least 3 characters");
            etFullName.requestFocus();
            return;
        }

        // 2. Email check
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // 3. Password check
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // 4. Confirm Password check
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // ============ ALL VALIDATIONS PASSED ============

        // Show loading
        btnRegister.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Log.d(TAG, "Validations passed, creating user...");

        // Firebase - Create new user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ User created successfully!");

                        // Registration successful ✅
                        FirebaseUser user = mAuth.getCurrentUser();

                        // User ke profile mein name save karo
                        if (user != null) {
                            Log.d(TAG, "User UID: " + user.getUid());

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "✅ Profile updated successfully!");

                                            // ✅ EMAIL VERIFICATION BHEJO
                                            Log.d(TAG, "Sending verification email to: " + email);

                                            user.sendEmailVerification()
                                                    .addOnCompleteListener(verificationTask -> {
                                                        progressBar.setVisibility(View.GONE);
                                                        btnRegister.setEnabled(true);

                                                        if (verificationTask.isSuccessful()) {
                                                            Log.d(TAG, "✅ VERIFICATION EMAIL SENT SUCCESSFULLY!");

                                                            // ✅ DIALOG BOX DIKHAO - REGISTER PAGE PE HI RUKENGE
                                                            new android.app.AlertDialog.Builder(RegisterActivity.this)
                                                                    .setTitle("✅ Account Created!")
                                                                    .setMessage("Your account has been created successfully!\n\n" +
                                                                            "📧 A verification email has been sent to:\n" +
                                                                            email + "\n\n" +
                                                                            "Please check your inbox (and spam folder) and click the verification link.\n\n" +
                                                                            "After verification, come back to the app and login.")
                                                                    .setPositiveButton("OK, Got it!", (dialog, which) -> {
                                                                        // Dialog close hone pe kuch nahi karna - user Register page pe hi rahega
                                                                        dialog.dismiss();
                                                                    })
                                                                    .setCancelable(false)
                                                                    .show();

                                                            // ✅ Sign out kar do (security ke liye)
                                                            mAuth.signOut();

                                                        } else {
                                                            // ❌ Email sending failed
                                                            String error = verificationTask.getException() != null ?
                                                                    verificationTask.getException().getMessage() : "Unknown error";

                                                            Log.e(TAG, "❌ FAILED TO SEND EMAIL: " + error);

                                                            Toast.makeText(RegisterActivity.this,
                                                                    "⚠️ Account created but email failed to send. Error: " + error,
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        } else {
                                            progressBar.setVisibility(View.GONE);
                                            btnRegister.setEnabled(true);

                                            String error = profileTask.getException() != null ?
                                                    profileTask.getException().getMessage() : "Unknown error";
                                            Log.e(TAG, "❌ Profile update failed: " + error);

                                            Toast.makeText(RegisterActivity.this,
                                                    "Failed to update profile: " + error,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                    } else {
                        // Registration failed ❌
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Log.e(TAG, "❌ Registration failed: " + errorMessage);

                        Toast.makeText(RegisterActivity.this,
                                "Error: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}