package com.librarymanager.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a book in the personal library with full organization metadata.
 */
public class Book {
    private Long id;
    private String title;
    private String author;
    private int totalPages;
    private int totalParts = 1;
    private int currentPage;
    private ReadingStatus status = ReadingStatus.NOT_STARTED;
    private LocalDate dateAdded;
    private LocalDate dateStarted;
    private LocalDate dateCompleted;
    private String description;
    private String coverImage;
    private int completedChaptersCount;
    private int totalChaptersCount;

    // Organization & Discovery Metadata
    private String category;
    private String publisher;
    private String isbn;
    private String tags;
    private boolean favorite;
    private boolean wishlist;

    public Book() {
        this.dateAdded = LocalDate.now();
    }

    public Book(String title, String author, int totalPages, int totalParts) {
        this();
        this.title = title;
        this.author = author;
        this.totalPages = totalPages;
        this.totalParts = Math.max(1, totalParts);
        this.currentPage = 0;
        this.status = ReadingStatus.NOT_STARTED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : "";
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author != null ? author.trim() : "";
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(int totalParts) {
        this.totalParts = Math.max(1, totalParts);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status != null ? status : ReadingStatus.NOT_STARTED;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDate getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(LocalDate dateStarted) {
        this.dateStarted = dateStarted;
    }

    public LocalDate getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(LocalDate dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public int getCompletedChaptersCount() {
        return completedChaptersCount;
    }

    public void setCompletedChaptersCount(int completedChaptersCount) {
        this.completedChaptersCount = completedChaptersCount;
    }

    public int getTotalChaptersCount() {
        return totalChaptersCount;
    }

    public void setTotalChaptersCount(int totalChaptersCount) {
        this.totalChaptersCount = totalChaptersCount;
    }

    public boolean hasChapters() {
        return totalChaptersCount > 0;
    }

    public String getChapterProgressString() {
        return completedChaptersCount + " / " + totalChaptersCount;
    }

    public double getChapterProgressPercentage() {
        if (totalChaptersCount <= 0) return 0.0;
        return ((double) completedChaptersCount / (double) totalChaptersCount) * 100.0;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim() : null;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher != null ? publisher.trim() : null;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn != null ? isbn.trim() : null;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags != null ? tags.trim() : null;
    }

    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isWishlist() {
        return wishlist;
    }

    public void setWishlist(boolean wishlist) {
        this.wishlist = wishlist;
    }

    /**
     * Calculates the reading progress percentage (0.0 to 100.0).
     */
    public double getProgressPercentage() {
        if (totalPages <= 0) {
            return 0.0;
        }
        double pct = ((double) currentPage / (double) totalPages) * 100.0;
        if (pct < 0.0) return 0.0;
        if (pct > 100.0) return 100.0;
        return pct;
    }

    /**
     * Formatted string for progress, e.g. "30%".
     */
    public String getFormattedProgress() {
        return String.format("%.0f%%", getProgressPercentage());
    }

    /**
     * Ratio string, e.g. "Page 150 / 500".
     */
    public String getProgressRatioString() {
        return String.format("Page %d / %d", currentPage, totalPages);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", totalPages=" + totalPages +
                ", currentPage=" + currentPage +
                ", status=" + status +
                '}';
    }
}
