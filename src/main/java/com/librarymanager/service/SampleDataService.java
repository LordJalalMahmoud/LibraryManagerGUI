package com.librarymanager.service;

import com.librarymanager.model.Book;
import com.librarymanager.model.Chapter;
import com.librarymanager.model.ReadingStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to load curated sample books on demand for testing and demoing.
 */
public class SampleDataService {
    private final BookService bookService;

    public SampleDataService(BookService bookService) {
        this.bookService = bookService;
    }

    public List<Book> createSampleBooks() {
        List<Book> list = new ArrayList<>();

        // 1. Clean Code (Reading, Favorite)
        Book b1 = new Book("Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin", 464, 1);
        b1.setStatus(ReadingStatus.READING);
        b1.setCurrentPage(334);
        b1.setDateAdded(LocalDate.now().minusDays(20));
        b1.setDateStarted(LocalDate.now().minusDays(14));
        b1.setCategory("Software Engineering");
        b1.setPublisher("Prentice Hall");
        b1.setIsbn("978-0132350884");
        b1.setTags("clean-code, craftsmanship, agile, refactoring");
        b1.setFavorite(true);
        b1.setDescription("Even bad code can function. But if code isn't clean, it can bring a development organization to its knees. Every year, countless hours and significant resources are lost because of poorly written code.");
        list.add(b1);

        // 2. Effective Java (Completed, Favorite)
        Book b2 = new Book("Effective Java (3rd Edition)", "Joshua Bloch", 412, 1);
        b2.setStatus(ReadingStatus.COMPLETED);
        b2.setCurrentPage(412);
        b2.setDateAdded(LocalDate.now().minusDays(45));
        b2.setDateStarted(LocalDate.now().minusDays(40));
        b2.setDateCompleted(LocalDate.now().minusDays(5));
        b2.setCategory("Programming");
        b2.setPublisher("Addison-Wesley Professional");
        b2.setIsbn("978-0134685991");
        b2.setTags("java, best-practices, performance, oop");
        b2.setFavorite(true);
        b2.setDescription("The definitive guide to Java platform best practices—updated for Java 7, 8, and 9. Provides essential guidance for designing robust, performant software.");
        list.add(b2);

        // 3. The Pragmatic Programmer (Reading)
        Book b3 = new Book("The Pragmatic Programmer: Your Journey To Mastery", "David Thomas, Andrew Hunt", 352, 1);
        b3.setStatus(ReadingStatus.READING);
        b3.setCurrentPage(150);
        b3.setDateAdded(LocalDate.now().minusDays(12));
        b3.setDateStarted(LocalDate.now().minusDays(8));
        b3.setCategory("Software Engineering");
        b3.setPublisher("Addison-Wesley Professional");
        b3.setIsbn("978-0135957059");
        b3.setTags("career, best-practices, pragmatism, architecture");
        b3.setDescription("Straight from the programming trenches, The Pragmatic Programmer cuts through the increasing specialization and technicalities of modern software development.");
        list.add(b3);

        // 4. Think Python (Not Started, Wishlist)
        Book b4 = new Book("Think Python: How to Think Like a Computer Scientist", "Allen B. Downey", 300, 1);
        b4.setStatus(ReadingStatus.NOT_STARTED);
        b4.setCurrentPage(0);
        b4.setDateAdded(LocalDate.now().minusDays(7));
        b4.setCategory("Computer Science");
        b4.setPublisher("O'Reilly Media");
        b4.setIsbn("978-1491939369");
        b4.setTags("python, algorithms, beginner, education");
        b4.setWishlist(true);
        b4.setDescription("An introduction to Python programming for beginners. Starts with basic concepts and is carefully designed to define all terms when first used.");
        list.add(b4);

        // 5. Design Patterns (Completed)
        Book b5 = new Book("Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", 395, 1);
        b5.setStatus(ReadingStatus.COMPLETED);
        b5.setCurrentPage(395);
        b5.setDateAdded(LocalDate.now().minusDays(60));
        b5.setDateStarted(LocalDate.now().minusDays(50));
        b5.setDateCompleted(LocalDate.now().minusDays(18));
        b5.setCategory("Architecture");
        b5.setPublisher("Addison-Wesley Professional");
        b5.setIsbn("978-0201633610");
        b5.setTags("design-patterns, oop, gang-of-four, architecture");
        b5.setDescription("Captures a wealth of experience in the design of object-oriented software. 23 patterns that solve recurring problems in OOP architecture.");
        list.add(b5);

        // 6. Refactoring (Not Started, Wishlist)
        Book b6 = new Book("Refactoring: Improving the Design of Existing Code", "Martin Fowler", 448, 1);
        b6.setStatus(ReadingStatus.NOT_STARTED);
        b6.setCurrentPage(0);
        b6.setDateAdded(LocalDate.now().minusDays(2));
        b6.setCategory("Software Engineering");
        b6.setPublisher("Addison-Wesley Professional");
        b6.setIsbn("978-0134757599");
        b6.setTags("refactoring, clean-code, design");
        b6.setWishlist(true);
        b6.setDescription("Explains the principles and best practices of refactoring, and points out where to start digging in to improve a code base.");
        list.add(b6);

        // 7. Designing Data-Intensive Applications (Reading, Favorite)
        Book b7 = new Book("Designing Data-Intensive Applications", "Martin Kleppmann", 616, 1);
        b7.setStatus(ReadingStatus.READING);
        b7.setCurrentPage(260);
        b7.setDateAdded(LocalDate.now().minusDays(30));
        b7.setDateStarted(LocalDate.now().minusDays(22));
        b7.setCategory("Architecture");
        b7.setPublisher("O'Reilly Media");
        b7.setIsbn("978-1449373320");
        b7.setTags("distributed-systems, databases, scalability, architecture");
        b7.setFavorite(true);
        b7.setDescription("Data is at the center of many challenges in system design today. Difficult issues need to be figured out, such as scalability, consistency, reliability, and maintainability.");
        list.add(b7);

        return list;
    }

