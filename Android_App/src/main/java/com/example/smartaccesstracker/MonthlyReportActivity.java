package com.example.smartaccesstracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private Spinner spinnerMonth, spinnerYear;
    private Button btnLoadReport, btnGeneratePDF;
    private RecyclerView recyclerViewStudents;
    private TextView tvTotalStudents, tvAverageAttendance, tvLowAttendanceCount, tvNoData;
    private View cardStatistics;

    private MonthlyStudentAdapter adapter;
    private List<MonthlyReportModel> studentReportList;

    private DatabaseReference databaseReference;

    private String selectedMonth;
    private int selectedYear;

    // Hardcoded student UIDs and details (same as AddStudentActivity)
    private final Map<String, StudentInfo> studentInfoMap = new HashMap<String, StudentInfo>() {{
        put("73C1D0A6", new StudentInfo("Aarav Sharma", "001", "CSE-A"));
        put("F350D0A6", new StudentInfo("Ananya Gupta", "002", "CSE-A"));
        put("53D6D5A6", new StudentInfo("Rohan Verma", "003", "CSE-B"));
        put("A3E0D6A6", new StudentInfo("Priya Singh", "004", "CSE-B"));
        put("23F5D1A6", new StudentInfo("Arjun Patel", "005", "IT-A"));
        put("C398D2A6", new StudentInfo("Sneha Reddy", "006", "IT-A"));
        put("13D3D3A6", new StudentInfo("Karan Mehta", "007", "ECE-A"));
        put("B3CCD4A6", new StudentInfo("Neha Iyer", "008", "ECE-A"));
        put("03BAD5A6", new StudentInfo("Vikram Desai", "009", "ME-A"));
        put("E3A9D6A6", new StudentInfo("Pooja Joshi", "010", "ME-A"));
        put("D3B7D7A6", new StudentInfo("Aditya Kumar", "011", "CE-A"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_report);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("attendance");

        // Initialize views
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnLoadReport = findViewById(R.id.btnLoadReport);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        recyclerViewStudents = findViewById(R.id.recyclerViewStudents);
        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvAverageAttendance = findViewById(R.id.tvAverageAttendance);
        tvLowAttendanceCount = findViewById(R.id.tvLowAttendanceCount);
        tvNoData = findViewById(R.id.tvNoData);
        cardStatistics = findViewById(R.id.cardStatistics);

        // Setup RecyclerView
        studentReportList = new ArrayList<>();
        adapter = new MonthlyStudentAdapter(this, studentReportList);
        recyclerViewStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewStudents.setAdapter(adapter);

        // Setup Spinners
        setupMonthSpinner();
        setupYearSpinner();

        // Load Report button
        btnLoadReport.setOnClickListener(v -> loadMonthlyReport());

        // Generate PDF button
        btnGeneratePDF.setOnClickListener(v -> {
            if (checkPermission()) {
                generatePDF();
            } else {
                requestPermission();
            }
        });
    }

    private void setupMonthSpinner() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // Set current month as default
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        spinnerMonth.setSelection(currentMonth);
    }

    private void setupYearSpinner() {
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear; i >= currentYear - 5; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
    }

    private void loadMonthlyReport() {
        selectedMonth = String.format("%02d", spinnerMonth.getSelectedItemPosition() + 1);
        selectedYear = Integer.parseInt(spinnerYear.getSelectedItem().toString());

        // Show loading
        Toast.makeText(this, "Loading report...", Toast.LENGTH_SHORT).show();

        // Clear previous data
        studentReportList.clear();

        // Get all dates in the selected month
        List<String> datesInMonth = getDatesInMonth(selectedYear, selectedMonth);

        // Fetch attendance data for all students
        fetchMonthlyAttendance(datesInMonth);
    }

    private List<String> getDatesInMonth(int year, String month) {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, Integer.parseInt(month) - 1, 1);

        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        for (int day = 1; day <= maxDay; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            dates.add(dateFormat.format(calendar.getTime()));
        }

        return dates;
    }

    private void fetchMonthlyAttendance(List<String> dates) {
        final int[] processedDates = {0};
        final Map<String, AttendanceStats> studentStatsMap = new HashMap<>();

        // Initialize stats for all students
        for (String uid : studentInfoMap.keySet()) {
            studentStatsMap.put(uid, new AttendanceStats());
        }

        if (dates.isEmpty()) {
            showNoDataMessage();
            return;
        }

        for (String date : dates) {
            databaseReference.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                            String uid = uidSnapshot.getKey();
                            if (studentInfoMap.containsKey(uid)) {
                                String status = uidSnapshot.child("status").getValue(String.class);
                                AttendanceStats stats = studentStatsMap.get(uid);
                                if (stats != null && status != null) {
                                    switch (status.toLowerCase()) {
                                        case "present":
                                            stats.presentDays++;
                                            break;
                                        case "absent":
                                            stats.absentDays++;
                                            break;
                                        case "late":
                                            stats.lateDays++;
                                            break;
                                    }
                                }
                            }
                        }
                    }

                    processedDates[0]++;
                    if (processedDates[0] == dates.size()) {
                        // All dates processed, calculate and display results
                        displayResults(studentStatsMap);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MonthlyReportActivity.this,
                            "Error loading data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void displayResults(Map<String, AttendanceStats> studentStatsMap) {
        studentReportList.clear();
        int lowAttendanceCount = 0;
        double totalPercentage = 0;
        int studentsWithData = 0;

        for (Map.Entry<String, StudentInfo> entry : studentInfoMap.entrySet()) {
            String uid = entry.getKey();
            StudentInfo info = entry.getValue();
            AttendanceStats stats = studentStatsMap.get(uid);

            if (stats != null) {
                int totalDays = stats.presentDays + stats.absentDays + stats.lateDays;
                double percentage = 0;

                if (totalDays > 0) {
                    percentage = (stats.presentDays * 100.0) / totalDays;
                    studentsWithData++;
                    totalPercentage += percentage;
                }

                MonthlyReportModel model = new MonthlyReportModel(
                        info.name,
                        info.rollNo,
                        info.className,
                        uid,
                        stats.presentDays,
                        stats.absentDays,
                        stats.lateDays,
                        percentage
                );

                studentReportList.add(model);

                if (percentage < 50) {
                    lowAttendanceCount++;
                }
            }
        }

        if (studentsWithData == 0) {
            showNoDataMessage();
            return;
        }

        // Update statistics
        double averageAttendance = totalPercentage / studentsWithData;
        tvTotalStudents.setText(String.valueOf(studentInfoMap.size()));
        tvAverageAttendance.setText(String.format("%.1f%%", averageAttendance));
        tvLowAttendanceCount.setText(String.valueOf(lowAttendanceCount));

        // Show data
        adapter.updateList(studentReportList);
        cardStatistics.setVisibility(View.VISIBLE);
        recyclerViewStudents.setVisibility(View.VISIBLE);
        btnGeneratePDF.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
    }

    private void showNoDataMessage() {
        cardStatistics.setVisibility(View.GONE);
        recyclerViewStudents.setVisibility(View.GONE);
        btnGeneratePDF.setVisibility(View.GONE);
        tvNoData.setVisibility(View.VISIBLE);
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF();
            } else {
                Toast.makeText(this, "Permission denied. Cannot generate PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generatePDF() {
        if (studentReportList.isEmpty()) {
            Toast.makeText(this, "No data to generate PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // Page info
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();

        // Title
        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        String monthName = spinnerMonth.getSelectedItem().toString();
        String title = "Monthly Attendance Report - " + monthName + " " + selectedYear;
        canvas.drawText(title, 50, 50, titlePaint);

        // Summary
        paint.setTextSize(12);
        int yPos = 100;
        canvas.drawText("Total Students: " + tvTotalStudents.getText(), 50, yPos, paint);
        yPos += 20;
        canvas.drawText("Average Attendance: " + tvAverageAttendance.getText(), 50, yPos, paint);
        yPos += 20;
        canvas.drawText("Low Attendance Students: " + tvLowAttendanceCount.getText(), 50, yPos, paint);
        yPos += 40;

        // Table Header
        paint.setFakeBoldText(true);
        canvas.drawText("Roll", 50, yPos, paint);
        canvas.drawText("Name", 100, yPos, paint);
        canvas.drawText("Class", 250, yPos, paint);
        canvas.drawText("P", 320, yPos, paint);
        canvas.drawText("A", 360, yPos, paint);
        canvas.drawText("L", 400, yPos, paint);
        canvas.drawText("%", 440, yPos, paint);
        canvas.drawText("Status", 490, yPos, paint);
        paint.setFakeBoldText(false);
        yPos += 20;

        // Draw line
        canvas.drawLine(50, yPos, 545, yPos, paint);
        yPos += 10;

        // Student data
        for (MonthlyReportModel student : studentReportList) {
            if (yPos > 800) {
                pdfDocument.finishPage(page);
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                yPos = 50;
            }

            canvas.drawText(student.getRollNo(), 50, yPos, paint);
            canvas.drawText(student.getName(), 100, yPos, paint);
            canvas.drawText(student.getClassName(), 250, yPos, paint);
            canvas.drawText(String.valueOf(student.getPresentDays()), 320, yPos, paint);
            canvas.drawText(String.valueOf(student.getAbsentDays()), 360, yPos, paint);
            canvas.drawText(String.valueOf(student.getLateDays()), 400, yPos, paint);
            canvas.drawText(String.format("%.1f", student.getAttendancePercentage()), 440, yPos, paint);
            canvas.drawText(student.getStatusBadge(), 490, yPos, paint);
            yPos += 20;
        }

        // Footer
        yPos += 30;
        paint.setTextSize(10);
        String footer = "Generated on " + new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                .format(Calendar.getInstance().getTime()) + " by Smart Access Tracker";
        canvas.drawText(footer, 50, yPos, paint);

        pdfDocument.finishPage(page);

        // Save PDF
        String fileName = "Monthly_Report_" + monthName + "_" + selectedYear + ".pdf";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        pdfDocument.close();
    }

    // Helper classes
    private static class StudentInfo {
        String name;
        String rollNo;
        String className;

        StudentInfo(String name, String rollNo, String className) {
            this.name = name;
            this.rollNo = rollNo;
            this.className = className;
        }
    }

    private static class AttendanceStats {
        int presentDays = 0;
        int absentDays = 0;
        int lateDays = 0;
    }
}