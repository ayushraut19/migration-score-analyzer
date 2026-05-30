package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.UserPreferences;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all bonus calculations for the scoring engine.
 * Bonuses reward localities that excel in specific combinations of factors.
 * 
 * Design: Each bonus is independent and comes with a documented reason.
 * Bonuses are additive to the weighted score, making excellent localities stand out.
 */
public class BonusCalculator {

    private final List<String> bonusReasons = new ArrayList<>();

    /**
     * Calculate all applicable bonuses for a locality.
     * Returns total bonus amount to be added to base score.
     *
     * @param locality    The locality being evaluated
     * @param preferences The user's preferences
     * @param scoreBreakdown Map of factor scores (for balance detection)
     * @return Total bonus (cumulative from all conditions)
     */
    public double calculateTotalBonuses(Locality locality, UserPreferences preferences,
                                       java.util.Map<String, Double> scoreBreakdown) {
        bonusReasons.clear();
        double totalBonus = 0.0;

        // Strong safety + healthcare combination
        totalBonus += calculateSafetyHealthcareBonus(locality);

        // Strong safety + education combination (proxy: environment quality)
        totalBonus += calculateSafetyEnvironmentBonus(locality);

        // Good budget affordability match
        totalBonus += calculateBudgetMatchBonus(locality, preferences);

        // Balanced scores across all metrics
        totalBonus += calculateBalanceBonus(scoreBreakdown);

        // Remote worker bonus (if applicable)
        totalBonus += calculateRemoteWorkerBonus(locality, preferences);

        // Excellent overall locality bonus
        totalBonus += calculateExcellentLocalityBonus(scoreBreakdown);

        return totalBonus;
    }

    /**
     * Bonus for localities with excellent safety AND healthcare.
     * Particularly relevant for families.
     *
     * @param locality The locality being evaluated
     * @return Bonus value (addition to score)
     */
    private double calculateSafetyHealthcareBonus(Locality locality) {
        double safetyScore = locality.getSafetyScore();
        double healthcareScore = locality.getHospitalRating();

        // Both must be strong for this bonus
        if (safetyScore >= ScoringConfig.SAFETY_BONUS_THRESHOLD && 
            healthcareScore >= 8.0) {
            
            bonusReasons.add(String.format(
                "Strong safety (%.1f) and healthcare (%.1f) combination provides excellent family security",
                safetyScore,
                healthcareScore
            ));
            
            return ScoringConfig.SAFETY_HEALTHCARE_BONUS;
        }

        return 0.0;
    }

    /**
     * Bonus for localities with excellent safety AND clean environment.
     * Shows commitment to comprehensive quality of life.
     *
     * @param locality The locality being evaluated
     * @return Bonus value (addition to score)
     */
    private double calculateSafetyEnvironmentBonus(Locality locality) {
        double safetyScore = locality.getSafetyScore();
        double pollutionIndex = locality.getPollutionIndex();

        // Low pollution (good environment) + high safety
        if (safetyScore >= ScoringConfig.SAFETY_BONUS_THRESHOLD && 
            pollutionIndex < 30) {
            
            bonusReasons.add(String.format(
                "Excellent safety and clean environment (pollution: %.1f) create ideal living conditions",
                pollutionIndex
            ));
            
            return ScoringConfig.SAFETY_EDUCATION_BONUS;
        }

        return 0.0;
    }

