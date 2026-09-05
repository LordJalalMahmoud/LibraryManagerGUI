package com.librarymanager.service;

import com.librarymanager.dao.SettingsDao;
import com.librarymanager.dao.SqliteSettingsDao;
import com.librarymanager.util.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing user settings, theme preferences, and application language persistence.
 */
public class SettingsService {
    private static final Logger LOGGER = Logger.getLogger(SettingsService.class.getName());

    public static final String KEY_THEME = "theme";
    public static final String KEY_CONFIRM_DELETE = "confirm_delete";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";
    public static final String KEY_AUTO_BACKUP_FREQ = "auto_backup_freq";
    public static final String KEY_AUTO_BACKUP_RETENTION = "auto_backup_retention";
    public static final String KEY_AUTO_BACKUP_LAST_RUN = "auto_backup_last_run";
    public static final String KEY_BACKUP_CUSTOM_DIR = "backup_custom_dir";

    public static final String THEME_DARK = "DARK";
    public static final String THEME_LIGHT = "LIGHT";

    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_AR = "ar";

    private final SettingsDao settingsDao;
    private final List<Consumer<String>> themeChangeListeners = new ArrayList<>();
    private final List<Consumer<String>> languageChangeListeners = new ArrayList<>();

    public SettingsService() {
        this(new SqliteSettingsDao());
    }

    public SettingsService(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
        // Initialize I18n with saved language preference
        String savedLang = getLanguage();
        I18n.setLanguage(savedLang);
    }

    public String getTheme() {
        return settingsDao.getOrDefault(KEY_THEME, THEME_DARK);
    }

    public boolean isDarkMode() {
        return THEME_DARK.equalsIgnoreCase(getTheme());
    }

    public void setTheme(String theme) {
        String normalized = THEME_LIGHT.equalsIgnoreCase(theme) ? THEME_LIGHT : THEME_DARK;
        settingsDao.set(KEY_THEME, normalized);
        notifyThemeChanged(normalized);
    }

    public void toggleTheme() {
        setTheme(isDarkMode() ? THEME_LIGHT : THEME_DARK);
    }

    public String getLanguage() {
        return settingsDao.getOrDefault(KEY_LANGUAGE, LANGUAGE_EN);
    }

    public boolean isArabic() {
        return LANGUAGE_AR.equalsIgnoreCase(getLanguage());
    }

    public void setLanguage(String language) {
        String normalized = LANGUAGE_AR.equalsIgnoreCase(language) ? LANGUAGE_AR : LANGUAGE_EN;
        settingsDao.set(KEY_LANGUAGE, normalized);
        I18n.setLanguage(normalized);
        notifyLanguageChanged(normalized);
    }

    public boolean isConfirmDeleteEnabled() {
        return settingsDao.getBoolean(KEY_CONFIRM_DELETE, true);
    }

    public void setConfirmDelete(boolean enabled) {
        settingsDao.setBoolean(KEY_CONFIRM_DELETE, enabled);
    }

    public boolean isAutoBackupEnabled() {
        return settingsDao.getBoolean(KEY_AUTO_BACKUP_ENABLED, true);
    }

    public void setAutoBackupEnabled(boolean enabled) {
        settingsDao.setBoolean(KEY_AUTO_BACKUP_ENABLED, enabled);
    }

    public String getAutoBackupFrequency() {
        return settingsDao.getOrDefault(KEY_AUTO_BACKUP_FREQ, "DAILY");
    }

    public void setAutoBackupFrequency(String freq) {
        settingsDao.set(KEY_AUTO_BACKUP_FREQ, freq);
    }

    public int getAutoBackupRetention() {
        return settingsDao.getInt(KEY_AUTO_BACKUP_RETENTION, 10);
    }

    public void setAutoBackupRetention(int count) {
        settingsDao.setInt(KEY_AUTO_BACKUP_RETENTION, count);
    }

    public String getAutoBackupLastRun() {
        return settingsDao.getOrDefault(KEY_AUTO_BACKUP_LAST_RUN, "");
    }

    public void setAutoBackupLastRun(String lastRun) {
        settingsDao.set(KEY_AUTO_BACKUP_LAST_RUN, lastRun);
    }

    public String getBackupCustomDirectory() {
        return settingsDao.getOrDefault(KEY_BACKUP_CUSTOM_DIR, "");
    }

    public void setBackupCustomDirectory(String dir) {
        settingsDao.set(KEY_BACKUP_CUSTOM_DIR, dir);
    }

    public void addThemeChangeListener(Consumer<String> listener) {
        if (listener != null) {
            themeChangeListeners.add(listener);
        }
    }

    public void addLanguageChangeListener(Consumer<String> listener) {
        if (listener != null) {
            languageChangeListeners.add(listener);
        }
    }

    private void notifyThemeChanged(String newTheme) {
        for (Consumer<String> listener : themeChangeListeners) {
            try {
                listener.accept(newTheme);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing theme change listener", e);
            }
        }
    }

    private void notifyLanguageChanged(String newLanguage) {
        for (Consumer<String> listener : languageChangeListeners) {
            try {
                listener.accept(newLanguage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing language change listener", e);
            }
        }
    }
}
