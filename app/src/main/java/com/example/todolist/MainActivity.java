package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDate = sdfKey.format(calendar.getTime());

        initViews();
        displayDate(calendar);
        loadTasks();

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            intent.putExtra("default_date", todayDate);
            startActivity(intent);
        });

        btnSchedule.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CalendarActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void initViews() {
        tvDayName     = findViewById(R.id.tvDayName);
        tvFullDate    = findViewById(R.id.tvFullDate);
        tvTaskCount   = findViewById(R.id.tvTaskCount);
        tvEmpty       = findViewById(R.id.tvEmpty);
        recyclerTasks = findViewById(R.id.recyclerTasks);
        btnAddTask    = findViewById(R.id.btnAddTask);
        btnSchedule   = findViewById(R.id.btnSchedule);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void displayDate(Calendar calendar) {
        SimpleDateFormat dayFormat  = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvDayName.setText(dayFormat.format(calendar.getTime()).toUpperCase());
        tvFullDate.setText(dateFormat.format(calendar.getTime()));
    }

    private void loadTasks() {
        taskList = dbHelper.getTasksByDate(todayDate);
        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(taskList, dbHelper, this::confirmDelete);
            recyclerTasks.setAdapter(taskAdapter);
        } else {
            taskAdapter.updateList(taskList);
        }
        if (taskList.isEmpty()) {
            recyclerTasks.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvTaskCount.setText("No tasks scheduled for today");
        } else {
            recyclerTasks.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            int count = taskList.size();
            tvTaskCount.setText(count + (count == 1 ? " task" : " tasks") + " for today");
        }
    }

    private void confirmDelete(Task task, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Delete \"" + task.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTask(task.getId());
                    taskAdapter.removeItem(position);
                    loadTasks();
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
