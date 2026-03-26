package com.example.todolist;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    private TextInputEditText etTaskName;
    private Button btnPickDate, btnSaveTask;
    private ImageButton btnBack;
    private Spinner spinnerRepeat;
    private LinearLayout layoutWeeklyDays;
    private CheckBox chkSun, chkMon, chkTue, chkWed, chkThu, chkFri, chkSat;
    private TextView tvTitle;

    private DatabaseHelper dbHelper;
    private Calendar selectedDate;
    private String selectedDateStr;


    private static final int REPEAT_NONE    = 0;
    private static final int REPEAT_DAILY   = 1;
    private static final int REPEAT_WEEKLY  = 2;  // specific days

    private int repeatPosition = REPEAT_NONE;
    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        dbHelper = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();

        initViews();
        setupRepeatSpinner();

        taskId = getIntent().getIntExtra("task_id", -1);

        if (taskId != -1) {
            // Edit mode
            tvTitle.setText("Edit Task");
            etTaskName.setText(getIntent().getStringExtra("task_name"));
            selectedDateStr = getIntent().getStringExtra("task_date");
            String repeatVal = getIntent().getStringExtra("task_repeat");
            restoreRepeatUI(repeatVal);
        } else {
            // New task — pre-fill date if provided
            String defaultDate = getIntent().getStringExtra("default_date");
            if (defaultDate != null) {
                try {
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(defaultDate);
                    selectedDate.setTime(
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .parse(defaultDate)
                    );
                } catch (Exception ignored) {}
            }
            updateDateStr();
        }

        updateDateButton();

        btnBack.setOnClickListener(v -> finish());

        btnPickDate.setOnClickListener(v -> new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedDate.set(year, month, day);
                    updateDateStr();
                    updateDateButton();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show());

        btnSaveTask.setOnClickListener(v -> saveTask());
    }

    private void initViews() {
        etTaskName       = findViewById(R.id.etTaskName);
        btnPickDate      = findViewById(R.id.btnPickDate);
        btnSaveTask      = findViewById(R.id.btnSaveTask);
        btnBack          = findViewById(R.id.btnBack);
        spinnerRepeat    = findViewById(R.id.spinnerRepeat);
        layoutWeeklyDays = findViewById(R.id.layoutWeeklyDays);
        chkSun = findViewById(R.id.chkSun);
        chkMon = findViewById(R.id.chkMon);
        chkTue = findViewById(R.id.chkTue);
        chkWed = findViewById(R.id.chkWed);
        chkThu = findViewById(R.id.chkThu);
        chkFri = findViewById(R.id.chkFri);
        chkSat = findViewById(R.id.chkSat);
        tvTitle = findViewById(R.id.tvTitle);


        for (CheckBox cb : new CheckBox[]{chkSun, chkMon, chkTue, chkWed, chkThu, chkFri, chkSat}) {
            syncDayToggleColor(cb);
            cb.setOnCheckedChangeListener((btn, checked) -> syncDayToggleColor((CheckBox) btn));
        }
    }


    private void syncDayToggleColor(CheckBox cb) {
        cb.setTextColor(cb.isChecked()
                ? 0xFFFFFFFF
                : getResources().getColor(R.color.primary, getTheme()));
    }

    private void setupRepeatSpinner() {
        String[] options = {"No repeat", "Every day", "Specific days of the week"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options);
        spinnerRepeat.setAdapter(adapter);

        spinnerRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                repeatPosition = position;
                layoutWeeklyDays.setVisibility(
                        position == REPEAT_WEEKLY ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void restoreRepeatUI(String repeatVal) {
        if (repeatVal == null || repeatVal.equals("none")) {
            spinnerRepeat.setSelection(REPEAT_NONE);
        } else if (repeatVal.equals("daily")) {
            spinnerRepeat.setSelection(REPEAT_DAILY);
        } else if (repeatVal.startsWith("weekly_")) {
            spinnerRepeat.setSelection(REPEAT_WEEKLY);
            layoutWeeklyDays.setVisibility(View.VISIBLE);
            String days = repeatVal.substring("weekly_".length());
            for (String d : days.split(",")) {
                switch (d.trim().toUpperCase(Locale.ROOT)) {
                    case "SUN": chkSun.setChecked(true); break;
                    case "MON": chkMon.setChecked(true); break;
                    case "TUE": chkTue.setChecked(true); break;
                    case "WED": chkWed.setChecked(true); break;
                    case "THU": chkThu.setChecked(true); break;
                    case "FRI": chkFri.setChecked(true); break;
                    case "SAT": chkSat.setChecked(true); break;
                }
            }
        } else if (repeatVal.equals("weekly")) {

            spinnerRepeat.setSelection(REPEAT_WEEKLY);
            layoutWeeklyDays.setVisibility(View.VISIBLE);
            String dayKey = DatabaseHelper.getDayKey(selectedDateStr);
            if (dayKey != null) tickDayCheckbox(dayKey);
        }
    }

    private void tickDayCheckbox(String key) {
        switch (key.toUpperCase(Locale.ROOT)) {
            case "SUN": chkSun.setChecked(true); break;
            case "MON": chkMon.setChecked(true); break;
            case "TUE": chkTue.setChecked(true); break;
            case "WED": chkWed.setChecked(true); break;
            case "THU": chkThu.setChecked(true); break;
            case "FRI": chkFri.setChecked(true); break;
            case "SAT": chkSat.setChecked(true); break;
        }
    }


    private String buildRepeatValue() {
        switch (repeatPosition) {
            case REPEAT_DAILY:  return "daily";
            case REPEAT_WEEKLY: {
                StringBuilder sb = new StringBuilder("weekly_");
                boolean any = false;
                if (chkSun.isChecked()) { sb.append("SUN,"); any = true; }
                if (chkMon.isChecked()) { sb.append("MON,"); any = true; }
                if (chkTue.isChecked()) { sb.append("TUE,"); any = true; }
                if (chkWed.isChecked()) { sb.append("WED,"); any = true; }
                if (chkThu.isChecked()) { sb.append("THU,"); any = true; }
                if (chkFri.isChecked()) { sb.append("FRI,"); any = true; }
                if (chkSat.isChecked()) { sb.append("SAT,"); any = true; }
                if (!any) {
                    Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show();
                    return null; // signal validation failure
                }
                // Remove trailing comma
                return sb.substring(0, sb.length() - 1);
            }
            default: return "none";
        }
    }

    private void updateDateStr() {
        selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(selectedDate.getTime());
    }

    private void updateDateButton() {
        try {
            selectedDate.setTime(
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(selectedDateStr));
        } catch (Exception ignored) {}
        btnPickDate.setText(
                new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
                        .format(selectedDate.getTime()));
    }

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            etTaskName.setError("Enter task name");
            return;
        }

        String repeatVal = buildRepeatValue();
        if (repeatVal == null) return; // validation failed (no days selected)

        Task task = new Task(name, selectedDateStr);
        task.setRepeat(repeatVal);

        if (taskId != -1) {
            task.setId(taskId);
            dbHelper.updateTask(task);
            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.addTask(task);
            Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}