    /**
     * Bonus for good budget affordability match.
     * User feels comfortable financially in this locality.
     *
     * @param locality    The locality being evaluated
     * @param preferences User's budget
     * @return Bonus value (addition to score)
     */
    private double calculateBudgetMatchBonus(Locality locality, UserPreferences preferences) {
        if (preferences.getBudget() <= 0) {
            return 0.0; // No constraint
        }

        double monthlyRent = locality.getAvgRent();
        double annualRent = monthlyRent * 12;
        double userBudget = preferences.getBudget();
        double rentPercentageOfBudget = annualRent / userBudget;

        // Good match: rent is within 25-35% of budget (comfortable but not wasteful)
        if (rentPercentageOfBudget >= 0.20 && rentPercentageOfBudget <= 0.35) {
            double matchScore = (0.35 - rentPercentageOfBudget) * 50; // Better if closer to 20%
            double bonus = Math.min(ScoringConfig.GOOD_BUDGET_MATCH_BONUS, matchScore * 0.1);
            
            bonusReasons.add(String.format(
                "Excellent budget match: rent (%.0f%% of budget) is comfortable and sustainable",
                rentPercentageOfBudget * 100
            ));
            
            return bonus;
        }

        return 0.0;
    }

    /**
     * Bonus for balanced scores across all metrics.
     * No single weak factor that pulls down overall appeal.
     * 
     * This rewards "jack of all trades" localities that are good at everything.
     *
     * @param scoreBreakdown Map of factor scores
     * @return Bonus value (addition to score)
     */
    private double calculateBalanceBonus(java.util.Map<String, Double> scoreBreakdown) {
        if (scoreBreakdown == null || scoreBreakdown.isEmpty()) {
            return 0.0;
        }

        // Calculate standard deviation of all scores
        double mean = scoreBreakdown.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(5.0);

        double variance = scoreBreakdown.values().stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);

        // If scores are tightly grouped (low variance), it's balanced
        if (stdDev < ScoringConfig.BALANCE_VARIANCE_THRESHOLD) {
            bonusReasons.add(String.format(
                "Balanced profile: all factors score well with low variance (%.2f), no obvious weakness",
                stdDev
            ));
            
            return ScoringConfig.BALANCED_SCORE_BONUS;
        }

        return 0.0;
    }

    /**
     * Special bonus for remote workers when locality has good quality of life.
     * Remote workers care less about jobs, more about lifestyle and cost.
     *
     * @param locality    The locality being evaluated
     * @param preferences User's work type
     * @return Bonus value (addition to score)
     */
    private double calculateRemoteWorkerBonus(Locality locality, UserPreferences preferences) {
        if (!"Remote".equalsIgnoreCase(preferences.getWorkType())) {
            return 0.0;
        }

        double lifestyleScore = locality.getLifestyleScore();
        double pollutionIndex = locality.getPollutionIndex();
        double safetyScore = locality.getSafetyScore();

        // Remote workers value lifestyle, safety, and environment
        if (lifestyleScore >= 7.0 && pollutionIndex < 40 && safetyScore >= 6.0) {
            bonusReasons.add(String.format(
                "Perfect remote-friendly locality: great lifestyle (%.1f), safety (%.1f), clean environment",
                lifestyleScore,
                safetyScore
            ));
            
            return 0.4;
        }

        return 0.0;
    }

    /**
     * Bonus for localities that excel across multiple dimensions.
     * If majority of factors are 7+, this is an excellent locality.
     *
     * @param scoreBreakdown Map of factor scores
     * @return Bonus value (addition to score)
     */
    private double calculateExcellentLocalityBonus(java.util.Map<String, Double> scoreBreakdown) {
        if (scoreBreakdown == null || scoreBreakdown.isEmpty()) {
            return 0.0;
        }

        long excellentCount = scoreBreakdown.values().stream()
                .filter(score -> score >= 7.5)
                .count();

        long totalCount = scoreBreakdown.size();

        // If 70%+ of factors are excellent
        if ((double) excellentCount / totalCount >= 0.7) {
            bonusReasons.add(String.format(
                "Outstanding locality: %d out of %d factors score excellently (7.5+)",
                excellentCount,
                totalCount
            ));
            
            return ScoringConfig.EXCELLENT_LOCALITY_BONUS;
        }

        return 0.0;
    }

    /**
     * Get list of reasons for bonuses applied.
     *
     * @return List of bonus reason strings
     */
    public List<String> getBonusReasons() {
        return new ArrayList<>(bonusReasons);
    }
}
