package com.librarymanager.model;

import java.util.Objects;

/**
 * Encapsulates reading metrics for a specific month (1-12):
 * number of books completed, pages read, and reading duration in minutes.
 */
public class MonthlyReadingStat {
    private int month; // 1 to 12
    private int booksCompleted;
    private int pagesRead;
    private int durationMinutes;

    public MonthlyReadingStat() {
    }

    public MonthlyReadingStat(int month, int booksCompleted, int pagesRead, int durationMinutes) {
        this.month = month;
        this.booksCompleted = booksCompleted;
        this.pagesRead = pagesRead;
        this.durationMinutes = durationMinutes;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getBooksCompleted() {
        return booksCompleted;
    }

    public void setBooksCompleted(int booksCompleted) {
        this.booksCompleted = booksCompleted;
    }

    public int getPagesRead() {
        return pagesRead;
    }

    public void setPagesRead(int pagesRead) {
        this.pagesRead = pagesRead;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonthlyReadingStat that = (MonthlyReadingStat) o;
        return month == that.month &&
                booksCompleted == that.booksCompleted &&
                pagesRead == that.pagesRead &&
                durationMinutes == that.durationMinutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(month, booksCompleted, pagesRead, durationMinutes);
    }

    @Override
    public String toString() {
        return "MonthlyReadingStat{" +
                "month=" + month +
                ", booksCompleted=" + booksCompleted +
                ", pagesRead=" + pagesRead +
                ", durationMinutes=" + durationMinutes +
                '}';
    }
}
