package com.librarymanager.model;

import java.util.Objects;

/**
 * Encapsulates aggregated statistics for an author:
 * total books in library, completed books, and pages read.
 */
public class AuthorStat {
    private String author;
    private int completedCount;
    private int totalBooksCount;
    private int pagesRead;

    public AuthorStat() {
    }

    public AuthorStat(String author, int completedCount, int totalBooksCount, int pagesRead) {
        this.author = author;
        this.completedCount = completedCount;
        this.totalBooksCount = totalBooksCount;
        this.pagesRead = pagesRead;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorStat that = (AuthorStat) o;
        return completedCount == that.completedCount &&
                totalBooksCount == that.totalBooksCount &&
                pagesRead == that.pagesRead &&
                Objects.equals(author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, completedCount, totalBooksCount, pagesRead);
    }

    @Override
    public String toString() {
        return "AuthorStat{" +
                "author='" + author + '\'' +
                ", completedCount=" + completedCount +
                ", totalBooksCount=" + totalBooksCount +
                ", pagesRead=" + pagesRead +
                '}';
    }
}
