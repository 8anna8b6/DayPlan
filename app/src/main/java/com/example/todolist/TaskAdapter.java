package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final DatabaseHelper dbHelper;
    private final OnTaskDeleteListener deleteListener;
    private final OnTaskClickListener clickListener;

    public interface OnTaskDeleteListener {
        void onTaskDelete(Task task);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);           // NEW — long-press to edit
        void onTaskDoneChanged(Task task, boolean isDone);
    }

    public TaskAdapter(List<Task> taskList, DatabaseHelper dbHelper,
                       OnTaskDeleteListener deleteListener,
                       OnTaskClickListener clickListener) {
        this.taskList       = taskList;
        this.dbHelper       = dbHelper;
        this.deleteListener = deleteListener;
        this.clickListener  = clickListener;
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
        holder.tvTaskDate.setText(task.getDate());

        // ── Completed visual state ───────────────────────────────────────────
        applyCompletedStyle(holder, task.isCompleted());

        // Use setOnCheckedChangeListener carefully to avoid recursive calls
        holder.chkDone.setOnCheckedChangeListener(null);
        holder.chkDone.setChecked(task.isCompleted());
        holder.chkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.setTaskCompleted(task.getId(), isChecked);
            applyCompletedStyle(holder, isChecked);
            if (clickListener != null) clickListener.onTaskDoneChanged(task, isChecked);
        });

        // ── Delete ────────────────────────────────────────────────────────────
        holder.btnDeleteTask.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onTaskDelete(task);
        });

        // ── Click (short) → view / select ────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTaskClick(task);
        });

        // ── Long-press → edit ─────────────────────────────────────────────────
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) clickListener.onTaskLongClick(task);
            return true; // consume the event
        });
    }

    /** Apply or remove the visual "done" treatment on a holder. */
    private void applyCompletedStyle(TaskViewHolder holder, boolean completed) {
        if (completed) {
            // Strikethrough + dimmed text
            holder.tvTaskName.setPaintFlags(
                    holder.tvTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskDate.setPaintFlags(
                    holder.tvTaskDate.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskName.setAlpha(0.45f);
            holder.tvTaskDate.setAlpha(0.45f);
            // Tint the whole card slightly
            holder.itemView.setAlpha(0.75f);
        } else {
            holder.tvTaskName.setPaintFlags(
                    holder.tvTaskName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskDate.setPaintFlags(
                    holder.tvTaskDate.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskName.setAlpha(1f);
            holder.tvTaskDate.setAlpha(1f);
            holder.itemView.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTaskName, tvTaskDate;
        CheckBox    chkDone;
        ImageButton btnDeleteTask;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName   = itemView.findViewById(R.id.tvTaskName);
            tvTaskDate   = itemView.findViewById(R.id.tvTaskDate);
            chkDone      = itemView.findViewById(R.id.chkDone);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }

    public void updateTasks(List<Task> newTasks) {
        taskList.clear();
        taskList.addAll(newTasks);
        notifyDataSetChanged();
    }

    public void removeItem(Task task) {
        int index = taskList.indexOf(task);
        if (index != -1) {
            taskList.remove(index);
            notifyItemRemoved(index);
        }
    }
}