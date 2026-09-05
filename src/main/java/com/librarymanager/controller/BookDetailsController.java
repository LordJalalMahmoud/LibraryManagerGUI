package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.Chapter;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BookService;
import com.librarymanager.service.ChapterService;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.DateUtil;
import com.librarymanager.util.DialogUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.util.List;

/**
 * Dedicated, visually impressive book details and reading experience screen
 * with university course chapter assignments tracking, i18n & RTL.
 */
public class BookDetailsController {

    private final MainController mainController;
    private final BookService bookService;
    private final ChapterService chapterService;
    private final SettingsService settingsService;
    private Book book;

    private final ScrollPane rootScrollPane;
    private final VBox contentBox;

    // Dynamic UI elements
    private Label statusBadge;
    private Label pageProgressText;
    private Label percentText;
    private ProgressBar progressBar;
    private Spinner<Integer> pageSpinner;
    private Label dateStartedVal;
    private Label dateCompletedVal;

    // Chapter UI elements
    private VBox chaptersCard;
    private VBox chaptersListContainer;
    private Label chapterSummaryLabel;
    private ProgressBar chapterProgressBar;

    public BookDetailsController(MainController mainController, BookService bookService, SettingsService settingsService, Book book) {
        this.mainController = mainController;
        this.bookService = bookService;
        this.chapterService = bookService.getChapterService();
        this.settingsService = settingsService;
        this.book = book;

        contentBox = new VBox(24);
        contentBox.setPadding(new Insets(0, 10, 30, 0));
        contentBox.setFillWidth(true);

        rootScrollPane = new ScrollPane(contentBox);
        rootScrollPane.setFitToWidth(true);
        rootScrollPane.getStyleClass().add("scroll-pane");
        rootScrollPane.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        buildView();
    }

    public Node getView() {
        return rootScrollPane;
    }

