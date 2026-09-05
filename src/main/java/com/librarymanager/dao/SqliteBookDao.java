package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.AuthorStat;
import com.librarymanager.model.Book;
import com.librarymanager.model.CategoryStat;
import com.librarymanager.model.DuplicateGroup;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.util.I18n;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of BookDao using PreparedStatements.
 */
public class SqliteBookDao implements BookDao {
    private static final Logger LOGGER = Logger.getLogger(SqliteBookDao.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DatabaseManager databaseManager;

    public SqliteBookDao() {
        this(DatabaseManager.getInstance());
    }

    public SqliteBookDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Book save(Book book) {
        String sql = """
            INSERT INTO books (
                title, author, total_pages, total_parts, current_page, status,
                description, cover_image, date_added, date_started, date_completed,
                category, publisher, isbn, tags, is_favorite, is_wishlist
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getTotalPages());
            stmt.setInt(4, book.getTotalParts());
            stmt.setInt(5, book.getCurrentPage());
            stmt.setString(6, book.getStatus().name());
            stmt.setString(7, book.getDescription());
            stmt.setString(8, book.getCoverImage());
            stmt.setString(9, book.getDateAdded() != null ? book.getDateAdded().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER));
            stmt.setString(10, book.getDateStarted() != null ? book.getDateStarted().format(DATE_FORMATTER) : null);
            stmt.setString(11, book.getDateCompleted() != null ? book.getDateCompleted().format(DATE_FORMATTER) : null);
            stmt.setString(12, book.getCategory());
            stmt.setString(13, book.getPublisher());
            stmt.setString(14, book.getIsbn());
            stmt.setString(15, book.getTags());
            stmt.setInt(16, book.isFavorite() ? 1 : 0);
            stmt.setInt(17, book.isWishlist() ? 1 : 0);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setId(generatedKeys.getLong(1));
                    }
                }
            }
            return book;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert book: " + book.getTitle(), e);
            throw new RuntimeException("Database error saving book", e);
        }
    }

    @Override
    public void update(Book book) {
        if (book.getId() == null) {
            throw new IllegalArgumentException("Cannot update book without ID");
        }

        String sql = """
            UPDATE books
            SET title = ?, author = ?, total_pages = ?, total_parts = ?, current_page = ?, status = ?,
                description = ?, cover_image = ?, date_started = ?, date_completed = ?,
                category = ?, publisher = ?, isbn = ?, tags = ?, is_favorite = ?, is_wishlist = ?
            WHERE id = ?;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getTotalPages());
            stmt.setInt(4, book.getTotalParts());
            stmt.setInt(5, book.getCurrentPage());
            stmt.setString(6, book.getStatus().name());
            stmt.setString(7, book.getDescription());
            stmt.setString(8, book.getCoverImage());
            stmt.setString(9, book.getDateStarted() != null ? book.getDateStarted().format(DATE_FORMATTER) : null);
            stmt.setString(10, book.getDateCompleted() != null ? book.getDateCompleted().format(DATE_FORMATTER) : null);
            stmt.setString(11, book.getCategory());
            stmt.setString(12, book.getPublisher());
            stmt.setString(13, book.getIsbn());
            stmt.setString(14, book.getTags());
            stmt.setInt(15, book.isFavorite() ? 1 : 0);
            stmt.setInt(16, book.isWishlist() ? 1 : 0);
            stmt.setLong(17, book.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update book id: " + book.getId(), e);
            throw new RuntimeException("Database error updating book", e);
        }
    }

