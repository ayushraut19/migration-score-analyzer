package com.smartcity.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartcity.model.Locality;
import com.smartcity.utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Retrieves live locality metrics from remote REST APIs using HttpURLConnection.
 * All parsing uses Gson and all remote work is executed off the Swing EDT.
 */
public class ApiService {

    private final boolean enableLiveApi;
    private final String rapidApiKey;
    private final String rapidApiHost;
    private final String numbeoApiKey;
    private final String googleApiKey;
    private static final long METRIC_CACHE_TTL_MS = 60L * 60L * 1000L; // 1 hour

    private final ExecutorService executorService;
    private final Map<String, Coordinates> coordinateCache;
    private final Map<String, CachedMetrics> metricsCache;

    public ApiService() {
        this.enableLiveApi = Boolean.parseBoolean(
                ConfigLoader.getProperty("feature.realTimeUpdate", "true"));
        this.rapidApiKey = ConfigLoader.getProperty("api.rapid.key", "");
        this.rapidApiHost = ConfigLoader.getProperty("api.rapid.host", "jsearch.p.rapidapi.com");
        this.numbeoApiKey = ConfigLoader.getProperty("api.numbeo.key", "");
        this.googleApiKey = ConfigLoader.getProperty("api.google.key", "");
        this.executorService = Executors.newFixedThreadPool(6);
        this.coordinateCache = new ConcurrentHashMap<>();
        this.metricsCache = new ConcurrentHashMap<>();
    }

    public static class RawLocalityMetrics {
        public double jobCount = -1;
        public double costIndex = -1;
        public double aqi = -1;
        public double transportDuration = -1;
        public double hospitalCount = -1;
        public String source = "Cached";
        public String timestamp = Instant.now().toString();
        public double confidenceScore = 0.50;
        public String statusMessage = "Using cached data.";
    }

    private static final class Coordinates {
        private final double latitude;
        private final double longitude;

        private Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final class CachedMetrics {
        private RawLocalityMetrics metrics;
        private long expiresAt;
    }

    public CompletableFuture<RawLocalityMetrics> fetchMetrics(Locality locality) {
        if (!enableLiveApi) {
            return CompletableFuture.completedFuture(createCachedMetrics("Live API disabled in configuration."));
        }

        String cacheKey = locality.getId();
        CachedMetrics cached = metricsCache.get(cacheKey);
        if (cached != null && cached.expiresAt >= System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(cached.metrics);
        }

        CompletableFuture<Coordinates> coordinatesFuture = fetchCoordinates(locality)
                .exceptionally(ex -> null);

        CompletableFuture<Double> jobFuture = fetchJobCount(locality).exceptionally(ex -> -1.0);
        CompletableFuture<Double> costFuture = fetchCostOfLivingIndex(locality).exceptionally(ex -> -1.0);
        CompletableFuture<Double> pollutionFuture = coordinatesFuture.thenCompose(coords -> fetchAirQualityIndex(coords))
                .exceptionally(ex -> -1.0);
        CompletableFuture<Double> transportFuture = coordinatesFuture.thenCompose(coords -> fetchTransportDuration(coords))
                .exceptionally(ex -> -1.0);
        CompletableFuture<Double> healthcareFuture = coordinatesFuture.thenCompose(coords -> fetchHealthcareCount(coords))
                .exceptionally(ex -> -1.0);

        return CompletableFuture.allOf(jobFuture, costFuture, pollutionFuture, transportFuture, healthcareFuture)
                .thenApply(ignored -> {
                    RawLocalityMetrics metrics = buildMetrics(
                            jobFuture.join(),
                            costFuture.join(),
                            pollutionFuture.join(),
                            transportFuture.join(),
                            healthcareFuture.join());
                    if ("Live".equals(metrics.source)) {
                        cacheMetrics(cacheKey, metrics);
                    }
                    return metrics;
                })
                .exceptionally(ex -> createCachedMetrics("Unable to fetch live metrics right now."));
    }

