package com.smartcity.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.smartcity.model.LiveMetricType;
import com.smartcity.model.Locality;
import com.smartcity.model.MetricSourceStatus;
import com.smartcity.utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ApiService {
    private static final int MAX_ATTEMPTS = 1;
    private static final long BASE_BACKOFF_MS = 150;
    private static final int TIMEOUT_MS = 2_000;

    private static final long AQI_TTL_MS = 30L * 60L * 1000L;
    private static final long TRANSPORT_TTL_MS = 60L * 60L * 1000L;
    private static final long JOBS_TTL_MS = 6L * 60L * 60L * 1000L;
    private static final long HEALTHCARE_TTL_MS = 24L * 60L * 60L * 1000L;
    private static final long COST_TTL_MS = 7L * 24L * 60L * 60L * 1000L;

    private static final APIHealthManager HEALTH_MANAGER = new APIHealthManager();
    private static final CacheManager CACHE_MANAGER = new CacheManager();
    private static final Map<String, Double> COST_OF_LIVING_INDEX = loadCostOfLivingIndex();
    private static final Map<String, Coordinates> CITY_COORDINATES = createCityCoordinates();
    private static final boolean DIAGNOSTICS_ENABLED = Boolean.parseBoolean(
            ConfigLoader.getProperty("api.diagnostics.enabled", "true"));

    private final boolean enableLiveApi;
    private final String adzunaAppId;
    private final String adzunaAppKey;
    private final String openRouteServiceApiKey;
    private final String geoapifyApiKey;
    private final ExecutorService executorService;
    private final Map<String, Coordinates> coordinateCache;
    private final Map<String, CompletableFuture<MetricValue>> inFlightMetrics;

    public ApiService() {
        this.enableLiveApi = Boolean.parseBoolean(ConfigLoader.getProperty("feature.realTimeUpdate", "true"));
        this.adzunaAppId = ConfigLoader.getProperty("api.adzuna.appId", "ac977b39");
        this.adzunaAppKey = ConfigLoader.getProperty("api.adzuna.appKey", "508aa94491ed1b392ed831e6e031c1e1");
        this.openRouteServiceApiKey = ConfigLoader.getProperty("api.openrouteservice.key", "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImZmNTk5YTE3ZDgwNTQyNDk4NDYxZmFkMmUxNmMyZTA1IiwiaCI6Im11cm11cjY0In0=");
        this.geoapifyApiKey = ConfigLoader.getProperty("api.geoapify.key", "757d27891d774ca9adaee40a8fc6067e");
        this.executorService = Executors.newFixedThreadPool(8);
        this.coordinateCache = new ConcurrentHashMap<>();
        this.inFlightMetrics = new ConcurrentHashMap<>();
        logConstructorDiagnostics();
    }

    public static APIHealthManager getHealthManager() {
        return HEALTH_MANAGER;
    }

    public static class RawLocalityMetrics {
        public MetricValue jobCount;
        public MetricValue costIndex;
        public MetricValue aqi;
        public MetricValue transportDuration;
        public MetricValue hospitalCount;
        public String source = "Cached";
        public String timestamp = Instant.now().toString();
        public double confidenceScore = 0.50;
        public String statusMessage = "Using cached data.";
        public Map<LiveMetricType, MetricSourceStatus> metricStatuses = new EnumMap<>(LiveMetricType.class);

        private RawLocalityMetrics() {
            for (LiveMetricType type : LiveMetricType.values()) {
                metricStatuses.put(type, MetricSourceStatus.cached("Using stored fallback value."));
            }
        }
    }

    public static final class MetricValue {
        private final double value;
        private final boolean available;
        private final boolean live;
        private final boolean trusted;
        private final MetricSourceStatus status;

        private MetricValue(double value, boolean available, boolean live, boolean trusted, MetricSourceStatus status) {
            this.value = value;
            this.available = available;
            this.live = live;
            this.trusted = trusted;
            this.status = status;
        }

        public static MetricValue live(double value) {
            return new MetricValue(value, true, true, true, MetricSourceStatus.live("Live metric loaded successfully."));
        }

        public static MetricValue cached(double value, String message) {
            return new MetricValue(value, true, false, false, MetricSourceStatus.cached(message));
        }

        public static MetricValue local(double value, String message) {
            return new MetricValue(value, true, false, true, new MetricSourceStatus("LOCAL", Instant.now().toString(), message));
        }

        public static MetricValue unavailable(String message) {
            return new MetricValue(-1.0, false, false, false, MetricSourceStatus.cached(message));
        }

        public double getValue() {
            return value;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isLive() {
            return live;
        }

        public boolean isTrusted() {
            return trusted;
        }

        public MetricSourceStatus getStatus() {
            return status;
        }
    }

    private static final class Coordinates {
        private final double latitude;
        private final double longitude;

        private Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public CompletableFuture<RawLocalityMetrics> fetchMetrics(Locality locality) {
        if (!enableLiveApi) {
            return CompletableFuture.completedFuture(createDisabledMetrics());
        }

        CompletableFuture<Coordinates> coordinatesFuture = fetchCoordinates(locality).exceptionally(ex -> null);
        CompletableFuture<MetricValue> jobFuture = fetchMetric(locality, LiveMetricType.JOBS, JOBS_TTL_MS,
                () -> fetchJobCountValue(locality));
        CompletableFuture<MetricValue> costFuture = CompletableFuture.completedFuture(fetchCostOfLivingValue(locality));
        CompletableFuture<MetricValue> aqiFuture = coordinatesFuture.thenCompose(coords -> fetchMetric(locality,
                LiveMetricType.AQI, AQI_TTL_MS, () -> fetchAirQualityValue(coords)));
        CompletableFuture<MetricValue> transportFuture = coordinatesFuture.thenCompose(coords -> fetchMetric(locality,
                LiveMetricType.TRANSPORT, TRANSPORT_TTL_MS, () -> fetchTransportValue(coords)));
        CompletableFuture<MetricValue> healthcareFuture = coordinatesFuture.thenCompose(coords -> fetchMetric(locality,
                LiveMetricType.HEALTHCARE, HEALTHCARE_TTL_MS, () -> fetchHealthcareValue(coords)));

        return CompletableFuture.allOf(jobFuture, costFuture, aqiFuture, transportFuture, healthcareFuture)
                .thenApply(ignored -> buildMetrics(jobFuture.join(), costFuture.join(), aqiFuture.join(),
                        transportFuture.join(), healthcareFuture.join()))
                .exceptionally(ex -> createDisabledMetrics());
    }

    private CompletableFuture<MetricValue> fetchMetric(Locality locality, LiveMetricType type, long ttlMs,
                                                       Supplier<Double> supplier) {
        String cacheKey = metricCacheKey(locality, type);
        Double cached = CACHE_MANAGER.getFresh(cacheKey, type);
        if (cached != null) {
            return CompletableFuture.completedFuture(
                    MetricValue.cached(cached, "Using fresh cached live value."));
        }

        String inFlightKey = cacheKey + ":" + type.name();
        return inFlightMetrics.computeIfAbsent(inFlightKey, ignored -> CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                double value = withRetry(type, supplier);
                CACHE_MANAGER.put(cacheKey, type, value, ttlMs);
                HEALTH_MANAGER.recordSuccess(type, System.currentTimeMillis() - start);
                MetricValue metricValue = MetricValue.live(value);
                logMetricResult(type, metricValue, System.currentTimeMillis() - start);
                return metricValue;
            } catch (Exception ex) {
                String reason = failureReason(ex);
                HEALTH_MANAGER.recordFailure(type, System.currentTimeMillis() - start, reason);
                if (cached != null) {
                    MetricValue metricValue = MetricValue.cached(cached, reason + "; using fresh cached live value.");
                    logMetricResult(type, metricValue, System.currentTimeMillis() - start);
                    return metricValue;
                }
                MetricValue metricValue = MetricValue.unavailable(reason + "; using stored fallback value.");
                logMetricResult(type, metricValue, System.currentTimeMillis() - start);
                return metricValue;
            }
        }, executorService).whenComplete((value, throwable) -> inFlightMetrics.remove(inFlightKey)));
    }

    private String metricCacheKey(Locality locality, LiveMetricType type) {
        return "locality:" + localityKey(locality);
    }

    private String localityKey(Locality locality) {
        String name = normalizeLocalityToken(locality.getName());
        String city = normalizeLocalityToken(locality.getCity());
        String state = normalizeLocalityToken(locality.getState());
        return name + "|" + city + "|" + state;
    }

    private String normalizeLocalityToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private double withRetry(LiveMetricType type, Supplier<Double> supplier) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                double value = supplier.get();
                if (value >= 0) {
                    return value;
                }
                throw new IOException(type.getDisplayName() + " returned no usable value.");
            } catch (Exception ex) {
                lastFailure = ex;
                if (attempt < MAX_ATTEMPTS) {
                    sleep(backoffForAttempt(attempt));
                }
            }
        }
        throw new CompletionException(lastFailure);
    }

    private long backoffForAttempt(int attempt) {
        return (long) (BASE_BACKOFF_MS * Math.pow(2, Math.max(0, attempt - 1)));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CompletionException(ex);
        }
    }

    private RawLocalityMetrics buildMetrics(MetricValue jobs, MetricValue cost, MetricValue aqi,
                                            MetricValue transport, MetricValue healthcare) {
        RawLocalityMetrics metrics = new RawLocalityMetrics();
        metrics.jobCount = jobs;
        metrics.costIndex = cost;
        metrics.aqi = aqi;
        metrics.transportDuration = transport;
        metrics.hospitalCount = healthcare;
        metrics.timestamp = Instant.now().toString();
        metrics.metricStatuses.put(LiveMetricType.JOBS, jobs.getStatus());
        metrics.metricStatuses.put(LiveMetricType.COST_OF_LIVING, cost.getStatus());
        metrics.metricStatuses.put(LiveMetricType.AQI, aqi.getStatus());
        metrics.metricStatuses.put(LiveMetricType.TRANSPORT, transport.getStatus());
        metrics.metricStatuses.put(LiveMetricType.HEALTHCARE, healthcare.getStatus());
        metrics.confidenceScore = calculateConfidence(jobs, cost, aqi, transport, healthcare);
        metrics.source = metrics.confidenceScore >= 1.0 ? "Live" : (hasAnyLive(jobs, cost, aqi, transport, healthcare) ? "Mixed" : "Cached");
        metrics.statusMessage = buildStatusMessage(metrics.source);
        return metrics;
    }

    private RawLocalityMetrics createDisabledMetrics() {
        RawLocalityMetrics metrics = new RawLocalityMetrics();
        metrics.jobCount = MetricValue.unavailable("Live API disabled; using stored fallback value.");
        metrics.costIndex = MetricValue.unavailable("Live API disabled; using stored fallback value.");
        metrics.aqi = MetricValue.unavailable("Live API disabled; using stored fallback value.");
        metrics.transportDuration = MetricValue.unavailable("Live API disabled; using stored fallback value.");
        metrics.hospitalCount = MetricValue.unavailable("Live API disabled; using stored fallback value.");
        metrics.confidenceScore = 0.45;
        metrics.statusMessage = "Live API disabled in configuration.";
        return metrics;
    }

    private boolean hasAnyLive(MetricValue... values) {
        for (MetricValue value : values) {
            if (value.isLive()) {
                return true;
            }
        }
        return false;
    }

    private double calculateConfidence(MetricValue... values) {
        int liveCount = 0;
        for (MetricValue value : values) {
            liveCount += value.isLive() || value.isTrusted() ? 1 : 0;
        }
        if (liveCount == 5) {
            return 1.00;
        }
        if (liveCount == 4) {
            return 0.85;
        }
        if (liveCount == 3) {
            return 0.70;
        }
        if (liveCount == 2) {
            return 0.50;
        }
        if (liveCount == 1) {
            return 0.40;
        }
        return 0.35;
    }

    private String buildStatusMessage(String source) {
        if ("Live".equals(source)) {
            return "All live API metrics loaded successfully.";
        }
        if ("Mixed".equals(source)) {
            return "Some live APIs failed; available live metrics were used with metric-level fallback.";
        }
        return "Live API data unavailable; using stored fallback metrics.";
    }

    private CompletableFuture<Coordinates> fetchCoordinates(Locality locality) {
        Coordinates cached = coordinateCache.get(locality.getId());
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        Coordinates localityCoordinates = buildLocalityCoordinates(locality);
        if (localityCoordinates != null) {
            coordinateCache.put(locality.getId(), localityCoordinates);
            System.out.println("[LocalityDebug] " + locality.getName() + " | Lat=" + localityCoordinates.latitude
                    + " | Lon=" + localityCoordinates.longitude);
            return CompletableFuture.completedFuture(localityCoordinates);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = URLEncoder.encode(locality.getName() + ", " + locality.getCity() + ", "
                        + locality.getState() + ", India", StandardCharsets.UTF_8);
                String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + query;
                JsonArray array = getJsonArray(url, Map.of("User-Agent", "SmartCityRecommendationSystem/1.0"));
                if (array.size() == 0) {
                    query = URLEncoder.encode(locality.getCity() + ", " + locality.getState() + ", India",
                            StandardCharsets.UTF_8);
                    url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + query;
                    array = getJsonArray(url, Map.of("User-Agent", "SmartCityRecommendationSystem/1.0"));
                }
                if (array.size() == 0) {
                    throw new IOException("No coordinates returned for " + locality.getName());
                }
                JsonObject item = array.get(0).getAsJsonObject();
                Coordinates coordinates = new Coordinates(item.get("lat").getAsDouble(), item.get("lon").getAsDouble());
                coordinateCache.put(locality.getId(), coordinates);
                return coordinates;
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }, executorService);
    }

    private double fetchJobCountValue(Locality locality) {
        if (adzunaAppId.isBlank() || adzunaAppKey.isBlank()) {
            throw new ApiCallException("Missing API key");
        }
        try {
            String where = URLEncoder.encode(locality.getName() + ", " + locality.getCity() + ", "
                    + locality.getState(), StandardCharsets.UTF_8);
            String url = String.format("%s?app_id=%s&app_key=%s&where=%s&results_per_page=1",
                    ConfigLoader.getProperty("api.adzuna.url", "https://api.adzuna.com/v1/api/jobs/in/search/1"),
                    URLEncoder.encode(adzunaAppId, StandardCharsets.UTF_8),
                    URLEncoder.encode(adzunaAppKey, StandardCharsets.UTF_8),
                    where);
            JsonObject json = getJsonObject(url, Map.of());
            double count = getOptionalDouble(json, "count", -1.0);
            logParsedValue(LiveMetricType.JOBS, count);
            return count;
        } catch (ApiCallException ex) {
            throw ex;
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        } catch (Exception ex) {
            throw new ApiCallException(failureReason(ex), ex);
        }
    }

    private MetricValue fetchCostOfLivingValue(Locality locality) {
        long start = System.currentTimeMillis();
        Double value = COST_OF_LIVING_INDEX.get(normalizeCity(locality.getCity()));
        if (value == null) {
            value = COST_OF_LIVING_INDEX.get("default");
        }
        if (value != null) {
            HEALTH_MANAGER.recordSuccess(LiveMetricType.COST_OF_LIVING, System.currentTimeMillis() - start);
            MetricValue metricValue = MetricValue.local(value, "Loaded from maintained local cost-of-living dataset.");
            logParsedValue(LiveMetricType.COST_OF_LIVING, value);
            logMetricResult(LiveMetricType.COST_OF_LIVING, metricValue, System.currentTimeMillis() - start);
            return metricValue;
        }
        HEALTH_MANAGER.recordFailure(LiveMetricType.COST_OF_LIVING, System.currentTimeMillis() - start, "Missing local dataset");
        MetricValue metricValue = MetricValue.unavailable("Missing local cost-of-living dataset; using stored fallback value.");
        logMetricResult(LiveMetricType.COST_OF_LIVING, metricValue, System.currentTimeMillis() - start);
        return metricValue;
    }

    private double fetchAirQualityValue(Coordinates coordinates) {
        if (coordinates == null) {
            return -1.0;
        }
        try {
            String url = String.format(
                    "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%s&longitude=%s&current=pm2_5",
                    coordinates.latitude, coordinates.longitude);
            JsonObject json = getJsonObject(url, Map.of());
            JsonObject current = json.has("current") ? json.getAsJsonObject("current") : null;
            double pm25 = current != null ? getOptionalDouble(current, "pm2_5", -1.0) : -1.0;
            logParsedValue(LiveMetricType.AQI, pm25);
            return pm25;
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        } catch (Exception ex) {
            throw new ApiCallException(failureReason(ex), ex);
        }
    }

    private double fetchTransportValue(Coordinates coordinates) {
        if (coordinates == null) {
            throw new ApiCallException("Missing coordinates");
        }
        if (openRouteServiceApiKey.isBlank()) {
            throw new ApiCallException("Missing API key");
        }
        try {
            String url = ConfigLoader.getProperty("api.openrouteservice.matrix.url",
                    "https://api.openrouteservice.org/v2/matrix/driving-car");
            String body = String.format(
                    "{\"locations\":[[%s,%s],[%s,%s]],\"metrics\":[\"duration\"],\"sources\":[0],\"destinations\":[1]}",
                    coordinates.longitude,
                    coordinates.latitude,
                    coordinates.longitude + 0.03,
                    coordinates.latitude);
            JsonObject json = postJsonObject(url, body, Map.of(
                    "Authorization", openRouteServiceApiKey,
                    "Content-Type", "application/json"));
            JsonArray durations = json.has("durations") ? json.getAsJsonArray("durations") : new JsonArray();
            if (durations.size() == 0 || !durations.get(0).isJsonArray()) {
                return -1.0;
            }
            JsonArray row = durations.get(0).getAsJsonArray();
            if (row.size() == 0 || row.get(0).isJsonNull()) {
                return -1.0;
            }
            double duration = row.get(0).getAsDouble();
            logParsedValue(LiveMetricType.TRANSPORT, duration);
            return duration;
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        } catch (Exception ex) {
            throw new ApiCallException(failureReason(ex), ex);
        }
    }

    private double fetchHealthcareValue(Coordinates coordinates) {
        if (coordinates == null) {
            throw new ApiCallException("Missing coordinates");
        }
        if (geoapifyApiKey.isBlank()) {
            throw new ApiCallException("Missing API key");
        }
        try {
            String url = String.format(
                    "%s?categories=healthcare.hospital&filter=circle:%s,%s,5000&limit=50&apiKey=%s",
                    ConfigLoader.getProperty("api.geoapify.places.url", "https://api.geoapify.com/v2/places"),
                    coordinates.longitude,
                    coordinates.latitude,
                    URLEncoder.encode(geoapifyApiKey, StandardCharsets.UTF_8));
            JsonObject json = getJsonObject(url, Map.of());
            JsonArray features = json.has("features") ? json.getAsJsonArray("features") : new JsonArray();
            double count = features.size();
            logParsedValue(LiveMetricType.HEALTHCARE, count);
            return count;
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        } catch (Exception ex) {
            throw new ApiCallException(failureReason(ex), ex);
        }
    }

    private JsonObject getJsonObject(String url, Map<String, String> headers) throws IOException {
        try {
            return JsonParser.parseString(executeGet(url, headers)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        }
    }

    private JsonArray getJsonArray(String url, Map<String, String> headers) throws IOException {
        try {
            return JsonParser.parseString(executeGet(url, headers)).getAsJsonArray();
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        }
    }

    private JsonObject postJsonObject(String url, String body, Map<String, String> headers) throws IOException {
        try {
            return JsonParser.parseString(executePost(url, body, headers)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException ex) {
            throw new ApiCallException("Parse Error", ex);
        }
    }

    private String executeGet(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readBody(stream);
            logHttp("GET", url, null, status, body);
            if (status < 200 || status >= 300) {
                throw httpFailure(status, url, body);
            }
            return body;
        } catch (SocketTimeoutException ex) {
            throw new ApiCallException("Timeout", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String executePost(String url, String body, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            connection.getOutputStream().write(bytes);

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = readBody(stream);
            logHttp("POST", url, body, status, response);
            if (status < 200 || status >= 300) {
                throw httpFailure(status, url, response);
            }
            return response;
        } catch (SocketTimeoutException ex) {
            throw new ApiCallException("Timeout", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ApiCallException httpFailure(int status, String url, String body) {
        if (status == 401) {
            return new ApiCallException("401 Unauthorized");
        }
        if (status == 403) {
            return new ApiCallException("403 Forbidden");
        }
        if (status == 429) {
            return new ApiCallException("429 Rate Limit");
        }
        if (status >= 500) {
            return new ApiCallException("500 Server Error");
        }
        return new ApiCallException("HTTP " + status + " for " + url + ": " + body);
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
        if (object == null || !object.has(member) || object.get(member).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(member).getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String failureReason(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ApiCallException) {
                return current.getMessage();
            }
            if (current instanceof SocketTimeoutException) {
                return "Timeout";
            }
            current = current.getCause();
        }
        return throwable == null || throwable.getMessage() == null ? "Unknown failure" : throwable.getMessage();
    }

    private static Map<String, Double> loadCostOfLivingIndex() {
        Map<String, Double> costs = new HashMap<>();
        try (InputStream input = openCostDataset()) {
            if (input == null) {
                return costs;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                costs.put(normalizeCity(entry.getKey()), entry.getValue().getAsDouble());
            }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load cost_of_living.json: " + ex.getMessage());
        }
        return costs;
    }

    private static InputStream openCostDataset() throws IOException {
        InputStream resource = ApiService.class.getClassLoader().getResourceAsStream("cost_of_living.json");
        if (resource != null) {
            return resource;
        }
        try {
            return new FileInputStream("data/cost_of_living.json");
        } catch (IOException ex) {
            return new FileInputStream("cost_of_living.json");
        }
    }

    private static Map<String, Coordinates> createCityCoordinates() {
        Map<String, Coordinates> coordinates = new HashMap<>();
        coordinates.put(normalizeCity("Mumbai"), new Coordinates(19.0760, 72.8777));
        coordinates.put(normalizeCity("Pune"), new Coordinates(18.5204, 73.8567));
        coordinates.put(normalizeCity("Bangalore"), new Coordinates(12.9716, 77.5946));
        coordinates.put(normalizeCity("Bengaluru"), new Coordinates(12.9716, 77.5946));
        coordinates.put(normalizeCity("Delhi"), new Coordinates(28.6139, 77.2090));
        coordinates.put(normalizeCity("New Delhi"), new Coordinates(28.6139, 77.2090));
        coordinates.put(normalizeCity("Hyderabad"), new Coordinates(17.3850, 78.4867));
        coordinates.put(normalizeCity("Chennai"), new Coordinates(13.0827, 80.2707));
        coordinates.put(normalizeCity("Kolkata"), new Coordinates(22.5726, 88.3639));
        coordinates.put(normalizeCity("Ahmedabad"), new Coordinates(23.0225, 72.5714));
        coordinates.put(normalizeCity("Surat"), new Coordinates(21.1702, 72.8311));
        coordinates.put(normalizeCity("Jaipur"), new Coordinates(26.9124, 75.7873));
        coordinates.put(normalizeCity("Lucknow"), new Coordinates(26.8467, 80.9462));
        coordinates.put(normalizeCity("Kanpur"), new Coordinates(26.4499, 80.3319));
        coordinates.put(normalizeCity("Nagpur"), new Coordinates(21.1458, 79.0882));
        coordinates.put(normalizeCity("Indore"), new Coordinates(22.7196, 75.8577));
        coordinates.put(normalizeCity("Thane"), new Coordinates(19.2183, 72.9781));
        coordinates.put(normalizeCity("Nashik"), new Coordinates(19.9975, 73.7898));
        coordinates.put(normalizeCity("Kolhapur"), new Coordinates(16.7050, 74.2433));
        return coordinates;
    }

    private Coordinates buildLocalityCoordinates(Locality locality) {
        Coordinates cityCoordinates = CITY_COORDINATES.get(normalizeCity(locality.getCity()));
        if (cityCoordinates == null) {
            return null;
        }

        String seed = locality.getName() + "|" + locality.getCity() + "|" + locality.getState();
        int hash = Math.abs(seed.toLowerCase(Locale.ROOT).hashCode());
        double latitudeOffset = ((hash % 13) - 6) * 0.018;
        double longitudeOffset = (((hash / 13) % 11) - 5) * 0.018;

        return new Coordinates(
                cityCoordinates.latitude + latitudeOffset,
                cityCoordinates.longitude + longitudeOffset
        );
    }

    private static String normalizeCity(String city) {
        return city == null ? "" : city.trim().toLowerCase();
    }

    private void logConstructorDiagnostics() {
        if (!DIAGNOSTICS_ENABLED) {
            return;
        }
        System.out.println("[ApiDiagnostics] ApiService constructor executed");
        System.out.println("[ApiDiagnostics] feature.realTimeUpdate=" + enableLiveApi);
        System.out.println("[ApiDiagnostics] api.adzuna.appId=" + redactValue(adzunaAppId));
        System.out.println("[ApiDiagnostics] api.adzuna.appKey=" + redactValue(adzunaAppKey));
        System.out.println("[ApiDiagnostics] api.openrouteservice.key=" + redactValue(openRouteServiceApiKey));
        System.out.println("[ApiDiagnostics] api.geoapify.key=" + redactValue(geoapifyApiKey));
    }

    private void logParsedValue(LiveMetricType type, double value) {
        if (DIAGNOSTICS_ENABLED) {
            System.out.println("[ApiDiagnostics] " + type.getDisplayName() + " parsedValue=" + value);
        }
    }

    private void logMetricResult(LiveMetricType type, MetricValue value, long elapsedMs) {
        if (!DIAGNOSTICS_ENABLED) {
            return;
        }
        System.out.println("[ApiDiagnostics] " + type.getDisplayName()
                + " finalState={available=" + value.isAvailable()
                + ", live=" + value.isLive()
                + ", trusted=" + value.isTrusted()
                + ", value=" + value.getValue()
                + ", source=" + value.getStatus().getSource()
                + ", reason=\"" + value.getStatus().getMessage()
                + "\", elapsedMs=" + elapsedMs + "}");
    }

    private void logHttp(String method, String url, String requestBody, int status, String responseBody) {
        if (!DIAGNOSTICS_ENABLED) {
            return;
        }
        System.out.println("[ApiDiagnostics] " + method + " " + redactUrl(url));
        if (requestBody != null) {
            System.out.println("[ApiDiagnostics] requestBody=" + sample(requestBody));
        }
        System.out.println("[ApiDiagnostics] responseCode=" + status);
        System.out.println("[ApiDiagnostics] responseBodySample=" + sample(responseBody));
    }

    private String redactUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i)(app_key=)[^&]+", "$1***")
                .replaceAll("(?i)(apiKey=)[^&]+", "$1***");
    }

    private String redactValue(String value) {
        if (value == null || value.isBlank()) {
            return "<blank>";
        }
        if (value.length() <= 8) {
            return "<configured:" + value.length() + " chars>";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4)
                + " (" + value.length() + " chars)";
    }

    private String sample(String body) {
        if (body == null) {
            return "";
        }
        String compact = body.replace('\n', ' ').replace('\r', ' ');
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }

    private static final class ApiCallException extends RuntimeException {
        private ApiCallException(String reason) {
            super(reason);
        }

        private ApiCallException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }
}
