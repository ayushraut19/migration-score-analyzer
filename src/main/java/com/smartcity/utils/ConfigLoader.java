package com.smartcity.utils;

import java.io.*;
import java.util.*;

/**
 * Application configuration loader
 */
public class ConfigLoader {
    
    private static Properties properties;
    private static final String CONFIG_FILE = "application.properties";

    static {
        loadConfig();
    }

    /**
     * Load configuration from properties file
     */
    private static void loadConfig() {
        properties = new Properties();
        try {
            InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (input != null) {
                properties.load(input);
                input.close();
            } else {
                // Try to load from file system
                try (InputStream fis = new FileInputStream(CONFIG_FILE)) {
                    properties.load(fis);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load configuration file: " + e.getMessage());
            setDefaults();
        }
    }

    /**
     * Set default properties
     */
    private static void setDefaults() {
        properties.setProperty("app.name", "Smart City Recommendation System");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("ui.theme", "light");
        properties.setProperty("logging.level", "INFO");
    }

    /**
     * Get property value
     */
    public static String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Get property value
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get boolean property
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(String.valueOf(key));
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    /**
     * Get integer property
     */
    public static int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(String.valueOf(key));
            if (value == null) return defaultValue;
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
