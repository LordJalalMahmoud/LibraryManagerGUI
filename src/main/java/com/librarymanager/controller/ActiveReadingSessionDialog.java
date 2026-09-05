package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingGoal;
import com.librarymanager.model.ReadingSession;
import com.librarymanager.service.ReadingTrackerService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.DateUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interactive Active Reading Session Companion dialog (v1.7 Reading Experience).
 * Provides a live stopwatch timer (Start/Pause/Resume/Finish), real-time Pages Per Minute (PPM)
 * speed calculations, live Estimated Time to Finish (ETA), daily target tracker, and page steppers.
 */
public class ActiveReadingSessionDialog {
    private static final Logger LOGGER = Logger.getLogger(ActiveReadingSessionDialog.class.getName());

    public enum TimerState {
        READY, RUNNING, PAUSED
    }

    private final MainController mainController;
    private final ReadingTrackerService readingTrackerService;
    private final Book book;
    private final Runnable onSuccess;

    private Stage dialogStage;
    private Timeline timeline;
    private int elapsedSeconds = 0;
    private TimerState timerState = TimerState.READY;

    private final int initialStartPage;
    private int currentPage;
    private final int todayPagesBaseline;
    private final double baselinePpm;
    private final ReadingGoal readingGoal;

    // UI Nodes
    private Label timerDigitsLabel;
    private Label timerStatusBadge;
    private Button startPauseButton;
    private Button finishButton;

    // Metric Tile Labels
    private ProgressBar bookProgressBar;
    private Label progressRatioLabel;
    private Label progressPctLabel;

    private Label todayPagesValueLabel;
    private Label todayGoalSubLabel;

    private Label speedValueLabel;
    private Label speedSubLabel;

    private Label etaValueLabel;
    private Label etaSubLabel;

    private Spinner<Integer> pageSpinner;
    private Label pagesReadBadge;
    private TextArea notesArea;

    public ActiveReadingSessionDialog(MainController mainController,
                                      ReadingTrackerService readingTrackerService,
                                      Book book,
                                      Runnable onSuccess) {
        this.mainController = mainController;
        this.readingTrackerService = readingTrackerService;
        this.book = book;
        this.onSuccess = onSuccess;

        this.initialStartPage = book.getCurrentPage();
        this.currentPage = book.getCurrentPage();
        this.todayPagesBaseline = readingTrackerService.getPagesReadTodayForBook(book.getId() != null ? book.getId() : 0);
        this.baselinePpm = readingTrackerService.getEffectiveReadingSpeedPpm(book);
        this.readingGoal = readingTrackerService.getReadingGoal();
    }

    public void showAsDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(I18n.get("experience.timer.title", book.getTitle()));
        dialogStage.setMinWidth(620);
        dialogStage.setMinHeight(680);
        dialogStage.setResizable(false);

        VBox root = buildUi();

        Scene scene = new Scene(root, 640, 720);

        String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
        String themeStyle;
        if (mainController.getSettingsService().isHighContrast()) {
            themeStyle = getClass().getResource("/css/theme-high-contrast.css").toExternalForm();
        } else if (mainController.getSettingsService().isDarkMode()) {
            themeStyle = getClass().getResource("/css/theme-dark.css").toExternalForm();
        } else {
            themeStyle = getClass().getResource("/css/theme-light.css").toExternalForm();
        }
        scene.getStylesheets().addAll(themeStyle, baseStyle);