    @Override
    public void delete(long id) {
        String deleteChapters = "DELETE FROM chapters WHERE book_id = ?;";
        String deleteBook = "DELETE FROM books WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt1 = conn.prepareStatement(deleteChapters)) {
                    stmt1.setLong(1, id);
                    stmt1.executeUpdate();
                }
                try (PreparedStatement stmt2 = conn.prepareStatement(deleteBook)) {
                    stmt2.setLong(1, id);
                    stmt2.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete book id: " + id, e);
            throw new RuntimeException("Database error deleting book", e);
        }
    }

    @Override
    public Optional<Book> findById(long id) {
        String sql = """
            SELECT b.*,
                (SELECT count(*) FROM chapters WHERE book_id = b.id) AS total_chapters,
                (SELECT count(*) FROM chapters WHERE book_id = b.id AND is_completed = 1) AS completed_chapters
            FROM books b WHERE b.id = ?;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find book by id: " + id, e);
            throw new RuntimeException("Database error finding book", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() {
        return search(null, null, "date_added", false);
    }

    @Override
    public List<Book> findByStatus(ReadingStatus status) {
        return search(null, status, "date_added", false);
    }

    @Override
    public List<Book> search(String query, ReadingStatus statusFilter, String sortBy, boolean ascending) {
        return search(query, statusFilter, null, null, null, null, sortBy, ascending);
    }

    @Override
    public List<Book> search(String query, ReadingStatus statusFilter, String categoryFilter, String tagFilter,
                             Boolean isFavorite, Boolean isWishlist, String sortBy, boolean ascending) {
        return search(query, null, statusFilter, categoryFilter, tagFilter, isFavorite, isWishlist, null, null, sortBy, ascending);
    }

    @Override
    public List<Book> search(String query, String authorQuery, ReadingStatus statusFilter, String categoryFilter, String tagFilter,
                             Boolean isFavorite, Boolean isWishlist, Integer minPages, Integer maxPages,
                             String sortBy, boolean ascending) {
        StringBuilder sql = new StringBuilder("""
            SELECT b.*,
                (SELECT count(*) FROM chapters WHERE book_id = b.id) AS total_chapters,
                (SELECT count(*) FROM chapters WHERE book_id = b.id AND is_completed = 1) AS completed_chapters
            FROM books b WHERE 1=1\s""");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("""
                AND (
                    LOWER(b.title) LIKE ?
                    OR LOWER(b.author) LIKE ?
                    OR LOWER(COALESCE(b.publisher, '')) LIKE ?
                    OR LOWER(COALESCE(b.isbn, '')) LIKE ?
                    OR LOWER(COALESCE(b.category, '')) LIKE ?
                    OR LOWER(COALESCE(b.tags, '')) LIKE ?
                    OR LOWER(COALESCE(b.description, '')) LIKE ?
                )\s""");
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            for (int i = 0; i < 7; i++) {
                params.add(wildcard);
            }
        }

        if (authorQuery != null && !authorQuery.trim().isEmpty()) {
            sql.append("AND LOWER(b.author) LIKE ? ");
            params.add("%" + authorQuery.trim().toLowerCase() + "%");
        }

        if (statusFilter != null) {
            sql.append("AND b.status = ? ");
            params.add(statusFilter.name());
        }

        if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
            sql.append("AND LOWER(b.category) = LOWER(?) ");
            params.add(categoryFilter.trim());
        }

        if (tagFilter != null && !tagFilter.trim().isEmpty()) {
            sql.append("AND LOWER(COALESCE(b.tags, '')) LIKE ? ");
            params.add("%" + tagFilter.trim().toLowerCase() + "%");
        }

        if (isFavorite != null) {
            sql.append("AND b.is_favorite = ? ");
            params.add(isFavorite ? 1 : 0);
        }

        if (isWishlist != null) {
            sql.append("AND b.is_wishlist = ? ");
            params.add(isWishlist ? 1 : 0);
        }

        if (minPages != null && minPages > 0) {
            sql.append("AND b.total_pages >= ? ");
            params.add(minPages);
        }

        if (maxPages != null && maxPages > 0) {
            sql.append("AND b.total_pages <= ? ");
            params.add(maxPages);
        }

        // Safe order by column mapping
        String orderColumn;
        if ("title".equalsIgnoreCase(sortBy)) {
            orderColumn = "LOWER(b.title)";
        } else if ("author".equalsIgnoreCase(sortBy)) {
            orderColumn = "LOWER(b.author)";
        } else if ("publisher".equalsIgnoreCase(sortBy)) {
            orderColumn = "LOWER(COALESCE(b.publisher, ''))";
        } else if ("category".equalsIgnoreCase(sortBy)) {
            orderColumn = "LOWER(COALESCE(b.category, ''))";
        } else if ("progress".equalsIgnoreCase(sortBy)) {
            orderColumn = "(CAST(b.current_page AS REAL) / b.total_pages)";
        } else if ("total_pages".equalsIgnoreCase(sortBy)) {
            orderColumn = "b.total_pages";
        } else {
            orderColumn = "b.date_added";
        }

        sql.append("ORDER BY ").append(orderColumn).append(ascending ? " ASC" : " DESC");
        sql.append(", b.id").append(ascending ? " ASC" : " DESC");

        List<Book> books = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Search query failed", e);
            throw new RuntimeException("Database error executing search", e);
        }

        return books;
    }

    @Override
    public List<String> findAllCategories() {
        String sql = "SELECT DISTINCT category FROM books WHERE category IS NOT NULL AND TRIM(category) != '' ORDER BY category COLLATE NOCASE ASC;";
        List<String> categories = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String cat = rs.getString(1);
                if (cat != null && !cat.trim().isEmpty()) {
                    categories.add(cat.trim());
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load distinct categories", e);
        }
        return categories;
    }

    @Override
    public List<String> findAllTags() {
        String sql = "SELECT tags FROM books WHERE tags IS NOT NULL AND TRIM(tags) != '';";
        java.util.Set<String> tagSet = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String raw = rs.getString(1);
                if (raw != null) {
                    for (String t : raw.split(",")) {
                        String clean = t.trim();
                        if (!clean.isEmpty()) {
                            tagSet.add(clean);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load tags", e);
        }
        return new ArrayList<>(tagSet);
    }

    @Override
    public void toggleFavorite(long id, boolean isFavorite) {
        String sql = "UPDATE books SET is_favorite = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, isFavorite ? 1 : 0);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to toggle favorite for book id: " + id, e);
            throw new RuntimeException("Database error updating favorite status", e);
        }
    }

    @Override
    public void toggleWishlist(long id, boolean isWishlist) {
        String sql = "UPDATE books SET is_wishlist = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, isWishlist ? 1 : 0);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to toggle wishlist for book id: " + id, e);
            throw new RuntimeException("Database error updating wishlist status", e);
        }
    }

    @Override
    public LibraryStats getStatistics() {
        LibraryStats stats = new LibraryStats();

        String countSql = """
            SELECT
                count(*) AS total_count,
                SUM(CASE WHEN status = 'READING' THEN 1 ELSE 0 END) AS reading_count,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
                SUM(CASE WHEN status = 'NOT_STARTED' THEN 1 ELSE 0 END) AS not_started_count,
                COALESCE(SUM(total_pages), 0) AS total_pages_sum,
                COALESCE(SUM(current_page), 0) AS pages_read_sum
            FROM books;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.setTotalBooks(rs.getInt("total_count"));
                stats.setReadingCount(rs.getInt("reading_count"));
                stats.setCompletedCount(rs.getInt("completed_count"));
                stats.setNotStartedCount(rs.getInt("not_started_count"));
                stats.setTotalPages(rs.getInt("total_pages_sum"));
                stats.setPagesRead(rs.getInt("pages_read_sum"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to compute library stats", e);
            throw new RuntimeException("Database error computing stats", e);
        }

        // Fetch recently added books (up to 5)
        String recentAddedSql = "SELECT * FROM books ORDER BY id DESC LIMIT 5;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(recentAddedSql);
             ResultSet rs = stmt.executeQuery()) {
            List<Book> recent = new ArrayList<>();
            while (rs.next()) {
                recent.add(mapResultSetToBook(rs));
            }
            stats.setRecentlyAdded(recent);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load recently added books", e);
        }

        // Fetch recently completed books (up to 5)
        String recentCompletedSql = "SELECT * FROM books WHERE status = 'COMPLETED' ORDER BY date_completed DESC, id DESC LIMIT 5;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(recentCompletedSql);
             ResultSet rs = stmt.executeQuery()) {
            List<Book> completed = new ArrayList<>();
            while (rs.next()) {
                completed.add(mapResultSetToBook(rs));
            }
            stats.setRecentlyCompleted(completed);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load recently completed books", e);
        }

        return stats;
    }

    @Override
    public void deleteAll() {
        try (Connection conn = databaseManager.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM chapters;");
                stmt.execute("DELETE FROM books;");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete all books", e);
            throw new RuntimeException("Database error resetting books", e);
        }
    }

    @Override
    public Map<Integer, Integer> getBooksCompletedByMonthInYear(int year) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            result.put(m, 0);
        }

        String sql = """
            SELECT CAST(substr(date_completed, 6, 2) AS INTEGER) AS m, count(*) AS completed_count
            FROM books
            WHERE status = 'COMPLETED' AND date_completed IS NOT NULL AND substr(date_completed, 1, 4) = ?
            GROUP BY m;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int m = rs.getInt("m");
                    int count = rs.getInt("completed_count");
                    if (m >= 1 && m <= 12) {
                        result.put(m, count);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get books completed by month for year: " + year, e);
        }
        return result;
    }

    @Override
    public List<AuthorStat> getTopAuthors(int limit) {
        String sql = """
            SELECT
                author,
                count(CASE WHEN status = 'COMPLETED' THEN 1 ELSE NULL END) AS completed_count,
                count(*) AS total_count,
                COALESCE(SUM(current_page), 0) AS pages_sum
            FROM books
            WHERE author IS NOT NULL AND trim(author) != ''
            GROUP BY author
            ORDER BY completed_count DESC, pages_sum DESC, total_count DESC
            LIMIT ?;
            """;

        List<AuthorStat> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuthorStat(
                            rs.getString("author"),
                            rs.getInt("completed_count"),
                            rs.getInt("total_count"),
                            rs.getInt("pages_sum")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load top authors", e);
        }
        return list;
    }

    @Override
    public List<CategoryStat> getTopCategories(int limit) {
        int totalBooksInLibrary = 0;
        String countSql = "SELECT count(*) FROM books WHERE category IS NOT NULL AND trim(category) != '';";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                totalBooksInLibrary = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count categorized books", e);
        }

        String sql = """
            SELECT
                category,
                count(CASE WHEN status = 'COMPLETED' THEN 1 ELSE NULL END) AS completed_count,
                count(*) AS total_count,
                COALESCE(SUM(current_page), 0) AS pages_sum
            FROM books
            WHERE category IS NOT NULL AND trim(category) != ''
            GROUP BY category
            ORDER BY completed_count DESC, pages_sum DESC, total_count DESC
            LIMIT ?;
            """;

        List<CategoryStat> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int totalCount = rs.getInt("total_count");
                    double pct = totalBooksInLibrary > 0 ? ((double) totalCount / totalBooksInLibrary) * 100.0 : 0.0;
                    list.add(new CategoryStat(
                            rs.getString("category"),
                            rs.getInt("completed_count"),
                            totalCount,
                            rs.getInt("pages_sum"),
                            pct
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load top categories", e);
        }
        return list;
    }

    @Override
    public AuthorStat getTopAuthorInYear(int year) {
        String sql = """
            SELECT
                author,
                count(*) AS completed_count,
                count(*) AS total_count,
                COALESCE(SUM(total_pages), 0) AS pages_sum
            FROM books
            WHERE status = 'COMPLETED' AND date_completed IS NOT NULL AND substr(date_completed, 1, 4) = ? AND author IS NOT NULL AND trim(author) != ''
            GROUP BY author
            ORDER BY completed_count DESC, pages_sum DESC
            LIMIT 1;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthorStat(
                            rs.getString("author"),
                            rs.getInt("completed_count"),
                            rs.getInt("total_count"),
                            rs.getInt("pages_sum")
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load top author for year: " + year, e);
        }
        return null;
    }

    @Override
    public CategoryStat getTopCategoryInYear(int year) {
        String sql = """
            SELECT
                category,
                count(*) AS completed_count,
                count(*) AS total_count,
                COALESCE(SUM(total_pages), 0) AS pages_sum
            FROM books
            WHERE status = 'COMPLETED' AND date_completed IS NOT NULL AND substr(date_completed, 1, 4) = ? AND category IS NOT NULL AND trim(category) != ''
            GROUP BY category
            ORDER BY completed_count DESC, pages_sum DESC
            LIMIT 1;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CategoryStat(
                            rs.getString("category"),
                            rs.getInt("completed_count"),
                            rs.getInt("total_count"),
                            rs.getInt("pages_sum"),
                            100.0
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load top category for year: " + year, e);
        }
        return null;
    }

    @Override
    public List<Integer> getDistinctCompletedYears() {
        String sql = """
            SELECT DISTINCT substr(date_completed, 1, 4) AS yr
            FROM books
            WHERE status = 'COMPLETED' AND date_completed IS NOT NULL AND length(date_completed) >= 4
            ORDER BY yr DESC;
            """;

        List<Integer> years = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String yr = rs.getString("yr");
                if (yr != null && !yr.trim().isEmpty()) {
                    try {
                        years.add(Integer.parseInt(yr.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch distinct completed years", e);
        }
        return years;
    }

    @Override
    public int countBooksCompletedInYear(int year) {
        String sql = "SELECT count(*) FROM books WHERE status = 'COMPLETED' AND date_completed IS NOT NULL AND substr(date_completed, 1, 4) = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count books completed in year: " + year, e);
        }
        return 0;
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

    @Override
    public List<DuplicateGroup> findDuplicates() {
        List<Book> allBooks = findAll();
        if (allBooks.size() < 2) {
            return Collections.emptyList();
        }

        int n = allBooks.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        java.util.function.IntUnaryOperator find = new java.util.function.IntUnaryOperator() {
            @Override
            public int applyAsInt(int i) {
                int root = i;
                while (root != parent[root]) root = parent[root];
                int curr = i;
                while (curr != root) {
                    int next = parent[curr];
                    parent[curr] = root;
                    curr = next;
                }
                return root;
            }
        };

        Map<String, List<Integer>> isbnIndex = new HashMap<>();
        Map<String, List<Integer>> titleAuthorIndex = new HashMap<>();

        for (int i = 0; i < n; i++) {
            Book b = allBooks.get(i);
            String isbn = cleanIsbn(b.getIsbn());
            if (isbn != null) {
                isbnIndex.computeIfAbsent(isbn, k -> new ArrayList<>()).add(i);
            }
            String normTitle = normalizeText(b.getTitle());
            String normAuthor = normalizeText(b.getAuthor());
            if (!normTitle.isEmpty() && !normAuthor.isEmpty()) {
                titleAuthorIndex.computeIfAbsent(normTitle + " ::: " + normAuthor, k -> new ArrayList<>()).add(i);
            }
        }

        // Connect ISBN matches
        for (List<Integer> list : isbnIndex.values()) {
            if (list.size() > 1) {
                int root = find.applyAsInt(list.get(0));
                for (int i = 1; i < list.size(); i++) {
                    int otherRoot = find.applyAsInt(list.get(i));
                    parent[otherRoot] = root;
                }
            }
        }

        // Connect Title + Author matches
        for (List<Integer> list : titleAuthorIndex.values()) {
            if (list.size() > 1) {
                int root = find.applyAsInt(list.get(0));
                for (int i = 1; i < list.size(); i++) {
                    int otherRoot = find.applyAsInt(list.get(i));
                    parent[otherRoot] = root;
                }
            }
        }

        // Group by root
        Map<Integer, List<Book>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find.applyAsInt(i);
            clusters.computeIfAbsent(root, k -> new ArrayList<>()).add(allBooks.get(i));
        }

        List<DuplicateGroup> result = new ArrayList<>();
        for (List<Book> groupBooks : clusters.values()) {
            if (groupBooks.size() > 1) {
                boolean hasSameIsbn = false;
                String matchedIsbn = null;
                for (int i = 0; i < groupBooks.size(); i++) {
                    String isbn1 = cleanIsbn(groupBooks.get(i).getIsbn());
                    for (int j = i + 1; j < groupBooks.size(); j++) {
                        String isbn2 = cleanIsbn(groupBooks.get(j).getIsbn());
                        if (isbn1 != null && isbn1.equalsIgnoreCase(isbn2)) {
                            hasSameIsbn = true;
                            matchedIsbn = isbn1;
                            break;
                        }
                    }
                    if (hasSameIsbn) break;
                }

                String reason;
                if (hasSameIsbn) {
                    reason = I18n.getOrDefault("duplicate.reason.isbn", "Matching ISBN: " + matchedIsbn, matchedIsbn);
                } else {
                    reason = I18n.getOrDefault("duplicate.reason.title_author", "Matching Title & Author");
                }
                result.add(new DuplicateGroup(reason, groupBooks));
            }
        }

        return result;
    }

    @Override
    public void resolveDuplicate(long bookToKeepId, long bookToDeleteId, boolean mergeProgress) {
        if (bookToKeepId == bookToDeleteId) return;

        Optional<Book> keepOpt = findById(bookToKeepId);
        Optional<Book> delOpt = findById(bookToDeleteId);
        if (keepOpt.isEmpty() || delOpt.isEmpty()) {
            return;
        }

        Book keep = keepOpt.get();
        Book del = delOpt.get();

        if (mergeProgress) {
            // Keep max current page
            if (del.getCurrentPage() > keep.getCurrentPage()) {
                keep.setCurrentPage(del.getCurrentPage());
            }

            // Keep status: COMPLETED takes precedence over READING, which takes precedence over NOT_STARTED
            if (del.getStatus() == ReadingStatus.COMPLETED && keep.getStatus() != ReadingStatus.COMPLETED) {
                keep.setStatus(ReadingStatus.COMPLETED);
                if (keep.getDateCompleted() == null) {
                    keep.setDateCompleted(del.getDateCompleted() != null ? del.getDateCompleted() : LocalDate.now());
                }
            } else if (del.getStatus() == ReadingStatus.READING && keep.getStatus() == ReadingStatus.NOT_STARTED) {
                keep.setStatus(ReadingStatus.READING);
                if (keep.getDateStarted() == null) {
                    keep.setDateStarted(del.getDateStarted() != null ? del.getDateStarted() : LocalDate.now());
                }
            }

            // Fill missing metadata
            if ((keep.getDescription() == null || keep.getDescription().isBlank()) && del.getDescription() != null && !del.getDescription().isBlank()) {
                keep.setDescription(del.getDescription());
            }
            if ((keep.getCoverImage() == null || keep.getCoverImage().isBlank()) && del.getCoverImage() != null && !del.getCoverImage().isBlank()) {
                keep.setCoverImage(del.getCoverImage());
            }
            if ((keep.getPublisher() == null || keep.getPublisher().isBlank()) && del.getPublisher() != null && !del.getPublisher().isBlank()) {
                keep.setPublisher(del.getPublisher());
            }
            if ((keep.getIsbn() == null || keep.getIsbn().isBlank()) && del.getIsbn() != null && !del.getIsbn().isBlank()) {
                keep.setIsbn(del.getIsbn());
            }
            if ((keep.getCategory() == null || keep.getCategory().isBlank()) && del.getCategory() != null && !del.getCategory().isBlank()) {
                keep.setCategory(del.getCategory());
            }
            if (del.isFavorite()) {
                keep.setFavorite(true);
            }
            if (del.isWishlist() && !keep.isWishlist() && keep.getStatus() == ReadingStatus.NOT_STARTED) {
                keep.setWishlist(true);
            }

            // Merge tags
            if (del.getTags() != null && !del.getTags().isBlank()) {
                Set<String> tagSet = new LinkedHashSet<>();
                if (keep.getTags() != null && !keep.getTags().isBlank()) {
                    for (String t : keep.getTags().split(",")) {
                        if (!t.trim().isEmpty()) tagSet.add(t.trim());
                    }
                }
                for (String t : del.getTags().split(",")) {
                    if (!t.trim().isEmpty()) tagSet.add(t.trim());
                }
                keep.setTags(String.join(", ", tagSet));
            }

            // Reassign reading_sessions to bookToKeepId so reading history isn't lost
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE reading_sessions SET book_id = ? WHERE book_id = ?;")) {
                stmt.setLong(1, bookToKeepId);
                stmt.setLong(2, bookToDeleteId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to migrate reading sessions when resolving duplicate", e);
            }

            // If keep has no chapters, migrate chapters from del
            try (Connection conn = databaseManager.getConnection()) {
                int keepChapters = 0;
                try (PreparedStatement checkStmt = conn.prepareStatement("SELECT count(*) FROM chapters WHERE book_id = ?;")) {
                    checkStmt.setLong(1, bookToKeepId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) keepChapters = rs.getInt(1);
                    }
                }
                if (keepChapters == 0) {
                    try (PreparedStatement migStmt = conn.prepareStatement("UPDATE chapters SET book_id = ? WHERE book_id = ?;")) {
                        migStmt.setLong(1, bookToKeepId);
                        migStmt.setLong(2, bookToDeleteId);
                        migStmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to migrate chapters when resolving duplicate", e);
            }

            update(keep);
        }

        // Delete duplicate book
        delete(bookToDeleteId);
    }

    @Override
    public void bulkMarkAsCompleted(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(bookIds.size(), "?"));
        String sql = "UPDATE books SET status = 'COMPLETED', current_page = total_pages, date_completed = COALESCE(date_completed, date('now')) WHERE id IN (" + placeholders + ");";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < bookIds.size(); i++) {
                stmt.setLong(i + 1, bookIds.get(i));
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to bulk mark books as completed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bulkMarkAsReading(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(bookIds.size(), "?"));
        String sql = "UPDATE books SET status = 'READING', date_started = COALESCE(date_started, date('now')) WHERE id IN (" + placeholders + ");";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < bookIds.size(); i++) {
                stmt.setLong(i + 1, bookIds.get(i));
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to bulk mark books as reading", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bulkDelete(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(bookIds.size(), "?"));
        String sql = "DELETE FROM books WHERE id IN (" + placeholders + ");";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < bookIds.size(); i++) {
                stmt.setLong(i + 1, bookIds.get(i));
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to bulk delete books", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bulkUpdateCategory(List<Long> bookIds, String newCategory) {
        if (bookIds == null || bookIds.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(bookIds.size(), "?"));
        String sql = "UPDATE books SET category = ? WHERE id IN (" + placeholders + ");";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, (newCategory != null && !newCategory.isBlank()) ? newCategory.trim() : null);
            for (int i = 0; i < bookIds.size(); i++) {
                stmt.setLong(i + 2, bookIds.get(i));
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to bulk update category", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bulkAddTag(List<Long> bookIds, String tag) {
        if (bookIds == null || bookIds.isEmpty() || tag == null || tag.trim().isEmpty()) return;
        String cleanTag = tag.trim();
        for (Long id : bookIds) {
            Optional<Book> opt = findById(id);
            if (opt.isPresent()) {
                Book b = opt.get();
                Set<String> tags = new LinkedHashSet<>();
                if (b.getTags() != null && !b.getTags().isBlank()) {
                    for (String t : b.getTags().split(",")) {
                        if (!t.trim().isEmpty()) tags.add(t.trim());
                    }
                }
                if (!tags.contains(cleanTag)) {
                    tags.add(cleanTag);
                    b.setTags(String.join(", ", tags));
                    update(b);
                }
            }
        }
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setTotalPages(rs.getInt("total_pages"));
        book.setTotalParts(rs.getInt("total_parts"));
        book.setCurrentPage(rs.getInt("current_page"));
        book.setStatus(ReadingStatus.fromString(rs.getString("status")));
        book.setDescription(rs.getString("description"));
        book.setCoverImage(rs.getString("cover_image"));

        String dateAddedStr = rs.getString("date_added");
        if (dateAddedStr != null && !dateAddedStr.isEmpty()) {
            try {
                book.setDateAdded(LocalDate.parse(dateAddedStr, DATE_FORMATTER));
            } catch (Exception ignored) {
                book.setDateAdded(LocalDate.now());
            }
        }

        String dateStartedStr = rs.getString("date_started");
        if (dateStartedStr != null && !dateStartedStr.isEmpty()) {
            try {
                book.setDateStarted(LocalDate.parse(dateStartedStr, DATE_FORMATTER));
            } catch (Exception ignored) {
            }
        }

        String dateCompletedStr = rs.getString("date_completed");
        if (dateCompletedStr != null && !dateCompletedStr.isEmpty()) {
            try {
                book.setDateCompleted(LocalDate.parse(dateCompletedStr, DATE_FORMATTER));
            } catch (Exception ignored) {
            }
        }

        try {
            book.setTotalChaptersCount(rs.getInt("total_chapters"));
            book.setCompletedChaptersCount(rs.getInt("completed_chapters"));
        } catch (SQLException ignored) {
        }

        try {
            book.setCategory(rs.getString("category"));
            book.setPublisher(rs.getString("publisher"));
            book.setIsbn(rs.getString("isbn"));
            book.setTags(rs.getString("tags"));
            book.setFavorite(rs.getInt("is_favorite") == 1);
            book.setWishlist(rs.getInt("is_wishlist") == 1);
        } catch (SQLException ignored) {
        }

        return book;
    }
}
