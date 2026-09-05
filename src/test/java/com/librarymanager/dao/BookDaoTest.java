package com.librarymanager.dao;

import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BookDaoTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private SqliteBookDao bookDao;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test_library.db");
        dbManager = new DatabaseManager(dbPath);
        bookDao = new SqliteBookDao(dbManager);
    }

    @Test
    @DisplayName("Test Save and FindById")
    void testSaveAndFindById() {
        Book book = new Book("Design Patterns", "Gang of Four", 395, 1);
        book.setCurrentPage(100);
        book.setStatus(ReadingStatus.READING);
        book.setDateAdded(LocalDate.now());

        Book saved = bookDao.save(book);
        assertNotNull(saved.getId());

        Optional<Book> found = bookDao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Design Patterns", found.get().getTitle());
        assertEquals("Gang of Four", found.get().getAuthor());
        assertEquals(395, found.get().getTotalPages());
        assertEquals(100, found.get().getCurrentPage());
        assertEquals(ReadingStatus.READING, found.get().getStatus());
    }

    @Test
    @DisplayName("Test Update and Delete")
    void testUpdateAndDelete() {
        Book book = new Book("Refactoring", "Martin Fowler", 448, 1);
        Book saved = bookDao.save(book);

        saved.setCurrentPage(200);
        saved.setStatus(ReadingStatus.READING);
        bookDao.update(saved);

        Book updated = bookDao.findById(saved.getId()).orElseThrow();
        assertEquals(200, updated.getCurrentPage());
        assertEquals(ReadingStatus.READING, updated.getStatus());

        bookDao.delete(saved.getId());
        assertTrue(bookDao.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("Test Search and Status Filter")
    void testSearchAndFilter() {
        Book b1 = new Book("Effective Java", "Joshua Bloch", 412, 1);
        b1.setStatus(ReadingStatus.COMPLETED);
        bookDao.save(b1);

        Book b2 = new Book("Java Concurrency in Practice", "Brian Goetz", 384, 1);
        b2.setStatus(ReadingStatus.READING);
        bookDao.save(b2);

        Book b3 = new Book("Python Crash Course", "Eric Matthes", 544, 1);
        b3.setStatus(ReadingStatus.NOT_STARTED);
        bookDao.save(b3);

        // Search by query "Java"
        List<Book> searchResults = bookDao.search("Java", null, "title", true);
        assertEquals(2, searchResults.size());

        // Filter by Status COMPLETED
        List<Book> completed = bookDao.search(null, ReadingStatus.COMPLETED, "title", true);
        assertEquals(1, completed.size());
        assertEquals("Effective Java", completed.get(0).getTitle());

        // Search + Filter together
        List<Book> filteredSearch = bookDao.search("Java", ReadingStatus.READING, "title", true);
        assertEquals(1, filteredSearch.size());
        assertEquals("Java Concurrency in Practice", filteredSearch.get(0).getTitle());
    }

    @Test
    @DisplayName("Test Library Statistics Calculation")
    void testGetStatistics() {
        Book b1 = new Book("Book 1", "Author 1", 100, 1);
        b1.setCurrentPage(100);
        b1.setStatus(ReadingStatus.COMPLETED);
        bookDao.save(b1);

        Book b2 = new Book("Book 2", "Author 2", 200, 1);
        b2.setCurrentPage(50);
        b2.setStatus(ReadingStatus.READING);
        bookDao.save(b2);

        Book b3 = new Book("Book 3", "Author 3", 100, 1);
        b3.setCurrentPage(0);
        b3.setStatus(ReadingStatus.NOT_STARTED);
        bookDao.save(b3);

        LibraryStats stats = bookDao.getStatistics();
        assertEquals(3, stats.getTotalBooks());
        assertEquals(1, stats.getCompletedCount());
        assertEquals(1, stats.getReadingCount());
        assertEquals(1, stats.getNotStartedCount());
        assertEquals(400, stats.getTotalPages());
        assertEquals(150, stats.getPagesRead());
        assertEquals(37.5, stats.getOverallProgress(), 0.01);
    }

    @Test
    @DisplayName("Test Organization Metadata Persistence and Querying")
    void testOrganizationMetadata() {
        Book book = new Book("Designing Data-Intensive Applications", "Martin Kleppmann", 616, 1);
        book.setCategory("Architecture");
        book.setPublisher("O'Reilly");
        book.setIsbn("978-1449373320");
        book.setTags("distributed-systems, databases, architecture");
        book.setFavorite(true);
        book.setWishlist(false);

        Book saved = bookDao.save(book);
        assertNotNull(saved.getId());

        Book loaded = bookDao.findById(saved.getId()).orElseThrow();
        assertEquals("Architecture", loaded.getCategory());
        assertEquals("O'Reilly", loaded.getPublisher());
        assertEquals("978-1449373320", loaded.getIsbn());
        assertEquals("distributed-systems, databases, architecture", loaded.getTags());
        assertEquals(3, loaded.getTagList().size());
        assertTrue(loaded.getTagList().contains("databases"));
        assertTrue(loaded.isFavorite());
        assertFalse(loaded.isWishlist());

        // Test toggle favorite and wishlist
        bookDao.toggleFavorite(saved.getId(), false);
        bookDao.toggleWishlist(saved.getId(), true);

        Book toggled = bookDao.findById(saved.getId()).orElseThrow();
        assertFalse(toggled.isFavorite());
        assertTrue(toggled.isWishlist());
    }

    @Test
    @DisplayName("Test FindAllCategories and FindAllTags")
    void testFindAllCategoriesAndTags() {
        Book b1 = new Book("Book 1", "Author 1", 100, 1);
        b1.setCategory("Programming");
        b1.setTags("java, oop");
        bookDao.save(b1);

        Book b2 = new Book("Book 2", "Author 2", 200, 1);
        b2.setCategory("Architecture");
        b2.setTags("oop, design-patterns");
        bookDao.save(b2);

        Book b3 = new Book("Book 3", "Author 3", 300, 1);
        b3.setCategory("Programming");
        b3.setTags("algorithms");
        bookDao.save(b3);

        List<String> categories = bookDao.findAllCategories();
        assertEquals(2, categories.size());
        assertTrue(categories.contains("Architecture"));
        assertTrue(categories.contains("Programming"));

        List<String> tags = bookDao.findAllTags();
        assertEquals(4, tags.size());
        assertTrue(tags.contains("java"));
        assertTrue(tags.contains("oop"));
        assertTrue(tags.contains("design-patterns"));
        assertTrue(tags.contains("algorithms"));
    }

    @Test
    @DisplayName("Test Multi-Criteria Search (Category, Tags, Favorite, Wishlist, Publisher, ISBN)")
    void testMultiCriteriaSearch() {
        Book b1 = new Book("Clean Code", "Robert C. Martin", 464, 1);
        b1.setCategory("Software Engineering");
        b1.setPublisher("Prentice Hall");
        b1.setIsbn("978-0132350884");
        b1.setTags("clean-code, agile");
        b1.setFavorite(true);
        bookDao.save(b1);

        Book b2 = new Book("Effective Java", "Joshua Bloch", 412, 1);
        b2.setCategory("Programming");
        b2.setPublisher("Addison-Wesley");
        b2.setIsbn("978-0134685991");
        b2.setTags("java, best-practices");
        b2.setWishlist(true);
        bookDao.save(b2);

        // Search by ISBN
        List<Book> byIsbn = bookDao.search("978-0132350884", null, "title", true);
        assertEquals(1, byIsbn.size());
        assertEquals("Clean Code", byIsbn.get(0).getTitle());

        // Search by Publisher
        List<Book> byPub = bookDao.search("Addison-Wesley", null, "title", true);
        assertEquals(1, byPub.size());
        assertEquals("Effective Java", byPub.get(0).getTitle());

        // Search by Category filter
        List<Book> byCat = bookDao.search(null, null, "Software Engineering", null, null, null, "title", true);
        assertEquals(1, byCat.size());
        assertEquals("Clean Code", byCat.get(0).getTitle());

        // Search by Tag filter
        List<Book> byTag = bookDao.search(null, null, null, "agile", null, null, "title", true);
        assertEquals(1, byTag.size());
        assertEquals("Clean Code", byTag.get(0).getTitle());

        // Search Favorites
        List<Book> favs = bookDao.search(null, null, null, null, true, null, "title", true);
        assertEquals(1, favs.size());
        assertEquals("Clean Code", favs.get(0).getTitle());

        // Search Wishlist
        List<Book> wish = bookDao.search(null, null, null, null, null, true, "title", true);
        assertEquals(1, wish.size());
        assertEquals("Effective Java", wish.get(0).getTitle());
    }
}
