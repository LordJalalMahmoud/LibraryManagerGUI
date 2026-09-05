package com.librarymanager.model;

import java.util.Objects;

/**
 * Encapsulates aggregated statistics for a category/genre:
 * total books count, completed books count, pages read, and collection percentage.
 */
public class CategoryStat {
    private String category;
    private int completedCount;
    private int totalBooksCount;
    private int pagesRead;
    private double percentage;

    public CategoryStat() {
    }

    public CategoryStat(String category, int completedCount, int totalBooksCount, int pagesRead, double percentage) {
        this.category = category;
        this.completedCount = completedCount;
        this.totalBooksCount = totalBooksCount;
        this.pagesRead = pagesRead;
        this.percentage = percentage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getTotalBooksCount() {
        return totalBooksCount;
    }

    public void setTotalBooksCount(int totalBooksCount) {
        this.totalBooksCount = totalBooksCount;
    }

    public int getPagesRead() {
        return pagesRead;
    }

    public void setPagesRead(int pagesRead) {
        this.pagesRead = pagesRead;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getFormattedPercentage() {
        return String.format(java.util.Locale.US, "%.1f%%", percentage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryStat that = (CategoryStat) o;
        return completedCount == that.completedCount &&
                totalBooksCount == that.totalBooksCount &&
                pagesRead == that.pagesRead &&
                Double.compare(that.percentage, percentage) == 0 &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, completedCount, totalBooksCount, pagesRead, percentage);
    }

    @Override
    public String toString() {
        return "CategoryStat{" +
                "category='" + category + '\'' +
                ", completedCount=" + completedCount +
                ", totalBooksCount=" + totalBooksCount +
                ", pagesRead=" + pagesRead +
                ", percentage=" + percentage +
                '}';
    }
}
