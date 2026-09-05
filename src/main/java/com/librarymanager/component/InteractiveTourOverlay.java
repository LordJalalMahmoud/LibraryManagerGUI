package com.librarymanager.component;

import com.librarymanager.controller.MainController;
import com.librarymanager.model.Book;
import com.librarymanager.service.BookService;
import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive product tour overlay presenting a comprehensive, 12-step guided walkthrough
 * across the entire application shell, steering views underneath as the user advances.
 */
public class InteractiveTourOverlay extends StackPane {

    public record TourStep(
            String id,
            IconUtil.IconType iconType,
            String categoryKey,
            String titleKey,
            String descKey,
            String shortcutBadge,
            Runnable action
    ) {}

    private final MainController mainController;
    private final BookService bookService;
    private final List<TourStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;

    // UI Nodes
    private VBox card;
    private Label categoryLabel;
    private Label stepCounterLabel;
    private Button closeBtn;
    private ProgressBar progressBar;
    private StackPane iconContainer;
    private SVGPath iconView;
    private Label titleLabel;
    private Label shortcutBadgeLabel;
    private Label descLabel;
    private HBox dotsBox;
    private final List<Region> dotIndicators = new ArrayList<>();
    private Button skipBtn;
    private Button prevBtn;
    private Button nextBtn;

    public InteractiveTourOverlay(MainController mainController, BookService bookService) {
        this.mainController = mainController;
        this.bookService = bookService;

        initSteps();
        buildUi();
        setupKeyHandlers();
    }

    private void initSteps() {
        steps.clear();

        // 1. Welcome & Introduction
        steps.add(new TourStep(
                "welcome",
                IconUtil.IconType.SPARKLES,
                "tour.cat.welcome",
                "tour.step1.title",
                "tour.step1.desc",
                "Ctrl + T",
                () -> mainController.navigateToDashboard()
        ));

        // 2. Sidebar & Command Hub
        steps.add(new TourStep(
                "navigation",
                IconUtil.IconType.LIBRARY,
                "tour.cat.navigation",
                "tour.step2.title",
                "tour.step2.desc",
                "Ctrl + 1..5",
                () -> mainController.navigateToDashboard()
        ));

        // 3. Live Dashboard & Streaks
        steps.add(new TourStep(
                "dashboard",
                IconUtil.IconType.FIRE,
                "tour.cat.dashboard",
                "tour.step3.title",
                "tour.step3.desc",
                "Ctrl + Shift + D",
                () -> mainController.navigateToDashboard()
        ));

        // 4. Smart Library & Search
        steps.add(new TourStep(
                "library",
                IconUtil.IconType.SEARCH,
                "tour.cat.library",
                "tour.step4.title",
                "tour.step4.desc",
                "Ctrl + F",
                () -> mainController.navigateToLibrary(null)
        ));

        // 5. Multi-Select & Bulk Actions
        steps.add(new TourStep(
                "bulk",
                IconUtil.IconType.CHECK,
                "tour.cat.bulk",
                "tour.step5.title",
                "tour.step5.desc",
                "☑ Multi-Select",
                () -> mainController.navigateToLibrary(null)
        ));

        // 6. Drag & Drop Import
        steps.add(new TourStep(
                "dragdrop",
                IconUtil.IconType.IMPORT_FILE,
                "tour.cat.dragdrop",
                "tour.step6.title",
                "tour.step6.desc",
                "Drag & Drop",
                () -> mainController.navigateToLibrary(null)
        ));

        // 7. Academic Chapters & Page Ranges
        steps.add(new TourStep(
                "chapters",
                IconUtil.IconType.PAGES,
                "tour.cat.chapters",
                "tour.step7.title",
                "tour.step7.desc",
                "pp. 45 - 80",
                this::showFirstBookOrLibrary
        ));

        // 8. Active Reading Companion Timer
        steps.add(new TourStep(
                "timer",
                IconUtil.IconType.CLOCK,
                "tour.cat.experience",
                "tour.step8.title",
                "tour.step8.desc",
                "Ctrl + R",
                this::showFirstBookOrLibrary
        ));

        // 9. Real-Time Speed & ETA Projection
        steps.add(new TourStep(
                "speed_eta",
                IconUtil.IconType.SPEED,
                "tour.cat.speed_eta",
                "tour.step9.title",
                "tour.step9.desc",
                "PPM & ETA",
                this::showFirstBookOrLibrary
        ));

        // 10. Progress Timeline & Milestones
        steps.add(new TourStep(
                "milestones",
                IconUtil.IconType.TROPHY,
                "tour.cat.milestones",
                "tour.step10.title",
                "tour.step10.desc",
                "🚀 🎯 🌟 ⚡ 🏆",
                this::showFirstBookOrLibrary
        ));

        // 11. Data Portability, Backups & SQLite Integrity
        steps.add(new TourStep(
                "datamgmt",
                IconUtil.IconType.DATABASE,
                "tour.cat.datamgmt",
                "tour.step11.title",
                "tour.step11.desc",
                "Ctrl + B",
                () -> mainController.navigateToDataManagement()
        ));

        // 12. Customization, Accessibility & Shortcuts
        steps.add(new TourStep(
                "accessibility",
                IconUtil.IconType.SETTINGS,
                "tour.cat.accessibility",
                "tour.step12.title",
                "tour.step12.desc",
                "F1 / Ctrl + D",
                () -> mainController.navigateToDashboard()
        ));
    }

