package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScoreCalculator - Computes normalized recommendations and explanations.
 */
public class ScoreCalculator {

    public RecommendationResult calculateScore(Locality locality, UserPreferences preferences) {
        Map<String, Double> breakdown = new HashMap<>();

        double jobScore = normalizeMetric(locality.getJobIndex());
        double healthcareScore = normalizeMetric(locality.getHospitalRating());
        double transportScore = normalizeMetric(locality.getTransportScore());
        double safetyScore = normalizeMetric(locality.getSafetyScore());
        double environmentScore = normalizeMetric(10 - locality.getPollutionIndex());
        double lifestyleScore = normalizeMetric(locality.getLifestyleScore());
        double costOfLivingScore = calculateCostOfLivingScore(locality.getAvgRent(), preferences.getBudget());
        double budgetMatchScore = calculateBudgetMatchScore(locality.getAvgRent(), preferences.getBudget());

        if (preferences.getMinimumSafetyThreshold() > 0 && safetyScore < preferences.getMinimumSafetyThreshold()) {
            safetyScore = Math.max(0, safetyScore - 1.0);
        }

        double totalWeight = preferences.getJobWeight() + preferences.getCostOfLivingWeight() +
                preferences.getHealthcareWeight() + preferences.getTransportWeight() +
                preferences.getSafetyWeight() + preferences.getEnvironmentWeight() +
                preferences.getLifestyleWeight();

        if (totalWeight <= 0) {
            totalWeight = 1;
        }

        double jobWeight = preferences.getJobWeight() / totalWeight;
        double costWeight = preferences.getCostOfLivingWeight() / totalWeight;
        double healthWeight = preferences.getHealthcareWeight() / totalWeight;
        double transportWeight = preferences.getTransportWeight() / totalWeight;
        double safetyWeight = preferences.getSafetyWeight() / totalWeight;
        double environmentWeight = preferences.getEnvironmentWeight() / totalWeight;
        double lifestyleWeight = preferences.getLifestyleWeight() / totalWeight;

        double score = jobScore * jobWeight +
                costOfLivingScore * costWeight +
                healthcareScore * healthWeight +
                transportScore * transportWeight +
                safetyScore * safetyWeight +
                environmentScore * environmentWeight +
                lifestyleScore * lifestyleWeight;

        if (preferences.getBudgetMatchThreshold() > 0 && budgetMatchScore * 10.0 < preferences.getBudgetMatchThreshold()) {
            score -= 0.8;
        }

        score = Math.max(0, Math.min(10, score));

        breakdown.put("Job Opportunities", jobScore);
        breakdown.put("Cost of Living", costOfLivingScore);
        breakdown.put("Budget Match", budgetMatchScore);
        breakdown.put("Healthcare", healthcareScore);
        breakdown.put("Transport", transportScore);
        breakdown.put("Safety", safetyScore);
        breakdown.put("Environment", environmentScore);
        breakdown.put("Lifestyle", lifestyleScore);

        String explanation = buildExplanation(locality, preferences, jobScore, costOfLivingScore, budgetMatchScore,
                healthcareScore, safetyScore, lifestyleScore);

        RecommendationResult result = new RecommendationResult(locality, score, explanation);
        result.setScoreBreakdown(breakdown);
        result.setDataSource(locality.getDataSource());
        result.setLastUpdated(locality.getLastUpdated());
        result.setConfidenceScore(locality.getConfidenceScore());
        return result;
    }

    private double normalizeMetric(double value) {
        return Math.max(0, Math.min(10, value));
    }

    private double calculateCostOfLivingScore(double avgRent, double budget) {
        if (budget <= 0) {
            return 5.0;
        }
        double ratio = (avgRent * 12.0) / budget;
        if (ratio <= 0.3) {
            return 10.0;
        } else if (ratio <= 0.5) {
            return 8.0;
        } else if (ratio <= 0.7) {
            return 6.0;
        } else if (ratio <= 1.0) {
            return 4.0;
        }
        return 2.0;
    }

    private double calculateBudgetMatchScore(double avgRent, double budget) {
        if (budget <= 0) {
            return 5.0;
        }
        double ratio = Math.min(1.0, (avgRent * 12.0) / budget);
        return Math.max(0, 10.0 - ratio * 10.0);
    }

    private String buildExplanation(Locality locality,
                                    UserPreferences preferences,
                                    double jobScore,
                                    double costScore,
                                    double budgetMatchScore,
                                    double healthcareScore,
                                    double safetyScore,
                                    double lifestyleScore) {
        List<String> reasons = new ArrayList<>();
        if (preferences.getJobWeight() >= 7 && jobScore >= 8) {
            reasons.add("high job availability");
        }
        if (preferences.getCostOfLivingWeight() >= 7 && costScore >= 8) {
            reasons.add("strong affordability");
        }
        if (budgetMatchScore >= 8) {
            reasons.add("good budget fit");
        }
        if (preferences.getHealthcareWeight() >= 7 && healthcareScore >= 8) {
            reasons.add("excellent healthcare coverage");
        }
        if (preferences.getSafetyWeight() >= 7 && safetyScore >= 8) {
            reasons.add("high safety levels");
        }
        if (preferences.getLifestyleWeight() >= 7 && lifestyleScore >= 8) {
            reasons.add("vibrant lifestyle options");
        }

        if (reasons.isEmpty()) {
            reasons.add("balanced performance across your chosen priorities");
        }

        return "Best for you because " + String.join(" and ", reasons) + ".";
    }
}
