package com.librarymanager.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a single reading session logged for a book.
 * Records the date, page range, pages read count, duration in minutes, and optional notes.
 */
public class ReadingSession {
    private Long id;
    private Long bookId;
    private String bookTitle; // Convenience field for feeds/displays
    private LocalDate sessionDate = LocalDate.now();
    private int startPage;
    private int endPage;
    private int pagesRead;
    private int durationMinutes;
    private String notes;

    public ReadingSession() {
    }

    public ReadingSession(Long bookId, LocalDate sessionDate, int startPage, int endPage, int pagesRead, int durationMinutes, String notes) {
        this.bookId = bookId;
        this.sessionDate = sessionDate != null ? sessionDate : LocalDate.now();
        this.startPage = startPage;
        this.endPage = endPage;
        this.pagesRead = pagesRead;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
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

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate != null ? sessionDate : LocalDate.now();
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = endPage;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFormattedDuration() {
        if (durationMinutes <= 0) {
            return "—";
        }
        if (durationMinutes < 60) {
            return durationMinutes + "m";
        }
        int hours = durationMinutes / 60;
        int mins = durationMinutes % 60;
        if (mins == 0) {
            return hours + "h";
        }
        return hours + "h " + mins + "m";
    }

    public String getPageRangeString() {
        if (startPage > 0 && endPage >= startPage) {
            return startPage + " - " + endPage;
        } else if (endPage > 0) {
            return String.valueOf(endPage);
        }
        return "—";
    }

    public double getReadingSpeedPagesPerMinute() {
        if (durationMinutes <= 0 || pagesRead <= 0) {
            return 0.0;
        }
        return (double) pagesRead / (double) durationMinutes;
    }

    public String getFormattedSpeedPpm() {
        double ppm = getReadingSpeedPagesPerMinute();
        if (ppm <= 0.0) {
            return "—";
        }
        return String.format(java.util.Locale.US, "%.1f", ppm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadingSession that = (ReadingSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReadingSession{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", sessionDate=" + sessionDate +
                ", pagesRead=" + pagesRead +
                ", durationMinutes=" + durationMinutes +
                '}';
    }
}
