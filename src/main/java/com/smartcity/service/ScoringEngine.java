package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;
import com.smartcity.utils.NormalizationUtil;
import java.util.*;

/**
 * UPGRADED: Decision-Aware Scoring Engine
 * 
 * Transforms basic linear scoring into a realistic, multi-dimensional recommendation system.
 * 
 * Key Features:
 * - Normalization layer ensuring 0-10 scale consistency
 * - Penalty-based logic for real-world constraints
 * - Conditional decision rules based on user context
 * - Non-linear hybrid scoring formula
 * - Bonus system for exceptional combinations
 * - Full explainability (penalties, bonuses, breakdowns)
 * 
 * Formula:
 *   finalScore = (weightedSum * 0.7) + (bonuses * 0.2) - (penalties * 0.3)
 *   Clamped to [0, 10]
 */
public class ScoringEngine {

    private final PenaltyCalculator penaltyCalculator;
    private final BonusCalculator bonusCalculator;
    private final DecisionRulesEngine decisionRulesEngine;

    public ScoringEngine() {
        this.penaltyCalculator = new PenaltyCalculator();
        this.bonusCalculator = new BonusCalculator();
        this.decisionRulesEngine = new DecisionRulesEngine();
    }

    /**
     * MAIN METHOD: Calculate comprehensive score for a locality.
     * 
     * Process:
     * 1. Filter hard constraints (safety threshold, extreme budget overage)
     * 2. Normalize all metrics to 0-10 scale
     * 3. Calculate weighted base score
     * 4. Apply decision rule multipliers
     * 5. Calculate penalties and bonuses
     * 6. Apply hybrid formula
     * 7. Generate explainable breakdown
     *
     * @param locality    The locality being evaluated
     * @param preferences The user's preferences
     * @return RecommendationResult with score and detailed explanation
     */
    public RecommendationResult calculateScore(Locality locality, UserPreferences preferences) {
        // Step 1: Early filter for hard budget constraints. Safety remains preference-aware below.
        if (!passesBudgetConstraint(locality, preferences)) {
            double disqualifyScore = 1.0;
            RecommendationResult result = new RecommendationResult(
                locality, 
                disqualifyScore, 
                "Does not meet budget requirements."
            );
            result.setBaseScore(disqualifyScore);
            return result;
        }

        // Step 2: Normalize all metrics to 0-10 scale
        Map<String, Double> breakdown = normalizeAllMetrics(locality, preferences);
        Map<String, Double> effectiveWeights = calculateEffectiveWeights(preferences);

        Map<String, Double> activeBreakdown = filterActiveMetrics(breakdown, effectiveWeights);
        Map<String, Double> activeWeights = filterActiveWeights(effectiveWeights);

        // Step 3: Calculate weighted base score
        double baseScore = calculateWeightedScore(activeBreakdown, activeWeights);

        // Step 4: Apply decision rule multipliers (context-aware)
        double decisionMultiplier = calculateDecisionRuleMultiplier(locality, preferences, activeBreakdown, activeWeights);
        baseScore *= decisionMultiplier;

        // Step 5: Calculate penalties (reductions for bad conditions)
        double penalties = calculatePersonalizedPenalties(locality, preferences, activeBreakdown, activeWeights);

        // Step 6: Calculate bonuses (additions for excellent combinations)
        double bonuses = calculatePersonalizedBonuses(locality, preferences, activeBreakdown, activeWeights);

        // Step 7: Apply hybrid formula
        double finalScore = baseScore + bonuses - penalties;
        finalScore = NormalizationUtil.clamp(finalScore);

        // Step 8: Generate result with full explainability
        String explanation = generateDetailedExplanation(activeBreakdown, preferences, 
                                                        Collections.emptyList(),
                                                        Collections.emptyList());

        RecommendationResult result = new RecommendationResult(locality, finalScore, explanation);
        result.setScoreBreakdown(activeBreakdown);
        result.setBaseScore(baseScore);
        result.setTotalPenalties(penalties);
        result.setTotalBonuses(bonuses);
        result.setPenaltyReasons(Collections.emptyList());
        result.setBonusReasons(Collections.emptyList());

        return result;
    }

