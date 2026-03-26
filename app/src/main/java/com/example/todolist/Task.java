package com.example.todolist;

public class Task {
    private int id;
    private String name;
    private String date;
    private boolean completed;
    private String repeat; // "none", "daily", "weekly"

    public Task() {}

    public Task(String name, String date) {
        this.name = name;
        this.date = date;
        this.completed = false;
        this.repeat = "none";
    }

    public Task(int id, String name, String date, boolean completed, String repeat) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.completed = completed;
        this.repeat = repeat;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getRepeat() { return repeat; }
    public void setRepeat(String repeat) { this.repeat = repeat; }
}