package com.librarymanager.component;

import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modern card representing a single book in the library grid, supporting i18n & RTL.
 */
public class BookCardComponent extends VBox {
    private static final Logger LOGGER = Logger.getLogger(BookCardComponent.class.getName());

    private final Book book;
    private final ProgressBar progressBar;
    private final Label progressTextLabel;
    private final Label statusChip;

    public BookCardComponent(Book book,
                             Consumer<Book> onOpen,
                             Consumer<Book> onEdit,
                             BiConsumer<BookCardComponent, Book> onDelete,
                             Consumer<Book> onQuickAdvance) {
        this(book, onOpen, onEdit, onDelete, onQuickAdvance, null);
    }

    public BookCardComponent(Book book,
                             Consumer<Book> onOpen,
                             Consumer<Book> onEdit,
                             BiConsumer<BookCardComponent, Book> onDelete,
                             Consumer<Book> onQuickAdvance,
                             Consumer<Book> onToggleFavorite) {
        this.book = book;
        getStyleClass().add("book-card");
        setSpacing(10);
        setPadding(new Insets(14));
        setPrefWidth(240);
        setMaxWidth(300);
        setMinWidth(220);
        setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // 1. Cover area
        StackPane coverContainer = createCoverView(book);
        coverContainer.setOnMouseClicked(e -> {
            if (onOpen != null) onOpen.accept(book);
        });

        // Quick favorite toggle button on cover
        Button favBtn = new Button();
        favBtn.getStyleClass().addAll("btn", "btn-icon", "btn-sm");
        SVGPath favIcon = IconUtil.createIcon(IconUtil.IconType.HEART, 14);
        if (book.isFavorite()) {
            favIcon.setStyle("-fx-fill: #ef4444;");
            favBtn.setTooltip(new Tooltip(I18n.get("book.favorite.remove_tooltip")));
        } else {
            favIcon.setStyle("-fx-fill: rgba(255,255,255,0.7);");
            favBtn.setTooltip(new Tooltip(I18n.get("book.favorite.tooltip")));
        }
        favBtn.setGraphic(favIcon);
        favBtn.setStyle("-fx-background-color: rgba(0, 0, 0, 0.45); -fx-background-radius: 50%; -fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px; -fx-cursor: hand; -fx-padding: 0;");
        StackPane.setAlignment(favBtn, I18n.isRTL() ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
        StackPane.setMargin(favBtn, new Insets(8));
        favBtn.setOnAction(e -> {
            e.consume();
            if (onToggleFavorite != null) {
                onToggleFavorite.accept(book);
            }
        });
        coverContainer.getChildren().add(favBtn);

        // 2. Status Badge Chip & Metadata Chips
        statusChip = new Label(book.getStatus().getDisplayName());
        statusChip.getStyleClass().addAll("badge-chip", book.getStatus().getStyleClass());

        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(I18n.isRTL() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        badgeRow.getChildren().add(statusChip);

        if (book.getCategory() != null && !book.getCategory().trim().isEmpty()) {
            Label catBadge = new Label(book.getCategory().trim());
            catBadge.setStyle("-fx-background-color: -surface-border; -fx-text-fill: -text-main; -fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 2 7 2 7; -fx-background-radius: 12px;");
            badgeRow.getChildren().add(catBadge);
        }

        if (book.isWishlist()) {
            Label wishBadge = new Label("🌟");
            wishBadge.setTooltip(new Tooltip(I18n.get("book.wishlist")));
            wishBadge.setStyle("-fx-font-size: 11px;");
            badgeRow.getChildren().add(wishBadge);
        }

        if (book.hasChapters()) {
            Label chapterBadge = new Label(I18n.get("chapters.badge", book.getCompletedChaptersCount(), book.getTotalChaptersCount()));
            chapterBadge.setStyle("-fx-background-color: -accent-subtle; -fx-text-fill: -accent-primary; -fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 2 7 2 7; -fx-background-radius: 12px;");
            badgeRow.getChildren().add(chapterBadge);
        }

        // 3. Title & Author
        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxHeight(48);
        titleLabel.setTooltip(new Tooltip(book.getTitle()));

        Label authorLabel = new Label(I18n.get("book.card.by", book.getAuthor()));
        authorLabel.getStyleClass().add("book-card-author");
        authorLabel.setTooltip(new Tooltip(book.getAuthor()));

        // 4. Tags Chip Row
        HBox tagBox = null;
        List<String> tagList = book.getTagList();
        if (!tagList.isEmpty()) {
            tagBox = new HBox(4);
            tagBox.setAlignment(I18n.isRTL() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            for (int i = 0; i < Math.min(3, tagList.size()); i++) {
                Label tagChip = new Label("#" + tagList.get(i));
                tagChip.setStyle("-fx-font-size: 10px; -fx-text-fill: -accent-primary; -fx-background-color: -accent-subtle; -fx-padding: 2 6 2 6; -fx-background-radius: 6px;");
                tagBox.getChildren().add(tagChip);
            }
            if (tagList.size() > 3) {
                Label moreChip = new Label("+" + (tagList.size() - 3));
                moreChip.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-muted;");
                tagBox.getChildren().add(moreChip);
            }
        }

        // 5. Meta info (Pages, Parts)
        HBox metaRow = new HBox();
        metaRow.setSpacing(10);
        metaRow.setAlignment(I18n.isRTL() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label pagesLabel = new Label(I18n.get("book.card.pages", book.getTotalPages()));
        pagesLabel.getStyleClass().add("book-card-meta");

        String partsStr = book.getTotalParts() > 1
                ? I18n.get("book.card.parts", book.getTotalParts())
                : I18n.get("book.card.part_single");
        Label partsLabel = new Label(partsStr);
        partsLabel.getStyleClass().add("book-card-meta");

        metaRow.getChildren().addAll(pagesLabel, new Label("•"), partsLabel);

        // 6. Reading Progress Section
        VBox progressSection = new VBox(6);
        HBox progressHeader = new HBox();
        progressHeader.setAlignment(I18n.isRTL() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label pageRatioLabel = new Label(I18n.get("book.card.page_ratio", book.getCurrentPage(), book.getTotalPages()));
        pageRatioLabel.getStyleClass().add("progress-text-secondary");
        HBox.setHgrow(pageRatioLabel, Priority.ALWAYS);

        progressTextLabel = new Label(book.getFormattedProgress());
        progressTextLabel.getStyleClass().add("progress-text-bold");

        if (I18n.isRTL()) {
            progressHeader.getChildren().addAll(progressTextLabel, pageRatioLabel);
        } else {
            progressHeader.getChildren().addAll(pageRatioLabel, progressTextLabel);
        }

        progressBar = new ProgressBar(book.getProgressPercentage() / 100.0);
        progressBar.getStyleClass().add("book-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        progressSection.getChildren().addAll(progressHeader, progressBar);

        // 7. Action Bar
        HBox actionBar = new HBox(8);
        actionBar.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        actionBar.setPadding(new Insets(4, 0, 0, 0));

        Button openBtn = new Button(I18n.get("book.card.view"));
        openBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        openBtn.setOnAction(e -> {
            if (onOpen != null) onOpen.accept(book);
        });
        HBox.setHgrow(openBtn, Priority.ALWAYS);
        openBtn.setMaxWidth(Double.MAX_VALUE);

        if (book.getStatus() != ReadingStatus.COMPLETED && onQuickAdvance != null) {
            Button quickBtn = new Button("+10");
            quickBtn.getStyleClass().addAll("btn", "btn-outline", "btn-sm");
            quickBtn.setTooltip(new Tooltip(I18n.get("book.card.quick_advance")));
            quickBtn.setOnAction(e -> onQuickAdvance.accept(book));
            actionBar.getChildren().add(quickBtn);
        }

        Button editBtn = new Button();
        editBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.EDIT, 13));
        editBtn.getStyleClass().addAll("btn", "btn-icon", "btn-sm");
        editBtn.setTooltip(new Tooltip(I18n.get("book.card.edit")));
        editBtn.setOnAction(e -> {
            if (onEdit != null) onEdit.accept(book);
        });

        Button deleteBtn = new Button();
        deleteBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.TRASH, 13));
        deleteBtn.getStyleClass().addAll("btn", "btn-icon-danger", "btn-sm");
        deleteBtn.setTooltip(new Tooltip(I18n.get("book.card.delete")));
        deleteBtn.setOnAction(e -> {
            if (onDelete != null) onDelete.accept(this, book);
        });

        actionBar.getChildren().addAll(openBtn, editBtn, deleteBtn);

        // Assembly
        getChildren().addAll(coverContainer, badgeRow, titleLabel, authorLabel);
        if (tagBox != null) {
            getChildren().add(tagBox);
        }
        getChildren().addAll(metaRow, progressSection, actionBar);

        AnimationUtil.addCardHover(this);
    }

    private StackPane createCoverView(Book book) {
        StackPane container = new StackPane();
        container.getStyleClass().add("book-cover-container");
        container.setPrefHeight(160);
        container.setMinHeight(160);
        container.setMaxHeight(160);
        container.setMaxWidth(Double.MAX_VALUE);

        boolean imageLoaded = false;
        if (book.getCoverImage() != null && !book.getCoverImage().trim().isEmpty()) {
            try {
                File file = new File(book.getCoverImage());
                Image img;
                if (file.exists()) {
                    img = new Image(file.toURI().toString(), 240, 160, true, true);
                } else if (book.getCoverImage().startsWith("http://") || book.getCoverImage().startsWith("https://")) {
                    img = new Image(book.getCoverImage(), 240, 160, true, true, true);
                } else {
                    img = null;
                }
                if (img != null && !img.isError()) {
                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(220);
                    imageView.setFitHeight(150);
                    imageView.setPreserveRatio(true);
                    imageView.getStyleClass().add("book-cover-image");
                    container.getChildren().add(imageView);
                    imageLoaded = true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to load cover image: " + book.getCoverImage(), e);
            }
        }

        if (!imageLoaded) {
            VBox fallback = new VBox(6);
            fallback.setAlignment(Pos.CENTER);
            fallback.setPadding(new Insets(12));
            fallback.getStyleClass().addAll("book-cover-placeholder", getCoverGradientClass(book.getTitle()));

            SVGPath bookIcon = IconUtil.createIcon(IconUtil.IconType.PAGES, 28);
            bookIcon.getStyleClass().add("cover-placeholder-icon");

            Label coverTitle = new Label(book.getTitle());
            coverTitle.getStyleClass().add("cover-placeholder-title");
            coverTitle.setWrapText(true);
            coverTitle.setAlignment(Pos.CENTER);
            coverTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            coverTitle.setMaxHeight(50);

            Label coverAuthor = new Label(book.getAuthor());
            coverAuthor.getStyleClass().add("cover-placeholder-author");
            coverAuthor.setMaxWidth(200);
            coverAuthor.setAlignment(Pos.CENTER);

            fallback.getChildren().addAll(bookIcon, coverTitle, coverAuthor);
            container.getChildren().add(fallback);
        }

        return container;
    }

    private String getCoverGradientClass(String title) {
        if (title == null || title.isEmpty()) return "cover-gradient-1";
        int hash = Math.abs(title.hashCode()) % 5;
        return "cover-gradient-" + (hash + 1);
    }

    public Book getBook() {
        return book;
    }
}
