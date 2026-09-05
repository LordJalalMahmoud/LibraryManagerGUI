package com.librarymanager.service;

import com.librarymanager.dao.SettingsDao;
import com.librarymanager.util.AnimationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UxEnhancementsTest {

    private InMemorySettingsDao settingsDao;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsDao = new InMemorySettingsDao();
        settingsService = new SettingsService(settingsDao);
    }

    @Test
    void testWindowGeometryPersistence() {
        // Defaults
        assertEquals(1180.0, settingsService.getWindowWidth());
        assertEquals(780.0, settingsService.getWindowHeight());
        assertEquals(-1.0, settingsService.getWindowPosX());
        assertEquals(-1.0, settingsService.getWindowPosY());
        assertFalse(settingsService.isWindowMaximized());

        // Updates
        settingsService.setWindowWidth(1440.0);
        settingsService.setWindowHeight(900.0);
        settingsService.setWindowPosX(120.0);
        settingsService.setWindowPosY(80.0);
        settingsService.setWindowMaximized(true);

        assertEquals(1440.0, settingsService.getWindowWidth());
        assertEquals(900.0, settingsService.getWindowHeight());
        assertEquals(120.0, settingsService.getWindowPosX());
        assertEquals(80.0, settingsService.getWindowPosY());
        assertTrue(settingsService.isWindowMaximized());
    }

    @Test
    void testDashboardCustomization() {
        // Default order
        List<String> defaultOrder = settingsService.getDashboardSectionOrder();
        assertEquals(SettingsService.DEFAULT_DASHBOARD_ORDER, defaultOrder);

        // All visible by default
        assertTrue(settingsService.isDashboardSectionVisible(SettingsService.SECTION_METRICS));
        assertTrue(settingsService.isDashboardSectionVisible(SettingsService.SECTION_YEARLY));

        // Toggle visibility
        settingsService.setDashboardSectionVisible(SettingsService.SECTION_YEARLY, false);
        assertFalse(settingsService.isDashboardSectionVisible(SettingsService.SECTION_YEARLY));
        assertTrue(settingsService.isDashboardSectionVisible(SettingsService.SECTION_METRICS));

        // Reorder sections
        List<String> customOrder = new ArrayList<>(defaultOrder);
        Collections.swap(customOrder, 0, 1); // Swap METRICS and YEARLY
        settingsService.setDashboardSectionOrder(customOrder);

        List<String> retrievedOrder = settingsService.getDashboardSectionOrder();
        assertEquals(customOrder.get(0), retrievedOrder.get(0));
        assertEquals(customOrder.get(1), retrievedOrder.get(1));

        // Reset layout
        settingsService.resetDashboardLayout();
        assertEquals(SettingsService.DEFAULT_DASHBOARD_ORDER, settingsService.getDashboardSectionOrder());
        assertTrue(settingsService.isDashboardSectionVisible(SettingsService.SECTION_YEARLY));
    }

    @Test
    void testAccessibilityAndHighContrast() {
        // Theme defaults to DARK
        assertEquals(SettingsService.THEME_DARK, settingsService.getTheme());
        assertFalse(settingsService.isHighContrast());

        // Set High Contrast
        settingsService.setHighContrast(true);
        assertTrue(settingsService.isHighContrast());
        assertEquals(SettingsService.THEME_HIGH_CONTRAST, settingsService.getTheme());

        // Toggle theme from High Contrast should switch to DARK
        settingsService.toggleTheme();
        assertEquals(SettingsService.THEME_DARK, settingsService.getTheme());

        // Font scaling
        assertEquals(SettingsService.FONT_SCALE_NORMAL, settingsService.getFontSizeScale());

        AtomicReference<String> notifiedScale = new AtomicReference<>();
        settingsService.addFontSizeChangeListener(notifiedScale::set);

        settingsService.setFontSizeScale(SettingsService.FONT_SCALE_LARGE);
        assertEquals(SettingsService.FONT_SCALE_LARGE, settingsService.getFontSizeScale());
        assertEquals(SettingsService.FONT_SCALE_LARGE, notifiedScale.get());

        settingsService.setFontSizeScale(SettingsService.FONT_SCALE_EXTRA_LARGE);
        assertEquals(SettingsService.FONT_SCALE_EXTRA_LARGE, settingsService.getFontSizeScale());

        settingsService.setFontSizeScale("INVALID_SCALE");
        assertEquals(SettingsService.FONT_SCALE_NORMAL, settingsService.getFontSizeScale());

        // Reduce motion
        assertFalse(settingsService.isReduceMotionEnabled());
        settingsService.setReduceMotion(true);
        assertTrue(settingsService.isReduceMotionEnabled());

        AnimationUtil.setReduceMotion(true);
        assertTrue(AnimationUtil.isReduceMotion());
        AnimationUtil.setReduceMotion(false);
        assertFalse(AnimationUtil.isReduceMotion());
    }

    private static class InMemorySettingsDao implements SettingsDao {
        private final Map<String, String> store = new HashMap<>();

        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public String getOrDefault(String key, String defaultValue) {
            return store.getOrDefault(key, defaultValue);
        }

        @Override
        public void set(String key, String value) {
            store.put(key, value);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            String val = store.get(key);
            return val != null ? Boolean.parseBoolean(val) : defaultValue;
        }

        @Override
        public void setBoolean(String key, boolean value) {
            store.put(key, String.valueOf(value));
        }

        @Override
        public int getInt(String key, int defaultValue) {
            String val = store.get(key);
            if (val == null) return defaultValue;
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public void setInt(String key, int value) {
            store.put(key, String.valueOf(value));
        }

        @Override
        public double getDouble(String key, double defaultValue) {
            String val = store.get(key);
            if (val == null) return defaultValue;
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public void setDouble(String key, double value) {
            store.put(key, String.valueOf(value));
        }
    }
}
