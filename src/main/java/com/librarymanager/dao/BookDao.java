package com.librarymanager.dao;

import com.librarymanager.model.Book;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Book entities.
 */
public interface BookDao {
    Book save(Book book);
    void update(Book book);
    void delete(long id);
    Optional<Book> findById(long id);
    List<Book> findAll();
    List<Book> findByStatus(ReadingStatus status);
    List<Book> search(String query, ReadingStatus statusFilter, String sortBy, boolean ascending);
    LibraryStats getStatistics();
    void deleteAll();
}
