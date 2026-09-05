package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.ReadingSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of ReadingSessionDao with support for v1.3 reading analytics.
 */
public class SqliteReadingSessionDao implements ReadingSessionDao {
    private static final Logger LOGGER = Logger.getLogger(SqliteReadingSessionDao.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DatabaseManager databaseManager;

    public SqliteReadingSessionDao() {
        this(DatabaseManager.getInstance());
    }

    public SqliteReadingSessionDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public ReadingSession save(ReadingSession session) {
        String sql = """
            INSERT INTO reading_sessions (book_id, session_date, start_page, end_page, pages_read, duration_minutes, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, session.getBookId());
            stmt.setString(2, session.getSessionDate() != null ? session.getSessionDate().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
            stmt.setInt(3, session.getStartPage());
            stmt.setInt(4, session.getEndPage());
            stmt.setInt(5, session.getPagesRead());
            stmt.setInt(6, session.getDurationMinutes());
            stmt.setString(7, session.getNotes());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        session.setId(rs.getLong(1));
                    }
                }
            }
            return session;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert reading session for book: " + session.getBookId(), e);
            throw new RuntimeException("Database error saving reading session", e);
        }
    }

    @Override
    public void update(ReadingSession session) {
        if (session.getId() == null) {
            throw new IllegalArgumentException("Cannot update reading session without ID");
        }

        String sql = """
            UPDATE reading_sessions
            SET book_id = ?, session_date = ?, start_page = ?, end_page = ?, pages_read = ?, duration_minutes = ?, notes = ?
            WHERE id = ?;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, session.getBookId());
            stmt.setString(2, session.getSessionDate() != null ? session.getSessionDate().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
            stmt.setInt(3, session.getStartPage());
            stmt.setInt(4, session.getEndPage());
            stmt.setInt(5, session.getPagesRead());
            stmt.setInt(6, session.getDurationMinutes());
            stmt.setString(7, session.getNotes());
            stmt.setLong(8, session.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update reading session: " + session.getId(), e);
            throw new RuntimeException("Database error updating reading session", e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM reading_sessions WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete reading session: " + id, e);
            throw new RuntimeException("Database error deleting reading session", e);
        }
    }

    @Override
    public Optional<ReadingSession> findById(long id) {
        String sql = """
            SELECT s.*, b.title AS book_title
            FROM reading_sessions s
            LEFT JOIN books b ON s.book_id = b.id
            WHERE s.id = ?;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find reading session by id: " + id, e);
            throw new RuntimeException("Database error finding reading session", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ReadingSession> findByBookId(long bookId) {
        String sql = """
            SELECT s.*, b.title AS book_title
            FROM reading_sessions s
            LEFT JOIN books b ON s.book_id = b.id
            WHERE s.book_id = ?
            ORDER BY s.session_date DESC, s.id DESC;
            """;
        List<ReadingSession> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find reading sessions for book: " + bookId, e);
            throw new RuntimeException("Database error loading reading sessions", e);
        }
        return list;
    }

    @Override
    public List<ReadingSession> findRecent(int limit) {
        String sql = """
            SELECT s.*, b.title AS book_title
            FROM reading_sessions s
            LEFT JOIN books b ON s.book_id = b.id
            ORDER BY s.session_date DESC, s.id DESC
            LIMIT ?;
            """;
        List<ReadingSession> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find recent reading sessions", e);
            throw new RuntimeException("Database error loading recent reading sessions", e);
        }
        return list;
    }

    @Override
    public List<ReadingSession> findAll() {
        String sql = """
            SELECT s.*, b.title AS book_title
            FROM reading_sessions s
            LEFT JOIN books b ON s.book_id = b.id
            ORDER BY s.session_date DESC, s.id DESC;
            """;
        List<ReadingSession> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToSession(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find all reading sessions", e);
            throw new RuntimeException("Database error loading all reading sessions", e);
        }
        return list;
    }

    @Override
    public int getTotalPagesRead() {
        String sql = "SELECT COALESCE(SUM(pages_read), 0) FROM reading_sessions;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate total pages read", e);
        }
        return 0;
    }

    @Override
    public int getPagesReadOnDate(LocalDate date) {
        if (date == null) return 0;
        String sql = "SELECT COALESCE(SUM(pages_read), 0) FROM reading_sessions WHERE session_date = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, date.format(DATE_FORMATTER));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get pages read on date: " + date, e);
        }
        return 0;
    }

    @Override
    public List<LocalDate> getDistinctSessionDates() {
        String sql = "SELECT DISTINCT session_date FROM reading_sessions WHERE session_date IS NOT NULL ORDER BY session_date DESC;";
        List<LocalDate> dates = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String d = rs.getString(1);
                if (d != null && !d.isEmpty()) {
                    try {
                        dates.add(LocalDate.parse(d, DATE_FORMATTER));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch distinct session dates", e);
        }
        return dates;
    }

    @Override
    public int countSessions() {
        String sql = "SELECT count(*) FROM reading_sessions;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count reading sessions", e);
        }
        return 0;
    }

    // =========================================================================
    // v1.3 Statistics & Analytics Implementation
    // =========================================================================

    @Override
    public int getTotalReadingTimeMinutes() {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM reading_sessions;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate total reading time", e);
        }
        return 0;
    }

    @Override
    public int getReadingTimeInYear(int year) {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM reading_sessions WHERE substr(session_date, 1, 4) = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate reading time for year: " + year, e);
        }
        return 0;
    }

    @Override
    public int getPagesReadInYear(int year) {
        String sql = "SELECT COALESCE(SUM(pages_read), 0) FROM reading_sessions WHERE substr(session_date, 1, 4) = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate pages read for year: " + year, e);
        }
        return 0;
    }

    @Override
    public Map<Integer, Integer> getPagesReadByMonthInYear(int year) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            result.put(m, 0);
        }

        String sql = """
            SELECT CAST(substr(session_date, 6, 2) AS INTEGER) AS m, COALESCE(SUM(pages_read), 0) AS total_pages
            FROM reading_sessions
            WHERE substr(session_date, 1, 4) = ?
            GROUP BY m;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int m = rs.getInt("m");
                    int pages = rs.getInt("total_pages");
                    if (m >= 1 && m <= 12) {
                        result.put(m, pages);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get monthly pages read for year: " + year, e);
        }
        return result;
    }

    @Override
    public Map<Integer, Integer> getReadingTimeByMonthInYear(int year) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            result.put(m, 0);
        }

        String sql = """
            SELECT CAST(substr(session_date, 6, 2) AS INTEGER) AS m, COALESCE(SUM(duration_minutes), 0) AS total_time
            FROM reading_sessions
            WHERE substr(session_date, 1, 4) = ?
            GROUP BY m;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int m = rs.getInt("m");
                    int duration = rs.getInt("total_time");
                    if (m >= 1 && m <= 12) {
                        result.put(m, duration);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get monthly reading time for year: " + year, e);
        }
        return result;
    }

    @Override
    public double getAverageReadingSpeedPagesPerHour() {
        String sql = """
            SELECT COALESCE(SUM(pages_read), 0) AS total_pages, COALESCE(SUM(duration_minutes), 0) AS total_minutes
            FROM reading_sessions
            WHERE duration_minutes > 0 AND pages_read > 0;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int totalPages = rs.getInt("total_pages");
                int totalMinutes = rs.getInt("total_minutes");
                if (totalMinutes > 0 && totalPages > 0) {
                    return (totalPages * 60.0) / totalMinutes;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate average reading speed", e);
        }
        return 0.0;
    }

