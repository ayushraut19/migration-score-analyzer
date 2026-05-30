package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.UserPreferences;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all penalty calculations for the scoring engine.
 * Penalties reduce scores based on real-world constraints and negative conditions.
 * 
 * Design: Each penalty is calculated independently and documented with a reason.
 */
public class PenaltyCalculator {

    private final List<String> penaltyReasons = new ArrayList<>();

    /**
     * Calculate all applicable penalties for a locality.
     * Returns total penalty amount to be subtracted from base score.
     *
     * @param locality    The locality being evaluated
     * @param preferences The user's preferences
     * @return Total penalty (cumulative from all conditions)
     */
    public double calculateTotalPenalties(Locality locality, UserPreferences preferences) {
        penaltyReasons.clear();
        double totalPenalty = 0.0;

        // Safety penalty (critical - can drastically reduce score)
        totalPenalty += calculateSafetyPenalty(locality);

        // Budget overflow penalty (realistic constraint)
        totalPenalty += calculateBudgetOverflowPenalty(locality, preferences);

        // Pollution penalty (exponential for bad air quality)
        totalPenalty += calculatePollutionPenalty(locality);

        // Population density penalty
        totalPenalty += calculateDensityPenalty(locality);

        // Poor transportation penalty
        totalPenalty += calculateTransportationPenalty(locality);

        // Job mismatch penalty for non-remote workers
        totalPenalty += calculateJobMismatchPenalty(locality, preferences);

        // Healthcare mismatch penalty for large families
        totalPenalty += calculateHealthcareMismatchPenalty(locality, preferences);

        return totalPenalty;
    }

    /**
     * CRITICAL PENALTY: Apply penalty if safety is below minimum threshold.
     * Safety is non-negotiable for human wellbeing.
     *
     * @param locality The locality being evaluated
     * @return Penalty value (multiplier or deduction)
     */
    private double calculateSafetyPenalty(Locality locality) {
        double safetyScore = locality.getSafetyScore();
        
        // If safety is critically low, reduce overall score significantly
        if (safetyScore < ScoringConfig.MINIMUM_SAFETY_THRESHOLD) {
            double safetyGap = ScoringConfig.MINIMUM_SAFETY_THRESHOLD - safetyScore;
            double penalty = safetyGap * 0.5; // 0.5 per point below minimum
            penaltyReasons.add(String.format(
                "Safety concern: score %.1f is below acceptable threshold (%.1f)", 
                safetyScore, 
                ScoringConfig.MINIMUM_SAFETY_THRESHOLD
            ));
            return Math.min(penalty, 3.0); // Cap at 3.0
        }
        
        // Moderate penalty even above minimum but still concerning
        if (safetyScore < 5.0) {
            double penalty = (5.0 - safetyScore) * 0.3;
            penaltyReasons.add(String.format(
                "Safety warning: score %.1f is below ideal (5.0)", 
                safetyScore
            ));
            return penalty;
        }

        return 0.0;
    }

    /**
     * Apply penalty proportional to rent exceeding budget.
     * Realistic constraint: if rent forces you over budget, score is hurt.
     *
     * @param locality    The locality being evaluated
     * @param preferences User's budget constraint
     * @return Penalty value (deduction from score)
     */
    private double calculateBudgetOverflowPenalty(Locality locality, UserPreferences preferences) {
        if (preferences.getBudget() <= 0) {
            return 0.0; // No constraint
        }

        double monthlyRent = locality.getAvgRent();
        double annualRent = monthlyRent * 12;
        double userBudget = preferences.getBudget();
        double rentPercentageOfBudget = annualRent / userBudget;

        // If rent fits within budget, no penalty
        if (rentPercentageOfBudget <= 1.0) {
            return 0.0;
        }

        // For each 10% over budget, apply increasing penalty
        double overagePercentage = (rentPercentageOfBudget - 1.0) * 100; // % over budget
        double numPenaltyUnits = Math.ceil(overagePercentage / 10.0);
        double penalty = numPenaltyUnits * ScoringConfig.RENT_OVERFLOW_PENALTY_PER_10_PERCENT;
        penalty = Math.min(penalty, ScoringConfig.MAX_RENT_OVERFLOW_PENALTY);

        penaltyReasons.add(String.format(
            "Budget constraint: monthly rent (₹%.0f, annual ₹%.0f) exceeds annual budget (₹%.0f) by %.1f%%",
            monthlyRent,
            annualRent,
            userBudget,
            (rentPercentageOfBudget - 1.0) * 100
        ));

        return penalty;
    }