    private void buildView() {
        // 1. Top action header
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button(I18n.get("details.back"));
        backBtn.getStyleClass().addAll("btn", "btn-secondary");
        backBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.BACK, 14));
        backBtn.setGraphicTextGap(8);
        backBtn.setOnAction(e -> mainController.navigateToLibrary(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button(I18n.get("details.edit"));
        editBtn.getStyleClass().addAll("btn", "btn-secondary");
        editBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.EDIT, 14));
        editBtn.setGraphicTextGap(8);
        editBtn.setOnAction(e -> {
            mainController.openBookFormDialog(book);
            refreshData();
        });

        Button deleteBtn = new Button(I18n.get("details.delete"));
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.TRASH, 14));
        deleteBtn.setGraphicTextGap(8);
        deleteBtn.setOnAction(e -> handleDelete());

        topBar.getChildren().addAll(backBtn, spacer, editBtn, deleteBtn);
        contentBox.getChildren().add(topBar);

        // 2. Main Detail Card
        HBox mainCard = new HBox(28);
        mainCard.getStyleClass().add("stat-card");
        mainCard.setPadding(new Insets(28));
        mainCard.setAlignment(Pos.TOP_LEFT);

        // Left column: Cover preview
        StackPane coverPane = buildCoverNode();
        coverPane.setPrefWidth(220);
        coverPane.setMinWidth(200);
        coverPane.setMaxWidth(240);

        // Right column: Info & Reading Progress
        VBox rightCol = new VBox(18);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Title and Author
        VBox titles = new VBox(6);
        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        titleLabel.setWrapText(true);

        Label authorLabel = new Label(I18n.get("book.card.by", book.getAuthor()));
        authorLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 500; -fx-text-fill: -text-muted;");

        // Status chip
        statusBadge = new Label(book.getStatus().getDisplayName());
        statusBadge.getStyleClass().addAll("badge-chip", book.getStatus().getStyleClass());

        titles.getChildren().addAll(statusBadge, titleLabel, authorLabel);

        // Interactive Reading Experience Card
        VBox readingExperienceBox = buildReadingExperienceCard();

        // Metadata grid (Dates, Parts, etc.)
        GridPane metaGrid = new GridPane();
        metaGrid.setHgap(24);
        metaGrid.setVgap(12);
        metaGrid.setPadding(new Insets(12, 0, 0, 0));

        addMetaItem(metaGrid, 0, 0, I18n.get("details.total_pages"), String.valueOf(book.getTotalPages()));
        addMetaItem(metaGrid, 1, 0, I18n.get("details.volumes"), String.valueOf(book.getTotalParts()));
        addMetaItem(metaGrid, 0, 1, I18n.get("details.date_added"), DateUtil.format(book.getDateAdded()));

        dateStartedVal = new Label(DateUtil.format(book.getDateStarted()));
        dateStartedVal.getStyleClass().add("stat-value");
        dateStartedVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        VBox startedBox = new VBox(2, new Label(I18n.get("details.date_started")) {{ getStyleClass().add("stat-subtext"); }}, dateStartedVal);
        metaGrid.add(startedBox, 1, 1);

        dateCompletedVal = new Label(DateUtil.format(book.getDateCompleted()));
        dateCompletedVal.getStyleClass().add("stat-value");
        dateCompletedVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        VBox completedBox = new VBox(2, new Label(I18n.get("details.date_completed")) {{ getStyleClass().add("stat-subtext"); }}, dateCompletedVal);
        metaGrid.add(completedBox, 0, 2);

        // Description / Notes
        VBox descBox = new VBox(6);
        Label descHeader = new Label(I18n.get("details.notes"));
        descHeader.getStyleClass().add("stat-title");
        descHeader.setStyle("-fx-font-weight: 700;");

        String desc = (book.getDescription() != null && !book.getDescription().trim().isEmpty())
                ? book.getDescription()
                : I18n.get("details.no_notes");
        Label descContent = new Label(desc);
        descContent.setStyle("-fx-text-fill: -text-main; -fx-line-spacing: 4;");
        descContent.setWrapText(true);
        descBox.getChildren().addAll(descHeader, descContent);

        rightCol.getChildren().addAll(titles, readingExperienceBox, metaGrid, descBox);
        mainCard.getChildren().addAll(coverPane, rightCol);
        contentBox.getChildren().add(mainCard);

        // 3. Chapters & University Assignments Section (New!)
        chaptersCard = buildChaptersCard();
        contentBox.getChildren().add(chaptersCard);
    }

    private VBox buildReadingExperienceCard() {
        VBox box = new VBox(14);
        box.getStyleClass().addAll("form-container");
        box.setPadding(new Insets(18));

        Label expTitle = new Label(I18n.get("details.reading_progress"));
        expTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        // Progress Text Header
        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);

        pageProgressText = new Label(book.getProgressRatioString());
        pageProgressText.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-main;");
        HBox.setHgrow(pageProgressText, Priority.ALWAYS);

        percentText = new Label(book.getFormattedProgress());
        percentText.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -accent-primary;");

        if (I18n.isRTL()) {
            progressHeader.getChildren().addAll(percentText, pageProgressText);
        } else {
            progressHeader.getChildren().addAll(pageProgressText, percentText);
        }

        progressBar = new ProgressBar(book.getProgressPercentage() / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("book-progress-bar");
        progressBar.setPrefHeight(10);

        // Page Control Row
        HBox controlRow = new HBox(12);
        controlRow.setAlignment(Pos.CENTER_LEFT);

        Label pageLabel = new Label(I18n.get("details.current_page"));
        pageLabel.getStyleClass().add("form-label");

        pageSpinner = new Spinner<>(0, book.getTotalPages(), book.getCurrentPage());
        pageSpinner.setEditable(true);
        pageSpinner.setPrefWidth(100);
        pageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != book.getCurrentPage()) {
                book.setCurrentPage(newVal);
                saveAndAnimateProgress();
            }
        });

        // Quick page buttons
        Button p1 = new Button(I18n.get("details.quick.p1"));
        p1.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        p1.setOnAction(e -> advancePages(1));

        Button p10 = new Button(I18n.get("details.quick.p10"));
        p10.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        p10.setOnAction(e -> advancePages(10));

        Button p25 = new Button(I18n.get("details.quick.p25"));
        p25.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        p25.setOnAction(e -> advancePages(25));

        Button completeBtn = new Button(I18n.get("details.quick.complete"));
        completeBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        completeBtn.setOnAction(e -> {
            bookService.markAsCompleted(book);
            mainController.showToast(I18n.get("toast.completed"), ToastNotification.ToastType.SUCCESS);
            refreshData();
        });

        controlRow.getChildren().addAll(pageLabel, pageSpinner, p1, p10, p25, completeBtn);

        box.getChildren().addAll(expTitle, progressHeader, progressBar, controlRow);
        return box;
    }

    private VBox buildChaptersCard() {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("stat-card");
        card.setPadding(new Insets(24));

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label title = new Label(I18n.get("chapters.title"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label sub = new Label(I18n.get("chapters.subtitle"));
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        chapterSummaryLabel = new Label();
        chapterSummaryLabel.getStyleClass().addAll("badge-chip", "status-reading");
        chapterSummaryLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 10 4 10;");

        Button addChapterBtn = new Button(I18n.get("chapters.add"));
        addChapterBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        SVGPath plusIcon = IconUtil.createIcon(IconUtil.IconType.PLUS, 13);
        addChapterBtn.setGraphic(plusIcon);
        addChapterBtn.setGraphicTextGap(6);
        addChapterBtn.setOnAction(e -> openAddChapterDialog());

        header.getChildren().addAll(titleBox, chapterSummaryLabel, addChapterBtn);

        // Chapter Progress Bar
        chapterProgressBar = new ProgressBar(0.0);
        chapterProgressBar.setMaxWidth(Double.MAX_VALUE);
        chapterProgressBar.getStyleClass().add("book-progress-bar");
        chapterProgressBar.setPrefHeight(8);

        // List Container
        chaptersListContainer = new VBox(10);

        card.getChildren().addAll(header, chapterProgressBar, chaptersListContainer);

        reloadChapters();
        return card;
    }

    private void reloadChapters() {
        if (book == null || book.getId() == null) return;

        List<Chapter> chapters = chapterService.getChaptersByBookId(book.getId());
        chaptersListContainer.getChildren().clear();

        int total = chapters.size();
        int completed = 0;
        for (Chapter c : chapters) {
            if (c.isCompleted()) completed++;
        }

        int pct = total > 0 ? (int) Math.round(((double) completed / total) * 100.0) : 0;
        chapterSummaryLabel.setText(I18n.get("chapters.summary", completed, total, pct));
        AnimationUtil.animateProgressBar(chapterProgressBar, total > 0 ? (double) completed / total : 0.0);

        if (chapters.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(24));
            emptyBox.setStyle("-fx-background-color: -bg-hover; -fx-background-radius: 8px; -fx-border-radius: 8px;");

            Label emptyLabel = new Label(I18n.get("chapters.empty"));
            emptyLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-weight: 500;");

            Button addBtn = new Button(I18n.get("chapters.add"));
            addBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            addBtn.setOnAction(e -> openAddChapterDialog());

            emptyBox.getChildren().addAll(emptyLabel, addBtn);
            chaptersListContainer.getChildren().add(emptyBox);
            chapterSummaryLabel.setVisible(false);
            chapterSummaryLabel.setManaged(false);
            chapterProgressBar.setVisible(false);
            chapterProgressBar.setManaged(false);
        } else {
            chapterSummaryLabel.setVisible(true);
            chapterSummaryLabel.setManaged(true);
            chapterProgressBar.setVisible(true);
            chapterProgressBar.setManaged(true);

            for (Chapter ch : chapters) {
                chaptersListContainer.getChildren().add(buildChapterRow(ch));
            }
        }
    }

    private HBox buildChapterRow(Chapter chapter) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: -bg-card; -fx-border-color: -border-subtle; -fx-border-width: 1; -fx-background-radius: 8px; -fx-border-radius: 8px;");

        // Checkbox
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(chapter.isCompleted());
        checkBox.setOnAction(e -> {
            boolean isChecked = checkBox.isSelected();
            chapterService.toggleChapter(chapter.getId(), isChecked);
            chapter.setCompleted(isChecked);
            mainController.showToast(I18n.get("toast.chapter_toggled"), ToastNotification.ToastType.INFO);
            reloadChapters();
            mainController.refreshActiveViews();
        });

        // Chapter number badge
        Label numBadge = new Label(I18n.get("chapters.number") + " " + chapter.getChapterNumber());
        numBadge.setStyle("-fx-background-color: -bg-hover; -fx-text-fill: -text-main; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 6px;");

        // Chapter title
        Label title = new Label(chapter.getTitle());
        title.setStyle(chapter.isCompleted()
                ? "-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: -text-muted; -fx-strikethrough: true;"
                : "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-main;");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        // Page Range badge if available
        if (chapter.getStartPage() > 0 && chapter.getEndPage() >= chapter.getStartPage()) {
            Label pageBadge = new Label(I18n.get("chapters.pages_range", chapter.getStartPage(), chapter.getEndPage()));
            pageBadge.setStyle("-fx-background-color: -accent-subtle; -fx-text-fill: -accent-primary; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 2 7 2 7; -fx-background-radius: 6px;");
            row.getChildren().add(pageBadge);
        } else if (chapter.getStartPage() > 0) {
            Label pageBadge = new Label(I18n.get("chapters.page_single", chapter.getStartPage()));
            pageBadge.setStyle("-fx-background-color: -accent-subtle; -fx-text-fill: -accent-primary; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 2 7 2 7; -fx-background-radius: 6px;");
            row.getChildren().add(pageBadge);
        }

        // Notes tooltip if available
        if (chapter.getNotes() != null && !chapter.getNotes().trim().isEmpty()) {
            Tooltip tip = new Tooltip(chapter.getNotes());
            Tooltip.install(title, tip);
        }

        // Action buttons: Edit & Delete
        Button editBtn = new Button();
        editBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.EDIT, 12));
        editBtn.getStyleClass().addAll("btn", "btn-icon", "btn-sm");
        editBtn.setTooltip(new Tooltip(I18n.get("chapters.edit")));
        editBtn.setOnAction(e -> openEditChapterDialog(chapter));

        Button deleteBtn = new Button();
        deleteBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.TRASH, 12));
        deleteBtn.getStyleClass().addAll("btn", "btn-icon-danger", "btn-sm");
        deleteBtn.setTooltip(new Tooltip(I18n.get("chapters.delete")));
        deleteBtn.setOnAction(e -> handleConfirmDeleteChapter(chapter));

        HBox actions = new HBox(6, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(checkBox, numBadge, title, actions);
        AnimationUtil.addCardHover(row);
        return row;
    }

    private void openAddChapterDialog() {
        ChapterFormController dialog = new ChapterFormController(
                mainController,
                chapterService,
                book.getId(),
                null,
                this::reloadChapters
        );
        dialog.showAsDialog(mainController.getPrimaryStage());
    }

    private void openEditChapterDialog(Chapter chapter) {
        ChapterFormController dialog = new ChapterFormController(
                mainController,
                chapterService,
                book.getId(),
                chapter,
                this::reloadChapters
        );
        dialog.showAsDialog(mainController.getPrimaryStage());
    }

    private void handleConfirmDeleteChapter(Chapter chapter) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainController.getPrimaryStage());
        alert.setTitle(I18n.get("chapters.confirm_delete.title"));
        alert.setHeaderText(I18n.get("chapters.confirm_delete.header", chapter.getChapterNumber(), chapter.getTitle()));
        alert.setContentText(I18n.get("chapters.confirm_delete.content"));
        alert.getDialogPane().setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        ButtonType delBtn = new ButtonType(I18n.get("dialog.confirm_delete.delete"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(I18n.get("dialog.confirm_delete.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(delBtn, cancelBtn);

        alert.showAndWait().ifPresent(res -> {
            if (res == delBtn) {
                chapterService.deleteChapter(chapter.getId());
                mainController.showToast(I18n.get("toast.chapter_deleted"), ToastNotification.ToastType.SUCCESS);
                reloadChapters();
                mainController.refreshActiveViews();
            }
        });
    }

    private void advancePages(int delta) {
        bookService.advancePage(book, delta);
        mainController.showToast(I18n.get("toast.advanced", delta), ToastNotification.ToastType.INFO);
        refreshData();
    }

    private void saveAndAnimateProgress() {
        bookService.updateBook(book);
        updateProgressDisplay();
    }

    private void updateProgressDisplay() {
        pageProgressText.setText(book.getProgressRatioString());
        percentText.setText(book.getFormattedProgress());
        AnimationUtil.animateProgressBar(progressBar, book.getProgressPercentage() / 100.0);

        // Update badge
        statusBadge.setText(book.getStatus().getDisplayName());
        statusBadge.getStyleClass().removeAll("status-not-started", "status-reading", "status-completed");
        statusBadge.getStyleClass().add(book.getStatus().getStyleClass());

        dateStartedVal.setText(DateUtil.format(book.getDateStarted()));
        dateCompletedVal.setText(DateUtil.format(book.getDateCompleted()));
    }

    private void refreshData() {
        bookService.getBookById(book.getId()).ifPresent(updated -> {
            this.book = updated;
            pageSpinner.getValueFactory().setValue(book.getCurrentPage());
            updateProgressDisplay();
            reloadChapters();
        });
    }

    private void addMetaItem(GridPane grid, int col, int row, String label, String value) {
        VBox item = new VBox(2);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-subtext");
        Label val = new Label(value);
        val.getStyleClass().add("stat-value");
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        item.getChildren().addAll(lbl, val);
        grid.add(item, col, row);
    }

    private StackPane buildCoverNode() {
        StackPane container = new StackPane();
        container.getStyleClass().add("book-cover-container");
        container.setPrefHeight(280);
        container.setMinHeight(260);

        boolean imageLoaded = false;
        if (book.getCoverImage() != null && !book.getCoverImage().trim().isEmpty()) {
            try {
                File file = new File(book.getCoverImage());
                Image img;
                if (file.exists()) {
                    img = new Image(file.toURI().toString(), 240, 320, true, true);
                } else if (book.getCoverImage().startsWith("http://") || book.getCoverImage().startsWith("https://")) {
                    img = new Image(book.getCoverImage(), 240, 320, true, true, true);
                } else {
                    img = null;
                }
                if (img != null && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(220);
                    iv.setFitHeight(300);
                    iv.setPreserveRatio(true);
                    container.getChildren().add(iv);
                    imageLoaded = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (!imageLoaded) {
            VBox fallback = new VBox(14);
            fallback.setAlignment(Pos.CENTER);
            fallback.setPadding(new Insets(24));
            fallback.getStyleClass().addAll("book-cover-placeholder", "cover-gradient-2");
            fallback.setPrefHeight(280);

            SVGPath bookIcon = IconUtil.createIcon(IconUtil.IconType.PAGES, 48);
            bookIcon.getStyleClass().add("cover-placeholder-icon");

            Label coverTitle = new Label(book.getTitle());
            coverTitle.getStyleClass().add("cover-placeholder-title");
            coverTitle.setStyle("-fx-font-size: 16px;");
            coverTitle.setWrapText(true);
            coverTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Label coverAuthor = new Label(book.getAuthor());
            coverAuthor.getStyleClass().add("cover-placeholder-author");
            coverAuthor.setStyle("-fx-font-size: 13px;");

            fallback.getChildren().addAll(bookIcon, coverTitle, coverAuthor);
            container.getChildren().add(fallback);
        }

        return container;
    }

    private void handleDelete() {
        boolean confirm = true;
        if (settingsService.isConfirmDeleteEnabled()) {
            confirm = DialogUtil.confirmDelete(mainController.getPrimaryStage(), book.getTitle());
        }
        if (confirm) {
            bookService.deleteBook(book.getId());
            mainController.showToast(I18n.get("toast.book_deleted", book.getTitle()), ToastNotification.ToastType.SUCCESS);
            mainController.navigateToLibrary(null);
            mainController.refreshActiveViews();
        }
    }
}
