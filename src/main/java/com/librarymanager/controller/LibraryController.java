package com.librarymanager.controller;

import com.librarymanager.component.BookCardComponent;
import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.DuplicateGroup;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.model.SavedSearch;
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
import javafx.util.StringConverter;

import java.util.*;

/**
 * Controller for the Book Library view with advanced multi-filter search,
 * saved searches, duplicate-book detection, and bulk operations.
 */
public class LibraryController {

    private final MainController mainController;
    private final BookService bookService;
    private final SettingsService settingsService;

    private final VBox rootBox;
    private final ScrollPane scrollPane;
    private final FlowPane bookGrid;
    private final VBox emptyStateBox;

    // Search & Filter Controls
    private TextField searchField;
    private TextField authorField;
    private ComboBox<String> categoryComboBox;
    private ComboBox<String> tagComboBox;
    private TextField minPagesField;
    private TextField maxPagesField;
    private ComboBox<SortOption> sortComboBox;

    // Filter Buttons (Simultaneous)
    private ReadingStatus activeStatusFilter = null; // null means ALL
    private final List<Button> statusButtons = new ArrayList<>();
    private Button filterFavoritesBtn;
    private Button filterWishlistBtn;
    private boolean isFavoriteFilter = false;
    private boolean isWishlistFilter = false;

    // Advanced search toggle & panel
    private Button advancedToggleBtn;
    private VBox advancedSearchPanel;

    // Saved searches
    private ComboBox<SavedSearch> savedSearchesComboBox;
    private Button deleteSavedSearchBtn;

    // Duplicates banner & button
    private HBox duplicateBanner;
    private Label duplicateBannerLabel;
    private Button duplicatePillBtn;
    private boolean duplicateBannerDismissed = false;

    // Bulk selection & actions bar
    private final Set<Long> selectedBookIds = new LinkedHashSet<>();
    private final List<BookCardComponent> currentCards = new ArrayList<>();
    private HBox bulkActionBar;
    private Label bulkCountLabel;
    private Button bulkSelectAllBtn;

    private PauseTransition searchDebounce;

