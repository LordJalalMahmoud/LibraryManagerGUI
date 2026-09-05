package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Chapter;
import com.librarymanager.service.ChapterService;
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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modal dialog controller for adding and editing course chapter assignments.
 */
public class ChapterFormController {
    private static final Logger LOGGER = Logger.getLogger(ChapterFormController.class.getName());

    private final MainController mainController;
    private final ChapterService chapterService;
    private final Long bookId;
    private final Chapter chapterToEdit;
    private final boolean isEditMode;
    private final Runnable onSuccess;

    private Stage dialogStage;

    private TextField numberField;
    private TextField titleField;
    private TextField startPageField;
    private TextField endPageField;
    private TextArea notesArea;
    private CheckBox completedCheck;

    private Label titleError;
    private Label pageError;

    public ChapterFormController(MainController mainController,
                                 ChapterService chapterService,
                                 Long bookId,
                                 Chapter chapterToEdit,
                                 Runnable onSuccess) {
        this.mainController = mainController;
        this.chapterService = chapterService;
        this.bookId = bookId;
        this.chapterToEdit = chapterToEdit;
        this.isEditMode = chapterToEdit != null;
        this.onSuccess = onSuccess;
    }

    public void showAsDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle(isEditMode ? I18n.get("chapters.dialog.edit.title") : I18n.get("chapters.dialog.add.title"));
        dialogStage.setResizable(false);

        VBox root = buildForm();

        Scene scene = new Scene(root, 480, 520);

        // Inherit application stylesheets & theme
        String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
        String themeStyle = mainController.getSettingsService().isDarkMode()
                ? getClass().getResource("/css/theme-dark.css").toExternalForm()
                : getClass().getResource("/css/theme-light.css").toExternalForm();
        scene.getStylesheets().addAll(themeStyle, baseStyle);

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private VBox buildForm() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell", "form-container");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = IconUtil.createIcon(isEditMode ? IconUtil.IconType.EDIT : IconUtil.IconType.PLUS, 18);
        icon.setStyle("-fx-fill: -accent-primary;");

        VBox titleBox = new VBox(2);
        Label headerTitle = new Label(isEditMode ? I18n.get("chapters.dialog.edit.title") : I18n.get("chapters.dialog.add.title"));
        headerTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label headerSub = new Label(I18n.get("chapters.subtitle"));
        headerSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(headerTitle, headerSub);

        header.getChildren().addAll(icon, titleBox);
        root.getChildren().add(header);

        // Chapter Number & Title
        HBox topRow = new HBox(12);

        numberField = new TextField(isEditMode ? String.valueOf(chapterToEdit.getChapterNumber()) : "1");
        numberField.setPrefWidth(90);
        numberField.getStyleClass().add("text-input");
        VBox numberGroup = createFieldGroup(I18n.get("chapters.number"), numberField, null);

        titleField = new TextField(isEditMode ? chapterToEdit.getTitle() : "");
        titleField.setPromptText(I18n.get("chapters.chapter_title"));
        titleField.getStyleClass().add("text-input");
        titleError = createErrorLabel();
        VBox titleGroup = createFieldGroup(I18n.get("chapters.chapter_title") + " *", titleField, titleError);
        HBox.setHgrow(titleGroup, Priority.ALWAYS);

        topRow.getChildren().addAll(numberGroup, titleGroup);
        root.getChildren().add(topRow);

        // Page Range Row
        HBox pageRow = new HBox(12);

        startPageField = new TextField(isEditMode && chapterToEdit.getStartPage() > 0 ? String.valueOf(chapterToEdit.getStartPage()) : "");
        startPageField.setPromptText("e.g. 15");
        startPageField.getStyleClass().add("text-input");
        VBox startGroup = createFieldGroup(I18n.get("chapters.start_page"), startPageField, null);
        HBox.setHgrow(startGroup, Priority.ALWAYS);

