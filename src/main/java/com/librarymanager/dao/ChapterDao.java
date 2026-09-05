package com.librarymanager.dao;

import com.librarymanager.model.Chapter;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Chapter entities.
 */
public interface ChapterDao {
    Chapter save(Chapter chapter);
    void update(Chapter chapter);
    void delete(long id);
    Optional<Chapter> findById(long id);
    List<Chapter> findByBookId(long bookId);
    List<Chapter> findAll();
    void toggleCompletion(long id, boolean isCompleted);
    int countCompletedByBookId(long bookId);
    int countTotalByBookId(long bookId);
}
