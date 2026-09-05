package com.librarymanager.controller;

import com.librarymanager.component.BookCardComponent;
import com.librarymanager.component.StatCardComponent;
import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.*;
import com.librarymanager.service.BookService;
import com.librarymanager.service.ReadingTrackerService;
import com.librarymanager.service.SampleDataService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.DateUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the enhanced v1.3 Dashboard screen:
 * - Books read per month & Pages read per month interactive trends
 * - Total reading time & average reading speed
 * - Most-read authors leaderboard & categories distribution
 * - Interactive Yearly Reading Summary (Year in Review)
 * - Reading streaks, goals, habits, and recent activities.
 */
public class DashboardController {

    public enum MonthlyViewMode {
        PAGES,
        BOOKS,
        TIME
    }

    private final MainController mainController;
    private final BookService bookService;
    private final ReadingTrackerService readingTrackerService;
    private final SampleDataService sampleDataService;

    private final ScrollPane rootScrollPane;
    private final VBox contentBox;

    // 1. KPI Stat Cards
    private StatCardComponent totalBooksCard;
    private StatCardComponent completedCard;
    private StatCardComponent pagesCard;
    private StatCardComponent timeCard;
    private StatCardComponent speedCard;
    private StatCardComponent streakCard;

    // 2. Yearly Reading Summary Card
    private Label yearlySubtitleLabel;
    private ComboBox<Integer> yearSelector;
    private GridPane yearlyGrid;
    private Label yBooksValLabel, yBooksSubLabel;
    private ProgressBar yBooksBar;
    private Label yPagesValLabel, yPagesSubLabel;
    private Label yTimeValLabel, yTimeSubLabel;
    private Label ySpeedValLabel, ySpeedSubLabel;
    private Label yAuthorValLabel, yAuthorSubLabel;
    private Label yCategoryValLabel, yCategorySubLabel;

    // 3. Monthly Activity Bar Chart
    private BarChart<String, Number> monthlyChart;
    private CategoryAxis monthXAxis;
    private NumberAxis monthYAxis;
    private MonthlyViewMode currentMonthlyMode = MonthlyViewMode.PAGES;
    private Button btnPagesMode, btnBooksMode, btnTimeMode;

    // 4. Categories & Top Authors
    private PieChart categoriesPieChart;
    private Label categoriesEmptyLabel;
    private VBox topAuthorsContainer;

    // 5. Habits & Goals Card
    private StatCardComponent dailyAvgCard;
    private Label dailyGoalLabel;
    private ProgressBar dailyGoalBar;
    private Label yearlyGoalLabel;
    private ProgressBar yearlyGoalBar;

    // 6. Content feeds
    private FlowPane currentlyReadingSection;
    private VBox readingSectionContainer;
    private FlowPane recentSessionsSection;
    private VBox recentSessionsContainer;
    private FlowPane recentBooksSection;
    private VBox recentSectionContainer;
    private VBox emptyPromptBox;

    // Modular section containers for customization
    private HBox kpiStatsRow;
    private VBox yearlySummaryCard;
    private VBox monthlyActivityCard;
    private HBox splitSection;
    private HBox habitsRow;
    private HBox customizeHeaderBar;

    public DashboardController(MainController mainController, BookService bookService, SampleDataService sampleDataService) {
        this.mainController = mainController;
        this.bookService = bookService;
        this.readingTrackerService = bookService.getReadingTrackerService();
        this.sampleDataService = sampleDataService;

        contentBox = new VBox(24);
        contentBox.setPadding(new Insets(0, 10, 30, 0));
        contentBox.setFillWidth(true);

        rootScrollPane = new ScrollPane(contentBox);
        rootScrollPane.setFitToWidth(true);
        rootScrollPane.getStyleClass().add("scroll-pane");
        rootScrollPane.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        buildDashboard();
    }

    public Node getView() {
        return rootScrollPane;
    }

    private void buildDashboard() {
        // 0. Top Bar with Customize Dashboard action
        customizeHeaderBar = buildCustomizeHeaderBar();

        // 1. Top KPI Statistics Ribbon (6 cards)
        kpiStatsRow = buildKpiStatsRow();

        // 2. Empty prompt if library is empty
        emptyPromptBox = buildEmptyPromptBox();

        // 3. Yearly Reading Summary Card (Year in Review)
        yearlySummaryCard = buildYearlySummaryCard();

        // 4. Monthly Reading Activity & Trends Chart
        monthlyActivityCard = buildMonthlyTrendsCard();

        // 5. Split Row: Top Categories & Top Authors Leaderboard
        splitSection = buildCategoriesAndAuthorsRow();

        // 6. Reading Habits & Goals Row
        habitsRow = buildHabitsAndGoalsRow();

        // 7. Currently Reading Feed
        readingSectionContainer = new VBox(12);
        Label readingHeading = new Label(I18n.get("dashboard.currently_reading"));
        readingHeading.getStyleClass().add("stat-title");
        readingHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        currentlyReadingSection = new FlowPane();
        currentlyReadingSection.setHgap(16);
        currentlyReadingSection.setVgap(16);
        readingSectionContainer.getChildren().addAll(readingHeading, currentlyReadingSection);

        // 8. Recent Reading Sessions Feed
        recentSessionsContainer = new VBox(12);
        Label sessionsHeading = new Label(I18n.get("dashboard.recent_sessions"));
        sessionsHeading.getStyleClass().add("stat-title");
        sessionsHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        recentSessionsSection = new FlowPane();
        recentSessionsSection.setHgap(16);
        recentSessionsSection.setVgap(16);
        recentSessionsContainer.getChildren().addAll(sessionsHeading, recentSessionsSection);

        // 9. Recently Added Books Section
        recentSectionContainer = new VBox(12);
        Label recentHeading = new Label(I18n.get("dashboard.recently_added"));
        recentHeading.getStyleClass().add("stat-title");
        recentHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        recentBooksSection = new FlowPane();
        recentBooksSection.setHgap(16);
        recentBooksSection.setVgap(16);
        recentSectionContainer.getChildren().addAll(recentHeading, recentBooksSection);

        rebuildDashboardLayout();
    }

