package com.librarymanager.service;

import com.librarymanager.component.InteractiveTourOverlay;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteractiveTourTest {

    @BeforeEach
    void setUp() {
        I18n.setLanguage("en");
    }

    @Test
    @DisplayName("Verify all 12 guided tour steps have complete localization in English")
    void testEnglishTourLocalization() {
        I18n.setLanguage("en");

        assertEquals("Guided Tour (Ctrl+T)", I18n.get("tour.btn.tooltip"));
        assertEquals("Guided Product Tour (Ctrl+T)", I18n.get("shortcuts.desc.guided_tour"));
        assertEquals("Skip Tour", I18n.get("tour.skip"));
        assertEquals("Previous", I18n.get("tour.prev"));
        assertEquals("Next", I18n.get("tour.next"));
        assertEquals("Get Started 🎉", I18n.get("tour.finish"));
        assertEquals("Step 1 of 12", I18n.get("tour.step_counter", 1, 12));
        assertTrue(I18n.get("tour.toast.completed").contains("Tour completed"));

        // All 12 categories
        String[] cats = {
                "tour.cat.welcome", "tour.cat.navigation", "tour.cat.dashboard",
                "tour.cat.library", "tour.cat.bulk", "tour.cat.dragdrop",
                "tour.cat.chapters", "tour.cat.experience", "tour.cat.speed_eta",
                "tour.cat.milestones", "tour.cat.datamgmt", "tour.cat.accessibility"
        };
        for (String catKey : cats) {
            String val = I18n.get(catKey);
            assertNotEquals(catKey, val, "Missing EN translation for category: " + catKey);
            assertFalse(val.isBlank());
        }

        // All 12 steps
        for (int i = 1; i <= 12; i++) {
            String titleKey = "tour.step" + i + ".title";
            String descKey = "tour.step" + i + ".desc";
            String title = I18n.get(titleKey);
            String desc = I18n.get(descKey);

            assertNotEquals(titleKey, title, "Missing EN translation for title: " + titleKey);
            assertNotEquals(descKey, desc, "Missing EN translation for desc: " + descKey);
            assertFalse(title.isBlank());
            assertFalse(desc.isBlank());
        }
    }

    @Test
    @DisplayName("Verify all 12 guided tour steps have complete localization in Arabic")
    void testArabicTourLocalization() {
        I18n.setLanguage("ar");

        assertEquals("جولة تعليمية تفاعلية شاملة (Ctrl+T)", I18n.get("tour.btn.tooltip"));
        assertEquals("جولة تعليمية تفاعلية شاملة (Ctrl+T)", I18n.get("shortcuts.desc.guided_tour"));
        assertEquals("تخطي الجولة", I18n.get("tour.skip"));
        assertEquals("السابق", I18n.get("tour.prev"));
        assertEquals("التالي", I18n.get("tour.next"));
        assertEquals("ابدأ الاستخدام 🎉", I18n.get("tour.finish"));
        assertEquals("خطوة 1 من 12", I18n.get("tour.step_counter", 1, 12));
        assertTrue(I18n.get("tour.toast.completed").contains("تهانينا"));

        // All 12 categories
        String[] cats = {
                "tour.cat.welcome", "tour.cat.navigation", "tour.cat.dashboard",
                "tour.cat.library", "tour.cat.bulk", "tour.cat.dragdrop",
                "tour.cat.chapters", "tour.cat.experience", "tour.cat.speed_eta",
                "tour.cat.milestones", "tour.cat.datamgmt", "tour.cat.accessibility"
        };
        for (String catKey : cats) {
            String val = I18n.get(catKey);
            assertNotEquals(catKey, val, "Missing AR translation for category: " + catKey);
            assertFalse(val.isBlank());
        }

        // All 12 steps
        for (int i = 1; i <= 12; i++) {
            String titleKey = "tour.step" + i + ".title";
            String descKey = "tour.step" + i + ".desc";
            String title = I18n.get(titleKey);
            String desc = I18n.get(descKey);

            assertNotEquals(titleKey, title, "Missing AR translation for title: " + titleKey);
            assertNotEquals(descKey, desc, "Missing AR translation for desc: " + descKey);
            assertFalse(title.isBlank());
            assertFalse(desc.isBlank());
        }
    }

    @Test
    @DisplayName("Verify TourStep record integrity and structure")
    void testTourStepRecord() {
        InteractiveTourOverlay.TourStep step = new InteractiveTourOverlay.TourStep(
                "step-test",
                IconUtil.IconType.SPARKLES,
                "tour.cat.welcome",
                "tour.step1.title",
                "tour.step1.desc",
                "Ctrl + T",
                () -> {}
        );

        assertEquals("step-test", step.id());
        assertEquals(IconUtil.IconType.SPARKLES, step.iconType());
        assertEquals("tour.cat.welcome", step.categoryKey());
        assertEquals("tour.step1.title", step.titleKey());
        assertEquals("tour.step1.desc", step.descKey());
        assertEquals("Ctrl + T", step.shortcutBadge());
        assertNotNull(step.action());
    }
}
