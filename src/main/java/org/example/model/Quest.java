package org.example.model;

public class Quest {
    private String title;
    private String date;
    private int point;
    private boolean isCompleted;

    public Quest() {
    }

    public Quest(String title, String date, int point) {
        this.title = title;
        this.date = date;
        this.point = point;
        this.isCompleted = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}