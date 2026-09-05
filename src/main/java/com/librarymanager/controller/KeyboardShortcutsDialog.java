package com.librarymanager.controller;

import com.librarymanager.service.SettingsService;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Modal cheat sheet dialog presenting all application keyboard shortcuts.
 */
public class KeyboardShortcutsDialog {

    private final SettingsService settingsService;

    public KeyboardShortcutsDialog(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void show(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.get("shortcuts.title"));
        dialog.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell", "shortcuts-dialog");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        root.setPrefWidth(580);
        root.setMaxWidth(580);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("brand-icon-box");
        SVGPath icon = IconUtil.createIcon(IconUtil.IconType.SETTINGS, 20);
        icon.setStyle("-fx-fill: -accent-primary;");
        iconBox.getChildren().add(icon);

        VBox headerText = new VBox(2);
        Label titleLabel = new Label(I18n.get("shortcuts.title"));
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -text-main;");

        Label subtitleLabel = new Label(I18n.get("shortcuts.subtitle"));
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        headerText.getChildren().addAll(titleLabel, subtitleLabel);

        header.getChildren().addAll(iconBox, headerText);
        root.getChildren().add(header);

        // Content
        VBox content = new VBox(14);
        content.setPadding(new Insets(4, 8, 4, 4));

        // Category 1: Navigation
        content.getChildren().add(createCategory(I18n.get("shortcuts.category.navigation"), List.of(
                new ShortcutEntry(List.of("Ctrl", "1"), I18n.get("shortcuts.desc.dashboard")),
                new ShortcutEntry(List.of("Ctrl", "2"), I18n.get("shortcuts.desc.all_books")),
                new ShortcutEntry(List.of("Ctrl", "3"), I18n.get("shortcuts.desc.reading")),
                new ShortcutEntry(List.of("Ctrl", "4"), I18n.get("shortcuts.desc.completed")),
                new ShortcutEntry(List.of("Ctrl", "5"), I18n.get("shortcuts.desc.data_mgmt")),
                new ShortcutEntry(List.of("Ctrl", ","), I18n.get("shortcuts.desc.settings"))
        )));

        // Category 2: Actions & Books
        content.getChildren().add(createCategory(I18n.get("shortcuts.category.books"), List.of(
                new ShortcutEntry(List.of("Ctrl", "N"), I18n.get("shortcuts.desc.add_book")),
                new ShortcutEntry(List.of("Ctrl", "R"), I18n.get("shortcuts.desc.reading_timer")),
                new ShortcutEntry(List.of("Ctrl", "B"), I18n.get("shortcuts.desc.backup")),
                new ShortcutEntry(List.of("Ctrl", "Shift", "D"), I18n.get("shortcuts.desc.customize_dash"))
        )));

        // Category 3: Search & Filters
        content.getChildren().add(createCategory(I18n.get("shortcuts.category.search"), List.of(
                new ShortcutEntry(List.of("Ctrl", "F"), I18n.get("shortcuts.desc.search")),
                new ShortcutEntry(List.of("Esc"), I18n.get("shortcuts.desc.escape"))
        )));

        // Category 4: System & Accessibility
        content.getChildren().add(createCategory(I18n.get("shortcuts.category.system"), List.of(
                new ShortcutEntry(List.of("Ctrl", "D"), I18n.get("shortcuts.desc.toggle_theme")),
                new ShortcutEntry(List.of("F1"), I18n.get("shortcuts.desc.shortcuts_dialog"))
        )));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setPrefHeight(420);
        root.getChildren().add(scrollPane);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button(I18n.get("dialog.close"));
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(closeBtn);
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

    private VBox createCategory(String categoryName, List<ShortcutEntry> entries) {
        VBox box = new VBox(6);
        Label title = new Label(categoryName);
        title.getStyleClass().add("shortcut-category-header");
        box.getChildren().add(title);

        for (ShortcutEntry entry : entries) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("shortcut-row");

            HBox keysBox = new HBox(4);
            keysBox.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < entry.keys.size(); i++) {
                Label keyBadge = new Label(entry.keys.get(i));
                keyBadge.getStyleClass().add("key-badge");
                keysBox.getChildren().add(keyBadge);
                if (i < entry.keys.size() - 1) {
                    Label plus = new Label("+");
                    plus.setStyle("-fx-text-fill: -text-muted; -fx-font-weight: 700;");
                    keysBox.getChildren().add(plus);
                }
            }
            keysBox.setPrefWidth(160);
            keysBox.setMinWidth(160);

            Label descLabel = new Label(entry.description);
            descLabel.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 13px; -fx-font-weight: 500;");
            HBox.setHgrow(descLabel, Priority.ALWAYS);

            row.getChildren().addAll(keysBox, descLabel);
            box.getChildren().add(row);
        }

        return box;
    }

    private record ShortcutEntry(List<String> keys, String description) {}
}
