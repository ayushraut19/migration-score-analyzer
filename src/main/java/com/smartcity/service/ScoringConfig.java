package com.smartcity.service;

/**
 * Configuration class for scoring engine parameters.
 * Centralizes all tuning constants and thresholds for easy adjustment.
 * 
 * Values are organized by category for clarity.
 * Adjust these values to fine-tune scoring behavior without code changes.
 */
public class ScoringConfig {

    // ===== SAFETY THRESHOLDS =====
    /** Minimum safety score required for a locality to be considered safe */
    public static final double MINIMUM_SAFETY_THRESHOLD = 4.0;
    
    /** Safety score above which bonus is applied */
    public static final double SAFETY_BONUS_THRESHOLD = 8.5;
    
    /** Penalty multiplier for safety below minimum (applied to entire score) */
    public static final double SAFETY_PENALTY_MULTIPLIER = 0.6;
    
    // ===== COST/RENT PENALTIES =====
    /** Penalty applied when rent exceeds budget (per 10% overflow) */
    public static final double RENT_OVERFLOW_PENALTY_PER_10_PERCENT = 0.5;
    
    /** Maximum penalty that can be applied for budget overflow */
    public static final double MAX_RENT_OVERFLOW_PENALTY = 3.0;
    
    // ===== POLLUTION PENALTIES =====
    /** Exponential power applied to pollution penalty (higher = more aggressive) */
    public static final double POLLUTION_PENALTY_EXPONENT = 1.3;
    
    /** Maximum penalty for high pollution */
    public static final double MAX_POLLUTION_PENALTY = 2.5;
    
    // ===== POPULATION DENSITY PENALTIES =====
    /** Penalty applied for high population density */
    public static final double HIGH_DENSITY_PENALTY = 0.3;
    
    /** Density threshold above which penalty applies (people per sq km) */
    public static final double DENSITY_THRESHOLD = 5000;
    
    // ===== CONDITIONAL RULE MULTIPLIERS =====
    /** Score multiplier if locality has poor transportation (transport score < 4) */
    public static final double POOR_TRANSPORT_MULTIPLIER = 0.85;
    
    /** Score multiplier for remote workers (reduce importance of job scores) */
    public static final double REMOTE_WORKER_BONUS_MULTIPLIER = 1.05;
    
    /** Score multiplier if health/education critical for family but locality weak in these */
    public static final double FAMILY_CRITICAL_MISMATCH_MULTIPLIER = 0.80;
    
    // ===== BONUS SYSTEM =====
    /** Bonus for excellent safety + healthcare combination */
    public static final double SAFETY_HEALTHCARE_BONUS = 0.8;
    
    /** Bonus for excellent safety + education combination (if applicable) */
    public static final double SAFETY_EDUCATION_BONUS = 0.7;
    
    /** Bonus for good budget match (rent within 30% of budget) */
    public static final double GOOD_BUDGET_MATCH_BONUS = 0.5;
    
    /** Bonus for balanced scores (low variance across metrics) */
    public static final double BALANCED_SCORE_BONUS = 0.6;
    
    /** Bonus for top-performing locality (high scores across all metrics) */
    public static final double EXCELLENT_LOCALITY_BONUS = 1.0;
    
    // ===== BALANCE DETECTION =====
    /** Standard deviation threshold below which scores are considered "balanced" */
    public static final double BALANCE_VARIANCE_THRESHOLD = 1.5;
    
    // ===== HYBRID SCORING FORMULA WEIGHTS =====
    /** Weight of weighted sum in final score (should sum to 1.0 with penalties/bonuses) */
    public static final double WEIGHTED_SUM_FACTOR = 0.70;
    
    /** Weight of bonus factors in final score */
    public static final double BONUS_FACTOR = 0.20;
    
    /** Weight of penalties in final score (reduction) */
    public static final double PENALTY_FACTOR = 0.30;
    
    // ===== RE-RANKING CONFIGURATION =====
    /** Number of top localities to apply balance boost to */
    public static final int RE_RANK_TOP_N_BOOST = 3;
    
    /** Boost applied to balanced top localities during re-ranking */
    public static final double RE_RANK_BALANCE_BOOST = 0.3;
    
    /** Minimum score threshold for inclusion in final recommendations */
    public static final double MINIMUM_RECOMMENDATION_SCORE = 3.5;
    
    // ===== DATA RANGES (for normalization) =====
    /** Maximum expected job index value */
    public static final double MAX_JOB_INDEX = 10.0;
    
    /** Maximum expected hospital rating */
    public static final double MAX_HOSPITAL_RATING = 10.0;
    
    /** Maximum expected transport score */
    public static final double MAX_TRANSPORT_SCORE = 10.0;
    
    /** Maximum expected safety score */
    public static final double MAX_SAFETY_SCORE = 10.0;
    
    /** Maximum expected pollution index */
    public static final double MAX_POLLUTION_INDEX = 100.0;
    
    /** Maximum expected lifestyle score */
    public static final double MAX_LIFESTYLE_SCORE = 10.0;
    
    /** Maximum expected population density */
    public static final double MAX_POPULATION_DENSITY = 15000.0;
    
    // ===== WEIGHT DISTRIBUTION =====
    /** Default weight for dimensions if not specified */
    public static final double DEFAULT_WEIGHT = 1.0;
    
    /** Family size threshold above which safety/healthcare become critical */
    public static final int FAMILY_SIZE_CRITICAL_THRESHOLD = 3;
    
    /**
     * Provides configuration description for logging/debugging.
     * Useful for understanding what parameters affect scoring.
     */
    public static String getConfigurationSummary() {
        return String.format(
            "ScoringConfig Summary:\n" +
            "  Safety Minimum: %.1f, Bonus Threshold: %.1f\n" +
            "  Safety Penalty Multiplier: %.1f\n" +
            "  Pollution Penalty Exponent: %.1f\n" +
            "  Remote Worker Bonus: %.2f\n" +
            "  Formula Weights - Sum: %.0f%%, Bonus: %.0f%%, Penalty: %.0f%%",
            MINIMUM_SAFETY_THRESHOLD,
            SAFETY_BONUS_THRESHOLD,
            SAFETY_PENALTY_MULTIPLIER,
            POLLUTION_PENALTY_EXPONENT,
            REMOTE_WORKER_BONUS_MULTIPLIER,
            WEIGHTED_SUM_FACTOR * 100,
            BONUS_FACTOR * 100,
            PENALTY_FACTOR * 100
        );
    }
}
