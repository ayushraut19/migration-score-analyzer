package com.smartcity.service;

import com.smartcity.model.LiveMetricType;
import com.smartcity.model.Locality;

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
                    if (throwable != null || metrics == null) {
                        enriched.setDataSource("Cached");
                        enriched.setLastUpdated("N/A");
                        enriched.setConfidenceScore(0.35);
                        return enriched;
                    }

                    applyLiveMetrics(enriched, metrics);
                    return enriched;
                });
    }

    private void applyLiveMetrics(Locality locality, ApiService.RawLocalityMetrics metrics) {
        if (metrics.jobCount.isAvailable()) {
            locality.setJobIndex(normalizeJobCount(metrics.jobCount.getValue(), locality.getJobIndex()));
        }
        if (metrics.hospitalCount.isAvailable()) {
            locality.setHospitalRating(normalizeHealthcareCount(metrics.hospitalCount.getValue(), locality.getHospitalRating()));
        }
        if (metrics.transportDuration.isAvailable()) {
            locality.setTransportScore(normalizeTransportDuration(metrics.transportDuration.getValue(), locality.getTransportScore()));
        }
        if (metrics.aqi.isAvailable()) {
            locality.setPollutionIndex(normalizeAirQuality(metrics.aqi.getValue(), locality.getPollutionIndex()));
        }

        if (locality.getLifestyleScore() <= 0) {
            locality.setLifestyleScore(6.0);
        }

        locality.setDataSource(metrics.source);
        locality.setLastUpdated(metrics.timestamp);
        locality.setConfidenceScore(metrics.confidenceScore);
        locality.setMetricStatuses(metrics.metricStatuses);

        if (metrics.costIndex.isAvailable()) {
            locality.setAvgRent(normalizeCostIndex(metrics.costIndex.getValue(), locality.getAvgRent()));
        }

        applyFallbackStatusForUnavailable(locality, metrics);
    }

    private void applyFallbackStatusForUnavailable(Locality locality, ApiService.RawLocalityMetrics metrics) {
        if (!metrics.jobCount.isAvailable()) {
            locality.setMetricStatus(LiveMetricType.JOBS, metrics.jobCount.getStatus());
        }
        if (!metrics.costIndex.isAvailable()) {
            locality.setMetricStatus(LiveMetricType.COST_OF_LIVING, metrics.costIndex.getStatus());
        }
        if (!metrics.aqi.isAvailable()) {
            locality.setMetricStatus(LiveMetricType.AQI, metrics.aqi.getStatus());
        }
        if (!metrics.transportDuration.isAvailable()) {
            locality.setMetricStatus(LiveMetricType.TRANSPORT, metrics.transportDuration.getStatus());
        }
        if (!metrics.hospitalCount.isAvailable()) {
            locality.setMetricStatus(LiveMetricType.HEALTHCARE, metrics.hospitalCount.getStatus());
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
