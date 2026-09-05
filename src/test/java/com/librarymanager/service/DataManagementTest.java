package com.librarymanager.service;

import com.librarymanager.dao.*;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataManagementTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private BookDao bookDao;
    private ChapterDao chapterDao;
    private ReadingSessionDao sessionDao;
    private SavedSearchDao savedSearchDao;
    private SettingsService settingsService;
    private BackupService backupService;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("data_mgmt_test.db");
        dbManager = new DatabaseManager(dbPath);

        bookDao = new SqliteBookDao(dbManager);
        chapterDao = new SqliteChapterDao(dbManager);
        sessionDao = new SqliteReadingSessionDao(dbManager);
        savedSearchDao = new SqliteSavedSearchDao(dbManager);

        SettingsDao settingsDao = new SqliteSettingsDao(dbManager);
        settingsService = new SettingsService(settingsDao);

        // Configure backup dir to tempDir/backups
        Path backupDir = tempDir.resolve("backups");
        settingsService.setBackupCustomDirectory(backupDir.toString());

        backupService = new BackupService(dbManager, bookDao, chapterDao, sessionDao, savedSearchDao, settingsService);
    }

    @AfterEach
    void tearDown() {
        backupService.shutdown();
    }

    @Test
    @DisplayName("1. Full JSON Export and Import in Replace Mode")
    void testJsonExportAndImportReplace() throws IOException, SQLException {
        // Setup initial library
        Book b1 = new Book("Clean Architecture", "Robert Martin", 350, 1);
        b1.setIsbn("978-0134494166");
        b1.setStatus(ReadingStatus.READING);
        b1.setCurrentPage(120);
        b1 = bookDao.save(b1);

        Chapter c1 = new Chapter(b1.getId(), 1, "Chapter 1: Design Principles", 1, 30);
        chapterDao.save(c1);

        ReadingSession s1 = new ReadingSession(b1.getId(), LocalDate.now().minusDays(1), 1, 30, 30, 45, "Good chapter");
        sessionDao.save(s1);

        SavedSearch search1 = new SavedSearch("Reading Books", "Architecture", "Robert", ReadingStatus.READING, null, null, null, null, null, null, "DATE_ADDED", false);
        savedSearchDao.save(search1);

        // Export to JSON
        File jsonFile = tempDir.resolve("export_test.json").toFile();
        LibraryExportData exported = backupService.exportJson(jsonFile);
        assertNotNull(exported);
        assertTrue(jsonFile.exists());
        assertTrue(jsonFile.length() > 0);

        String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(jsonContent.contains("Clean Architecture"));
        assertTrue(jsonContent.contains("Chapter 1: Design Principles"));
        assertTrue(jsonContent.contains("Reading Books"));

        // Now modify existing library (e.g. wipe and add completely different book)
        dbManager.resetAllData();
        assertEquals(0, bookDao.findAll().size());
        Book bDifferent = new Book("Completely Different", "Other", 100, 1);
        bookDao.save(bDifferent);
        assertEquals(1, bookDao.findAll().size());

        // Import with Replace Mode
        BackupService.ImportSummary summary = backupService.importJson(jsonFile, false);
        assertEquals(1, summary.getBooksAdded());
        assertEquals(1, summary.getChaptersAdded());
        assertEquals(1, summary.getSessionsAdded());
        assertEquals(1, summary.getSearchesAdded());

        List<Book> books = bookDao.findAll();
        assertEquals(1, books.size());
        Book importedBook = books.get(0);
        assertEquals("Clean Architecture", importedBook.getTitle());
        assertEquals("978-0134494166", importedBook.getIsbn());
        assertEquals(120, importedBook.getCurrentPage());

        List<Chapter> chapters = chapterDao.findByBookId(importedBook.getId());
        assertEquals(1, chapters.size());
        assertEquals("Chapter 1: Design Principles", chapters.get(0).getTitle());

        List<ReadingSession> sessions = sessionDao.findByBookId(importedBook.getId());
        assertEquals(1, sessions.size());
        assertEquals(45, sessions.get(0).getDurationMinutes());
    }

    @Test
    @DisplayName("2. Full JSON Import in Merge Mode")
    void testJsonImportMergeMode() throws IOException, SQLException {
        // Existing book in library
        Book bExisting = new Book("Refactoring", "Martin Fowler", 400, 1);
        bExisting.setIsbn("978-0201485677");
        bExisting.setCurrentPage(50);
        bExisting.setStatus(ReadingStatus.READING);
        bookDao.save(bExisting);

        // Prepare JSON export containing updated Refactoring + a new book Domain-Driven Design
        Book bUpdated = new Book("Refactoring", "Martin Fowler", 400, 1);
        bUpdated.setIsbn("978-0201485677");
        bUpdated.setCurrentPage(250); // More progress
        bUpdated.setStatus(ReadingStatus.COMPLETED);
        bUpdated.setDateCompleted(LocalDate.now());
        bUpdated.setTags("Architecture, Code Quality");

        Book bNew = new Book("Domain-Driven Design", "Eric Evans", 500, 1);
        bNew.setIsbn("978-0321125217");

        LibraryExportData data = new LibraryExportData();
        data.setVersion("1.5.0");
        data.setExportDate(LocalDateTime.now().toString());
        data.setBooks(List.of(bUpdated, bNew));
        data.setChapters(List.of());
        data.setReadingSessions(List.of());
        data.setSavedSearches(List.of());

        File jsonFile = tempDir.resolve("merge_test.json").toFile();
        com.google.gson.Gson gson = backupService.getGson();
        try (var writer = Files.newBufferedWriter(jsonFile.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }

        // Import with Merge
        BackupService.ImportSummary summary = backupService.importJson(jsonFile, true);
        assertEquals(1, summary.getBooksAdded());
        assertEquals(1, summary.getBooksUpdated());

        List<Book> allBooks = bookDao.findAll();
        assertEquals(2, allBooks.size());

        Book refactored = allBooks.stream().filter(b -> b.getTitle().equals("Refactoring")).findFirst().orElseThrow();
        assertEquals(250, refactored.getCurrentPage());
        assertEquals(ReadingStatus.COMPLETED, refactored.getStatus());
        assertEquals("Architecture, Code Quality", refactored.getTags());

        assertTrue(allBooks.stream().anyMatch(b -> b.getTitle().equals("Domain-Driven Design")));
    }

    @Test
    @DisplayName("3. CSV Export with UTF-8 BOM and RFC 4180 Escaping")
    void testCsvExport() throws IOException {
        Book b1 = new Book("The Pragmatic Programmer", "David Thomas, Andrew Hunt", 352, 1);
        b1.setCategory("Software Engineering");
        b1.setDescription("A book with \"quotes\", commas, and\nnewlines!");
        b1.setIsbn("978-0135957059");
        bookDao.save(b1);

        Book b2 = new Book("كتاب بالعربية", "كاتب عربي", 200, 1);
        bookDao.save(b2);

        File csvFile = tempDir.resolve("export.csv").toFile();
        int count = backupService.exportCsv(csvFile);
        assertEquals(2, count);
        assertTrue(csvFile.exists());

        byte[] rawBytes = Files.readAllBytes(csvFile.toPath());
        // Verify UTF-8 BOM (0xEF, 0xBB, 0xBF)
        assertEquals((byte) 0xEF, rawBytes[0]);
        assertEquals((byte) 0xBB, rawBytes[1]);
        assertEquals((byte) 0xBF, rawBytes[2]);

        String content = new String(rawBytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("The Pragmatic Programmer"));
        assertTrue(content.contains("\"David Thomas, Andrew Hunt\"")); // Commas escaped
        assertTrue(content.contains("\"A book with \"\"quotes\"\", commas, and\nnewlines!\"")); // RFC 4180 double-quotes
        assertTrue(content.contains("كتاب بالعربية")); // Arabic preserved
    }

    @Test
    @DisplayName("4. Restore Points and Backup History Management")
    void testRestorePointsAndHistory() throws IOException, SQLException {
        Book b1 = new Book("Initial Book", "Author", 200, 1);
        bookDao.save(b1);

        // Create manual restore point
        BackupRecord rp = backupService.createRestorePoint("Point before big change");
        assertNotNull(rp);
        assertEquals(BackupType.RESTORE_POINT, rp.getType());
        assertEquals("Point before big change", rp.getDescription());
        assertTrue(rp.isValid());

        // Create manual backup
        BackupRecord manual = backupService.createBackup(BackupType.MANUAL, "Manual backup test");
        assertEquals(BackupType.MANUAL, manual.getType());

        // Check history
        List<BackupRecord> history = backupService.getBackupHistory();
        assertTrue(history.size() >= 2);
        assertTrue(history.stream().anyMatch(r -> r.getFilename().equals(rp.getFilename())));
        assertTrue(history.stream().anyMatch(r -> r.getFilename().equals(manual.getFilename())));

        // Add more data to DB
        bookDao.save(new Book("Temporary Book", "Temp", 100, 1));
        assertEquals(2, bookDao.findAll().size());

        // Restore to restore point
        backupService.restoreFromRecord(rp);

        // Should have rolled back to 1 book
        assertEquals(1, bookDao.findAll().size());
        assertEquals("Initial Book", bookDao.findAll().get(0).getTitle());

        // Delete backup record
        boolean deleted = backupService.deleteBackup(manual);
        assertTrue(deleted);
        assertFalse(Files.exists(manual.getPath()));
    }

    @Test
    @DisplayName("5. Automatic Scheduled Backup and Retention Pruning")
    void testAutoBackupAndPruning() throws IOException, SQLException {
        settingsService.setAutoBackupEnabled(true);
        settingsService.setAutoBackupFrequency("ON_STARTUP");
        settingsService.setAutoBackupRetention(2);

        // Run auto backup 3 times
        backupService.createBackup(BackupType.AUTO, "Auto 1");
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        backupService.createBackup(BackupType.AUTO, "Auto 2");
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        backupService.createBackup(BackupType.AUTO, "Auto 3");

        // Also create a manual backup and a restore point
        backupService.createBackup(BackupType.MANUAL, "Manual keep");
        backupService.createRestorePoint("Restore point keep");

        // Trigger auto-backup check and purge
        backupService.checkAndRunAutoBackup();

        List<BackupRecord> history = backupService.getBackupHistory();
        long autoCount = history.stream().filter(r -> r.getType() == BackupType.AUTO).count();
        // Retention is 2, so autoCount should not exceed 2
        assertTrue(autoCount <= 2, "Auto backups should be pruned to retention limit");

        // MANUAL and RESTORE_POINT must not be pruned!
        assertTrue(history.stream().anyMatch(r -> r.getType() == BackupType.MANUAL));
        assertTrue(history.stream().anyMatch(r -> r.getType() == BackupType.RESTORE_POINT));
    }

    @Test
    @DisplayName("6. Database Integrity Check and VACUUM Defragmentation")
    void testDatabaseIntegrityAndOptimize() throws SQLException {
        bookDao.save(new Book("Test Book", "Author", 150, 1));

        DatabaseIntegrityReport report = backupService.checkDatabaseIntegrity();
        assertNotNull(report);
        assertTrue(report.isHealthy());
        assertTrue(report.isIntegrityOk());
        assertEquals("ok", report.getIntegrityMessage().toLowerCase());
        assertEquals(0, report.getForeignKeyViolationsCount());
        assertTrue(report.getPageCount() > 0);
        assertTrue(report.getPageSize() > 0);

        // Optimize and vacuum
        assertDoesNotThrow(() -> backupService.optimizeDatabase());

        DatabaseIntegrityReport reportAfter = backupService.checkDatabaseIntegrity();
        assertTrue(reportAfter.isHealthy());
    }
}