    /**
     * Step 2: Normalize all metrics to consistent 0-10 scale.
     * Different metrics have different raw scales; this ensures consistency.
     *
     * @param locality    The locality with raw metrics
     * @param preferences User preferences (for budget context)
     * @return Map of metric name -> normalized score (0-10)
     */
    private Map<String, Double> normalizeAllMetrics(Locality locality, UserPreferences preferences) {
        Map<String, Double> normalized = new LinkedHashMap<>();

        // Job opportunities (typically 0-10 already)
        normalized.put("Job Opportunities", 
            NormalizationUtil.normalize0To10(locality.getJobIndex()));

        // Cost of living based on budget affordability
        normalized.put("Cost of Living", 
            NormalizationUtil.normalizeBudgetAffordability(locality.getAvgRent(), preferences.getBudget()));

        // Healthcare (typically 0-10)
        normalized.put("Healthcare", 
            NormalizationUtil.normalize0To10(locality.getHospitalRating()));

        // Transport (typically 0-10)
        normalized.put("Transport", 
            NormalizationUtil.normalize0To10(locality.getTransportScore()));

        // Safety (typically 0-10)
        normalized.put("Safety", 
            NormalizationUtil.normalize0To10(locality.getSafetyScore()));

        // Environment (inverse: lower pollution is better)
        normalized.put("Environment", 
            NormalizationUtil.normalizePollution(locality.getPollutionIndex(), ScoringConfig.MAX_POLLUTION_INDEX));

        // Lifestyle (typically 0-10)
        normalized.put("Lifestyle", 
            NormalizationUtil.normalize0To10(locality.getLifestyleScore()));

        return normalized;
    }

