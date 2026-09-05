package com.librarymanager.controller;

import com.librarymanager.component.BookCardComponent;
import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BookService;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.DialogUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Book Library view with real-time search,
 * status filtering, sorting, responsive card grid, and localized RTL support.
 */
public class LibraryController {

    private final MainController mainController;
    private final BookService bookService;
    private final SettingsService settingsService;

    private final VBox rootBox;
    private final ScrollPane scrollPane;
    private final FlowPane bookGrid;
    private final VBox emptyStateBox;

    // Controls
    private TextField searchField;
    private ComboBox<SortOption> sortComboBox;
    private final List<Button> filterButtons = new ArrayList<>();
    private ReadingStatus activeStatusFilter = null;

    private PauseTransition searchDebounce;

    public LibraryController(MainController mainController, BookService bookService, SettingsService settingsService) {
        this.mainController = mainController;
        this.bookService = bookService;
        this.settingsService = settingsService;

        rootBox = new VBox(18);
        rootBox.setFillWidth(true);
        rootBox.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Filter & Search Toolbar
        HBox toolbar = buildToolbar();
        rootBox.getChildren().add(toolbar);

        // Content Area with Grid & Empty State
        StackPane gridContainer = new StackPane();
        VBox.setVgrow(gridContainer, Priority.ALWAYS);

        bookGrid = new FlowPane();
        bookGrid.setHgap(18);
        bookGrid.setVgap(18);
        bookGrid.setPadding(new Insets(4, 8, 24, 4));

        scrollPane = new ScrollPane(bookGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        emptyStateBox = buildEmptyStateBox();

        gridContainer.getChildren().addAll(scrollPane, emptyStateBox);
        rootBox.getChildren().add(gridContainer);
    }

    public Node getView() {
        return rootBox;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Search Input
        HBox searchContainer = new HBox(8);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.getStyleClass().add("search-field");
        HBox.setHgrow(searchContainer, Priority.ALWAYS);

        SVGPath searchIcon = IconUtil.createIcon(IconUtil.IconType.SEARCH, 15);
        searchIcon.getStyleClass().add("app-icon");

        searchField = new TextField();
        searchField.setPromptText(I18n.get("library.search.prompt"));
        searchField.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchDebounce = new PauseTransition(Duration.millis(200));
        searchDebounce.setOnFinished(e -> reloadBooks());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.playFromStart();
        });

        searchContainer.getChildren().addAll(searchIcon, searchField);

        // Filter Pills
        HBox filterGroup = new HBox(6);
        filterGroup.setAlignment(Pos.CENTER_LEFT);

        Button filterAll = createFilterButton(I18n.get("library.filter.all"), null);
        Button filterReading = createFilterButton(ReadingStatus.READING.getDisplayName(), ReadingStatus.READING);
        Button filterCompleted = createFilterButton(ReadingStatus.COMPLETED.getDisplayName(), ReadingStatus.COMPLETED);
        Button filterNotStarted = createFilterButton(ReadingStatus.NOT_STARTED.getDisplayName(), ReadingStatus.NOT_STARTED);

        filterButtons.addAll(List.of(filterAll, filterReading, filterCompleted, filterNotStarted));
        filterGroup.getChildren().addAll(filterAll, filterReading, filterCompleted, filterNotStarted);
        setActiveFilterButton(filterAll);

        // Sort Dropdown
        sortComboBox = new ComboBox<>();
        sortComboBox.getStyleClass().add("combo-box");
        sortComboBox.getItems().addAll(
                new SortOption(I18n.get("library.sort.date_added_desc"), "date_added", false),
                new SortOption(I18n.get("library.sort.date_added_asc"), "date_added", true),
                new SortOption(I18n.get("library.sort.title_asc"), "title", true),
                new SortOption(I18n.get("library.sort.author_asc"), "author", true),
                new SortOption(I18n.get("library.sort.progress_desc"), "progress", false),
                new SortOption(I18n.get("library.sort.pages_desc"), "total_pages", false)
        );
        sortComboBox.getSelectionModel().selectFirst();
        sortComboBox.setOnAction(e -> reloadBooks());

