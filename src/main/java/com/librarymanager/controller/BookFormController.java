package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BookService;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * Controller and dialog view for adding and editing books with live validation, i18n & RTL.
 */
public class BookFormController {

    private final MainController mainController;
    private final BookService bookService;
    private final Book bookToEdit;
    private final boolean isEditMode;

    private Stage dialogStage;

    // Form inputs
    private TextField titleField;
    private TextField authorField;
    private TextField totalPagesField;
    private TextField totalPartsField;
    private TextField currentPageField;
    private ComboBox<ReadingStatus> statusComboBox;
    private TextField coverImageField;
    private TextArea descriptionArea;

    // Validation error labels
    private Label titleError;
    private Label authorError;
    private Label pagesError;
    private Label partsError;
    private Label currentError;

    private Button saveButton;

    public BookFormController(MainController mainController, BookService bookService, Book bookToEdit) {
        this.mainController = mainController;
        this.bookService = bookService;
        this.bookToEdit = bookToEdit;
        this.isEditMode = bookToEdit != null;
    }

    public void showAsDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle(isEditMode ? I18n.get("form.edit.title") : I18n.get("form.add.title"));
        dialogStage.setResizable(false);

        VBox root = buildForm();

        Scene scene = new Scene(root, 560, 680);

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
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell", "form-container");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = IconUtil.createIcon(isEditMode ? IconUtil.IconType.EDIT : IconUtil.IconType.PLUS, 20);
        icon.setStyle("-fx-fill: -accent-primary;");

        VBox titleBox = new VBox(2);
        Label headerTitle = new Label(isEditMode ? I18n.get("form.edit.title") : I18n.get("form.add.title"));
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label headerSub = new Label(isEditMode ? I18n.get("form.edit.sub") : I18n.get("form.add.sub"));
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(headerTitle, headerSub);

        header.getChildren().addAll(icon, titleBox);
        root.getChildren().add(header);

        // Form Fields in ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox form = new VBox(14);
        form.setPadding(new Insets(4, 12, 12, 4));

        // 1. Title
        titleField = new TextField();
        titleField.setPromptText(I18n.get("form.title.prompt"));
        titleField.getStyleClass().add("text-input");
        titleError = createErrorLabel();
        form.getChildren().addAll(createFieldGroup(I18n.get("form.title"), titleField, titleError));

        // 2. Author
        authorField = new TextField();
        authorField.setPromptText(I18n.get("form.author.prompt"));
        authorField.getStyleClass().add("text-input");
        authorError = createErrorLabel();
        form.getChildren().addAll(createFieldGroup(I18n.get("form.author"), authorField, authorError));

        // 3. Pages & Parts Row
        HBox numbersRow = new HBox(16);
        numbersRow.setFillHeight(true);

        totalPagesField = new TextField("300");
        totalPagesField.getStyleClass().add("text-input");
        pagesError = createErrorLabel();
        VBox pagesGroup = createFieldGroup(I18n.get("form.total_pages"), totalPagesField, pagesError);
        HBox.setHgrow(pagesGroup, Priority.ALWAYS);

        totalPartsField = new TextField("1");
        totalPartsField.getStyleClass().add("text-input");
        partsError = createErrorLabel();
        VBox partsGroup = createFieldGroup(I18n.get("form.volumes"), totalPartsField, partsError);
        HBox.setHgrow(partsGroup, Priority.ALWAYS);

        numbersRow.getChildren().addAll(pagesGroup, partsGroup);
        form.getChildren().add(numbersRow);

        // 4. Progress & Status Row
        HBox progressRow = new HBox(16);
        progressRow.setFillHeight(true);

        currentPageField = new TextField("0");
        currentPageField.getStyleClass().add("text-input");
        currentError = createErrorLabel();
        VBox currentGroup = createFieldGroup(I18n.get("form.current_page"), currentPageField, currentError);
        HBox.setHgrow(currentGroup, Priority.ALWAYS);

        statusComboBox = new ComboBox<>();
        statusComboBox.getStyleClass().add("combo-box");
        statusComboBox.getItems().addAll(ReadingStatus.values());
        statusComboBox.setValue(ReadingStatus.NOT_STARTED);
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        VBox statusGroup = createFieldGroup(I18n.get("form.status"), statusComboBox, null);
        HBox.setHgrow(statusGroup, Priority.ALWAYS);

        progressRow.getChildren().addAll(currentGroup, statusGroup);
        form.getChildren().add(progressRow);

        // 5. Cover Image Picker
        HBox coverRow = new HBox(8);
        coverImageField = new TextField();
        coverImageField.setPromptText(I18n.get("form.cover.prompt"));
        coverImageField.getStyleClass().add("text-input");
        HBox.setHgrow(coverImageField, Priority.ALWAYS);

        Button browseBtn = new Button(I18n.get("form.browse"));
        browseBtn.getStyleClass().addAll("btn", "btn-secondary");
        browseBtn.setOnAction(e -> handleBrowseImage());
        coverRow.getChildren().addAll(coverImageField, browseBtn);

        form.getChildren().add(createFieldGroup(I18n.get("form.cover"), coverRow, null));

