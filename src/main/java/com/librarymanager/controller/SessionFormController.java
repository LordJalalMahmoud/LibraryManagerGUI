package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingSession;
import com.librarymanager.service.ReadingTrackerService;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modal dialog controller for logging a new reading session with live page calculation.
 */
public class SessionFormController {
    private static final Logger LOGGER = Logger.getLogger(SessionFormController.class.getName());

    private final MainController mainController;
    private final ReadingTrackerService readingTrackerService;
    private final Book book;
    private final Runnable onSuccess;

    private Stage dialogStage;

    private DatePicker datePicker;
    private TextField startPageField;
    private TextField endPageField;
    private TextField pagesReadField;
    private TextField durationField;
    private TextArea notesArea;

    private Label pageError;

    public SessionFormController(MainController mainController,
                                 ReadingTrackerService readingTrackerService,
                                 Book book,
                                 Runnable onSuccess) {
        this.mainController = mainController;
        this.readingTrackerService = readingTrackerService;
        this.book = book;
        this.onSuccess = onSuccess;
    }

    public void showAsDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle(I18n.get("session.dialog.title") + " — " + book.getTitle());
        dialogStage.setResizable(false);

        VBox root = buildForm();

        Scene scene = new Scene(root, 490, 560);

        String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
        String themeStyle = mainController.getSettingsService().isDarkMode()
                ? getClass().getResource("/css/theme-dark.css").toExternalForm()
                : getClass().getResource("/css/theme-light.css").toExternalForm();
        scene.getStylesheets().addAll(themeStyle, baseStyle);

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private VBox buildForm() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell", "form-container");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = IconUtil.createIcon(IconUtil.IconType.CLOCK, 20);
        icon.setStyle("-fx-fill: -accent-primary;");

        VBox titleBox = new VBox(2);
        Label headerTitle = new Label(I18n.get("session.dialog.title"));
        headerTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label headerSub = new Label(book.getTitle() + " (" + book.getCurrentPage() + "/" + book.getTotalPages() + " pages)");
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(headerTitle, headerSub);

        header.getChildren().addAll(icon, titleBox);
        root.getChildren().add(header);

        // Date Picker Row
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.getStyleClass().add("text-input");
        VBox dateGroup = createFieldGroup(I18n.get("session.dialog.date") + " *", datePicker, null);
        root.getChildren().add(dateGroup);

        // Start Page & End Page Row
        HBox pageRow = new HBox(12);

        startPageField = new TextField(String.valueOf(book.getCurrentPage()));
        startPageField.getStyleClass().add("text-input");
        VBox startGroup = createFieldGroup(I18n.get("session.dialog.start_page"), startPageField, null);
        HBox.setHgrow(startGroup, Priority.ALWAYS);

        endPageField = new TextField(String.valueOf(Math.min(book.getTotalPages(), book.getCurrentPage() + 10)));
        endPageField.getStyleClass().add("text-input");
        VBox endGroup = createFieldGroup(I18n.get("session.dialog.end_page"), endPageField, null);
        HBox.setHgrow(endGroup, Priority.ALWAYS);

        pageRow.getChildren().addAll(startGroup, endGroup);

        pageError = new Label();
        pageError.getStyleClass().add("form-error-label");
        pageError.setVisible(false);
        pageError.setManaged(false);

        VBox pageContainer = new VBox(4, pageRow, pageError);
        root.getChildren().add(pageContainer);

        // Pages Read & Duration Row
        HBox metricsRow = new HBox(12);

        int initialPages = Math.max(0, Math.min(book.getTotalPages(), book.getCurrentPage() + 10) - book.getCurrentPage());
        pagesReadField = new TextField(String.valueOf(initialPages));
        pagesReadField.getStyleClass().add("text-input");
        VBox pagesReadGroup = createFieldGroup(I18n.get("session.dialog.pages_read"), pagesReadField, null);
        HBox.setHgrow(pagesReadGroup, Priority.ALWAYS);

        durationField = new TextField("30");
        durationField.setPromptText(I18n.get("session.dialog.duration_prompt"));
        durationField.getStyleClass().add("text-input");
        VBox durationGroup = createFieldGroup(I18n.get("session.dialog.duration"), durationField, null);
        HBox.setHgrow(durationGroup, Priority.ALWAYS);

        metricsRow.getChildren().addAll(pagesReadGroup, durationGroup);
        root.getChildren().add(metricsRow);

        // Auto-calculate pages read when start or end page changes
        startPageField.textProperty().addListener((obs, oldV, newV) -> updateCalculatedPages());
        endPageField.textProperty().addListener((obs, oldV, newV) -> updateCalculatedPages());

        // Notes Area
        notesArea = new TextArea();
        notesArea.setPromptText(I18n.get("session.dialog.notes_prompt"));
        notesArea.getStyleClass().add("text-input");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        root.getChildren().add(createFieldGroup(I18n.get("session.dialog.notes"), notesArea, null));

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button cancelBtn = new Button(I18n.get("form.cancel"));
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button saveBtn = new Button(I18n.get("sessions.log_button"));
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        saveBtn.setOnAction(e -> handleSave());

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().add(footer);

        return root;
    }

    private void updateCalculatedPages() {
        try {
            int start = Integer.parseInt(startPageField.getText().trim());
            int end = Integer.parseInt(endPageField.getText().trim());
            if (end >= start) {
                pagesReadField.setText(String.valueOf(end - start));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private VBox createFieldGroup(String labelText, javafx.scene.Node control, Label errorLabel) {
        VBox group = new VBox(4);
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        group.getChildren().addAll(label, control);
        if (errorLabel != null) {
            group.getChildren().add(errorLabel);
        }
        return group;
    }

    private void handleSave() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            date = LocalDate.now();
        }

        int startPage = 0;
        try {
            startPage = Integer.parseInt(startPageField.getText().trim());
        } catch (NumberFormatException e) {
            showError(I18n.get("form.error.integer"));
            return;
        }

        int endPage = 0;
        try {
            endPage = Integer.parseInt(endPageField.getText().trim());
        } catch (NumberFormatException e) {
            showError(I18n.get("form.error.integer"));
            return;
        }

        if (startPage < 0 || endPage < 0) {
            showError(I18n.get("form.error.current_negative"));
            return;
        }

        if (endPage > 0 && endPage < startPage) {
            showError(I18n.get("form.error.current_exceeds", endPage));
            return;
        }

        int pagesRead = Math.max(0, endPage - startPage);
        if (pagesReadField.getText() != null && !pagesReadField.getText().trim().isEmpty()) {
            try {
                pagesRead = Integer.parseInt(pagesReadField.getText().trim());
            } catch (NumberFormatException ignored) {
            }
        }

        int duration = 0;
        if (durationField.getText() != null && !durationField.getText().trim().isEmpty()) {
            try {
                duration = Integer.parseInt(durationField.getText().trim());
            } catch (NumberFormatException ignored) {
            }
        }

        ReadingSession session = new ReadingSession(
                book.getId(),
                date,
                startPage,
                endPage,
                pagesRead,
                duration,
                notesArea.getText() != null ? notesArea.getText().trim() : null
        );

        try {
            readingTrackerService.logSession(session);
            mainController.showToast(I18n.get("toast.session_logged", pagesRead), ToastNotification.ToastType.SUCCESS);
            if (onSuccess != null) {
                onSuccess.run();
            }
            dialogStage.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to log reading session", e);
            showError("Error: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        pageError.setText(msg);
        pageError.setVisible(true);
        pageError.setManaged(true);
    }
}