    private RawLocalityMetrics buildMetrics(double jobCount,
                                            double costIndex,
                                            double aqi,
                                            double transportDuration,
                                            double hospitalCount) {
        RawLocalityMetrics metrics = new RawLocalityMetrics();
        metrics.jobCount = jobCount;
        metrics.costIndex = costIndex;
        metrics.aqi = aqi;
        metrics.transportDuration = transportDuration;
        metrics.hospitalCount = hospitalCount;
        metrics.timestamp = Instant.now().toString();

        boolean hasLiveData = jobCount >= 0 || costIndex >= 0 || aqi >= 0
                || transportDuration >= 0 || hospitalCount >= 0;
        metrics.source = hasLiveData ? "Live" : "Cached";
        metrics.confidenceScore = calculateConfidence(jobCount, costIndex, aqi, transportDuration, hospitalCount);
        metrics.statusMessage = hasLiveData
                ? "Live API metrics loaded successfully."
                : "API request failed. Cached metrics will be used.";
        return metrics;
    }

    private RawLocalityMetrics createCachedMetrics(String statusMessage) {
        RawLocalityMetrics metrics = new RawLocalityMetrics();
        metrics.source = "Cached";
        metrics.confidenceScore = 0.45;
        metrics.timestamp = Instant.now().toString();
        metrics.statusMessage = statusMessage;
        return metrics;
    }

    private double calculateConfidence(double jobCount, double costIndex, double aqi,
                                       double transportDuration, double hospitalCount) {
        int liveCount = 0;
        liveCount += jobCount >= 0 ? 1 : 0;
        liveCount += costIndex >= 0 ? 1 : 0;
        liveCount += aqi >= 0 ? 1 : 0;
        liveCount += transportDuration >= 0 ? 1 : 0;
        liveCount += hospitalCount >= 0 ? 1 : 0;
        return 0.40 + (liveCount / 5.0) * 0.60;
    }

    private CompletableFuture<Coordinates> fetchCoordinates(Locality locality) {
        String cacheKey = locality.getId();
        Coordinates cached = coordinateCache.get(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = URLEncoder.encode(locality.getName() + ", " + locality.getCity() + ", " + locality.getState() + ", India", StandardCharsets.UTF_8);
                String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + query;
                JsonArray array = getJsonArray(url, Map.of("User-Agent", "SmartCityRecommendationSystem/1.0"));
                if (array.size() == 0) {
                    // Fallback to city-level coordinates when exact locality lookup fails.
                    query = URLEncoder.encode(locality.getCity() + ", " + locality.getState() + ", India", StandardCharsets.UTF_8);
                    url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + query;
                    array = getJsonArray(url, Map.of("User-Agent", "SmartCityRecommendationSystem/1.0"));
                }
                if (array.size() == 0) {
                    throw new IOException("No coordinates returned for " + locality.getName());
                }

                JsonObject item = array.get(0).getAsJsonObject();
                Coordinates coordinates = new Coordinates(
                        item.get("lat").getAsDouble(),
                        item.get("lon").getAsDouble());
                coordinateCache.put(cacheKey, coordinates);
                return coordinates;
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executorService);
    }

    private void cacheMetrics(String key, RawLocalityMetrics metrics) {
        CachedMetrics cached = new CachedMetrics();
        cached.metrics = metrics;
        cached.expiresAt = System.currentTimeMillis() + METRIC_CACHE_TTL_MS;
        metricsCache.put(key, cached);
    }

    private CompletableFuture<Double> fetchJobCount(Locality locality) {
        if (rapidApiKey.isBlank()) {
            return CompletableFuture.completedFuture(-1.0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = URLEncoder.encode(locality.getCity() + " jobs", StandardCharsets.UTF_8);
                String url = String.format("%s?query=%s&page=1",
                        ConfigLoader.getProperty("api.jobs.url", "https://jsearch.p.rapidapi.com/search"),
                        query);
                JsonObject json = getJsonObject(url, Map.of(
                        "X-RapidAPI-Key", rapidApiKey,
                        "X-RapidAPI-Host", rapidApiHost));
                JsonArray data = json.has("data") && json.get("data").isJsonArray()
                        ? json.getAsJsonArray("data")
                        : new JsonArray();
                return (double) data.size();
            } catch (Exception ex) {
                return -1.0;
            }
        }, executorService);
    }

