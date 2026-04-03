package com.example.smartaccesstracker;

public class MonthlyReportModel {
    private String name;
    private String rollNo;
    private String className;
    private String uid;
    private int presentDays;
    private int absentDays;
    private int lateDays;
    private double attendancePercentage;

    public MonthlyReportModel() {
        // Default constructor required for Firebase
    }

    public MonthlyReportModel(String name, String rollNo, String className, String uid,
                              int presentDays, int absentDays, int lateDays, double attendancePercentage) {
        this.name = name;
        this.rollNo = rollNo;
        this.className = className;
        this.uid = uid;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
        this.lateDays = lateDays;
        this.attendancePercentage = attendancePercentage;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }

    public int getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(int absentDays) {
        this.absentDays = absentDays;
    }

    public int getLateDays() {
        return lateDays;
    }

    public void setLateDays(int lateDays) {
        this.lateDays = lateDays;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getTotalDays() {
        return presentDays + absentDays + lateDays;
    }

    public String getStatusBadge() {
        if (attendancePercentage >= 75) {
            return "Good";
        } else if (attendancePercentage >= 50) {
            return "Average";
        } else {
            return "Low";
        }
    }

    public int getStatusColor() {
        if (attendancePercentage >= 75) {
            return 0xFF4CAF50; // Green
        } else if (attendancePercentage >= 50) {
            return 0xFFFFC107; // Yellow
        } else {
            return 0xFFF44336; // Red
        }
    }
}