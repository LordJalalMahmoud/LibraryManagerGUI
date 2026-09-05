package com.librarymanager.service;

import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.dao.SqliteReadingSessionDao;
import com.librarymanager.dao.SqliteSettingsDao;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingGoal;
import com.librarymanager.model.ReadingSession;
import com.librarymanager.model.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReadingTrackerServiceTest {

    @TempDir
    Path tempDir;

    private BookService bookService;
    private ReadingTrackerService readingTrackerService;
    private Book book;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("tracker_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        SqliteBookDao bookDao = new SqliteBookDao(dbManager);
        SqliteReadingSessionDao sessionDao = new SqliteReadingSessionDao(dbManager);
        SqliteSettingsDao settingsDao = new SqliteSettingsDao(dbManager);

        readingTrackerService = new ReadingTrackerService(sessionDao, bookDao, settingsDao);
        bookService = new BookService(bookDao, new ChapterService(), readingTrackerService);

        book = bookService.addBook(new Book("Atomic Habits", "James Clear", 300, 1));
    }

    @Test
    @DisplayName("Test Log Session Auto-Advances Book Progress")
    void testLogSessionAutoAdvance() {
        assertEquals(0, book.getCurrentPage());
        assertEquals(ReadingStatus.NOT_STARTED, book.getStatus());

        ReadingSession session = new ReadingSession(book.getId(), LocalDate.now(), 0, 45, 45, 30, "Habit loops");
        readingTrackerService.logSession(session);

        Book updated = bookService.getBookById(book.getId()).orElseThrow();
        assertEquals(45, updated.getCurrentPage());
        assertEquals(ReadingStatus.READING, updated.getStatus());
        assertNotNull(updated.getDateStarted());
    }

    @Test
    @DisplayName("Test Log Session Completing Book")
    void testLogSessionCompletingBook() {
        book.setCurrentPage(280);
        bookService.updateBook(book);

        ReadingSession session = new ReadingSession(book.getId(), LocalDate.now(), 280, 300, 20, 25, "Finished final chapter");
        readingTrackerService.logSession(session);

        Book updated = bookService.getBookById(book.getId()).orElseThrow();
        assertEquals(300, updated.getCurrentPage());
        assertEquals(ReadingStatus.COMPLETED, updated.getStatus());
        assertNotNull(updated.getDateCompleted());
    }

    @Test
    @DisplayName("Test Current Streak When Reading Today and Yesterday")
    void testCurrentStreakToday() {
        LocalDate today = LocalDate.now();
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(2), 0, 20, 20, 15, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(1), 20, 40, 20, 15, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today, 40, 60, 20, 15, ""));

        assertEquals(3, readingTrackerService.calculateCurrentStreak());
        assertTrue(readingTrackerService.hasReadToday());
    }

    @Test
    @DisplayName("Test Current Streak Alive from Yesterday When Not Read Today Yet")
    void testCurrentStreakFromYesterday() {
        LocalDate today = LocalDate.now();
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(2), 0, 20, 20, 15, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(1), 20, 40, 20, 15, ""));

        // User hasn't read today yet, but read yesterday -> streak should still be 2!
        assertEquals(2, readingTrackerService.calculateCurrentStreak());
        assertFalse(readingTrackerService.hasReadToday());
    }

    @Test
    @DisplayName("Test Broken Streak (Missed Yesterday and Today)")
    void testBrokenStreak() {
        LocalDate today = LocalDate.now();
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(3), 0, 20, 20, 15, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(2), 20, 40, 20, 15, ""));

        // Missed yesterday and today
        assertEquals(0, readingTrackerService.calculateCurrentStreak());
    }

    @Test
    @DisplayName("Test Best Streak Across History")
    void testBestStreakHistorical() {
        LocalDate today = LocalDate.now();

        // 4 consecutive days in the past
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(15), 0, 10, 10, 10, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(14), 10, 20, 10, 10, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(13), 20, 30, 10, 10, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(12), 30, 40, 10, 10, ""));

        // Then a gap...
        // Current streak: 2 days (today and yesterday)
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(1), 40, 50, 10, 10, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today, 50, 60, 10, 10, ""));

        assertEquals(2, readingTrackerService.calculateCurrentStreak());
        assertEquals(4, readingTrackerService.calculateBestStreak());
    }

    @Test
    @DisplayName("Test Daily Average Pages Calculation")
    void testDailyAveragePages() {
        LocalDate today = LocalDate.now();
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(2), 0, 30, 30, 20, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today.minusDays(1), 30, 60, 30, 20, ""));
        readingTrackerService.logSession(new ReadingSession(book.getId(), today, 60, 120, 60, 40, ""));

        // Total 120 pages over 3 days -> 40.0 pages/day
        assertEquals(120, readingTrackerService.getTotalPagesRead());
        assertEquals(40.0, readingTrackerService.calculateDailyAveragePages(), 0.01);
    }

    @Test
    @DisplayName("Test Reading Goals Configuration and Progress Tracking")
    void testGoalsTracking() {
        readingTrackerService.setGoals(30, 10);

        ReadingGoal goal = readingTrackerService.getReadingGoal();
        assertEquals(30, goal.getDailyPagesGoal());
        assertEquals(10, goal.getYearlyBooksGoal());
        assertEquals(0, goal.getPagesReadToday());
        assertFalse(goal.isDailyGoalAchieved());

        // Log 15 pages today
        readingTrackerService.logSession(new ReadingSession(book.getId(), LocalDate.now(), 0, 15, 15, 20, ""));
        ReadingGoal progress1 = readingTrackerService.getReadingGoal();
        assertEquals(15, progress1.getPagesReadToday());
        assertEquals(50.0, progress1.getDailyProgressPercentage(), 0.01);
        assertFalse(progress1.isDailyGoalAchieved());

        // Log another 20 pages today -> 35 total >= 30 target -> achieved!
        readingTrackerService.logSession(new ReadingSession(book.getId(), LocalDate.now(), 15, 35, 20, 25, ""));
        ReadingGoal progress2 = readingTrackerService.getReadingGoal();
        assertEquals(35, progress2.getPagesReadToday());
        assertTrue(progress2.isDailyGoalAchieved());
    }

    @Test
    @DisplayName("Test Validation Rules")
    void testValidation() {
        // Null book ID
        assertThrows(IllegalArgumentException.class, () -> {
            readingTrackerService.logSession(new ReadingSession(null, LocalDate.now(), 0, 10, 10, 10, ""));
        });

        // Negative start page
        assertThrows(IllegalArgumentException.class, () -> {
            ReadingSession s = new ReadingSession(book.getId(), LocalDate.now(), 0, 10, 10, 10, "");
            s.setStartPage(-1);
            readingTrackerService.logSession(s);
        });

        // End page less than start page
        assertThrows(IllegalArgumentException.class, () -> {
            ReadingSession s = new ReadingSession(book.getId(), LocalDate.now(), 50, 20, 10, 10, "");
            readingTrackerService.logSession(s);
        });
    }
}