    public LibraryController(MainController mainController, BookService bookService, SettingsService settingsService) {
        this.mainController = mainController;
        this.bookService = bookService;
        this.settingsService = settingsService;

        rootBox = new VBox(14);
        rootBox.setFillWidth(true);
        rootBox.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Filter & Search Toolbar
        VBox toolbar = buildToolbar();
        rootBox.getChildren().add(toolbar);

        // Duplicates Alert Banner
        duplicateBanner = buildDuplicateBanner();
        rootBox.getChildren().add(duplicateBanner);

        // Bulk Operations Bar
        bulkActionBar = buildBulkActionBar();
        rootBox.getChildren().add(bulkActionBar);

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

        // Row 1: Search Input + Category Filter + Sort Dropdown + Advanced Search Toggle
        HBox topRow = new HBox(10);
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

        Button clearBtn = new Button();
        SVGPath closeIcon = IconUtil.createIcon(IconUtil.IconType.CLOSE, 10);
        closeIcon.getStyleClass().add("app-icon");
        clearBtn.setGraphic(closeIcon);
        clearBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2 4 2 4; -fx-cursor: hand;");
        clearBtn.setVisible(false);
        clearBtn.setManaged(false);
        clearBtn.setOnAction(e -> searchField.clear());

        searchDebounce = new PauseTransition(Duration.millis(200));
        searchDebounce.setOnFinished(e -> reloadBooks());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = newVal != null && !newVal.trim().isEmpty();
            clearBtn.setVisible(hasText);
            clearBtn.setManaged(hasText);
            searchDebounce.playFromStart();
        });

        searchContainer.getChildren().addAll(searchIcon, searchField, clearBtn);

        // Category Filter Dropdown
        categoryComboBox = new ComboBox<>();
        categoryComboBox.getStyleClass().add("combo-box");
        categoryComboBox.setPrefWidth(150);
        refreshCategories();
        categoryComboBox.setOnAction(e -> reloadBooks());

        // Sort Dropdown
        sortComboBox = new ComboBox<>();
        sortComboBox.getStyleClass().add("combo-box");
        sortComboBox.setPrefWidth(160);
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

        // Advanced Search Toggle Button
        advancedToggleBtn = new Button(I18n.get("library.advanced_search") + " ▼");
        advancedToggleBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        advancedToggleBtn.setOnAction(e -> toggleAdvancedPanel());

        topRow.getChildren().addAll(searchContainer, categoryComboBox, sortComboBox, advancedToggleBtn);

        // Row 2: Status Pills, Favorites toggle, Wishlist toggle, Saved Searches, and Duplicate Button
        HBox filterGroup = new HBox(8);
        filterGroup.setAlignment(Pos.CENTER_LEFT);

        Button filterAll = createStatusButton(I18n.get("library.filter.all"), null);
        Button filterReading = createStatusButton(ReadingStatus.READING.getDisplayName(), ReadingStatus.READING);
        Button filterCompleted = createStatusButton(ReadingStatus.COMPLETED.getDisplayName(), ReadingStatus.COMPLETED);
        Button filterNotStarted = createStatusButton(ReadingStatus.NOT_STARTED.getDisplayName(), ReadingStatus.NOT_STARTED);

        statusButtons.addAll(List.of(filterAll, filterReading, filterCompleted, filterNotStarted));

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        // Independent Toggle: Favorites
        filterFavoritesBtn = new Button("❤️ " + I18n.get("library.filter.favorites"));
        filterFavoritesBtn.getStyleClass().add("filter-pill");
        filterFavoritesBtn.setOnAction(e -> {
            isFavoriteFilter = !isFavoriteFilter;
            updateFilterButtonsUI();
            reloadBooks();
        });

        // Independent Toggle: Wishlist
        filterWishlistBtn = new Button("🌟 " + I18n.get("library.filter.wishlist"));
        filterWishlistBtn.getStyleClass().add("filter-pill");
        filterWishlistBtn.setOnAction(e -> {
            isWishlistFilter = !isWishlistFilter;
            updateFilterButtonsUI();
            reloadBooks();
        });

        // Duplicate Pill Button (appears if duplicates exist)
        duplicatePillBtn = new Button();
        duplicatePillBtn.getStyleClass().addAll("btn", "btn-warning", "btn-sm");
        duplicatePillBtn.setVisible(false);
        duplicatePillBtn.setManaged(false);
        duplicatePillBtn.setOnAction(e -> openDuplicateResolutionDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Saved Searches Dropdown & Buttons
        HBox savedSearchBox = new HBox(6);
        savedSearchBox.setAlignment(Pos.CENTER_LEFT);

        savedSearchesComboBox = new ComboBox<>();
        savedSearchesComboBox.getStyleClass().add("combo-box");
        savedSearchesComboBox.setPrefWidth(160);
        savedSearchesComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SavedSearch s) {
                return s == null ? "" : s.getName();
            }

            @Override
            public SavedSearch fromString(String string) {
                return null;
            }
        });
        savedSearchesComboBox.setOnAction(e -> {
            SavedSearch sel = savedSearchesComboBox.getValue();
            if (sel != null) {
                applySavedSearch(sel);
            }
        });

        Button saveSearchBtn = new Button("💾 " + I18n.get("search.saved.save_button"));
        saveSearchBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        saveSearchBtn.setOnAction(e -> handleSaveSearch());

        deleteSavedSearchBtn = new Button();
        SVGPath trashIcon = IconUtil.createIcon(IconUtil.IconType.TRASH, 12);
        trashIcon.setStyle("-fx-fill: -accent-danger;");
        deleteSavedSearchBtn.setGraphic(trashIcon);
        deleteSavedSearchBtn.getStyleClass().addAll("btn", "btn-icon", "btn-sm");
        deleteSavedSearchBtn.setTooltip(new Tooltip(I18n.get("search.saved.delete_tooltip")));
        deleteSavedSearchBtn.setDisable(true);
        deleteSavedSearchBtn.setOnAction(e -> handleDeleteSavedSearch());

        savedSearchBox.getChildren().addAll(savedSearchesComboBox, saveSearchBtn, deleteSavedSearchBtn);
        refreshSavedSearches();

        filterGroup.getChildren().addAll(
                filterAll, filterReading, filterCompleted, filterNotStarted,
                sep1, filterFavoritesBtn, filterWishlistBtn, duplicatePillBtn,
                spacer, savedSearchBox
        );

        // Row 3: Collapsible Advanced Search Panel
        advancedSearchPanel = buildAdvancedSearchPanel();

        bar.getChildren().addAll(topRow, filterGroup, advancedSearchPanel);
        updateFilterButtonsUI();
        return bar;
    }

    private VBox buildAdvancedSearchPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("advanced-search-panel");
        panel.setVisible(false);
        panel.setManaged(false);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Author input
        VBox authorBox = new VBox(4);
        Label authorLbl = new Label(I18n.get("library.advanced_search.author"));
        authorLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: 600;");
        authorField = new TextField();
        authorField.setPromptText("e.g. Frank Herbert");
        authorField.setPrefWidth(150);
        authorField.textProperty().addListener((o, oldV, newV) -> searchDebounce.playFromStart());
        authorBox.getChildren().addAll(authorLbl, authorField);

        // Tag ComboBox
        VBox tagBox = new VBox(4);
        Label tagLbl = new Label(I18n.get("library.advanced_search.tag"));
        tagLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: 600;");
        tagComboBox = new ComboBox<>();
        tagComboBox.getStyleClass().add("combo-box");
        tagComboBox.setPrefWidth(140);
        refreshTags();
        tagComboBox.setOnAction(e -> reloadBooks());
        tagBox.getChildren().addAll(tagLbl, tagComboBox);

        // Min Pages
        VBox minBox = new VBox(4);
        Label minLbl = new Label(I18n.get("library.advanced_search.min_pages"));
        minLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: 600;");
        minPagesField = new TextField();
        minPagesField.setPromptText("0");
        minPagesField.setPrefWidth(80);
        minPagesField.textProperty().addListener((o, oldV, newV) -> {
            if (!newV.matches("\\d*")) minPagesField.setText(newV.replaceAll("[^\\d]", ""));
            searchDebounce.playFromStart();
        });
        minBox.getChildren().addAll(minLbl, minPagesField);

        // Max Pages
        VBox maxBox = new VBox(4);
        Label maxLbl = new Label(I18n.get("library.advanced_search.max_pages"));
        maxLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted; -fx-font-weight: 600;");
        maxPagesField = new TextField();
        maxPagesField.setPromptText("1000");
        maxPagesField.setPrefWidth(80);
        maxPagesField.textProperty().addListener((o, oldV, newV) -> {
            if (!newV.matches("\\d*")) maxPagesField.setText(newV.replaceAll("[^\\d]", ""));
            searchDebounce.playFromStart();
        });
        maxBox.getChildren().addAll(maxLbl, maxPagesField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearAdvancedBtn = new Button(I18n.get("library.advanced_search.clear"));
        clearAdvancedBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        clearAdvancedBtn.setOnAction(e -> resetAllFilters());

        row.getChildren().addAll(authorBox, tagBox, minBox, maxBox, spacer, clearAdvancedBtn);
        panel.getChildren().add(row);
        return panel;
    }

    private void toggleAdvancedPanel() {
        boolean visible = !advancedSearchPanel.isVisible();
        advancedSearchPanel.setVisible(visible);
        advancedSearchPanel.setManaged(visible);
        advancedToggleBtn.setText(I18n.get("library.advanced_search") + (visible ? " ▲" : " ▼"));
    }

    private HBox buildDuplicateBanner() {
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("duplicate-banner");
        banner.setVisible(false);
        banner.setManaged(false);

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size: 16px;");

        duplicateBannerLabel = new Label();
        duplicateBannerLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: -text-main;");
        HBox.setHgrow(duplicateBannerLabel, Priority.ALWAYS);

        Button reviewBtn = new Button(I18n.get("duplicate.banner.review"));
        reviewBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        reviewBtn.setOnAction(e -> openDuplicateResolutionDialog());

        Button dismissBtn = new Button("✕");
        dismissBtn.getStyleClass().addAll("btn", "btn-icon", "btn-sm");
        dismissBtn.setOnAction(e -> {
            duplicateBannerDismissed = true;
            duplicateBanner.setVisible(false);
            duplicateBanner.setManaged(false);
        });

        banner.getChildren().addAll(icon, duplicateBannerLabel, reviewBtn, dismissBtn);
        return banner;
    }

    private HBox buildBulkActionBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("bulk-actions-bar");
        bar.setVisible(false);
        bar.setManaged(false);

        bulkCountLabel = new Label();
        bulkCountLabel.getStyleClass().add("bulk-count-label");

        bulkSelectAllBtn = new Button(I18n.get("bulk.select_all"));
        bulkSelectAllBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        bulkSelectAllBtn.setOnAction(e -> handleBulkSelectAllToggle());

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        // Bulk operations
        Button markCompletedBtn = new Button("✓ " + I18n.get("bulk.action.mark_completed"));
        markCompletedBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        markCompletedBtn.setOnAction(e -> handleBulkMarkCompleted());

        Button changeCategoryBtn = new Button("📁 " + I18n.get("bulk.action.change_category"));
        changeCategoryBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        changeCategoryBtn.setOnAction(e -> handleBulkChangeCategory());

        Button addTagBtn = new Button("🏷️ " + I18n.get("bulk.action.add_tag"));
        addTagBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        addTagBtn.setOnAction(e -> handleBulkAddTag());

        Button deleteBtn = new Button("🗑️ " + I18n.get("bulk.action.delete"));
        deleteBtn.getStyleClass().addAll("btn", "btn-danger", "btn-sm");
        deleteBtn.setOnAction(e -> handleBulkDelete());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearBtn = new Button(I18n.get("bulk.clear_selection") + " ✕");
        clearBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        clearBtn.setOnAction(e -> handleBulkClear());

        bar.getChildren().addAll(
                bulkCountLabel, bulkSelectAllBtn, sep1,
                markCompletedBtn, changeCategoryBtn, addTagBtn, deleteBtn,
                spacer, clearBtn
        );
        return bar;
    }

    private Button createStatusButton(String label, ReadingStatus status) {
        Button btn = new Button(label);
        btn.getStyleClass().add("filter-pill");
        btn.setOnAction(e -> {
            this.activeStatusFilter = status;
            updateFilterButtonsUI();
            reloadBooks();
        });
        return btn;
    }

    private void updateFilterButtonsUI() {
        for (Button b : statusButtons) {
            b.getStyleClass().remove("active");
        }
        if (activeStatusFilter == null) {
            statusButtons.get(0).getStyleClass().add("active");
        } else if (activeStatusFilter == ReadingStatus.READING) {
            statusButtons.get(1).getStyleClass().add("active");
        } else if (activeStatusFilter == ReadingStatus.COMPLETED) {
            statusButtons.get(2).getStyleClass().add("active");
        } else if (activeStatusFilter == ReadingStatus.NOT_STARTED) {
            statusButtons.get(3).getStyleClass().add("active");
        }

        if (isFavoriteFilter) {
            if (!filterFavoritesBtn.getStyleClass().contains("active")) filterFavoritesBtn.getStyleClass().add("active");
        } else {
            filterFavoritesBtn.getStyleClass().remove("active");
        }

        if (isWishlistFilter) {
            if (!filterWishlistBtn.getStyleClass().contains("active")) filterWishlistBtn.getStyleClass().add("active");
        } else {
            filterWishlistBtn.getStyleClass().remove("active");
        }
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

    public void refreshTags() {
        if (tagComboBox == null) return;
        String currentSelection = tagComboBox.getValue();
        List<String> tags = bookService.getAllTags();
        tagComboBox.getItems().clear();
        tagComboBox.getItems().add(I18n.get("library.tag.all"));
        tagComboBox.getItems().addAll(tags);
        if (currentSelection != null && tagComboBox.getItems().contains(currentSelection)) {
            tagComboBox.setValue(currentSelection);
        } else {
            tagComboBox.getSelectionModel().selectFirst();
        }
    }

    public void refreshSavedSearches() {
        if (savedSearchesComboBox == null) return;
        List<SavedSearch> list = bookService.getAllSavedSearches();
        savedSearchesComboBox.getItems().clear();
        savedSearchesComboBox.getItems().addAll(list);
        savedSearchesComboBox.setPromptText(I18n.get("search.saved.placeholder", list.size()));
        deleteSavedSearchBtn.setDisable(true);
    }

    private void handleSaveSearch() {
        Optional<String> nameOpt = DialogUtil.promptText(
                mainController.getPrimaryStage(),
                I18n.get("search.saved.dialog_title"),
                I18n.get("search.saved.dialog_header"),
                I18n.get("search.saved.dialog_prompt"),
                ""
        );

        if (nameOpt.isPresent() && !nameOpt.get().trim().isEmpty()) {
            String name = nameOpt.get().trim();
            String cat = categoryComboBox.getValue();
            if (cat != null && cat.equals(I18n.get("library.category.all"))) cat = null;

            String tag = tagComboBox != null ? tagComboBox.getValue() : null;
            if (tag != null && tag.equals(I18n.get("library.tag.all"))) tag = null;

            SortOption sort = sortComboBox.getValue();

            SavedSearch s = new SavedSearch(
                    name,
                    searchField.getText(),
                    authorField != null ? authorField.getText() : null,
                    activeStatusFilter,
                    cat,
                    tag,
                    isFavoriteFilter ? true : null,
                    isWishlistFilter ? true : null,
                    parseIntegerOrNull(minPagesField != null ? minPagesField.getText() : null),
                    parseIntegerOrNull(maxPagesField != null ? maxPagesField.getText() : null),
                    sort != null ? sort.column : "date_added",
                    sort != null && sort.ascending
            );

            SavedSearch saved = bookService.saveSearch(s);
            refreshSavedSearches();
            savedSearchesComboBox.setValue(saved);
            mainController.showToast(I18n.get("search.saved.toast_saved", name), ToastNotification.ToastType.SUCCESS);
        }
    }

    private void handleDeleteSavedSearch() {
        SavedSearch s = savedSearchesComboBox.getValue();
        if (s != null && s.getId() != null) {
            bookService.deleteSavedSearch(s.getId());
            refreshSavedSearches();
            mainController.showToast(I18n.get("search.saved.deleted"), ToastNotification.ToastType.SUCCESS);
        }
    }

    private void applySavedSearch(SavedSearch s) {
        if (s == null) return;
        searchField.setText(s.getQuery() != null ? s.getQuery() : "");
        if (authorField != null) authorField.setText(s.getAuthor() != null ? s.getAuthor() : "");
        activeStatusFilter = s.getStatus();
        isFavoriteFilter = Boolean.TRUE.equals(s.getFavorite());
        isWishlistFilter = Boolean.TRUE.equals(s.getWishlist());

        if (s.getCategory() != null && categoryComboBox.getItems().contains(s.getCategory())) {
            categoryComboBox.setValue(s.getCategory());
        } else {
            categoryComboBox.getSelectionModel().selectFirst();
        }

        if (tagComboBox != null) {
            if (s.getTag() != null && tagComboBox.getItems().contains(s.getTag())) {
                tagComboBox.setValue(s.getTag());
            } else {
                tagComboBox.getSelectionModel().selectFirst();
            }
        }

        if (minPagesField != null) minPagesField.setText(s.getMinPages() != null ? String.valueOf(s.getMinPages()) : "");
        if (maxPagesField != null) maxPagesField.setText(s.getMaxPages() != null ? String.valueOf(s.getMaxPages()) : "");

        if (s.getSortBy() != null) {
            for (SortOption opt : sortComboBox.getItems()) {
                if (opt.column.equalsIgnoreCase(s.getSortBy()) && opt.ascending == s.isAscending()) {
                    sortComboBox.setValue(opt);
                    break;
                }
            }
        }

        // Auto open advanced panel if advanced fields are present
        boolean hasAdvanced = (s.getAuthor() != null && !s.getAuthor().isEmpty())
                || (s.getTag() != null && !s.getTag().isEmpty())
                || s.getMinPages() != null
                || s.getMaxPages() != null;
        if (hasAdvanced && !advancedSearchPanel.isVisible()) {
            toggleAdvancedPanel();
        }

        deleteSavedSearchBtn.setDisable(false);
        updateFilterButtonsUI();
        reloadBooks();
    }

    public void applyFilter(ReadingStatus status) {
        this.activeStatusFilter = status;
        this.isFavoriteFilter = false;
        this.isWishlistFilter = false;
        updateFilterButtonsUI();
        reloadBooks();
    }

    private void resetAllFilters() {
        searchField.clear();
        if (authorField != null) authorField.clear();
        if (minPagesField != null) minPagesField.clear();
        if (maxPagesField != null) maxPagesField.clear();
        categoryComboBox.getSelectionModel().selectFirst();
        if (tagComboBox != null) tagComboBox.getSelectionModel().selectFirst();
        activeStatusFilter = null;
        isFavoriteFilter = false;
        isWishlistFilter = false;
        updateFilterButtonsUI();
        reloadBooks();
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
        actionBtn.setOnAction(e -> resetAllFilters());

        box.getChildren().addAll(icon, title, desc, actionBtn);
        return box;
    }

    private Integer parseIntegerOrNull(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            int val = Integer.parseInt(text.trim());
            return val > 0 ? val : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void reloadBooks() {
        String query = searchField.getText();
        String author = authorField != null ? authorField.getText() : null;

        String selectedCat = categoryComboBox.getValue();
        String categoryFilter = null;
        if (selectedCat != null && !selectedCat.equals(I18n.get("library.category.all"))) {
            categoryFilter = selectedCat;
        }

        String selectedTag = tagComboBox != null ? tagComboBox.getValue() : null;
        String tagFilter = null;
        if (selectedTag != null && !selectedTag.equals(I18n.get("library.tag.all"))) {
            tagFilter = selectedTag;
        }

        Integer minPages = parseIntegerOrNull(minPagesField != null ? minPagesField.getText() : null);
        Integer maxPages = parseIntegerOrNull(maxPagesField != null ? maxPagesField.getText() : null);

        SortOption sort = sortComboBox.getValue();
        String sortBy = sort != null ? sort.column : "date_added";
        boolean ascending = sort != null && sort.ascending;

        Boolean fav = isFavoriteFilter ? true : null;
        Boolean wish = isWishlistFilter ? true : null;

        List<Book> books = bookService.searchBooks(
                query, author, activeStatusFilter, categoryFilter, tagFilter, fav, wish, minPages, maxPages, sortBy, ascending
        );

        // Check duplicates for banner & pill button
        updateDuplicateBanner();

        bookGrid.getChildren().clear();
        currentCards.clear();

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

                if (selectedBookIds.contains(b.getId())) {
                    card.setSelected(true);
                }

                card.setOnSelectionChanged(isSelected -> {
                    if (isSelected) {
                        selectedBookIds.add(b.getId());
                    } else {
                        selectedBookIds.remove(b.getId());
                    }
                    updateBulkActionBar();
                });

                currentCards.add(card);
                bookGrid.getChildren().add(card);
            }
        }
        updateBulkActionBar();
    }

    private void updateDuplicateBanner() {
        List<DuplicateGroup> duplicates = bookService.findDuplicates();
        int count = duplicates.size();
        if (count > 0) {
            duplicatePillBtn.setText("⚠️ " + I18n.get("duplicate.banner.review") + " (" + count + ")");
            duplicatePillBtn.setVisible(true);
            duplicatePillBtn.setManaged(true);

            if (!duplicateBannerDismissed) {
                duplicateBannerLabel.setText(I18n.get("duplicate.banner.message", count));
                duplicateBanner.setVisible(true);
                duplicateBanner.setManaged(true);
            }
        } else {
            duplicatePillBtn.setVisible(false);
            duplicatePillBtn.setManaged(false);
            duplicateBanner.setVisible(false);
            duplicateBanner.setManaged(false);
        }
    }

    private void openDuplicateResolutionDialog() {
        DuplicateResolutionDialog dialog = new DuplicateResolutionDialog(mainController, bookService);
        dialog.showAsDialog(mainController.getPrimaryStage());
        reloadBooks();
    }

    private void updateBulkActionBar() {
        int count = selectedBookIds.size();
        if (count == 0) {
            bulkActionBar.setVisible(false);
            bulkActionBar.setManaged(false);
        } else {
            bulkActionBar.setVisible(true);
            bulkActionBar.setManaged(true);
            bulkCountLabel.setText(I18n.get("bulk.selected_count", count));

            boolean allSelected = !currentCards.isEmpty() && currentCards.stream().allMatch(BookCardComponent::isSelected);
            bulkSelectAllBtn.setText(allSelected ? I18n.get("bulk.deselect_all") : I18n.get("bulk.select_all"));
        }
    }

    private void handleBulkSelectAllToggle() {
        boolean allSelected = !currentCards.isEmpty() && currentCards.stream().allMatch(BookCardComponent::isSelected);
        if (allSelected) {
            for (BookCardComponent c : currentCards) {
                c.setSelected(false);
            }
            selectedBookIds.clear();
        } else {
            for (BookCardComponent c : currentCards) {
                c.setSelected(true);
                selectedBookIds.add(c.getBook().getId());
            }
        }
        updateBulkActionBar();
    }

    private void handleBulkClear() {
        for (BookCardComponent c : currentCards) {
            c.setSelected(false);
        }
        selectedBookIds.clear();
        updateBulkActionBar();
    }

    private void handleBulkMarkCompleted() {
        if (selectedBookIds.isEmpty()) return;
        int count = selectedBookIds.size();
        bookService.bulkMarkAsCompleted(new ArrayList<>(selectedBookIds));
        mainController.showToast(I18n.get("bulk.toast.marked_completed", count), ToastNotification.ToastType.SUCCESS);
        selectedBookIds.clear();
        reloadBooks();
    }

    private void handleBulkDelete() {
        if (selectedBookIds.isEmpty()) return;
        int count = selectedBookIds.size();
        boolean confirm = true;
        if (settingsService.isConfirmDeleteEnabled()) {
            confirm = DialogUtil.confirmDelete(mainController.getPrimaryStage(), I18n.get("bulk.delete.confirm_msg", count));
        }
        if (confirm) {
            bookService.bulkDelete(new ArrayList<>(selectedBookIds));
            mainController.showToast(I18n.get("bulk.toast.deleted", count), ToastNotification.ToastType.SUCCESS);
            selectedBookIds.clear();
            reloadBooks();
        }
    }

    private void handleBulkChangeCategory() {
        if (selectedBookIds.isEmpty()) return;
        int count = selectedBookIds.size();
        Optional<String> res = DialogUtil.promptText(
                mainController.getPrimaryStage(),
                I18n.get("bulk.category.title"),
                I18n.get("bulk.category.header", count),
                I18n.get("bulk.category.prompt"),
                ""
        );
        if (res.isPresent() && !res.get().trim().isEmpty()) {
            String newCat = res.get().trim();
            bookService.bulkUpdateCategory(new ArrayList<>(selectedBookIds), newCat);
            mainController.showToast(I18n.get("bulk.toast.category_updated", count), ToastNotification.ToastType.SUCCESS);
            refreshCategories();
            selectedBookIds.clear();
            reloadBooks();
        }
    }

    private void handleBulkAddTag() {
        if (selectedBookIds.isEmpty()) return;
        int count = selectedBookIds.size();
        Optional<String> res = DialogUtil.promptText(
                mainController.getPrimaryStage(),
                I18n.get("bulk.tag.title"),
                I18n.get("bulk.tag.header", count),
                I18n.get("bulk.tag.prompt"),
                ""
        );
        if (res.isPresent() && !res.get().trim().isEmpty()) {
            String tag = res.get().trim();
            bookService.bulkAddTag(new ArrayList<>(selectedBookIds), tag);
            mainController.showToast(I18n.get("bulk.toast.tag_added", count), ToastNotification.ToastType.SUCCESS);
            refreshTags();
            selectedBookIds.clear();
            reloadBooks();
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
            AnimationUtil.animateCardRemoval(card, () -> {
                bookService.deleteBook(book.getId());
                bookGrid.getChildren().remove(card);
                selectedBookIds.remove(book.getId());
                updateBulkActionBar();
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

    public void focusSearch() {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
        }
    }

    public void clearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
    }
}