    private CompletableFuture<Double> fetchCostOfLivingIndex(Locality locality) {
        if (numbeoApiKey.isBlank()) {
            return CompletableFuture.completedFuture(-1.0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String city = URLEncoder.encode(locality.getCity(), StandardCharsets.UTF_8);
                String url = String.format("%s?api_key=%s&city=%s",
                        ConfigLoader.getProperty("api.numbeo.url", "https://www.numbeo.com/api/city_prices"),
                        numbeoApiKey,
                        city);
                JsonObject json = getJsonObject(url, Map.of());
                return getOptionalDouble(json, "cost_of_living_index", -1.0);
            } catch (Exception ex) {
                return -1.0;
            }
        }, executorService);
    }

    private CompletableFuture<Double> fetchAirQualityIndex(Coordinates coordinates) {
        if (coordinates == null) {
            return CompletableFuture.completedFuture(-1.0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(
                        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%s&longitude=%s&current=pm2_5",
                        coordinates.latitude,
                        coordinates.longitude);
                JsonObject json = getJsonObject(url, Map.of());
                JsonObject current = json.has("current") ? json.getAsJsonObject("current") : null;
                return current != null ? getOptionalDouble(current, "pm2_5", -1.0) : -1.0;
            } catch (Exception ex) {
                return -1.0;
            }
        }, executorService);
    }

    private CompletableFuture<Double> fetchTransportDuration(Coordinates coordinates) {
        if (coordinates == null || googleApiKey.isBlank()) {
            return CompletableFuture.completedFuture(-1.0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String origin = coordinates.latitude + "," + coordinates.longitude;
                String destination = coordinates.latitude + "," + (coordinates.longitude + 0.05);
                String url = String.format(
                        "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s&destinations=%s&key=%s",
                        URLEncoder.encode(origin, StandardCharsets.UTF_8),
                        URLEncoder.encode(destination, StandardCharsets.UTF_8),
                        googleApiKey);
                JsonObject json = getJsonObject(url, Map.of());
                JsonArray rows = json.has("rows") ? json.getAsJsonArray("rows") : new JsonArray();
                if (rows.size() == 0) {
                    return -1.0;
                }

                JsonObject row = rows.get(0).getAsJsonObject();
                JsonArray elements = row.has("elements") ? row.getAsJsonArray("elements") : new JsonArray();
                if (elements.size() == 0) {
                    return -1.0;
                }

                JsonObject duration = elements.get(0).getAsJsonObject().getAsJsonObject("duration");
                return getOptionalDouble(duration, "value", -1.0);
            } catch (Exception ex) {
                return -1.0;
            }
        }, executorService);
    }

    private CompletableFuture<Double> fetchHealthcareCount(Coordinates coordinates) {
        if (coordinates == null) {
            return CompletableFuture.completedFuture(-1.0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = String.format(
                        "[out:json][timeout:15];node(around:5000,%s,%s)[amenity=hospital];out count;",
                        coordinates.latitude,
                        coordinates.longitude);
                String url = "https://overpass-api.de/api/interpreter?data="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8);
                JsonObject json = getJsonObject(url, Map.of("User-Agent", "SmartCityRecommendationSystem/1.0"));
                JsonArray elements = json.has("elements") ? json.getAsJsonArray("elements") : new JsonArray();
                if (elements.size() == 0) {
                    return -1.0;
                }

                JsonObject countNode = elements.get(0).getAsJsonObject();
                JsonElement tags = countNode.get("tags");
                if (tags != null && tags.isJsonObject() && tags.getAsJsonObject().has("total")) {
                    return tags.getAsJsonObject().get("total").getAsDouble();
                }
                return elements.size() - 1.0;
            } catch (Exception ex) {
                return -1.0;
            }
        }, executorService);
    }

    private JsonObject getJsonObject(String url, Map<String, String> headers) throws IOException {
        String response = executeGet(url, headers);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    private JsonArray getJsonArray(String url, Map<String, String> headers) throws IOException {
        String response = executeGet(url, headers);
        return JsonParser.parseString(response).getAsJsonArray();
    }

    private String executeGet(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = readBody(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " for " + url + ": " + body);
            }
            return body;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }

    private double getOptionalDouble(JsonObject object, String member, double fallback) {
        if (object == null || !object.has(member)) {
            return fallback;
        }

        try {
            return object.get(member).getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }
}
