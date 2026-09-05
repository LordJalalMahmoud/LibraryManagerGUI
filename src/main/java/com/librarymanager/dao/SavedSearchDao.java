package com.librarymanager.dao;

import com.librarymanager.model.SavedSearch;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for SavedSearch entities.
 */
public interface SavedSearchDao {
    SavedSearch save(SavedSearch search);
    void delete(long id);
    List<SavedSearch> findAll();
    Optional<SavedSearch> findById(long id);
}
