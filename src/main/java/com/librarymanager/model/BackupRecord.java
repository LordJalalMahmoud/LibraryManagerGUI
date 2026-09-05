package com.librarymanager.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Encapsulates metadata about an existing backup file or restore point snapshot.
 */
public class BackupRecord {
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String filename;
    private Path path;
    private LocalDateTime timestamp;
    private BackupType type;
    private long sizeInBytes;
    private String description;
    private boolean valid;

    public BackupRecord() {
    }

    public BackupRecord(String filename, Path path, LocalDateTime timestamp, BackupType type, long sizeInBytes, String description, boolean valid) {
        this.filename = filename;
        this.path = path;
        this.timestamp = timestamp;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
        this.description = description;
        this.valid = valid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BackupType getType() {
        return type;
    }

    public void setType(BackupType type) {
        this.type = type;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getFormattedSize() {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        }
        double kb = sizeInBytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DISPLAY_FORMATTER) : "-";
    }
}
