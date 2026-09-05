package com.librarymanager.controller;

import com.librarymanager.component.BookCardComponent;
import com.librarymanager.component.StatCardComponent;
import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingGoal;
import com.librarymanager.model.ReadingSession;
import com.librarymanager.model.ReadingStatus;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.Locale;

/**
 * Controller for the Dashboard screen, rendering statistics cards,
 * reading streaks, daily averages, goals, dynamic charts,
 * and recently added/completed books with i18n & RTL support.
 */
public class DashboardController {

    private final MainController mainController;
    private final BookService bookService;
    private final ReadingTrackerService readingTrackerService;
    private final SampleDataService sampleDataService;

    private final ScrollPane rootScrollPane;
    private final VBox contentBox;

    // Stat Cards (Collection)
    private StatCardComponent totalBooksCard;
    private StatCardComponent readingCard;
    private StatCardComponent completedCard;
    private StatCardComponent notStartedCard;
    private StatCardComponent progressCard;

    // Stat Cards (Reading Habit & Streaks)
    private StatCardComponent streakCard;
    private StatCardComponent dailyAvgCard;

    // Goals Widget
    private Label dailyGoalLabel;
    private ProgressBar dailyGoalBar;
    private Label yearlyGoalLabel;
    private ProgressBar yearlyGoalBar;

    // Charts
    private PieChart statusPieChart;
    private BarChart<String, Number> progressBreakdownChart;

    // Content sections
    private FlowPane currentlyReadingSection;
    private VBox readingSectionContainer;
    private FlowPane recentSessionsSection;
    private VBox recentSessionsContainer;
    private FlowPane recentBooksSection;
    private VBox emptyPromptBox;

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
        // 1. Statistics Cards Row
        HBox statsRow = new HBox(16);
        statsRow.setFillHeight(true);

        totalBooksCard = new StatCardComponent(I18n.get("stat.total_books"), "0", "", IconUtil.IconType.LIBRARY, "stat-accent-indigo");
        readingCard = new StatCardComponent(I18n.get("stat.reading"), "0", I18n.get("stat.reading.sub"), IconUtil.IconType.READING, "stat-accent-blue");
        completedCard = new StatCardComponent(I18n.get("stat.completed"), "0", I18n.get("stat.completed.sub"), IconUtil.IconType.COMPLETED, "stat-accent-emerald");
        notStartedCard = new StatCardComponent(I18n.get("stat.not_started"), "0", I18n.get("stat.not_started.sub"), IconUtil.IconType.NOT_STARTED, "stat-accent-amber");
        progressCard = new StatCardComponent(I18n.get("stat.overall_progress"), "0%", "", IconUtil.IconType.STATS, "stat-accent-purple");

        statsRow.getChildren().addAll(totalBooksCard, readingCard, completedCard, notStartedCard, progressCard);
        contentBox.getChildren().add(statsRow);

        // 2. Empty prompt if library is empty
        emptyPromptBox = buildEmptyPromptBox();
        contentBox.getChildren().add(emptyPromptBox);

        // 3. Habits & Goals Row (v1.2 Reading Tracker)
        HBox habitsRow = buildHabitsAndGoalsRow();
        contentBox.getChildren().add(habitsRow);

        // 4. Charts Section
        HBox chartsRow = new HBox(20);
        chartsRow.setFillHeight(true);

        // Pie Chart: Status Distribution
        VBox pieChartCard = new VBox(10);
        pieChartCard.getStyleClass().add("stat-card");
        pieChartCard.setPadding(new Insets(16));
        HBox.setHgrow(pieChartCard, Priority.ALWAYS);

        Label pieTitle = new Label(I18n.get("chart.status_distribution"));
        pieTitle.getStyleClass().add("stat-title");

        statusPieChart = new PieChart();
        statusPieChart.setPrefHeight(260);
        statusPieChart.setAnimated(true);
        statusPieChart.setLegendVisible(true);
        pieChartCard.getChildren().addAll(pieTitle, statusPieChart);

