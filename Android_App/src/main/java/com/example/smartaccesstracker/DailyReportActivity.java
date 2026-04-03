package com.example.smartaccesstracker;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DailyReportActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvReportDate, tvReportTotal, tvReportPresent, tvReportAbsent, tvReportPercentage;
    private Button btnBack, btnSavePDF, btnShare;
    private RecyclerView rvReportStudents;
    private ProgressBar progressBar;

    // Firebase
    private DatabaseReference attendanceRef;
    private DatabaseReference registeredCardsRef;

    // Data
    private List<Student> allStudents; // All students (present + absent)
    private StudentAdapter studentAdapter;

    // Stats
    private int totalStudents = 0;
    private int presentCount = 0;
    private int absentCount = 0;
    private double attendancePercentage = 0.0;

    // PDF File
    private File pdfFile;

    // Current date
    private String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_report);

        // Initialize Firebase
        attendanceRef = FirebaseDatabase.getInstance().getReference("attendance");
        registeredCardsRef = FirebaseDatabase.getInstance().getReference("registered_cards");

        // Initialize UI
        initializeViews();

        // Set today's date
        setTodayDate();

        // Initialize data lists
        allStudents = new ArrayList<>();

        // Setup RecyclerView
        setupRecyclerView();

        // Load data from Firebase
        loadReportData();

        // Button listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        tvReportDate = findViewById(R.id.tvReportDate);
        tvReportTotal = findViewById(R.id.tvReportTotal);
        tvReportPresent = findViewById(R.id.tvReportPresent);
        tvReportAbsent = findViewById(R.id.tvReportAbsent);
        tvReportPercentage = findViewById(R.id.tvReportPercentage);

        btnBack = findViewById(R.id.btnBack);
        btnSavePDF = findViewById(R.id.btnSavePDF);
        btnShare = findViewById(R.id.btnShare);

        rvReportStudents = findViewById(R.id.rvReportStudents);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setTodayDate() {
        // Format for display: "15 December 2024"
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        String displayDate = displayFormat.format(new Date());
        tvReportDate.setText("📅 Date: " + displayDate);

        // Format for Firebase: "2024-12-15"
        SimpleDateFormat firebaseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        currentDate = firebaseFormat.format(new Date());
    }

    private void setupRecyclerView() {
        rvReportStudents.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentAdapter(allStudents);
        rvReportStudents.setAdapter(studentAdapter);
    }

    private void loadReportData() {
        progressBar.setVisibility(View.VISIBLE);

        // Step 1: Get total registered students count
        registeredCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot registeredSnapshot) {
                totalStudents = (int) registeredSnapshot.getChildrenCount();

                // Step 2: Get today's attendance
                loadTodayAttendance(registeredSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DailyReportActivity.this,
                        "Error loading registered cards: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTodayAttendance(DataSnapshot registeredSnapshot) {
        attendanceRef.child(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                allStudents.clear();
                presentCount = 0;

                // Create a map of all registered students
                Map<String, String> registeredStudents = new HashMap<>();
                for (DataSnapshot cardSnapshot : registeredSnapshot.getChildren()) {
                    String uid = cardSnapshot.child("uid").getValue(String.class);
                    String name = cardSnapshot.child("name").getValue(String.class);
                    if (uid != null && name != null) {
                        registeredStudents.put(uid, name);
                    }
                }

                // Track which students are present
                Set<String> presentUIDs = new HashSet<>();

                // Add present students from today's attendance
                for (DataSnapshot studentSnapshot : attendanceSnapshot.getChildren()) {
                    String uid = studentSnapshot.child("uid").getValue(String.class);
                    String name = studentSnapshot.child("name").getValue(String.class);
                    String status = studentSnapshot.child("status").getValue(String.class);

                    if (uid != null && "PRESENT".equalsIgnoreCase(status)) {
                        presentUIDs.add(uid);

                        Student student = new Student();
                        student.setUid(uid);
                        student.setName(name != null ? name : "Unknown");
                        student.setStatus("PRESENT");
                        allStudents.add(student);
                        presentCount++;
                    }
                }

                // Add absent students (those not in today's attendance)
                for (Map.Entry<String, String> entry : registeredStudents.entrySet()) {
                    String uid = entry.getKey();
                    String name = entry.getValue();

                    if (!presentUIDs.contains(uid)) {
                        Student student = new Student();
                        student.setUid(uid);
                        student.setName(name);
                        student.setStatus("ABSENT");
                        allStudents.add(student);
                    }
                }

                // Calculate absent count
                absentCount = totalStudents - presentCount;

                // Calculate percentage
                if (totalStudents > 0) {
                    attendancePercentage = (presentCount * 100.0) / totalStudents;
                } else {
                    attendancePercentage = 0.0;
                }

                // Update UI
                updateReportUI();
                studentAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                // Show message if no registered students
                if (totalStudents == 0) {
                    Toast.makeText(DailyReportActivity.this,
                            "No registered students found",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DailyReportActivity.this,
                        "Error loading attendance: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateReportUI() {
        tvReportTotal.setText(String.valueOf(totalStudents));
        tvReportPresent.setText(String.valueOf(presentCount));
        tvReportAbsent.setText(String.valueOf(absentCount));
        tvReportPercentage.setText(String.format(Locale.getDefault(),
                "Overall Attendance: %.1f%%", attendancePercentage));
    }

    private void setupButtonListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Save PDF button
        btnSavePDF.setOnClickListener(v -> {
            if (allStudents.isEmpty()) {
                Toast.makeText(this, "No data to generate report!", Toast.LENGTH_SHORT).show();
                return;
            }
            generatePDF();
        });

        // Share button
        btnShare.setOnClickListener(v -> {
            if (pdfFile != null && pdfFile.exists()) {
                sharePDF();
            } else {
                Toast.makeText(this, "Please save PDF first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generatePDF() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Create PDF document (A4 size: 595 x 842)
                PdfDocument pdfDocument = new PdfDocument();

                // Calculate pages needed
                int studentsPerPage = 35;
                int totalPages = (int) Math.ceil((double) allStudents.size() / studentsPerPage);
                if (totalPages == 0) totalPages = 1;

                for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                    // Create page
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNum + 1).create();
                    PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();
                    Paint paint = new Paint();

                    // ========== HEADER ==========
                    paint.setColor(Color.parseColor("#2C5F4F"));
                    canvas.drawRect(0, 0, 595, 100, paint);

                    paint.setColor(Color.WHITE);
                    paint.setTextSize(24);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("DAILY ATTENDANCE REPORT", 297, 40, paint);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                    String date = sdf.format(new Date());
                    paint.setTextSize(14);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    canvas.drawText("Date: " + date, 297, 65, paint);

                    paint.setTextSize(10);
                    canvas.drawText("Page " + (pageNum + 1) + " of " + totalPages, 297, 85, paint);

                    // ========== SUMMARY (Only on first page) ==========
                    int startY = 130;
                    if (pageNum == 0) {
                        paint.setColor(Color.BLACK);
                        paint.setTextAlign(Paint.Align.LEFT);
                        paint.setTextSize(16);
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        canvas.drawText("SUMMARY", 40, startY, paint);

                        startY += 30;
                        paint.setTextSize(12);
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        canvas.drawText("Total Students: " + totalStudents, 60, startY, paint);

                        startY += 20;
                        paint.setColor(Color.parseColor("#4CAF50"));
                        canvas.drawText("Present: " + presentCount, 60, startY, paint);

                        startY += 20;
                        paint.setColor(Color.parseColor("#F44336"));
                        canvas.drawText("Absent: " + absentCount, 60, startY, paint);

                        startY += 20;
                        paint.setColor(Color.BLACK);
                        canvas.drawText("Attendance: " + String.format(Locale.getDefault(), "%.1f%%", attendancePercentage), 60, startY, paint);

                        startY += 30;
                    }

                    // ========== STUDENT LIST ==========
                    paint.setColor(Color.BLACK);
                    paint.setTextAlign(Paint.Align.LEFT);
                    paint.setTextSize(16);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("STUDENT LIST", 40, startY, paint);

                    startY += 25;
                    paint.setColor(Color.parseColor("#E0E0E0"));
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(40, startY - 15, 555, startY + 5, paint);

                    paint.setColor(Color.BLACK);
                    paint.setTextSize(11);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("No.", 50, startY, paint);
                    canvas.drawText("Name", 100, startY, paint);
                    canvas.drawText("UID", 320, startY, paint);
                    canvas.drawText("Status", 480, startY, paint);

                    int startIndex = pageNum * studentsPerPage;
                    int endIndex = Math.min(startIndex + studentsPerPage, allStudents.size());

                    startY += 20;
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    paint.setTextSize(10);

                    for (int i = startIndex; i < endIndex; i++) {
                        Student student = allStudents.get(i);

                        if ((i - startIndex) % 2 == 0) {
                            paint.setColor(Color.parseColor("#F5F5F5"));
                            paint.setStyle(Paint.Style.FILL);
                            canvas.drawRect(40, startY - 12, 555, startY + 8, paint);
                        }

                        paint.setColor(Color.BLACK);
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawText(String.valueOf(i + 1), 50, startY, paint);

                        String name = student.getName();
                        if (name != null && name.length() > 25) {
                            name = name.substring(0, 25) + "...";
                        }
                        canvas.drawText(name != null ? name : "Unknown", 100, startY, paint);
                        canvas.drawText(student.getUid() != null ? student.getUid() : "N/A", 320, startY, paint);

                        // Status
                        if ("PRESENT".equalsIgnoreCase(student.getStatus())) {
                            paint.setColor(Color.parseColor("#4CAF50"));
                            canvas.drawText("✓ PRESENT", 480, startY, paint);
                        } else if ("ABSENT".equalsIgnoreCase(student.getStatus())) {
                            paint.setColor(Color.parseColor("#F44336"));
                            canvas.drawText("✗ ABSENT", 480, startY, paint);
                        } else {
                            paint.setColor(Color.GRAY);
                            canvas.drawText("—", 480, startY, paint);
                        }

                        startY += 20;
                    }

                    paint.setColor(Color.GRAY);
                    paint.setTextSize(8);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("Generated by Smart Access Tracker - " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()), 297, 820, paint);

                    pdfDocument.finishPage(page);
                }

                // Save to cache directory
                String fileName = "AttendanceReport_" + new SimpleDateFormat("ddMMMyyyy_HHmm", Locale.getDefault()).format(new Date()) + ".pdf";
                File cacheDir = getCacheDir();
                pdfFile = new File(cacheDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                pdfDocument.writeTo(fos);
                pdfDocument.close();
                fos.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ PDF saved successfully!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void sharePDF() {
        try {
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Daily Attendance Report - " +
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached the daily attendance report.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Report via"));

        } catch (Exception e) {
            Toast.makeText(this, "❌ Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}