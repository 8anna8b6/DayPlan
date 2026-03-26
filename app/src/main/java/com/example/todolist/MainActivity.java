package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private TextView tvDayName, tvFullDate, tvTaskCount;
    private LinearLayout tvEmpty;
    private RecyclerView recyclerTasks;
    private Button btnAddTask, btnSchedule;

    private DatabaseHelper dbHelper;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        Calendar cal = Calendar.getInstance();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        initViews();
        displayDate(cal);
        loadTasks();

        btnAddTask.setOnClickListener(v -> {
            Intent i = new Intent(this, AddTaskActivity.class);
            i.putExtra("default_date", todayDate);
            startActivity(i);
        });

        btnSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));

        enableDragReorder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void initViews() {
        tvDayName   = findViewById(R.id.tvDayName);
        tvFullDate  = findViewById(R.id.tvFullDate);
        tvTaskCount = findViewById(R.id.tvTaskCount);
        tvEmpty     = findViewById(R.id.tvEmpty);
        recyclerTasks = findViewById(R.id.recyclerTasks);
        btnAddTask  = findViewById(R.id.btnAddTask);
        btnSchedule = findViewById(R.id.btnSchedule);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void displayDate(Calendar cal) {
        tvDayName.setText(
                new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime()).toUpperCase());
        tvFullDate.setText(
                new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(cal.getTime()));
    }

    private void loadTasks() {
        taskList = dbHelper.getTasksByDate(todayDate);

        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(
                    taskList, dbHelper,
                    this::confirmDelete,
                    new TaskAdapter.OnTaskClickListener() {
                        @Override public void onTaskClick(Task task) { /* no-op on main */ }
                        @Override public void onTaskLongClick(Task task) { openEdit(task); }
                        @Override public void onTaskDoneChanged(Task task, boolean isDone) {}
                    }
            );
            recyclerTasks.setAdapter(taskAdapter);
        } else {
            taskAdapter.updateTasks(taskList);
        }

        if (taskList.isEmpty()) {
            recyclerTasks.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvTaskCount.setText("No tasks today");
        } else {
            recyclerTasks.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            tvTaskCount.setText(taskList.size() + " tasks today");
        }
    }

    private void openEdit(Task task) {
        Intent i = new Intent(this, AddTaskActivity.class);
        i.putExtra("task_id",     task.getId());
        i.putExtra("task_name",   task.getName());
        i.putExtra("task_date",   task.getDate());
        i.putExtra("task_repeat", task.getRepeat());
        startActivity(i);
    }

    private void confirmDelete(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Delete \"" + task.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    dbHelper.deleteTask(task.getId());
                    if (taskAdapter != null) taskAdapter.removeItem(task);
                    loadTasks();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void enableDragReorder() {
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                    @Override
                    public boolean onMove(RecyclerView rv,
                                          RecyclerView.ViewHolder vh,
                                          RecyclerView.ViewHolder target) {
                        int from = vh.getAdapterPosition();
                        int to   = target.getAdapterPosition();
                        Collections.swap(taskList, from, to);
                        taskAdapter.notifyItemMoved(from, to);
                        dbHelper.updateTaskOrder(taskList);
                        return true;
                    }

                    @Override public void onSwiped(RecyclerView.ViewHolder vh, int dir) {}
                });
        helper.attachToRecyclerView(recyclerTasks);
    }
}