package com.librarymanager.service;

import com.librarymanager.database.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Service managing SQLite database backup, export, restore, and validation.
 */
public class BackupService {
    private final DatabaseManager databaseManager;

    public BackupService() {
        this(DatabaseManager.getInstance());
    }

    public BackupService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void exportBackup(File targetFile) throws IOException, SQLException {
        if (targetFile == null) {
            throw new IllegalArgumentException("Target backup file cannot be null");
        }
        databaseManager.backupTo(targetFile.toPath());
    }

    public void restoreBackup(File sourceFile) throws IOException, SQLException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Source backup file does not exist");
        }
        databaseManager.restoreFrom(sourceFile.toPath());
    }

    public String getDatabaseLocation() {
        return databaseManager.getDatabasePath().toAbsolutePath().toString();
    }

    public String getDatabaseSizeFormatted() {
        long bytes = databaseManager.getDatabaseSizeInBytes();
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }
}
