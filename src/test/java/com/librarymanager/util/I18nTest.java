package com.librarymanager.util;

import com.librarymanager.model.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @BeforeEach
    void setUp() {
        I18n.setLanguage("en");
    }

    @Test
    @DisplayName("Test English messages retrieval and interpolation")
    void testEnglishMessages() {
        I18n.setLanguage("en");
        assertEquals("Dashboard", I18n.get("nav.dashboard"));
        assertEquals("Settings", I18n.get("nav.settings"));
        assertEquals("Not Started", ReadingStatus.NOT_STARTED.getDisplayName());
        assertEquals("Reading", ReadingStatus.READING.getDisplayName());
        assertEquals("Completed", ReadingStatus.COMPLETED.getDisplayName());
        assertFalse(I18n.isRTL());

        String formatted = I18n.get("book.card.pages", 350);
        assertEquals("350 pages", formatted);
    }

    @Test
    @DisplayName("Test Arabic messages retrieval, RTL and interpolation")
    void testArabicMessages() {
        I18n.setLanguage("ar");
        assertEquals("لوحة التحكم", I18n.get("nav.dashboard"));
        assertEquals("الإعدادات", I18n.get("nav.settings"));
        assertEquals("لم يبدأ", ReadingStatus.NOT_STARTED.getDisplayName());
        assertEquals("قيد القراءة", ReadingStatus.READING.getDisplayName());
        assertEquals("مكتمل", ReadingStatus.COMPLETED.getDisplayName());
        assertTrue(I18n.isRTL());

        String formatted = I18n.get("book.card.pages", 350);
        assertEquals("350 صفحة", formatted);
    }

    @Test
    @DisplayName("Test fallback on non-existent key")
    void testFallback() {
        assertEquals("some.nonexistent.key", I18n.get("some.nonexistent.key"));
    }
}
