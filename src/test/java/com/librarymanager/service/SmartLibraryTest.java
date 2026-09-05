package com.librarymanager.service;

import com.librarymanager.dao.SqliteBookDao;
import com.librarymanager.dao.SqliteSavedSearchDao;
import com.librarymanager.database.DatabaseManager;
import com.librarymanager.model.Book;
import com.librarymanager.model.DuplicateGroup;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.model.SavedSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SmartLibraryTest {

    @TempDir
    Path tempDir;

    private BookService bookService;
    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("smart_library_test.db");
        dbManager = new DatabaseManager(dbPath);
        SqliteBookDao bookDao = new SqliteBookDao(dbManager);
        SqliteSavedSearchDao searchDao = new SqliteSavedSearchDao(dbManager);
        bookService = new BookService(bookDao, searchDao);
    }

    @Test
    @DisplayName("Simultaneous multi-filter search combinations")
    void testSimultaneousMultiFilterSearch() {
        Book b1 = new Book("Dune", "Frank Herbert", 412, 1);
        b1.setCategory("Sci-Fi");
        b1.setTags("classic, space, epic");
        b1.setStatus(ReadingStatus.READING);
        b1.setCurrentPage(150);
        b1.setFavorite(true);
        b1.setWishlist(false);
        bookService.addBook(b1);

        Book b2 = new Book("Children of Dune", "Frank Herbert", 444, 1);
        b2.setCategory("Sci-Fi");
        b2.setTags("space, series");
        b2.setStatus(ReadingStatus.COMPLETED);
        b2.setFavorite(true);
        b2.setWishlist(false);
        bookService.addBook(b2);

        Book b3 = new Book("Clean Code", "Robert C. Martin", 464, 1);
        b3.setCategory("Programming");
        b3.setTags("code, tech");
        b3.setStatus(ReadingStatus.READING);
        b3.setCurrentPage(50);
        b3.setFavorite(false);
        b3.setWishlist(true);
        bookService.addBook(b3);

        Book b4 = new Book("The Hobbit", "J.R.R. Tolkien", 310, 1);
        b4.setCategory("Fantasy");
        b4.setTags("adventure, classic");
        b4.setStatus(ReadingStatus.NOT_STARTED);
        b4.setFavorite(false);
        b4.setWishlist(true);
        bookService.addBook(b4);

        // Filter 1: Author "Herbert" + Status READING + Category "Sci-Fi" + Favorite = true -> Only b1
        List<Book> res1 = bookService.searchBooks(null, "Herbert", ReadingStatus.READING, "Sci-Fi", null, true, null, null, null, "title", true);
        assertEquals(1, res1.size());
        assertEquals("Dune", res1.get(0).getTitle());

        // Filter 2: Min Pages 420 + Max Pages 500 + Favorite = true -> Only b2
        List<Book> res2 = bookService.searchBooks(null, null, null, null, null, true, null, 420, 500, "total_pages", true);
        assertEquals(1, res2.size());
        assertEquals("Children of Dune", res2.get(0).getTitle());

        // Filter 3: Tag "classic" + Status NOT_STARTED -> Only b4
        List<Book> res3 = bookService.searchBooks(null, null, ReadingStatus.NOT_STARTED, null, "classic", null, null, null, null, "title", true);
        assertEquals(1, res3.size());
        assertEquals("The Hobbit", res3.get(0).getTitle());

        // Filter 4: Wishlist = true + Category "Programming" -> Only b3
        List<Book> res4 = bookService.searchBooks(null, null, null, "Programming", null, null, true, null, null, "title", true);
        assertEquals(1, res4.size());
        assertEquals("Clean Code", res4.get(0).getTitle());

        // Filter 5: Query "code" across all fields -> Only b3
        List<Book> res5 = bookService.searchBooks("code", null, null, null, null, null, null, null, null, "title", true);
        assertEquals(1, res5.size());
        assertEquals("Clean Code", res5.get(0).getTitle());
    }

    @Test
    @DisplayName("Saved searches CRUD operations")
    void testSavedSearches() {
        SavedSearch s1 = new SavedSearch("My SciFi Reading", null, "Herbert", ReadingStatus.READING, "Sci-Fi", "space", true, false, 200, 500, "title", true);
        SavedSearch saved = bookService.saveSearch(s1);
        assertNotNull(saved.getId());
        assertEquals("My SciFi Reading", saved.getName());

        List<SavedSearch> list = bookService.getAllSavedSearches();
        assertEquals(1, list.size());
        assertEquals("My SciFi Reading", list.get(0).getName());
        assertEquals(ReadingStatus.READING, list.get(0).getStatus());
        assertEquals(200, list.get(0).getMinPages());

        bookService.deleteSavedSearch(saved.getId());
        assertTrue(bookService.getAllSavedSearches().isEmpty());
    }

    @Test
    @DisplayName("Duplicate book detection by ISBN and Title+Author")
    void testDuplicateBookDetection() {
        // Pair 1: Same normalized ISBN
        Book b1 = new Book("Clean Code", "Robert C. Martin", 464, 1);
        b1.setIsbn("978-0132350884");
        bookService.addBook(b1);

        Book b2 = new Book("Clean Code (2nd Copy)", "Robert Martin", 464, 1);
        b2.setIsbn("978-0-13-235088-4");
        bookService.addBook(b2);

        // Pair 2: Matching normalized title and author (no ISBN)
        Book b3 = new Book("The Pragmatic Programmer", "Andrew Hunt", 352, 1);
        bookService.addBook(b3);

        Book b4 = new Book("The Pragmatic Programmer!", "andrew hunt", 352, 1);
        bookService.addBook(b4);

        // Unique Book
        Book b5 = new Book("Design Patterns", "Erich Gamma", 395, 1);
        b5.setIsbn("978-0201633610");
        bookService.addBook(b5);

        List<DuplicateGroup> duplicates = bookService.findDuplicates();
        assertEquals(2, duplicates.size(), "Should detect 2 duplicate groups");

        boolean foundIsbnGroup = false;
        boolean foundTitleAuthorGroup = false;

        for (DuplicateGroup g : duplicates) {
            assertEquals(2, g.getBooks().size());
            if (g.getMatchReason().contains("ISBN")) {
                foundIsbnGroup = true;
            } else if (g.getMatchReason().contains("Title") || g.getMatchReason().contains("العنوان")) {
                foundTitleAuthorGroup = true;
            }
        }

        assertTrue(foundIsbnGroup, "Expected an ISBN matching duplicate group");
        assertTrue(foundTitleAuthorGroup, "Expected a Title & Author matching duplicate group");
    }

    @Test
    @DisplayName("Duplicate book resolution with metadata and progress merge")
    void testDuplicateResolution() {
        Book b1 = new Book("Refactoring", "Martin Fowler", 448, 1);
        b1.setStatus(ReadingStatus.READING);
        b1.setCurrentPage(50);
        b1.setDescription("Primary copy description");
        b1.setIsbn("978-0201485677");
        b1 = bookService.addBook(b1);

        Book b2 = new Book("Refactoring", "Martin Fowler", 448, 1);
        b2.setStatus(ReadingStatus.COMPLETED);
        b2.setCurrentPage(448);
        b2.setCategory("Software Engineering");
        b2.setTags("code-quality, architecture");
        b2.setPublisher("Addison-Wesley");
        b2.setIsbn("978-0201485677");
        b2.setFavorite(true);
        b2 = bookService.addBook(b2);

        // Resolve: keep b1, delete b2, merge progress & metadata
        bookService.resolveDuplicate(b1.getId(), b2.getId(), true);

        // Verify b2 is deleted
        Optional<Book> delOpt = bookService.getBookById(b2.getId());
        assertTrue(delOpt.isEmpty(), "Duplicate book should be deleted");

        // Verify b1 updated with merged values
        Optional<Book> keptOpt = bookService.getBookById(b1.getId());
        assertTrue(keptOpt.isPresent());
        Book kept = keptOpt.get();

        assertEquals(448, kept.getCurrentPage(), "Should have merged higher page count");
        assertEquals(ReadingStatus.COMPLETED, kept.getStatus(), "Should have merged COMPLETED status");
        assertEquals("Primary copy description", kept.getDescription(), "Should retain existing description");
        assertEquals("Software Engineering", kept.getCategory(), "Should adopt category from duplicate");
        assertEquals("Addison-Wesley", kept.getPublisher(), "Should adopt publisher from duplicate");
        assertTrue(kept.isFavorite(), "Should adopt favorite status");
        assertTrue(kept.getTags().contains("code-quality"));
    }

    @Test
    @DisplayName("Bulk operations: mark completed, update category, add tag, delete")
    void testBulkOperations() {
        Book b1 = new Book("Book 1", "Author A", 100, 1);
        b1 = bookService.addBook(b1);

        Book b2 = new Book("Book 2", "Author B", 200, 1);
        b2 = bookService.addBook(b2);

        Book b3 = new Book("Book 3", "Author C", 300, 1);
        b3 = bookService.addBook(b3);

        List<Long> targetIds = List.of(b1.getId(), b2.getId());

        // 1. Bulk Update Category
        bookService.bulkUpdateCategory(targetIds, "Classics");
        assertEquals("Classics", bookService.getBookById(b1.getId()).get().getCategory());
        assertEquals("Classics", bookService.getBookById(b2.getId()).get().getCategory());
        assertNull(bookService.getBookById(b3.getId()).get().getCategory());

        // 2. Bulk Add Tag
        bookService.bulkAddTag(targetIds, "must-read");
        assertTrue(bookService.getBookById(b1.getId()).get().getTags().contains("must-read"));
        assertTrue(bookService.getBookById(b2.getId()).get().getTags().contains("must-read"));
        assertNull(bookService.getBookById(b3.getId()).get().getTags());

        // 3. Bulk Mark As Completed
        bookService.bulkMarkAsCompleted(targetIds);
        Book updatedB1 = bookService.getBookById(b1.getId()).get();
        assertEquals(ReadingStatus.COMPLETED, updatedB1.getStatus());
        assertEquals(100, updatedB1.getCurrentPage());

        Book updatedB2 = bookService.getBookById(b2.getId()).get();
        assertEquals(ReadingStatus.COMPLETED, updatedB2.getStatus());
        assertEquals(200, updatedB2.getCurrentPage());

        // 4. Bulk Delete
        bookService.bulkDelete(targetIds);
        assertTrue(bookService.getBookById(b1.getId()).isEmpty());
        assertTrue(bookService.getBookById(b2.getId()).isEmpty());
        assertTrue(bookService.getBookById(b3.getId()).isPresent(), "Book 3 should not be deleted");
    }
}
