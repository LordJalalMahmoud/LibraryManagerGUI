package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.model.BackupRecord;
import com.librarymanager.model.BackupType;
import com.librarymanager.model.DatabaseIntegrityReport;
import com.librarymanager.service.BackupService;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.DialogUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Data Management screen providing JSON export/import, CSV export,
 * automatic backup scheduling, backup history, restore points, and database diagnostics.
 */
public class DataManagementController {
    private static final Logger LOGGER = Logger.getLogger(DataManagementController.class.getName());
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MainController mainController;
    private final BackupService backupService;
    private final SettingsService settingsService;

    private final ScrollPane rootScrollPane;
    private final VBox contentBox;

    // Metrics Row
    private Label dbSizeValueLabel;
    private Label backupsCountValueLabel;
    private Label integrityHealthValueLabel;
    private Label lastAutoBackupValueLabel;

    // Auto-backup controls
    private CheckBox autoBackupToggle;
    private ComboBox<String> frequencyCombo;
    private ComboBox<Integer> retentionCombo;

    // Table
    private TableView<BackupRecord> backupTable;
    private final ObservableList<BackupRecord> tableData = FXCollections.observableArrayList();

    // Diagnostics details labels
    private Label diagIntegrityLabel;
    private Label diagFkLabel;
    private Label diagFreelistLabel;
    private Label diagPageCountLabel;
    private Label diagPageSizeLabel;

    public DataManagementController(MainController mainController,
                                  BackupService backupService,
                                  SettingsService settingsService) {
        this.mainController = mainController;
        this.backupService = backupService;
        this.settingsService = settingsService;

        contentBox = new VBox(24);
        contentBox.setPadding(new Insets(0, 10, 30, 0));
        contentBox.setFillWidth(true);

        rootScrollPane = new ScrollPane(contentBox);
        rootScrollPane.setFitToWidth(true);
        rootScrollPane.getStyleClass().add("scroll-pane");
        rootScrollPane.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        buildView();
        refresh();
    }

    public Node getView() {
        return rootScrollPane;
    }

    private void buildView() {
        // 1. KPI Metrics Cards Row
        contentBox.getChildren().add(buildMetricsRow());

        // 2. Export & Import Card
        contentBox.getChildren().add(buildExportImportSection());

        // 3. Automatic Scheduled Backups Card
        contentBox.getChildren().add(buildAutoBackupSection());

        // 4. Backup History & Restore Points Table Card
        contentBox.getChildren().add(buildBackupHistorySection());

        // 5. Database Diagnostics & Integrity Check Card
        contentBox.getChildren().add(buildDiagnosticsSection());
    }

    // ==========================================
    // 1. Metrics Header Row
    // ==========================================

    private Node buildMetricsRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        // Card 1: Database Size
        VBox dbSizeCard = createMetricCard(
                IconUtil.IconType.DATABASE,
                I18n.get("data.metrics.db_size"),
                "0 KB",
                backupService.getDatabaseLocation()
        );
        dbSizeValueLabel = (Label) dbSizeCard.lookup(".data-metric-value");
        grid.add(dbSizeCard, 0, 0);

        // Card 2: Backups & Snapshots Count
        VBox backupsCard = createMetricCard(
                IconUtil.IconType.BACKUP,
                I18n.get("data.metrics.backups_count"),
                "0",
                backupService.getBackupDirectory().getFileName().toString()
        );
        backupsCountValueLabel = (Label) backupsCard.lookup(".data-metric-value");
        grid.add(backupsCard, 1, 0);

        // Card 3: Integrity Health
        VBox integrityCard = createMetricCard(
                IconUtil.IconType.SHIELD_CHECK,
                I18n.get("data.metrics.integrity_status"),
                I18n.get("data.metrics.healthy"),
                "PRAGMA check"
        );
        integrityHealthValueLabel = (Label) integrityCard.lookup(".data-metric-value");
        grid.add(integrityCard, 2, 0);

