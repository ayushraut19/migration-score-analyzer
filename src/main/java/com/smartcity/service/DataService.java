package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.utils.JsonDataLoader;
import java.util.*;

/**
 * Data Service - Manages locality metadata and fallback caching.
 * The service supplies skeleton locality objects and preserves local backup data.
 */
public class DataService {

    private final Map<String, Locality> localityCache;
    private final Map<String, List<Locality>> cityCache;
    private final List<String> cities;

    public DataService() {
        this.localityCache = new HashMap<>();
        this.cityCache = new HashMap<>();
        this.cities = new ArrayList<>();
        loadFallbackData();
    }

    /**
     * Load fallback locality data from JSON if live APIs are unavailable.
     */
    private void loadFallbackData() {
        List<Locality> localities = JsonDataLoader.loadLocalities();

        for (Locality locality : localities) {
            localityCache.put(locality.getId(), locality);

            String city = locality.getCity();
            if (!cityCache.containsKey(city)) {
                cityCache.put(city, new ArrayList<>());
                cities.add(city);
            }
            cityCache.get(city).add(locality);
        }

        Collections.sort(cities);
    }

    /**
     * Get locality by ID from fallback cache.
     */
    public Locality getLocalityById(String id) {
        return localityCache.get(id);
    }

    /**
     * Get all localities in a city.
     */
    public List<Locality> getLocalitiesByCity(String city) {
        return cityCache.getOrDefault(city, new ArrayList<>());
    }

    /**
     * Get list of all configured cities.
     */
    public List<String> getAllCities() {
        return new ArrayList<>(cities);
    }

    /**
     * Get all fallback localities.
     */
    public List<Locality> getAllLocalities() {
        return new ArrayList<>(localityCache.values());
    }

    /**
     * Reload fallback data.
     */
    public void reloadData() {
        localityCache.clear();
        cityCache.clear();
        cities.clear();
        loadFallbackData();
    }
}
