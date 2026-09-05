package com.librarymanager.service;

import com.librarymanager.dao.BookDao;
import com.librarymanager.dao.SavedSearchDao;
import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.dao.SqliteSavedSearchDao;
import com.librarymanager.model.Book;
import com.librarymanager.model.DuplicateGroup;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.model.SavedSearch;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating book business logic, validation, and reading status transitions.
 */
public class BookService {
    private final BookDao bookDao;
    private final ChapterService chapterService;
    private final ReadingTrackerService readingTrackerService;
    private final SavedSearchDao savedSearchDao;

    public BookService() {
        this(new SqliteBookDao(), new ChapterService(), new ReadingTrackerService(), new SqliteSavedSearchDao());
    }

    public BookService(BookDao bookDao) {
        this(bookDao, new ChapterService(), new ReadingTrackerService(), new SqliteSavedSearchDao());
    }

    public BookService(BookDao bookDao, ChapterService chapterService) {
        this(bookDao, chapterService, new ReadingTrackerService(), new SqliteSavedSearchDao());
    }

    public BookService(BookDao bookDao, ChapterService chapterService, ReadingTrackerService readingTrackerService) {
        this(bookDao, chapterService, readingTrackerService, new SqliteSavedSearchDao());
    }

    public BookService(BookDao bookDao, SavedSearchDao savedSearchDao) {
        this(bookDao, new ChapterService(), new ReadingTrackerService(), savedSearchDao);
    }

    public BookService(BookDao bookDao, ChapterService chapterService, ReadingTrackerService readingTrackerService, SavedSearchDao savedSearchDao) {
        this.bookDao = bookDao;
        this.chapterService = chapterService;
        this.readingTrackerService = readingTrackerService;
        this.savedSearchDao = savedSearchDao;
    }

    public ChapterService getChapterService() {
        return chapterService;
    }

    public ReadingTrackerService getReadingTrackerService() {
        return readingTrackerService;
    }

