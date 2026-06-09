package com.smartcity.controller;

import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;
import com.smartcity.service.RecommendationService;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main Controller for MVC architecture
 * Manages interaction between Model and View
 */
public class RecommendationController {

    private final RecommendationService recommendationService;
    private UserPreferences currentPreferences;
    private List<RecommendationResult> lastResults;
    private final List<ControllerListener> listeners;
    private final AtomicInteger requestSequence;
    private String lastResultSignature;

    public RecommendationController() {
        this.recommendationService = new RecommendationService();
        this.currentPreferences = new UserPreferences();
        this.listeners = new ArrayList<>();
        this.lastResults = new ArrayList<>();
        this.requestSequence = new AtomicInteger(0);
        this.lastResultSignature = "";
    }

    public List<String> getCities() {
        return recommendationService.getAllCities();
    }

    public List<String> getLocalitiesForCity(String city) {
        return recommendationService.getLocalityNamesForCity(city);
    }

    public void calculateRecommendations(String city) {
        currentPreferences.setSelectedCity(city);
        int requestId = requestSequence.incrementAndGet();
        notifyLoadingStateChanged(true, "Fetching live locality metrics...");
        recommendationService.resetApiHealth();

        recommendationService.getRecommendations(city, currentPreferences)
                .thenAccept(results -> SwingUtilities.invokeLater(() -> {
                    if (requestId != requestSequence.get()) {
                        return;
                    }
                    lastResults = results;
                    notifyLoadingStateChanged(false, "Recommendations ready");
                    notifyListenersIfChanged(results);
                    if (!results.isEmpty()
                            && results.stream().allMatch(result -> "Cached".equalsIgnoreCase(result.getDataSource()))) {
                        notifyRecommendationError("Live API data is unavailable right now. Showing cached fallback results.");
                    }
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        if (requestId != requestSequence.get()) {
                            return;
                        }
                        notifyLoadingStateChanged(false, "Fallback cached results shown");
                        notifyRecommendationError("Live API fetch failed. Showing cached fallback data.");
                    });
                    return null;
                });
    }

    public void updateUserPreferences(UserPreferences preferences) {
        this.currentPreferences = preferences;
    }

    public void updatePreference(String key, Object value) {
        switch (key) {
            case "budget":
                currentPreferences.setBudget((Double) value);
                break;
            case "familySize":
                currentPreferences.setFamilySize((Integer) value);
                break;
            case "workType":
                currentPreferences.setWorkType((String) value);
                break;
            case "jobWeight":
                currentPreferences.setJobWeight((Double) value);
                break;
            case "costOfLivingWeight":
                currentPreferences.setCostOfLivingWeight((Double) value);
                break;
            case "healthcareWeight":
                currentPreferences.setHealthcareWeight((Double) value);
                break;
            case "transportWeight":
                currentPreferences.setTransportWeight((Double) value);
                break;
            case "safetyWeight":
                currentPreferences.setSafetyWeight((Double) value);
                break;
            case "environmentWeight":
                currentPreferences.setEnvironmentWeight((Double) value);
                break;
            case "lifestyleWeight":
                currentPreferences.setLifestyleWeight((Double) value);
                break;
            case "minimumSafetyThreshold":
                currentPreferences.setMinimumSafetyThreshold((Double) value);
                break;
            case "budgetMatchThreshold":
                currentPreferences.setBudgetMatchThreshold((Double) value);
                break;
            case "profileType":
                currentPreferences.applyProfilePreset((String) value);
                break;
            case "selectedCity":
                currentPreferences.setSelectedCity((String) value);
                break;
        }
    }

    public void recalculate() {
        if (currentPreferences.getSelectedCity() != null) {
            calculateRecommendations(currentPreferences.getSelectedCity());
        }
    }

    public UserPreferences getCurrentPreferences() {
        return currentPreferences;
    }

    public List<RecommendationResult> getLastResults() {
        return lastResults;
    }

    public RecommendationService getRecommendationService() {
        return recommendationService;
    }

    public void addListener(ControllerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ControllerListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ControllerListener listener : listeners) {
            listener.onRecommendationsUpdated(lastResults);
        }
    }

    private void notifyListenersIfChanged(List<RecommendationResult> results) {
        String signature = buildResultSignature(results);
        if (signature.equals(lastResultSignature)) {
            return;
        }

        lastResultSignature = signature;
        notifyListeners();
    }

    private String buildResultSignature(List<RecommendationResult> results) {
        StringBuilder builder = new StringBuilder();
        for (RecommendationResult result : results) {
            builder.append(result.getLocality().getId())
                    .append(':')
                    .append(String.format("%.2f", result.getFinalScore()))
                    .append('|');
        }
        return builder.toString();
    }

    private void notifyLoadingStateChanged(boolean loading, String message) {
        for (ControllerListener listener : listeners) {
            listener.onLoadingStateChanged(loading, message);
        }
    }

    private void notifyRecommendationError(String message) {
        for (ControllerListener listener : listeners) {
            listener.onRecommendationError(message);
        }
    }

    public interface ControllerListener {
        void onRecommendationsUpdated(List<RecommendationResult> results);

        void onLoadingStateChanged(boolean loading, String message);

        void onRecommendationError(String message);
    }
}
