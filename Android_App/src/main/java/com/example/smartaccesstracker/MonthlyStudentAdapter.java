package com.example.smartaccesstracker;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MonthlyStudentAdapter extends RecyclerView.Adapter<MonthlyStudentAdapter.ViewHolder> {

    private Context context;
    private List<MonthlyReportModel> studentList;

    public MonthlyStudentAdapter(Context context, List<MonthlyReportModel> studentList) {
        this.context = context;
        this.studentList = studentList != null ? studentList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_monthly_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthlyReportModel student = studentList.get(position);

        holder.tvStudentName.setText(student.getName());
        holder.tvRollNo.setText("Roll: " + student.getRollNo());
        holder.tvClassName.setText("Class: " + student.getClassName());
        holder.tvPresentDays.setText(String.valueOf(student.getPresentDays()));
        holder.tvAbsentDays.setText(String.valueOf(student.getAbsentDays()));
        holder.tvLateDays.setText(String.valueOf(student.getLateDays()));
        holder.tvAttendancePercentage.setText(String.format("Attendance: %.1f%%", student.getAttendancePercentage()));
        holder.tvStatusBadge.setText(student.getStatusBadge());

        // Set status badge background color
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(20f);
        drawable.setColor(student.getStatusColor());
        holder.tvStatusBadge.setBackground(drawable);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateList(List<MonthlyReportModel> newList) {
        this.studentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvRollNo, tvClassName, tvPresentDays, tvAbsentDays,
                tvLateDays, tvAttendancePercentage, tvStatusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvRollNo = itemView.findViewById(R.id.tvRollNo);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvPresentDays = itemView.findViewById(R.id.tvPresentDays);
            tvAbsentDays = itemView.findViewById(R.id.tvAbsentDays);
            tvLateDays = itemView.findViewById(R.id.tvLateDays);
            tvAttendancePercentage = itemView.findViewById(R.id.tvAttendancePercentage);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        }
    }
}