    private HBox buildCustomizeHeaderBar() {
        HBox bar = new HBox();
        bar.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button customizeBtn = new Button(I18n.get("dashboard.customize.btn"));
        customizeBtn.getStyleClass().addAll("btn", "btn-secondary");
        SVGPath slidersIcon = IconUtil.createIcon(IconUtil.IconType.SETTINGS, 13);
        customizeBtn.setGraphic(slidersIcon);
        customizeBtn.setGraphicTextGap(6);
        customizeBtn.setOnAction(e -> openCustomizationDialog());

        bar.getChildren().add(customizeBtn);
        return bar;
    }

    public void openCustomizationDialog() {
        new DashboardCustomizationDialog(mainController, mainController.getSettingsService(), this)
                .show(mainController.getPrimaryStage());
    }

    public void rebuildDashboardLayout() {
        contentBox.getChildren().clear();
        contentBox.getChildren().addAll(customizeHeaderBar, emptyPromptBox);

        List<String> order = mainController.getSettingsService().getDashboardSectionOrder();
        for (String section : order) {
            if (!mainController.getSettingsService().isDashboardSectionVisible(section)) {
                continue;
            }
            switch (section) {
                case com.librarymanager.service.SettingsService.SECTION_METRICS ->
                        contentBox.getChildren().add(kpiStatsRow);
                case com.librarymanager.service.SettingsService.SECTION_YEARLY ->
                        contentBox.getChildren().add(yearlySummaryCard);
                case com.librarymanager.service.SettingsService.SECTION_CHARTS -> {
                    contentBox.getChildren().add(monthlyActivityCard);
                    contentBox.getChildren().add(splitSection);
                }
                case com.librarymanager.service.SettingsService.SECTION_GOALS ->
                        contentBox.getChildren().add(habitsRow);
                case com.librarymanager.service.SettingsService.SECTION_CURRENTLY_READING ->
                        contentBox.getChildren().add(readingSectionContainer);
                case com.librarymanager.service.SettingsService.SECTION_RECENT_SESSIONS ->
                        contentBox.getChildren().add(recentSessionsContainer);
                case com.librarymanager.service.SettingsService.SECTION_RECENT_BOOKS ->
                        contentBox.getChildren().add(recentSectionContainer);
            }
        }
    }

    private HBox buildKpiStatsRow() {
        HBox row = new HBox(16);
        row.setFillHeight(true);

        totalBooksCard = new StatCardComponent(I18n.get("stat.total_books"), "0", "", IconUtil.IconType.LIBRARY, "stat-accent-indigo");
        completedCard = new StatCardComponent(I18n.get("stat.completed"), "0", I18n.get("stat.completed.sub"), IconUtil.IconType.COMPLETED, "stat-accent-emerald");
        pagesCard = new StatCardComponent(I18n.get("chart.pages_read"), "0", "", IconUtil.IconType.PAGES, "stat-accent-blue");
        timeCard = new StatCardComponent(I18n.get("stat.reading_time"), "0m", "", IconUtil.IconType.CLOCK, "stat-accent-purple");
        speedCard = new StatCardComponent(I18n.get("stat.reading_speed"), "0", "", IconUtil.IconType.SPEED, "stat-accent-teal");
        streakCard = new StatCardComponent(I18n.get("stat.streak"), "0", "", IconUtil.IconType.FIRE, "stat-accent-rose");

        row.getChildren().addAll(totalBooksCard, completedCard, pagesCard, timeCard, speedCard, streakCard);
        return row;
    }

