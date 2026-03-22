package com.example.todolist;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTaskName;
    private Button btnPickDate, btnSaveTask;
    private ImageButton btnBack;

    private DatabaseHelper dbHelper;
    private Calendar selectedDate;
    private String selectedDateStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        dbHelper = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();


        String defaultDate = getIntent().getStringExtra("default_date");
        if (defaultDate != null && !defaultDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate.setTime(sdf.parse(defaultDate));
            } catch (Exception e) {
                // fallback to today
            }
        }
        updateDateStr();

        initViews();
        updateDateButton();

        btnBack.setOnClickListener(v -> finish());

        btnPickDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedDate.set(year, month, day);
                        updateDateStr();
                        updateDateButton();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        btnSaveTask.setOnClickListener(v -> saveTask());
    }

    private void initViews() {
        etTaskName  = findViewById(R.id.etTaskName);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnBack     = findViewById(R.id.btnBack);
    }

    private void updateDateStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateStr = sdf.format(selectedDate.getTime());
    }

    private void updateDateButton() {
        SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());
        btnPickDate.setText(displayFmt.format(selectedDate.getTime()));
    }

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            etTaskName.setError("Please enter a task name");
            etTaskName.requestFocus();
            return;
        }

        Task task = new Task(name, selectedDateStr);
        long result = dbHelper.addTask(task);

        if (result != -1) {
            Toast.makeText(this, "Task saved!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save task", Toast.LENGTH_SHORT).show();
        }
    }
}
