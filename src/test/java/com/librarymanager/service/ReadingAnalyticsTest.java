package com.librarymanager.service;

import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.dao.SqliteReadingSessionDao;
import com.librarymanager.dao.SqliteSettingsDao;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.*;
import com.librarymanager.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReadingAnalyticsTest {

    @TempDir
    Path tempDir;

    private SqliteBookDao bookDao;
    private SqliteReadingSessionDao sessionDao;
    private SqliteSettingsDao settingsDao;
    private ReadingTrackerService readingTrackerService;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("analytics_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        bookDao = new SqliteBookDao(dbManager);
        sessionDao = new SqliteReadingSessionDao(dbManager);
        settingsDao = new SqliteSettingsDao(dbManager);

        readingTrackerService = new ReadingTrackerService(sessionDao, bookDao, settingsDao);
        bookService = new BookService(bookDao, new ChapterService(), readingTrackerService);
    }

    @Test
    @DisplayName("Test Reading Time and Average Reading Speed Calculation")
    void testReadingTimeAndSpeed() {
        Book b1 = new Book("Clean Architecture", "Robert C. Martin", 400, 1);
        Book b2 = new Book("Refactoring", "Martin Fowler", 450, 1);
        b1 = bookService.addBook(b1);
        b2 = bookService.addBook(b2);

        // Session 1: 60 pages in 60 mins -> 60 pages/hour
        ReadingSession s1 = new ReadingSession(b1.getId(), LocalDate.of(2026, 3, 10), 1, 60, 60, 60, "Arch intro");
        readingTrackerService.logSession(s1);

        // Session 2: 40 pages in 40 mins -> 60 pages/hour
        ReadingSession s2 = new ReadingSession(b2.getId(), LocalDate.of(2026, 3, 15), 1, 40, 40, 40, "Refactoring basics");
        readingTrackerService.logSession(s2);

        assertEquals(100, readingTrackerService.getTotalReadingTimeMinutes());
        assertEquals(100, readingTrackerService.getReadingTimeInYear(2026));

        // Speed: 100 pages / 100 minutes = 1 page/min = 60.0 pages/hour
        assertEquals(60.0, readingTrackerService.getAverageReadingSpeedPagesPerHour(), 0.01);
        assertEquals(60.0, readingTrackerService.getAverageReadingSpeedPagesPerHourInYear(2026), 0.01);
    }

    @Test
    @DisplayName("Test Books and Pages Read per Month Breakdown")
    void testMonthlyReadingBreakdown() {
        Book b1 = new Book("Java Concurrency", "Brian Goetz", 380, 1);
        b1 = bookService.addBook(b1);

        // Session in February: 80 pages, 90 mins
        ReadingSession s1 = new ReadingSession(b1.getId(), LocalDate.of(2026, 2, 10), 1, 80, 80, 90, "Concurrency models");
        readingTrackerService.logSession(s1);

        // Session in March: 120 pages, 120 mins
        ReadingSession s2 = new ReadingSession(b1.getId(), LocalDate.of(2026, 3, 15), 81, 200, 120, 120, "Locks and sync");
        readingTrackerService.logSession(s2);

        // Complete a book in March
        Book b2 = new Book("Effective Java", "Joshua Bloch", 412, 1);
        b2.setStatus(ReadingStatus.COMPLETED);
        b2.setCurrentPage(412);
        b2.setDateCompleted(LocalDate.of(2026, 3, 20));
        bookService.addBook(b2);

        List<MonthlyReadingStat> monthlyStats = readingTrackerService.getMonthlyReadingStatsInYear(2026);
        assertEquals(12, monthlyStats.size());

        // Month 1 (January): 0 pages, 0 books
        assertEquals(0, monthlyStats.get(0).getPagesRead());
        assertEquals(0, monthlyStats.get(0).getBooksCompleted());

        // Month 2 (February): 80 pages, 0 completed books, 90 duration
        assertEquals(80, monthlyStats.get(1).getPagesRead());
        assertEquals(0, monthlyStats.get(1).getBooksCompleted());
        assertEquals(90, monthlyStats.get(1).getDurationMinutes());

        // Month 3 (March): 120 pages, 1 completed book, 120 duration
        assertEquals(120, monthlyStats.get(2).getPagesRead());
        assertEquals(1, monthlyStats.get(2).getBooksCompleted());
        assertEquals(120, monthlyStats.get(2).getDurationMinutes());
    }

    @Test
    @DisplayName("Test Top Authors and Top Categories Aggregations")
    void testTopAuthorsAndCategories() {
        Book b1 = new Book("Clean Code", "Robert C. Martin", 464, 1);
        b1.setCategory("Software Engineering");
        b1.setStatus(ReadingStatus.COMPLETED);
        b1.setCurrentPage(464);
        b1.setDateCompleted(LocalDate.of(2026, 1, 15));
        bookService.addBook(b1);

        Book b2 = new Book("Clean Architecture", "Robert C. Martin", 400, 1);
        b2.setCategory("Software Engineering");
        b2.setStatus(ReadingStatus.COMPLETED);
        b2.setCurrentPage(400);
        b2.setDateCompleted(LocalDate.of(2026, 2, 15));
        bookService.addBook(b2);

        Book b3 = new Book("Effective Java", "Joshua Bloch", 412, 1);
        b3.setCategory("Programming");
        b3.setStatus(ReadingStatus.READING);
        b3.setCurrentPage(200);
        bookService.addBook(b3);

        List<AuthorStat> topAuthors = readingTrackerService.getTopAuthors(5);
        assertFalse(topAuthors.isEmpty());
        AuthorStat a1 = topAuthors.get(0);
        assertEquals("Robert C. Martin", a1.getAuthor());
        assertEquals(2, a1.getCompletedCount());
        assertEquals(864, a1.getPagesRead());

        List<CategoryStat> topCats = readingTrackerService.getTopCategories(5);
        assertFalse(topCats.isEmpty());
        CategoryStat cat1 = topCats.get(0);
        assertEquals("Software Engineering", cat1.getCategory());
        assertEquals(2, cat1.getCompletedCount());
        assertEquals(2, cat1.getTotalBooksCount());
    }

    @Test
    @DisplayName("Test Yearly Reading Summary Calculation and Goals")
    void testYearlyReadingSummary() {
        Book b1 = new Book("Domain-Driven Design", "Eric Evans", 560, 1);
        b1.setCategory("Architecture");
        b1.setStatus(ReadingStatus.COMPLETED);
        b1.setCurrentPage(560);
        b1.setDateCompleted(LocalDate.of(2026, 4, 10));
        bookService.addBook(b1);

        ReadingSession s = new ReadingSession(b1.getId(), LocalDate.of(2026, 4, 10), 1, 560, 560, 560, "Deep dive");
        readingTrackerService.logSession(s);

        settingsDao.setInt(ReadingTrackerService.KEY_GOAL_YEARLY_BOOKS, 5);

        YearlyReadingSummary summary = readingTrackerService.getYearlyReadingSummary(2026);
        assertEquals(2026, summary.getYear());
        assertEquals(1, summary.getBooksCompleted());
        assertEquals(560, summary.getPagesRead());
        assertEquals(560, summary.getReadingTimeMinutes());
        assertEquals(60.0, summary.getReadingSpeedPagesPerHour(), 0.01);
        assertEquals("Eric Evans", summary.getTopAuthor());
        assertEquals("Architecture", summary.getTopCategory());
        assertEquals(5, summary.getYearlyGoal());
        assertEquals(0.2, summary.getGoalProgressRatio(), 0.01);
        assertFalse(summary.isGoalAchieved());
    }

    @Test
    @DisplayName("Test Duration and Speed Formatting")
    void testDurationAndSpeedFormatting() {
        assertEquals("—", DateUtil.formatDuration(0));
        assertEquals("—", DateUtil.formatDuration(-10));
        assertEquals("—", DateUtil.formatReadingSpeed(0.0));
    }
}
