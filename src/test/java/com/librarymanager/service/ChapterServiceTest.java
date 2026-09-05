package com.librarymanager.service;

import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.dao.SqliteChapterDao;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.Chapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChapterServiceTest {

    @TempDir
    Path tempDir;

    private BookService bookService;
    private ChapterService chapterService;
    private Book testBook;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("chapter_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        SqliteBookDao bookDao = new SqliteBookDao(dbManager);
        SqliteChapterDao chapterDao = new SqliteChapterDao(dbManager);
        chapterService = new ChapterService(chapterDao);
        bookService = new BookService(bookDao, chapterService);

        testBook = bookService.addBook(new Book("University Algorithms", "Prof. Smith", 600, 1));
    }

    @Test
    @DisplayName("Test Add Chapter and Retrieval")
    void testAddAndRetrieveChapters() {
        Chapter ch1 = new Chapter(testBook.getId(), 1, "Asymptotic Analysis", 1, 30);
        ch1.setNotes("Week 1 lecture");
        Chapter saved1 = chapterService.addChapter(ch1);
        assertNotNull(saved1.getId());

        Chapter ch2 = new Chapter(testBook.getId(), 2, "Divide and Conquer", 31, 75);
        ch2.setCompleted(true);
        chapterService.addChapter(ch2);

        List<Chapter> chapters = chapterService.getChaptersByBookId(testBook.getId());
        assertEquals(2, chapters.size());
        assertEquals("Asymptotic Analysis", chapters.get(0).getTitle());
        assertEquals(1, chapters.get(0).getChapterNumber());
        assertEquals("Divide and Conquer", chapters.get(1).getTitle());

        assertEquals(2, chapterService.getTotalChaptersCount(testBook.getId()));
        assertEquals(1, chapterService.getCompletedChaptersCount(testBook.getId()));
        assertEquals(50.0, chapterService.getChapterProgressPercentage(testBook.getId()), 0.01);
    }

    @Test
    @DisplayName("Test Chapter Validation")
    void testChapterValidation() {
        // Empty title
        assertThrows(IllegalArgumentException.class, () -> {
            chapterService.addChapter(new Chapter(testBook.getId(), 1, "", 1, 20));
        });

        // Negative start page
        assertThrows(IllegalArgumentException.class, () -> {
            Chapter c = new Chapter(testBook.getId(), 1, "Title", -5, 20);
            chapterService.addChapter(c);
        });

        // End page less than start page
        assertThrows(IllegalArgumentException.class, () -> {
            Chapter c = new Chapter(testBook.getId(), 1, "Title", 50, 20);
            chapterService.addChapter(c);
        });
    }

    @Test
    @DisplayName("Test Toggle Chapter Completion")
    void testToggleCompletion() {
        Chapter ch = chapterService.addChapter(new Chapter(testBook.getId(), 1, "Binary Trees", 100, 140));
        assertFalse(ch.isCompleted());

        chapterService.toggleChapter(ch.getId(), true);
        Chapter updated = chapterService.getChapterById(ch.getId()).orElseThrow();
        assertTrue(updated.isCompleted());

        chapterService.toggleChapter(ch.getId(), false);
        Chapter toggledBack = chapterService.getChapterById(ch.getId()).orElseThrow();
        assertFalse(toggledBack.isCompleted());
    }

    @Test
    @DisplayName("Test Delete Chapter and Cascade Deletion")
    void testDeleteChapter() {
        Chapter ch = chapterService.addChapter(new Chapter(testBook.getId(), 1, "Graph Theory", 200, 250));
        assertEquals(1, chapterService.getChaptersByBookId(testBook.getId()).size());

        chapterService.deleteChapter(ch.getId());
        assertEquals(0, chapterService.getChaptersByBookId(testBook.getId()).size());

        // Test cascade deletion when book is deleted
        Chapter ch2 = chapterService.addChapter(new Chapter(testBook.getId(), 2, "Dynamic Programming", 260, 310));
        assertEquals(1, chapterService.getChaptersByBookId(testBook.getId()).size());

        bookService.deleteBook(testBook.getId());
        assertEquals(0, chapterService.getChaptersByBookId(testBook.getId()).size());
    }
}
