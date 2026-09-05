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
        Scene scene = new Scene(rootController, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // Apply base style and initial theme
        String baseStyle = getClass().getResource("/css/styles.css").toExternalForm();
        String themeStyle = rootController.getSettingsService().isDarkMode()
                ? getClass().getResource("/css/theme-dark.css").toExternalForm()
                : getClass().getResource("/css/theme-light.css").toExternalForm();
        scene.getStylesheets().addAll(themeStyle, baseStyle);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
