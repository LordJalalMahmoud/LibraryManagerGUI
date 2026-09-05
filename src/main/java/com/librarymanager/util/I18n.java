package com.librarymanager.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internationalization (i18n) utility supporting dynamic switching between
 * English and Arabic (RTL) with UTF-8 resource bundles.
 */
public class I18n {
    private static final Logger LOGGER = Logger.getLogger(I18n.class.getName());

    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_AR = Locale.forLanguageTag("ar");

    private static Locale currentLocale = LOCALE_EN;
    private static PropertyResourceBundle currentBundle;
    private static final List<Consumer<Locale>> localeChangeListeners = new ArrayList<>();

    static {
        loadBundle(LOCALE_EN);
    }

    public static synchronized void setLanguage(String langCode) {
        if ("ar".equalsIgnoreCase(langCode)) {
            setLocale(LOCALE_AR);
        } else {
            setLocale(LOCALE_EN);
        }
    }

    public static synchronized void setLocale(Locale locale) {
        if (locale == null) locale = LOCALE_EN;
        currentLocale = locale;
        loadBundle(locale);
        notifyLocaleChanged(locale);
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static boolean isRTL() {
        return "ar".equalsIgnoreCase(currentLocale.getLanguage());
    }

    private static void loadBundle(Locale locale) {
        String lang = locale.getLanguage();
        String resourcePath = "/i18n/messages_" + lang + ".properties";
        try (InputStream is = I18n.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    currentBundle = new PropertyResourceBundle(reader);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load bundle for locale: " + locale, e);
        }

        // Fallback to English
        try (InputStream is = I18n.class.getResourceAsStream("/i18n/messages_en.properties")) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    currentBundle = new PropertyResourceBundle(reader);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load fallback English bundle", e);
        }
    }

    public static String get(String key, Object... args) {
        if (key == null) return "";
        String val = key;
        if (currentBundle != null && currentBundle.containsKey(key)) {
            val = currentBundle.getString(key);
        }

        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(val, args);
            } catch (Exception e) {
                return val;
            }
        }
        return val;
    }

    public static void addLocaleChangeListener(Consumer<Locale> listener) {
        if (listener != null) {
            localeChangeListeners.add(listener);
        }
    }

    private static void notifyLocaleChanged(Locale newLocale) {
        for (Consumer<Locale> listener : localeChangeListeners) {
            try {
                listener.accept(newLocale);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing locale change listener", e);
            }
        }
    }
}
