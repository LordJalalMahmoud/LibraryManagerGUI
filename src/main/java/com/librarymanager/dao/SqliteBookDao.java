package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of BookDao using PreparedStatements.
 */
public class SqliteBookDao implements BookDao {
    private static final Logger LOGGER = Logger.getLogger(SqliteBookDao.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DatabaseManager databaseManager;

    public SqliteBookDao() {
        this(DatabaseManager.getInstance());
    }

    public SqliteBookDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Book save(Book book) {
        String sql = """
            INSERT INTO books (title, author, total_pages, total_parts, current_page, status, description, cover_image, date_added, date_started, date_completed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getTotalPages());
            stmt.setInt(4, book.getTotalParts());
            stmt.setInt(5, book.getCurrentPage());
            stmt.setString(6, book.getStatus().name());
            stmt.setString(7, book.getDescription());
            stmt.setString(8, book.getCoverImage());
            stmt.setString(9, book.getDateAdded() != null ? book.getDateAdded().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
            stmt.setString(10, book.getDateStarted() != null ? book.getDateStarted().format(DATE_FORMATTER) : null);
            stmt.setString(11, book.getDateCompleted() != null ? book.getDateCompleted().format(DATE_FORMATTER) : null);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setId(generatedKeys.getLong(1));
                    }
                }
            }
            return book;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert book: " + book.getTitle(), e);
            throw new RuntimeException("Database error saving book", e);
        }
    }

    @Override
    public void update(Book book) {
        if (book.getId() == null) {
            throw new IllegalArgumentException("Cannot update book without ID");
        }

        String sql = """
            UPDATE books
            SET title = ?, author = ?, total_pages = ?, total_parts = ?, current_page = ?, status = ?,
                description = ?, cover_image = ?, date_started = ?, date_completed = ?
            WHERE id = ?;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getTotalPages());
            stmt.setInt(4, book.getTotalParts());
            stmt.setInt(5, book.getCurrentPage());
            stmt.setString(6, book.getStatus().name());
            stmt.setString(7, book.getDescription());
            stmt.setString(8, book.getCoverImage());
            stmt.setString(9, book.getDateStarted() != null ? book.getDateStarted().format(DATE_FORMATTER) : null);
            stmt.setString(10, book.getDateCompleted() != null ? book.getDateCompleted().format(DATE_FORMATTER) : null);
            stmt.setLong(11, book.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update book id: " + book.getId(), e);
            throw new RuntimeException("Database error updating book", e);
        }
    }

