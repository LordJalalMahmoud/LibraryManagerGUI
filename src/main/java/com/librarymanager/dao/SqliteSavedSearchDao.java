package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.model.SavedSearch;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of SavedSearchDao.
 */
public class SqliteSavedSearchDao implements SavedSearchDao {
    private static final Logger LOGGER = Logger.getLogger(SqliteSavedSearchDao.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DatabaseManager databaseManager;

    public SqliteSavedSearchDao() {
        this(DatabaseManager.getInstance());
    }

    public SqliteSavedSearchDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public SavedSearch save(SavedSearch search) {
        String sql = """
            INSERT INTO saved_searches (name, query, author, status, category, tag, is_favorite, is_wishlist, min_pages, max_pages, sort_by, ascending, date_created)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, search.getName());
            stmt.setString(2, search.getQuery());
            stmt.setString(3, search.getAuthor());
            stmt.setString(4, search.getStatus() != null ? search.getStatus().name() : null);
            stmt.setString(5, search.getCategory());
            stmt.setString(6, search.getTag());
            if (search.getFavorite() != null) stmt.setInt(7, search.getFavorite() ? 1 : 0);
            else stmt.setNull(7, Types.INTEGER);

            if (search.getWishlist() != null) stmt.setInt(8, search.getWishlist() ? 1 : 0);
            else stmt.setNull(8, Types.INTEGER);

            if (search.getMinPages() != null) stmt.setInt(9, search.getMinPages());
            else stmt.setNull(9, Types.INTEGER);

            if (search.getMaxPages() != null) stmt.setInt(10, search.getMaxPages());
            else stmt.setNull(10, Types.INTEGER);

            stmt.setString(11, search.getSortBy());
            stmt.setInt(12, search.isAscending() ? 1 : 0);
            stmt.setString(13, search.getDateCreated() != null ? search.getDateCreated().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        search.setId(rs.getLong(1));
                    }
                }
            }
            return search;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save saved search: " + search.getName(), e);
            throw new RuntimeException("Database error saving search", e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM saved_searches WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete saved search: " + id, e);
            throw new RuntimeException("Database error deleting search", e);
        }
    }

    @Override
    public List<SavedSearch> findAll() {
        String sql = "SELECT * FROM saved_searches ORDER BY id DESC;";
        List<SavedSearch> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToSearch(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load saved searches", e);
        }
        return list;
    }

    @Override
    public Optional<SavedSearch> findById(long id) {
        String sql = "SELECT * FROM saved_searches WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSearch(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find saved search by id: " + id, e);
        }
        return Optional.empty();
    }

    private SavedSearch mapResultSetToSearch(ResultSet rs) throws SQLException {
        SavedSearch s = new SavedSearch();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        s.setQuery(rs.getString("query"));
        s.setAuthor(rs.getString("author"));

        String statusStr = rs.getString("status");
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                s.setStatus(ReadingStatus.valueOf(statusStr));
            } catch (Exception ignored) {
            }
        }

        s.setCategory(rs.getString("category"));
        s.setTag(rs.getString("tag"));

        int fav = rs.getInt("is_favorite");
        if (!rs.wasNull()) s.setFavorite(fav == 1);

        int wish = rs.getInt("is_wishlist");
        if (!rs.wasNull()) s.setWishlist(wish == 1);

        int minP = rs.getInt("min_pages");
        if (!rs.wasNull()) s.setMinPages(minP);

        int maxP = rs.getInt("max_pages");
        if (!rs.wasNull()) s.setMaxPages(maxP);

        s.setSortBy(rs.getString("sort_by"));
        s.setAscending(rs.getInt("ascending") == 1);

        String dateStr = rs.getString("date_created");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                s.setDateCreated(LocalDate.parse(dateStr, DATE_FORMATTER));
            } catch (Exception ignored) {
            }
        }
        return s;
    }
}
