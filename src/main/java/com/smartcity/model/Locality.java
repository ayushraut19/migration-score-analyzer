package com.smartcity.model;

import com.smartcity.utils.ValidationUtils;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a locality with various metrics.
 * This is the core data model for the recommendation system.
 */
public class Locality {
    private String id;
    private String name;
    private String city;
    private String state;
    private double avgRent;
    private double jobIndex;
    private double hospitalRating;
    private double transportScore;
    private double safetyScore;
    private double pollutionIndex;
    private double lifestyleScore;
    private double populationDensity;
    private String description;
    private String dataSource = "Cached";
    private String lastUpdated = "N/A";
    private double confidenceScore = 0.5;
    private Map<LiveMetricType, MetricSourceStatus> metricStatuses = createDefaultMetricStatuses();

    // Constructor
    public Locality(String id, String name, String city, String state, double avgRent, 
                    double jobIndex, double hospitalRating, double transportScore,
                    double safetyScore, double pollutionIndex, double lifestyleScore,
                    double populationDensity, String description) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.state = state;
        this.avgRent = avgRent;
        this.jobIndex = ValidationUtils.clampScore10(jobIndex);
        this.hospitalRating = ValidationUtils.clampScore10(hospitalRating);
        this.transportScore = ValidationUtils.clampScore10(transportScore);
        this.safetyScore = ValidationUtils.clampScore10(safetyScore);
        this.pollutionIndex = ValidationUtils.clampScore10(pollutionIndex);
        this.lifestyleScore = ValidationUtils.clampScore10(lifestyleScore);
        this.populationDensity = populationDensity;
        this.description = description;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public double getAvgRent() {
        return avgRent;
    }

    public void setAvgRent(double avgRent) {
        this.avgRent = avgRent;
    }

    public double getJobIndex() {
        return jobIndex;
    }

    public void setJobIndex(double jobIndex) {
        this.jobIndex = ValidationUtils.clampScore10(jobIndex);
    }

    public double getHospitalRating() {
        return hospitalRating;
    }

    public void setHospitalRating(double hospitalRating) {
        this.hospitalRating = ValidationUtils.clampScore10(hospitalRating);
    }

    public double getTransportScore() {
        return transportScore;
    }

    public void setTransportScore(double transportScore) {
        this.transportScore = ValidationUtils.clampScore10(transportScore);
    }

    public double getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(double safetyScore) {
        this.safetyScore = ValidationUtils.clampScore10(safetyScore);
    }

    public double getPollutionIndex() {
        return pollutionIndex;
    }

    public void setPollutionIndex(double pollutionIndex) {
        this.pollutionIndex = ValidationUtils.clampScore10(pollutionIndex);
    }

    public double getLifestyleScore() {
        return lifestyleScore;
    }

    public void setLifestyleScore(double lifestyleScore) {
        this.lifestyleScore = ValidationUtils.clampScore10(lifestyleScore);
    }

    public double getPopulationDensity() {
        return populationDensity;
    }

    public void setPopulationDensity(double populationDensity) {
        this.populationDensity = populationDensity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        for (Map.Entry<LiveMetricType, MetricSourceStatus> entry : metricStatuses.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public void setMetricStatuses(Map<LiveMetricType, MetricSourceStatus> metricStatuses) {
        this.metricStatuses = createDefaultMetricStatuses();
        if (metricStatuses == null) {
            return;
        }
        for (Map.Entry<LiveMetricType, MetricSourceStatus> entry : metricStatuses.entrySet()) {
            this.metricStatuses.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public void setMetricStatus(LiveMetricType type, MetricSourceStatus status) {
        if (type != null && status != null) {
            metricStatuses.put(type, status.copy());
        }
    }

    private Map<LiveMetricType, MetricSourceStatus> createDefaultMetricStatuses() {
        Map<LiveMetricType, MetricSourceStatus> defaults = new EnumMap<>(LiveMetricType.class);
        for (LiveMetricType type : LiveMetricType.values()) {
            defaults.put(type, MetricSourceStatus.cached("Using stored fallback value."));
        }
        return defaults;
    }

    public Locality copy() {
        Locality copy = new Locality(id, name, city, state, avgRent, jobIndex, hospitalRating, transportScore,
                safetyScore, pollutionIndex, lifestyleScore, populationDensity, description);
        copy.setDataSource(dataSource);
        copy.setLastUpdated(lastUpdated);
        copy.setConfidenceScore(confidenceScore);
        copy.setMetricStatuses(metricStatuses);
        return copy;
    }

    @Override
    public String toString() {
        return name + ", " + city + ", " + state;
    }
}
