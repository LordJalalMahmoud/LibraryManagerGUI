package com.librarymanager.service;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private BookService bookService;
    private BackupService backupService;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("backup_test.db");
        dbManager = new DatabaseManager(dbPath);
        bookService = new BookService(new SqliteBookDao(dbManager));
        backupService = new BackupService(dbManager);
    }

    @Test
    @DisplayName("Test Export and Restore Database")
    void testExportAndRestore() throws IOException, SQLException {
        // Add a book
        Book book = new Book("Original Book", "Author", 300, 1);
        bookService.addBook(book);
        assertEquals(1, bookService.getAllBooks().size());

        // Export backup
        File backupFile = tempDir.resolve("my_backup.db").toFile();
        backupService.exportBackup(backupFile);
        assertTrue(backupFile.exists());
        assertTrue(backupFile.length() > 0);

        // Modify database (add 2 more books)
        bookService.addBook(new Book("Extra 1", "Author", 100, 1));
        bookService.addBook(new Book("Extra 2", "Author", 100, 1));
        assertEquals(3, bookService.getAllBooks().size());

        // Restore backup
        backupService.restoreBackup(backupFile);

        // After restore, should only have the 1 original book
        List<Book> restoredBooks = bookService.getAllBooks();
        assertEquals(1, restoredBooks.size());
        assertEquals("Original Book", restoredBooks.get(0).getTitle());
    }

    @Test
    @DisplayName("Test restoring invalid backup file fails gracefully")
    void testRestoreInvalidFile() throws IOException {
        File invalidFile = tempDir.resolve("corrupt.db").toFile();
        Files.writeString(invalidFile.toPath(), "This is not an SQLite database file!");

        assertThrows(SQLException.class, () -> {
            backupService.restoreBackup(invalidFile);
        });
    }
}
