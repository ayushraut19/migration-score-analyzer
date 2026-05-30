package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Recommendation Service - Facade for recommendation workflow.
 */
public class RecommendationService {

    private final DataService dataService;
    private final RecommendationEngine recommendationEngine;

    public RecommendationService() {
        this.dataService = new DataService();
        this.recommendationEngine = new RecommendationEngine(this.dataService);
    }

    public List<String> getAllCities() {
        return dataService.getAllCities();
    }

    public List<String> getLocalityNamesForCity(String city) {
        return dataService.getLocalitiesByCity(city).stream()
                .map(Locality::getName)
                .collect(Collectors.toList());
    }

    public CompletableFuture<List<RecommendationResult>> getRecommendations(String city, UserPreferences preferences) {
        return recommendationEngine.rankLocalities(city, preferences);
    }

    public List<Locality> getAllLocalities() {
        return dataService.getAllLocalities();
    }
}