        // Bar Chart: Pages Read vs Remaining
        VBox barChartCard = new VBox(10);
        barChartCard.getStyleClass().add("stat-card");
        barChartCard.setPadding(new Insets(16));
        HBox.setHgrow(barChartCard, Priority.ALWAYS);

        Label barTitle = new Label(I18n.get("chart.progress_breakdown"));
        barTitle.getStyleClass().add("stat-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        progressBreakdownChart = new BarChart<>(xAxis, yAxis);
        progressBreakdownChart.setPrefHeight(260);
        progressBreakdownChart.setAnimated(false);
        progressBreakdownChart.setLegendVisible(false);
        barChartCard.getChildren().addAll(barTitle, progressBreakdownChart);

        chartsRow.getChildren().addAll(pieChartCard, barChartCard);
        contentBox.getChildren().add(chartsRow);

        // 5. Currently Reading Section
        readingSectionContainer = new VBox(12);
        Label readingHeading = new Label(I18n.get("dashboard.currently_reading"));
        readingHeading.getStyleClass().add("stat-title");
        readingHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        currentlyReadingSection = new FlowPane();
        currentlyReadingSection.setHgap(16);
        currentlyReadingSection.setVgap(16);
        readingSectionContainer.getChildren().addAll(readingHeading, currentlyReadingSection);
        contentBox.getChildren().add(readingSectionContainer);

        // 6. Recent Reading Sessions Feed (v1.2)
        recentSessionsContainer = new VBox(12);
        Label sessionsHeading = new Label(I18n.get("dashboard.recent_sessions"));
        sessionsHeading.getStyleClass().add("stat-title");
        sessionsHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        recentSessionsSection = new FlowPane();
        recentSessionsSection.setHgap(16);
        recentSessionsSection.setVgap(16);
        recentSessionsContainer.getChildren().addAll(sessionsHeading, recentSessionsSection);
        contentBox.getChildren().add(recentSessionsContainer);

        // 7. Recently Added Books Section
        VBox recentSectionContainer = new VBox(12);
        Label recentHeading = new Label(I18n.get("dashboard.recently_added"));
        recentHeading.getStyleClass().add("stat-title");
        recentHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        recentBooksSection = new FlowPane();
        recentBooksSection.setHgap(16);
        recentBooksSection.setVgap(16);
        recentSectionContainer.getChildren().addAll(recentHeading, recentBooksSection);
        contentBox.getChildren().add(recentSectionContainer);
    }