    /**
     * Apply exponential penalty for high pollution.
     * Higher pollution = non-linear (worse) penalty.
     * Formula: penalty = (pollutionIndex / maxIndex) ^ exponent
     *
     * @param locality The locality being evaluated
     * @return Penalty value (deduction from score)
     */
    private double calculatePollutionPenalty(Locality locality) {
        double pollutionIndex = locality.getPollutionIndex();
        
        if (pollutionIndex < 0) {
            return 0.0;
        }

        // Normalize pollution to 0-1 range
        double normalizedPollution = Math.min(1.0, 
            pollutionIndex / ScoringConfig.MAX_POLLUTION_INDEX);

        // Apply exponential to make it non-linear (high pollution is worse)
        double pollutionFactor = Math.pow(normalizedPollution, 
            ScoringConfig.POLLUTION_PENALTY_EXPONENT);

        // Convert to penalty points (0.0 - maxPenalty)
        double penalty = pollutionFactor * ScoringConfig.MAX_POLLUTION_PENALTY;

        if (penalty > 0.1) {
            penaltyReasons.add(String.format(
                "Air quality: pollution index %.1f requires environmental penalty",
                pollutionIndex
            ));
        }

        return penalty;
    }

    /**
     * Apply penalty for high population density.
     * Less relevant than safety/budget but affects quality of life.
     *
     * @param locality The locality being evaluated
     * @return Penalty value (deduction from score)
     */
    private double calculateDensityPenalty(Locality locality) {
        double density = locality.getPopulationDensity();
        
        if (density <= ScoringConfig.DENSITY_THRESHOLD) {
            return 0.0; // Acceptable density
        }

        // Penalty increases with density above threshold
        double excessDensity = density - ScoringConfig.DENSITY_THRESHOLD;
        double maxExcess = ScoringConfig.MAX_POPULATION_DENSITY - ScoringConfig.DENSITY_THRESHOLD;
        double densityFraction = Math.min(1.0, excessDensity / maxExcess);
        double penalty = densityFraction * ScoringConfig.HIGH_DENSITY_PENALTY;

        if (penalty > 0.05) {
            penaltyReasons.add(String.format(
                "Population density: %.0f people/km² may feel crowded",
                density
            ));
        }

        return penalty;
    }

    /**
     * Apply penalty for poor transportation infrastructure.
     * Affects convenience for job commute (especially for on-site workers).
     *
     * @param locality The locality being evaluated
     * @return Penalty value (deduction from score)
     */
    private double calculateTransportationPenalty(Locality locality) {
        double transportScore = locality.getTransportScore();
        
        if (transportScore >= 4.0) {
            return 0.0; // Acceptable transport
        }

        // Penalty for poor transport
        double transportGap = 4.0 - transportScore;
        double penalty = transportGap * 0.25; // 0.25 per point below 4.0

        penaltyReasons.add(String.format(
            "Transportation: score %.1f suggests limited public transit/accessibility",
            transportScore
        ));

        return Math.min(penalty, 1.0);
    }

    /**
     * Apply penalty if job opportunities are poor for on-site workers.
     * Remote workers don't need this penalty.
     *
     * @param locality    The locality being evaluated
     * @param preferences User's work configuration
     * @return Penalty value (deduction from score)
     */
    private double calculateJobMismatchPenalty(Locality locality, UserPreferences preferences) {
        // Don't penalize remote workers
        if ("Remote".equalsIgnoreCase(preferences.getWorkType())) {
            return 0.0;
        }

        double jobScore = locality.getJobIndex();
        double jobWeight = preferences.getJobWeight();

        // Only penalize if jobs are important to this user AND locality is weak
        if (jobWeight >= 6 && jobScore < 3.0) {
            double penalty = (6.0 - jobScore) * 0.2;
            penaltyReasons.add(String.format(
                "Job market: score %.1f may be insufficient for on-site role seekers",
                jobScore
            ));
            return Math.min(penalty, 1.5);
        }

        return 0.0;
    }

    /**
     * Apply penalty if healthcare/safety are critical (family) but locality is weak.
     *
     * @param locality    The locality being evaluated
     * @param preferences User's family/healthcare needs
     * @return Penalty value (deduction from score)
     */
    private double calculateHealthcareMismatchPenalty(Locality locality, UserPreferences preferences) {
        // Only applies to families
        if (preferences.getFamilySize() <= ScoringConfig.FAMILY_SIZE_CRITICAL_THRESHOLD) {
            return 0.0;
        }

        double healthcareWeight = preferences.getHealthcareWeight();
        double healthcareScore = locality.getHospitalRating();

        // If healthcare is important but locality is weak
        if (healthcareWeight >= 7 && healthcareScore < 5.0) {
            double penalty = (7.0 - healthcareScore) * 0.15;
            penaltyReasons.add(String.format(
                "Healthcare concern: family of %d needs better healthcare facilities (score %.1f)",
                preferences.getFamilySize(),
                healthcareScore
            ));
            return Math.min(penalty, 1.2);
        }

        return 0.0;
    }

    /**
     * Get list of reasons for penalties applied.
     *
     * @return List of penalty reason strings
     */
    public List<String> getPenaltyReasons() {
        return new ArrayList<>(penaltyReasons);
    }
}
