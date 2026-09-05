package com.librarymanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    @DisplayName("Test progress percentage calculation")
    void testProgressCalculation() {
        Book book = new Book("Clean Code", "Robert C. Martin", 500, 1);
        book.setCurrentPage(150);

        assertEquals(30.0, book.getProgressPercentage(), 0.001);
        assertEquals("30%", book.getFormattedProgress());
        assertEquals("Page 150 / 500", book.getProgressRatioString());
    }

    @Test
    @DisplayName("Test progress boundary clamping")
    void testProgressBoundaryClamping() {
        Book book = new Book("Test Book", "Author", 200, 1);

        book.setCurrentPage(0);
        assertEquals(0.0, book.getProgressPercentage(), 0.001);
        assertEquals("0%", book.getFormattedProgress());

        book.setCurrentPage(200);
        assertEquals(100.0, book.getProgressPercentage(), 0.001);
        assertEquals("100%", book.getFormattedProgress());

        // Clamping if totalPages <= 0
        Book invalidPagesBook = new Book("Invalid", "Author", 0, 1);
        assertEquals(0.0, invalidPagesBook.getProgressPercentage(), 0.001);
    }

    @Test
    @DisplayName("Test ReadingStatus fromString parsing")
    void testReadingStatusParsing() {
        assertEquals(ReadingStatus.NOT_STARTED, ReadingStatus.fromString("NOT_STARTED"));
        assertEquals(ReadingStatus.NOT_STARTED, ReadingStatus.fromString("Not Started"));
        assertEquals(ReadingStatus.READING, ReadingStatus.fromString("READING"));
        assertEquals(ReadingStatus.READING, ReadingStatus.fromString("Reading"));
        assertEquals(ReadingStatus.COMPLETED, ReadingStatus.fromString("COMPLETED"));
        assertEquals(ReadingStatus.COMPLETED, ReadingStatus.fromString("Completed"));
        assertEquals(ReadingStatus.NOT_STARTED, ReadingStatus.fromString("Unknown"));
        assertEquals(ReadingStatus.NOT_STARTED, ReadingStatus.fromString(null));
    }
}
