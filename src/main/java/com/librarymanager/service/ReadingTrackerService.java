package com.librarymanager.service;

import com.librarymanager.dao.*;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingGoal;
import com.librarymanager.model.ReadingSession;
import com.librarymanager.model.ReadingStatus;

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
        List<Book> completedBooks = bookDao.findByStatus(ReadingStatus.COMPLETED);
        int count = 0;
        for (Book b : completedBooks) {
            if (b.getDateCompleted() != null && b.getDateCompleted().getYear() == year) {
                count++;
            }
        }
        return count;
    }
}