    private void showFirstBookOrLibrary() {
        List<Book> books = bookService.getAllBooks();
        if (!books.isEmpty()) {
            mainController.navigateToBookDetails(books.get(0));
        } else {
            mainController.navigateToLibrary(null);
        }
    }

    private void buildUi() {
        getStyleClass().add("tour-overlay");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);
        updateOrientation();

        card = new VBox(16);
        card.getStyleClass().add("tour-card");
        card.setMaxWidth(530);
        card.setMinWidth(480);
        card.setAlignment(Pos.TOP_LEFT);

        // 1. Header (Category pill, step counter, spacer, close button)
        HBox header = new HBox(10);
        header.getStyleClass().add("tour-header");
        header.setAlignment(Pos.CENTER_LEFT);

        categoryLabel = new Label();
        categoryLabel.getStyleClass().add("tour-category-pill");

        stepCounterLabel = new Label();
        stepCounterLabel.getStyleClass().add("tour-step-counter");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        closeBtn = new Button();
        closeBtn.getStyleClass().addAll("btn", "btn-icon", "tour-close-btn");
        closeBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.CLOSE, 14));
        closeBtn.setTooltip(new Tooltip(I18n.get("dialog.close")));
        closeBtn.setOnAction(e -> closeTour());

        header.getChildren().addAll(categoryLabel, stepCounterLabel, headerSpacer, closeBtn);

        // 2. Progress bar
        progressBar = new ProgressBar(0.0);
        progressBar.getStyleClass().add("tour-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        // 3. Body (Icon box on left, Title + Shortcut badge + Description on right)
        HBox body = new HBox(18);
        body.getStyleClass().add("tour-body");
        body.setAlignment(Pos.TOP_LEFT);

        iconContainer = new StackPane();
        iconContainer.getStyleClass().add("tour-icon-box");

        VBox textCol = new VBox(8);
        textCol.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label();
        titleLabel.getStyleClass().add("tour-title");

        shortcutBadgeLabel = new Label();
        shortcutBadgeLabel.getStyleClass().add("tour-shortcut-badge");

        titleRow.getChildren().addAll(titleLabel, shortcutBadgeLabel);

        descLabel = new Label();
        descLabel.getStyleClass().add("tour-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(420);

        textCol.getChildren().addAll(titleRow, descLabel);
        body.getChildren().addAll(iconContainer, textCol);

        // 4. Dot indicators
        dotsBox = new HBox(6);
        dotsBox.getStyleClass().add("tour-dots-box");
        dotsBox.setAlignment(Pos.CENTER);
        buildDotIndicators();

        // 5. Footer (Skip on left, Prev + Next on right)
        HBox footer = new HBox(12);
        footer.getStyleClass().add("tour-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        skipBtn = new Button(I18n.get("tour.skip"));
        skipBtn.getStyleClass().addAll("btn", "btn-ghost");
        skipBtn.setOnAction(e -> closeTour());

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        prevBtn = new Button(I18n.get("tour.prev"));
        prevBtn.getStyleClass().addAll("btn", "btn-secondary");
        prevBtn.setOnAction(e -> prevStep());

        nextBtn = new Button(I18n.get("tour.next"));
        nextBtn.getStyleClass().addAll("btn", "btn-primary");
        nextBtn.setDefaultButton(true);
        nextBtn.setOnAction(e -> nextStep());

        footer.getChildren().addAll(skipBtn, footerSpacer, prevBtn, nextBtn);

        card.getChildren().addAll(header, progressBar, body, dotsBox, footer);
        getChildren().add(card);
    }

    private void buildDotIndicators() {
        dotsBox.getChildren().clear();
        dotIndicators.clear();

        for (int i = 0; i < steps.size(); i++) {
            final int stepIdx = i;
            Region dot = new Region();
            dot.getStyleClass().add("tour-dot");
            dot.setOnMouseClicked(e -> goToStep(stepIdx));
            dotIndicators.add(dot);
            dotsBox.getChildren().add(dot);
        }
    }

    private void setupKeyHandlers() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!isVisible()) return;

            if (event.getCode() == KeyCode.ESCAPE) {
                closeTour();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                if (I18n.isRTL()) {
                    prevStep();
                } else {
                    nextStep();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                if (I18n.isRTL()) {
                    nextStep();
                } else {
                    prevStep();
                }
                event.consume();
            }
        });
    }

    public void updateOrientation() {
        setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
    }

    public void refreshTexts() {
        updateOrientation();
        skipBtn.setText(I18n.get("tour.skip"));
        prevBtn.setText(I18n.get("tour.prev"));
        closeBtn.setTooltip(new Tooltip(I18n.get("dialog.close")));
        renderStep(currentStepIndex);
    }

    public void startTour() {
        updateOrientation();
        currentStepIndex = 0;
        setVisible(true);
        setManaged(true);
        renderStep(0);
        AnimationUtil.fadeIn(this, Duration.millis(200), () -> requestFocus());
    }

    public void nextStep() {
        if (currentStepIndex < steps.size() - 1) {
            goToStep(currentStepIndex + 1);
        } else {
            finishTour();
        }
    }

    public void prevStep() {
        if (currentStepIndex > 0) {
            goToStep(currentStepIndex - 1);
        }
    }

    public void goToStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        currentStepIndex = index;
        renderStep(index);
    }

    private void renderStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        TourStep step = steps.get(index);

        // Update labels
        categoryLabel.setText(I18n.get(step.categoryKey()));
        stepCounterLabel.setText(I18n.get("tour.step_counter", (index + 1), steps.size()));
        titleLabel.setText(I18n.get(step.titleKey()));
        descLabel.setText(I18n.get(step.descKey()));

        if (step.shortcutBadge() != null && !step.shortcutBadge().isBlank()) {
            shortcutBadgeLabel.setText(step.shortcutBadge());
            shortcutBadgeLabel.setVisible(true);
            shortcutBadgeLabel.setManaged(true);
        } else {
            shortcutBadgeLabel.setVisible(false);
            shortcutBadgeLabel.setManaged(false);
        }

        // Update Icon
        iconContainer.getChildren().clear();
        iconView = IconUtil.createIcon(step.iconType(), 24);
        iconContainer.getChildren().add(iconView);

        // Update Progress Bar
        double progress = (double) (index + 1) / steps.size();
        progressBar.setProgress(progress);

        // Update Dots
        for (int i = 0; i < dotIndicators.size(); i++) {
            Region dot = dotIndicators.get(i);
            dot.getStyleClass().remove("tour-dot-active");
            if (i == index) {
                dot.getStyleClass().add("tour-dot-active");
            }
        }

        // Update buttons
        prevBtn.setDisable(index == 0);
        if (index == steps.size() - 1) {
            nextBtn.setText(I18n.get("tour.finish"));
        } else {
            nextBtn.setText(I18n.get("tour.next"));
        }

        // Execute step-specific application steering action
        try {
            if (step.action() != null) {
                step.action().run();
            }
        } catch (Exception ex) {
            // Ignore any background view layout hiccups
        }

        // Subtle animation on content
        AnimationUtil.slideFadeIn(card, Duration.millis(160), 6.0, null);
    }

    public void closeTour() {
        AnimationUtil.fadeOut(this, Duration.millis(160), () -> {
            setVisible(false);
            setManaged(false);
        });
    }

    public void finishTour() {
        closeTour();
        Platform.runLater(() -> {
            mainController.navigateToDashboard();
            mainController.showToast(I18n.get("tour.toast.completed"), ToastNotification.ToastType.SUCCESS);
        });
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public int getTotalSteps() {
        return steps.size();
    }

    public List<TourStep> getSteps() {
        return List.copyOf(steps);
    }
}
