package com.smartcity.utils;

/**
 * Utility class for normalizing various metrics to a 0-10 scale.
 * Ensures consistent normalization across the scoring engine.
 */
public class NormalizationUtil {

    /**
     * Normalize a metric to 0-10 range using linear scaling.
     * Clamps values outside min/max to min/max.
     *
     * @param value       The value to normalize
     * @param minValue    The minimum expected value
     * @param maxValue    The maximum expected value
     * @return Normalized value (0-10)
     */
    public static double normalizeLinear(double value, double minValue, double maxValue) {
        if (maxValue <= minValue) {
            return 5.0; // Default neutral value if range invalid
        }
        
        // Clamp value to range
        double clampedValue = Math.max(minValue, Math.min(maxValue, value));
        
        // Normalize to 0-10
        return (clampedValue - minValue) / (maxValue - minValue) * 10;
    }

    /**
     * Normalize rent as percentage of budget (inverse scoring).
     * Lower rent (as % of budget) gets higher score.
     *
     * @param monthlyRent  Monthly rent in dollars
     * @param annualBudget Annual budget in dollars
     * @return Normalized score (0-10)
     */
    public static double normalizeBudgetAffordability(double monthlyRent, double annualBudget) {
        if (annualBudget <= 0) {
            return 5.0; // Neutral if no budget constraint
        }
        
        double annualRent = monthlyRent * 12;
        double rentPercentageOfBudget = annualRent / annualBudget;
        
        // Scoring thresholds
        if (rentPercentageOfBudget <= 0.25) {
            return 10.0; // Excellent - rent is 25% or less of annual budget
        } else if (rentPercentageOfBudget <= 0.35) {
            return 8.5; // Very Good
        } else if (rentPercentageOfBudget <= 0.50) {
            return 7.0; // Good
        } else if (rentPercentageOfBudget <= 0.75) {
            return 5.0; // Moderate
        } else if (rentPercentageOfBudget <= 1.0) {
            return 2.5; // Tight
        } else {
            return 0.0; // Over budget
        }
    }

    /**
     * Normalize pollution index (inverse scoring).
     * Lower pollution gets higher score.
     *
     * @param pollutionIndex  Pollution index value (0-100 or similar)
     * @param maxPollution    Maximum expected pollution value
     * @return Normalized score (0-10)
     */
    public static double normalizePollution(double pollutionIndex, double maxPollution) {
        if (maxPollution <= 0) {
            return 5.0; // Default neutral
        }
        
        // Clamp pollution to max
        double clampedPollution = Math.max(0, Math.min(maxPollution, pollutionIndex));
        
        // Inverse: lower pollution = higher score
        double normalizedScore = 10 - (clampedPollution / maxPollution * 10);
        return Math.max(0, normalizedScore);
    }

    /**
     * Normalize population density (inverse scoring for urban preference).
     * Lower density gets higher score (for those who prefer space).
     *
     * @param density    Population density (people per sq km or similar)
     * @param maxDensity Maximum expected density
     * @return Normalized score (0-10)
     */
    public static double normalizeDensity(double density, double maxDensity) {
        if (maxDensity <= 0) {
            return 5.0; // Default neutral
        }
        
        double clampedDensity = Math.max(0, Math.min(maxDensity, density));
        double normalizedScore = 10 - (clampedDensity / maxDensity * 10);
        return Math.max(0, normalizedScore);
    }

    /**
     * Normalize a metric that is already on 0-10 scale.
     * Simply bounds-checks it to ensure valid range.
     *
     * @param value The value to normalize
     * @return Normalized value (0-10)
     */
    public static double normalize0To10(double value) {
        return Math.max(0, Math.min(10, value));
    }

    /**
     * Normalize a metric on arbitrary scale to 0-10.
     * If value is already 0-10, just validates. Otherwise scales.
     *
     * @param value   The value to normalize
     * @param maxVal  The maximum value in the original scale
     * @return Normalized value (0-10)
     */
    public static double normalizeFromMax(double value, double maxVal) {
        if (maxVal <= 0 || value < 0) {
            return 0.0;
        }
        
        double normalized = (value / maxVal) * 10;
        return Math.min(10, normalized);
    }

    /**
     * Clamp a score to valid 0-10 range.
     *
     * @param score The score to clamp
     * @return Score bounded to [0, 10]
     */
    public static double clamp(double score) {
        return Math.max(0, Math.min(10, score));
    }

    /**
     * Check if value is within acceptable range.
     *
     * @param value The value to check
     * @param min   Minimum acceptable value
     * @param max   Maximum acceptable value
     * @return true if value is in [min, max]
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
