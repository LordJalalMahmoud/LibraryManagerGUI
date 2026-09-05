package com.librarymanager.util;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Modern themed and localized dialog utilities for confirmation, alerts, and user prompts.
 */
public class DialogUtil {

    private static void applyThemeAndOrientation(Dialog<?> dialog) {
        if (dialog != null && dialog.getDialogPane() != null) {
            DialogPane pane = dialog.getDialogPane();
            pane.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            try {
                pane.getStylesheets().clear();
                String baseStyle = DialogUtil.class.getResource("/css/styles.css").toExternalForm();
                String themeStyle = null;
                if (dialog.getOwner() instanceof Stage stage && stage.getScene() != null) {
                    for (String sheet : stage.getScene().getStylesheets()) {
                        if (sheet.contains("theme-")) {
                            themeStyle = sheet;
                            break;
                        }
                    }
                }
                if (themeStyle == null) {
                    themeStyle = DialogUtil.class.getResource("/css/theme-dark.css").toExternalForm();
                }
                pane.getStylesheets().addAll(themeStyle, baseStyle);
                pane.getStyleClass().add("form-container");
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean confirmDelete(Stage owner, String itemDescription) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(alert);

        alert.setTitle(I18n.get("dialog.confirm_delete.title"));
        alert.setHeaderText(I18n.get("dialog.confirm_delete.header", itemDescription));
        alert.setContentText(I18n.get("dialog.confirm_delete.content"));

        ButtonType deleteButton = new ButtonType(I18n.get("dialog.confirm_delete.delete"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(I18n.get("dialog.confirm_delete.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == deleteButton;
    }

    public static boolean confirmResetLibrary(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(alert);

        alert.setTitle(I18n.get("dialog.reset.title"));
        alert.setHeaderText(I18n.get("dialog.reset.header"));
        alert.setContentText(I18n.get("dialog.reset.content"));

        ButtonType confirmButton = new ButtonType(I18n.get("dialog.reset.confirm"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(I18n.get("dialog.reset.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }

    public static void showError(Stage owner, String title, String userFriendlyMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(alert);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(userFriendlyMessage);
        alert.showAndWait();
    }

    public static void showInfo(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(alert);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static Optional<String> promptText(Stage owner, String title, String header, String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue != null ? defaultValue : "");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(dialog);

        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(prompt);

        return dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    public static Optional<String> promptChoice(Stage owner, String title, String header, String prompt, List<String> choices, String defaultChoice) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(dialog);

        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(prompt);

        return dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    public static boolean confirm(Stage owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        applyThemeAndOrientation(alert);

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        ButtonType confirmButton = new ButtonType(I18n.get("dialog.confirm"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(I18n.get("form.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }
}
