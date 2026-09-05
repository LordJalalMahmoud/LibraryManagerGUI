package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for persisting application settings.
 */
public class SqliteSettingsDao implements SettingsDao {
    private static final Logger LOGGER = Logger.getLogger(SqliteSettingsDao.class.getName());

    private final DatabaseManager databaseManager;

    public SqliteSettingsDao() {
        this(DatabaseManager.getInstance());
    }

    public SqliteSettingsDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void set(String key, String value) {
        String sql = """
            INSERT INTO app_settings (setting_key, setting_value)
            VALUES (?, ?)
            ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save setting: " + key, e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load setting: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        set(key, Boolean.toString(value));
    }
}
