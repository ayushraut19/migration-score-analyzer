package com.smartcity.service;

import com.smartcity.model.LiveMetricType;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class APIHealthManager {
    private final Map<LiveMetricType, ApiHealthStatus> statuses;

    public APIHealthManager() {
        statuses = new EnumMap<>(LiveMetricType.class);
        for (LiveMetricType type : LiveMetricType.values()) {
            statuses.put(type, new ApiHealthStatus(type));
        }
    }

    public void recordSuccess(LiveMetricType type, long responseTimeMs) {
        statuses.get(type).recordSuccess(responseTimeMs);
    }

    public void recordFailure(LiveMetricType type, long responseTimeMs) {
        recordFailure(type, responseTimeMs, "Unknown failure");
    }

    public void recordFailure(LiveMetricType type, long responseTimeMs, String reason) {
        statuses.get(type).recordFailure(responseTimeMs, reason);
    }

    public void reset() {
        for (ApiHealthStatus status : statuses.values()) {
            status.reset();
        }
    }

    public Map<LiveMetricType, ApiHealthSnapshot> snapshot() {
        Map<LiveMetricType, ApiHealthSnapshot> copy = new EnumMap<>(LiveMetricType.class);
        for (Map.Entry<LiveMetricType, ApiHealthStatus> entry : statuses.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().snapshot());
        }
        return copy;
    }

    private static final class ApiHealthStatus {
        private final LiveMetricType type;
        private final AtomicInteger successCount = new AtomicInteger();
        private final AtomicInteger failureCount = new AtomicInteger();
        private final AtomicLong totalResponseTimeMs = new AtomicLong();
        private volatile String lastSuccessfulCall = "N/A";
        private volatile String lastFailureReason = "N/A";

        private ApiHealthStatus(LiveMetricType type) {
            this.type = type;
        }

        private void recordSuccess(long responseTimeMs) {
            successCount.incrementAndGet();
            totalResponseTimeMs.addAndGet(Math.max(0, responseTimeMs));
            lastSuccessfulCall = Instant.now().toString();
            lastFailureReason = "N/A";
        }

        private void recordFailure(long responseTimeMs, String reason) {
            failureCount.incrementAndGet();
            totalResponseTimeMs.addAndGet(Math.max(0, responseTimeMs));
            lastFailureReason = reason == null || reason.isBlank() ? "Unknown failure" : reason;
        }

        private ApiHealthSnapshot snapshot() {
            int successes = successCount.get();
            int failures = failureCount.get();
            int total = successes + failures;
            double successRate = total == 0 ? 0.0 : successes / (double) total;
            long average = total == 0 ? 0 : totalResponseTimeMs.get() / total;
            return new ApiHealthSnapshot(type, successRate, lastSuccessfulCall, average, failures, lastFailureReason);
        }

        private void reset() {
            successCount.set(0);
            failureCount.set(0);
            totalResponseTimeMs.set(0);
            lastSuccessfulCall = "N/A";
            lastFailureReason = "N/A";
        }
    }

    public static final class ApiHealthSnapshot {
        private final LiveMetricType type;
        private final double successRate;
        private final String lastSuccessfulCall;
        private final long averageResponseTimeMs;
        private final int failureCount;
        private final String lastFailureReason;

        private ApiHealthSnapshot(LiveMetricType type, double successRate, String lastSuccessfulCall,
                                  long averageResponseTimeMs, int failureCount, String lastFailureReason) {
            this.type = type;
            this.successRate = successRate;
            this.lastSuccessfulCall = lastSuccessfulCall;
            this.averageResponseTimeMs = averageResponseTimeMs;
            this.failureCount = failureCount;
            this.lastFailureReason = lastFailureReason;
        }

        public LiveMetricType getType() {
            return type;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public String getLastSuccessfulCall() {
            return lastSuccessfulCall;
        }

        public long getAverageResponseTimeMs() {
            return averageResponseTimeMs;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public String getLastFailureReason() {
            return lastFailureReason;
        }
    }
}
