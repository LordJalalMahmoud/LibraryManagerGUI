package com.librarymanager.service;

import com.librarymanager.dao.*;
import com.librarymanager.model.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing reading sessions, reading streaks (current & best),
 * daily average pages, and customizable daily/yearly reading goals.
 */
public class ReadingTrackerService {
    private static final Logger LOGGER = Logger.getLogger(ReadingTrackerService.class.getName());

    public static final String KEY_GOAL_DAILY_PAGES = "reading_goal.daily_pages";
    public static final String KEY_GOAL_YEARLY_BOOKS = "reading_goal.yearly_books";

    public static final int DEFAULT_DAILY_PAGES_GOAL = 25;
    public static final int DEFAULT_YEARLY_BOOKS_GOAL = 12;

    private final ReadingSessionDao readingSessionDao;
    private final BookDao bookDao;
    private final SettingsDao settingsDao;

    public ReadingTrackerService() {
        this(new SqliteReadingSessionDao(), new SqliteBookDao(), new SqliteSettingsDao());
    }

    public ReadingTrackerService(ReadingSessionDao readingSessionDao, BookDao bookDao, SettingsDao settingsDao) {
        this.readingSessionDao = readingSessionDao;
        this.bookDao = bookDao;
        this.settingsDao = settingsDao;
    }

