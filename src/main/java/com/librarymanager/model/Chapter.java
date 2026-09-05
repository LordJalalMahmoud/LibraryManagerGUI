package com.librarymanager.model;

import java.util.Objects;

/**
 * Represents a specific chapter or reading assignment within a book.
 * Specially designed for university course reading assignments.
 */
public class Chapter {
    private Long id;
    private Long bookId;
    private int chapterNumber = 1;
    private String title;
    private int startPage;
    private int endPage;
    private boolean completed;
    private String notes;

    public Chapter() {
    }

    public Chapter(Long bookId, int chapterNumber, String title, int startPage, int endPage) {
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.startPage = startPage;
        this.endPage = endPage;
        this.completed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = Math.max(1, chapterNumber);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : "";
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = Math.max(0, startPage);
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = Math.max(0, endPage);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getPageCount() {
        if (endPage >= startPage && startPage > 0) {
            return (endPage - startPage) + 1;
        }
        return 0;
    }

    public String getPageRangeString() {
        if (startPage > 0 && endPage >= startPage) {
            return startPage + " - " + endPage;
        } else if (startPage > 0) {
            return String.valueOf(startPage);
        }
        return "—";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chapter chapter = (Chapter) o;
        return Objects.equals(id, chapter.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", chapterNumber=" + chapterNumber +
                ", title='" + title + '\'' +
                ", completed=" + completed +
                '}';
    }
}