        // 6. Description / Notes
        descriptionArea = new TextArea();
        descriptionArea.setPromptText(I18n.get("form.notes.prompt"));
        descriptionArea.getStyleClass().add("text-input");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);
        form.getChildren().add(createFieldGroup(I18n.get("form.notes"), descriptionArea, null));

        scrollPane.setContent(form);
        root.getChildren().add(scrollPane);

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button cancelBtn = new Button(I18n.get("form.cancel"));
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        cancelBtn.setOnAction(e -> dialogStage.close());

        saveButton = new Button(isEditMode ? I18n.get("form.save") : I18n.get("form.add"));
        saveButton.getStyleClass().addAll("btn", "btn-primary");
        saveButton.setOnAction(e -> handleSave());

        footer.getChildren().addAll(cancelBtn, saveButton);
        root.getChildren().add(footer);

        // Populate existing data if editing
        if (isEditMode) {
            populateExistingData();
        }

        setupLiveValidation();

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

    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("form.cover"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            coverImageField.setText(file.getAbsolutePath());
        }
    }

    private void populateExistingData() {
        titleField.setText(bookToEdit.getTitle());
        authorField.setText(bookToEdit.getAuthor());
        totalPagesField.setText(String.valueOf(bookToEdit.getTotalPages()));
        totalPartsField.setText(String.valueOf(bookToEdit.getTotalParts()));
        currentPageField.setText(String.valueOf(bookToEdit.getCurrentPage()));
        statusComboBox.setValue(bookToEdit.getStatus());
        if (bookToEdit.getCoverImage() != null) {
            coverImageField.setText(bookToEdit.getCoverImage());
        }
        if (bookToEdit.getDescription() != null) {
            descriptionArea.setText(bookToEdit.getDescription());
        }
    }

    private void setupLiveValidation() {
        titleField.textProperty().addListener((obs, o, n) -> validateTitle());
        authorField.textProperty().addListener((obs, o, n) -> validateAuthor());
        totalPagesField.textProperty().addListener((obs, o, n) -> validatePages());
        currentPageField.textProperty().addListener((obs, o, n) -> validateCurrentPage());
        totalPartsField.textProperty().addListener((obs, o, n) -> validateParts());
    }

    private boolean validateTitle() {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            titleError.setText(I18n.get("form.error.title_empty"));
            titleError.setVisible(true);
            titleError.setManaged(true);
            return false;
        }
        titleError.setVisible(false);
        titleError.setManaged(false);
        return true;
    }

    private boolean validateAuthor() {
        if (authorField.getText() == null || authorField.getText().trim().isEmpty()) {
            authorError.setText(I18n.get("form.error.author_empty"));
            authorError.setVisible(true);
            authorError.setManaged(true);
            return false;
        }
        authorError.setVisible(false);
        authorError.setManaged(false);
        return true;
    }

    private boolean validatePages() {
        try {
            int pages = Integer.parseInt(totalPagesField.getText().trim());
            if (pages <= 0) {
                pagesError.setText(I18n.get("form.error.pages_invalid"));
                pagesError.setVisible(true);
                pagesError.setManaged(true);
                return false;
            }
            pagesError.setVisible(false);
            pagesError.setManaged(false);
            return true;
        } catch (NumberFormatException e) {
            pagesError.setText(I18n.get("form.error.integer"));
            pagesError.setVisible(true);
            pagesError.setManaged(true);
            return false;
        }
    }

    private boolean validateParts() {
        try {
            int parts = Integer.parseInt(totalPartsField.getText().trim());
            if (parts <= 0) {
                partsError.setText(I18n.get("form.error.parts_invalid"));
                partsError.setVisible(true);
                partsError.setManaged(true);
                return false;
            }
            partsError.setVisible(false);
            partsError.setManaged(false);
            return true;
        } catch (NumberFormatException e) {
            partsError.setText(I18n.get("form.error.integer"));
            partsError.setVisible(true);
            partsError.setManaged(true);
            return false;
        }
    }

    private boolean validateCurrentPage() {
        try {
            int current = Integer.parseInt(currentPageField.getText().trim());
            int total = 0;
            try {
                total = Integer.parseInt(totalPagesField.getText().trim());
            } catch (Exception ignored) {
            }

            if (current < 0) {
                currentError.setText(I18n.get("form.error.current_negative"));
                currentError.setVisible(true);
                currentError.setManaged(true);
                return false;
            }
            if (total > 0 && current > total) {
                currentError.setText(I18n.get("form.error.current_exceeds", total));
                currentError.setVisible(true);
                currentError.setManaged(true);
                return false;
            }
            currentError.setVisible(false);
            currentError.setManaged(false);
            return true;
        } catch (NumberFormatException e) {
            currentError.setText(I18n.get("form.error.integer"));
            currentError.setVisible(true);
            currentError.setManaged(true);
            return false;
        }
    }

    private void handleSave() {
        boolean valid = validateTitle() & validateAuthor() & validatePages() & validateParts() & validateCurrentPage();
        if (!valid) {
            return;
        }

        Book target = isEditMode ? bookToEdit : new Book();
        target.setTitle(titleField.getText().trim());
        target.setAuthor(authorField.getText().trim());
        target.setTotalPages(Integer.parseInt(totalPagesField.getText().trim()));
        target.setTotalParts(Integer.parseInt(totalPartsField.getText().trim()));
        target.setCurrentPage(Integer.parseInt(currentPageField.getText().trim()));
        target.setStatus(statusComboBox.getValue());

        String cover = coverImageField.getText();
        target.setCoverImage(cover != null && !cover.trim().isEmpty() ? cover.trim() : null);

        String desc = descriptionArea.getText();
        target.setDescription(desc != null && !desc.trim().isEmpty() ? desc.trim() : null);

        try {
            if (isEditMode) {
                bookService.updateBook(target);
                mainController.showToast(I18n.get("toast.book_updated"), ToastNotification.ToastType.SUCCESS);
            } else {
                bookService.addBook(target);
                mainController.showToast(I18n.get("toast.book_added"), ToastNotification.ToastType.SUCCESS);
            }
            mainController.refreshActiveViews();
            dialogStage.close();
        } catch (Exception e) {
            mainController.showToast("Error: " + e.getMessage(), ToastNotification.ToastType.ERROR);
        }
    }
}
