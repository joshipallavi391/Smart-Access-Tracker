package com.example.smartaccesstracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TextView tvWelcome, tvTotalStudents, tvPresentToday;
    private Button btnLogout, btnDailyReport, btnMonthlyReport, btnSearch;
    private EditText etSearchStudent;

    private FirebaseAuth mAuth;
    private DatabaseReference attendanceRef;
    private DatabaseReference registeredCardsRef;

    private String currentDate;
    private boolean cardsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Firebase initialize
        mAuth = FirebaseAuth.getInstance();
        attendanceRef = FirebaseDatabase.getInstance().getReference("attendance");
        registeredCardsRef = FirebaseDatabase.getInstance().getReference("registered_cards");

        // Get current date (format: yyyy-MM-dd)
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Log.d(TAG, "Current Date: " + currentDate);

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvPresentToday = findViewById(R.id.tvPresentToday);
        etSearchStudent = findViewById(R.id.etSearchStudent);
        btnSearch = findViewById(R.id.btnSearch);
        btnLogout = findViewById(R.id.btnLogout);
        btnDailyReport = findViewById(R.id.btnDailyReport);
        btnMonthlyReport = findViewById(R.id.btnMonthlyReport);

        // Set welcome message
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvWelcome.setText("Welcome, " + displayName + "!");
            } else {
                tvWelcome.setText("Welcome!");
            }
        }

        // Logout button
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Daily Report button
        btnDailyReport.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, DailyReportActivity.class);
            startActivity(intent);
        });

        // Monthly Report button
        btnMonthlyReport.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MonthlyReportActivity.class);
            startActivity(intent);
        });

        // Search button
        btnSearch.setOnClickListener(v -> {
            String query = etSearchStudent.getText().toString().trim();
            if (!query.isEmpty()) {
                searchStudent(query);
            } else {
                Toast.makeText(this, "Please enter student name or UID", Toast.LENGTH_SHORT).show();
            }
        });

        // Check if cards are initialized, if not - initialize them once
        checkAndInitializeCards();

        // Load dashboard data
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Refreshing dashboard data");
        loadDashboardData();
    }

    /**
     * Check if registered cards exist in Firebase
     * If not, initialize them once automatically
     */
    private void checkAndInitializeCards() {
        registeredCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    Log.d(TAG, "Registered cards not found. Initializing...");
                    initializeRegisteredCards();
                } else {
                    Log.d(TAG, "Registered cards already exist: " + snapshot.getChildrenCount());
                    cardsInitialized = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking registered cards: " + error.getMessage());
            }
        });
    }

    /**
     * Initialize all 11 RFID cards in Firebase
     * This runs automatically once when app first loads
     */
    private void initializeRegisteredCards() {
        Log.d(TAG, "Starting card initialization...");

        // 11 RFID cards with UIDs and student names
        Map<String, String> cards = new HashMap<>();
        cards.put("DD4A3402", "STUDENT 10");
        cards.put("E49F3502", "STUDENT 9");
        cards.put("FAAAB402", "STUDENT 8");
        cards.put("6DA9B402", "STUDENT 7");
        cards.put("21D83D02", "STUDENT 2");
        cards.put("3767B302", "STUDENT 6");
        cards.put("BA034102", "STUDENT 5");
        cards.put("A2BD3D02", "STUDENT 4");
        cards.put("B332FF3E", "STUDENT 1");
        cards.put("B16C3502", "STUDENT 3");
        cards.put("2747ACB5", "STUDENT 11");

        // Save each card to Firebase
        for (Map.Entry<String, String> entry : cards.entrySet()) {
            String uid = entry.getKey();
            String name = entry.getValue();

            Map<String, Object> cardData = new HashMap<>();
            cardData.put("uid", uid);
            cardData.put("name", name);
            cardData.put("status", "active");

            registeredCardsRef.child(uid).setValue(cardData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Card registered: " + uid + " - " + name);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to register card: " + uid, e);
                    });
        }

        Toast.makeText(this, "All 11 cards registered successfully!", Toast.LENGTH_SHORT).show();
        cardsInitialized = true;

        // Refresh dashboard to show updated count
        loadDashboardData();
    }

    private void loadDashboardData() {
        Log.d(TAG, "Loading dashboard data for date: " + currentDate);
        fetchTotalStudents();
        fetchPresentCount();
    }

    /**
     * Fetch total registered students (from registered_cards node)
     * This will always show 11
     */
    private void fetchTotalStudents() {
        registeredCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalCards = snapshot.getChildrenCount();
                Log.d(TAG, "Total registered cards: " + totalCards);
                tvTotalStudents.setText(String.valueOf(totalCards));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching total students: " + error.getMessage());
                Toast.makeText(DashboardActivity.this, "Error loading student count", Toast.LENGTH_SHORT).show();
                tvTotalStudents.setText("0");
            }
        });
    }

    /**
     * Fetch present count for today only
     * This will show how many cards were scanned today
     */
    private void fetchPresentCount() {
        attendanceRef.child(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int presentCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.child("status").getValue(String.class);
                    if (status != null && "PRESENT".equalsIgnoreCase(status)) {
                        presentCount++;
                    }
                }
                Log.d(TAG, "Present today: " + presentCount);
                tvPresentToday.setText(String.valueOf(presentCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching present count: " + error.getMessage());
                Toast.makeText(DashboardActivity.this, "Error loading present count", Toast.LENGTH_SHORT).show();
                tvPresentToday.setText("0");
            }
        });
    }

    private void searchStudent(String query) {
        // Search in today's attendance
        attendanceRef.child(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    String uid = child.child("uid").getValue(String.class);
                    String status = child.child("status").getValue(String.class);

                    if ((name != null && name.toLowerCase().contains(query.toLowerCase())) ||
                            (uid != null && uid.toLowerCase().contains(query.toLowerCase()))) {

                        found = true;
                        String message = "Student: " + name + "\nUID: " + uid + "\nStatus: " + status;
                        Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_LONG).show();
                        break;
                    }
                }

                if (!found) {
                    Toast.makeText(DashboardActivity.this, "No student found with: " + query, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Search failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}