    private VBox buildYearlySummaryCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("yearly-summary-card");
        card.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Header
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane trophyBox = new StackPane();
        trophyBox.getStyleClass().add("stat-icon-container");
        trophyBox.setStyle("-fx-background-color: rgba(234, 179, 8, 0.15);");
        trophyBox.setMinSize(38, 38);
        trophyBox.setMaxSize(38, 38);
        SVGPath trophy = IconUtil.createIcon(IconUtil.IconType.TROPHY, 18);
        trophy.setStyle("-fx-fill: #eab308;");
        trophyBox.getChildren().add(trophy);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(I18n.get("stat.yearly_summary.title"));
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        yearlySubtitleLabel = new Label("");
        yearlySubtitleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(titleLabel, yearlySubtitleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        HBox selectorBox = new HBox(8);
        selectorBox.setAlignment(Pos.CENTER_RIGHT);
        Label yearPrompt = new Label(I18n.get("stat.yearly_summary.select_year"));
        yearPrompt.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-muted;");

        yearSelector = new ComboBox<>();
        yearSelector.setStyle("-fx-font-size: 12px; -fx-pref-width: 100px; -fx-cursor: hand;");
        yearSelector.setOnAction(e -> {
            Integer sel = yearSelector.getValue();
            if (sel != null) {
                renderYearlySummary(sel);
                renderMonthlyChart(sel);
            }
        });
        selectorBox.getChildren().addAll(yearPrompt, yearSelector);

        headerRow.getChildren().addAll(trophyBox, titleBox, selectorBox);

        // Grid of 6 mini-cards
        yearlyGrid = new GridPane();
        yearlyGrid.setHgap(16);
        yearlyGrid.setVgap(14);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(33.333);
            col.setHgrow(Priority.ALWAYS);
            yearlyGrid.getColumnConstraints().add(col);
        }

        // 1. Books Completed Card with progress bar
        VBox b1 = new VBox(6);
        b1.getStyleClass().add("yearly-mini-card");
        Label t1 = new Label(I18n.get("stat.yearly_summary.books_completed"));
        t1.getStyleClass().add("yearly-mini-title");
        yBooksValLabel = new Label("0");
        yBooksValLabel.getStyleClass().add("yearly-mini-value");
        yBooksBar = new ProgressBar(0.0);
        yBooksBar.setMaxWidth(Double.MAX_VALUE);
        yBooksBar.setPrefHeight(6);
        yBooksBar.getStyleClass().add("book-progress-bar");
        yBooksSubLabel = new Label("");
        yBooksSubLabel.getStyleClass().add("yearly-mini-sub");
        b1.getChildren().addAll(t1, yBooksValLabel, yBooksBar, yBooksSubLabel);
        yearlyGrid.add(b1, 0, 0);

        // 2. Pages Read Card
        VBox b2 = new VBox(6);
        b2.getStyleClass().add("yearly-mini-card");
        Label t2 = new Label(I18n.get("stat.yearly_summary.pages_read"));
        t2.getStyleClass().add("yearly-mini-title");
        yPagesValLabel = new Label("0");
        yPagesValLabel.getStyleClass().add("yearly-mini-value");
        yPagesSubLabel = new Label("");
        yPagesSubLabel.getStyleClass().add("yearly-mini-sub");
        b2.getChildren().addAll(t2, yPagesValLabel, yPagesSubLabel);
        yearlyGrid.add(b2, 1, 0);

        // 3. Time Spent Reading
        VBox b3 = new VBox(6);
        b3.getStyleClass().add("yearly-mini-card");
        Label t3 = new Label(I18n.get("stat.yearly_summary.time_spent"));
        t3.getStyleClass().add("yearly-mini-title");
        yTimeValLabel = new Label("0m");
        yTimeValLabel.getStyleClass().add("yearly-mini-value");
        yTimeSubLabel = new Label("");
        yTimeSubLabel.getStyleClass().add("yearly-mini-sub");
        b3.getChildren().addAll(t3, yTimeValLabel, yTimeSubLabel);
        yearlyGrid.add(b3, 2, 0);

        // 4. Average Speed Card
        VBox b4 = new VBox(6);
        b4.getStyleClass().add("yearly-mini-card");
        Label t4 = new Label(I18n.get("stat.yearly_summary.avg_speed"));
        t4.getStyleClass().add("yearly-mini-title");
        ySpeedValLabel = new Label("—");
        ySpeedValLabel.getStyleClass().add("yearly-mini-value");
        ySpeedSubLabel = new Label("");
        ySpeedSubLabel.getStyleClass().add("yearly-mini-sub");
        b4.getChildren().addAll(t4, ySpeedValLabel, ySpeedSubLabel);
        yearlyGrid.add(b4, 0, 1);

        // 5. Top Author Card
        VBox b5 = new VBox(6);
        b5.getStyleClass().add("yearly-mini-card");
        Label t5 = new Label(I18n.get("stat.yearly_summary.top_author"));
        t5.getStyleClass().add("yearly-mini-title");
        yAuthorValLabel = new Label("—");
        yAuthorValLabel.getStyleClass().add("yearly-mini-value");
        yAuthorValLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");
        yAuthorSubLabel = new Label("");
        yAuthorSubLabel.getStyleClass().add("yearly-mini-sub");
        b5.getChildren().addAll(t5, yAuthorValLabel, yAuthorSubLabel);
        yearlyGrid.add(b5, 1, 1);

        // 6. Top Category Card
        VBox b6 = new VBox(6);
        b6.getStyleClass().add("yearly-mini-card");
        Label t6 = new Label(I18n.get("stat.yearly_summary.top_category"));
        t6.getStyleClass().add("yearly-mini-title");
        yCategoryValLabel = new Label("—");
        yCategoryValLabel.getStyleClass().add("yearly-mini-value");
        yCategoryValLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");
        yCategorySubLabel = new Label("");
        yCategorySubLabel.getStyleClass().add("yearly-mini-sub");
        b6.getChildren().addAll(t6, yCategoryValLabel, yCategorySubLabel);
        yearlyGrid.add(b6, 2, 1);

