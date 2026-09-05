package com.librarymanager.controller;

import com.librarymanager.component.ToastNotification;
import com.librarymanager.service.BackupService;
import com.librarymanager.service.BookService;
import com.librarymanager.service.SampleDataService;
import com.librarymanager.service.SettingsService;
import com.librarymanager.util.DialogUtil;
import com.librarymanager.util.I18n;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Settings screen managing language (English/Arabic), theme preferences,
 * confirmation rules, database info, backup & restore, and sample data.
 */
public class SettingsController {
    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());

    private final MainController mainController;
    private final SettingsService settingsService;
    private final BackupService backupService;
    private final SampleDataService sampleDataService;
    private final BookService bookService;

    private final ScrollPane rootScrollPane;
    private final VBox contentBox;

    // Setting inputs
    private RadioButton enRadio;
    private RadioButton arRadio;
    private RadioButton darkRadio;
    private RadioButton lightRadio;
    private CheckBox confirmDeleteCheck;
    private Label dbPathLabel;
    private Label dbSizeLabel;
    private Label totalRecordsLabel;

    public SettingsController(MainController mainController,
                              SettingsService settingsService,
                              BackupService backupService,
                              SampleDataService sampleDataService,
                              BookService bookService) {
        this.mainController = mainController;
        this.settingsService = settingsService;
        this.backupService = backupService;
        this.sampleDataService = sampleDataService;
        this.bookService = bookService;

        contentBox = new VBox(24);
        contentBox.setPadding(new Insets(0, 10, 30, 0));
        contentBox.setFillWidth(true);

        rootScrollPane = new ScrollPane(contentBox);
        rootScrollPane.setFitToWidth(true);
        rootScrollPane.getStyleClass().add("scroll-pane");
        rootScrollPane.setNodeOrientation(I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        buildSettings();
    }

    public Node getView() {
        return rootScrollPane;
    }

    private void buildSettings() {
        // 1. Language Section (New!)
        VBox langCard = createCard(I18n.get("settings.language.title"), I18n.get("settings.language.sub"));
        ToggleGroup langGroup = new ToggleGroup();

        arRadio = new RadioButton(I18n.get("settings.language.ar"));
        arRadio.setToggleGroup(langGroup);
        arRadio.setSelected(settingsService.isArabic());
        arRadio.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 600;");

        enRadio = new RadioButton(I18n.get("settings.language.en"));
        enRadio.setToggleGroup(langGroup);
        enRadio.setSelected(!settingsService.isArabic());
        enRadio.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 600;");

        langGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == arRadio && !settingsService.isArabic()) {
                settingsService.setLanguage(SettingsService.LANGUAGE_AR);
            } else if (newVal == enRadio && settingsService.isArabic()) {
                settingsService.setLanguage(SettingsService.LANGUAGE_EN);
            }
        });

        HBox langOptions = new HBox(24, arRadio, enRadio);
        langCard.getChildren().add(langOptions);
        contentBox.getChildren().add(langCard);

        // 2. Appearance Section
        VBox appearanceCard = createCard(I18n.get("settings.appearance.title"), I18n.get("settings.appearance.sub"));
        ToggleGroup themeGroup = new ToggleGroup();

        darkRadio = new RadioButton(I18n.get("settings.theme.dark"));
        darkRadio.setToggleGroup(themeGroup);
        darkRadio.setSelected(settingsService.isDarkMode());
        darkRadio.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 500;");

        lightRadio = new RadioButton(I18n.get("settings.theme.light"));
        lightRadio.setToggleGroup(themeGroup);
        lightRadio.setSelected(!settingsService.isDarkMode());
        lightRadio.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 500;");

        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == darkRadio) {
                settingsService.setTheme(SettingsService.THEME_DARK);
                mainController.showToast(I18n.get("toast.theme_switched", "dark"), ToastNotification.ToastType.INFO);
            } else if (newVal == lightRadio) {
                settingsService.setTheme(SettingsService.THEME_LIGHT);
                mainController.showToast(I18n.get("toast.theme_switched", "light"), ToastNotification.ToastType.INFO);
            }
        });

        VBox themeOptions = new VBox(10, darkRadio, lightRadio);
        appearanceCard.getChildren().add(themeOptions);
        contentBox.getChildren().add(appearanceCard);

        // 3. Preferences Section
        VBox prefsCard = createCard(I18n.get("settings.prefs.title"), I18n.get("settings.prefs.sub"));
        confirmDeleteCheck = new CheckBox(I18n.get("settings.prefs.confirm_delete"));
        confirmDeleteCheck.setSelected(settingsService.isConfirmDeleteEnabled());
        confirmDeleteCheck.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 500;");
        confirmDeleteCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            settingsService.setConfirmDelete(newVal);
            mainController.showToast(newVal ? I18n.get("toast.delete_confirm_enabled") : I18n.get("toast.delete_confirm_disabled"), ToastNotification.ToastType.INFO);
        });

        prefsCard.getChildren().add(confirmDeleteCheck);
        contentBox.getChildren().add(prefsCard);

        // 4. Database Information Section
        VBox dbCard = createCard(I18n.get("settings.db.title"), I18n.get("settings.db.sub"));
        GridPane dbGrid = new GridPane();
        dbGrid.setHgap(16);
        dbGrid.setVgap(10);

        dbGrid.add(createLabel(I18n.get("settings.db.file")), 0, 0);
        dbPathLabel = createValueLabel(backupService.getDatabaseLocation());
        dbGrid.add(dbPathLabel, 1, 0);

        dbGrid.add(createLabel(I18n.get("settings.db.size")), 0, 1);
        dbSizeLabel = createValueLabel(backupService.getDatabaseSizeFormatted());
        dbGrid.add(dbSizeLabel, 1, 1);

        dbGrid.add(createLabel(I18n.get("settings.db.total_books")), 0, 2);
        totalRecordsLabel = createValueLabel(String.valueOf(bookService.getAllBooks().size()));
        dbGrid.add(totalRecordsLabel, 1, 2);

        // Backup & Restore Buttons
        HBox backupButtons = new HBox(12);
        backupButtons.setPadding(new Insets(12, 0, 0, 0));

        Button exportBtn = new Button(I18n.get("settings.db.export"));
        exportBtn.getStyleClass().addAll("btn", "btn-secondary");
        exportBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.BACKUP, 14));
        exportBtn.setGraphicTextGap(8);
        exportBtn.setOnAction(e -> handleExportBackup());

        Button restoreBtn = new Button(I18n.get("settings.db.restore"));
        restoreBtn.getStyleClass().addAll("btn", "btn-secondary");
        restoreBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.RESTORE, 14));
        restoreBtn.setGraphicTextGap(8);
        restoreBtn.setOnAction(e -> handleRestoreBackup());

        backupButtons.getChildren().addAll(exportBtn, restoreBtn);
        dbCard.getChildren().addAll(dbGrid, backupButtons);
        contentBox.getChildren().add(dbCard);

        // 5. Sample Data Section
        VBox sampleCard = createCard(I18n.get("settings.sample.title"), I18n.get("settings.sample.sub"));
        Label sampleDesc = new Label(I18n.get("settings.sample.desc"));
        sampleDesc.setStyle("-fx-text-fill: -text-muted;");

        Button loadSampleBtn = new Button(I18n.get("settings.sample.load"));
        loadSampleBtn.getStyleClass().addAll("btn", "btn-secondary");
        loadSampleBtn.setOnAction(e -> {
            int count = sampleDataService.loadSampleData();
            mainController.showToast(I18n.get("toast.samples_loaded", count), ToastNotification.ToastType.SUCCESS);
            refreshInfo();
            mainController.refreshActiveViews();
        });

        sampleCard.getChildren().addAll(sampleDesc, loadSampleBtn);
        contentBox.getChildren().add(sampleCard);

        // 6. Danger Zone Section
        VBox dangerCard = createCard(I18n.get("settings.danger.title"), I18n.get("settings.danger.sub"));
        dangerCard.setStyle("-fx-border-color: rgba(239, 68, 68, 0.4);");

        Label dangerDesc = new Label(I18n.get("settings.danger.desc"));
        dangerDesc.setStyle("-fx-text-fill: -text-muted;");

        Button resetBtn = new Button(I18n.get("settings.danger.reset"));
        resetBtn.getStyleClass().addAll("btn", "btn-danger");
        resetBtn.setGraphic(IconUtil.createIcon(IconUtil.IconType.TRASH, 14));
        resetBtn.setGraphicTextGap(8);
        resetBtn.setOnAction(e -> handleResetLibrary());

        dangerCard.getChildren().addAll(dangerDesc, resetBtn);
        contentBox.getChildren().add(dangerCard);

        // 7. About Section
        VBox aboutCard = createCard(I18n.get("settings.about.title"), I18n.get("settings.about.sub"));
        Label appName = new Label(I18n.get("settings.about.version"));
        appName.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: -text-main;");
        Label appTech = new Label(I18n.get("settings.about.desc"));
        appTech.setStyle("-fx-text-fill: -text-muted;");
        aboutCard.getChildren().addAll(appName, appTech);
        contentBox.getChildren().add(aboutCard);
    }

    private VBox createCard(String title, String subtitle) {
        VBox card = new VBox(12);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20));

        VBox header = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -text-main;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-muted;");

        header.getChildren().addAll(titleLabel, subLabel);
        card.getChildren().add(header);
        return card;
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("form-label");
        return lbl;
    }

    private Label createValueLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 500;");
        return lbl;
    }

    private void handleExportBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("settings.db.export"));
        chooser.setInitialFileName("library-backup.db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database Files (*.db)", "*.db"));

        File target = chooser.showSaveDialog(mainController.getPrimaryStage());
        if (target != null) {
            try {
                backupService.exportBackup(target);
                mainController.showToast(I18n.get("toast.backup_success"), ToastNotification.ToastType.SUCCESS);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to export database backup", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "Backup Failed", "Could not export database backup: " + ex.getMessage());
            }
        }
    }

    private void handleRestoreBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("settings.db.restore"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database Files (*.db)", "*.db"));

        File source = chooser.showOpenDialog(mainController.getPrimaryStage());
        if (source != null) {
            try {
                backupService.restoreBackup(source);
                mainController.showToast(I18n.get("toast.restore_success"), ToastNotification.ToastType.SUCCESS);
                refreshInfo();
                mainController.refreshActiveViews();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to restore database backup", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "Restore Failed", "Could not restore database: " + ex.getMessage());
            }
        }
    }

    private void handleResetLibrary() {
        if (DialogUtil.confirmResetLibrary(mainController.getPrimaryStage())) {
            try {
                bookService.resetLibrary();
                mainController.showToast(I18n.get("toast.library_reset"), ToastNotification.ToastType.SUCCESS);
                refreshInfo();
                mainController.refreshActiveViews();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to reset library database", ex);
                DialogUtil.showError(mainController.getPrimaryStage(), "Reset Failed", "Could not reset library: " + ex.getMessage());
            }
        }
    }

    public void refreshInfo() {
        if (dbSizeLabel != null) {
            dbSizeLabel.setText(backupService.getDatabaseSizeFormatted());
        }
        if (totalRecordsLabel != null) {
            totalRecordsLabel.setText(String.valueOf(bookService.getAllBooks().size()));
        }
        if (darkRadio != null && lightRadio != null) {
            darkRadio.setSelected(settingsService.isDarkMode());
            lightRadio.setSelected(!settingsService.isDarkMode());
        }
        if (arRadio != null && enRadio != null) {
            arRadio.setSelected(settingsService.isArabic());
            enRadio.setSelected(!settingsService.isArabic());
        }
    }
}
