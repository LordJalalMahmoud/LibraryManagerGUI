package com.librarymanager.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Encapsulates a user-defined saved search filter configuration.
 */
public class SavedSearch {
    private Long id;
    private String name;
    private String query;
    private String author;
    private ReadingStatus status;
    private String category;
    private String tag;
    private Boolean isFavorite;
    private Boolean isWishlist;
    private Integer minPages;
    private Integer maxPages;
    private String sortBy;
    private boolean ascending;
    private LocalDate dateCreated = LocalDate.now();

    public SavedSearch() {
    }

    public SavedSearch(String name, String query, String author, ReadingStatus status,
                       String category, String tag, Boolean isFavorite, Boolean isWishlist,
                       Integer minPages, Integer maxPages, String sortBy, boolean ascending) {
        this.name = name;
        this.query = query;
        this.author = author;
        this.status = status;
        this.category = category;
        this.tag = tag;
        this.isFavorite = isFavorite;
        this.isWishlist = isWishlist;
        this.minPages = minPages;
        this.maxPages = maxPages;
        this.sortBy = sortBy;
        this.ascending = ascending;
        this.dateCreated = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
    }

    public Boolean getWishlist() {
        return isWishlist;
    }

    public void setWishlist(Boolean wishlist) {
        isWishlist = wishlist;
    }

    public Integer getMinPages() {
        return minPages;
    }

    public void setMinPages(Integer minPages) {
        this.minPages = minPages;
    }

    public Integer getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(Integer maxPages) {
        this.maxPages = maxPages;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public LocalDate getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDate dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedSearch that = (SavedSearch) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name != null ? name : "SavedSearch #" + id;
    }
}
