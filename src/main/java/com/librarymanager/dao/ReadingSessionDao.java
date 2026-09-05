package com.librarymanager.dao;

import com.librarymanager.model.ReadingSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for ReadingSession entities.
 */
public interface ReadingSessionDao {
    ReadingSession save(ReadingSession session);
    void update(ReadingSession session);
    void delete(long id);
    Optional<ReadingSession> findById(long id);
    List<ReadingSession> findByBookId(long bookId);
    List<ReadingSession> findRecent(int limit);
    List<ReadingSession> findAll();
    int getTotalPagesRead();
    int getPagesReadOnDate(LocalDate date);
    List<LocalDate> getDistinctSessionDates();
    int countSessions();
}