    /**
     * Validates book fields. Throws IllegalArgumentException on invalid data.
     */
    public void validateBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title is required.");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author name is required.");
        }
        if (book.getTotalPages() <= 0) {
            throw new IllegalArgumentException("Total pages must be greater than zero.");
        }
        if (book.getTotalParts() <= 0) {
            throw new IllegalArgumentException("Total volumes/parts must be at least 1.");
        }
        if (book.getCurrentPage() < 0) {
            throw new IllegalArgumentException("Current page cannot be negative.");
        }
        if (book.getCurrentPage() > book.getTotalPages()) {
            throw new IllegalArgumentException("Current page cannot exceed total pages (" + book.getTotalPages() + ").");
        }
        if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
            if (book.getIsbn().trim().length() > 30) {
                throw new IllegalArgumentException("ISBN is too long (maximum 30 characters).");
            }
        }
    }

    /**
     * Applies automatic status and date transitions based on page count and status rules.
     */
    public void applyReadingStateRules(Book book) {
        if (book.getCurrentPage() == book.getTotalPages() && book.getTotalPages() > 0) {
            // Reached the end -> Completed
            book.setStatus(ReadingStatus.COMPLETED);
            if (book.getDateCompleted() == null) {
                book.setDateCompleted(LocalDate.now());
            }
            if (book.getDateStarted() == null) {
                book.setDateStarted(LocalDate.now());
            }
        } else if (book.getCurrentPage() > 0 && book.getCurrentPage() < book.getTotalPages()) {
            // In between pages -> Reading
            if (book.getStatus() == ReadingStatus.NOT_STARTED || book.getStatus() == ReadingStatus.COMPLETED) {
                book.setStatus(ReadingStatus.READING);
            }
            if (book.getDateStarted() == null) {
                book.setDateStarted(LocalDate.now());
            }
            book.setDateCompleted(null);
        } else if (book.getCurrentPage() == 0) {
            // At page 0
            if (book.getStatus() == ReadingStatus.COMPLETED) {
                // If marked completed manually at 0 pages, set current page to total pages
                book.setCurrentPage(book.getTotalPages());
                if (book.getDateCompleted() == null) {
                    book.setDateCompleted(LocalDate.now());
                }
            } else if (book.getStatus() == ReadingStatus.READING) {
                if (book.getDateStarted() == null) {
                    book.setDateStarted(LocalDate.now());
                }
            }
        }

        // Additional status checks
        if (book.getStatus() == ReadingStatus.COMPLETED) {
            book.setCurrentPage(book.getTotalPages());
            if (book.getDateCompleted() == null) {
                book.setDateCompleted(LocalDate.now());
            }
        } else if (book.getStatus() == ReadingStatus.NOT_STARTED) {
            book.setCurrentPage(0);
            book.setDateStarted(null);
            book.setDateCompleted(null);
        }
    }

    /**
     * Saves a new book after validation and state processing.
     */
    public Book addBook(Book book) {
        validateBook(book);
        applyReadingStateRules(book);
        if (book.getDateAdded() == null) {
            book.setDateAdded(LocalDate.now());
        }
        return bookDao.save(book);
    }

    /**
     * Updates an existing book.
     */
    public void updateBook(Book book) {
        validateBook(book);
        applyReadingStateRules(book);
        bookDao.update(book);
    }

    /**
     * Quick progress updater: advances or decreases current page by delta.
     */
    public void advancePage(Book book, int delta) {
        int target = Math.max(0, Math.min(book.getTotalPages(), book.getCurrentPage() + delta));
        book.setCurrentPage(target);
        updateBook(book);
    }

    /**
     * Quick action: marks book as completed.
     */
    public void markAsCompleted(Book book) {
        book.setCurrentPage(book.getTotalPages());
        book.setStatus(ReadingStatus.COMPLETED);
        book.setDateCompleted(LocalDate.now());
        if (book.getDateStarted() == null) {
            book.setDateStarted(LocalDate.now());
        }
        updateBook(book);
    }

    /**
     * Quick action: marks book as reading.
     */
    public void startReading(Book book) {
        book.setStatus(ReadingStatus.READING);
        if (book.getCurrentPage() == 0) {
            book.setCurrentPage(1);
        }
        if (book.getDateStarted() == null) {
            book.setDateStarted(LocalDate.now());
        }
        book.setDateCompleted(null);
        updateBook(book);
    }

    public void deleteBook(long id) {
        bookDao.delete(id);
    }

    public Optional<Book> getBookById(long id) {
        return bookDao.findById(id);
    }

    public List<Book> getAllBooks() {
        return bookDao.findAll();
    }

    public List<Book> getBooksByStatus(ReadingStatus status) {
        return bookDao.findByStatus(status);
    }

    public List<Book> searchBooks(String query, ReadingStatus statusFilter, String sortBy, boolean ascending) {
        return bookDao.search(query, statusFilter, sortBy, ascending);
    }

    public List<Book> searchBooks(String query, ReadingStatus statusFilter, String categoryFilter, String tagFilter,
                                  Boolean isFavorite, Boolean isWishlist, String sortBy, boolean ascending) {
        return bookDao.search(query, statusFilter, categoryFilter, tagFilter, isFavorite, isWishlist, sortBy, ascending);
    }

    public List<Book> searchBooks(String query, String authorQuery, ReadingStatus statusFilter, String categoryFilter,
                                  String tagFilter, Boolean isFavorite, Boolean isWishlist, Integer minPages, Integer maxPages,
                                  String sortBy, boolean ascending) {
        return bookDao.search(query, authorQuery, statusFilter, categoryFilter, tagFilter, isFavorite, isWishlist, minPages, maxPages, sortBy, ascending);
    }

    public List<DuplicateGroup> findDuplicates() {
        return bookDao.findDuplicates();
    }

    public void resolveDuplicate(long bookToKeepId, long bookToDeleteId, boolean mergeProgress) {
        bookDao.resolveDuplicate(bookToKeepId, bookToDeleteId, mergeProgress);
    }

    public void bulkMarkAsCompleted(List<Long> bookIds) {
        bookDao.bulkMarkAsCompleted(bookIds);
    }

    public void bulkMarkAsReading(List<Long> bookIds) {
        bookDao.bulkMarkAsReading(bookIds);
    }

    public void bulkDelete(List<Long> bookIds) {
        bookDao.bulkDelete(bookIds);
    }

    public void bulkUpdateCategory(List<Long> bookIds, String newCategory) {
        bookDao.bulkUpdateCategory(bookIds, newCategory);
    }

    public void bulkAddTag(List<Long> bookIds, String tag) {
        bookDao.bulkAddTag(bookIds, tag);
    }

    public SavedSearch saveSearch(SavedSearch search) {
        if (search == null || search.getName() == null || search.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Search name is required.");
        }
        return savedSearchDao.save(search);
    }

    public List<SavedSearch> getAllSavedSearches() {
        return savedSearchDao.findAll();
    }

    public void deleteSavedSearch(long id) {
        savedSearchDao.delete(id);
    }

    public List<String> getAllCategories() {
        return bookDao.findAllCategories();
    }

    public List<String> getAllTags() {
        return bookDao.findAllTags();
    }

    public void toggleFavorite(long id, boolean isFavorite) {
        bookDao.toggleFavorite(id, isFavorite);
    }

    public void toggleWishlist(long id, boolean isWishlist) {
        bookDao.toggleWishlist(id, isWishlist);
    }

    public LibraryStats getLibraryStatistics() {
        return bookDao.getStatistics();
    }

    public void resetLibrary() {
        bookDao.deleteAll();
    }
}
