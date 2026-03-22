package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private TextView tvMonthYear, tvSelectedDateLabel, tvDayEmpty;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView recyclerCalendar, recyclerDayTasks;
    private Button btnAddTask;

    private DatabaseHelper dbHelper;
    private CalendarAdapter calendarAdapter;
    private TaskAdapter dayTaskAdapter;

    private Calendar displayedMonth;
    private int selectedDay = -1;
    private String selectedDateStr = "";

    private List<Integer> dayList = new ArrayList<>();
    private List<String> datesWithTasks = new ArrayList<>();
    private List<Task> dayTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        dbHelper = new DatabaseHelper(this);
        displayedMonth = Calendar.getInstance();

        initViews();
        setupCalendar();


        recyclerDayTasks.setVisibility(View.GONE);
        tvDayEmpty.setVisibility(View.GONE);
        tvSelectedDateLabel.setText("Tap a day to see tasks");

        btnPrevMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            selectedDay = -1;
            selectedDateStr = "";
            tvSelectedDateLabel.setText("Tap a day to see tasks");
            dayTasks.clear();
            if (dayTaskAdapter != null) dayTaskAdapter.updateList(dayTasks);
            recyclerDayTasks.setVisibility(View.GONE);
            tvDayEmpty.setVisibility(View.GONE);
            setupCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            selectedDay = -1;
            selectedDateStr = "";
            tvSelectedDateLabel.setText("Tap a day to see tasks");
            dayTasks.clear();
            if (dayTaskAdapter != null) dayTaskAdapter.updateList(dayTasks);
            recyclerDayTasks.setVisibility(View.GONE);
            tvDayEmpty.setVisibility(View.GONE);
            setupCalendar();
        });

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, AddTaskActivity.class);
            if (!selectedDateStr.isEmpty()) intent.putExtra("default_date", selectedDateStr);
            startActivity(intent);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        int year  = displayedMonth.get(Calendar.YEAR);
        int month = displayedMonth.get(Calendar.MONTH) + 1;
        datesWithTasks = dbHelper.getDatesWithTasksInMonth(year, month);
        if (calendarAdapter != null) calendarAdapter.updateDatesWithTasks(datesWithTasks);
        if (!selectedDateStr.isEmpty()) loadDayTasks();
    }

    private void initViews() {
        tvMonthYear         = findViewById(R.id.tvMonthYear);
        tvSelectedDateLabel = findViewById(R.id.tvSelectedDateLabel);
        tvDayEmpty          = findViewById(R.id.tvDayEmpty);
        btnPrevMonth        = findViewById(R.id.btnPrevMonth);
        btnNextMonth        = findViewById(R.id.btnNextMonth);
        recyclerCalendar    = findViewById(R.id.recyclerCalendar);
        recyclerDayTasks    = findViewById(R.id.recyclerDayTasks);
        btnAddTask          = findViewById(R.id.btnAddTask);
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        recyclerDayTasks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupCalendar() {
        int year  = displayedMonth.get(Calendar.YEAR);
        int month = displayedMonth.get(Calendar.MONTH) + 1;

        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(fmt.format(displayedMonth.getTime()));

        datesWithTasks = dbHelper.getDatesWithTasksInMonth(year, month);

        dayList.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int firstDow    = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i < firstDow; i++) dayList.add(0);
        for (int d = 1; d <= daysInMonth; d++) dayList.add(d);
        int remaining = 7 - (dayList.size() % 7);
        if (remaining < 7) for (int i = 0; i < remaining; i++) dayList.add(0);

        calendarAdapter = new CalendarAdapter(this, dayList, datesWithTasks,
                year, month, selectedDay, day -> {
            selectedDay = day;
            calendarAdapter.setSelectedDay(day);
            buildSelectedDateStr();
            loadDayTasks();
        });
        recyclerCalendar.setAdapter(calendarAdapter);
    }

    private void buildSelectedDateStr() {
        if (selectedDay < 1) { selectedDateStr = ""; return; }
        int year  = displayedMonth.get(Calendar.YEAR);
        int month = displayedMonth.get(Calendar.MONTH) + 1;
        selectedDateStr = String.format("%04d-%02d-%02d", year, month, selectedDay);
    }

    private void loadDayTasks() {
        if (selectedDateStr.isEmpty()) return;
        dayTasks = dbHelper.getTasksByDate(selectedDateStr);

        // Update header label
        try {
            SimpleDateFormat inFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outFmt = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
            tvSelectedDateLabel.setText("Tasks for " + outFmt.format(inFmt.parse(selectedDateStr)));
        } catch (Exception e) {
            tvSelectedDateLabel.setText("Tasks for " + selectedDateStr);
        }

        // Bind adapter
        if (dayTaskAdapter == null) {
            dayTaskAdapter = new TaskAdapter(dayTasks, dbHelper, this::confirmDelete);
            recyclerDayTasks.setAdapter(dayTaskAdapter);
        } else {
            dayTaskAdapter.updateList(dayTasks);
        }

        // Show list or empty message
        if (dayTasks.isEmpty()) {
            recyclerDayTasks.setVisibility(View.GONE);
            tvDayEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerDayTasks.setVisibility(View.VISIBLE);
            tvDayEmpty.setVisibility(View.GONE);
        }
    }

    private void confirmDelete(Task task, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Delete \"" + task.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTask(task.getId());
                    dayTaskAdapter.removeItem(position);
                    int year  = displayedMonth.get(Calendar.YEAR);
                    int month = displayedMonth.get(Calendar.MONTH) + 1;
                    datesWithTasks = dbHelper.getDatesWithTasksInMonth(year, month);
                    if (calendarAdapter != null) calendarAdapter.updateDatesWithTasks(datesWithTasks);
                    loadDayTasks();
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}