    private Map<String, Double> filterActiveMetrics(Map<String, Double> breakdown,
                                                    Map<String, Double> effectiveWeights) {
        Map<String, Double> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            if (effectiveWeights.getOrDefault(entry.getKey(), 0.0) > 0) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private Map<String, Double> filterActiveWeights(Map<String, Double> effectiveWeights) {
        Map<String, Double> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : effectiveWeights.entrySet()) {
            if (entry.getValue() > 0) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /**
     * Step 3: Calculate base weighted score from normalized metrics.
     * Uses user's weight preferences to determine importance.
     * Result is before decision rules, penalties, bonuses.
     *
     * @param breakdown   Normalized metric scores
     * @param preferences User weight preferences
     * @return Unweighted score (0-10 scale range)
     */
    private double calculateWeightedScore(Map<String, Double> breakdown, Map<String, Double> effectiveWeights) {
        double totalWeight = effectiveWeights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalWeight <= 0 || breakdown.isEmpty()) {
            return 5.0;
        }

        double score = 0.0;
        for (Map.Entry<String, Double> entry : effectiveWeights.entrySet()) {
            if (entry.getValue() <= 0 || !breakdown.containsKey(entry.getKey())) {
                continue;
            }
            score += breakdown.get(entry.getKey()) * (entry.getValue() / totalWeight);
        }
        return score;
    }

    private Map<String, Double> calculateEffectiveWeights(UserPreferences preferences) {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("Job Opportunities", preferencePower(preferences.getJobWeight()));
        weights.put("Cost of Living", preferencePower(preferences.getCostOfLivingWeight()));
        weights.put("Healthcare", preferencePower(preferences.getHealthcareWeight()));
        weights.put("Transport", preferencePower(preferences.getTransportWeight()));
        weights.put("Safety", preferencePower(preferences.getSafetyWeight()));
        weights.put("Environment", preferencePower(preferences.getEnvironmentWeight()));
        weights.put("Lifestyle", preferencePower(preferences.getLifestyleWeight()));

        applyProfileModifiers(weights, preferences);
        applyFamilyModifiers(weights, preferences);
        applyWorkTypeModifiers(weights, preferences);
        return weights;
    }

    private double preferencePower(double value) {
        double normalized = NormalizationUtil.normalize0To10(value) / 10.0;
        if (normalized <= 0) {
            return 0.0;
        }
        return Math.pow(normalized, 1.65) * 10.0;
    }

    private void applyProfileModifiers(Map<String, Double> weights, UserPreferences preferences) {
        String profile = preferences.getProfileType();
        if ("Student".equalsIgnoreCase(profile)) {
            multiply(weights, "Cost of Living", 1.30);
            multiply(weights, "Transport", 1.20);
            multiply(weights, "Lifestyle", 1.25);
            multiply(weights, "Healthcare", 0.85);
        } else if ("Bachelor".equalsIgnoreCase(profile)) {
            multiply(weights, "Job Opportunities", 1.30);
            multiply(weights, "Lifestyle", 1.25);
            multiply(weights, "Transport", 1.15);
            multiply(weights, "Healthcare", 0.80);
            multiply(weights, "Safety", 0.90);
        } else if ("Family".equalsIgnoreCase(profile)) {
            multiply(weights, "Healthcare", 1.35);
            multiply(weights, "Safety", 1.35);
            multiply(weights, "Cost of Living", 1.20);
            multiply(weights, "Transport", 1.15);
            multiply(weights, "Lifestyle", 0.90);
        }
    }

    private void applyFamilyModifiers(Map<String, Double> weights, UserPreferences preferences) {
        int familySize = preferences.getFamilySize();
        if (familySize >= 5) {
            multiply(weights, "Healthcare", 1.45);
            multiply(weights, "Safety", 1.40);
            multiply(weights, "Cost of Living", 1.30);
            multiply(weights, "Transport", 1.20);
            multiply(weights, "Environment", 1.15);
            multiply(weights, "Lifestyle", 0.85);
            multiply(weights, "Job Opportunities", 0.90);
        } else if (familySize >= 3) {
            multiply(weights, "Healthcare", 1.25);
            multiply(weights, "Safety", 1.25);
            multiply(weights, "Cost of Living", 1.15);
            multiply(weights, "Transport", 1.10);
        } else if (familySize <= 1) {
            multiply(weights, "Job Opportunities", 1.20);
            multiply(weights, "Lifestyle", 1.20);
            multiply(weights, "Cost of Living", 1.15);
            multiply(weights, "Transport", 1.10);
            multiply(weights, "Healthcare", 0.80);
            multiply(weights, "Safety", 0.90);
        }
    }

    private void applyWorkTypeModifiers(Map<String, Double> weights, UserPreferences preferences) {
        String workType = preferences.getWorkType();
        if ("Remote".equalsIgnoreCase(workType)) {
            multiply(weights, "Job Opportunities", 0.35);
            multiply(weights, "Lifestyle", 1.25);
            multiply(weights, "Environment", 1.25);
            multiply(weights, "Cost of Living", 1.15);
            multiply(weights, "Safety", 1.10);
        } else if ("Hybrid".equalsIgnoreCase(workType)) {
            multiply(weights, "Transport", 1.20);
            multiply(weights, "Job Opportunities", 1.05);
            multiply(weights, "Lifestyle", 1.10);
        } else {
            multiply(weights, "Job Opportunities", 1.20);
            multiply(weights, "Transport", 1.25);
        }
    }

    private void multiply(Map<String, Double> weights, String key, double factor) {
        weights.put(key, weights.get(key) * factor);
    }

    private double calculateDecisionRuleMultiplier(Locality locality, UserPreferences preferences,
                                                   Map<String, Double> breakdown,
                                                   Map<String, Double> effectiveWeights) {
        double multiplier = 1.0;
        double totalWeight = effectiveWeights.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalWeight > 0) {
            multiplier += weightedFitAdjustment(getMetricScore(breakdown, "Safety"), effectiveWeights.getOrDefault("Safety", 0.0), totalWeight, 0.18);
            multiplier += weightedFitAdjustment(getMetricScore(breakdown, "Healthcare"), effectiveWeights.getOrDefault("Healthcare", 0.0), totalWeight, 0.15);
            multiplier += weightedFitAdjustment(getMetricScore(breakdown, "Transport"), effectiveWeights.getOrDefault("Transport", 0.0), totalWeight, 0.13);
            multiplier += weightedFitAdjustment(getMetricScore(breakdown, "Job Opportunities"), effectiveWeights.getOrDefault("Job Opportunities", 0.0), totalWeight, 0.15);
            multiplier += weightedFitAdjustment(getMetricScore(breakdown, "Lifestyle"), effectiveWeights.getOrDefault("Lifestyle", 0.0), totalWeight, 0.10);
        }

        if (preferences.getFamilySize() >= 4
                && effectiveWeights.getOrDefault("Healthcare", 0.0) > 0
                && effectiveWeights.getOrDefault("Safety", 0.0) > 0
                && getMetricScore(breakdown, "Healthcare") >= 8
                && getMetricScore(breakdown, "Safety") >= 8) {
            multiplier += 0.06;
        }
        if (preferences.getFamilySize() <= 1
                && effectiveWeights.getOrDefault("Job Opportunities", 0.0) > 0
                && effectiveWeights.getOrDefault("Lifestyle", 0.0) > 0
                && getMetricScore(breakdown, "Job Opportunities") >= 8
                && getMetricScore(breakdown, "Lifestyle") >= 8) {
            multiplier += 0.05;
        }
        if ("Remote".equalsIgnoreCase(preferences.getWorkType())
                && effectiveWeights.getOrDefault("Environment", 0.0) > 0
                && effectiveWeights.getOrDefault("Lifestyle", 0.0) > 0
                && getMetricScore(breakdown, "Environment") >= 8
                && getMetricScore(breakdown, "Lifestyle") >= 7) {
            multiplier += 0.05;
        }

        return Math.max(0.72, Math.min(1.28, multiplier));
    }

