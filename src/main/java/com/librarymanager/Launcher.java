package com.librarymanager;

/**
 * Main launcher entry point to bypass JavaFX runtime module checks
 * when executing from packaged JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