        // Card 4: Last Auto Backup
        VBox lastAutoCard = createMetricCard(
                IconUtil.IconType.CLOCK,
                I18n.get("data.metrics.last_auto_backup"),
                I18n.get("data.metrics.never"),
                settingsService.getAutoBackupFrequency()
        );
        lastAutoBackupValueLabel = (Label) lastAutoCard.lookup(".data-metric-value");
        grid.add(lastAutoCard, 3, 0);

        return grid;
    }

    private VBox createMetricCard(IconUtil.IconType iconType, String title, String initialValue, String subtitle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("data-metric-card");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("data-metric-icon");
        SVGPath icon = IconUtil.createIcon(iconType, 18);
        icon.setStyle("-fx-fill: -accent-primary;");
        iconBox.getChildren().add(icon);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("data-metric-label");
        titleBox.getChildren().add(lblTitle);

        top.getChildren().addAll(iconBox, titleBox);

        Label lblValue = new Label(initialValue);
        lblValue.getStyleClass().add("data-metric-value");

        Label lblSub = new Label(subtitle);
        lblSub.getStyleClass().add("data-metric-sub");
        lblSub.setWrapText(true);

        card.getChildren().addAll(top, lblValue, lblSub);
        return card;
    }

    // ==========================================
    // 2. Export & Import Section
    // ==========================================

    private Node buildExportImportSection() {
        VBox card = createCard(I18n.get("data.export_import.title"), I18n.get("data.export_import.sub"));

        HBox actionsRow = new HBox(16);
        actionsRow.setFillHeight(true);

        // Option 1: Export JSON
        VBox jsonExportBox = createActionTile(
                IconUtil.IconType.EXPORT_FILE,
                I18n.get("data.export.json"),
                I18n.get("data.export.json_desc"),
                I18n.get("data.export.json"),
                "btn-primary",
                this::handleExportJson
        );
        HBox.setHgrow(jsonExportBox, Priority.ALWAYS);

        // Option 2: Import JSON
        VBox jsonImportBox = createActionTile(
                IconUtil.IconType.IMPORT_FILE,
                I18n.get("data.import.json"),
                I18n.get("data.import.json_desc"),
                I18n.get("data.import.json"),
                "btn-secondary",
                this::handleImportJson
        );
        HBox.setHgrow(jsonImportBox, Priority.ALWAYS);

        // Option 3: Export CSV
        VBox csvExportBox = createActionTile(
                IconUtil.IconType.PAGES,
                I18n.get("data.export.csv"),
                I18n.get("data.export.csv_desc"),
                I18n.get("data.export.csv"),
                "btn-secondary",
                this::handleExportCsv
        );
        HBox.setHgrow(csvExportBox, Priority.ALWAYS);

        actionsRow.getChildren().addAll(jsonExportBox, jsonImportBox, csvExportBox);
        card.getChildren().add(actionsRow);
        return card;
    }

    private VBox createActionTile(IconUtil.IconType iconType, String title, String description,
                                  String buttonText, String btnStyleClass, Runnable onAction) {
        VBox tile = new VBox(12);
        tile.setStyle("-fx-background-color: -bg-surface; -fx-border-color: -border-subtle; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 16px;");
        tile.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: -accent-subtle; -fx-background-radius: 8px; -fx-padding: 6px;");
        SVGPath icon = IconUtil.createIcon(iconType, 16);
        icon.setStyle("-fx-fill: -accent-primary;");
        iconBox.getChildren().add(icon);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-main;");
        header.getChildren().addAll(iconBox, lblTitle);

        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted; -fx-min-height: 48px;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button actionBtn = new Button(buttonText);
        actionBtn.getStyleClass().addAll("btn", btnStyleClass);
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setOnAction(e -> onAction.run());

        tile.getChildren().addAll(header, lblDesc, spacer, actionBtn);
        return tile;
    }

    // ==========================================
    // 3. Auto-Backup Section
    // ==========================================

    private Node buildAutoBackupSection() {
        VBox card = createCard(I18n.get("data.autobackup.title"), I18n.get("data.autobackup.sub"));

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(20);
        settingsGrid.setVgap(14);
        settingsGrid.setAlignment(Pos.CENTER_LEFT);

        // Toggle: Enable
        autoBackupToggle = new CheckBox(I18n.get("data.autobackup.enable"));
        autoBackupToggle.setSelected(settingsService.isAutoBackupEnabled());
        autoBackupToggle.setStyle("-fx-font-weight: 600; -fx-text-fill: -text-main; -fx-font-size: 13px;");
        autoBackupToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            settingsService.setAutoBackupEnabled(newVal);
            mainController.showToast(newVal ? I18n.get("data.autobackup.enable") : I18n.get("toast.delete_confirm_disabled"), ToastNotification.ToastType.INFO);
            updateAutoBackupControlsState();
        });
        settingsGrid.add(autoBackupToggle, 0, 0, 2, 1);

        // Frequency combo
        Label lblFreq = new Label(I18n.get("data.autobackup.frequency"));
        lblFreq.getStyleClass().add("form-label");

        frequencyCombo = new ComboBox<>();
        frequencyCombo.getItems().addAll("DAILY", "WEEKLY", "ON_STARTUP");
        frequencyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getFrequencyLabel(item));
                }
            }
        });
        frequencyCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getFrequencyLabel(item));
                }
            }
        });
        frequencyCombo.setValue(settingsService.getAutoBackupFrequency());
        frequencyCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settingsService.setAutoBackupFrequency(newVal);
                refresh();
            }
        });
        settingsGrid.add(lblFreq, 0, 1);
        settingsGrid.add(frequencyCombo, 1, 1);

        // Retention limit
        Label lblRetention = new Label(I18n.get("data.autobackup.retention"));
        lblRetention.getStyleClass().add("form-label");

        retentionCombo = new ComboBox<>();
        retentionCombo.getItems().addAll(3, 5, 10, 20, 50);
        retentionCombo.setValue(settingsService.getAutoBackupRetention());
        retentionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.get("data.autobackup.retention_units", item));
            }
        });
        retentionCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.get("data.autobackup.retention_units", item));
            }
        });
        retentionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settingsService.setAutoBackupRetention(newVal);
            }
        });
        settingsGrid.add(lblRetention, 0, 2);
        settingsGrid.add(retentionCombo, 1, 2);

        // Action Buttons Row
        HBox btnRow = new HBox(12);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        Button backupNowBtn = new Button(I18n.get("data.autobackup.backup_now"));
        backupNowBtn.getStyleClass().addAll("btn", "btn-secondary");
        backupNowBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.BACKUP, 14));
        backupNowBtn.setGraphicTextGap(8);
        backupNowBtn.setOnAction(e -> handleBackupNow());

        Button restorePointBtn = new Button(I18n.get("data.autobackup.create_restore_point"));
        restorePointBtn.getStyleClass().addAll("btn", "btn-secondary");
        restorePointBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.SHIELD_CHECK, 14));
        restorePointBtn.setGraphicTextGap(8);
        restorePointBtn.setOnAction(e -> handleCreateRestorePoint());

        btnRow.getChildren().addAll(backupNowBtn, restorePointBtn);

        card.getChildren().addAll(settingsGrid, btnRow);
        updateAutoBackupControlsState();
        return card;
    }

    private String getFrequencyLabel(String key) {
        if ("WEEKLY".equalsIgnoreCase(key)) return I18n.get("data.autobackup.freq.weekly");
        if ("ON_STARTUP".equalsIgnoreCase(key)) return I18n.get("data.autobackup.freq.on_startup");
        return I18n.get("data.autobackup.freq.daily");
    }

    private void updateAutoBackupControlsState() {
        boolean enabled = autoBackupToggle.isSelected();
        frequencyCombo.setDisable(!enabled);
        retentionCombo.setDisable(!enabled);
    }

    // ==========================================
    // 4. Backup History & Restore Points Table
    // ==========================================

    private Node buildBackupHistorySection() {
        VBox card = createCard(I18n.get("data.history.title"), I18n.get("data.history.sub"));

        backupTable = new TableView<>(tableData);
        backupTable.getStyleClass().add("data-table");
        backupTable.setPlaceholder(new Label(I18n.get("data.history.empty")));
        backupTable.setPrefHeight(260);
        backupTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Column: Name
        TableColumn<BackupRecord, String> colName = new TableColumn<>(I18n.get("data.history.col.filename"));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFilename()));
        colName.setPrefWidth(220);

        // Column: Type Badge
        TableColumn<BackupRecord, BackupType> colType = new TableColumn<>(I18n.get("data.history.col.type"));
        colType.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getType()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BackupType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(getTypeLabel(item));
                    badge.getStyleClass().addAll("badge", item.getStyleClass());
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colType.setPrefWidth(120);

        // Column: Date
        TableColumn<BackupRecord, String> colDate = new TableColumn<>(I18n.get("data.history.col.date"));
        colDate.setCellValueFactory(c -> {
            if (c.getValue().getTimestamp() != null) {
                return new SimpleStringProperty(c.getValue().getTimestamp().format(DISPLAY_TIME_FORMAT));
            }
            return new SimpleStringProperty("—");
        });
        colDate.setPrefWidth(150);

        // Column: Size
        TableColumn<BackupRecord, String> colSize = new TableColumn<>(I18n.get("data.history.col.size"));
        colSize.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedSize()));
        colSize.setPrefWidth(90);

        // Column: Description
        TableColumn<BackupRecord, String> colDesc = new TableColumn<>(I18n.get("data.history.col.description"));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colDesc.setPrefWidth(200);

        // Column: Health Check
        TableColumn<BackupRecord, Boolean> colHealth = new TableColumn<>(I18n.get("data.history.col.health"));
        colHealth.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isValid()));
        colHealth.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean valid, boolean empty) {
                super.updateItem(valid, empty);
                if (empty || valid == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(valid ? I18n.get("data.health.valid") : I18n.get("data.health.corrupted"));
                    badge.getStyleClass().addAll("badge", valid ? "badge-valid" : "badge-invalid");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colHealth.setPrefWidth(130);

        // Column: Actions
        TableColumn<BackupRecord, Void> colActions = new TableColumn<>(I18n.get("data.history.col.actions"));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button restoreBtn = new Button(I18n.get("data.history.action.restore"));
            private final Button exportBtn = new Button(I18n.get("data.history.action.export"));
            private final Button deleteBtn = new Button(I18n.get("data.history.action.delete"));
            private final HBox box = new HBox(6, restoreBtn, exportBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                restoreBtn.getStyleClass().addAll("btn-table-action", "btn-secondary");
                exportBtn.getStyleClass().addAll("btn-table-action", "btn-secondary");
                deleteBtn.getStyleClass().addAll("btn-table-action", "btn-danger");

                restoreBtn.setOnAction(e -> {
                    BackupRecord rec = getTableView().getItems().get(getIndex());
                    handleRestoreRecord(rec);
                });

                exportBtn.setOnAction(e -> {
                    BackupRecord rec = getTableView().getItems().get(getIndex());
                    handleExportRecord(rec);
                });

                deleteBtn.setOnAction(e -> {
                    BackupRecord rec = getTableView().getItems().get(getIndex());
                    handleDeleteRecord(rec);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        colActions.setPrefWidth(210);

        backupTable.getColumns().addAll(colName, colType, colDate, colSize, colDesc, colHealth, colActions);
        card.getChildren().add(backupTable);
        return card;
    }

    private String getTypeLabel(BackupType type) {
        return switch (type) {
            case AUTO -> I18n.get("data.type.auto");
            case RESTORE_POINT -> I18n.get("data.type.restore_point");
            case PRE_IMPORT -> I18n.get("data.type.pre_import");
            default -> I18n.get("data.type.manual");
        };
    }

    // ==========================================
    // 5. Diagnostics Section
    // ==========================================

    private Node buildDiagnosticsSection() {
        VBox card = createCard(I18n.get("data.diagnostics.title"), I18n.get("data.diagnostics.sub"));

        HBox topButtons = new HBox(12);
        Button checkBtn = new Button(I18n.get("data.diagnostics.check_btn"));
        checkBtn.getStyleClass().addAll("btn", "btn-secondary");
        checkBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.SHIELD_CHECK, 14));
        checkBtn.setGraphicTextGap(8);
        checkBtn.setOnAction(e -> runDiagnosticsCheck());

        Button vacuumBtn = new Button(I18n.get("data.diagnostics.vacuum_btn"));
        vacuumBtn.getStyleClass().addAll("btn", "btn-secondary");
        vacuumBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.SPARKLES, 14));
        vacuumBtn.setGraphicTextGap(8);
        vacuumBtn.setOnAction(e -> handleOptimizeAndVacuum());

        topButtons.getChildren().addAll(checkBtn, vacuumBtn);

        // Details grid
        GridPane diagGrid = new GridPane();
        diagGrid.setHgap(24);
        diagGrid.setVgap(12);
        diagGrid.setPadding(new Insets(10, 0, 0, 0));

        diagGrid.add(createFormLabel(I18n.get("data.diagnostics.integrity")), 0, 0);
        diagIntegrityLabel = createFormValue("OK (PRAGMA integrity_check)");
        diagGrid.add(diagIntegrityLabel, 1, 0);

        diagGrid.add(createFormLabel(I18n.get("data.diagnostics.foreign_keys")), 0, 1);
        diagFkLabel = createFormValue("Passed (0 violations)");
        diagGrid.add(diagFkLabel, 1, 1);

        diagGrid.add(createFormLabel(I18n.get("data.diagnostics.freelist")), 2, 0);
        diagFreelistLabel = createFormValue("0 pages");
        diagGrid.add(diagFreelistLabel, 3, 0);

        diagGrid.add(createFormLabel(I18n.get("data.diagnostics.page_count")), 2, 1);
        diagPageCountLabel = createFormValue("—");
        diagGrid.add(diagPageCountLabel, 3, 1);

        diagGrid.add(createFormLabel(I18n.get("data.diagnostics.page_size")), 2, 2);
        diagPageSizeLabel = createFormValue("4096 bytes");
        diagGrid.add(diagPageSizeLabel, 3, 2);

        card.getChildren().addAll(topButtons, diagGrid);
        return card;
    }

    // ==========================================
    // Handlers & Business Logic
    // ==========================================

    private void handleExportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("data.export.json"));
        chooser.setInitialFileName("library_backup_" + java.time.LocalDate.now() + ".json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File target = chooser.showSaveDialog(mainController.getPrimaryStage());
        if (target != null) {
            try {
                backupService.exportJson(target);
                mainController.showToast(I18n.get("data.toast.export_json_success", target.getName()), ToastNotification.ToastType.SUCCESS);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "JSON export failed", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "JSON Export Error", ex.getMessage());
            }
        }
    }

    private void handleImportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("data.import.json"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File source = chooser.showOpenDialog(mainController.getPrimaryStage());
        if (source != null) {
            // Ask user: Merge or Replace?
            Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle(I18n.get("data.import.dialog.title"));
            dialog.setHeaderText(I18n.get("data.import.dialog.header"));
            dialog.setContentText(I18n.get("data.import.dialog.content"));

            ButtonType mergeBtn = new ButtonType(I18n.get("data.import.mode.merge"));
            ButtonType replaceBtn = new ButtonType(I18n.get("data.import.mode.replace"));
            ButtonType cancelBtn = new ButtonType(I18n.get("form.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

            dialog.getButtonTypes().setAll(mergeBtn, replaceBtn, cancelBtn);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() != cancelBtn) {
                boolean isMerge = result.get() == mergeBtn;

                if (!isMerge) {
                    // Double confirmation for destructive replace
                    Alert confirmReplace = new Alert(Alert.AlertType.WARNING);
                    confirmReplace.setTitle(I18n.get("data.import.replace_confirm_title"));
                    confirmReplace.setHeaderText(null);
                    confirmReplace.setContentText(I18n.get("data.import.replace_confirm_msg"));
                    Optional<ButtonType> confirmRes = confirmReplace.showAndWait();
                    if (confirmRes.isEmpty() || confirmRes.get() != ButtonType.OK) {
                        return;
                    }
                }

                try {
                    BackupService.ImportSummary summary = backupService.importJson(source, isMerge);
                    mainController.showToast(I18n.get("data.toast.import_json_success",
                            summary.getBooksAdded() + summary.getBooksUpdated(),
                            summary.getChaptersAdded(),
                            summary.getSessionsAdded()), ToastNotification.ToastType.SUCCESS);
                    refresh();
                    mainController.refreshActiveViews();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "JSON import failed", ex);
                    DialogUtil.showError(mainController.getPrimaryStage(), "JSON Import Error", ex.getMessage());
                }
            }
        }
    }

    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("data.export.csv"));
        chooser.setInitialFileName("library_books_" + java.time.LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File target = chooser.showSaveDialog(mainController.getPrimaryStage());
        if (target != null) {
            try {
                int count = backupService.exportCsv(target);
                mainController.showToast(I18n.get("data.toast.export_csv_success", count), ToastNotification.ToastType.SUCCESS);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "CSV export failed", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "CSV Export Error", ex.getMessage());
            }
        }
    }

    private void handleBackupNow() {
        try {
            BackupRecord rec = backupService.createBackup(BackupType.MANUAL, "Manual backup from Data Management");
            mainController.showToast(I18n.get("data.toast.backup_created", rec.getFilename()), ToastNotification.ToastType.SUCCESS);
            refresh();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to create manual backup", ex);
            DialogUtil.showError(mainController.getPrimaryStage(), "Backup Failed", ex.getMessage());
        }
    }

    private void handleCreateRestorePoint() {
        TextInputDialog dialog = new TextInputDialog(I18n.get("data.autobackup.default_restore_desc"));
        dialog.setTitle(I18n.get("data.autobackup.create_restore_point"));
        dialog.setHeaderText(I18n.get("data.autobackup.create_restore_point"));
        dialog.setContentText(I18n.get("data.autobackup.prompt_restore_desc"));

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(desc -> {
            try {
                backupService.createRestorePoint(desc);
                mainController.showToast(I18n.get("data.toast.restore_point_created"), ToastNotification.ToastType.SUCCESS);
                refresh();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to create restore point", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "Restore Point Failed", ex.getMessage());
            }
        });
    }

    private void handleRestoreRecord(BackupRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("data.history.restore_confirm_title"));
        confirm.setHeaderText(null);
        confirm.setContentText(I18n.get("data.history.restore_confirm_msg", record.getFilename()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                backupService.restoreFromRecord(record);
                mainController.showToast(I18n.get("data.toast.backup_restored", record.getFilename()), ToastNotification.ToastType.SUCCESS);
                refresh();
                mainController.refreshActiveViews();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to restore database from record", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "Restore Failed", ex.getMessage());
            }
        }
    }

    private void handleExportRecord(BackupRecord record) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("data.history.action.export"));
        chooser.setInitialFileName(record.getFilename());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database Files (*.db)", "*.db"));

        File target = chooser.showSaveDialog(mainController.getPrimaryStage());
        if (target != null) {
            try {
                Files.copy(record.getPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                mainController.showToast(I18n.get("toast.backup_success"), ToastNotification.ToastType.SUCCESS);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to copy backup file", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "Export Error", ex.getMessage());
            }
        }
    }

    private void handleDeleteRecord(BackupRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("data.history.delete_confirm_title"));
        confirm.setHeaderText(null);
        confirm.setContentText(I18n.get("data.history.delete_confirm_msg", record.getFilename()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean deleted = backupService.deleteBackup(record);
            if (deleted) {
                mainController.showToast(I18n.get("data.toast.backup_deleted", record.getFilename()), ToastNotification.ToastType.INFO);
                refresh();
            } else {
                DialogUtil.showError(mainController.getPrimaryStage(), "Delete Error", "Could not delete backup file.");
            }
        }
    }

    private void runDiagnosticsCheck() {
        DatabaseIntegrityReport report = backupService.checkDatabaseIntegrity();
        diagIntegrityLabel.setText(report.isIntegrityOk() ? "OK (Passed)" : "FAILED (" + report.getRawIntegrityResult() + ")");
        diagIntegrityLabel.setStyle(report.isIntegrityOk() ? "-fx-text-fill: #10b981; -fx-font-weight: 700;" : "-fx-text-fill: #ef4444; -fx-font-weight: 700;");

        diagFkLabel.setText(report.isForeignKeyOk() ? "Passed (0 violations)" : report.getForeignKeyViolationsCount() + " violations detected!");
        diagFkLabel.setStyle(report.isForeignKeyOk() ? "-fx-text-fill: #10b981; -fx-font-weight: 700;" : "-fx-text-fill: #ef4444; -fx-font-weight: 700;");

        diagFreelistLabel.setText(report.getFreelistCount() + " pages");
        diagPageCountLabel.setText(String.valueOf(report.getPageCount()));
        diagPageSizeLabel.setText(report.getPageSize() + " bytes");

        if (report.isHealthy()) {
            mainController.showToast(I18n.get("data.toast.integrity_healthy"), ToastNotification.ToastType.SUCCESS);
        } else {
            mainController.showToast(I18n.get("data.toast.integrity_issues"), ToastNotification.ToastType.WARNING);
        }
    }

    private void handleOptimizeAndVacuum() {
        try {
            backupService.optimizeDatabase();
            mainController.showToast(I18n.get("data.toast.vacuum_success"), ToastNotification.ToastType.SUCCESS);
            refresh();
            runDiagnosticsCheck();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Optimize and vacuum failed", ex);
            DialogUtil.showError(mainController.getPrimaryStage(), "Optimization Error", ex.getMessage());
        }
    }

    // ==========================================
    // Public Refresh
    // ==========================================

    public void refresh() {
        // Update Metrics
        if (dbSizeValueLabel != null) {
            dbSizeValueLabel.setText(backupService.getDatabaseSizeFormatted());
        }

        List<BackupRecord> history = backupService.getBackupHistory();
        if (backupsCountValueLabel != null) {
            backupsCountValueLabel.setText(String.valueOf(history.size()));
        }

        tableData.setAll(history);

        DatabaseIntegrityReport report = backupService.checkDatabaseIntegrity();
        if (integrityHealthValueLabel != null) {
            integrityHealthValueLabel.setText(report.isHealthy() ? I18n.get("data.metrics.healthy") : I18n.get("data.metrics.corrupted"));
            integrityHealthValueLabel.setStyle(report.isHealthy() ? "-fx-text-fill: #10b981; -fx-font-weight: 800;" : "-fx-text-fill: #ef4444; -fx-font-weight: 800;");
        }

        if (lastAutoBackupValueLabel != null) {
            String lastRun = settingsService.getAutoBackupLastRun();
            if (lastRun == null || lastRun.isBlank()) {
                lastAutoBackupValueLabel.setText(I18n.get("data.metrics.never"));
            } else {
                try {
                    java.time.LocalDateTime dt = java.time.LocalDateTime.parse(lastRun);
                    lastAutoBackupValueLabel.setText(dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
                } catch (Exception e) {
                    lastAutoBackupValueLabel.setText(lastRun);
                }
            }
        }
    }

    private VBox createCard(String title, String subtitle) {
        VBox card = new VBox(14);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20));

        VBox header = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        header.getChildren().addAll(titleLabel, subLabel);
        card.getChildren().add(header);
        return card;
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("form-label");
        return lbl;
    }

    private Label createFormValue(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 600;");
        return lbl;
    }
}
