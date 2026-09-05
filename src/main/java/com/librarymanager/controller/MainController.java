package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BackupService;
import com.librarymanager.service.BookService;
import com.librarymanager.service.SampleDataService;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

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

        contentWrapper.getChildren().addAll(viewContainer, toastContainer);
        centerLayout.getChildren().add(contentWrapper);

        setCenter(centerLayout);
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

        navSettings = createNavButton(I18n.get("nav.settings"), IconUtil.IconType.SETTINGS, this::navigateToSettings);
        box.getChildren().add(navSettings);

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
        if (settingsService.isDarkMode()) {
            themeIcon = IconUtil.createIcon(IconUtil.IconType.SUN, 16);
        } else {
            themeIcon = IconUtil.createIcon(IconUtil.IconType.MOON, 16);
        }
        themeToggleBtn.setGraphic(themeIcon);
    }

    private void setupThemeAndLanguageHandling() {
        applyTheme(settingsService.getTheme());
        settingsService.addThemeChangeListener(this::applyTheme);
        settingsService.addLanguageChangeListener(lang -> handleLanguageChanged());
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
            settingsController = null;

            if (currentActiveNav == navSettings) {
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
                String themeStyle = SettingsService.THEME_LIGHT.equalsIgnoreCase(theme)
                        ? getClass().getResource("/css/theme-light.css").toExternalForm()
                        : getClass().getResource("/css/theme-dark.css").toExternalForm();
                getScene().getStylesheets().addAll(themeStyle, baseStyle);
            }
            updateThemeIcon();
        });
    }

    public void switchView(Node newView, String title, String subtitle) {
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);

        if (viewContainer.getChildren().isEmpty()) {
            viewContainer.getChildren().setAll(newView);
            AnimationUtil.fadeIn(newView, Duration.millis(200), null);
        } else {
            Node oldView = viewContainer.getChildren().get(0);
            AnimationUtil.fadeOut(oldView, Duration.millis(120), () -> {
                viewContainer.getChildren().setAll(newView);
                AnimationUtil.fadeIn(newView, Duration.millis(180), null);
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

    public void openBookFormDialog(Book bookToEdit) {
        BookFormController formController = new BookFormController(this, bookService, bookToEdit);
        formController.showAsDialog(primaryStage);
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
}
