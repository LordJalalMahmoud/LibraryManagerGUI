package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReadingSessionDaoTest {

    @TempDir
    Path tempDir;

    private SqliteBookDao bookDao;
    private SqliteReadingSessionDao sessionDao;
    private Book testBook;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("session_dao_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        bookDao = new SqliteBookDao(dbManager);
        sessionDao = new SqliteReadingSessionDao(dbManager);

        testBook = bookDao.save(new Book("Test Engineering", "Author A", 500, 1));
    }

    @Test
    @DisplayName("Test Save, Find by ID and Map Fields")
    void testSaveAndFind() {
        ReadingSession session = new ReadingSession(
                testBook.getId(),
                LocalDate.now(),
                10,
                50,
                40,
                35,
                "Covered chapter 1"
        );

        ReadingSession saved = sessionDao.save(session);
        assertNotNull(saved.getId());

        ReadingSession retrieved = sessionDao.findById(saved.getId()).orElseThrow();
        assertEquals(testBook.getId(), retrieved.getBookId());
        assertEquals(10, retrieved.getStartPage());
        assertEquals(50, retrieved.getEndPage());
        assertEquals(40, retrieved.getPagesRead());
        assertEquals(35, retrieved.getDurationMinutes());
        assertEquals("Covered chapter 1", retrieved.getNotes());
        assertEquals("Test Engineering", retrieved.getBookTitle());
    }

    @Test
    @DisplayName("Test Find By Book ID and Sorting")
    void testFindByBookId() {
        sessionDao.save(new ReadingSession(testBook.getId(), LocalDate.now().minusDays(2), 1, 30, 30, 25, "Note 1"));
        sessionDao.save(new ReadingSession(testBook.getId(), LocalDate.now(), 31, 80, 50, 45, "Note 2"));

        List<ReadingSession> list = sessionDao.findByBookId(testBook.getId());
        assertEquals(2, list.size());
        assertEquals(LocalDate.now(), list.get(0).getSessionDate());
        assertEquals(LocalDate.now().minusDays(2), list.get(1).getSessionDate());
    }

    @Test
    @DisplayName("Test Update and Delete Session")
    void testUpdateAndDelete() {
        ReadingSession session = sessionDao.save(new ReadingSession(testBook.getId(), LocalDate.now(), 1, 20, 20, 20, "Init"));

        session.setPagesRead(25);
        session.setEndPage(25);
        session.setNotes("Updated notes");
        sessionDao.update(session);

        ReadingSession updated = sessionDao.findById(session.getId()).orElseThrow();
        assertEquals(25, updated.getPagesRead());
        assertEquals(25, updated.getEndPage());
        assertEquals("Updated notes", updated.getNotes());

        sessionDao.delete(session.getId());
        assertTrue(sessionDao.findById(session.getId()).isEmpty());
    }

    @Test
    @DisplayName("Test Cascade Deletion when Book is Deleted")
    void testCascadeDelete() {
        sessionDao.save(new ReadingSession(testBook.getId(), LocalDate.now(), 1, 50, 50, 40, "Session A"));
        assertEquals(1, sessionDao.findByBookId(testBook.getId()).size());

        bookDao.delete(testBook.getId());
        assertEquals(0, sessionDao.findByBookId(testBook.getId()).size());
    }

    @Test
    @DisplayName("Test Date Aggregations and Distinct Dates")
    void testDateAggregations() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        sessionDao.save(new ReadingSession(testBook.getId(), today, 1, 20, 20, 15, null));
        sessionDao.save(new ReadingSession(testBook.getId(), today, 21, 50, 30, 20, null));
        sessionDao.save(new ReadingSession(testBook.getId(), yesterday, 51, 80, 30, 25, null));

        assertEquals(80, sessionDao.getTotalPagesRead());
        assertEquals(50, sessionDao.getPagesReadOnDate(today));
        assertEquals(30, sessionDao.getPagesReadOnDate(yesterday));

        List<LocalDate> distinctDates = sessionDao.getDistinctSessionDates();
        assertEquals(2, distinctDates.size());
        assertTrue(distinctDates.contains(today));
        assertTrue(distinctDates.contains(yesterday));
    }
}
