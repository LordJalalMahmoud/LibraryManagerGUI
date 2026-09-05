package com.librarymanager.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object encapsulating complete library state for JSON export & import.
 */
public class LibraryExportData {
    private String version = "1.5.0";
    private String exportDate;
    private int totalBooks;
    private List<Book> books = new ArrayList<>();
    private List<Chapter> chapters = new ArrayList<>();
    private List<ReadingSession> readingSessions = new ArrayList<>();
    private List<SavedSearch> savedSearches = new ArrayList<>();
    private Map<String, String> settings = new LinkedHashMap<>();

    public LibraryExportData() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExportDate() {
        return exportDate;
    }

    public void setExportDate(String exportDate) {
        this.exportDate = exportDate;
    }

    public int getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books != null ? books : new ArrayList<>();
        this.totalBooks = this.books.size();
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters != null ? chapters : new ArrayList<>();
    }

    public List<ReadingSession> getReadingSessions() {
        return readingSessions;
    }

    public void setReadingSessions(List<ReadingSession> readingSessions) {
        this.readingSessions = readingSessions != null ? readingSessions : new ArrayList<>();
    }

    public List<SavedSearch> getSavedSearches() {
        return savedSearches;
    }

    public void setSavedSearches(List<SavedSearch> savedSearches) {
        this.savedSearches = savedSearches != null ? savedSearches : new ArrayList<>();
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings != null ? settings : new LinkedHashMap<>();
    }
}
