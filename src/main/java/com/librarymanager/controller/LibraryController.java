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
    private ComboBox<String> categoryComboBox;
    private ComboBox<SortOption> sortComboBox;
    private final List<Button> filterButtons = new ArrayList<>();

    public enum FilterMode {
        ALL, READING, COMPLETED, NOT_STARTED, FAVORITES, WISHLIST
    }
    private FilterMode activeFilterMode = FilterMode.ALL;

    private PauseTransition searchDebounce;

    public LibraryController(MainController mainController, BookService bookService, SettingsService settingsService) {
        this.mainController = mainController;
        this.bookService = bookService;
        this.settingsService = settingsService;

        rootBox = new VBox(16);
        rootBox.setFillWidth(true);
        rootBox.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Filter & Search Toolbar
        VBox toolbar = buildToolbar();
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

    private VBox buildToolbar() {
        VBox bar = new VBox(10);

        // Row 1: Search Input + Category Filter + Sort Dropdown
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

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
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchDebounce.playFromStart());

        searchContainer.getChildren().addAll(searchIcon, searchField);

        // Category Filter Dropdown
        categoryComboBox = new ComboBox<>();
        categoryComboBox.getStyleClass().add("combo-box");
        categoryComboBox.setPrefWidth(160);
        refreshCategories();
        categoryComboBox.setOnAction(e -> reloadBooks());

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

        topRow.getChildren().addAll(searchContainer, categoryComboBox, sortComboBox);

        // Row 2: Filter Pills
        HBox filterGroup = new HBox(8);
        filterGroup.setAlignment(Pos.CENTER_LEFT);

        Button filterAll = createFilterButton(I18n.get("library.filter.all"), FilterMode.ALL);
        Button filterReading = createFilterButton(ReadingStatus.READING.getDisplayName(), FilterMode.READING);
        Button filterCompleted = createFilterButton(ReadingStatus.COMPLETED.getDisplayName(), FilterMode.COMPLETED);
        Button filterNotStarted = createFilterButton(ReadingStatus.NOT_STARTED.getDisplayName(), FilterMode.NOT_STARTED);
        Button filterFavorites = createFilterButton("❤️ " + I18n.get("library.filter.favorites"), FilterMode.FAVORITES);
        Button filterWishlist = createFilterButton("🌟 " + I18n.get("library.filter.wishlist"), FilterMode.WISHLIST);

        filterButtons.addAll(List.of(filterAll, filterReading, filterCompleted, filterNotStarted, filterFavorites, filterWishlist));
        filterGroup.getChildren().addAll(filterAll, filterReading, filterCompleted, filterNotStarted, filterFavorites, filterWishlist);
        updateActiveFilterButtonUI();

        bar.getChildren().addAll(topRow, filterGroup);
        return bar;
    }

    public void refreshCategories() {
        String currentSelection = categoryComboBox.getValue();
        List<String> categories = bookService.getAllCategories();
        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().add(I18n.get("library.category.all"));
        categoryComboBox.getItems().addAll(categories);
        if (currentSelection != null && categoryComboBox.getItems().contains(currentSelection)) {
            categoryComboBox.setValue(currentSelection);
        } else {
            categoryComboBox.getSelectionModel().selectFirst();
        }
    }

    private Button createFilterButton(String label, FilterMode mode) {
        Button btn = new Button(label);
        btn.getStyleClass().add("filter-pill");
        btn.setOnAction(e -> {
            this.activeFilterMode = mode;
            updateActiveFilterButtonUI();
            reloadBooks();
        });
        return btn;
    }

    private void updateActiveFilterButtonUI() {
        for (Button b : filterButtons) {
            b.getStyleClass().remove("active");
        }
        int index = activeFilterMode.ordinal();
        if (index >= 0 && index < filterButtons.size()) {
            filterButtons.get(index).getStyleClass().add("active");
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
            categoryComboBox.getSelectionModel().selectFirst();
            activeFilterMode = FilterMode.ALL;
            updateActiveFilterButtonUI();
            reloadBooks();
        });

        box.getChildren().addAll(icon, title, desc, actionBtn);
        return box;
    }

    public void applyFilter(ReadingStatus status) {
        if (status == null) {
            activeFilterMode = FilterMode.ALL;
        } else if (status == ReadingStatus.READING) {
            activeFilterMode = FilterMode.READING;
        } else if (status == ReadingStatus.COMPLETED) {
            activeFilterMode = FilterMode.COMPLETED;
        } else if (status == ReadingStatus.NOT_STARTED) {
            activeFilterMode = FilterMode.NOT_STARTED;
        }
        updateActiveFilterButtonUI();
        reloadBooks();
    }

    public void reloadBooks() {
        String query = searchField.getText();
        SortOption sort = sortComboBox.getValue();
        String sortBy = sort != null ? sort.column : "date_added";
        boolean ascending = sort != null && sort.ascending;

        ReadingStatus status = null;
        Boolean isFavorite = null;
        Boolean isWishlist = null;

        switch (activeFilterMode) {
            case READING -> status = ReadingStatus.READING;
            case COMPLETED -> status = ReadingStatus.COMPLETED;
            case NOT_STARTED -> status = ReadingStatus.NOT_STARTED;
            case FAVORITES -> isFavorite = true;
            case WISHLIST -> isWishlist = true;
            default -> {}
        }

        String selectedCat = categoryComboBox.getValue();
        String categoryFilter = null;
        if (selectedCat != null && !selectedCat.equals(I18n.get("library.category.all"))) {
            categoryFilter = selectedCat;
        }

        List<Book> books = bookService.searchBooks(query, status, categoryFilter, null, isFavorite, isWishlist, sortBy, ascending);

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
                        book -> handleQuickAdvance(book, 10),
                        book -> handleToggleFavorite(book)
                );
                bookGrid.getChildren().add(card);
            }
        }
    }

    private void handleToggleFavorite(Book book) {
        boolean newFavorite = !book.isFavorite();
        bookService.toggleFavorite(book.getId(), newFavorite);
        book.setFavorite(newFavorite);
        String msg = newFavorite
                ? I18n.get("toast.favorite_added", book.getTitle())
                : I18n.get("toast.favorite_removed", book.getTitle());
        mainController.showToast(msg, ToastNotification.ToastType.SUCCESS);
        reloadBooks();
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
