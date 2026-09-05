package com.librarymanager.model;

import com.librarymanager.util.I18n;

/**
 * Represents the reading status of a book, localized dynamically.
 */
public enum ReadingStatus {
    NOT_STARTED("status-not-started"),
    READING("status-reading"),
    COMPLETED("status-completed");

    private final String styleClass;

    ReadingStatus(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getDisplayName() {
        return I18n.get("status." + name().toLowerCase());
    }

    public String getStyleClass() {
        return styleClass;
    }

    public static ReadingStatus fromString(String text) {
        if (text == null) {
            return NOT_STARTED;
        }
        for (ReadingStatus status : ReadingStatus.values()) {
            if (status.name().equalsIgnoreCase(text) ||
                status.getDisplayName().equalsIgnoreCase(text) ||
                (status == NOT_STARTED && ("Not Started".equalsIgnoreCase(text) || "لم يبدأ".equalsIgnoreCase(text))) ||
                (status == READING && ("Reading".equalsIgnoreCase(text) || "قيد القراءة".equalsIgnoreCase(text))) ||
                (status == COMPLETED && ("Completed".equalsIgnoreCase(text) || "مكتمل".equalsIgnoreCase(text)))) {
                return status;
            }
        }
        return NOT_STARTED;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