    private double weightedFitAdjustment(double metricScore, double weight, double totalWeight, double strength) {
        if (weight <= 0 || totalWeight <= 0) {
            return 0.0;
        }
        double share = weight / totalWeight;
        double centeredScore = (metricScore - 5.0) / 5.0;
        return centeredScore * share * strength;
    }

    private double calculatePersonalizedPenalties(Locality locality, UserPreferences preferences,
                                                  Map<String, Double> breakdown,
                                                  Map<String, Double> effectiveWeights) {
        double penalties = 0.0;
        penalties += priorityPenalty(getMetricScore(breakdown, "Safety"), effectiveWeights.getOrDefault("Safety", 0.0), effectiveWeights, 2.6);
        penalties += priorityPenalty(getMetricScore(breakdown, "Healthcare"), effectiveWeights.getOrDefault("Healthcare", 0.0), effectiveWeights, 1.9);
        penalties += priorityPenalty(getMetricScore(breakdown, "Transport"), effectiveWeights.getOrDefault("Transport", 0.0), effectiveWeights, 1.5);
        penalties += priorityPenalty(getMetricScore(breakdown, "Job Opportunities"), effectiveWeights.getOrDefault("Job Opportunities", 0.0), effectiveWeights, 1.8);
        penalties += priorityPenalty(getMetricScore(breakdown, "Cost of Living"), effectiveWeights.getOrDefault("Cost of Living", 0.0), effectiveWeights, 2.2);
        penalties += priorityPenalty(getMetricScore(breakdown, "Environment"), effectiveWeights.getOrDefault("Environment", 0.0), effectiveWeights, 1.4);

        if (preferences.getMinimumSafetyThreshold() > 0 && preferences.getSafetyWeight() > 0) {
            double activeThreshold = preferences.getMinimumSafetyThreshold()
                    * (0.35 + (preferences.getSafetyWeight() / 10.0) * 0.65);
            if (getMetricScore(breakdown, "Safety") < activeThreshold) {
                penalties += (activeThreshold - getMetricScore(breakdown, "Safety")) * 0.20 * (preferences.getSafetyWeight() / 10.0);
            }
        }

        if (preferences.getBudgetMatchThreshold() > 0 && preferences.getCostOfLivingWeight() > 0) {
            double affordabilityPercent = getMetricScore(breakdown, "Cost of Living") * 10.0;
            if (affordabilityPercent < preferences.getBudgetMatchThreshold()) {
                penalties += ((preferences.getBudgetMatchThreshold() - affordabilityPercent) / 100.0)
                        * 1.6 * (preferences.getCostOfLivingWeight() / 10.0);
            }
        }

        if (preferences.getFamilySize() >= 5) {
            double familyRisk = Math.max(0, 6.5 - getMetricScore(breakdown, "Healthcare"))
                    + Math.max(0, 6.5 - getMetricScore(breakdown, "Safety"))
                    + Math.max(0, 5.5 - getMetricScore(breakdown, "Cost of Living"));
            penalties += familyRisk * 0.10;
        }

        if (!"Remote".equalsIgnoreCase(preferences.getWorkType())) {
            penalties += Math.max(0, 5.0 - getMetricScore(breakdown, "Transport")) * 0.12
                    * (0.5 + preferences.getTransportWeight() / 10.0);
        }

        return Math.min(4.0, penalties);
    }