        // Keyboard navigation: Spacebar toggles start/pause, Escape confirms close
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE && !notesArea.isFocused() && !(scene.getFocusOwner() instanceof TextInputControl)) {
                event.consume();
                toggleTimer();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                handleCancel();
            }
        });

        initTimer();

        dialogStage.setOnCloseRequest(event -> {
            event.consume();
            handleCancel();
        });

        dialogStage.setScene(scene);
        updateMetrics();
        dialogStage.showAndWait();
    }

    private void initTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            elapsedSeconds++;
            updateTimerDisplay();
            updateMetrics();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private VBox buildUi() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell", "reading-companion-modal");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // 1. Top Header: Book Info & Status
        HBox topHeader = new HBox(14);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        SVGPath bookIcon = IconUtil.createIcon(IconUtil.IconType.LIBRARY, 28);
        bookIcon.setStyle("-fx-fill: -accent-primary;");

        VBox bookMetaBox = new VBox(2);
        HBox.setHgrow(bookMetaBox, Priority.ALWAYS);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -text-main;");
        titleLabel.setWrapText(true);

        Label authorLabel = new Label(I18n.get("book.card.by", book.getAuthor()) + " • " +
                I18n.get("book.card.pages", book.getTotalPages()));
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        bookMetaBox.getChildren().addAll(titleLabel, authorLabel);

        timerStatusBadge = new Label(I18n.get("experience.timer.status.ready"));
        timerStatusBadge.getStyleClass().addAll("timer-state-chip", "timer-state-ready");

        topHeader.getChildren().addAll(bookIcon, bookMetaBox, timerStatusBadge);
        root.getChildren().add(topHeader);

        // 2. Big Stopwatch / Timer Card
        VBox timerCard = new VBox(12);
        timerCard.getStyleClass().addAll("reading-timer-container");
        timerCard.setAlignment(Pos.CENTER);

        timerDigitsLabel = new Label("00:00");
        timerDigitsLabel.getStyleClass().add("timer-digits");

        HBox timerControls = new HBox(12);
        timerControls.setAlignment(Pos.CENTER);

        startPauseButton = new Button(I18n.get("experience.timer.start"));
        startPauseButton.getStyleClass().addAll("btn", "btn-primary");
        startPauseButton.setGraphic(IconUtil.createIcon(IconUtil.IconType.PLAY, 16));
        startPauseButton.setGraphicTextGap(8);
        startPauseButton.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 8 20 8 20;");
        startPauseButton.setOnAction(e -> toggleTimer());

        finishButton = new Button(I18n.get("experience.timer.finish"));
        finishButton.getStyleClass().addAll("btn", "btn-secondary");
        finishButton.setGraphic(IconUtil.createIcon(IconUtil.IconType.CHECK, 16));
        finishButton.setGraphicTextGap(8);
        finishButton.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-padding: 8 18 8 18;");
        finishButton.setOnAction(e -> handleFinishSession());

        timerControls.getChildren().addAll(startPauseButton, finishButton);

        Label shortcutTip = new Label(I18n.get("experience.timer.shortcut_hint"));
        shortcutTip.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

        timerCard.getChildren().addAll(timerDigitsLabel, timerControls, shortcutTip);
        root.getChildren().add(timerCard);

        // 3. The 4 Live Metric KPI Cards (Matching User Request Mockup)
        GridPane metricGrid = new GridPane();
        metricGrid.setHgap(12);
        metricGrid.setVgap(12);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        metricGrid.getColumnConstraints().addAll(col1, col2);

        // Tile 1: Progress
        VBox tileProgress = buildProgressMetricTile();
        // Tile 2: Today's Pages
        VBox tileToday = buildTodayMetricTile();
        // Tile 3: Reading Speed (Pages/min)
        VBox tileSpeed = buildSpeedMetricTile();
        // Tile 4: Estimated Remaining Time (ETA)
        VBox tileEta = buildEtaMetricTile();

        metricGrid.add(tileProgress, 0, 0);
        metricGrid.add(tileToday, 1, 0);
        metricGrid.add(tileSpeed, 0, 1);
        metricGrid.add(tileEta, 1, 1);

        root.getChildren().add(metricGrid);

        // 4. Interactive Page Control Stepper
        VBox pageControlCard = buildPageControlCard();
        root.getChildren().add(pageControlCard);

        // 5. Notes field (Takeaways & reflections)
        VBox notesBox = new VBox(6);
        Label notesLabel = new Label(I18n.get("session.dialog.notes"));
        notesLabel.getStyleClass().add("form-label");

        notesArea = new TextArea();
        notesArea.setPromptText(I18n.get("experience.notes_placeholder"));
        notesArea.setPrefRowCount(2);
        notesArea.getStyleClass().add("text-input");
        notesArea.setWrapText(true);
        notesBox.getChildren().addAll(notesLabel, notesArea);
        root.getChildren().add(notesBox);

        // 6. Bottom Cancel / Close row
        HBox bottomRow = new HBox(12);
        bottomRow.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button cancelBtn = new Button(I18n.get("form.cancel"));
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        cancelBtn.setOnAction(e -> handleCancel());

        bottomRow.getChildren().add(cancelBtn);
        root.getChildren().add(bottomRow);

        return root;
    }

    private VBox buildProgressMetricTile() {
        VBox tile = new VBox(6);
        tile.getStyleClass().add("metric-tile");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(I18n.get("experience.metric.progress"));
        title.getStyleClass().add("metric-tile-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        progressPctLabel = new Label(book.getFormattedProgress());
        progressPctLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -accent-primary;");

        if (I18n.isRTL()) {
            header.getChildren().addAll(progressPctLabel, title);
        } else {
            header.getChildren().addAll(title, progressPctLabel);
        }

        bookProgressBar = new ProgressBar(book.getProgressPercentage() / 100.0);
        bookProgressBar.setMaxWidth(Double.MAX_VALUE);
        bookProgressBar.setPrefHeight(8);
        bookProgressBar.getStyleClass().add("book-progress-bar");

        progressRatioLabel = new Label(currentPage + " / " + book.getTotalPages() + " " + I18n.get("details.pages_unit"));
        progressRatioLabel.getStyleClass().add("metric-tile-sub");

        tile.getChildren().addAll(header, bookProgressBar, progressRatioLabel);
        return tile;
    }

    private VBox buildTodayMetricTile() {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("metric-tile");

        Label title = new Label(I18n.get("experience.metric.today"));
        title.getStyleClass().add("metric-tile-title");

        todayPagesValueLabel = new Label(todayPagesBaseline + " " + I18n.get("details.pages_unit"));
        todayPagesValueLabel.getStyleClass().add("metric-tile-value");

        int dailyGoalPages = readingGoal != null ? readingGoal.getDailyPagesGoal() : 25;
        todayGoalSubLabel = new Label(I18n.get("experience.metric.daily_goal", (readingGoal != null ? readingGoal.getPagesReadToday() : todayPagesBaseline), dailyGoalPages));
        todayGoalSubLabel.getStyleClass().add("metric-tile-sub");

        tile.getChildren().addAll(title, todayPagesValueLabel, todayGoalSubLabel);
        return tile;
    }

    private VBox buildSpeedMetricTile() {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("metric-tile");

        Label title = new Label(I18n.get("experience.metric.speed"));
        title.getStyleClass().add("metric-tile-title");

        speedValueLabel = new Label(formatSpeed(baselinePpm));
        speedValueLabel.getStyleClass().add("metric-tile-value");

        speedSubLabel = new Label(I18n.get("experience.metric.avg_speed"));
        speedSubLabel.getStyleClass().add("metric-tile-sub");

        tile.getChildren().addAll(title, speedValueLabel, speedSubLabel);
        return tile;
    }

    private VBox buildEtaMetricTile() {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("metric-tile");

        Label title = new Label(I18n.get("experience.metric.eta"));
        title.getStyleClass().add("metric-tile-title");

        etaValueLabel = new Label(calculateEtaString());
        etaValueLabel.getStyleClass().add("metric-tile-value");

        int remainingPages = Math.max(0, book.getTotalPages() - currentPage);
        etaSubLabel = new Label(I18n.get("experience.metric.remaining_pages", remainingPages));
        etaSubLabel.getStyleClass().add("metric-tile-sub");

        tile.getChildren().addAll(title, etaValueLabel, etaSubLabel);
        return tile;
    }

    private VBox buildPageControlCard() {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("stat-card");
        card.setPadding(new Insets(14, 18, 14, 18));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(I18n.get("experience.page_controls"));
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -text-main;");
        HBox.setHgrow(label, Priority.ALWAYS);

        pagesReadBadge = new Label("+" + getPagesReadThisSession() + " " + I18n.get("details.pages_unit"));
        pagesReadBadge.getStyleClass().addAll("badge-chip", "status-reading");

        header.getChildren().addAll(label, pagesReadBadge);

        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);

        pageSpinner = new Spinner<>(0, book.getTotalPages(), currentPage);
        pageSpinner.setEditable(true);
        pageSpinner.setPrefWidth(90);
        pageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != currentPage) {
                currentPage = newVal;
                onPageUpdated();
            }
        });

        Button minus1 = new Button("-1");
        minus1.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        minus1.setOnAction(e -> advancePages(-1));

        Button plus1 = new Button("+1");
        plus1.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        plus1.setOnAction(e -> advancePages(1));

        Button plus5 = new Button("+5");
        plus5.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        plus5.setOnAction(e -> advancePages(5));

        Button plus10 = new Button("+10");
        plus10.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        plus10.setOnAction(e -> advancePages(10));

        Button plus25 = new Button("+25");
        plus25.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        plus25.setOnAction(e -> advancePages(25));

        Button finishBookBtn = new Button(I18n.get("experience.finish_book_btn"));
        finishBookBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        finishBookBtn.setOnAction(e -> {
            currentPage = book.getTotalPages();
            pageSpinner.getValueFactory().setValue(currentPage);
            onPageUpdated();
        });

        controls.getChildren().addAll(minus1, pageSpinner, plus1, plus5, plus10, plus25, finishBookBtn);
        card.getChildren().addAll(header, controls);
        return card;
    }

    private void advancePages(int delta) {
        int newPage = Math.max(0, Math.min(book.getTotalPages(), currentPage + delta));
        currentPage = newPage;
        if (pageSpinner != null) {
            pageSpinner.getValueFactory().setValue(newPage);
        }
        onPageUpdated();
    }

    private void onPageUpdated() {
        if (timerState == TimerState.READY) {
            // Auto start timer on first page progress if user hasn't explicitly started
            toggleTimer();
        }
        updateMetrics();
    }

    private void toggleTimer() {
        if (timerState == TimerState.READY || timerState == TimerState.PAUSED) {
            timerState = TimerState.RUNNING;
            timeline.play();

            startPauseButton.setText(I18n.get("experience.timer.pause"));
            startPauseButton.setGraphic(IconUtil.createIcon(IconUtil.IconType.PAUSE, 16));
            startPauseButton.getStyleClass().removeAll("btn-primary");
            if (!startPauseButton.getStyleClass().contains("btn-secondary")) {
                startPauseButton.getStyleClass().add("btn-secondary");
            }

            timerStatusBadge.setText(I18n.get("experience.timer.status.running"));
            timerStatusBadge.getStyleClass().removeAll("timer-state-ready", "timer-state-paused");
            if (!timerStatusBadge.getStyleClass().contains("timer-state-running")) {
                timerStatusBadge.getStyleClass().add("timer-state-running");
            }

            timerDigitsLabel.getParent().getStyleClass().removeAll("paused");
            if (!timerDigitsLabel.getParent().getStyleClass().contains("running")) {
                timerDigitsLabel.getParent().getStyleClass().add("running");
            }
        } else if (timerState == TimerState.RUNNING) {
            timerState = TimerState.PAUSED;
            timeline.pause();

            startPauseButton.setText(I18n.get("experience.timer.resume"));
            startPauseButton.setGraphic(IconUtil.createIcon(IconUtil.IconType.PLAY, 16));
            startPauseButton.getStyleClass().removeAll("btn-secondary");
            if (!startPauseButton.getStyleClass().contains("btn-primary")) {
                startPauseButton.getStyleClass().add("btn-primary");
            }

            timerStatusBadge.setText(I18n.get("experience.timer.status.paused"));
            timerStatusBadge.getStyleClass().removeAll("timer-state-ready", "timer-state-running");
            if (!timerStatusBadge.getStyleClass().contains("timer-state-paused")) {
                timerStatusBadge.getStyleClass().add("timer-state-paused");
            }

            timerDigitsLabel.getParent().getStyleClass().removeAll("running");
            if (!timerDigitsLabel.getParent().getStyleClass().contains("paused")) {
                timerDigitsLabel.getParent().getStyleClass().add("paused");
            }
        }
    }

    private void updateTimerDisplay() {
        int hrs = elapsedSeconds / 3600;
        int mins = (elapsedSeconds % 3600) / 60;
        int secs = elapsedSeconds % 60;

        if (hrs > 0) {
            timerDigitsLabel.setText(String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs));
        } else {
            timerDigitsLabel.setText(String.format(Locale.US, "%02d:%02d", mins, secs));
        }
    }

    private int getPagesReadThisSession() {
        return Math.max(0, currentPage - initialStartPage);
    }

    private double calculateLiveSpeedPpm() {
        int pagesRead = getPagesReadThisSession();
        if (elapsedSeconds >= 15 && pagesRead > 0) {
            return pagesRead / (elapsedSeconds / 60.0);
        }
        return baselinePpm > 0.05 ? baselinePpm : 0.8;
    }

    private String calculateEtaString() {
        int remainingPages = Math.max(0, book.getTotalPages() - currentPage);
        if (remainingPages == 0) {
            return I18n.get("experience.metric.eta_completed");
        }
        double speed = calculateLiveSpeedPpm();
        if (speed <= 0.05) speed = 0.8;
        int remainingMinutes = (int) Math.max(1, Math.round(remainingPages / speed));
        return readingTrackerService.formatEstimatedRemainingTime(remainingMinutes);
    }

    private String formatSpeed(double ppm) {
        if (ppm <= 0.0) return "—";
        return String.format(Locale.US, "%.1f", ppm) + " " + I18n.get("experience.metric.ppm_unit");
    }

    private void updateMetrics() {
        // 1. Progress
        double pct = book.getTotalPages() > 0 ? ((double) currentPage / book.getTotalPages()) * 100.0 : 0.0;
        progressPctLabel.setText(String.format(Locale.US, "%.0f%%", pct));
        AnimationUtil.animateProgressBar(bookProgressBar, Math.min(1.0, pct / 100.0));
        progressRatioLabel.setText(currentPage + " / " + book.getTotalPages() + " " + I18n.get("details.pages_unit"));

        // 2. Today's Pages
        int pagesThisSession = getPagesReadThisSession();
        int totalTodayForBook = todayPagesBaseline + pagesThisSession;
        todayPagesValueLabel.setText(totalTodayForBook + " " + I18n.get("details.pages_unit"));

        int globalToday = (readingGoal != null ? readingGoal.getPagesReadToday() : 0) + pagesThisSession;
        int dailyGoal = readingGoal != null ? readingGoal.getDailyPagesGoal() : 25;
        todayGoalSubLabel.setText(I18n.get("experience.metric.daily_goal", globalToday, dailyGoal));

        // 3. Reading Speed
        double currentSpeed = calculateLiveSpeedPpm();
        speedValueLabel.setText(formatSpeed(currentSpeed));
        if (elapsedSeconds >= 15 && pagesThisSession > 0) {
            speedSubLabel.setText(I18n.get("experience.metric.live_speed"));
        } else {
            speedSubLabel.setText(I18n.get("experience.metric.avg_speed"));
        }

        // 4. Estimated Remaining
        etaValueLabel.setText(calculateEtaString());
        int remainingPages = Math.max(0, book.getTotalPages() - currentPage);
        etaSubLabel.setText(I18n.get("experience.metric.remaining_pages", remainingPages));

        // 5. Page Controls badge
        pagesReadBadge.setText("+" + pagesThisSession + " " + I18n.get("details.pages_unit"));
    }

    private void handleFinishSession() {
        if (timeline != null) {
            timeline.stop();
        }

        int pagesRead = getPagesReadThisSession();
        // Calculate duration: minimum 1 min if user read pages or spent at least 20 seconds
        int durationMinutes = Math.max(1, (int) Math.ceil(elapsedSeconds / 60.0));

        if (pagesRead <= 0 && elapsedSeconds < 20) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(dialogStage);
            confirm.setTitle(I18n.get("experience.cancel_confirm.title"));
            confirm.setHeaderText(I18n.get("experience.cancel_confirm.header"));
            confirm.setContentText(I18n.get("experience.cancel_confirm.content"));
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    dialogStage.close();
                }
            });
            return;
        }

        ReadingSession session = new ReadingSession(
                book.getId(),
                LocalDate.now(),
                initialStartPage,
                currentPage,
                pagesRead,
                durationMinutes,
                notesArea.getText() != null ? notesArea.getText().trim() : null
        );

        try {
            readingTrackerService.logSession(session);

            boolean completed = (currentPage >= book.getTotalPages() && book.getTotalPages() > 0);
            if (completed) {
                mainController.showToast(I18n.get("experience.toast.book_completed", book.getTitle()), ToastNotification.ToastType.SUCCESS);
            } else {
                mainController.showToast(I18n.get("experience.toast.session_finished", pagesRead, DateUtil.formatDuration(durationMinutes)), ToastNotification.ToastType.SUCCESS);
            }

            if (onSuccess != null) {
                onSuccess.run();
            }
            dialogStage.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to log active reading session", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.initOwner(dialogStage);
            alert.showAndWait();
        }
    }

    private void handleCancel() {
        if (timeline != null) {
            timeline.pause();
        }

        if (elapsedSeconds > 30 || getPagesReadThisSession() > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(dialogStage);
            confirm.setTitle(I18n.get("experience.cancel_confirm.title"));
            confirm.setHeaderText(I18n.get("experience.cancel_confirm.header"));
            confirm.setContentText(I18n.get("experience.cancel_confirm.content"));
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    if (timeline != null) timeline.stop();
                    dialogStage.close();
                } else {
                    if (timerState == TimerState.RUNNING && timeline != null) {
                        timeline.play();
                    }
                }
            });
        } else {
            if (timeline != null) timeline.stop();
            dialogStage.close();
        }
    }
}
