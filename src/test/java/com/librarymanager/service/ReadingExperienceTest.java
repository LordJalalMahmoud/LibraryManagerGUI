package com.librarymanager.service;

import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.dao.SqliteReadingSessionDao;
import com.librarymanager.dao.SqliteSettingsDao;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingSession;
import com.librarymanager.model.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for v1.7 Reading Experience features:
 * - Live speed calculation (Pages Per Minute)
 * - Estimated time to finish (ETA)
 * - Today's reading tracker by book
 * - Book-specific velocity and progress milestones
 */
class ReadingExperienceTest {

    @TempDir
    Path tempDir;

    private BookService bookService;
    private ReadingTrackerService readingTrackerService;
    private SqliteReadingSessionDao sessionDao;
    private Book cleanCodeBook;
    private Book pragmaticProgrammerBook;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("reading_exp_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        SqliteBookDao bookDao = new SqliteBookDao(dbManager);
        sessionDao = new SqliteReadingSessionDao(dbManager);
        SqliteSettingsDao settingsDao = new SqliteSettingsDao(dbManager);

        readingTrackerService = new ReadingTrackerService(sessionDao, bookDao, settingsDao);
        bookService = new BookService(bookDao, new ChapterService(), readingTrackerService);

        cleanCodeBook = bookService.addBook(new Book("Clean Code", "Robert C. Martin", 464, 1));
        pragmaticProgrammerBook = bookService.addBook(new Book("The Pragmatic Programmer", "Andrew Hunt", 352, 1));
    }

    @Test
    @DisplayName("ReadingSession PPM speed calculation and formatting")
    void testReadingSessionPpmSpeed() {
        ReadingSession session = new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 0, 30, 30, 20, "Fast reading");
        assertEquals(1.5, session.getReadingSpeedPagesPerMinute(), 0.001);
        assertEquals("1.5", session.getFormattedSpeedPpm());

        ReadingSession zeroSession = new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 0, 0, 0, 0, null);
        assertEquals(0.0, zeroSession.getReadingSpeedPagesPerMinute());
        assertEquals("—", zeroSession.getFormattedSpeedPpm());
    }

    @Test
    @DisplayName("Today's pages tracking specific to each book")
    void testTodayPagesTrackingByBook() {
        // Book A: 2 sessions today (15 pages + 8 pages = 23 pages)
        readingTrackerService.logSession(new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 0, 15, 15, 15, "Session 1"));
        readingTrackerService.logSession(new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 15, 23, 8, 10, "Session 2"));

        // Book B: 1 session today (30 pages)
        readingTrackerService.logSession(new ReadingSession(pragmaticProgrammerBook.getId(), LocalDate.now(), 0, 30, 30, 25, "Book B session"));

        // Book A: 1 session yesterday (50 pages) - should NOT count for today
        readingTrackerService.logSession(new ReadingSession(cleanCodeBook.getId(), LocalDate.now().minusDays(1), 0, 50, 50, 45, "Yesterday session"));

        assertEquals(23, readingTrackerService.getPagesReadTodayForBook(cleanCodeBook.getId()));
        assertEquals(30, readingTrackerService.getPagesReadTodayForBook(pragmaticProgrammerBook.getId()));
    }

    @Test
    @DisplayName("Total reading time and total pages read per book")
    void testTotalReadingTimeAndPagesPerBook() {
        readingTrackerService.logSession(new ReadingSession(cleanCodeBook.getId(), LocalDate.now().minusDays(2), 0, 40, 40, 45, "Day 1"));
        readingTrackerService.logSession(new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 40, 75, 35, 35, "Day 2"));

        assertEquals(80, readingTrackerService.getTotalReadingTimeMinutesForBook(cleanCodeBook.getId()));
        assertEquals(75, readingTrackerService.getTotalPagesReadForBook(cleanCodeBook.getId()));
    }

    @Test
    @DisplayName("Effective reading speed (PPM) based on book history")
    void testEffectiveReadingSpeedPpm() {
        // 60 pages in 60 minutes = 1.0 page/minute (60 pages/hour)
        readingTrackerService.logSession(new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 0, 60, 60, 60, "1 hr reading"));

        double speed = readingTrackerService.getEffectiveReadingSpeedPpm(cleanCodeBook);
        assertEquals(1.0, speed, 0.01);

        // A book with no history falls back to overall user average or baseline
        double fallbackSpeed = readingTrackerService.getEffectiveReadingSpeedPpm(pragmaticProgrammerBook);
        assertTrue(fallbackSpeed > 0.0);
    }

    @Test
    @DisplayName("Estimated Time to Finish (ETA) calculation")
    void testEstimatedMinutesRemaining() {
        // cleanCodeBook: 464 total pages.
        // If current page is 364 -> 100 pages remaining
        cleanCodeBook.setCurrentPage(364);

        // At 1.0 PPM -> exactly 100 minutes
        int eta1 = readingTrackerService.getEstimatedMinutesRemaining(cleanCodeBook, 1.0);
        assertEquals(100, eta1);

        // At 2.0 PPM -> 50 minutes
        int eta2 = readingTrackerService.getEstimatedMinutesRemaining(cleanCodeBook, 2.0);
        assertEquals(50, eta2);

        // When book is completed (464 / 464) -> 0 minutes
        cleanCodeBook.setCurrentPage(464);
        int etaCompleted = readingTrackerService.getEstimatedMinutesRemaining(cleanCodeBook, 1.0);
        assertEquals(0, etaCompleted);
    }

    @Test
    @DisplayName("Format estimated remaining duration string")
    void testFormatEstimatedRemainingTime() {
        assertEquals("0m", readingTrackerService.formatEstimatedRemainingTime(0));
        assertEquals("45m", readingTrackerService.formatEstimatedRemainingTime(45));
        assertEquals("1h 35m", readingTrackerService.formatEstimatedRemainingTime(95));
        assertEquals("2h", readingTrackerService.formatEstimatedRemainingTime(120));
        assertEquals("3h 15m", readingTrackerService.formatEstimatedRemainingTime(195));
    }

    @Test
    @DisplayName("Session logging auto-completes book when total pages are reached")
    void testSessionLoggingAutoCompletesBook() {
        assertEquals(ReadingStatus.NOT_STARTED, cleanCodeBook.getStatus());

        // Log session reaching page 464 of 464
        ReadingSession finishSession = new ReadingSession(cleanCodeBook.getId(), LocalDate.now(), 0, 464, 464, 300, "Finished the book!");
        readingTrackerService.logSession(finishSession);

        Book reloaded = bookService.getBookById(cleanCodeBook.getId()).orElseThrow();
        assertEquals(ReadingStatus.COMPLETED, reloaded.getStatus());
        assertEquals(464, reloaded.getCurrentPage());
        assertNotNull(reloaded.getDateCompleted());
    }
}