        card.getChildren().addAll(headerRow, yearlyGrid);
        return card;
    }

    private VBox buildMonthlyTrendsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20));
        card.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Header: Title & Segmented Control
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(I18n.get("chart.monthly_activity"));
        title.getStyleClass().add("stat-title");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox segmentedControl = new HBox(4);
        segmentedControl.getStyleClass().add("segmented-control");

        btnPagesMode = new Button(I18n.get("chart.view_pages"));
        btnPagesMode.getStyleClass().addAll("segmented-button", "segmented-button-active");
        btnPagesMode.setOnAction(e -> setMonthlyViewMode(MonthlyViewMode.PAGES));

        btnBooksMode = new Button(I18n.get("chart.view_books"));
        btnBooksMode.getStyleClass().add("segmented-button");
        btnBooksMode.setOnAction(e -> setMonthlyViewMode(MonthlyViewMode.BOOKS));

        btnTimeMode = new Button(I18n.get("chart.view_time"));
        btnTimeMode.getStyleClass().add("segmented-button");
        btnTimeMode.setOnAction(e -> setMonthlyViewMode(MonthlyViewMode.TIME));

        segmentedControl.getChildren().addAll(btnPagesMode, btnBooksMode, btnTimeMode);
        header.getChildren().addAll(title, segmentedControl);

        // Bar Chart
        monthXAxis = new CategoryAxis();
        monthYAxis = new NumberAxis();
        monthYAxis.setForceZeroInRange(true);

        monthlyChart = new BarChart<>(monthXAxis, monthYAxis);
        monthlyChart.setPrefHeight(270);
        monthlyChart.setAnimated(false);
        monthlyChart.setLegendVisible(false);

        card.getChildren().addAll(header, monthlyChart);
        return card;
    }

    private void setMonthlyViewMode(MonthlyViewMode mode) {
        this.currentMonthlyMode = mode;
        btnPagesMode.getStyleClass().remove("segmented-button-active");
        btnBooksMode.getStyleClass().remove("segmented-button-active");
        btnTimeMode.getStyleClass().remove("segmented-button-active");

        switch (mode) {
            case PAGES -> btnPagesMode.getStyleClass().add("segmented-button-active");
            case BOOKS -> btnBooksMode.getStyleClass().add("segmented-button-active");
            case TIME -> btnTimeMode.getStyleClass().add("segmented-button-active");
        }

        Integer yr = yearSelector.getValue();
        renderMonthlyChart(yr != null ? yr : LocalDate.now().getYear());
    }

    private HBox buildCategoriesAndAuthorsRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        // Left: Most-Read Categories
        VBox catCard = new VBox(12);
        catCard.getStyleClass().add("leaderboard-card");
        catCard.setPadding(new Insets(20));
        HBox.setHgrow(catCard, Priority.ALWAYS);

        HBox catHeader = new HBox(10);
        catHeader.setAlignment(Pos.CENTER_LEFT);
        SVGPath tagIcon = IconUtil.createIcon(IconUtil.IconType.TAG, 16);
        tagIcon.setStyle("-fx-fill: -accent-primary;");
        Label catTitle = new Label(I18n.get("stat.top_categories.title"));
        catTitle.getStyleClass().add("stat-title");
        catTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");
        catHeader.getChildren().addAll(tagIcon, catTitle);

        categoriesPieChart = new PieChart();
        categoriesPieChart.setPrefHeight(260);
        categoriesPieChart.setAnimated(true);
        categoriesPieChart.setLegendVisible(true);

        categoriesEmptyLabel = new Label(I18n.get("stat.top_categories.empty"));
        categoriesEmptyLabel.getStyleClass().add("stat-subtext");
        categoriesEmptyLabel.setAlignment(Pos.CENTER);
        categoriesEmptyLabel.setVisible(false);
        categoriesEmptyLabel.setManaged(false);

        catCard.getChildren().addAll(catHeader, categoriesPieChart, categoriesEmptyLabel);

        // Right: Most-Read Authors Leaderboard
        VBox authorCard = new VBox(12);
        authorCard.getStyleClass().add("leaderboard-card");
        authorCard.setPadding(new Insets(20));
        HBox.setHgrow(authorCard, Priority.ALWAYS);

        HBox authorHeader = new HBox(10);
        authorHeader.setAlignment(Pos.CENTER_LEFT);
        SVGPath userIcon = IconUtil.createIcon(IconUtil.IconType.USER, 16);
        userIcon.setStyle("-fx-fill: -accent-primary;");
        Label authorTitle = new Label(I18n.get("stat.top_authors.title"));
        authorTitle.getStyleClass().add("stat-title");
        authorTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");
        authorHeader.getChildren().addAll(userIcon, authorTitle);

        topAuthorsContainer = new VBox(10);
        authorCard.getChildren().addAll(authorHeader, topAuthorsContainer);

        row.getChildren().addAll(catCard, authorCard);
        return row;
    }

    private HBox buildHabitsAndGoalsRow() {
        HBox row = new HBox(16);
        row.setFillHeight(true);

        // Daily Average Card
        dailyAvgCard = new StatCardComponent(
                I18n.get("stat.daily_avg"),
                "0",
                "",
                IconUtil.IconType.CLOCK,
                "stat-accent-teal"
        );
        dailyAvgCard.setMinWidth(220);

        // Reading Goals Card
        VBox goalsCard = new VBox(12);
        goalsCard.getStyleClass().addAll("stat-card", "stat-accent-indigo");
        goalsCard.setPadding(new Insets(18, 20, 18, 20));
        goalsCard.setMinWidth(320);
        HBox.setHgrow(goalsCard, Priority.ALWAYS);

        HBox topRow = new HBox(10);
        topRow.setAlignment(I18n.isRTL() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label goalsTitle = new Label(I18n.get("stat.goals.title"));
        goalsTitle.getStyleClass().add("stat-title");
        HBox.setHgrow(goalsTitle, Priority.ALWAYS);

        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("stat-icon-container");
        iconContainer.setMinSize(36, 36);
        iconContainer.setMaxSize(36, 36);
        SVGPath targetIcon = IconUtil.createIcon(IconUtil.IconType.TARGET, 16);
        targetIcon.getStyleClass().add("stat-icon");
        iconContainer.getChildren().add(targetIcon);

        topRow.getChildren().addAll(goalsTitle, iconContainer);

        // Daily Goal Progress
        VBox dailyBox = new VBox(4);
        HBox dailyHeader = new HBox();
        dailyHeader.setAlignment(Pos.CENTER_LEFT);

        Label dailyTitle = new Label(I18n.get("stat.goals.daily"));
        dailyTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-main;");
        HBox.setHgrow(dailyTitle, Priority.ALWAYS);

        dailyGoalLabel = new Label("0 / 25");
        dailyGoalLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -accent-primary;");
        dailyHeader.getChildren().addAll(dailyTitle, dailyGoalLabel);

        dailyGoalBar = new ProgressBar(0.0);
        dailyGoalBar.setMaxWidth(Double.MAX_VALUE);
        dailyGoalBar.setPrefHeight(7);
        dailyGoalBar.getStyleClass().add("book-progress-bar");
        dailyBox.getChildren().addAll(dailyHeader, dailyGoalBar);

        // Yearly Goal Progress
        VBox yearlyBox = new VBox(4);
        HBox yearlyHeader = new HBox();
        yearlyHeader.setAlignment(Pos.CENTER_LEFT);

        Label yearlyTitle = new Label(I18n.get("stat.goals.yearly"));
        yearlyTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-main;");
        HBox.setHgrow(yearlyTitle, Priority.ALWAYS);

        yearlyGoalLabel = new Label("0 / 12");
        yearlyGoalLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #10b981;");
        yearlyHeader.getChildren().addAll(yearlyTitle, yearlyGoalLabel);

        yearlyGoalBar = new ProgressBar(0.0);
        yearlyGoalBar.setMaxWidth(Double.MAX_VALUE);
        yearlyGoalBar.setPrefHeight(7);
        yearlyGoalBar.getStyleClass().add("book-progress-bar");
        yearlyBox.getChildren().addAll(yearlyHeader, yearlyGoalBar);

        goalsCard.getChildren().addAll(topRow, dailyBox, yearlyBox);
        AnimationUtil.addCardHover(goalsCard);

        row.getChildren().addAll(dailyAvgCard, goalsCard);
        return row;
    }

    private VBox buildEmptyPromptBox() {
        VBox box = new VBox(14);
        box.getStyleClass().addAll("stat-card", "empty-state-box");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));

        SVGPath icon = IconUtil.createIcon(IconUtil.IconType.LIBRARY, 36);
        icon.setStyle("-fx-fill: -accent-primary;");

        Label title = new Label(I18n.get("dashboard.welcome.title"));
        title.getStyleClass().add("empty-state-title");

        Label desc = new Label(I18n.get("dashboard.welcome.desc"));
        desc.getStyleClass().add("empty-state-subtitle");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button addBtn = new Button(I18n.get("dashboard.welcome.add_first"));
        addBtn.getStyleClass().addAll("btn", "btn-primary");
        addBtn.setOnAction(e -> mainController.openBookFormDialog(null));

        Button loadSamplesBtn = new Button(I18n.get("dashboard.welcome.load_samples"));
        loadSamplesBtn.getStyleClass().addAll("btn", "btn-secondary");
        loadSamplesBtn.setOnAction(e -> {
            int loaded = sampleDataService.loadSampleData();
            mainController.showToast(I18n.get("toast.samples_loaded", loaded), ToastNotification.ToastType.SUCCESS);
            mainController.refreshActiveViews();
        });

        actions.getChildren().addAll(addBtn, loadSamplesBtn);
        box.getChildren().addAll(icon, title, desc, actions);
        return box;
    }

    public void refresh() {
        LibraryStats stats = bookService.getLibraryStatistics();

        // 1. KPI Ribbon
        totalBooksCard.updateNumericValue(stats.getTotalBooks(), I18n.get("stat.total_books.sub", stats.getTotalBooks()));
        completedCard.updateNumericValue(stats.getCompletedCount(), I18n.get("stat.completed.sub"));
        pagesCard.updateNumericValue(stats.getPagesRead(), I18n.get("stat.progress.sub", stats.getPagesRead(), stats.getTotalPages()));

        int totalTime = readingTrackerService.getTotalReadingTimeMinutes();
        timeCard.updateTextValue(DateUtil.formatDuration(totalTime), I18n.get("stat.reading_time.sub", DateUtil.formatDuration(totalTime)));

        double avgSpeed = readingTrackerService.getAverageReadingSpeedPagesPerHour();
        speedCard.updateTextValue(DateUtil.formatReadingSpeed(avgSpeed), I18n.get("stat.reading_speed.sub"));

        int streak = readingTrackerService.calculateCurrentStreak();
        int bestStreak = readingTrackerService.calculateBestStreak();
        boolean readToday = readingTrackerService.hasReadToday();
        String streakStatus = readToday
                ? I18n.get("stat.streak.today_read")
                : (streak > 0 ? I18n.get("stat.streak.today_pending") : I18n.get("stat.streak.none"));
        String streakSub = streakStatus + " • " + I18n.get("stat.streak.best", bestStreak);
        streakCard.updateNumericValue(streak, streakSub);

        // 2. Refresh Year Selector
        List<Integer> years = readingTrackerService.getAllDistinctYears();
        Integer currentSelected = yearSelector.getValue();
        yearSelector.getItems().setAll(years);
        if (currentSelected != null && years.contains(currentSelected)) {
            yearSelector.setValue(currentSelected);
        } else if (!years.isEmpty()) {
            yearSelector.setValue(years.get(0));
        }

        int activeYear = yearSelector.getValue() != null ? yearSelector.getValue() : LocalDate.now().getYear();
        renderYearlySummary(activeYear);
        renderMonthlyChart(activeYear);

        // 3. Refresh Top Categories
        List<CategoryStat> topCategories = readingTrackerService.getTopCategories(6);
        ObservableList<PieChart.Data> catPieData = FXCollections.observableArrayList();
        for (CategoryStat cs : topCategories) {
            if (cs.getTotalBooksCount() > 0) {
                catPieData.add(new PieChart.Data(cs.getCategory() + " (" + cs.getTotalBooksCount() + ")", cs.getTotalBooksCount()));
            }
        }
        categoriesPieChart.setData(catPieData);
        boolean hasCats = !catPieData.isEmpty();
        categoriesPieChart.setVisible(hasCats);
        categoriesPieChart.setManaged(hasCats);
        categoriesEmptyLabel.setVisible(!hasCats);
        categoriesEmptyLabel.setManaged(!hasCats);

        // 4. Refresh Top Authors Leaderboard
        List<AuthorStat> topAuthors = readingTrackerService.getTopAuthors(5);
        renderTopAuthors(topAuthors);

        // 5. Daily Average & Goals
        double dailyAvg = readingTrackerService.calculateDailyAveragePages();
        String dailyAvgStr = String.format(Locale.US, "%.1f", dailyAvg);
        String dailyAvgSub = I18n.get("stat.daily_avg.sub", String.format(Locale.US, "%.0f", dailyAvg));
        dailyAvgCard.updateTextValue(dailyAvgStr, dailyAvgSub);

        ReadingGoal goal = readingTrackerService.getReadingGoal();
        String dailySub = I18n.get("stat.goals.daily_sub", goal.getPagesReadToday(), goal.getDailyPagesGoal());
        if (goal.isDailyGoalAchieved()) {
            dailySub += " " + I18n.get("stat.goals.achieved");
        }
        dailyGoalLabel.setText(dailySub);
        AnimationUtil.animateProgressBar(dailyGoalBar, goal.getDailyProgressRatio());

        String yearlySub = I18n.get("stat.goals.yearly_sub", goal.getBooksCompletedThisYear(), goal.getYearlyBooksGoal());
        if (goal.isYearlyGoalAchieved()) {
            yearlySub += " " + I18n.get("stat.goals.achieved");
        }
        yearlyGoalLabel.setText(yearlySub);
        AnimationUtil.animateProgressBar(yearlyGoalBar, goal.getYearlyProgressRatio());

        // Empty prompt box visibility
        boolean isEmpty = stats.getTotalBooks() == 0;
        emptyPromptBox.setVisible(isEmpty);
        emptyPromptBox.setManaged(isEmpty);

        // 6. Currently Reading Section
        currentlyReadingSection.getChildren().clear();
        List<Book> readingBooks = bookService.getBooksByStatus(ReadingStatus.READING);
        if (readingBooks.isEmpty()) {
            readingSectionContainer.setVisible(false);
            readingSectionContainer.setManaged(false);
        } else {
            readingSectionContainer.setVisible(true);
            readingSectionContainer.setManaged(true);
            for (Book b : readingBooks) {
                BookCardComponent card = new BookCardComponent(
                        b,
                        book -> mainController.navigateToBookDetails(book),
                        book -> mainController.openBookFormDialog(book),
                        (comp, book) -> confirmAndDelete(book),
                        book -> quickAdvance(book, 10),
                        this::toggleFavorite,
                        book -> mainController.openActiveReadingSessionDialog(book)
                );
                currentlyReadingSection.getChildren().add(card);
            }
        }

        // 7. Recent Reading Sessions Feed
        recentSessionsSection.getChildren().clear();
        List<ReadingSession> recentSessions = readingTrackerService.getRecentSessions(4);
        if (recentSessions.isEmpty()) {
            recentSessionsContainer.setVisible(false);
            recentSessionsContainer.setManaged(false);
        } else {
            recentSessionsContainer.setVisible(true);
            recentSessionsContainer.setManaged(true);
            for (ReadingSession s : recentSessions) {
                recentSessionsSection.getChildren().add(buildRecentSessionCard(s));
            }
        }

        // 8. Recently Added Section
        recentBooksSection.getChildren().clear();
        List<Book> recentBooks = stats.getRecentlyAdded();
        for (Book b : recentBooks) {
            BookCardComponent card = new BookCardComponent(
                    b,
                    book -> mainController.navigateToBookDetails(book),
                    book -> mainController.openBookFormDialog(book),
                    (comp, book) -> confirmAndDelete(book),
                    book -> quickAdvance(book, 10),
                    this::toggleFavorite,
                    book -> mainController.openActiveReadingSessionDialog(book)
            );
            recentBooksSection.getChildren().add(card);
        }
    }

    private void renderYearlySummary(int year) {
        YearlyReadingSummary summary = readingTrackerService.getYearlyReadingSummary(year);
        yearlySubtitleLabel.setText(I18n.get("stat.yearly_summary.year", String.valueOf(year)));

        // Books Completed
        yBooksValLabel.setText(summary.getBooksCompleted() + " / " + summary.getYearlyGoal());
        int pct = (int) (summary.getGoalProgressRatio() * 100);
        yBooksSubLabel.setText(I18n.get("stat.yearly_summary.goal_status", summary.getBooksCompleted(), summary.getYearlyGoal(), pct));
        AnimationUtil.animateProgressBar(yBooksBar, summary.getGoalProgressRatio());

        // Pages Read
        yPagesValLabel.setText(String.format(Locale.US, "%,d", summary.getPagesRead()));
        yPagesSubLabel.setText(I18n.get("stat.progress.sub", summary.getPagesRead(), "—"));

        // Time Spent
        yTimeValLabel.setText(summary.getFormattedReadingTime());
        yTimeSubLabel.setText(I18n.get("stat.reading_time.sub", summary.getFormattedReadingTime()));

        // Average Speed
        ySpeedValLabel.setText(summary.getFormattedReadingSpeed());
        ySpeedSubLabel.setText(I18n.get("stat.reading_speed.sub"));

        // Top Author
        if (summary.getTopAuthor() != null && !summary.getTopAuthor().trim().isEmpty()) {
            yAuthorValLabel.setText(summary.getTopAuthor());
            yAuthorSubLabel.setText(I18n.get("stat.author.books_count", summary.getTopAuthorBooks()));
        } else {
            yAuthorValLabel.setText("—");
            yAuthorSubLabel.setText(I18n.get("stat.yearly_summary.no_data"));
        }

        // Top Category
        if (summary.getTopCategory() != null && !summary.getTopCategory().trim().isEmpty()) {
            yCategoryValLabel.setText(summary.getTopCategory());
            yCategorySubLabel.setText(I18n.get("stat.category.books_count", summary.getTopCategoryBooks()));
        } else {
            yCategoryValLabel.setText("—");
            yCategorySubLabel.setText(I18n.get("stat.yearly_summary.no_data"));
        }
    }

    private void renderMonthlyChart(int year) {
        monthlyChart.getData().clear();
        List<MonthlyReadingStat> monthlyStats = readingTrackerService.getMonthlyReadingStatsInYear(year);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String seriesName = switch (currentMonthlyMode) {
            case PAGES -> I18n.get("chart.pages_per_month");
            case BOOKS -> I18n.get("chart.books_per_month");
            case TIME -> I18n.get("chart.time_per_month");
        };
        series.setName(seriesName);

        for (MonthlyReadingStat stat : monthlyStats) {
            String monthName = I18n.get("chart.month_" + stat.getMonth());
            Number value = switch (currentMonthlyMode) {
                case PAGES -> stat.getPagesRead();
                case BOOKS -> stat.getBooksCompleted();
                case TIME -> stat.getDurationMinutes();
            };

            XYChart.Data<String, Number> data = new XYChart.Data<>(monthName, value);
            series.getData().add(data);
        }

        monthlyChart.getData().add(series);

        // Add tooltips
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                String tip = data.getXValue() + ": " + data.getYValue() + " " + switch (currentMonthlyMode) {
                    case PAGES -> I18n.get("chart.pages_read");
                    case BOOKS -> I18n.get("stat.completed");
                    case TIME -> "m";
                };
                Tooltip.install(node, new Tooltip(tip));
            }
        }
    }

    private void renderTopAuthors(List<AuthorStat> topAuthors) {
        topAuthorsContainer.getChildren().clear();
        if (topAuthors.isEmpty()) {
            Label empty = new Label(I18n.get("stat.top_authors.empty"));
            empty.getStyleClass().add("stat-subtext");
            topAuthorsContainer.getChildren().add(empty);
            return;
        }

        int maxPages = topAuthors.stream().mapToInt(AuthorStat::getPagesRead).max().orElse(1);
        if (maxPages <= 0) maxPages = 1;

        for (int i = 0; i < topAuthors.size(); i++) {
            AuthorStat stat = topAuthors.get(i);
            int rank = i + 1;

            HBox item = new HBox(12);
            item.getStyleClass().add("leaderboard-item");
            item.setAlignment(Pos.CENTER_LEFT);

            Label rankBadge = new Label("#" + rank);
            rankBadge.getStyleClass().add("rank-badge");
            if (rank == 1) rankBadge.getStyleClass().add("rank-badge-1");
            else if (rank == 2) rankBadge.getStyleClass().add("rank-badge-2");
            else if (rank == 3) rankBadge.getStyleClass().add("rank-badge-3");
            else rankBadge.getStyleClass().add("rank-badge-default");

            VBox details = new VBox(4);
            HBox.setHgrow(details, Priority.ALWAYS);

            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(stat.getAuthor());
            nameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            int displayCount = stat.getCompletedCount() > 0 ? stat.getCompletedCount() : stat.getTotalBooksCount();
            Label booksBadge = new Label(I18n.get("stat.author.books_count", displayCount));
            booksBadge.getStyleClass().addAll("badge-chip", "status-completed");

            Label pagesBadge = new Label(I18n.get("stat.author.pages_count", stat.getPagesRead()));
            pagesBadge.getStyleClass().add("meta-chip");

            titleRow.getChildren().addAll(nameLabel, booksBadge, pagesBadge);

            ProgressBar bar = new ProgressBar((double) stat.getPagesRead() / maxPages);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(4);
            bar.getStyleClass().add("book-progress-bar");

            details.getChildren().addAll(titleRow, bar);
            item.getChildren().addAll(rankBadge, details);
            AnimationUtil.addCardHover(item);
            topAuthorsContainer.getChildren().add(item);
        }
    }

    private HBox buildRecentSessionCard(ReadingSession session) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: -bg-card; -fx-border-color: -border-subtle; -fx-border-width: 1; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;");
        card.setPrefWidth(320);

        // Date indicator
        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER);
        Label dateLbl = new Label(DateUtil.format(session.getSessionDate()));
        dateLbl.getStyleClass().add("meta-chip");
        dateBox.getChildren().add(dateLbl);

        // Info VBox
        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String titleStr = session.getBookTitle() != null ? session.getBookTitle() : "Book #" + session.getBookId();
        Label titleLabel = new Label(titleStr);
        titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -text-main;");
        titleLabel.setMaxWidth(200);

        HBox chips = new HBox(6);
        Label pagesChip = new Label("+" + I18n.get("sessions.pages_read", session.getPagesRead()));
        pagesChip.getStyleClass().addAll("badge-chip", "status-reading");

        Label durChip = null;
        if (session.getDurationMinutes() > 0) {
            durChip = new Label(session.getFormattedDuration());
            durChip.getStyleClass().add("tag-chip");
        }

        chips.getChildren().add(pagesChip);
        if (durChip != null) {
            chips.getChildren().add(durChip);
        }

        infoBox.getChildren().addAll(titleLabel, chips);
        card.getChildren().addAll(dateBox, infoBox);

        card.setOnMouseClicked(e -> {
            bookService.getBookById(session.getBookId()).ifPresent(b -> mainController.navigateToBookDetails(b));
        });

        AnimationUtil.addCardHover(card);
        return card;
    }

    private void toggleFavorite(Book book) {
        boolean newFav = !book.isFavorite();
        bookService.toggleFavorite(book.getId(), newFav);
        book.setFavorite(newFav);
        mainController.showToast(newFav ? I18n.get("toast.favorite_added", book.getTitle()) : I18n.get("toast.favorite_removed", book.getTitle()), ToastNotification.ToastType.SUCCESS);
        mainController.refreshActiveViews();
    }

    private void quickAdvance(Book book, int pages) {
        bookService.advancePage(book, pages);
        mainController.showToast(I18n.get("toast.progress_updated", book.getTitle()), ToastNotification.ToastType.SUCCESS);
        mainController.refreshActiveViews();
    }

    private void confirmAndDelete(Book book) {
        boolean confirm = true;
        if (mainController.getSettingsService().isConfirmDeleteEnabled()) {
            confirm = com.librarymanager.util.DialogUtil.confirmDelete(mainController.getPrimaryStage(), book.getTitle());
        }
        if (confirm) {
            bookService.deleteBook(book.getId());
            mainController.showToast(I18n.get("toast.book_deleted", book.getTitle()), ToastNotification.ToastType.SUCCESS);
            mainController.refreshActiveViews();
        }
    }
}
