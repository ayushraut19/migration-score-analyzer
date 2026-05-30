package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.utils.ConfigLoader;

import java.util.concurrent.CompletableFuture;

/**
 * DataAggregator - Enriches locality data with live API metrics and normalizes values.
 */
public class DataAggregator {

    private final ApiService apiService;

    public DataAggregator() {
        this.apiService = new ApiService();
    }

    public CompletableFuture<Locality> enrichLocality(Locality locality) {
        Locality enriched = locality.copy();
        return apiService.fetchMetrics(locality)
                .handle((metrics, throwable) -> {
                    if (throwable != null || metrics == null || "Cached" .equals(metrics.source)) {
                        enriched.setDataSource("Cached");
                        enriched.setLastUpdated(metrics != null ? metrics.timestamp : "N/A");
                        enriched.setConfidenceScore(metrics != null ? metrics.confidenceScore : 0.50);
                        return enriched;
                    }

                    applyLiveMetrics(enriched, metrics);
                    return enriched;
                });
    }

    private void applyLiveMetrics(Locality locality, ApiService.RawLocalityMetrics metrics) {
        locality.setJobIndex(normalizeJobCount(metrics.jobCount, locality.getJobIndex()));
        locality.setHospitalRating(normalizeHealthcareCount(metrics.hospitalCount, locality.getHospitalRating()));
        locality.setTransportScore(normalizeTransportDuration(metrics.transportDuration, locality.getTransportScore()));
        locality.setPollutionIndex(normalizeAirQuality(metrics.aqi, locality.getPollutionIndex()));

        // Keep existing lifestyle score as a baseline
        if (locality.getLifestyleScore() <= 0) {
            locality.setLifestyleScore(6.0);
        }

        locality.setDataSource(metrics.source);
        locality.setLastUpdated(metrics.timestamp);
        locality.setConfidenceScore(metrics.confidenceScore);

        if (metrics.costIndex >= 0) {
            locality.setAvgRent(normalizeCostIndex(metrics.costIndex, locality.getAvgRent()));
        }
    }

    private double normalizeJobCount(double jobCount, double fallback) {
        if (jobCount < 0) {
            return fallback;
        }
        double score = Math.min(10, jobCount / 20.0);
        return Math.max(0, score);
    }

    private double normalizeHealthcareCount(double hospitalCount, double fallback) {
        if (hospitalCount < 0) {
            return fallback;
        }
        double score = Math.min(10, (hospitalCount / 3.0) * 2.0);
        return Math.max(0, score);
    }

    private double normalizeTransportDuration(double durationInSeconds, double fallback) {
        if (durationInSeconds < 0) {
            return fallback;
        }
        double durationMinutes = durationInSeconds / 60.0;
        double score = 10.0 - Math.min(8.0, durationMinutes / 6.0);
        return Math.max(0, Math.min(10, score));
    }

    private double normalizeAirQuality(double aqi, double fallback) {
        if (aqi < 0) {
            return fallback;
        }
        // Map PM2.5-style air quality values into a 0-10 comfort score.
        double score = 10.0 - (aqi / 50.0);
        return Math.max(0, Math.min(10, score));
    }

    private double normalizeCostIndex(double costIndex, double fallbackRent) {
        if (costIndex <= 0) {
            return fallbackRent;
        }
        double normalized = fallbackRent;
        if (costIndex > 0) {
            normalized = Math.max(8000, Math.min(60000, costIndex * 500));
        }
        return normalized;
    }
}
