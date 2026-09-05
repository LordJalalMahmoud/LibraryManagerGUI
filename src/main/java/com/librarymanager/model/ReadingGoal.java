package com.librarymanager.model;

/**
 * Encapsulates user's reading goals and real-time progress.
 * Covers daily page target and annual book reading challenge.
 */
public class ReadingGoal {
    private int dailyPagesGoal = 25;
    private int yearlyBooksGoal = 12;
    private int pagesReadToday = 0;
    private int booksCompletedThisYear = 0;

    public ReadingGoal() {
    }

    public ReadingGoal(int dailyPagesGoal, int yearlyBooksGoal, int pagesReadToday, int booksCompletedThisYear) {
        this.dailyPagesGoal = Math.max(1, dailyPagesGoal);
        this.yearlyBooksGoal = Math.max(1, yearlyBooksGoal);
        this.pagesReadToday = Math.max(0, pagesReadToday);
        this.booksCompletedThisYear = Math.max(0, booksCompletedThisYear);
    }

    public int getDailyPagesGoal() {
        return dailyPagesGoal;
    }

    public void setDailyPagesGoal(int dailyPagesGoal) {
        this.dailyPagesGoal = Math.max(1, dailyPagesGoal);
    }

    public int getYearlyBooksGoal() {
        return yearlyBooksGoal;
    }

    public void setYearlyBooksGoal(int yearlyBooksGoal) {
        this.yearlyBooksGoal = Math.max(1, yearlyBooksGoal);
    }

    public int getPagesReadToday() {
        return pagesReadToday;
    }

    public void setPagesReadToday(int pagesReadToday) {
        this.pagesReadToday = Math.max(0, pagesReadToday);
    }

    public int getBooksCompletedThisYear() {
        return booksCompletedThisYear;
    }

    public void setBooksCompletedThisYear(int booksCompletedThisYear) {
        this.booksCompletedThisYear = Math.max(0, booksCompletedThisYear);
    }

    public double getDailyProgressRatio() {
        if (dailyPagesGoal <= 0) return 0.0;
        return Math.min(1.0, (double) pagesReadToday / (double) dailyPagesGoal);
    }

    public double getDailyProgressPercentage() {
        return getDailyProgressRatio() * 100.0;
    }

    public double getYearlyProgressRatio() {
        if (yearlyBooksGoal <= 0) return 0.0;
        return Math.min(1.0, (double) booksCompletedThisYear / (double) yearlyBooksGoal);
    }

    public double getYearlyProgressPercentage() {
        return getYearlyProgressRatio() * 100.0;
    }

    public boolean isDailyGoalAchieved() {
        return pagesReadToday >= dailyPagesGoal;
    }

    public boolean isYearlyGoalAchieved() {
        return booksCompletedThisYear >= yearlyBooksGoal;
    }

    @Override
    public String toString() {
        return "ReadingGoal{" +
                "dailyPagesGoal=" + dailyPagesGoal +
                ", yearlyBooksGoal=" + yearlyBooksGoal +
                ", pagesReadToday=" + pagesReadToday +
                ", booksCompletedThisYear=" + booksCompletedThisYear +
                '}';
    }
}
