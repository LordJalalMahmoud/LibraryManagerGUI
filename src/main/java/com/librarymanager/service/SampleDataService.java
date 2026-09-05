package com.librarymanager.service;

import com.librarymanager.model.Book;
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

        // 1. Clean Code (Reading)
        Book b1 = new Book("Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin", 464, 1);
        b1.setStatus(ReadingStatus.READING);
        b1.setCurrentPage(334);
        b1.setDateAdded(LocalDate.now().minusDays(20));
        b1.setDateStarted(LocalDate.now().minusDays(14));
        b1.setDescription("Even bad code can function. But if code isn't clean, it can bring a development organization to its knees. Every year, countless hours and significant resources are lost because of poorly written code.");
        list.add(b1);

        // 2. Effective Java (Completed)
        Book b2 = new Book("Effective Java (3rd Edition)", "Joshua Bloch", 412, 1);
        b2.setStatus(ReadingStatus.COMPLETED);
        b2.setCurrentPage(412);
        b2.setDateAdded(LocalDate.now().minusDays(45));
        b2.setDateStarted(LocalDate.now().minusDays(40));
        b2.setDateCompleted(LocalDate.now().minusDays(5));
        b2.setDescription("The definitive guide to Java platform best practices—updated for Java 7, 8, and 9. Provides essential guidance for designing robust, performant software.");
        list.add(b2);

        // 3. The Pragmatic Programmer (Reading)
        Book b3 = new Book("The Pragmatic Programmer: Your Journey To Mastery", "David Thomas, Andrew Hunt", 352, 1);
        b3.setStatus(ReadingStatus.READING);
        b3.setCurrentPage(150);
        b3.setDateAdded(LocalDate.now().minusDays(12));
        b3.setDateStarted(LocalDate.now().minusDays(8));
        b3.setDescription("Straight from the programming trenches, The Pragmatic Programmer cuts through the increasing specialization and technicalities of modern software development.");
        list.add(b3);

        // 4. Think Python (Not Started)
        Book b4 = new Book("Think Python: How to Think Like a Computer Scientist", "Allen B. Downey", 300, 1);
        b4.setStatus(ReadingStatus.NOT_STARTED);
        b4.setCurrentPage(0);
        b4.setDateAdded(LocalDate.now().minusDays(7));
        b4.setDescription("An introduction to Python programming for beginners. Starts with basic concepts and is carefully designed to define all terms when first used.");
        list.add(b4);

        // 5. Design Patterns (Completed)
        Book b5 = new Book("Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", 395, 1);
        b5.setStatus(ReadingStatus.COMPLETED);
        b5.setCurrentPage(395);
        b5.setDateAdded(LocalDate.now().minusDays(60));
        b5.setDateStarted(LocalDate.now().minusDays(50));
        b5.setDateCompleted(LocalDate.now().minusDays(18));
        b5.setDescription("Captures a wealth of experience in the design of object-oriented software. 23 patterns that solve recurring problems in OOP architecture.");
        list.add(b5);

        // 6. Refactoring (Not Started)
        Book b6 = new Book("Refactoring: Improving the Design of Existing Code", "Martin Fowler", 448, 1);
        b6.setStatus(ReadingStatus.NOT_STARTED);
        b6.setCurrentPage(0);
        b6.setDateAdded(LocalDate.now().minusDays(2));
        b6.setDescription("Explains the principles and best practices of refactoring, and points out where to start digging in to improve a code base.");
        list.add(b6);

        // 7. Designing Data-Intensive Applications (Reading)
        Book b7 = new Book("Designing Data-Intensive Applications", "Martin Kleppmann", 616, 1);
        b7.setStatus(ReadingStatus.READING);
        b7.setCurrentPage(260);
        b7.setDateAdded(LocalDate.now().minusDays(30));
        b7.setDateStarted(LocalDate.now().minusDays(22));
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
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 1, "Clean Code Overview", 1, 14) {{ setCompleted(true); setNotes("Lecture 1 - Introduction & Bad Code Costs"); }});
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 2, "Meaningful Names", 17, 36) {{ setCompleted(true); setNotes("Lecture 2 - Intent-revealing names & abstractions"); }});
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 3, "Functions", 37, 64) {{ setCompleted(true); setNotes("Lecture 3 - Single Responsibility & argument counts"); }});
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 7, "Error Handling", 103, 114) {{ setCompleted(true); setNotes("Required reading for Project 1"); }});
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 10, "Classes & Cohesion", 135, 152) {{ setCompleted(false); setNotes("Midterm review topic"); }});
            } else if (saved.getTitle().contains("Pragmatic")) {
                ChapterService cs = bookService.getChapterService();
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 1, "A Pragmatic Philosophy", 1, 34) {{ setCompleted(true); setNotes("Discussion Topic: Software entropy & stone soup"); }});
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 2, "A Pragmatic Approach", 35, 78) {{ setCompleted(true); setNotes("DRY & Orthogonality principles"); }});
                cs.addChapter(new com.librarymanager.model.Chapter(saved.getId(), 4, "Pragmatic Paranoia", 115, 148) {{ setCompleted(false); setNotes("Design by Contract & Assertions"); }});
            }
        }
        return samples.size();
    }
}