        bar.getChildren().addAll(searchContainer, filterGroup, sortComboBox);
        return bar;
    }

    private Button createFilterButton(String label, ReadingStatus status) {
        Button btn = new Button(label);
        btn.getStyleClass().add("filter-pill");
        btn.setOnAction(e -> {
            this.activeStatusFilter = status;
            setActiveFilterButton(btn);
            reloadBooks();
        });
        return btn;
    }

    private void setActiveFilterButton(Button active) {
        for (Button b : filterButtons) {
            b.getStyleClass().remove("active");
        }
        if (active != null && !active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
    }

    private VBox buildEmptyStateBox() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state-box");
        box.setVisible(false);
        box.setManaged(false);

        SVGPath icon = IconUtil.createIcon(IconUtil.IconType.LIBRARY, 48);
        icon.setStyle("-fx-fill: -text-muted;");

        Label title = new Label(I18n.get("library.empty.title"));
        title.getStyleClass().add("empty-state-title");

        Label desc = new Label(I18n.get("library.empty.desc"));
        desc.getStyleClass().add("empty-state-subtitle");

        Button actionBtn = new Button(I18n.get("library.empty.clear"));
        actionBtn.getStyleClass().addAll("btn", "btn-secondary");
        actionBtn.setOnAction(e -> {
            searchField.clear();
            applyFilter(null);
        });

        box.getChildren().addAll(icon, title, desc, actionBtn);
        return box;
    }

    public void applyFilter(ReadingStatus status) {
        this.activeStatusFilter = status;
        for (Button btn : filterButtons) {
            String text = btn.getText();
            if (status == null && I18n.get("library.filter.all").equals(text)) {
                setActiveFilterButton(btn);
            } else if (status != null && status.getDisplayName().equalsIgnoreCase(text)) {
                setActiveFilterButton(btn);
            }
        }
        reloadBooks();
    }

    public void reloadBooks() {
        String query = searchField.getText();
        SortOption sort = sortComboBox.getValue();
        String sortBy = sort != null ? sort.column : "date_added";
        boolean ascending = sort != null && sort.ascending;

        List<Book> books = bookService.searchBooks(query, activeStatusFilter, sortBy, ascending);

        bookGrid.getChildren().clear();

        if (books.isEmpty()) {
            scrollPane.setVisible(false);
            scrollPane.setManaged(false);
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
        } else {
            scrollPane.setVisible(true);
            scrollPane.setManaged(true);
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);

            for (Book b : books) {
                BookCardComponent card = new BookCardComponent(
                        b,
                        book -> mainController.navigateToBookDetails(book),
                        book -> mainController.openBookFormDialog(book),
                        (comp, book) -> handleDeleteBook(comp, book),
                        book -> handleQuickAdvance(book, 10)
                );
                bookGrid.getChildren().add(card);
            }
        }
    }

    private void handleQuickAdvance(Book book, int pages) {
        bookService.advancePage(book, pages);
        mainController.showToast(I18n.get("toast.progress_updated", book.getTitle()), ToastNotification.ToastType.SUCCESS);
        reloadBooks();
    }

    private void handleDeleteBook(BookCardComponent card, Book book) {
        boolean confirm = true;
        if (settingsService.isConfirmDeleteEnabled()) {
            confirm = DialogUtil.confirmDelete(mainController.getPrimaryStage(), book.getTitle());
        }
        if (confirm) {
            // Animate card removal smoothly
            AnimationUtil.animateCardRemoval(card, () -> {
                bookService.deleteBook(book.getId());
                bookGrid.getChildren().remove(card);
                mainController.showToast(I18n.get("toast.book_deleted", book.getTitle()), ToastNotification.ToastType.SUCCESS);
                if (bookGrid.getChildren().isEmpty()) {
                    reloadBooks();
                }
            });
        }
    }

    private static class SortOption {
        final String label;
        final String column;
        final boolean ascending;

        SortOption(String label, String column, boolean ascending) {
            this.label = label;
            this.column = column;
            this.ascending = ascending;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
