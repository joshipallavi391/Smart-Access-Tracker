package com.example.smartaccesstracker;

public class Student {
    private String uid;
    private String name;
    private String rollNo;
    private String className;
    private String status;
    private String time;

    // Default constructor for Firebase
    public Student() {
    }

    // Full constructor
    public Student(String uid, String name, String rollNo, String className, String status, String time) {
        this.uid = uid;
        this.name = name;
        this.rollNo = rollNo;
        this.className = className;
        this.status = status;
        this.time = time;
    }

    // Getters
    public String getUid() {
        return uid != null ? uid : "";
    }

    public String getName() {
        return name != null ? name : "Unknown";
    }

    public String getRollNo() {
        return rollNo != null ? rollNo : "";
    }

    public String getClassName() {
        return className != null ? className : "";
    }

    public String getStatus() {
        return status != null ? status : "ABSENT";
    }

    public String getTime() {
        return time != null ? time : "N/A";
    }

    // Setters
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Student{uid='" + uid + "', name='" + name + "', rollNo='" + rollNo +
                "', class='" + className + "', status='" + status + "', time='" + time + "'}";
    }
}