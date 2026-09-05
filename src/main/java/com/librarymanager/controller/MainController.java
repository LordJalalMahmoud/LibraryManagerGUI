package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BackupService;
import com.librarymanager.service.BookService;
import com.librarymanager.service.SampleDataService;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.DateUtil;
import com.librarymanager.util.DialogUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application shell controller orchestrating views, sidebar navigation,
 * header actions, theme and language changes, and toast notifications.
 */
public class MainController extends BorderPane {

    private final BookService bookService;
    private final SettingsService settingsService;
    private final BackupService backupService;
    private final SampleDataService sampleDataService;

    // UI Structure
    private VBox sidebar;
    private HBox header;
    private StackPane contentWrapper;
    private StackPane viewContainer;
    private StackPane dragOverlay;
    private VBox toastContainer;
    private Label pageTitleLabel;
    private Label pageSubtitleLabel;
    private Button themeToggleBtn;
    private Button addBookBtn;
    private SVGPath themeIcon;

    // Nav elements
    private final List<Button> navButtons = new ArrayList<>();
    private Button navDashboard;
    private Button navAll;
    private Button navReading;
    private Button navCompleted;
    private Button navNotStarted;
    private Button navDataManagement;
    private Button navSettings;
    private Button currentActiveNav;

    private Label brandTitleLabel;
    private Label brandSubtitleLabel;
    private Label overviewSectionLabel;
    private Label collectionSectionLabel;
    private Label systemSectionLabel;

    // Active sub-controllers
    private DashboardController dashboardController;
    private LibraryController libraryController;
    private DataManagementController dataManagementController;
    private SettingsController settingsController;

    private Stage primaryStage;

    public MainController(Stage stage) {
        this.primaryStage = stage;
        this.bookService = new BookService();
        this.settingsService = new SettingsService();
        this.backupService = new BackupService();
        this.sampleDataService = new SampleDataService(bookService);

        buildUi();
        setupThemeAndLanguageHandling();
        navigateToDashboard();
    }