    public int loadSampleData() {
        List<Book> samples = createSampleBooks();
        for (Book b : samples) {
            Book saved = bookService.addBook(b);

            // Add realistic university course reading chapters
            if (saved.getTitle().contains("Clean Code")) {
                ChapterService cs = bookService.getChapterService();
                addChapterHelper(cs, saved.getId(), 1, "Clean Code Overview", 1, 14, true, "Lecture 1 - Introduction & Bad Code Costs");
                addChapterHelper(cs, saved.getId(), 2, "Meaningful Names", 17, 36, true, "Lecture 2 - Intent-revealing names & abstractions");
                addChapterHelper(cs, saved.getId(), 3, "Functions", 37, 64, true, "Lecture 3 - Single Responsibility & argument counts");
                addChapterHelper(cs, saved.getId(), 7, "Error Handling", 103, 114, true, "Required reading for Project 1");
                addChapterHelper(cs, saved.getId(), 10, "Classes & Cohesion", 135, 152, false, "Midterm review topic");
            } else if (saved.getTitle().contains("Pragmatic")) {
                ChapterService cs = bookService.getChapterService();
                addChapterHelper(cs, saved.getId(), 1, "A Pragmatic Philosophy", 1, 34, true, "Discussion Topic: Software entropy & stone soup");
                addChapterHelper(cs, saved.getId(), 2, "A Pragmatic Approach", 35, 78, true, "DRY & Orthogonality principles");
                addChapterHelper(cs, saved.getId(), 4, "Pragmatic Paranoia", 115, 148, false, "Design by Contract & Assertions");
            }

            // Add realistic reading sessions for streak and goal tracking
            ReadingTrackerService rts = bookService.getReadingTrackerService();
            if (saved.getTitle().contains("Clean Code")) {
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(4), 1, 40, 40, 45, "Read intro chapters & meaning of clean code");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(3), 41, 100, 60, 60, "Deep dive into meaningful names & functions");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(2), 101, 180, 80, 75, "Comments and formatting conventions");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(1), 181, 260, 80, 70, "Objects, data structures & error handling");
                addSessionHelper(rts, saved.getId(), LocalDate.now(), 261, 334, 74, 65, "Clean classes & cohesive modules");
            } else if (saved.getTitle().contains("Effective Java")) {
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusMonths(2), 1, 150, 150, 120, "Generics & Enum patterns");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusMonths(1), 151, 300, 150, 110, "Lambdas, streams and exceptions");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(5), 301, 412, 112, 90, "Concurrency & serialization");
            } else if (saved.getTitle().contains("Design Patterns")) {
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusMonths(1).minusDays(5), 1, 200, 200, 150, "Creational and Structural patterns");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(18), 201, 395, 195, 140, "Behavioral patterns & recap");
            } else if (saved.getTitle().contains("Pragmatic")) {
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(8), 1, 80, 80, 60, "Philosophy & tracer bullets");
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(6), 81, 150, 70, 50, "Orthogonality & ubiquitous language");
            } else if (saved.getTitle().contains("Designing Data-Intensive")) {
                addSessionHelper(rts, saved.getId(), LocalDate.now().minusDays(2), 1, 120, 120, 90, "Reliability, scalability, and data models");
                addSessionHelper(rts, saved.getId(), LocalDate.now(), 121, 260, 140, 80, "Storage engines & transaction boundaries");
            }
        }
        return samples.size();
    }

    private void addSessionHelper(ReadingTrackerService rts, long bookId, LocalDate date, int start, int end, int pages, int duration, String notes) {
        com.librarymanager.model.ReadingSession session = new com.librarymanager.model.ReadingSession(bookId, date, start, end, pages, duration, notes);
        rts.logSession(session);
    }

    private void addChapterHelper(ChapterService cs, long bookId, int chapterNum, String title, int start, int end, boolean completed, String notes) {
        Chapter c = new Chapter(bookId, chapterNum, title, start, end);
        c.setCompleted(completed);
        c.setNotes(notes);
        cs.addChapter(c);
    }
}
