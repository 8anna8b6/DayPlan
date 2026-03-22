package com.example.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 2; // bumped to 2

    public static final String TABLE_TASKS = "tasks";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_DATE = "date";
    public static final String COL_COMPLETED = "completed"; // new column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_TASKS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT NOT NULL, " +
                COL_DATE + " TEXT NOT NULL, " +
                COL_COMPLETED + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add completed column to existing table without losing data
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COL_COMPLETED + " INTEGER DEFAULT 0");
        }
    }

    public long addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, task.getName());
        values.put(COL_DATE, task.getDate());
        values.put(COL_COMPLETED, 0);
        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    public List<Task> getTasksByDate(String date) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_TASKS, null,
                COL_DATE + " = ?", new String[]{date},
                null, null, COL_ID + " ASC");
        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                task.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
                task.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COL_COMPLETED)) == 1);
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public List<String> getDatesWithTasksInMonth(int year, int month) {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String prefix = String.format("%04d-%02d", year, month);
        Cursor cursor = db.query(
                true, TABLE_TASKS, new String[]{COL_DATE},
                COL_DATE + " LIKE ?", new String[]{prefix + "%"},
                COL_DATE, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dates;
    }

    // Mark task as complete/incomplete
    public void setTaskCompleted(int id, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_COMPLETED, completed ? 1 : 0);
        db.update(TABLE_TASKS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }


}