package com.librarymanager.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the SQLite database connection, initialization, schema setup,
 * and database file backup / restore operations.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_DIR_NAME = ".librarymanager";
    private static final String DB_FILE_NAME = "library.db";

    private static DatabaseManager instance;

    private final Path databasePath;
    private final String jdbcUrl;

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            Path defaultDir = Paths.get(System.getProperty("user.home"), DB_DIR_NAME);
            instance = new DatabaseManager(defaultDir.resolve(DB_FILE_NAME));
        }
        return instance;
    }

    /**
     * Allows custom database path (useful for testing or dedicated configurations).
     */
    public static synchronized void initCustom(Path customDbPath) {
        instance = new DatabaseManager(customDbPath);
    }

    public DatabaseManager(Path dbPath) {
        this.databasePath = dbPath;
        try {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not create database directory: " + dbPath.getParent(), e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initializeDatabase();
    }

    /**
     * Gets a new connection to the SQLite database.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public long getDatabaseSizeInBytes() {
        try {
            if (Files.exists(databasePath)) {
                return Files.size(databasePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read database size", e);
        }
        return 0L;
    }

    /**
     * Initializes the SQLite database schema and required indexes.
     */
    public void initializeDatabase() {
        String createBooksTable = """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                total_pages INTEGER NOT NULL CHECK(total_pages > 0),
                total_parts INTEGER NOT NULL DEFAULT 1 CHECK(total_parts > 0),
                current_page INTEGER NOT NULL DEFAULT 0 CHECK(current_page >= 0),
                status TEXT NOT NULL DEFAULT 'NOT_STARTED',
                description TEXT,
                cover_image TEXT,
                date_added TEXT NOT NULL,
                date_started TEXT,
                date_completed TEXT,
                category TEXT,
                publisher TEXT,
                isbn TEXT,
                tags TEXT,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                is_wishlist INTEGER NOT NULL DEFAULT 0
            );
            """;

        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS app_settings (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT
            );
            """;

        String createChaptersTable = """
            CREATE TABLE IF NOT EXISTS chapters (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                chapter_number INTEGER NOT NULL DEFAULT 1,
                title TEXT NOT NULL,
                start_page INTEGER DEFAULT 0,
                end_page INTEGER DEFAULT 0,
                is_completed INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
            );
            """;

        String createSessionsTable = """
            CREATE TABLE IF NOT EXISTS reading_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                session_date TEXT NOT NULL,
                start_page INTEGER NOT NULL DEFAULT 0,
                end_page INTEGER NOT NULL DEFAULT 0,
                pages_read INTEGER NOT NULL DEFAULT 0,
                duration_minutes INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
            );
            """;

        String createSavedSearchesTable = """
            CREATE TABLE IF NOT EXISTS saved_searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                query TEXT,
                author TEXT,
                status TEXT,
                category TEXT,
                tag TEXT,
                is_favorite INTEGER,
                is_wishlist INTEGER,
                min_pages INTEGER,
                max_pages INTEGER,
                sort_by TEXT,
                ascending INTEGER NOT NULL DEFAULT 0,
                date_created TEXT NOT NULL
            );
            """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_books_status ON books(status);
            CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
            CREATE INDEX IF NOT EXISTS idx_books_author ON books(author);
            CREATE INDEX IF NOT EXISTS idx_books_date_added ON books(date_added);
            CREATE INDEX IF NOT EXISTS idx_books_category ON books(category);
            CREATE INDEX IF NOT EXISTS idx_books_is_favorite ON books(is_favorite);
            CREATE INDEX IF NOT EXISTS idx_books_is_wishlist ON books(is_wishlist);
            CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);
            CREATE INDEX IF NOT EXISTS idx_chapters_book_id ON chapters(book_id);
            CREATE INDEX IF NOT EXISTS idx_sessions_book_id ON reading_sessions(book_id);
            CREATE INDEX IF NOT EXISTS idx_sessions_date ON reading_sessions(session_date);
            CREATE INDEX IF NOT EXISTS idx_saved_searches_name ON saved_searches(name);
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(createBooksTable);
            stmt.execute(createChaptersTable);
            stmt.execute(createSessionsTable);
            stmt.execute(createSettingsTable);
            stmt.execute(createSavedSearchesTable);
            migrateSchema(conn);
            stmt.execute(createIndexes);
            LOGGER.info("Database initialized successfully at: " + databasePath);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database", e);
            throw new RuntimeException("Database initialization error", e);
        }
    }

    private void migrateSchema(Connection conn) {
        String createSessionsTableIfMissing = """
            CREATE TABLE IF NOT EXISTS reading_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                session_date TEXT NOT NULL,
                start_page INTEGER NOT NULL DEFAULT 0,
                end_page INTEGER NOT NULL DEFAULT 0,
                pages_read INTEGER NOT NULL DEFAULT 0,
                duration_minutes INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
            );
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSessionsTableIfMissing);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_book_id ON reading_sessions(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_date ON reading_sessions(session_date);");
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Session table migration note: " + e.getMessage());
        }

        String[] alterStatements = {
            "ALTER TABLE books ADD COLUMN category TEXT;",
            "ALTER TABLE books ADD COLUMN publisher TEXT;",
            "ALTER TABLE books ADD COLUMN isbn TEXT;",
            "ALTER TABLE books ADD COLUMN tags TEXT;",
            "ALTER TABLE books ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0;",
            "ALTER TABLE books ADD COLUMN is_wishlist INTEGER NOT NULL DEFAULT 0;"
        };

        for (String sql : alterStatements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException ignored) {
                // Column already exists, safe to ignore
            }
        }

        String createSavedSearchesIfMissing = """
            CREATE TABLE IF NOT EXISTS saved_searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                query TEXT,
                author TEXT,
                status TEXT,
                category TEXT,
                tag TEXT,
                is_favorite INTEGER,
                is_wishlist INTEGER,
                min_pages INTEGER,
                max_pages INTEGER,
                sort_by TEXT,
                ascending INTEGER NOT NULL DEFAULT 0,
                date_created TEXT NOT NULL
            );
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSavedSearchesIfMissing);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_saved_searches_name ON saved_searches(name);");
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Saved searches table migration note: " + e.getMessage());
        }
    }

    /**
     * Backs up the SQLite database to the specified target path.
     */
    public void backupTo(Path targetFile) throws IOException, SQLException {
        // Run SQLite checkpoint before copying to ensure all transactions are flushed
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA wal_checkpoint(FULL);");
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Checkpoint note: " + e.getMessage());
        }

        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        Files.copy(databasePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restores database from a backup file after verifying it's a valid SQLite database.
     */
    public void restoreFrom(Path backupFile) throws IOException, SQLException {
        if (!Files.exists(backupFile)) {
            throw new IOException("Backup file does not exist: " + backupFile);
        }

        // Test opening backup file as SQLite database
        String testUrl = "jdbc:sqlite:" + backupFile.toAbsolutePath();
        try (Connection testConn = DriverManager.getConnection(testUrl);
             Statement testStmt = testConn.createStatement()) {
            testStmt.execute("SELECT count(*) FROM books;");
        } catch (SQLException e) {
            throw new SQLException("Invalid library backup file: Missing books table or corrupted SQLite file.", e);
        }

        // Copy backup over active database file
        Files.copy(backupFile, databasePath, StandardCopyOption.REPLACE_EXISTING);
        initializeDatabase();
    }

    /**
     * Resets database by truncating all books.
     */
    public void resetAllData() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM saved_searches;");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='saved_searches';");
            stmt.execute("DELETE FROM reading_sessions;");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='reading_sessions';");
            stmt.execute("DELETE FROM chapters;");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='chapters';");
            stmt.execute("DELETE FROM books;");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='books';");
            stmt.execute("VACUUM;");
        }
    }
}
