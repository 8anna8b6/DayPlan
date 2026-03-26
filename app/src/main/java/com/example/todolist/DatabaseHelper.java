package com.example.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 5; // bumped for migration safety

    public static final String TABLE_TASKS = "tasks";
    public static final String COL_ID        = "id";
    public static final String COL_NAME      = "name";
    public static final String COL_DATE      = "date";
    public static final String COL_COMPLETED = "completed";
    public static final String COL_REPEAT    = "repeat";
    public static final String COL_ORDER     = "task_order";

    // Day abbreviations used in the weekly repeat string, index = Calendar.DAY_OF_WEEK - 1
    // Calendar: 1=Sun,2=Mon,...,7=Sat
    public static final String[] DAY_KEYS = {"SUN","MON","TUE","WED","THU","FRI","SAT"};

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TASKS + " (" +
                COL_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME      + " TEXT NOT NULL, " +
                COL_DATE      + " TEXT NOT NULL, " +
                COL_COMPLETED + " INTEGER DEFAULT 0, " +
                COL_REPEAT    + " TEXT DEFAULT 'none', " +
                COL_ORDER     + " INTEGER DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS +
                    " ADD COLUMN " + COL_COMPLETED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS +
                    " ADD COLUMN " + COL_REPEAT + " TEXT DEFAULT 'none'");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS +
                    " ADD COLUMN " + COL_ORDER + " INTEGER DEFAULT 0");
        }
        // v5: repeat format extended — no schema change needed, old "weekly" values
        // are handled gracefully in getTasksByDate below.
    }

    // ── ADD ──────────────────────────────────────────────────────────────────
    public long addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_NAME,      task.getName());
        v.put(COL_DATE,      task.getDate());
        v.put(COL_COMPLETED, 0);
        v.put(COL_REPEAT,    task.getRepeat() == null ? "none" : task.getRepeat());
        v.put(COL_ORDER,     getMaxOrder(db) + 1);
        long id = db.insert(TABLE_TASKS, null, v);
        db.close();
        return id;
    }

    private int getMaxOrder(SQLiteDatabase db) {
        int max = 0;
        Cursor c = db.rawQuery("SELECT MAX(" + COL_ORDER + ") FROM " + TABLE_TASKS, null);
        if (c.moveToFirst()) max = c.getInt(0);
        c.close();
        return max;
    }

    // ── GET BY DATE ──────────────────────────────────────────────────────────
    /**
     * Returns tasks that should appear on the given date:
     *  - exact date match (any repeat value)
     *  - repeat = "daily"  → always
     *  - repeat starts with "weekly_" → show if the date's day-of-week is in the list
     *    e.g. "weekly_MON,WED,FRI"
     *  - legacy repeat = "weekly" → match same day-of-week as task's own date
     */
    public List<Task> getTasksByDate(String date) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Pull all candidates: exact date OR any repeating task
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_TASKS +
                        " WHERE " + COL_DATE + " = ?" +
                        " OR "    + COL_REPEAT + " = 'daily'" +
                        " OR "    + COL_REPEAT + " LIKE 'weekly%'" +
                        " ORDER BY " + COL_ORDER + " ASC",
                new String[]{ date }
        );

        // Determine day-of-week abbreviation for the requested date
        String dayKey = getDayKey(date); // e.g. "MON"

        if (cursor.moveToFirst()) {
            do {
                String taskDate   = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                String repeatVal  = cursor.getString(cursor.getColumnIndexOrThrow(COL_REPEAT));

                boolean include = false;

                if (taskDate.equals(date)) {
                    // Exact match — always include
                    include = true;
                } else if ("daily".equals(repeatVal)) {
                    // Daily — include if task date is on or before requested date
                    include = taskDate.compareTo(date) <= 0;
                } else if (repeatVal != null && repeatVal.startsWith("weekly_")) {
                    // New format: "weekly_MON,WED,FRI"
                    String daysPart = repeatVal.substring("weekly_".length()); // "MON,WED,FRI"
                    String[] days = daysPart.split(",");
                    for (String d : days) {
                        if (d.trim().equalsIgnoreCase(dayKey)) {
                            include = true;
                            break;
                        }
                    }
                } else if ("weekly".equals(repeatVal)) {
                    // Legacy format — match same day-of-week as task's original date
                    String taskDayKey = getDayKey(taskDate);
                    include = taskDayKey != null && taskDayKey.equalsIgnoreCase(dayKey);
                }

                if (include) {
                    Task task = new Task();
                    task.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                    task.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
                    task.setDate(taskDate);
                    task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COL_COMPLETED)) == 1);
                    task.setRepeat(repeatVal);
                    tasks.add(task);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return tasks;
    }

    /** Returns "MON", "TUE", … for a yyyy-MM-dd string, or null on parse error. */
    public static String getDayKey(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dateStr));
            return DAY_KEYS[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } catch (Exception e) {
            return null;
        }
    }

    // ── UPDATE (EDIT) ────────────────────────────────────────────────────────
    public void updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_NAME,      task.getName());
        v.put(COL_DATE,      task.getDate());
        v.put(COL_REPEAT,    task.getRepeat());
        v.put(COL_COMPLETED, task.isCompleted() ? 1 : 0);
        db.update(TABLE_TASKS, v, COL_ID + "=?",
                new String[]{ String.valueOf(task.getId()) });
        db.close();
    }

    // ── UPDATE ORDER ─────────────────────────────────────────────────────────
    public void updateTaskOrder(List<Task> tasks) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < tasks.size(); i++) {
            ContentValues v = new ContentValues();
            v.put(COL_ORDER, i);
            db.update(TABLE_TASKS, v, COL_ID + "=?",
                    new String[]{ String.valueOf(tasks.get(i).getId()) });
        }
        db.close();
    }

    // ── SET COMPLETED ────────────────────────────────────────────────────────
    public void setTaskCompleted(int id, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_COMPLETED, completed ? 1 : 0);
        db.update(TABLE_TASKS, v, COL_ID + "=?",
                new String[]{ String.valueOf(id) });
        db.close();
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_ID + "=?", new String[]{ String.valueOf(id) });
        db.close();
    }

    // ── DATES WITH TASKS (CALENDAR) ───────────────────────────────────────────
    public List<String> getDatesWithTasksInMonth(int year, int month) {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String prefix = String.format("%04d-%02d", year, month);

        Cursor cursor = db.query(
                true, TABLE_TASKS, new String[]{ COL_DATE },
                COL_DATE + " LIKE ?", new String[]{ prefix + "%" },
                COL_DATE, null, null, null
        );

        if (cursor.moveToFirst()) {
            do { dates.add(cursor.getString(0)); } while (cursor.moveToNext());
        }
        cursor.close();

        // Also mark dates that fall on repeating task days
        addRepeatingDatesForMonth(db, dates, year, month);

        db.close();
        return dates;
    }

    /** For daily/weekly repeating tasks, add the relevant dates within the month. */
    private void addRepeatingDatesForMonth(SQLiteDatabase db,
                                           List<String> dates, int year, int month) {
        Cursor c = db.rawQuery(
                "SELECT " + COL_DATE + ", " + COL_REPEAT +
                        " FROM " + TABLE_TASKS +
                        " WHERE " + COL_REPEAT + " != 'none'" +
                        " AND   " + COL_REPEAT + " IS NOT NULL",
                null
        );

        if (!c.moveToFirst()) { c.close(); return; }

        // Build all days in the month
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        do {
            String taskDate  = c.getString(0);
            String repeatVal = c.getString(1);

            for (int d = 1; d <= daysInMonth; d++) {
                String candidate = String.format("%04d-%02d-%02d", year, month, d);
                if (dates.contains(candidate)) continue;

                boolean add = false;
                if ("daily".equals(repeatVal)) {
                    add = candidate.compareTo(taskDate) >= 0;
                } else if (repeatVal.startsWith("weekly_")) {
                    String dayKey = getDayKey(candidate);
                    String daysPart = repeatVal.substring("weekly_".length());
                    for (String k : daysPart.split(",")) {
                        if (k.trim().equalsIgnoreCase(dayKey)) { add = true; break; }
                    }
                } else if ("weekly".equals(repeatVal)) {
                    String taskDayKey = getDayKey(taskDate);
                    add = taskDayKey != null && taskDayKey.equalsIgnoreCase(getDayKey(candidate));
                }

                if (add) dates.add(candidate);
            }
        } while (c.moveToNext());

        c.close();
    }
}