package com.librarymanager.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Utility functions for friendly, localized date and duration formatting.
 */
public class DateUtil {

    public static String format(LocalDate date) {
        if (date == null) {
            return "—";
        }
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(date, today);
        if (days == 0) {
            return I18n.get("date.today");
        } else if (days == 1) {
            return I18n.get("date.yesterday");
        } else if (days > 1 && days < 7) {
            return I18n.get("date.days_ago", days);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", I18n.getCurrentLocale());
        return date.format(formatter);
    }

    public static String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "—";
        }
        if (totalMinutes < 60) {
            return I18n.get("stat.time.mins_only", totalMinutes);
        }
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (mins == 0) {
            return I18n.get("stat.time.hours_only", hours);
        }
        return I18n.get("stat.time.hours_mins", hours, mins);
    }

    public static String formatReadingSpeed(double pagesPerHour) {
        if (pagesPerHour <= 0.0) {
            return "—";
        }
        return String.format(Locale.US, "%.0f", pagesPerHour) + " " + I18n.get("stat.speed.unit");
    }
}
