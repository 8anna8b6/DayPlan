package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskDeleteListener {
        void onDelete(Task task, int position);
    }

    private List<Task> taskList;
    private final DatabaseHelper dbHelper;
    private final OnTaskDeleteListener deleteListener;

    public TaskAdapter(List<Task> taskList, DatabaseHelper dbHelper, OnTaskDeleteListener deleteListener) {
        this.taskList = taskList;
        this.dbHelper = dbHelper;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTaskName.setText(task.getName());

        // Format date
        String[] parts = task.getDate().split("-");
        if (parts.length == 3) {
            String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec"};
            try {
                int m = Integer.parseInt(parts[1]) - 1;
                holder.tvTaskDate.setText(months[m] + " " + parts[2] + ", " + parts[0]);
            } catch (Exception e) {
                holder.tvTaskDate.setText(task.getDate());
            }
        } else {
            holder.tvTaskDate.setText(task.getDate());
        }

        // Apply strike-through if already completed
        applyStrikeThrough(holder.tvTaskName, task.isCompleted());

        // Reset listener before setting checked state to avoid ghost triggers
        holder.checkDone.setOnCheckedChangeListener(null);
        holder.checkDone.setChecked(task.isCompleted());

        holder.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.setTaskCompleted(task.getId(), isChecked);
            applyStrikeThrough(holder.tvTaskName, isChecked);
        });

        // ✅ Delete button click
        holder.btnDeleteTask.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(task, holder.getAdapterPosition());
            }
        });
    }

    private void applyStrikeThrough(TextView tv, boolean strike) {
        if (strike) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.5f);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void removeItem(int position) {
        taskList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, taskList.size());
    }

    public void updateList(List<Task> newList) {
        this.taskList = newList;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTaskDate;
        CheckBox checkDone;
        ImageButton btnDeleteTask;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName    = itemView.findViewById(R.id.tvTaskName);
            tvTaskDate    = itemView.findViewById(R.id.tvTaskDate);
            checkDone     = itemView.findViewById(R.id.btnDelete);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}