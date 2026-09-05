package com.librarymanager.dao;

import java.util.Optional;

/**
 * Data Access Object interface for application settings.
 */
public interface SettingsDao {
    void set(String key, String value);
    Optional<String> get(String key);
    String getOrDefault(String key, String defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
    void setBoolean(String key, boolean value);
}