    public void validateSession(ReadingSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Reading session cannot be null");
        }
        if (session.getBookId() == null || session.getBookId() <= 0) {
            throw new IllegalArgumentException("Book ID is required for a reading session.");
        }
        if (session.getSessionDate() == null) {
            throw new IllegalArgumentException("Session date cannot be null.");
        }
        if (session.getStartPage() < 0 || session.getEndPage() < 0) {
            throw new IllegalArgumentException("Page numbers cannot be negative.");
        }
        if (session.getEndPage() > 0 && session.getStartPage() > session.getEndPage()) {
            throw new IllegalArgumentException("End page cannot be less than start page.");
        }
        if (session.getPagesRead() < 0) {
            throw new IllegalArgumentException("Pages read cannot be negative.");
        }
        if (session.getDurationMinutes() < 0) {
            throw new IllegalArgumentException("Duration cannot be negative.");
        }
    }

    /**
     * Records a new reading session and automatically updates book progress.
     */
    public ReadingSession logSession(ReadingSession session) {
        if (session.getPagesRead() <= 0 && session.getEndPage() >= session.getStartPage() && session.getEndPage() > 0) {
            session.setPagesRead(session.getEndPage() - session.getStartPage());
        }

        validateSession(session);
        ReadingSession saved = readingSessionDao.save(session);

        // Auto-advance book progress if end_page exceeds current_page
        Optional<Book> bookOpt = bookDao.findById(session.getBookId());
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            boolean updated = false;

            if (session.getEndPage() > book.getCurrentPage()) {
                book.setCurrentPage(Math.min(book.getTotalPages(), session.getEndPage()));
                updated = true;
            }

            if (book.getCurrentPage() >= book.getTotalPages() && book.getTotalPages() > 0) {
                book.setStatus(ReadingStatus.COMPLETED);
                if (book.getDateCompleted() == null) {
                    book.setDateCompleted(session.getSessionDate());
                }
                updated = true;
            } else if (book.getCurrentPage() > 0 && book.getStatus() == ReadingStatus.NOT_STARTED) {
                book.setStatus(ReadingStatus.READING);
                if (book.getDateStarted() == null) {
                    book.setDateStarted(session.getSessionDate());
                }
                updated = true;
            }

            if (updated) {
                bookDao.update(book);
            }
        }

        return saved;
    }

    public void updateSession(ReadingSession session) {
        validateSession(session);
        readingSessionDao.update(session);
    }

    public void deleteSession(long id) {
        readingSessionDao.delete(id);
    }

    public Optional<ReadingSession> getSessionById(long id) {
        return readingSessionDao.findById(id);
    }

    public List<ReadingSession> getSessionsByBookId(long bookId) {
        return readingSessionDao.findByBookId(bookId);
    }

    public List<ReadingSession> getRecentSessions(int limit) {
        return readingSessionDao.findRecent(limit);
    }

    public List<ReadingSession> getAllSessions() {
        return readingSessionDao.findAll();
    }

    public int getTotalPagesRead() {
        return readingSessionDao.getTotalPagesRead();
    }

    public int getPagesReadToday() {
        return readingSessionDao.getPagesReadOnDate(LocalDate.now());
    }

    public boolean hasReadToday() {
        return getPagesReadToday() > 0;
    }

    /**
     * Calculates the current continuous daily reading streak.
     * If user read today, streak counts consecutive days up to today.
     * If user hasn't read today yet, but read yesterday, the streak is alive from yesterday.
     * If user didn't read yesterday or today, the streak is 0.
     */
    public int calculateCurrentStreak() {
        List<LocalDate> dates = readingSessionDao.getDistinctSessionDates();
        if (dates.isEmpty()) {
            return 0;
        }

        Set<LocalDate> dateSet = new HashSet<>(dates);
        LocalDate today = LocalDate.now();
        LocalDate checkDate;

        if (dateSet.contains(today)) {
            checkDate = today;
        } else if (dateSet.contains(today.minusDays(1))) {
            checkDate = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        while (dateSet.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }
        return streak;
    }

    /**
     * Calculates the all-time best reading streak in days.
     */
    public int calculateBestStreak() {
        List<LocalDate> dates = readingSessionDao.getDistinctSessionDates();
        if (dates.isEmpty()) {
            return 0;
        }

        // Sort ascending
        List<LocalDate> sorted = new ArrayList<>(new TreeSet<>(dates));
        int bestStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < sorted.size(); i++) {
            LocalDate prev = sorted.get(i - 1);
            LocalDate curr = sorted.get(i);

            if (ChronoUnit.DAYS.between(prev, curr) == 1) {
                currentStreak++;
                bestStreak = Math.max(bestStreak, currentStreak);
            } else if (ChronoUnit.DAYS.between(prev, curr) > 1) {
                currentStreak = 1;
            }
        }
        return Math.max(bestStreak, calculateCurrentStreak());
    }

    /**
     * Calculates average pages read per active reading day.
     */
    public double calculateDailyAveragePages() {
        List<LocalDate> dates = readingSessionDao.getDistinctSessionDates();
        if (dates.isEmpty()) {
            return 0.0;
        }
        int totalPages = getTotalPagesRead();
        return (double) totalPages / (double) dates.size();
    }

    /**
     * Returns the user's reading goals and current progress.
     */
    public ReadingGoal getReadingGoal() {
        int dailyPages = settingsDao.getInt(KEY_GOAL_DAILY_PAGES, DEFAULT_DAILY_PAGES_GOAL);
        int yearlyBooks = settingsDao.getInt(KEY_GOAL_YEARLY_BOOKS, DEFAULT_YEARLY_BOOKS_GOAL);

        int pagesToday = getPagesReadToday();
        int completedThisYear = countBooksCompletedInYear(LocalDate.now().getYear());

        return new ReadingGoal(dailyPages, yearlyBooks, pagesToday, completedThisYear);
    }

    public void setGoals(int dailyPages, int yearlyBooks) {
        settingsDao.setInt(KEY_GOAL_DAILY_PAGES, Math.max(1, dailyPages));
        settingsDao.setInt(KEY_GOAL_YEARLY_BOOKS, Math.max(1, yearlyBooks));
    }

    public int countBooksCompletedInYear(int year) {
        return bookDao.countBooksCompletedInYear(year);
    }

    // =========================================================================
    // v1.3 Statistics & Analytics Services
    // =========================================================================

    public int getTotalReadingTimeMinutes() {
        return readingSessionDao.getTotalReadingTimeMinutes();
    }

    public int getReadingTimeInYear(int year) {
        return readingSessionDao.getReadingTimeInYear(year);
    }

    public int getPagesReadInYear(int year) {
        int sessionPages = readingSessionDao.getPagesReadInYear(year);
        if (sessionPages > 0) {
            return sessionPages;
        }
        // Fallback for books marked completed without dedicated session logs
        List<Book> completed = bookDao.findByStatus(ReadingStatus.COMPLETED);
        int completedPages = 0;
        for (Book b : completed) {
            if (b.getDateCompleted() != null && b.getDateCompleted().getYear() == year) {
                completedPages += b.getTotalPages();
            }
        }
        return Math.max(sessionPages, completedPages);
    }

    public double getAverageReadingSpeedPagesPerHour() {
        return readingSessionDao.getAverageReadingSpeedPagesPerHour();
    }

    public double getAverageReadingSpeedPagesPerHourInYear(int year) {
        return readingSessionDao.getAverageReadingSpeedPagesPerHourInYear(year);
    }

    public List<MonthlyReadingStat> getMonthlyReadingStatsInYear(int year) {
        Map<Integer, Integer> booksByMonth = bookDao.getBooksCompletedByMonthInYear(year);
        Map<Integer, Integer> pagesByMonth = readingSessionDao.getPagesReadByMonthInYear(year);
        Map<Integer, Integer> timeByMonth = readingSessionDao.getReadingTimeByMonthInYear(year);

        List<MonthlyReadingStat> stats = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int books = booksByMonth.getOrDefault(m, 0);
            int pages = pagesByMonth.getOrDefault(m, 0);
            int duration = timeByMonth.getOrDefault(m, 0);
            stats.add(new MonthlyReadingStat(m, books, pages, duration));
        }
        return stats;
    }

    public List<AuthorStat> getTopAuthors(int limit) {
        return bookDao.getTopAuthors(limit);
    }

    public List<CategoryStat> getTopCategories(int limit) {
        return bookDao.getTopCategories(limit);
    }

    public List<Integer> getAllDistinctYears() {
        Set<Integer> yearSet = new TreeSet<>(Comparator.reverseOrder());
        yearSet.add(LocalDate.now().getYear());
        yearSet.addAll(readingSessionDao.getDistinctYears());
        yearSet.addAll(bookDao.getDistinctCompletedYears());
        return new ArrayList<>(yearSet);
    }

    public YearlyReadingSummary getYearlyReadingSummary(int year) {
        int booksCompleted = countBooksCompletedInYear(year);
        int pagesRead = getPagesReadInYear(year);
        int readingTime = readingSessionDao.getReadingTimeInYear(year);
        double speed = readingSessionDao.getAverageReadingSpeedPagesPerHourInYear(year);

        AuthorStat topAuthorStat = bookDao.getTopAuthorInYear(year);
        String topAuthor = topAuthorStat != null ? topAuthorStat.getAuthor() : null;
        int topAuthorBooks = topAuthorStat != null ? topAuthorStat.getCompletedCount() : 0;

        CategoryStat topCatStat = bookDao.getTopCategoryInYear(year);
        String topCategory = topCatStat != null ? topCatStat.getCategory() : null;
        int topCatBooks = topCatStat != null ? topCatStat.getCompletedCount() : 0;

        int yearlyGoal = settingsDao.getInt(KEY_GOAL_YEARLY_BOOKS, DEFAULT_YEARLY_BOOKS_GOAL);

        return new YearlyReadingSummary(
                year,
                booksCompleted,
                pagesRead,
                readingTime,
                speed,
                topAuthor,
                topAuthorBooks,
                topCategory,
                topCatBooks,
                yearlyGoal
        );
    }
}
