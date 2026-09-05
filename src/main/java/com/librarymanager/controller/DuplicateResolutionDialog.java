package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.Book;
import com.librarymanager.model.DuplicateGroup;
import com.librarymanager.model.ReadingStatus;
import com.librarymanager.service.BookService;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog controller for reviewing and resolving detected duplicate book entries.
 */
public class DuplicateResolutionDialog {

    private final MainController mainController;
    private final BookService bookService;
    private Stage dialogStage;
    private VBox groupsContainer;
    private Label countBadge;
    private List<DuplicateGroup> duplicateGroups;

    public DuplicateResolutionDialog(MainController mainController, BookService bookService) {
        this.mainController = mainController;
        this.bookService = bookService;
    }

    public void showAsDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle(I18n.get("duplicate.dialog.title"));
        dialogStage.setResizable(true);

        VBox root = buildUI();

        Scene scene = new Scene(root, 680, 620);
        String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
        String themeStyle = mainController.getSettingsService().isDarkMode()
                ? getClass().getResource("/css/theme-dark.css").toExternalForm()
                : getClass().getResource("/css/theme-light.css").toExternalForm();
        scene.getStylesheets().addAll(themeStyle, baseStyle);

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private VBox buildUI() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("app-shell", "form-container");
        root.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = IconUtil.createIcon(IconUtil.IconType.LIBRARY, 22);
        icon.setStyle("-fx-fill: -accent-warning;");

        VBox titleBox = new VBox(2);
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label(I18n.get("duplicate.dialog.title"));
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        countBadge = new Label();
        countBadge.getStyleClass().addAll("badge-chip", "badge-warning");

        titleRow.getChildren().addAll(headerTitle, countBadge);

        Label headerSub = new Label(I18n.get("duplicate.dialog.subtitle"));
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");
        titleBox.getChildren().addAll(titleRow, headerSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button autoResolveBtn = new Button(I18n.get("duplicate.action.auto_resolve_all"));
        autoResolveBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        autoResolveBtn.setOnAction(e -> handleAutoResolveAll());

        header.getChildren().addAll(icon, titleBox, spacer, autoResolveBtn);

        // Groups scroll container
        groupsContainer = new VBox(16);
        groupsContainer.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(groupsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button closeBtn = new Button(I18n.get("duplicate.action.close"));
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");
        closeBtn.setOnAction(e -> {
            mainController.refreshActiveViews();
            dialogStage.close();
        });

        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, scrollPane, footer);

        refreshDuplicateGroups();
        return root;
    }

    private void refreshDuplicateGroups() {
        duplicateGroups = bookService.findDuplicates();
        groupsContainer.getChildren().clear();

        countBadge.setText(String.valueOf(duplicateGroups.size()));

        if (duplicateGroups.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));

            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-font-size: 40px; -fx-text-fill: -accent-success;");

            Label title = new Label(I18n.get("duplicate.empty.title"));
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: -text-main;");

            Label desc = new Label(I18n.get("duplicate.empty.desc"));
            desc.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-muted;");

