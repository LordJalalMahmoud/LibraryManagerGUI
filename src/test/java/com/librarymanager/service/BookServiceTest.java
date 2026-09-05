package com.librarymanager.service;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {

    @TempDir
    Path tempDir;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("service_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        SqliteBookDao dao = new SqliteBookDao(dbManager);
        bookService = new BookService(dao);
    }

    @Test
    @DisplayName("Test validation fails on invalid inputs")
    void testValidation() {
        // Empty title
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.addBook(new Book("", "Author", 200, 1));
        });

        // Empty author
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.addBook(new Book("Title", "", 200, 1));
        });

        // Zero total pages
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.addBook(new Book("Title", "Author", 0, 1));
        });

        // Negative current page
        assertThrows(IllegalArgumentException.class, () -> {
            Book b = new Book("Title", "Author", 100, 1);
            b.setCurrentPage(-5);
            bookService.addBook(b);
        });

        // Current page exceeding total pages
        assertThrows(IllegalArgumentException.class, () -> {
            Book b = new Book("Title", "Author", 100, 1);
            b.setCurrentPage(101);
            bookService.addBook(b);
        });
    }

    @Test
    @DisplayName("Test automatic status transition when current page reaches total pages")
    void testAutoCompletionTransition() {
        Book book = new Book("Clean Code", "Uncle Bob", 464, 1);
        book.setCurrentPage(464);

        Book saved = bookService.addBook(book);
        assertEquals(ReadingStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getDateCompleted());
        assertEquals(LocalDate.now(), saved.getDateCompleted());
    }

    @Test
    @DisplayName("Test automatic status transition to READING when page advances")
    void testAutoReadingTransition() {
        Book book = new Book("The Pragmatic Programmer", "Andrew Hunt", 350, 1);
        book.setStatus(ReadingStatus.NOT_STARTED);
        book.setCurrentPage(0);

        Book saved = bookService.addBook(book);
        assertEquals(ReadingStatus.NOT_STARTED, saved.getStatus());

        bookService.advancePage(saved, 20);
        Book updated = bookService.getBookById(saved.getId()).orElseThrow();

        assertEquals(20, updated.getCurrentPage());
        assertEquals(ReadingStatus.READING, updated.getStatus());
        assertNotNull(updated.getDateStarted());
        assertNull(updated.getDateCompleted());
    }

    @Test
    @DisplayName("Test mark as completed")
    void testMarkAsCompleted() {
        Book book = new Book("Effective Java", "Joshua Bloch", 412, 1);
        Book saved = bookService.addBook(book);

        bookService.markAsCompleted(saved);
        Book updated = bookService.getBookById(saved.getId()).orElseThrow();

        assertEquals(412, updated.getCurrentPage());
        assertEquals(ReadingStatus.COMPLETED, updated.getStatus());
        assertEquals(LocalDate.now(), updated.getDateCompleted());
    }
}
