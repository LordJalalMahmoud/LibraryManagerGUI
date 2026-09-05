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

    // Window Geometry Persistence
    public static final String KEY_WINDOW_WIDTH = "window_width";
    public static final String KEY_WINDOW_HEIGHT = "window_height";
    public static final String KEY_WINDOW_POS_X = "window_pos_x";
    public static final String KEY_WINDOW_POS_Y = "window_pos_y";
    public static final String KEY_WINDOW_MAXIMIZED = "window_maximized";

    // Dashboard Customization
    public static final String KEY_DASHBOARD_SECTIONS_VISIBLE = "dashboard_sections_visible";
    public static final String KEY_DASHBOARD_SECTIONS_ORDER = "dashboard_sections_order";

    public static final String SECTION_METRICS = "METRICS";
    public static final String SECTION_YEARLY = "YEARLY_SUMMARY";
    public static final String SECTION_CHARTS = "MONTHLY_CHART";
    public static final String SECTION_GOALS = "GOALS_HABITS";
    public static final String SECTION_CURRENTLY_READING = "CURRENTLY_READING";
    public static final String SECTION_RECENT_SESSIONS = "RECENT_SESSIONS";
    public static final String SECTION_RECENT_BOOKS = "RECENT_BOOKS";

    public static final List<String> DEFAULT_DASHBOARD_ORDER = List.of(
            SECTION_METRICS,
            SECTION_YEARLY,
            SECTION_CHARTS,
            SECTION_GOALS,
            SECTION_CURRENTLY_READING,
            SECTION_RECENT_SESSIONS,
            SECTION_RECENT_BOOKS
    );

    // Accessibility
    public static final String KEY_FONT_SIZE_SCALE = "font_size_scale";
    public static final String KEY_REDUCE_MOTION = "reduce_motion";

    public static final String FONT_SCALE_NORMAL = "NORMAL";
    public static final String FONT_SCALE_LARGE = "LARGE";
    public static final String FONT_SCALE_EXTRA_LARGE = "EXTRA_LARGE";

    public static final String THEME_DARK = "DARK";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_HIGH_CONTRAST = "HIGH_CONTRAST";

    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_AR = "ar";

    private final SettingsDao settingsDao;
    private final List<Consumer<String>> themeChangeListeners = new ArrayList<>();
    private final List<Consumer<String>> languageChangeListeners = new ArrayList<>();
    private final List<Consumer<String>> fontSizeChangeListeners = new ArrayList<>();

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
        String normalized;
        if (THEME_HIGH_CONTRAST.equalsIgnoreCase(theme)) {
            normalized = THEME_HIGH_CONTRAST;
        } else if (THEME_LIGHT.equalsIgnoreCase(theme)) {
            normalized = THEME_LIGHT;
        } else {
            normalized = THEME_DARK;
        }
        settingsDao.set(KEY_THEME, normalized);
        notifyThemeChanged(normalized);
    }

    public void toggleTheme() {
        if (isHighContrast()) {
            setTheme(THEME_DARK);
        } else {
            setTheme(isDarkMode() ? THEME_LIGHT : THEME_DARK);
        }
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

    // ==========================================
    // Window Geometry Persistence
    // ==========================================

    public double getWindowWidth() {
        return settingsDao.getDouble(KEY_WINDOW_WIDTH, 1180.0);
    }

    public void setWindowWidth(double width) {
        settingsDao.setDouble(KEY_WINDOW_WIDTH, width);
    }

    public double getWindowHeight() {
        return settingsDao.getDouble(KEY_WINDOW_HEIGHT, 780.0);
    }

    public void setWindowHeight(double height) {
        settingsDao.setDouble(KEY_WINDOW_HEIGHT, height);
    }

    public double getWindowPosX() {
        return settingsDao.getDouble(KEY_WINDOW_POS_X, -1.0);
    }

    public void setWindowPosX(double x) {
        settingsDao.setDouble(KEY_WINDOW_POS_X, x);
    }

    public double getWindowPosY() {
        return settingsDao.getDouble(KEY_WINDOW_POS_Y, -1.0);
    }

    public void setWindowPosY(double y) {
        settingsDao.setDouble(KEY_WINDOW_POS_Y, y);
    }

    public boolean isWindowMaximized() {
        return settingsDao.getBoolean(KEY_WINDOW_MAXIMIZED, false);
    }

    public void setWindowMaximized(boolean maximized) {
        settingsDao.setBoolean(KEY_WINDOW_MAXIMIZED, maximized);
    }

    // ==========================================
    // Dashboard Customization
    // ==========================================

    public boolean isDashboardSectionVisible(String sectionId) {
        String visibleCsv = settingsDao.getOrDefault(KEY_DASHBOARD_SECTIONS_VISIBLE, "");
        if (visibleCsv.isBlank()) {
            return true; // All sections visible by default
        }
        String[] parts = visibleCsv.split(",");
        for (String p : parts) {
            String[] kv = p.split(":");
            if (kv.length == 2 && kv[0].equalsIgnoreCase(sectionId)) {
                return Boolean.parseBoolean(kv[1]);
            }
        }
        return true;
    }

    public void setDashboardSectionVisible(String sectionId, boolean visible) {
        List<String> order = getDashboardSectionOrder();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            String sec = order.get(i);
            boolean isVis = sec.equalsIgnoreCase(sectionId) ? visible : isDashboardSectionVisible(sec);
            if (i > 0) sb.append(",");
            sb.append(sec).append(":").append(isVis);
        }
        settingsDao.set(KEY_DASHBOARD_SECTIONS_VISIBLE, sb.toString());
    }

    public List<String> getDashboardSectionOrder() {
        String orderCsv = settingsDao.getOrDefault(KEY_DASHBOARD_SECTIONS_ORDER, "");
        if (orderCsv.isBlank()) {
            return new ArrayList<>(DEFAULT_DASHBOARD_ORDER);
        }
        String[] parts = orderCsv.split(",");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (DEFAULT_DASHBOARD_ORDER.contains(trimmed) && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        for (String def : DEFAULT_DASHBOARD_ORDER) {
            if (!result.contains(def)) {
                result.add(def);
            }
        }
        return result;
    }

    public void setDashboardSectionOrder(List<String> order) {
        if (order != null && !order.isEmpty()) {
            settingsDao.set(KEY_DASHBOARD_SECTIONS_ORDER, String.join(",", order));
        }
    }

    public void resetDashboardLayout() {
        settingsDao.set(KEY_DASHBOARD_SECTIONS_VISIBLE, "");
        settingsDao.set(KEY_DASHBOARD_SECTIONS_ORDER, "");
    }

    // ==========================================
    // Accessibility & Appearance
    // ==========================================

    public boolean isHighContrast() {
        return THEME_HIGH_CONTRAST.equalsIgnoreCase(getTheme());
    }

    public void setHighContrast(boolean enabled) {
        if (enabled) {
            setTheme(THEME_HIGH_CONTRAST);
        } else {
            setTheme(THEME_DARK);
        }
    }

    public String getFontSizeScale() {
        return settingsDao.getOrDefault(KEY_FONT_SIZE_SCALE, FONT_SCALE_NORMAL);
    }

    public void setFontSizeScale(String scale) {
        String normalized = switch (scale != null ? scale.toUpperCase() : "") {
            case FONT_SCALE_LARGE -> FONT_SCALE_LARGE;
            case FONT_SCALE_EXTRA_LARGE -> FONT_SCALE_EXTRA_LARGE;
            default -> FONT_SCALE_NORMAL;
        };
        settingsDao.set(KEY_FONT_SIZE_SCALE, normalized);
        notifyFontSizeChanged(normalized);
    }

    public boolean isReduceMotionEnabled() {
        return settingsDao.getBoolean(KEY_REDUCE_MOTION, false);
    }

    public void setReduceMotion(boolean reduce) {
        settingsDao.setBoolean(KEY_REDUCE_MOTION, reduce);
    }

    public void addFontSizeChangeListener(Consumer<String> listener) {
        if (listener != null) {
            fontSizeChangeListeners.add(listener);
        }
    }

    private void notifyFontSizeChanged(String newScale) {
        for (Consumer<String> listener : fontSizeChangeListeners) {
            try {
                listener.accept(newScale);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing font size change listener", e);
            }
        }
    }
}
