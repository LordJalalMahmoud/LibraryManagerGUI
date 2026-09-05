package com.librarymanager.service;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.dao.SqliteSettingsDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("settings_test.db");
        DatabaseManager dbManager = new DatabaseManager(dbPath);
        SqliteSettingsDao dao = new SqliteSettingsDao(dbManager);
        settingsService = new SettingsService(dao);
    }

    @Test
    @DisplayName("Test default settings")
    void testDefaultSettings() {
        assertEquals(SettingsService.THEME_DARK, settingsService.getTheme());
        assertTrue(settingsService.isDarkMode());
        assertTrue(settingsService.isConfirmDeleteEnabled());
    }

    @Test
    @DisplayName("Test theme toggle and listener notification")
    void testThemeToggleAndListener() {
        AtomicReference<String> notifiedTheme = new AtomicReference<>();
        settingsService.addThemeChangeListener(notifiedTheme::set);

        settingsService.toggleTheme();
        assertEquals(SettingsService.THEME_LIGHT, settingsService.getTheme());
        assertFalse(settingsService.isDarkMode());
        assertEquals(SettingsService.THEME_LIGHT, notifiedTheme.get());

        settingsService.toggleTheme();
        assertEquals(SettingsService.THEME_DARK, settingsService.getTheme());
        assertTrue(settingsService.isDarkMode());
        assertEquals(SettingsService.THEME_DARK, notifiedTheme.get());
    }

    @Test
    @DisplayName("Test confirm delete toggle")
    void testConfirmDeleteToggle() {
        settingsService.setConfirmDelete(false);
        assertFalse(settingsService.isConfirmDeleteEnabled());

        settingsService.setConfirmDelete(true);
        assertTrue(settingsService.isConfirmDeleteEnabled());
    }
}
