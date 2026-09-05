package com.librarymanager.controller;

import com.librarymanager.component.BookCardComponent;
import com.librarymanager.component.StatCardComponent;
import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.LibraryStats;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BookService;
import com.librarymanager.service.SampleDataService;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.List;

/**
 * Controller for the Dashboard screen, rendering statistics cards,
 * dynamic charts, and recently added/completed books with i18n & RTL support.
 */
public class DashboardController {

    private final MainController mainController;
    private final BookService bookService;
    private final SampleDataService sampleDataService;

    private final ScrollPane rootScrollPane;
    private final VBox contentBox;

    // Stat Cards
    private StatCardComponent totalBooksCard;
    private StatCardComponent readingCard;
    private StatCardComponent completedCard;
    private StatCardComponent notStartedCard;
    private StatCardComponent progressCard;

    // Charts
    private PieChart statusPieChart;
    private BarChart<String, Number> progressBreakdownChart;

    // Content sections
    private FlowPane currentlyReadingSection;
    private VBox readingSectionContainer;
    private FlowPane recentBooksSection;
    private VBox emptyPromptBox;

    public DashboardController(MainController mainController, BookService bookService, SampleDataService sampleDataService) {
        this.mainController = mainController;
        this.bookService = bookService;
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

        // 3. Charts Section
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

        // 4. Currently Reading Section
        readingSectionContainer = new VBox(12);
        Label readingHeading = new Label(I18n.get("dashboard.currently_reading"));
        readingHeading.getStyleClass().add("stat-title");
        readingHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        currentlyReadingSection = new FlowPane();
        currentlyReadingSection.setHgap(16);
        currentlyReadingSection.setVgap(16);
        readingSectionContainer.getChildren().addAll(readingHeading, currentlyReadingSection);
        contentBox.getChildren().add(readingSectionContainer);

        // 5. Recently Added Books Section
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
