package com.smartcity.model;

import com.smartcity.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a recommendation result for a locality.
 * Enhanced with explainability: includes score breakdown, penalties, and bonuses.
 */
public class RecommendationResult {
    private Locality locality;
    private double finalScore;
    private String explanation;
    private Map<String, Double> scoreBreakdown;
    private List<String> penaltyReasons;
    private List<String> bonusReasons;
    private int rank;
    private String dataSource;
    private String lastUpdated;
    private double confidenceScore;
    private Map<LiveMetricType, MetricSourceStatus> metricStatuses;
    
    // Scoring intermediate values (for explainability)
    private double baseScore;
    private double totalPenalties;
    private double totalBonuses;

    public RecommendationResult(Locality locality, double finalScore, String explanation) {
        this.locality = locality;
        this.finalScore = ValidationUtils.clampScore10(finalScore);
        this.explanation = explanation;
        this.scoreBreakdown = new HashMap<>();
        this.penaltyReasons = new ArrayList<>();
        this.bonusReasons = new ArrayList<>();
        this.dataSource = locality.getDataSource();
        this.lastUpdated = locality.getLastUpdated();
        this.confidenceScore = locality.getConfidenceScore();
        this.metricStatuses = locality.getMetricStatuses();
        this.baseScore = finalScore;
        this.totalPenalties = 0.0;
        this.totalBonuses = 0.0;
    }

    // Getters and Setters
    public Locality getLocality() {
        return locality;
    }

    public void setLocality(Locality locality) {
        this.locality = locality;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = ValidationUtils.clampScore10(finalScore);
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Map<String, Double> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(Map<String, Double> scoreBreakdown) {
        this.scoreBreakdown = new HashMap<>();
        if (scoreBreakdown == null) {
            return;
        }
        for (Map.Entry<String, Double> entry : scoreBreakdown.entrySet()) {
            this.scoreBreakdown.put(entry.getKey(), ValidationUtils.clampScore10(entry.getValue()));
        }
    }

    public void addScoreComponent(String component, double score) {
        this.scoreBreakdown.put(component, ValidationUtils.clampScore10(score));
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore));
    }

    public Map<LiveMetricType, MetricSourceStatus> getMetricStatuses() {
        Map<LiveMetricType, MetricSourceStatus> copy = new EnumMap<>(LiveMetricType.class);
        if (metricStatuses == null) {
            return copy;
        }
        for (Map.Entry<LiveMetricType, MetricSourceStatus> entry : metricStatuses.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public void setMetricStatuses(Map<LiveMetricType, MetricSourceStatus> metricStatuses) {
        this.metricStatuses = new EnumMap<>(LiveMetricType.class);
        if (metricStatuses == null) {
            return;
        }
        for (Map.Entry<LiveMetricType, MetricSourceStatus> entry : metricStatuses.entrySet()) {
            this.metricStatuses.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public List<String> getPenaltyReasons() {
        return penaltyReasons;
    }

    public void setPenaltyReasons(List<String> penaltyReasons) {
        this.penaltyReasons = penaltyReasons != null ? penaltyReasons : new ArrayList<>();
    }

    public void addPenaltyReason(String reason) {
        this.penaltyReasons.add(reason);
    }

    public List<String> getBonusReasons() {
        return bonusReasons;
    }

    public void setBonusReasons(List<String> bonusReasons) {
        this.bonusReasons = bonusReasons != null ? bonusReasons : new ArrayList<>();
    }

    public void addBonusReason(String reason) {
        this.bonusReasons.add(reason);
    }

    public double getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(double baseScore) {
        this.baseScore = ValidationUtils.clampScore10(baseScore);
    }

    public double getTotalPenalties() {
        return totalPenalties;
    }

    public void setTotalPenalties(double totalPenalties) {
        this.totalPenalties = totalPenalties;
    }

    public double getTotalBonuses() {
        return totalBonuses;
    }

    public void setTotalBonuses(double totalBonuses) {
        this.totalBonuses = totalBonuses;
    }

    @Override
    public String toString() {
        return String.format("%d. %s - Score: %.2f", rank, locality.getName(), finalScore);
    }
}
