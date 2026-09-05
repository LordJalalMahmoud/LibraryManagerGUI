package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Chapter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of ChapterDao using PreparedStatements.
 */
public class SqliteChapterDao implements ChapterDao {
    private static final Logger LOGGER = Logger.getLogger(SqliteChapterDao.class.getName());

    private final DatabaseManager databaseManager;

    public SqliteChapterDao() {
        this(DatabaseManager.getInstance());
    }

    public SqliteChapterDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Chapter save(Chapter chapter) {
        String sql = """
            INSERT INTO chapters (book_id, chapter_number, title, start_page, end_page, is_completed, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, chapter.getBookId());
            stmt.setInt(2, chapter.getChapterNumber());
            stmt.setString(3, chapter.getTitle());
            stmt.setInt(4, chapter.getStartPage());
            stmt.setInt(5, chapter.getEndPage());
            stmt.setInt(6, chapter.isCompleted() ? 1 : 0);
            stmt.setString(7, chapter.getNotes());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        chapter.setId(rs.getLong(1));
                    }
                }
            }
            return chapter;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert chapter: " + chapter.getTitle(), e);
            throw new RuntimeException("Database error saving chapter", e);
        }
    }

    @Override
    public void update(Chapter chapter) {
        if (chapter.getId() == null) {
            throw new IllegalArgumentException("Cannot update chapter without ID");
        }

        String sql = """
            UPDATE chapters
            SET chapter_number = ?, title = ?, start_page = ?, end_page = ?, is_completed = ?, notes = ?
            WHERE id = ?;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, chapter.getChapterNumber());
            stmt.setString(2, chapter.getTitle());
            stmt.setInt(3, chapter.getStartPage());
            stmt.setInt(4, chapter.getEndPage());
            stmt.setInt(5, chapter.isCompleted() ? 1 : 0);
            stmt.setString(6, chapter.getNotes());
            stmt.setLong(7, chapter.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update chapter: " + chapter.getId(), e);
            throw new RuntimeException("Database error updating chapter", e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM chapters WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete chapter: " + id, e);
            throw new RuntimeException("Database error deleting chapter", e);
        }
    }

    @Override
    public Optional<Chapter> findById(long id) {
        String sql = "SELECT * FROM chapters WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToChapter(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find chapter by id: " + id, e);
            throw new RuntimeException("Database error finding chapter", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Chapter> findByBookId(long bookId) {
        String sql = "SELECT * FROM chapters WHERE book_id = ? ORDER BY chapter_number ASC, start_page ASC, id ASC;";
        List<Chapter> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChapter(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load chapters for book: " + bookId, e);
            throw new RuntimeException("Database error loading chapters", e);
        }
        return list;
    }

    @Override
    public List<Chapter> findAll() {
        String sql = "SELECT * FROM chapters ORDER BY book_id ASC, chapter_number ASC, id ASC;";
        List<Chapter> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToChapter(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load all chapters", e);
            throw new RuntimeException("Database error loading all chapters", e);
        }
        return list;
    }

    @Override
    public void toggleCompletion(long id, boolean isCompleted) {
        String sql = "UPDATE chapters SET is_completed = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, isCompleted ? 1 : 0);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to toggle chapter completion: " + id, e);
            throw new RuntimeException("Database error updating chapter completion", e);
        }
    }

    @Override
    public int countCompletedByBookId(long bookId) {
        String sql = "SELECT count(*) FROM chapters WHERE book_id = ? AND is_completed = 1;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count completed chapters", e);
        }
        return 0;
    }

    @Override
    public int countTotalByBookId(long bookId) {
        String sql = "SELECT count(*) FROM chapters WHERE book_id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count total chapters", e);
        }
        return 0;
    }

    private Chapter mapResultSetToChapter(ResultSet rs) throws SQLException {
        Chapter c = new Chapter();
        c.setId(rs.getLong("id"));
        c.setBookId(rs.getLong("book_id"));
        c.setChapterNumber(rs.getInt("chapter_number"));
        c.setTitle(rs.getString("title"));
        c.setStartPage(rs.getInt("start_page"));
        c.setEndPage(rs.getInt("end_page"));
        c.setCompleted(rs.getInt("is_completed") == 1);
        c.setNotes(rs.getString("notes"));
        return c;
    }
}
