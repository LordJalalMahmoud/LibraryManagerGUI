package com.librarymanager.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a cluster of books that have been identified as potential duplicates
 * based on matching ISBN or matching normalized Title and Author.
 */
public class DuplicateGroup {
    private String matchReason;
    private List<Book> books = new ArrayList<>();

    public DuplicateGroup() {
    }

    public DuplicateGroup(String matchReason, List<Book> books) {
        this.matchReason = matchReason;
        this.books = books != null ? books : new ArrayList<>();
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books != null ? books : new ArrayList<>();
    }

    public int size() {
        return books.size();
    }
}
