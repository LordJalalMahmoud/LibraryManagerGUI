package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;

/**
 * Dialog allowing users to reorder and toggle visibility of individual dashboard sections.
 */
public class DashboardCustomizationDialog {

    private final MainController mainController;
    private final SettingsService settingsService;
    private final DashboardController dashboardController;

    private List<String> currentOrder;
    private final Map<String, Boolean> visibilityMap = new LinkedHashMap<>();
    private VBox listContainer;

    public DashboardCustomizationDialog(MainController mainController,
                                        SettingsService settingsService,
                                        DashboardController dashboardController) {
        this.mainController = mainController;
        this.settingsService = settingsService;
        this.dashboardController = dashboardController;
    }

    public void show(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.get("dashboard.customize.title"));
        dialog.setResizable(false);

        currentOrder = new ArrayList<>(settingsService.getDashboardSectionOrder());
        for (String sec : currentOrder) {
            visibilityMap.put(sec, settingsService.isDashboardSectionVisible(sec));
        }

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        root.setPrefWidth(540);
        root.setMaxWidth(540);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("brand-icon-box");
        SVGPath icon = IconUtil.createIcon(IconUtil.IconType.DASHBOARD, 20);
        icon.setStyle("-fx-fill: -accent-primary;");
        iconBox.getChildren().add(icon);

        VBox headerText = new VBox(2);
        Label titleLabel = new Label(I18n.get("dashboard.customize.title"));
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label subtitleLabel = new Label(I18n.get("dashboard.customize.subtitle"));
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        headerText.getChildren().addAll(titleLabel, subtitleLabel);

        header.getChildren().addAll(iconBox, headerText);
        root.getChildren().add(header);

        // Section List
        listContainer = new VBox(10);
        renderList();

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setPrefHeight(340);
        root.getChildren().add(scrollPane);

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button resetBtn = new Button(I18n.get("dashboard.customize.reset"));
        resetBtn.getStyleClass().addAll("btn", "btn-secondary");
        resetBtn.setOnAction(e -> {
            settingsService.resetDashboardLayout();
            currentOrder = new ArrayList<>(SettingsService.DEFAULT_DASHBOARD_ORDER);
            visibilityMap.clear();
            for (String sec : currentOrder) {
                visibilityMap.put(sec, true);
            }
            renderList();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = new Button(I18n.get("dialog.close"));
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button(I18n.get("dashboard.customize.save"));
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        saveBtn.setOnAction(e -> {
            applyChanges();
            dialog.close();
        });

        footer.getChildren().addAll(resetBtn, spacer, cancelBtn, saveBtn);
        root.getChildren().add(footer);

        Scene scene = new Scene(root);
        String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
        String themeStyle;
        if (settingsService.isHighContrast()) {
            themeStyle = getClass().getResource("/css/theme-high-contrast.css").toExternalForm();
        } else if (settingsService.isDarkMode()) {
            themeStyle = getClass().getResource("/css/theme-dark.css").toExternalForm();
        } else {
            themeStyle = getClass().getResource("/css/theme-light.css").toExternalForm();
        }
        scene.getStylesheets().addAll(themeStyle, baseStyle);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void renderList() {
        listContainer.getChildren().clear();
        for (int i = 0; i < currentOrder.size(); i++) {
            final int index = i;
            String secId = currentOrder.get(i);
            boolean isVis = visibilityMap.getOrDefault(secId, true);

            HBox row = new HBox(12);
            row.getStyleClass().add("dashboard-custom-item");
            row.setAlignment(Pos.CENTER_LEFT);

            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(isVis);
            checkBox.selectedProperty().addListener((obs, oldV, newV) -> visibilityMap.put(secId, newV));

            Label nameLabel = new Label(getSectionDisplayName(secId));
            nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-main;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Button upBtn = new Button("▲");
            upBtn.getStyleClass().addAll("btn", "btn-secondary");
            upBtn.setStyle("-fx-padding: 3px 8px; -fx-font-size: 10px;");
            upBtn.setDisable(index == 0);
            upBtn.setOnAction(e -> {
                Collections.swap(currentOrder, index, index - 1);
                renderList();
            });

            Button downBtn = new Button("▼");
            downBtn.getStyleClass().addAll("btn", "btn-secondary");
            downBtn.setStyle("-fx-padding: 3px 8px; -fx-font-size: 10px;");
            downBtn.setDisable(index == currentOrder.size() - 1);
            downBtn.setOnAction(e -> {
                Collections.swap(currentOrder, index, index + 1);
                renderList();
            });

            row.getChildren().addAll(checkBox, nameLabel, upBtn, downBtn);
            listContainer.getChildren().add(row);
        }
    }

    private String getSectionDisplayName(String sectionId) {
        return switch (sectionId) {
            case SettingsService.SECTION_METRICS -> I18n.get("dashboard.section.metrics");
            case SettingsService.SECTION_YEARLY -> I18n.get("dashboard.section.yearly");
            case SettingsService.SECTION_CHARTS -> I18n.get("dashboard.section.charts");
            case SettingsService.SECTION_GOALS -> I18n.get("dashboard.section.goals");
            case SettingsService.SECTION_CURRENTLY_READING -> I18n.get("dashboard.section.reading");
            case SettingsService.SECTION_RECENT_SESSIONS -> I18n.get("dashboard.section.sessions");
            case SettingsService.SECTION_RECENT_BOOKS -> I18n.get("dashboard.section.recent");
            default -> sectionId;
        };
    }

    private void applyChanges() {
        settingsService.setDashboardSectionOrder(currentOrder);
        for (Map.Entry<String, Boolean> entry : visibilityMap.entrySet()) {
            settingsService.setDashboardSectionVisible(entry.getKey(), entry.getValue());
        }
        if (dashboardController != null) {
            dashboardController.rebuildDashboardLayout();
        }
        mainController.showToast(I18n.get("dashboard.toast.customized"), ToastNotification.ToastType.SUCCESS);
    }
}
