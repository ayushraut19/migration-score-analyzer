package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.UserPreferences;

/**
 * Handles conditional, decision-based rules for the scoring engine.
 * These rules adjust scores based on user-specific contexts and preferences.
 * 
 * Rules are applied as multipliers to the base score to reflect real-world priorities.
 */
public class DecisionRulesEngine {

    /**
     * Apply all conditional decision rules to a base score.
     * Returns a multiplier (0.5 - 1.5) that adjusts the score based on user context.
     *
     * @param locality    The locality being evaluated
     * @param preferences The user's preferences and constraints
     * @return Multiplier to apply to base score (1.0 = no change)
     */
    public double calculateDecisionRuleMultiplier(Locality locality, UserPreferences preferences) {
        double multiplier = 1.0;

        // Apply each rule
        multiplier *= applyRemoteWorkerRule(locality, preferences);
        multiplier *= applyLargeFamilyRule(locality, preferences);
        multiplier *= applyPoorTransportRule(locality, preferences);
        multiplier *= applyWeakJobMarketRule(locality, preferences);

        // Clamp multiplier to reasonable range
        return Math.max(0.5, Math.min(1.5, multiplier));
    }

    /**
     * RULE: Remote workers don't need strong job markets.
     * Boost their scores because job opportunities are less critical.
     *
     * Logic:
     * - If work type is REMOTE: apply small boost
     * - This lets lifestyle/environment factors take precedence
     *
     * @param locality    The locality being evaluated
     * @param preferences User's work type
     * @return Multiplier (1.0 = no change, >1.0 = boost)
     */
    private double applyRemoteWorkerRule(Locality locality, UserPreferences preferences) {
        if ("Remote".equalsIgnoreCase(preferences.getWorkType())) {
            // Remote workers: lifestyle and environment more important
            // Slight boost to recognize lifestyle matters more
            return ScoringConfig.REMOTE_WORKER_BONUS_MULTIPLIER;
        }
        return 1.0;
    }

    /**
     * RULE: Large families need excellent safety and healthcare.
     * Adjust scores based on family size and available services.
     *
     * Logic:
     * - If family size > 3 AND (healthcare or safety is weak): apply penalty multiplier
     * - Rationale: families have more vulnerability; children need safe environment
     *
     * @param locality    The locality being evaluated
     * @param preferences User's family size
     * @return Multiplier (1.0 = no change, <1.0 = penalty)
     */
    private double applyLargeFamilyRule(Locality locality, UserPreferences preferences) {
        int familySize = preferences.getFamilySize();

        // Only apply if family is larger than threshold
        if (familySize <= ScoringConfig.FAMILY_SIZE_CRITICAL_THRESHOLD) {
            return 1.0;
        }

        double healthcareScore = locality.getHospitalRating();
        double safetyScore = locality.getSafetyScore();

        // If family needs critical services but locality is weak: apply penalty
        if (preferences.getHealthcareWeight() >= 7 && 
            (healthcareScore < 5.0 || safetyScore < 5.0)) {
            
            // Reduce score for poor healthcare/safety in family context
            return ScoringConfig.FAMILY_CRITICAL_MISMATCH_MULTIPLIER;
        }

        // If healthcare is excellent, provide slight boost for family
        if (healthcareScore >= 8.5 && safetyScore >= 8.0) {
            return 1.1; // Families value safety+healthcare highly
        }

        return 1.0;
    }

    /**
     * RULE: Poor transportation is worse for on-site workers.
     * Adjust based on work type and transport availability.
     *
     * Logic:
     * - If work type is ON_SITE and transport score is low: apply penalty
     * - Rationale: on-site workers must commute; poor transport is painful
     *
     * @param locality    The locality being evaluated
     * @param preferences User's work type
     * @return Multiplier (1.0 = no change, <1.0 = penalty)
     */
    private double applyPoorTransportRule(Locality locality, UserPreferences preferences) {
        String workType = preferences.getWorkType();
        
        // If not remote, user must commute
        if (!"Remote".equalsIgnoreCase(workType)) {
            double transportScore = locality.getTransportScore();
            
            if (transportScore < 4.0) {
                // Poor transportation for commuters is a real problem
                return ScoringConfig.POOR_TRANSPORT_MULTIPLIER;
            }
        }

        return 1.0;
    }

    /**
     * RULE: Weak job markets hurt on-site workers but not remote workers.
     * Adjust based on job importance and work type.
     *
     * Logic:
     * - If work type is ON_SITE AND job score is very low AND user prioritizes jobs: 
     *   apply penalty
     * - Rationale: on-site workers need good local job market
     *
     * @param locality    The locality being evaluated
     * @param preferences User's work type and job weight
     * @return Multiplier (1.0 = no change, <1.0 = penalty)
     */
    private double applyWeakJobMarketRule(Locality locality, UserPreferences preferences) {
        String workType = preferences.getWorkType();
        
        // Only penalize on-site workers
        if ("Remote".equalsIgnoreCase(workType)) {
            return 1.0;
        }

        double jobScore = locality.getJobIndex();
        double jobWeight = preferences.getJobWeight();

        // If jobs are critical to user AND locality has very few
        if (jobWeight >= 7 && jobScore < 2.0) {
            // This is a major issue for job seekers
            return 0.75; // Reduce score significantly
        } else if (jobWeight >= 6 && jobScore < 3.5) {
            // Moderate issue
            return 0.90;
        }

        return 1.0;
    }

    /**
     * Check if a locality should be filtered out based on hard constraints.
     * Some constraints are non-negotiable.
     *
     * @param locality    The locality being evaluated
     * @param preferences User's hard constraints
     * @return true if locality passes all hard constraints, false to filter out
     */
    public boolean passesHardConstraints(Locality locality, UserPreferences preferences) {
        // Safety is non-negotiable
        if (locality.getSafetyScore() < ScoringConfig.MINIMUM_SAFETY_THRESHOLD) {
            return false;
        }

        // Budget overflow is negotiable (has penalty instead)
        // But if way over budget, filter it
        if (preferences.getBudget() > 0) {
            double annualRent = locality.getAvgRent() * 12;
            double overageMultiple = annualRent / preferences.getBudget();
            if (overageMultiple > 1.5) { // Over 150% of budget
                return false; // Too expensive to even consider
            }
        }

        return true;
    }
}