    private void buildUi() {
        getStyleClass().add("app-shell");
        updateOrientation();

        // 1. Sidebar
        sidebar = buildSidebar();
        setLeft(sidebar);

        // 2. Main content area with header and view container
        VBox centerLayout = new VBox();
        centerLayout.setFillWidth(true);
        VBox.setVgrow(centerLayout, Priority.ALWAYS);

        header = buildHeader();
        centerLayout.getChildren().add(header);

        contentWrapper = new StackPane();
        VBox.setVgrow(contentWrapper, Priority.ALWAYS);

        viewContainer = new StackPane();
        viewContainer.setPadding(new Insets(20, 28, 24, 28));

        // Toast Container positioned top-right or top-left depending on RTL
        toastContainer = new VBox(10);
        toastContainer.setMouseTransparent(false);
        toastContainer.setPickOnBounds(false);
        toastContainer.setAlignment(I18n.isRTL() ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
        toastContainer.setPadding(new Insets(20, 24, 20, 20));
        StackPane.setAlignment(toastContainer, I18n.isRTL() ? Pos.TOP_LEFT : Pos.TOP_RIGHT);

        dragOverlay = buildDragOverlay();

        contentWrapper.getChildren().addAll(viewContainer, dragOverlay, toastContainer);
        centerLayout.getChildren().add(contentWrapper);

        setCenter(centerLayout);

        setupDragAndDrop();
        setupShortcuts();
    }

    private void updateOrientation() {
        setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        if (primaryStage != null) {
            primaryStage.setTitle(I18n.get("app.window.title"));
        }
    }

    private VBox buildSidebar() {
        VBox box = new VBox(6);
        box.getStyleClass().add("sidebar");
        box.setPrefWidth(240);
        box.setMinWidth(220);
        box.setMaxWidth(260);
        box.setPadding(new Insets(0, 12, 0, 12));

        // Brand Box
        HBox brand = new HBox(12);
        brand.getStyleClass().add("brand-box");
        brand.setAlignment(Pos.CENTER_LEFT);

        StackPane brandIconBox = new StackPane();
        brandIconBox.getStyleClass().add("brand-icon-box");
        SVGPath logoIcon = IconUtil.createIcon(IconUtil.IconType.LIBRARY, 20);
        logoIcon.getStyleClass().add("app-icon");
        logoIcon.setStyle("-fx-fill: -accent-primary;");
        brandIconBox.getChildren().add(logoIcon);

        VBox brandText = new VBox(2);
        brandTitleLabel = new Label(I18n.get("app.name"));
        brandTitleLabel.getStyleClass().add("brand-title");

        brandSubtitleLabel = new Label(I18n.get("app.subtitle"));
        brandSubtitleLabel.getStyleClass().add("brand-subtitle");

        brandText.getChildren().addAll(brandTitleLabel, brandSubtitleLabel);
        brand.getChildren().addAll(brandIconBox, brandText);

        box.getChildren().add(brand);

        // Section: Overview
        overviewSectionLabel = new Label(I18n.get("nav.overview"));
        overviewSectionLabel.getStyleClass().add("nav-section-title");
        box.getChildren().add(overviewSectionLabel);

        navDashboard = createNavButton(I18n.get("nav.dashboard"), IconUtil.IconType.DASHBOARD, this::navigateToDashboard);
        box.getChildren().add(navDashboard);

        // Section: Collection
        collectionSectionLabel = new Label(I18n.get("nav.collection"));
        collectionSectionLabel.getStyleClass().add("nav-section-title");
        box.getChildren().add(collectionSectionLabel);

        navAll = createNavButton(I18n.get("nav.all"), IconUtil.IconType.LIBRARY, () -> navigateToLibrary(null));
        navReading = createNavButton(I18n.get("nav.reading"), IconUtil.IconType.READING, () -> navigateToLibrary(ReadingStatus.READING));
        navCompleted = createNavButton(I18n.get("nav.completed"), IconUtil.IconType.COMPLETED, () -> navigateToLibrary(ReadingStatus.COMPLETED));
        navNotStarted = createNavButton(I18n.get("nav.not_started"), IconUtil.IconType.NOT_STARTED, () -> navigateToLibrary(ReadingStatus.NOT_STARTED));

        box.getChildren().addAll(navAll, navReading, navCompleted, navNotStarted);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        // Section: System
        systemSectionLabel = new Label(I18n.get("nav.system"));
        systemSectionLabel.getStyleClass().add("nav-section-title");
        box.getChildren().add(systemSectionLabel);

        navDataManagement = createNavButton(I18n.get("nav.data_management"), IconUtil.IconType.DATABASE, this::navigateToDataManagement);
        navSettings = createNavButton(I18n.get("nav.settings"), IconUtil.IconType.SETTINGS, this::navigateToSettings);
        box.getChildren().addAll(navDataManagement, navSettings);

        Region bottomPad = new Region();
        bottomPad.setPrefHeight(16);
        box.getChildren().add(bottomPad);

        return box;
    }

    private Button createNavButton(String text, IconUtil.IconType iconType, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        SVGPath icon = IconUtil.createIcon(iconType, 16);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(12);

        btn.setOnAction(e -> {
            setActiveNavButton(btn);
            action.run();
        });

        navButtons.add(btn);
        return btn;
    }

    private void setActiveNavButton(Button activeBtn) {
        this.currentActiveNav = activeBtn;
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
    }

    private HBox buildHeader() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("app-header");
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        pageTitleLabel = new Label(I18n.get("header.dashboard.title"));
        pageTitleLabel.getStyleClass().add("page-title");

        pageSubtitleLabel = new Label(I18n.get("header.dashboard.subtitle"));
        pageSubtitleLabel.getStyleClass().add("page-subtitle");

        titleBox.getChildren().addAll(pageTitleLabel, pageSubtitleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Theme Toggle Button
        themeToggleBtn = new Button();
        themeToggleBtn.getStyleClass().addAll("btn", "btn-icon");
        themeToggleBtn.setTooltip(new Tooltip(I18n.get("header.theme_toggle")));
        updateThemeIcon();
        themeToggleBtn.setOnAction(e -> {
            settingsService.toggleTheme();
            updateThemeIcon();
            showToast(I18n.get("toast.theme_switched", settingsService.getTheme().toLowerCase()), ToastNotification.ToastType.INFO);
        });

        // "+ Add Book" Header Button
        addBookBtn = new Button(I18n.get("header.add_book"));
        addBookBtn.getStyleClass().addAll("btn", "btn-primary");
        SVGPath plusIcon = IconUtil.createIcon(IconUtil.IconType.PLUS, 14);
        addBookBtn.setGraphic(plusIcon);
        addBookBtn.setGraphicTextGap(8);
        addBookBtn.setOnAction(e -> openBookFormDialog(null));

        bar.getChildren().addAll(titleBox, themeToggleBtn, addBookBtn);
        return bar;
    }

    private void updateThemeIcon() {
        if (settingsService.isHighContrast()) {
            themeIcon = IconUtil.createIcon(IconUtil.IconType.SETTINGS, 16);
        } else if (settingsService.isDarkMode()) {
            themeIcon = IconUtil.createIcon(IconUtil.IconType.SUN, 16);
        } else {
            themeIcon = IconUtil.createIcon(IconUtil.IconType.MOON, 16);
        }
        themeToggleBtn.setGraphic(themeIcon);
    }

    private void setupThemeAndLanguageHandling() {
        applyTheme(settingsService.getTheme());
        applyFontSizeScale(settingsService.getFontSizeScale());
        settingsService.addThemeChangeListener(this::applyTheme);
        settingsService.addLanguageChangeListener(lang -> handleLanguageChanged());
        settingsService.addFontSizeChangeListener(this::applyFontSizeScale);
    }

    private void handleLanguageChanged() {
        Platform.runLater(() -> {
            updateOrientation();

            // Update Brand Labels
            brandTitleLabel.setText(I18n.get("app.name"));
            brandSubtitleLabel.setText(I18n.get("app.subtitle"));
            overviewSectionLabel.setText(I18n.get("nav.overview"));
            collectionSectionLabel.setText(I18n.get("nav.collection"));
            systemSectionLabel.setText(I18n.get("nav.system"));

            // Update Nav Buttons
            navDashboard.setText(I18n.get("nav.dashboard"));
            navAll.setText(I18n.get("nav.all"));
            navReading.setText(I18n.get("nav.reading"));
            navCompleted.setText(I18n.get("nav.completed"));
            navNotStarted.setText(I18n.get("nav.not_started"));
            navDataManagement.setText(I18n.get("nav.data_management"));
            navSettings.setText(I18n.get("nav.settings"));

            // Update Header
            addBookBtn.setText(I18n.get("header.add_book"));
            themeToggleBtn.setTooltip(new Tooltip(I18n.get("header.theme_toggle")));

            // Toast alignment
            toastContainer.setAlignment(I18n.isRTL() ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
            StackPane.setAlignment(toastContainer, I18n.isRTL() ? Pos.TOP_LEFT : Pos.TOP_RIGHT);

            // Invalidate controllers so they re-create views with new language
            dashboardController = null;
            libraryController = null;
            dataManagementController = null;
            settingsController = null;

            if (currentActiveNav == navDataManagement) {
                navigateToDataManagement();
            } else if (currentActiveNav == navSettings) {
                navigateToSettings();
            } else if (currentActiveNav == navAll || currentActiveNav == navReading || currentActiveNav == navCompleted || currentActiveNav == navNotStarted) {
                navigateToLibrary(null);
            } else {
                navigateToDashboard();
            }

            String langName = settingsService.isArabic() ? "العربية" : "English";
            showToast(I18n.get("toast.lang_switched", langName), ToastNotification.ToastType.INFO);
        });
    }

    public void applyTheme(String theme) {
        Platform.runLater(() -> {
            if (getScene() != null) {
                getScene().getStylesheets().clear();
                String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
                String themeStyle;
                if (SettingsService.THEME_HIGH_CONTRAST.equalsIgnoreCase(theme)) {
                    themeStyle = getClass().getResource("/css/theme-high-contrast.css").toExternalForm();
                } else if (SettingsService.THEME_LIGHT.equalsIgnoreCase(theme)) {
                    themeStyle = getClass().getResource("/css/theme-light.css").toExternalForm();
                } else {
                    themeStyle = getClass().getResource("/css/theme-dark.css").toExternalForm();
                }
                getScene().getStylesheets().addAll(themeStyle, baseStyle);
            }
            updateThemeIcon();
        });
    }

    public void applyFontSizeScale(String scale) {
        Platform.runLater(() -> {
            getStyleClass().removeAll("font-scale-normal", "font-scale-large", "font-scale-extra-large");
            String cssClass = switch (scale != null ? scale.toUpperCase() : "") {
                case SettingsService.FONT_SCALE_LARGE -> "font-scale-large";
                case SettingsService.FONT_SCALE_EXTRA_LARGE -> "font-scale-extra-large";
                default -> "font-scale-normal";
            };
            getStyleClass().add(cssClass);
        });
    }

    public void switchView(Node newView, String title, String subtitle) {
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);

        if (viewContainer.getChildren().isEmpty()) {
            viewContainer.getChildren().setAll(newView);
            AnimationUtil.slideFadeIn(newView, Duration.millis(220), 12.0, null);
        } else {
            Node oldView = viewContainer.getChildren().get(0);
            AnimationUtil.fadeOut(oldView, Duration.millis(120), () -> {
                viewContainer.getChildren().setAll(newView);
                AnimationUtil.slideFadeIn(newView, Duration.millis(180), 12.0, null);
            });
        }
    }

    public void navigateToDashboard() {
        setActiveNavButton(navDashboard);
        if (dashboardController == null) {
            dashboardController = new DashboardController(this, bookService, sampleDataService);
        }
        dashboardController.refresh();
        switchView(dashboardController.getView(), I18n.get("header.dashboard.title"), I18n.get("header.dashboard.subtitle"));
    }

    public void navigateToLibrary(ReadingStatus initialFilter) {
        if (initialFilter == null) {
            setActiveNavButton(navAll);
        } else if (initialFilter == ReadingStatus.READING) {
            setActiveNavButton(navReading);
        } else if (initialFilter == ReadingStatus.COMPLETED) {
            setActiveNavButton(navCompleted);
        } else if (initialFilter == ReadingStatus.NOT_STARTED) {
            setActiveNavButton(navNotStarted);
        }

        if (libraryController == null) {
            libraryController = new LibraryController(this, bookService, settingsService);
        }
        libraryController.applyFilter(initialFilter);
        switchView(libraryController.getView(), I18n.get("header.library.title"), I18n.get("header.library.subtitle"));
    }

    public void navigateToBookDetails(Book book) {
        BookDetailsController detailsController = new BookDetailsController(this, bookService, settingsService, book);
        switchView(detailsController.getView(), I18n.get("header.details.title"), book.getTitle());
    }

    public void navigateToSettings() {
        setActiveNavButton(navSettings);
        if (settingsController == null) {
            settingsController = new SettingsController(this, settingsService, backupService, sampleDataService, bookService);
        }
        settingsController.refreshInfo();
        switchView(settingsController.getView(), I18n.get("header.settings.title"), I18n.get("header.settings.subtitle"));
    }

    public void navigateToDataManagement() {
        setActiveNavButton(navDataManagement);
        if (dataManagementController == null) {
            dataManagementController = new DataManagementController(this, backupService, settingsService);
        }
        dataManagementController.refresh();
        switchView(dataManagementController.getView(), I18n.get("header.data_management.title"), I18n.get("header.data_management.subtitle"));
    }

    public void openBookFormDialog(Book bookToEdit) {
        BookFormController formController = new BookFormController(this, bookService, bookToEdit);
        formController.showAsDialog(primaryStage);
    }

    public void openBookFormDialogWithDefaults(String title, String coverPath, String notes) {
        BookFormController formController = new BookFormController(this, bookService, null);
        formController.setInitialValues(title, coverPath, notes);
        formController.showAsDialog(primaryStage);
    }

    public void openActiveReadingSessionDialog(Book book) {
        if (book == null) {
            // Pick currently reading book first, or any uncompleted book
            List<Book> readingBooks = bookService.getBooksByStatus(ReadingStatus.READING);
            if (!readingBooks.isEmpty()) {
                book = readingBooks.get(0);
            } else {
                List<Book> allBooks = bookService.getAllBooks();
                for (Book b : allBooks) {
                    if (b.getStatus() != ReadingStatus.COMPLETED) {
                        book = b;
                        break;
                    }
                }
            }
        }

        if (book == null) {
            showToast(I18n.get("experience.no_books_to_read"), ToastNotification.ToastType.INFO);
            return;
        }

        ActiveReadingSessionDialog dialog = new ActiveReadingSessionDialog(
                this,
                bookService.getReadingTrackerService(),
                book,
                this::refreshActiveViews
        );
        dialog.showAsDialog(primaryStage);
    }

    private StackPane buildDragOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("drag-overlay");
        overlay.setVisible(false);
        overlay.setMouseTransparent(true);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);

