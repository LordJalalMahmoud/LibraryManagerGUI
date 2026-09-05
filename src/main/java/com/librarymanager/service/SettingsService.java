package com.librarymanager.service;

import com.librarymanager.dao.SettingsDao;
import com.librarymanager.dao.SqliteSettingsDao;
import com.librarymanager.util.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service managing user settings, theme preferences, and application language persistence.
 */
public class SettingsService {
    public static final String KEY_THEME = "theme";
    public static final String KEY_CONFIRM_DELETE = "confirm_delete";
    public static final String KEY_LANGUAGE = "language";

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
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyLanguageChanged(String newLanguage) {
        for (Consumer<String> listener : languageChangeListeners) {
            try {
                listener.accept(newLanguage);
            } catch (Exception ignored) {
            }
        }
    }
}
