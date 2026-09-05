package com.librarymanager.service;

import com.librarymanager.dao.ChapterDao;
import com.librarymanager.dao.SqliteChapterDao;
import com.librarymanager.model.Chapter;

import java.util.List;
import java.util.Optional;

/**
 * Service managing chapter assignments, validation, and reading progress.
 */
public class ChapterService {
    private final ChapterDao chapterDao;

    public ChapterService() {
        this(new SqliteChapterDao());
    }

    public ChapterService(ChapterDao chapterDao) {
        this.chapterDao = chapterDao;
    }

    public void validateChapter(Chapter chapter) {
        if (chapter == null) {
            throw new IllegalArgumentException("Chapter cannot be null");
        }
        if (chapter.getTitle() == null || chapter.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Chapter title is required.");
        }
        if (chapter.getChapterNumber() < 1) {
            throw new IllegalArgumentException("Chapter number must be at least 1.");
        }
        if (chapter.getStartPage() < 0 || chapter.getEndPage() < 0) {
            throw new IllegalArgumentException("Page numbers cannot be negative.");
        }
        if (chapter.getEndPage() > 0 && chapter.getEndPage() < chapter.getStartPage()) {
            throw new IllegalArgumentException("End page cannot be less than start page.");
        }
    }

    public Chapter addChapter(Chapter chapter) {
        validateChapter(chapter);
        return chapterDao.save(chapter);
    }

    public void updateChapter(Chapter chapter) {
        validateChapter(chapter);
        chapterDao.update(chapter);
    }

    public void deleteChapter(long id) {
        chapterDao.delete(id);
    }

    public Optional<Chapter> getChapterById(long id) {
        return chapterDao.findById(id);
    }

    public List<Chapter> getChaptersByBookId(long bookId) {
        return chapterDao.findByBookId(bookId);
    }

    public void toggleChapter(long chapterId, boolean isCompleted) {
        chapterDao.toggleCompletion(chapterId, isCompleted);
    }

    public int getCompletedChaptersCount(long bookId) {
        return chapterDao.countCompletedByBookId(bookId);
    }

    public int getTotalChaptersCount(long bookId) {
        return chapterDao.countTotalByBookId(bookId);
    }

    public double getChapterProgressPercentage(long bookId) {
        int total = getTotalChaptersCount(bookId);
        if (total == 0) return 0.0;
        int completed = getCompletedChaptersCount(bookId);
        return ((double) completed / (double) total) * 100.0;
    }
}
