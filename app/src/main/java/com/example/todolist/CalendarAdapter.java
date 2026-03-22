package com.example.todolist;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(int day);
    }

    private final Context context;
    private final List<Integer> days;
    private final List<String> datesWithTasks;
    private int selectedDay;
    private final int year;
    private final int month;
    private final OnDayClickListener listener;

    public CalendarAdapter(Context context, List<Integer> days, List<String> datesWithTasks,
                           int year, int month, int selectedDay, OnDayClickListener listener) {
        this.context = context;
        this.days = days;
        this.datesWithTasks = datesWithTasks;
        this.year = year;
        this.month = month;
        this.selectedDay = selectedDay;
        this.listener = listener;
    }

    public void setSelectedDay(int day) {
        this.selectedDay = day;
        notifyDataSetChanged();
    }

    public void updateDatesWithTasks(List<String> newDates) {
        this.datesWithTasks.clear();
        this.datesWithTasks.addAll(newDates);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int day = days.get(position);

        if (day == 0) {
            holder.tvDay.setText("");
            holder.dotIndicator.setVisibility(View.INVISIBLE);
            holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
            holder.cardView.setCardElevation(0);
            holder.itemView.setClickable(false);
            return;
        }

        holder.tvDay.setText(String.valueOf(day));
        holder.itemView.setClickable(true);

        String dateStr = String.format("%04d-%02d-%02d", year, month, day);
        boolean hasTasks = datesWithTasks.contains(dateStr);
        holder.dotIndicator.setVisibility(hasTasks ? View.VISIBLE : View.INVISIBLE);

        java.util.Calendar today = java.util.Calendar.getInstance();
        boolean isToday = (today.get(java.util.Calendar.YEAR) == year &&
                (today.get(java.util.Calendar.MONTH) + 1) == month &&
                today.get(java.util.Calendar.DAY_OF_MONTH) == day);

        boolean isSelected = (day == selectedDay);

        if (isSelected) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardView.setCardElevation(4f);
            holder.tvDay.setTextColor(Color.WHITE);
            holder.dotIndicator.setBackgroundResource(R.drawable.dot_white);
        } else if (isToday) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_light));
            holder.cardView.setCardElevation(2f);
            holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.dotIndicator.setBackgroundResource(R.drawable.dot_primary);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
            holder.cardView.setCardElevation(0f);
            holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.dotIndicator.setBackgroundResource(R.drawable.dot_primary);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvDay;
        View dotIndicator;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView     = itemView.findViewById(R.id.cardDay);
            tvDay        = itemView.findViewById(R.id.tvDay);
            dotIndicator = itemView.findViewById(R.id.dotIndicator);
        }
    }
}