    private double priorityPenalty(double score, double weight, Map<String, Double> effectiveWeights, double maxPenalty) {
        double totalWeight = effectiveWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (score >= 5.0 || weight <= 0 || totalWeight <= 0) {
            return 0.0;
        }
        double weakness = (5.0 - score) / 5.0;
        double priorityShare = weight / totalWeight;
        return weakness * priorityShare * maxPenalty;
    }

    private double calculatePersonalizedBonuses(Locality locality, UserPreferences preferences,
                                                Map<String, Double> breakdown,
                                                Map<String, Double> effectiveWeights) {
        double bonuses = 0.0;
        bonuses += priorityBonus(getMetricScore(breakdown, "Safety"), effectiveWeights.getOrDefault("Safety", 0.0), effectiveWeights, 1.0);
        bonuses += priorityBonus(getMetricScore(breakdown, "Healthcare"), effectiveWeights.getOrDefault("Healthcare", 0.0), effectiveWeights, 0.9);
        bonuses += priorityBonus(getMetricScore(breakdown, "Job Opportunities"), effectiveWeights.getOrDefault("Job Opportunities", 0.0), effectiveWeights, 0.9);
        bonuses += priorityBonus(getMetricScore(breakdown, "Lifestyle"), effectiveWeights.getOrDefault("Lifestyle", 0.0), effectiveWeights, 0.8);
        bonuses += priorityBonus(getMetricScore(breakdown, "Cost of Living"), effectiveWeights.getOrDefault("Cost of Living", 0.0), effectiveWeights, 0.9);
        bonuses += priorityBonus(getMetricScore(breakdown, "Transport"), effectiveWeights.getOrDefault("Transport", 0.0), effectiveWeights, 0.7);

        if (preferences.getFamilySize() >= 4
                && effectiveWeights.getOrDefault("Healthcare", 0.0) > 0
                && effectiveWeights.getOrDefault("Safety", 0.0) > 0
                && getMetricScore(breakdown, "Healthcare") >= 8
                && getMetricScore(breakdown, "Safety") >= 8) {
            bonuses += 0.45;
        }
        if (preferences.getFamilySize() <= 1
                && effectiveWeights.getOrDefault("Job Opportunities", 0.0) > 0
                && effectiveWeights.getOrDefault("Lifestyle", 0.0) > 0
                && getMetricScore(breakdown, "Job Opportunities") >= 8
                && getMetricScore(breakdown, "Lifestyle") >= 8) {
            bonuses += 0.35;
        }
        if ("Remote".equalsIgnoreCase(preferences.getWorkType())
                && effectiveWeights.getOrDefault("Environment", 0.0) > 0
                && effectiveWeights.getOrDefault("Cost of Living", 0.0) > 0
                && getMetricScore(breakdown, "Environment") >= 8
                && getMetricScore(breakdown, "Cost of Living") >= 7) {
            bonuses += 0.30;
        }

        return Math.min(2.2, bonuses);
    }

    private double priorityBonus(double score, double weight, Map<String, Double> effectiveWeights, double maxBonus) {
        double totalWeight = effectiveWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (score <= 7.0 || weight <= 0 || totalWeight <= 0) {
            return 0.0;
        }
        double excellence = (score - 7.0) / 3.0;
        double priorityShare = weight / totalWeight;
        return excellence * priorityShare * maxBonus;
    }

