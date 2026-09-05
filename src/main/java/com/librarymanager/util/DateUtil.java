package com.librarymanager.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions for friendly, localized date formatting.
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
}
