package com.librarymanager.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.librarymanager.dao.*;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing SQLite database backup, JSON import/export, CSV export,
 * automatic scheduled backups, backup history, restore points, and integrity diagnostics.
 */
public class BackupService {
    private static final Logger LOGGER = Logger.getLogger(BackupService.class.getName());
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final DateTimeFormatter FILE_TIMESTAMP_MILLIS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss_SSS");
    private static final String METADATA_FILENAME = "backups_metadata.json";

    private final DatabaseManager databaseManager;
    private final BookDao bookDao;
    private final ChapterDao chapterDao;
    private final ReadingSessionDao readingSessionDao;
    private final SavedSearchDao savedSearchDao;
    private final SettingsService settingsService;

    private final Gson gson;
    private ScheduledExecutorService scheduler;

    public BackupService() {
        this(
                DatabaseManager.getInstance(),
                new SqliteBookDao(),
                new SqliteChapterDao(),
                new SqliteReadingSessionDao(),
                new SqliteSavedSearchDao(),
                new SettingsService()
        );
    }

    public BackupService(DatabaseManager databaseManager) {
        this(
                databaseManager,
                new SqliteBookDao(databaseManager),
                new SqliteChapterDao(databaseManager),
                new SqliteReadingSessionDao(databaseManager),
                new SqliteSavedSearchDao(databaseManager),
                new SettingsService()
        );
    }

    public BackupService(DatabaseManager databaseManager, BookDao bookDao, ChapterDao chapterDao,
                         ReadingSessionDao readingSessionDao, SavedSearchDao savedSearchDao,
                         SettingsService settingsService) {
        this.databaseManager = databaseManager;
        this.bookDao = bookDao;
        this.chapterDao = chapterDao;
        this.readingSessionDao = readingSessionDao;
        this.savedSearchDao = savedSearchDao;
        this.settingsService = settingsService;

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        startAutoBackupScheduler();
    }

    public Gson getGson() {
        return gson;
    }

