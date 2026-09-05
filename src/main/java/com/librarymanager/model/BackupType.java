package com.librarymanager.model;

import com.librarymanager.util.I18n;

/**
 * Categorization for database backups and system snapshots.
 */
public enum BackupType {
    MANUAL("backup.type.manual", "badge-info"),
    AUTO("backup.type.auto", "badge-chip"),
    RESTORE_POINT("backup.type.restore_point", "badge-success"),
    PRE_IMPORT("backup.type.pre_import", "badge-warning");

    private final String i18nKey;
    private final String styleClass;

    BackupType(String i18nKey, String styleClass) {
        this.i18nKey = i18nKey;
        this.styleClass = styleClass;
    }

    public String getDisplayName() {
        return I18n.get(i18nKey);
    }

    public String getStyleClass() {
        return styleClass;
    }
}
