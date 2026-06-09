package com.smartcity.service;

import com.smartcity.model.LiveMetricType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private final Map<String, CachedMetric> cache = new ConcurrentHashMap<>();

    public void put(String localityId, LiveMetricType type, double value, long ttlMs) {
        cache.put(key(localityId, type), new CachedMetric(value, System.currentTimeMillis() + ttlMs));
    }

    public Double getFresh(String localityId, LiveMetricType type) {
        CachedMetric cached = cache.get(key(localityId, type));
        if (cached == null || cached.expiresAt < System.currentTimeMillis()) {
            return null;
        }
        return cached.value;
    }

    private String key(String localityId, LiveMetricType type) {
        return localityId + ":" + type.name();
    }

    private static final class CachedMetric {
        private final double value;
        private final long expiresAt;

        private CachedMetric(double value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
