package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BookDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private SqliteBookDao bookDao;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_library.db");
        dbManager = new DatabaseManager(dbPath);
        bookDao = new SqliteBookDao(dbManager);
    }

    @Test
    @DisplayName("Test Save and FindById")
    void testSaveAndFindById() {
        Book book = new Book("Design Patterns", "Gang of Four", 395, 1);
        book.setCurrentPage(100);
        book.setStatus(ReadingStatus.READING);
        book.setDateAdded(LocalDate.now());

        Book saved = bookDao.save(book);
        assertNotNull(saved.getId());

        Optional<Book> found = bookDao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Design Patterns", found.get().getTitle());
        assertEquals("Gang of Four", found.get().getAuthor());
        assertEquals(395, found.get().getTotalPages());
        assertEquals(100, found.get().getCurrentPage());
        assertEquals(ReadingStatus.READING, found.get().getStatus());
    }

    @Test
    @DisplayName("Test Update and Delete")
    void testUpdateAndDelete() {
        Book book = new Book("Refactoring", "Martin Fowler", 448, 1);
        Book saved = bookDao.save(book);

        saved.setCurrentPage(200);
        saved.setStatus(ReadingStatus.READING);
        bookDao.update(saved);

        Book updated = bookDao.findById(saved.getId()).orElseThrow();
        assertEquals(200, updated.getCurrentPage());
        assertEquals(ReadingStatus.READING, updated.getStatus());

        bookDao.delete(saved.getId());
        assertTrue(bookDao.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("Test Search and Status Filter")
    void testSearchAndFilter() {
        Book b1 = new Book("Effective Java", "Joshua Bloch", 412, 1);
        b1.setStatus(ReadingStatus.COMPLETED);
        bookDao.save(b1);

        Book b2 = new Book("Java Concurrency in Practice", "Brian Goetz", 384, 1);
        b2.setStatus(ReadingStatus.READING);
        bookDao.save(b2);

        Book b3 = new Book("Python Crash Course", "Eric Matthes", 544, 1);
        b3.setStatus(ReadingStatus.NOT_STARTED);
        bookDao.save(b3);

        // Search by query "Java"
        List<Book> searchResults = bookDao.search("Java", null, "title", true);
        assertEquals(2, searchResults.size());

        // Filter by Status COMPLETED
        List<Book> completed = bookDao.search(null, ReadingStatus.COMPLETED, "title", true);
        assertEquals(1, completed.size());
        assertEquals("Effective Java", completed.get(0).getTitle());

        // Search + Filter together
        List<Book> filteredSearch = bookDao.search("Java", ReadingStatus.READING, "title", true);
        assertEquals(1, filteredSearch.size());
        assertEquals("Java Concurrency in Practice", filteredSearch.get(0).getTitle());
    }

    @Test
    @DisplayName("Test Library Statistics Calculation")
    void testGetStatistics() {
        Book b1 = new Book("Book 1", "Author 1", 100, 1);
        b1.setCurrentPage(100);
        b1.setStatus(ReadingStatus.COMPLETED);
        bookDao.save(b1);

        Book b2 = new Book("Book 2", "Author 2", 200, 1);
        b2.setCurrentPage(50);
        b2.setStatus(ReadingStatus.READING);
        bookDao.save(b2);

        Book b3 = new Book("Book 3", "Author 3", 100, 1);
        b3.setCurrentPage(0);
        b3.setStatus(ReadingStatus.NOT_STARTED);
        bookDao.save(b3);

        LibraryStats stats = bookDao.getStatistics();
        assertEquals(3, stats.getTotalBooks());
        assertEquals(1, stats.getCompletedCount());
        assertEquals(1, stats.getReadingCount());
        assertEquals(1, stats.getNotStartedCount());
        assertEquals(400, stats.getTotalPages());
        assertEquals(150, stats.getPagesRead());
        assertEquals(37.5, stats.getOverallProgress(), 0.01);
    }
}