        SVGPath dropIcon = IconUtil.createIcon(IconUtil.IconType.LIBRARY, 48);
        dropIcon.setStyle("-fx-fill: -accent-primary;");

        Label titleLabel = new Label(I18n.get("dragdrop.overlay.title"));
        titleLabel.getStyleClass().add("drag-overlay-title");

        Label subLabel = new Label(I18n.get("dragdrop.overlay.sub"));
        subLabel.getStyleClass().add("drag-overlay-subtitle");

        content.getChildren().addAll(dropIcon, titleLabel, subLabel);
        overlay.getChildren().add(content);

        return overlay;
    }

    private void setupDragAndDrop() {
        this.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                dragOverlay.setVisible(true);
            }
            event.consume();
        });

        this.setOnDragExited(event -> {
            dragOverlay.setVisible(false);
            event.consume();
        });

        this.setOnDragDropped(event -> {
            dragOverlay.setVisible(false);
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                handleDroppedFiles(db.getFiles());
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void handleDroppedFiles(List<File> files) {
        if (files == null || files.isEmpty()) return;

        List<File> bookFiles = new ArrayList<>();
        File imageFile = null;
        File jsonBackup = null;

        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".pdf") || name.endsWith(".epub") || name.endsWith(".mobi") || name.endsWith(".txt") || name.endsWith(".azw3")) {
                bookFiles.add(f);
            } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp")) {
                if (imageFile == null) imageFile = f;
            } else if (name.endsWith(".json")) {
                jsonBackup = f;
            }
        }

        if (jsonBackup != null) {
            final File finalJson = jsonBackup;
            Platform.runLater(() -> {
                boolean confirmed = DialogUtil.confirm(
                        primaryStage,
                        I18n.get("data.import.dialog.title"),
                        I18n.get("dragdrop.toast.backup_detected"),
                        "Import data snapshot from " + finalJson.getName() + "?"
                );
                if (confirmed) {
                    try {
                        var res = backupService.importJson(finalJson, false);
                        refreshActiveViews();
                        showToast(I18n.get("data.toast.import_json_success", res.getBooksAdded(), res.getChaptersAdded(), res.getSessionsAdded()), ToastNotification.ToastType.SUCCESS);
                    } catch (Exception ex) {
                        showToast("Failed to import JSON: " + ex.getMessage(), ToastNotification.ToastType.ERROR);
                    }
                }
            });
            return;
        }

        if (bookFiles.size() == 1) {
            File f = bookFiles.get(0);
            String title = f.getName();
            int dot = title.lastIndexOf('.');
            if (dot > 0) title = title.substring(0, dot);
            String cover = imageFile != null ? imageFile.getAbsolutePath() : null;
            openBookFormDialogWithDefaults(title, cover, "Imported file: " + f.getAbsolutePath());
            showToast(I18n.get("dragdrop.toast.single_book"), ToastNotification.ToastType.SUCCESS);
        } else if (bookFiles.size() > 1) {
            int added = 0;
            for (File f : bookFiles) {
                String title = f.getName();
                int dot = title.lastIndexOf('.');
                if (dot > 0) title = title.substring(0, dot);
                Book b = new Book();
                b.setTitle(title);
                b.setAuthor("Unknown Author");
                b.setTotalPages(100);
                b.setTotalParts(1);
                b.setCurrentPage(0);
                b.setStatus(ReadingStatus.NOT_STARTED);
                b.setDescription("Imported file: " + f.getAbsolutePath());
                if (imageFile != null) {
                    b.setCoverImage(imageFile.getAbsolutePath());
                }
                bookService.addBook(b);
                added++;
            }
            refreshActiveViews();
            showToast(I18n.get("dragdrop.toast.multiple_books", added), ToastNotification.ToastType.SUCCESS);
        } else if (imageFile != null) {
            openBookFormDialogWithDefaults(null, imageFile.getAbsolutePath(), null);
            showToast(I18n.get("dragdrop.toast.cover_loaded"), ToastNotification.ToastType.INFO);
        }
    }

    private void setupShortcuts() {
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerAccelerators(newScene);
            }
        });
    }

    private void registerAccelerators(Scene scene) {
        if (scene == null) return;

        // Accelerators
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), () -> openBookFormDialog(null));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), this::navigateToLibraryWithSearchFocus);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN), this::navigateToDashboard);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN), () -> navigateToLibrary(null));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN), () -> navigateToLibrary(ReadingStatus.READING));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.SHORTCUT_DOWN), () -> navigateToLibrary(ReadingStatus.COMPLETED));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.SHORTCUT_DOWN), this::navigateToDataManagement);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), this::navigateToSettings);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), () -> {
            settingsService.toggleTheme();
            showToast(I18n.get("toast.theme_switched", settingsService.getTheme().toLowerCase()), ToastNotification.ToastType.INFO);
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), () -> openActiveReadingSessionDialog(null));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::triggerQuickBackup);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::openShortcutsDialog);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.SHORTCUT_DOWN), this::openShortcutsDialog);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), () -> {
            if (dashboardController != null) {
                dashboardController.openCustomizationDialog();
            }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (libraryController != null) {
                    libraryController.clearSearch();
                }
            }
        });
    }

    public void triggerQuickBackup() {
        try {
            var backup = backupService.createBackup(com.librarymanager.model.BackupType.MANUAL, "Quick Backup (" + DateUtil.formatDateTime(LocalDateTime.now()) + ")");
            showToast(I18n.get("data.toast.backup_created", backup.getFilename()), ToastNotification.ToastType.SUCCESS);
            refreshActiveViews();
        } catch (Exception e) {
            showToast("Backup failed: " + e.getMessage(), ToastNotification.ToastType.ERROR);
        }
    }

    public void openShortcutsDialog() {
        new KeyboardShortcutsDialog(settingsService).show(primaryStage);
    }

    public void navigateToLibraryWithSearchFocus() {
        navigateToLibrary(null);
        Platform.runLater(() -> {
            if (libraryController != null) {
                libraryController.focusSearch();
            }
        });
    }

    public void showToast(String message, ToastNotification.ToastType type) {
        Platform.runLater(() -> ToastNotification.show(toastContainer, message, type));
    }

    public void refreshActiveViews() {
        if (dashboardController != null) {
            dashboardController.refresh();
        }
        if (libraryController != null) {
            libraryController.refreshCategories();
            libraryController.reloadBooks();
        }
        if (dataManagementController != null) {
            dataManagementController.refresh();
        }
        if (settingsController != null) {
            settingsController.refreshInfo();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public BookService getBookService() {
        return bookService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public BackupService getBackupService() {
        return backupService;
    }
}
