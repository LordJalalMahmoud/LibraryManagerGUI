package com.librarymanager.dao;

import com.librarymanager.model.AuthorStat;
import com.librarymanager.model.Book;
import com.librarymanager.model.CategoryStat;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;

import java.util.List;
import java.util.Map;
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
    List<Book> search(String query, ReadingStatus statusFilter, String categoryFilter, String tagFilter, Boolean isFavorite, Boolean isWishlist, String sortBy, boolean ascending);

    default List<Book> search(String query, ReadingStatus statusFilter, String sortBy, boolean ascending) {
        return search(query, statusFilter, null, null, null, null, sortBy, ascending);
    }

    List<String> findAllCategories();
    List<String> findAllTags();
    void toggleFavorite(long id, boolean isFavorite);
    void toggleWishlist(long id, boolean isWishlist);

    LibraryStats getStatistics();
    void deleteAll();

    // v1.3 Analytics & Statistics
    Map<Integer, Integer> getBooksCompletedByMonthInYear(int year);
    List<AuthorStat> getTopAuthors(int limit);
    List<CategoryStat> getTopCategories(int limit);
    AuthorStat getTopAuthorInYear(int year);
    CategoryStat getTopCategoryInYear(int year);
    List<Integer> getDistinctCompletedYears();
    int countBooksCompletedInYear(int year);
}