            emptyBox.getChildren().addAll(checkIcon, title, desc);
            groupsContainer.getChildren().add(emptyBox);
            return;
        }

        for (int i = 0; i < duplicateGroups.size(); i++) {
            DuplicateGroup group = duplicateGroups.get(i);
            groupsContainer.getChildren().add(createGroupCard(group, i + 1));
        }
    }

    private Node createGroupCard(DuplicateGroup group, int groupNumber) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));
        card.setStyle("-fx-border-color: -border-color; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Header with reason
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label groupNumLabel = new Label(I18n.get("duplicate.group_header", groupNumber));
        groupNumLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: -text-main;");

        Label reasonBadge = new Label(group.getMatchReason());
        reasonBadge.getStyleClass().addAll("badge-chip", "badge-info");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox mergeCheckBox = new CheckBox(I18n.get("duplicate.merge_progress_checkbox"));
        mergeCheckBox.setSelected(true);
        mergeCheckBox.setStyle("-fx-font-size: 12px;");

        header.getChildren().addAll(groupNumLabel, reasonBadge, spacer, mergeCheckBox);

        // Books in group
        ToggleGroup toggleGroup = new ToggleGroup();
        VBox bookOptions = new VBox(8);

        List<RadioButton> radioButtons = new ArrayList<>();
        List<Book> books = group.getBooks();

        for (int bIdx = 0; bIdx < books.size(); bIdx++) {
            Book book = books.get(bIdx);
            RadioButton rb = new RadioButton();
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(book);

            HBox bookRow = new HBox(10);
            bookRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(bookRow, Priority.ALWAYS);

            VBox info = new VBox(3);
            Label title = new Label(book.getTitle() + (book.getAuthor() != null ? " — " + book.getAuthor() : ""));
            title.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: -text-main;");

            String metaText = String.format("%s • %s • %d/%d %s (%d%%)",
                    book.getStatus().getDisplayName(),
                    book.getCategory() != null ? book.getCategory() : "Uncategorized",
                    book.getCurrentPage(), book.getTotalPages(),
                    I18n.get("book.details.pages"),
                    book.getProgressPercentage());

            Label meta = new Label(metaText);
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-muted;");

            info.getChildren().addAll(title, meta);
            bookRow.getChildren().addAll(rb, info);

            bookRow.setOnMouseClicked(e -> rb.setSelected(true));
            bookOptions.getChildren().add(bookRow);
            radioButtons.add(rb);
        }

        // Default select book with highest progress
        Book bestBook = books.get(0);
        int bestIdx = 0;
        for (int i = 1; i < books.size(); i++) {
            Book b = books.get(i);
            if (b.getStatus() == ReadingStatus.COMPLETED && bestBook.getStatus() != ReadingStatus.COMPLETED) {
                bestBook = b;
                bestIdx = i;
            } else if (b.getCurrentPage() > bestBook.getCurrentPage()) {
                bestBook = b;
                bestIdx = i;
            }
        }
        radioButtons.get(bestIdx).setSelected(true);

        // Action row
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(I18n.isRTL() ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Button resolveBtn = new Button(I18n.get("duplicate.action.resolve_group"));
        resolveBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        resolveBtn.setOnAction(e -> {
            Book keep = (Book) toggleGroup.getSelectedToggle().getUserData();
            boolean merge = mergeCheckBox.isSelected();
            for (Book b : books) {
                if (!b.getId().equals(keep.getId())) {
                    bookService.resolveDuplicate(keep.getId(), b.getId(), merge);
                }
            }
            mainController.showToast(I18n.get("duplicate.toast.resolved", keep.getTitle()), ToastNotification.ToastType.SUCCESS);
            refreshDuplicateGroups();
        });

        actionRow.getChildren().add(resolveBtn);

        card.getChildren().addAll(header, bookOptions, actionRow);
        return card;
    }

    private void handleAutoResolveAll() {
        if (duplicateGroups == null || duplicateGroups.isEmpty()) return;

        int resolvedCount = 0;
        for (DuplicateGroup group : duplicateGroups) {
            List<Book> books = group.getBooks();
            if (books.size() < 2) continue;

            // Pick book with highest progress
            Book keep = books.get(0);
            for (int i = 1; i < books.size(); i++) {
                Book b = books.get(i);
                if (b.getStatus() == ReadingStatus.COMPLETED && keep.getStatus() != ReadingStatus.COMPLETED) {
                    keep = b;
                } else if (b.getCurrentPage() > keep.getCurrentPage()) {
                    keep = b;
                }
            }

            for (Book b : books) {
                if (!b.getId().equals(keep.getId())) {
                    bookService.resolveDuplicate(keep.getId(), b.getId(), true);
                }
            }
            resolvedCount++;
        }

        mainController.showToast(I18n.get("duplicate.toast.all_resolved", resolvedCount), ToastNotification.ToastType.SUCCESS);
        mainController.refreshActiveViews();
        refreshDuplicateGroups();
    }
}
