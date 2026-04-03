package com.example.smartaccesstracker;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvCreateAccount;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase initialize
        mAuth = FirebaseAuth.getInstance();

        // Views initialize
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        progressBar = findViewById(R.id.progressBar);

        // Login button click - Lambda expression
        btnLogin.setOnClickListener(v -> loginUser());

        // Create Account link click - Lambda expression
        tvCreateAccount.setOnClickListener(v -> {
            // RegisterActivity pe jao
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Attempting login for: " + email);

        // ============ VALIDATION ============
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

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Show loading
        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Firebase - Sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Login successful!");

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // ✅ EMAIL VERIFIED CHECK
                            if (user.isEmailVerified()) {
                                // Email verified hai - Dashboard pe bhejo
                                Log.d(TAG, "✅ Email is verified. Redirecting to Dashboard...");

                                Toast.makeText(LoginActivity.this,
                                        "Welcome back, " + user.getDisplayName() + "!",
                                        Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                            } else {
                                // Email verified NAHI hai
                                Log.d(TAG, "⚠️ Email NOT verified!");

                                Toast.makeText(LoginActivity.this,
                                        "⚠️ Please verify your email first. Check your inbox for verification link.",
                                        Toast.LENGTH_LONG).show();

                                // User ko sign out kar do
                                mAuth.signOut();
                            }
                        }

                    } else {
                        // ✅ LOGIN FAILED - USER-FRIENDLY MESSAGE
                        Log.e(TAG, "❌ Login failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));

                        // Simple, clear message for user
                        Toast.makeText(LoginActivity.this,
                                "❌ Login failed! Invalid email or password. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // Already logged in and verified - directly send to dashboard
            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }
    }
}