    @Override
    public void delete(long id) {
        String deleteChapters = "DELETE FROM chapters WHERE book_id = ?;";
        String deleteBook = "DELETE FROM books WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt1 = conn.prepareStatement(deleteChapters)) {
                    stmt1.setLong(1, id);
                    stmt1.executeUpdate();
                }
                try (PreparedStatement stmt2 = conn.prepareStatement(deleteBook)) {
                    stmt2.setLong(1, id);
                    stmt2.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete book id: " + id, e);
            throw new RuntimeException("Database error deleting book", e);
        }
    }

    @Override
    public Optional<Book> findById(long id) {
        String sql = """
            SELECT b.*,
                (SELECT count(*) FROM chapters WHERE book_id = b.id) AS total_chapters,
                (SELECT count(*) FROM chapters WHERE book_id = b.id AND is_completed = 1) AS completed_chapters
            FROM books b WHERE b.id = ?;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find book by id: " + id, e);
            throw new RuntimeException("Database error finding book", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() {
        return search(null, null, "date_added", false);
    }

    @Override
    public List<Book> findByStatus(ReadingStatus status) {
        return search(null, status, "date_added", false);
    }

    @Override
    public List<Book> search(String query, ReadingStatus statusFilter, String sortBy, boolean ascending) {
        StringBuilder sql = new StringBuilder("""
            SELECT b.*,
                (SELECT count(*) FROM chapters WHERE book_id = b.id) AS total_chapters,
                (SELECT count(*) FROM chapters WHERE book_id = b.id AND is_completed = 1) AS completed_chapters
            FROM books b WHERE 1=1\s""");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ?) ");
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            params.add(wildcard);
            params.add(wildcard);
        }

        if (statusFilter != null) {
            sql.append("AND b.status = ? ");
            params.add(statusFilter.name());
        }

        // Safe order by column mapping
        String orderColumn;
        if ("title".equalsIgnoreCase(sortBy)) {
            orderColumn = "LOWER(b.title)";
        } else if ("author".equalsIgnoreCase(sortBy)) {
            orderColumn = "LOWER(b.author)";
        } else if ("progress".equalsIgnoreCase(sortBy)) {
            orderColumn = "(CAST(b.current_page AS REAL) / b.total_pages)";
        } else if ("total_pages".equalsIgnoreCase(sortBy)) {
            orderColumn = "b.total_pages";
        } else {
            orderColumn = "b.id";
        }

        sql.append("ORDER BY ").append(orderColumn).append(ascending ? " ASC" : " DESC");

        List<Book> books = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Search query failed", e);
            throw new RuntimeException("Database error executing search", e);
        }

        return books;
    }

    @Override
    public LibraryStats getStatistics() {
        LibraryStats stats = new LibraryStats();

        String countSql = """
            SELECT
                count(*) AS total_count,
                SUM(CASE WHEN status = 'READING' THEN 1 ELSE 0 END) AS reading_count,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
                SUM(CASE WHEN status = 'NOT_STARTED' THEN 1 ELSE 0 END) AS not_started_count,
                COALESCE(SUM(total_pages), 0) AS total_pages_sum,
                COALESCE(SUM(current_page), 0) AS pages_read_sum
            FROM books;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.setTotalBooks(rs.getInt("total_count"));
                stats.setReadingCount(rs.getInt("reading_count"));
                stats.setCompletedCount(rs.getInt("completed_count"));
                stats.setNotStartedCount(rs.getInt("not_started_count"));
                stats.setTotalPages(rs.getInt("total_pages_sum"));
                stats.setPagesRead(rs.getInt("pages_read_sum"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to compute library stats", e);
            throw new RuntimeException("Database error computing stats", e);
        }

        // Fetch recently added books (up to 5)
        String recentAddedSql = "SELECT * FROM books ORDER BY id DESC LIMIT 5;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(recentAddedSql);
             ResultSet rs = stmt.executeQuery()) {
            List<Book> recent = new ArrayList<>();
            while (rs.next()) {
                recent.add(mapResultSetToBook(rs));
            }
            stats.setRecentlyAdded(recent);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load recently added books", e);
        }

        // Fetch recently completed books (up to 5)
        String recentCompletedSql = "SELECT * FROM books WHERE status = 'COMPLETED' ORDER BY date_completed DESC, id DESC LIMIT 5;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(recentCompletedSql);
             ResultSet rs = stmt.executeQuery()) {
            List<Book> completed = new ArrayList<>();
            while (rs.next()) {
                completed.add(mapResultSetToBook(rs));
            }
            stats.setRecentlyCompleted(completed);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load recently completed books", e);
        }

        return stats;
    }

    @Override
    public void deleteAll() {
        try (Connection conn = databaseManager.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM chapters;");
                stmt.execute("DELETE FROM books;");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete all books", e);
            throw new RuntimeException("Database error resetting books", e);
        }
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setTotalPages(rs.getInt("total_pages"));
        book.setTotalParts(rs.getInt("total_parts"));
        book.setCurrentPage(rs.getInt("current_page"));
        book.setStatus(ReadingStatus.fromString(rs.getString("status")));
        book.setDescription(rs.getString("description"));
        book.setCoverImage(rs.getString("cover_image"));

        String dateAddedStr = rs.getString("date_added");
        if (dateAddedStr != null && !dateAddedStr.isEmpty()) {
            try {
                book.setDateAdded(LocalDate.parse(dateAddedStr, DATE_FORMATTER));
            } catch (Exception ignored) {
                book.setDateAdded(LocalDate.now());
            }
        }

        String dateStartedStr = rs.getString("date_started");
        if (dateStartedStr != null && !dateStartedStr.isEmpty()) {
            try {
                book.setDateStarted(LocalDate.parse(dateStartedStr, DATE_FORMATTER));
            } catch (Exception ignored) {
            }
        }

        String dateCompletedStr = rs.getString("date_completed");
        if (dateCompletedStr != null && !dateCompletedStr.isEmpty()) {
            try {
                book.setDateCompleted(LocalDate.parse(dateCompletedStr, DATE_FORMATTER));
            } catch (Exception ignored) {
            }
        }

        try {
            book.setTotalChaptersCount(rs.getInt("total_chapters"));
            book.setCompletedChaptersCount(rs.getInt("completed_chapters"));
        } catch (SQLException ignored) {
        }

        return book;
    }
}