    // ==========================================
    // 1. Basic Backup & Restore
    // ==========================================

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
        // Take auto restore point before replacing
        try {
            createRestorePoint("Auto snapshot before manual database restore");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not create safety restore point", e);
        }
        databaseManager.restoreFrom(sourceFile.toPath());
    }

    public String getDatabaseLocation() {
        return databaseManager.getDatabasePath().toAbsolutePath().toString();
    }

    public String getDatabaseSizeFormatted() {
        long bytes = databaseManager.getDatabaseSizeInBytes();
        return formatSize(bytes);
    }

    public Path getBackupDirectory() {
        String customDir = settingsService.getBackupCustomDirectory();
        Path dir;
        if (customDir != null && !customDir.isBlank()) {
            dir = Paths.get(customDir);
        } else {
            String userHome = System.getProperty("user.home", ".");
            dir = Paths.get(userHome, ".librarymanager", "backups");
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create backup directory: " + dir, e);
        }
        return dir;
    }

    // ==========================================
    // 2. Backup History & Restore Points
    // ==========================================

    public BackupRecord createBackup(BackupType type, String description) throws IOException, SQLException {
        Path backupDir = getBackupDirectory();
        LocalDateTime now = LocalDateTime.now();
        String timestampStr = now.format(FILE_TIMESTAMP_MILLIS_FORMAT);

        String prefix = switch (type) {
            case AUTO -> "backup_auto_";
            case RESTORE_POINT -> "restorepoint_";
            case PRE_IMPORT -> "pre_import_";
            default -> "backup_manual_";
        };

        String filename = prefix + timestampStr + ".db";
        Path targetPath = backupDir.resolve(filename);
        int counter = 1;
        while (Files.exists(targetPath)) {
            filename = prefix + timestampStr + "_" + counter + ".db";
            targetPath = backupDir.resolve(filename);
            counter++;
        }

        databaseManager.backupTo(targetPath);

        long size = Files.size(targetPath);
        BackupRecord record = new BackupRecord(filename, targetPath, now, type, size, description, true);

        // Update metadata sidecar
        saveMetadataEntry(record);

        return record;
    }

    public BackupRecord createRestorePoint(String description) throws IOException, SQLException {
        return createBackup(BackupType.RESTORE_POINT, description != null && !description.isBlank() ? description : "Manual Restore Point");
    }

    public void restoreFromRecord(BackupRecord record) throws IOException, SQLException {
        if (record == null || record.getPath() == null || !Files.exists(record.getPath())) {
            throw new IllegalArgumentException("Backup record does not point to an existing file.");
        }
        // Safety restore point before rolling back
        createRestorePoint("Auto snapshot before rollback to " + record.getFilename());
        databaseManager.restoreFrom(record.getPath());
    }

    public boolean deleteBackup(BackupRecord record) {
        if (record == null || record.getPath() == null) return false;
        try {
            boolean deleted = Files.deleteIfExists(record.getPath());
            removeMetadataEntry(record.getFilename());
            return deleted;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete backup file: " + record.getPath(), e);
            return false;
        }
    }

    public List<BackupRecord> getBackupHistory() {
        Path backupDir = getBackupDirectory();
        List<BackupRecord> records = new ArrayList<>();
        if (!Files.exists(backupDir)) return records;

        Map<String, BackupMetadataEntry> metadataMap = loadAllMetadata();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, "*.db")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                long size = 0;
                try {
                    size = Files.size(file);
                } catch (IOException ignored) {}

                BackupMetadataEntry meta = metadataMap.get(filename);
                BackupType type;
                String desc = "";
                LocalDateTime timestamp = null;

                if (meta != null) {
                    type = meta.type != null ? meta.type : deriveTypeFromFilename(filename);
                    desc = meta.description != null ? meta.description : "";
                    if (meta.timestamp != null) {
                        try {
                            timestamp = LocalDateTime.parse(meta.timestamp);
                        } catch (Exception ignored) {}
                    }
                } else {
                    type = deriveTypeFromFilename(filename);
                }

                if (timestamp == null) {
                    timestamp = parseTimestampFromFilename(filename);
                }
                if (timestamp == null) {
                    try {
                        timestamp = LocalDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(), java.time.ZoneId.systemDefault());
                    } catch (Exception ignored) {
                        timestamp = LocalDateTime.now();
                    }
                }

                boolean valid = verifyBackupFile(file);
                records.add(new BackupRecord(filename, file, timestamp, type, size, desc, valid));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error scanning backup directory: " + backupDir, e);
        }

        // Sort descending: newest first
        records.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return records;
    }

    private BackupType deriveTypeFromFilename(String filename) {
        if (filename.startsWith("restorepoint_")) return BackupType.RESTORE_POINT;
        if (filename.startsWith("backup_auto_")) return BackupType.AUTO;
        if (filename.startsWith("pre_import_")) return BackupType.PRE_IMPORT;
        return BackupType.MANUAL;
    }

    private LocalDateTime parseTimestampFromFilename(String filename) {
        try {
            // Find pattern yyyy-MM-dd_HHmmss_SSS (21 chars)
            for (int i = 0; i <= filename.length() - 21; i++) {
                String sub = filename.substring(i, i + 21);
                if (sub.matches("\\d{4}-\\d{2}-\\d{2}_\\d{6}_\\d{3}")) {
                    return LocalDateTime.parse(sub, FILE_TIMESTAMP_MILLIS_FORMAT);
                }
            }
            // Fallback pattern yyyy-MM-dd_HHmmss (17 chars)
            for (int i = 0; i <= filename.length() - 17; i++) {
                String sub = filename.substring(i, i + 17);
                if (sub.matches("\\d{4}-\\d{2}-\\d{2}_\\d{6}")) {
                    return LocalDateTime.parse(sub, FILE_TIMESTAMP_FORMAT);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean verifyBackupFile(Path file) {
        String testUrl = "jdbc:sqlite:" + file.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA quick_check;")) {
            return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 3. Automatic Scheduled Backups
    // ==========================================

    public synchronized void checkAndRunAutoBackup() {
        if (!settingsService.isAutoBackupEnabled()) return;

        String freq = settingsService.getAutoBackupFrequency();
        String lastRunStr = settingsService.getAutoBackupLastRun();
        LocalDateTime now = LocalDateTime.now();

        boolean shouldRun = false;
        if (lastRunStr == null || lastRunStr.isBlank()) {
            shouldRun = true;
        } else {
            try {
                LocalDateTime lastRun = LocalDateTime.parse(lastRunStr);
                if ("ON_STARTUP".equalsIgnoreCase(freq)) {
                    shouldRun = true;
                } else if ("WEEKLY".equalsIgnoreCase(freq)) {
                    shouldRun = ChronoUnit.DAYS.between(lastRun, now) >= 7;
                } else {
                    // Default: DAILY
                    shouldRun = ChronoUnit.HOURS.between(lastRun, now) >= 20 || !lastRun.toLocalDate().equals(now.toLocalDate());
                }
            } catch (Exception e) {
                shouldRun = true;
            }
        }

        if (shouldRun) {
            try {
                LOGGER.log(Level.INFO, "Executing scheduled automatic backup...");
                createBackup(BackupType.AUTO, "Automatic scheduled backup (" + freq.toLowerCase() + ")");
                settingsService.setAutoBackupLastRun(now.toString());

                // Auto-prune older auto backups
                purgeOldAutoBackups();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to run scheduled auto-backup", e);
            }
        }
    }

    private void purgeOldAutoBackups() {
        int retention = settingsService.getAutoBackupRetention();
        if (retention <= 0) return;

        List<BackupRecord> history = getBackupHistory();
        List<BackupRecord> autoBackups = new ArrayList<>();
        for (BackupRecord rec : history) {
            if (rec.getType() == BackupType.AUTO) {
                autoBackups.add(rec);
            }
        }

        if (autoBackups.size() > retention) {
            // Already sorted newest first; items beyond retention are oldest
            for (int i = retention; i < autoBackups.size(); i++) {
                deleteBackup(autoBackups.get(i));
            }
        }
    }

    private void startAutoBackupScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoBackup-Worker");
            t.setDaemon(true);
            return t;
        });

        // Run check 10 seconds after startup, then every 1 hour
        scheduler.scheduleWithFixedDelay(this::checkAndRunAutoBackup, 10, 3600, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    // ==========================================
    // 4. JSON Full Library Export & Import
    // ==========================================

    public LibraryExportData exportJson(File targetFile) throws IOException {
        if (targetFile == null) throw new IllegalArgumentException("Target file cannot be null");

        LibraryExportData data = new LibraryExportData();
        data.setVersion("1.5.0");
        data.setExportDate(LocalDateTime.now().toString());

        data.setBooks(bookDao.findAll());
        data.setChapters(chapterDao.findAll());
        data.setReadingSessions(readingSessionDao.findAll());
        data.setSavedSearches(savedSearchDao.findAll());

        // Export active preferences
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put(SettingsService.KEY_THEME, settingsService.getTheme());
        settings.put(SettingsService.KEY_LANGUAGE, settingsService.getLanguage());
        settings.put(SettingsService.KEY_CONFIRM_DELETE, String.valueOf(settingsService.isConfirmDeleteEnabled()));
        settings.put(SettingsService.KEY_AUTO_BACKUP_ENABLED, String.valueOf(settingsService.isAutoBackupEnabled()));
        settings.put(SettingsService.KEY_AUTO_BACKUP_FREQ, settingsService.getAutoBackupFrequency());
        settings.put(SettingsService.KEY_AUTO_BACKUP_RETENTION, String.valueOf(settingsService.getAutoBackupRetention()));
        data.setSettings(settings);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }

        return data;
    }

    public ImportSummary importJson(File sourceFile, boolean merge) throws IOException, SQLException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Source JSON file does not exist");
        }

        LibraryExportData data;
        try (Reader reader = new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8)) {
            data = gson.fromJson(reader, LibraryExportData.class);
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON backup file: " + e.getMessage(), e);
        }

        if (data == null || data.getBooks() == null) {
            throw new IOException("Invalid backup format: Missing books list.");
        }

        // Automatic safety restore point before import
        createRestorePoint("Auto snapshot before JSON import (" + (merge ? "Merge" : "Replace") + ")");

        int booksAdded = 0;
        int booksUpdated = 0;
        int chaptersAdded = 0;
        int sessionsAdded = 0;
        int searchesAdded = 0;

        if (!merge) {
            // Replace mode: Wipe current tables and insert cleanly
            databaseManager.resetAllData();

            Map<Long, Long> oldToNewBookIdMap = new HashMap<>();

            for (Book b : data.getBooks()) {
                Long oldId = b.getId();
                Book saved = bookDao.save(b);
                if (oldId != null) {
                    oldToNewBookIdMap.put(oldId, saved.getId());
                }
                booksAdded++;
            }

            for (Chapter c : data.getChapters()) {
                Long newBookId = oldToNewBookIdMap.get(c.getBookId());
                if (newBookId != null) {
                    c.setBookId(newBookId);
                    chapterDao.save(c);
                    chaptersAdded++;
                }
            }

            for (ReadingSession s : data.getReadingSessions()) {
                Long newBookId = oldToNewBookIdMap.get(s.getBookId());
                if (newBookId != null) {
                    s.setBookId(newBookId);
                    readingSessionDao.save(s);
                    sessionsAdded++;
                }
            }

            for (SavedSearch s : data.getSavedSearches()) {
                savedSearchDao.save(s);
                searchesAdded++;
            }

        } else {
            // Merge mode: Match existing books by ISBN or Title+Author
            List<Book> existingBooks = bookDao.findAll();
            Map<Long, Long> oldToNewBookIdMap = new HashMap<>();

            for (Book importedBook : data.getBooks()) {
                Long oldId = importedBook.getId();
                Book match = findMatchingBook(importedBook, existingBooks);

                if (match != null) {
                    // Update progress & metadata if imported has more data
                    boolean changed = false;
                    if (importedBook.getCurrentPage() > match.getCurrentPage()) {
                        match.setCurrentPage(importedBook.getCurrentPage());
                        changed = true;
                    }
                    if (importedBook.getStatus() == ReadingStatus.COMPLETED && match.getStatus() != ReadingStatus.COMPLETED) {
                        match.setStatus(ReadingStatus.COMPLETED);
                        match.setDateCompleted(importedBook.getDateCompleted());
                        changed = true;
                    }
                    if ((match.getDescription() == null || match.getDescription().isBlank()) && importedBook.getDescription() != null) {
                        match.setDescription(importedBook.getDescription());
                        changed = true;
                    }
                    if ((match.getTags() == null || match.getTags().isBlank()) && importedBook.getTags() != null) {
                        match.setTags(importedBook.getTags());
                        changed = true;
                    }
                    if (changed) {
                        bookDao.update(match);
                        booksUpdated++;
                    }
                    if (oldId != null) {
                        oldToNewBookIdMap.put(oldId, match.getId());
                    }
                } else {
                    Book saved = bookDao.save(importedBook);
                    existingBooks.add(saved);
                    booksAdded++;
                    if (oldId != null) {
                        oldToNewBookIdMap.put(oldId, saved.getId());
                    }
                }
            }

            for (Chapter c : data.getChapters()) {
                Long newBookId = oldToNewBookIdMap.get(c.getBookId());
                if (newBookId != null) {
                    c.setBookId(newBookId);
                    chapterDao.save(c);
                    chaptersAdded++;
                }
            }

            for (ReadingSession s : data.getReadingSessions()) {
                Long newBookId = oldToNewBookIdMap.get(s.getBookId());
                if (newBookId != null) {
                    s.setBookId(newBookId);
                    readingSessionDao.save(s);
                    sessionsAdded++;
                }
            }

            List<SavedSearch> existingSearches = savedSearchDao.findAll();
            for (SavedSearch s : data.getSavedSearches()) {
                boolean exists = existingSearches.stream().anyMatch(e -> e.getName().equalsIgnoreCase(s.getName()));
                if (!exists) {
                    savedSearchDao.save(s);
                    searchesAdded++;
                }
            }
        }

        return new ImportSummary(booksAdded, booksUpdated, chaptersAdded, sessionsAdded, searchesAdded);
    }

    private Book findMatchingBook(Book target, List<Book> list) {
        if (target == null) return null;
        String cleanIsbn = cleanIsbn(target.getIsbn());
        String normTitle = normalizeText(target.getTitle());
        String normAuthor = normalizeText(target.getAuthor());

        for (Book b : list) {
            if (cleanIsbn != null) {
                String otherIsbn = cleanIsbn(b.getIsbn());
                if (cleanIsbn.equalsIgnoreCase(otherIsbn)) return b;
            }
            if (!normTitle.isEmpty() && !normAuthor.isEmpty()) {
                if (normTitle.equalsIgnoreCase(normalizeText(b.getTitle())) &&
                    normAuthor.equalsIgnoreCase(normalizeText(b.getAuthor()))) {
                    return b;
                }
            }
        }
        return null;
    }

    private String cleanIsbn(String isbn) {
        if (isbn == null) return null;
        String clean = isbn.replaceAll("[^0-9a-zA-Z]", "").toUpperCase();
        return clean.length() >= 8 ? clean : null;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[\\p{Punct}\\s]+", " ").trim();
    }

    // ==========================================
    // 5. CSV Export
    // ==========================================

    public int exportCsv(File targetFile) throws IOException {
        if (targetFile == null) throw new IllegalArgumentException("Target file cannot be null");

        List<Book> books = bookDao.findAll();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Microsoft Excel / Numbers compatibility
            writer.write("\uFEFF");

            // CSV Header
            writer.write("ID,Title,Author,Category,Status,Current Page,Total Pages,Progress %,Volumes,Publisher,ISBN,Tags,Favorite,Wishlist,Date Added,Date Started,Date Completed,Description\n");

            for (Book b : books) {
                StringBuilder line = new StringBuilder();
                line.append(escapeCsv(b.getId())).append(",");
                line.append(escapeCsv(b.getTitle())).append(",");
                line.append(escapeCsv(b.getAuthor())).append(",");
                line.append(escapeCsv(b.getCategory())).append(",");
                line.append(escapeCsv(b.getStatus() != null ? b.getStatus().name() : "")).append(",");
                line.append(escapeCsv(b.getCurrentPage())).append(",");
                line.append(escapeCsv(b.getTotalPages())).append(",");
                line.append(escapeCsv(b.getProgressPercentage())).append(",");
                line.append(escapeCsv(b.getTotalParts())).append(",");
                line.append(escapeCsv(b.getPublisher())).append(",");
                line.append(escapeCsv(b.getIsbn())).append(",");
                line.append(escapeCsv(b.getTags())).append(",");
                line.append(escapeCsv(b.isFavorite() ? "Yes" : "No")).append(",");
                line.append(escapeCsv(b.isWishlist() ? "Yes" : "No")).append(",");
                line.append(escapeCsv(b.getDateAdded())).append(",");
                line.append(escapeCsv(b.getDateStarted())).append(",");
                line.append(escapeCsv(b.getDateCompleted())).append(",");
                line.append(escapeCsv(b.getDescription()));
                line.append("\n");
                writer.write(line.toString());
            }
        }

        return books.size();
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ==========================================
    // 6. Database Integrity & Diagnostics
    // ==========================================

    public DatabaseIntegrityReport checkDatabaseIntegrity() {
        return databaseManager.checkIntegrity();
    }

    public void optimizeDatabase() throws SQLException {
        databaseManager.optimizeAndVacuum();
    }

    // ==========================================
    // Internal Metadata Persistence Helpers
    // ==========================================

    private static class BackupMetadataEntry {
        String description;
        BackupType type;
        String timestamp;
    }

    private Path getMetadataFilePath() {
        return getBackupDirectory().resolve(METADATA_FILENAME);
    }

    private synchronized Map<String, BackupMetadataEntry> loadAllMetadata() {
        Path metaFile = getMetadataFilePath();
        if (!Files.exists(metaFile)) return new HashMap<>();
        try (Reader reader = Files.newBufferedReader(metaFile, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, BackupMetadataEntry>>() {}.getType();
            Map<String, BackupMetadataEntry> map = gson.fromJson(reader, type);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not read backups metadata", e);
            return new HashMap<>();
        }
    }

    private synchronized void saveMetadataEntry(BackupRecord record) {
        Map<String, BackupMetadataEntry> map = loadAllMetadata();
        BackupMetadataEntry entry = new BackupMetadataEntry();
        entry.description = record.getDescription();
        entry.type = record.getType();
        entry.timestamp = record.getTimestamp() != null ? record.getTimestamp().toString() : LocalDateTime.now().toString();
        map.put(record.getFilename(), entry);
        writeMetadataMap(map);
    }

    private synchronized void removeMetadataEntry(String filename) {
        Map<String, BackupMetadataEntry> map = loadAllMetadata();
        if (map.remove(filename) != null) {
            writeMetadataMap(map);
        }
    }

    private synchronized void writeMetadataMap(Map<String, BackupMetadataEntry> map) {
        Path metaFile = getMetadataFilePath();
        try (Writer writer = Files.newBufferedWriter(metaFile, StandardCharsets.UTF_8)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write backups metadata", e);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f MB", mb);
    }

    // ==========================================
    // Supporting Types & Type Adapters
    // ==========================================

    public static class ImportSummary {
        private final int booksAdded;
        private final int booksUpdated;
        private final int chaptersAdded;
        private final int sessionsAdded;
        private final int searchesAdded;

        public ImportSummary(int booksAdded, int booksUpdated, int chaptersAdded, int sessionsAdded, int searchesAdded) {
            this.booksAdded = booksAdded;
            this.booksUpdated = booksUpdated;
            this.chaptersAdded = chaptersAdded;
            this.sessionsAdded = sessionsAdded;
            this.searchesAdded = searchesAdded;
        }

        public int getBooksAdded() { return booksAdded; }
        public int getBooksUpdated() { return booksUpdated; }
        public int getChaptersAdded() { return chaptersAdded; }
        public int getSessionsAdded() { return sessionsAdded; }
        public int getSearchesAdded() { return searchesAdded; }
    }

    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull() || json.getAsString().isBlank()) return null;
            try {
                return LocalDate.parse(json.getAsString());
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull() || json.getAsString().isBlank()) return null;
            try {
                return LocalDateTime.parse(json.getAsString());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
