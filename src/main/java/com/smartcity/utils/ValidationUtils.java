package com.smartcity.utils;

/**
 * Data validation utilities
 */
public class ValidationUtils {

    private static final double SCORE_10_MIN = 0.0;
    private static final double SCORE_10_MAX = 10.0;
    private static final double SCORE_100_MIN = 0.0;
    private static final double SCORE_100_MAX = 100.0;
    
    /**
     * Validate email
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validate budget
     */
    public static boolean isValidBudget(double budget) {
        return budget > 0 && budget < 100000000;
    }

    /**
     * Validate weight value
     */
    public static boolean isValidWeight(double weight) {
        return weight >= 0 && weight <= 10;
    }

    /**
     * Validate score on 0-100 scale.
     */
    public static boolean isValidPercentageScore(double score) {
        return score >= SCORE_100_MIN && score <= SCORE_100_MAX;
    }

    /**
     * Clamp score to 0-10 scale.
     */
    public static double clampScore10(double score) {
        return clamp(score, SCORE_10_MIN, SCORE_10_MAX);
    }

    /**
     * Clamp score to 0-100 scale.
     */
    public static int clampScore100(double score) {
        return (int) Math.round(clamp(score, SCORE_100_MIN, SCORE_100_MAX));
    }

    /**
     * Validate family size
     */
    public static boolean isValidFamilySize(int size) {
        return size > 0 && size <= 20;
    }

    /**
     * Clamp value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
