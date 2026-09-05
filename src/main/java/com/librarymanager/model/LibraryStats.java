package com.librarymanager.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for library statistics and dashboard metrics.
 */
public class LibraryStats {
    private int totalBooks;
    private int readingCount;
    private int completedCount;
    private int notStartedCount;
    private int totalPages;
    private int pagesRead;
    private List<Book> recentlyAdded = new ArrayList<>();
    private List<Book> recentlyCompleted = new ArrayList<>();

    public LibraryStats() {
    }

    public int getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }

    public int getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(int readingCount) {
        this.readingCount = readingCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getNotStartedCount() {
        return notStartedCount;
    }

    public void setNotStartedCount(int notStartedCount) {
        this.notStartedCount = notStartedCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPagesRead() {
        return pagesRead;
    }

    public void setPagesRead(int pagesRead) {
        this.pagesRead = pagesRead;
    }

    public double getOverallProgress() {
        if (totalPages <= 0) {
            return 0.0;
        }
        double pct = ((double) pagesRead / (double) totalPages) * 100.0;
        return Math.min(100.0, Math.max(0.0, pct));
    }

    public String getFormattedOverallProgress() {
        return String.format("%.1f%%", getOverallProgress());
    }

    public List<Book> getRecentlyAdded() {
        return recentlyAdded;
    }

    public void setRecentlyAdded(List<Book> recentlyAdded) {
        this.recentlyAdded = recentlyAdded != null ? recentlyAdded : new ArrayList<>();
    }

    public List<Book> getRecentlyCompleted() {
        return recentlyCompleted;
    }

    public void setRecentlyCompleted(List<Book> recentlyCompleted) {
        this.recentlyCompleted = recentlyCompleted != null ? recentlyCompleted : new ArrayList<>();
    }
}