    private HBox buildHabitsAndGoalsRow() {
        HBox row = new HBox(16);
        row.setFillHeight(true);

        // 1. Reading Streak Card
        streakCard = new StatCardComponent(
                I18n.get("stat.streak"),
                "0",
                "",
                IconUtil.IconType.FIRE,
                "stat-accent-rose"
        );
        streakCard.setMinWidth(200);

        // 2. Daily Average Card
        dailyAvgCard = new StatCardComponent(
                I18n.get("stat.daily_avg"),
                "0",
                "",
                IconUtil.IconType.CLOCK,
                "stat-accent-teal"
        );
        dailyAvgCard.setMinWidth(200);

        // 3. Reading Goals Card
        VBox goalsCard = new VBox(12);
        goalsCard.getStyleClass().addAll("stat-card", "stat-accent-indigo");
        goalsCard.setPadding(new Insets(18, 20, 18, 20));
        goalsCard.setMinWidth(300);
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

        row.getChildren().addAll(streakCard, dailyAvgCard, goalsCard);
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

        // Update cards with count animations
        totalBooksCard.updateNumericValue(stats.getTotalBooks(), I18n.get("stat.total_books.sub", stats.getTotalBooks()));
        readingCard.updateNumericValue(stats.getReadingCount(), I18n.get("stat.reading.sub"));
        completedCard.updateNumericValue(stats.getCompletedCount(), I18n.get("stat.completed.sub"));
        notStartedCard.updateNumericValue(stats.getNotStartedCount(), I18n.get("stat.not_started.sub"));

        String pageInfo = I18n.get("stat.progress.sub", stats.getPagesRead(), stats.getTotalPages());
        progressCard.updateTextValue(stats.getFormattedOverallProgress(), pageInfo);

        // 2. Update Streak & Habit Cards
        int streak = readingTrackerService.calculateCurrentStreak();
        int bestStreak = readingTrackerService.calculateBestStreak();
        boolean readToday = readingTrackerService.hasReadToday();

        String streakStatus = readToday
                ? I18n.get("stat.streak.today_read")
                : (streak > 0 ? I18n.get("stat.streak.today_pending") : I18n.get("stat.streak.none"));
        String streakSub = streakStatus + " • " + I18n.get("stat.streak.best", bestStreak);
        streakCard.updateNumericValue(streak, streakSub);

        double dailyAvg = readingTrackerService.calculateDailyAveragePages();
        String dailyAvgStr = String.format(Locale.US, "%.1f", dailyAvg);
        String dailyAvgSub = I18n.get("stat.daily_avg.sub", String.format(Locale.US, "%.0f", dailyAvg));
        dailyAvgCard.updateTextValue(dailyAvgStr, dailyAvgSub);

        // 3. Update Goals Widget
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

        // Visibility of empty prompt
        boolean isEmpty = stats.getTotalBooks() == 0;
        emptyPromptBox.setVisible(isEmpty);
        emptyPromptBox.setManaged(isEmpty);

        // Update Pie Chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (stats.getReadingCount() > 0) {
            PieChart.Data d = new PieChart.Data(ReadingStatus.READING.getDisplayName() + " (" + stats.getReadingCount() + ")", stats.getReadingCount());
            pieData.add(d);
        }
        if (stats.getCompletedCount() > 0) {
            PieChart.Data d = new PieChart.Data(ReadingStatus.COMPLETED.getDisplayName() + " (" + stats.getCompletedCount() + ")", stats.getCompletedCount());
            pieData.add(d);
        }
        if (stats.getNotStartedCount() > 0) {
            PieChart.Data d = new PieChart.Data(ReadingStatus.NOT_STARTED.getDisplayName() + " (" + stats.getNotStartedCount() + ")", stats.getNotStartedCount());
            pieData.add(d);
        }
        statusPieChart.setData(pieData);

        // Update Bar Chart
        progressBreakdownChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Pages");
        int pagesRead = stats.getPagesRead();
        int pagesRemaining = Math.max(0, stats.getTotalPages() - pagesRead);
        XYChart.Data<String, Number> readBar = new XYChart.Data<>(I18n.get("chart.pages_read"), pagesRead);
        XYChart.Data<String, Number> leftBar = new XYChart.Data<>(I18n.get("chart.pages_left"), pagesRemaining);
        series.getData().addAll(readBar, leftBar);
        progressBreakdownChart.getData().add(series);

        // Refresh Currently Reading Section
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
                        this::toggleFavorite
                );
                currentlyReadingSection.getChildren().add(card);
            }
        }

        // Refresh Recent Reading Sessions Activity Feed
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

        // Refresh Recently Added Section
        recentBooksSection.getChildren().clear();
        List<Book> recentBooks = stats.getRecentlyAdded();
        for (Book b : recentBooks) {
            BookCardComponent card = new BookCardComponent(
                    b,
                    book -> mainController.navigateToBookDetails(book),
                    book -> mainController.openBookFormDialog(book),
                    (comp, book) -> confirmAndDelete(book),
                    book -> quickAdvance(book, 10),
                    this::toggleFavorite
            );
            recentBooksSection.getChildren().add(card);
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

        // On card click -> open book details
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
