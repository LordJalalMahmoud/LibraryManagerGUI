package com.librarymanager.model;

import com.librarymanager.util.DateUtil;
import java.util.Objects;

/**
 * Encapsulates full annual reading statistics for a specific year:
 * books completed, pages read, reading duration, reading speed, top author, top category,
 * and yearly challenge goal progress.
 */
public class YearlyReadingSummary {
    private int year;
    private int booksCompleted;
    private int pagesRead;
    private int readingTimeMinutes;
    private double readingSpeedPagesPerHour;
    private String topAuthor;
    private int topAuthorBooks;
    private String topCategory;
    private int topCategoryBooks;
    private int yearlyGoal;

    public YearlyReadingSummary() {
    }

    public YearlyReadingSummary(int year, int booksCompleted, int pagesRead, int readingTimeMinutes,
                                double readingSpeedPagesPerHour, String topAuthor, int topAuthorBooks,
                                String topCategory, int topCategoryBooks, int yearlyGoal) {
        this.year = year;
        this.booksCompleted = booksCompleted;
        this.pagesRead = pagesRead;
        this.readingTimeMinutes = readingTimeMinutes;
        this.readingSpeedPagesPerHour = readingSpeedPagesPerHour;
        this.topAuthor = topAuthor;
        this.topAuthorBooks = topAuthorBooks;
        this.topCategory = topCategory;
        this.topCategoryBooks = topCategoryBooks;
        this.yearlyGoal = yearlyGoal;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
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

    public int getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public void setReadingTimeMinutes(int readingTimeMinutes) {
        this.readingTimeMinutes = readingTimeMinutes;
    }

    public double getReadingSpeedPagesPerHour() {
        return readingSpeedPagesPerHour;
    }

    public void setReadingSpeedPagesPerHour(double readingSpeedPagesPerHour) {
        this.readingSpeedPagesPerHour = readingSpeedPagesPerHour;
    }

    public String getTopAuthor() {
        return topAuthor;
    }

    public void setTopAuthor(String topAuthor) {
        this.topAuthor = topAuthor;
    }

    public int getTopAuthorBooks() {
        return topAuthorBooks;
    }

    public void setTopAuthorBooks(int topAuthorBooks) {
        this.topAuthorBooks = topAuthorBooks;
    }

    public String getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(String topCategory) {
        this.topCategory = topCategory;
    }

    public int getTopCategoryBooks() {
        return topCategoryBooks;
    }

    public void setTopCategoryBooks(int topCategoryBooks) {
        this.topCategoryBooks = topCategoryBooks;
    }

    public int getYearlyGoal() {
        return yearlyGoal;
    }

    public void setYearlyGoal(int yearlyGoal) {
        this.yearlyGoal = yearlyGoal;
    }

    public boolean isGoalAchieved() {
        return yearlyGoal > 0 && booksCompleted >= yearlyGoal;
    }

    public double getGoalProgressRatio() {
        if (yearlyGoal <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) booksCompleted / (double) yearlyGoal);
    }

    public String getFormattedReadingTime() {
        return DateUtil.formatDuration(readingTimeMinutes);
    }

    public String getFormattedReadingSpeed() {
        return DateUtil.formatReadingSpeed(readingSpeedPagesPerHour);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YearlyReadingSummary that = (YearlyReadingSummary) o;
        return year == that.year &&
                booksCompleted == that.booksCompleted &&
                pagesRead == that.pagesRead &&
                readingTimeMinutes == that.readingTimeMinutes &&
                Double.compare(that.readingSpeedPagesPerHour, readingSpeedPagesPerHour) == 0 &&
                topAuthorBooks == that.topAuthorBooks &&
                topCategoryBooks == that.topCategoryBooks &&
                yearlyGoal == that.yearlyGoal &&
                Objects.equals(topAuthor, that.topAuthor) &&
                Objects.equals(topCategory, that.topCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, booksCompleted, pagesRead, readingTimeMinutes, readingSpeedPagesPerHour, topAuthor, topAuthorBooks, topCategory, topCategoryBooks, yearlyGoal);
    }

    @Override
    public String toString() {
        return "YearlyReadingSummary{" +
                "year=" + year +
                ", booksCompleted=" + booksCompleted +
                ", pagesRead=" + pagesRead +
                ", readingTimeMinutes=" + readingTimeMinutes +
                ", readingSpeedPagesPerHour=" + readingSpeedPagesPerHour +
                ", topAuthor='" + topAuthor + '\'' +
                ", topCategory='" + topCategory + '\'' +
                ", yearlyGoal=" + yearlyGoal +
                '}';
    }
}