    private double getMetricScore(Map<String, Double> breakdown, String key) {
        return breakdown.getOrDefault(key, 5.0);
    }

    private boolean passesBudgetConstraint(Locality locality, UserPreferences preferences) {
        if (preferences.getBudget() <= 0) {
            return true;
        }
        double annualRent = locality.getAvgRent() * 12;
        return annualRent / preferences.getBudget() <= 1.8;
    }

    /**
     * Step 7: Apply hybrid formula combining weighted sum, bonuses, and penalties.
     * 
     * Formula:
     *   finalScore = (baseScore * 0.7) + (bonuses * 0.2) - (penalties * 0.3)
     * 
     * Rationale:
     * - 70% weight on core metrics (most important)
     * - 20% bonus factor for exceptional combinations
     * - 30% penalty factor for real-world constraints
     *
     * @param baseScore Weighted metric score
     * @param bonuses   Total bonus amount
     * @param penalties Total penalty amount
     * @return Final score before clamping
     */
    private double applyHybridFormula(double baseScore, double bonuses, double penalties) {
        double weighted = baseScore * ScoringConfig.WEIGHTED_SUM_FACTOR;
        double bonusComponent = bonuses * ScoringConfig.BONUS_FACTOR;
        double penaltyComponent = penalties * ScoringConfig.PENALTY_FACTOR;

        return weighted + bonusComponent - penaltyComponent;
    }

    /**
     * Generate detailed, human-readable explanation for recommendation.
     * Includes top reasons, penalties applied, and bonuses earned.
     *
     * @param breakdown      Score breakdown by factor
     * @param preferences    User preferences
     * @param penaltyReasons Reasons for penalties
     * @param bonusReasons   Reasons for bonuses
     * @return Formatted explanation string
     */
    private String generateDetailedExplanation(Map<String, Double> breakdown, 
                                              UserPreferences preferences,
                                              List<String> penaltyReasons,
                                              List<String> bonusReasons) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Recommendation Summary:\n");
        
        // Key strengths
        explanation.append("\nStrengths:\n");
        List<String> strengths = new ArrayList<>();
        
        if (breakdown.containsKey("Job Opportunities") && breakdown.get("Job Opportunities") >= 7 && preferences.getJobWeight() > 0) {
            strengths.add("• Strong job market");
        }
        if (breakdown.containsKey("Cost of Living") && breakdown.get("Cost of Living") >= 7 && preferences.getCostOfLivingWeight() > 0) {
            strengths.add("• Affordable living costs");
        }
        if (breakdown.containsKey("Healthcare") && breakdown.get("Healthcare") >= 8 && preferences.getHealthcareWeight() > 0) {
            strengths.add("• Excellent healthcare facilities");
        }
        if (breakdown.containsKey("Safety") && breakdown.get("Safety") >= 8 && preferences.getSafetyWeight() > 0) {
            strengths.add("• Very safe neighborhood");
        }
        if (breakdown.containsKey("Environment") && breakdown.get("Environment") >= 7 && preferences.getEnvironmentWeight() > 0) {
            strengths.add("• Good air quality and environment");
        }
        if (breakdown.containsKey("Lifestyle") && breakdown.get("Lifestyle") >= 7 && preferences.getLifestyleWeight() > 0) {
            strengths.add("• Vibrant lifestyle and entertainment");
        }
        
        if (strengths.isEmpty()) {
            explanation.append("• Balanced profile with no critical weaknesses\n");
        } else {
            explanation.append(String.join("\n", strengths)).append("\n");
        }

        // Bonuses earned
        if (!bonusReasons.isEmpty()) {
            explanation.append("\nBonuses Earned:\n");
            for (String reason : bonusReasons) {
                explanation.append("✓ ").append(reason).append("\n");
            }
        }

        // Concerns/Penalties
        if (!penaltyReasons.isEmpty()) {
            explanation.append("\nConcerns:\n");
            for (String reason : penaltyReasons) {
                explanation.append("⚠ ").append(reason).append("\n");
            }
        }

        return explanation.toString();
    }
}