        endPageField = new TextField(isEditMode && chapterToEdit.getEndPage() > 0 ? String.valueOf(chapterToEdit.getEndPage()) : "");
        endPageField.setPromptText("e.g. 45");
        endPageField.getStyleClass().add("text-input");
        VBox endGroup = createFieldGroup(I18n.get("chapters.end_page"), endPageField, null);
        HBox.setHgrow(endGroup, Priority.ALWAYS);

        pageRow.getChildren().addAll(startGroup, endGroup);
        pageError = createErrorLabel();

        VBox pageContainer = new VBox(4, pageRow, pageError);
        root.getChildren().add(pageContainer);

        // Notes / Topic Area
        notesArea = new TextArea(isEditMode ? chapterToEdit.getNotes() : "");
        notesArea.setPromptText(I18n.get("chapters.notes"));
        notesArea.getStyleClass().add("text-input");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        root.getChildren().add(createFieldGroup(I18n.get("chapters.notes"), notesArea, null));

        // Completion Checkbox
        completedCheck = new CheckBox(I18n.get("chapters.completed"));
        completedCheck.setSelected(isEditMode && chapterToEdit.isCompleted());
        completedCheck.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 600;");
        root.getChildren().add(completedCheck);

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button cancelBtn = new Button(I18n.get("form.cancel"));
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button saveBtn = new Button(isEditMode ? I18n.get("form.save") : I18n.get("form.add"));
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        saveBtn.setOnAction(e -> handleSave());

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().add(footer);

        return root;
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

    private Label createErrorLabel() {
        Label lbl = new Label();
        lbl.getStyleClass().add("form-error-label");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void handleSave() {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            titleError.setText(I18n.get("form.error.title_empty"));
            titleError.setVisible(true);
            titleError.setManaged(true);
            return;
        }
        titleError.setVisible(false);
        titleError.setManaged(false);

        int chNum = 1;
        try {
            chNum = Integer.parseInt(numberField.getText().trim());
        } catch (NumberFormatException ignored) {
        }

        int startPage = 0;
        if (startPageField.getText() != null && !startPageField.getText().trim().isEmpty()) {
            try {
                startPage = Integer.parseInt(startPageField.getText().trim());
            } catch (NumberFormatException e) {
                pageError.setText(I18n.get("form.error.integer"));
                pageError.setVisible(true);
                pageError.setManaged(true);
                return;
            }
        }

        int endPage = 0;
        if (endPageField.getText() != null && !endPageField.getText().trim().isEmpty()) {
            try {
                endPage = Integer.parseInt(endPageField.getText().trim());
            } catch (NumberFormatException e) {
                pageError.setText(I18n.get("form.error.integer"));
                pageError.setVisible(true);
                pageError.setManaged(true);
                return;
            }
        }

        if (endPage > 0 && startPage > 0 && endPage < startPage) {
            pageError.setText(I18n.get("form.error.current_exceeds", endPage));
            pageError.setVisible(true);
            pageError.setManaged(true);
            return;
        }
        pageError.setVisible(false);
        pageError.setManaged(false);

        Chapter target = isEditMode ? chapterToEdit : new Chapter();
        target.setBookId(bookId);
        target.setChapterNumber(chNum);
        target.setTitle(titleField.getText().trim());
        target.setStartPage(startPage);
        target.setEndPage(endPage);
        target.setCompleted(completedCheck.isSelected());
        target.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : null);

        try {
            if (isEditMode) {
                chapterService.updateChapter(target);
                mainController.showToast(I18n.get("toast.chapter_updated"), ToastNotification.ToastType.SUCCESS);
            } else {
                chapterService.addChapter(target);
                mainController.showToast(I18n.get("toast.chapter_added"), ToastNotification.ToastType.SUCCESS);
            }
            if (onSuccess != null) onSuccess.run();
            dialogStage.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save chapter", e);
            mainController.showToast("Error: " + e.getMessage(), ToastNotification.ToastType.ERROR);
        }
    }
}
