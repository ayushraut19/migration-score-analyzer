package com.smartcity.service;

import com.smartcity.model.LiveMetricType;
import com.smartcity.model.Locality;

import java.util.concurrent.CompletableFuture;

/**
 * DataAggregator - Enriches locality data with live API metrics and normalizes values.
 */
public class DataAggregator {

    private static final double LOCALITY_WEIGHT = 0.70;
    private static final double LIVE_WEIGHT = 0.30;

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
            double original = locality.getJobIndex();
            double liveScore = normalizeJobCount(metrics.jobCount.getValue(), original);
            double blended = blendMetric(original, liveScore);
            logMetricDebug(locality, "Jobs", original, metrics.jobCount.getValue(), liveScore, blended);
            locality.setJobIndex(blended);
        }
        if (metrics.hospitalCount.isAvailable()) {
            double original = locality.getHospitalRating();
            double liveScore = normalizeHealthcareCount(metrics.hospitalCount.getValue(), original);
            double blended = blendMetric(original, liveScore);
            logMetricDebug(locality, "Healthcare", original, metrics.hospitalCount.getValue(), liveScore, blended);
            locality.setHospitalRating(blended);
        }
        if (metrics.transportDuration.isAvailable()) {
            double original = locality.getTransportScore();
            double liveScore = normalizeTransportDuration(metrics.transportDuration.getValue(), original);
            double blended = blendMetric(original, liveScore);
            logMetricDebug(locality, "Transport", original, metrics.transportDuration.getValue(), liveScore, blended);
            locality.setTransportScore(blended);
        }
        if (metrics.aqi.isAvailable()) {
            double original = locality.getPollutionIndex();
            double liveScore = normalizeAirQuality(metrics.aqi.getValue(), original);
            double blended = blendMetric(original, liveScore);
            logMetricDebug(locality, "AQI", original, metrics.aqi.getValue(), liveScore, blended);
            locality.setPollutionIndex(blended);
        }

        if (locality.getLifestyleScore() <= 0) {
            locality.setLifestyleScore(6.0);
        }

        locality.setDataSource(metrics.source);
        locality.setLastUpdated(metrics.timestamp);
        locality.setConfidenceScore(metrics.confidenceScore);
        locality.setMetricStatuses(metrics.metricStatuses);

        if (metrics.costIndex.isAvailable()) {
            double original = locality.getAvgRent();
            double liveRent = normalizeCostIndex(metrics.costIndex.getValue(), original);
            double blended = blendMetric(original, liveRent);
            logMetricDebug(locality, "Cost", original, metrics.costIndex.getValue(), liveRent, blended);
            locality.setAvgRent(blended);
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

    static double blendMetric(double localityMetric, double liveMetric) {
        if (liveMetric < 0) {
            return localityMetric;
        }
        return (localityMetric * LOCALITY_WEIGHT) + (liveMetric * LIVE_WEIGHT);
    }

    private void logMetricDebug(Locality locality, String metricName, double localityMetric,
                                double rawApiValue, double normalizedApiValue, double finalValue) {
        System.out.println("[LocalityDebug] " + locality.getName()
                + " | " + metricName
                + " | JSON=" + String.format("%.2f", localityMetric)
                + " | RawAPI=" + String.format("%.2f", rawApiValue)
                + " | NormalizedAPI=" + String.format("%.2f", normalizedApiValue)
                + " | Final=" + String.format("%.2f", finalValue));
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
