package com.smartcity;

/**
 * Application version and metadata
 */
public class AppInfo {
    public static final String APP_NAME = "Smart City Recommendation System";
    public static final String APP_TITLE = "Migration Score Analyzer";
    public static final String VERSION = "1.0.0";
    public static final String AUTHOR = "Development Team";
    public static final String DESCRIPTION = 
        "A production-level Java desktop application that recommends " +
        "the best locality for users based on weighted factors such as " +
        "job opportunities, cost of living, healthcare, transport, safety, " +
        "environment, and lifestyle.";
    
    public static String getFullTitle() {
        return APP_NAME + " - " + APP_TITLE;
    }
    
    public static String getVersionString() {
        return "Version " + VERSION;
    }
}