    @Override
    public double getAverageReadingSpeedPagesPerHourInYear(int year) {
        String sql = """
            SELECT COALESCE(SUM(pages_read), 0) AS total_pages, COALESCE(SUM(duration_minutes), 0) AS total_minutes
            FROM reading_sessions
            WHERE duration_minutes > 0 AND pages_read > 0 AND substr(session_date, 1, 4) = ?;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalPages = rs.getInt("total_pages");
                    int totalMinutes = rs.getInt("total_minutes");
                    if (totalMinutes > 0 && totalPages > 0) {
                        return (totalPages * 60.0) / totalMinutes;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate average reading speed for year: " + year, e);
        }
        return 0.0;
    }

    @Override
    public List<Integer> getDistinctYears() {
        String sql = """
            SELECT DISTINCT substr(session_date, 1, 4) AS yr
            FROM reading_sessions
            WHERE session_date IS NOT NULL AND length(session_date) >= 4
            ORDER BY yr DESC;
            """;

        List<Integer> years = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String yr = rs.getString("yr");
                if (yr != null && !yr.trim().isEmpty()) {
                    try {
                        years.add(Integer.parseInt(yr.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch distinct session years", e);
        }
        return years;
    }

    private ReadingSession mapResultSetToSession(ResultSet rs) throws SQLException {
        ReadingSession session = new ReadingSession();
        session.setId(rs.getLong("id"));
        session.setBookId(rs.getLong("book_id"));
        session.setStartPage(rs.getInt("start_page"));
        session.setEndPage(rs.getInt("end_page"));
        session.setPagesRead(rs.getInt("pages_read"));
        session.setDurationMinutes(rs.getInt("duration_minutes"));
        session.setNotes(rs.getString("notes"));

        try {
            session.setBookTitle(rs.getString("book_title"));
        } catch (SQLException ignored) {
        }

        String dateStr = rs.getString("session_date");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                session.setSessionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
            } catch (Exception e) {
                session.setSessionDate(LocalDate.now());
            }
        }
        return session;
    }
}
