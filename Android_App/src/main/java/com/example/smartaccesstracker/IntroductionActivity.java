package com.example.smartaccesstracker; // Apne package name se replace kar lena

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

public class IntroductionActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);

        // Hide action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Disable back button on splash screen using new API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing - disable back button
            }
        });

        // Handler to start LoginActivity after 2 seconds (using lambda)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Start LoginActivity
            Intent intent = new Intent(IntroductionActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close IntroductionActivity so user can't go back to it
        }, SPLASH_DURATION);
    }
}