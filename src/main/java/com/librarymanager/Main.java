package com.librarymanager;

import com.librarymanager.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main JavaFX Application entry point.
 */
public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final int MIN_WIDTH = 980;
    private static final int MIN_HEIGHT = 680;
    private static final int DEFAULT_WIDTH = 1180;
    private static final int DEFAULT_HEIGHT = 780;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("LibraryManager — Personal Library");
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);

        // Try setting window icon if available
        try (InputStream is = getClass().getResourceAsStream("/icons/app-icon.png")) {
            if (is != null) {
                primaryStage.getIcons().add(new Image(is));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Application icon not available or failed to load", e);
        }

        MainController rootController = new MainController(primaryStage);
        var settingsService = rootController.getSettingsService();

        // Restore window dimensions
        double savedW = settingsService.getWindowWidth();
        double savedH = settingsService.getWindowHeight();
        double width = Math.max(MIN_WIDTH, savedW > 0 ? savedW : DEFAULT_WIDTH);
        double height = Math.max(MIN_HEIGHT, savedH > 0 ? savedH : DEFAULT_HEIGHT);

        Scene scene = new Scene(rootController, width, height);

        // Apply base style and initial theme
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

        // Initialize motion settings
        com.librarymanager.util.AnimationUtil.setReduceMotion(settingsService.isReduceMotionEnabled());

        primaryStage.setScene(scene);

        // Check if saved position is visible on any connected display
        double savedX = settingsService.getWindowPosX();
        double savedY = settingsService.getWindowPosY();
        if (savedX >= 0 && savedY >= 0) {
            boolean visibleOnAnyScreen = false;
            for (javafx.stage.Screen s : javafx.stage.Screen.getScreens()) {
                javafx.geometry.Rectangle2D b = s.getVisualBounds();
                if (b.contains(savedX + 50, savedY + 50)) {
                    visibleOnAnyScreen = true;
                    break;
                }
            }
            if (visibleOnAnyScreen) {
                primaryStage.setX(savedX);
                primaryStage.setY(savedY);
            }
        }

        boolean savedMaximized = settingsService.isWindowMaximized();
        if (savedMaximized) {
            primaryStage.setMaximized(true);
        }

        // Save geometry on close
        primaryStage.setOnCloseRequest(e -> {
            settingsService.setWindowMaximized(primaryStage.isMaximized());
            if (!primaryStage.isMaximized()) {
                settingsService.setWindowWidth(primaryStage.getWidth());
                settingsService.setWindowHeight(primaryStage.getHeight());
                settingsService.setWindowPosX(primaryStage.getX());
                settingsService.setWindowPosY(primaryStage.getY());
            }
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
