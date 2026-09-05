package com.librarymanager.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates the results of a SQLite database integrity and diagnostics check.
 */
public class DatabaseIntegrityReport {
    private boolean healthy;
    private String integrityMessage;
    private int foreignKeyViolations;
    private List<String> errorDetails = new ArrayList<>();
    private long pageCount;
    private long pageSize;
    private long freePages;
    private long fileSizeBytes;

    public DatabaseIntegrityReport() {
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public String getIntegrityMessage() {
        return integrityMessage != null ? integrityMessage : "";
    }

    public void setIntegrityMessage(String integrityMessage) {
        this.integrityMessage = integrityMessage;
    }

    public boolean isIntegrityOk() {
        return "ok".equalsIgnoreCase(integrityMessage);
    }

    public boolean isForeignKeyOk() {
        return foreignKeyViolations == 0;
    }

    public int getForeignKeyViolationsCount() {
        return foreignKeyViolations;
    }

    public long getFreelistCount() {
        return freePages;
    }

    public String getRawIntegrityResult() {
        return integrityMessage != null ? integrityMessage : "";
    }

    public int getForeignKeyViolations() {
        return foreignKeyViolations;
    }

    public void setForeignKeyViolations(int foreignKeyViolations) {
        this.foreignKeyViolations = foreignKeyViolations;
    }

    public List<String> getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(List<String> errorDetails) {
        this.errorDetails = errorDetails != null ? errorDetails : new ArrayList<>();
    }

    public long getPageCount() {
        return pageCount;
    }

    public void setPageCount(long pageCount) {
        this.pageCount = pageCount;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getFreePages() {
        return freePages;
    }

    public void setFreePages(long freePages) {
        this.freePages = freePages;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public double getFragmentationPercentage() {
        if (pageCount <= 0) return 0.0;
        return (double) freePages / pageCount * 100.0;
    }

    public String getFormattedSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        }
        double kb = fileSizeBytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }
}
