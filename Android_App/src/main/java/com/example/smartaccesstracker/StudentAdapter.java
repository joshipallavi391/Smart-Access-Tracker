package com.example.smartaccesstracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;

    public StudentAdapter(List<Student> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);

        // Set student name
        holder.tvName.setText(student.getName());

        // Set UID
        holder.tvUid.setText("UID: " + student.getUid());

        // Set time (formatted)
        String formattedTime = formatTime(student.getTime());
        holder.tvTime.setText("Time: " + formattedTime);

        // Set status and color
        String status = student.getStatus();
        holder.tvStatus.setText(status);

        if ("PRESENT".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(0xFF4CAF50); // Green
        } else {
            holder.tvStatus.setTextColor(0xFFF44336); // Red
        }
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    /**
     * Format time from "23:17:13" to "11:17 PM"
     */
    private String formatTime(String time) {
        if (time == null || time.isEmpty() || time.equals("N/A")) {
            return "N/A";
        }

        try {
            String[] parts = time.split(":");
            if (parts.length >= 2) {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                String amPm = hour >= 12 ? "PM" : "AM";
                int hour12 = hour % 12;
                if (hour12 == 0) hour12 = 12;

                return String.format("%02d:%02d %s", hour12, minute, amPm);
            }
        } catch (Exception e) {
            return time;
        }

        return time;
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUid, tvTime, tvStatus;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUid = itemView.findViewById(R.id.tvUid);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}