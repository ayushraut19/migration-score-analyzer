package com.smartcity.model;

import java.time.Instant;

public class MetricSourceStatus {
    private String source;
    private String timestamp;
    private String message;

    public MetricSourceStatus(String source, String timestamp, String message) {
        this.source = source;
        this.timestamp = timestamp;
        this.message = message;
    }

    public static MetricSourceStatus live(String message) {
        return new MetricSourceStatus("LIVE", Instant.now().toString(), message);
    }

    public static MetricSourceStatus cached(String message) {
        return new MetricSourceStatus("CACHED", Instant.now().toString(), message);
    }

    public String getSource() {
        return source;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLive() {
        return "LIVE".equalsIgnoreCase(source);
    }

    public MetricSourceStatus copy() {
        return new MetricSourceStatus(source, timestamp, message);
    }
}
