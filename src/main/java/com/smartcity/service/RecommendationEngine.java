package com.smartcity.service;

import com.smartcity.model.Locality;
import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * UPGRADED: RecommendationEngine with post-processing and re-ranking.
 * 
 * Orchestrates:
 * 1. Live enrichment of locality data
 * 2. Comprehensive scoring using Decision-Aware ScoringEngine
 * 3. Post-processing and re-ranking for diversity and quality
 * 4. Safety threshold filtering
 * 5. Balance-based boosting of top recommendations
 */
public class RecommendationEngine {

    private final DataService dataService;
    private final DataAggregator aggregator;
    private final ScoringEngine calculator;

    public RecommendationEngine(DataService dataService) {
        this.dataService = dataService;
        this.aggregator = new DataAggregator();
        this.calculator = new ScoringEngine();
    }

    /**
     * MAIN METHOD: Rank and post-process localities for recommendations.
     * 
     * Process:
     * 1. Retrieve all localities for city
     * 2. Enrich with live data (parallel async)
     * 3. Calculate comprehensive scores (Decision-Aware ScoringEngine)
     * 4. Sort by score
     * 5. Apply post-processing re-ranking:
     *    - Filter by minimum safety threshold
     *    - Boost balanced top 3 localities
     *    - Remove duplicates/similar profiles
     *    - Ensure minimum recommendation score
     * 6. Final ranking
     *
     * @param city        Target city
     * @param preferences User's preferences
     * @return Ranked list of RecommendationResult objects
     */
    public CompletableFuture<List<RecommendationResult>> rankLocalities(String city, UserPreferences preferences) {
        List<Locality> localities = dataService.getLocalitiesByCity(city);
        if (localities.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // Step 1-3: Enrich and score all localities asynchronously
        List<CompletableFuture<RecommendationResult>> futures = new ArrayList<>();
        for (Locality locality : localities) {
            CompletableFuture<RecommendationResult> future = aggregator.enrichLocality(locality)
                    .thenApply(enriched -> calculator.calculateScore(enriched, preferences))
                    .exceptionally(ex -> calculator.calculateScore(locality, preferences));
            futures.add(future);
        }

        // Step 4-6: Post-process results
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignore -> {
                    // Collect all scored results
                    List<RecommendationResult> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    // Step 5a: Filter by minimum score threshold
                    results = filterByMinimumScore(results);

                    // Step 5b: Sort by score (descending)
                    results.sort(Comparator.comparingDouble(RecommendationResult::getFinalScore).reversed());

                    // Step 5c: Apply re-ranking improvements
                    results = applyReRankingImprovements(results, preferences);

                    // Step 6: Set final ranks
                    for (int i = 0; i < results.size(); i++) {
                        results.get(i).setRank(i + 1);
                    }

                    return results;
                });
    }

    /**
     * Filter results by minimum recommendation score.
     * Very low scores aren't worth recommending.
     *
     * @param results Initial recommendation results
     * @return Filtered results above minimum threshold
     */
    private List<RecommendationResult> filterByMinimumScore(List<RecommendationResult> results) {
        return results.stream()
                .filter(r -> r.getFinalScore() >= ScoringConfig.MINIMUM_RECOMMENDATION_SCORE)
                .collect(Collectors.toList());
    }

    /**
     * Apply post-processing re-ranking improvements:
     * 
     * 1. REMOVE SAFETY OUTLIERS: 
     *    Exclude any remaining localities with safety below threshold (shouldn't happen but safety check)
     * 
     * 2. BALANCE BOOST:
     *    Boost top 3 most balanced localities to reward comprehensive goodness
     *    Prevents all top results from being "specialist" (good at one thing)
     * 
     * 3. DIVERSITY CONSIDERATION:
     *    If multiple localities are very similar (e.g., same neighborhood),
     *    keep only the top one to encourage diversity
     *
     * @param results       Sorted results (highest score first)
     * @param preferences   User preferences
     * @return Re-ranked results
     */
    private List<RecommendationResult> applyReRankingImprovements(List<RecommendationResult> results, 
                                                                  UserPreferences preferences) {
        if (results.isEmpty()) {
            return results;
        }

        // Step 1: Remove explicit safety outliers only when the user asked for a safety floor.
        // Safety as a preference is handled inside ScoringEngine, where a 0 slider has near-zero influence.
        if (preferences.getMinimumSafetyThreshold() > 0 && preferences.getSafetyWeight() > 0) {
            double activeSafetyFloor = preferences.getMinimumSafetyThreshold()
                    * (0.35 + (preferences.getSafetyWeight() / 10.0) * 0.65);
            results = results.stream()
                    .filter(r -> r.getLocality().getSafetyScore() >= activeSafetyFloor)
                    .collect(Collectors.toList());
        }

        if (results.isEmpty()) {
            return results;
        }

        // Step 2: Identify and boost balanced localities in top 3
        List<RecommendationResult> boosted = new ArrayList<>();
        int boostCount = 0;
        for (int i = 0; i < results.size() && boostCount < ScoringConfig.RE_RANK_TOP_N_BOOST; i++) {
            RecommendationResult result = results.get(i);
            
            // Check if this locality is well-balanced (low variance in scores)
            double variance = calculateScoreVariance(result.getScoreBreakdown());
            if (variance < ScoringConfig.BALANCE_VARIANCE_THRESHOLD) {
                // Apply boost to balanced localities
                double boostedScore = result.getFinalScore() + ScoringConfig.RE_RANK_BALANCE_BOOST;
                result.setFinalScore(boostedScore);
                boostCount++;
            }
        }

        // Step 3: Re-sort after boosts
        results.sort(Comparator.comparingDouble(RecommendationResult::getFinalScore).reversed());

        // Step 4: Remove near-duplicates (same city/area)
        results = removeSimilarLocalities(results);

        return results;
    }

    /**
     * Calculate variance in score breakdown to detect balance.
     * Low variance = balanced across all factors
     * High variance = specialist (strong in some, weak in others)
     *
     * @param scoreBreakdown Map of factor scores
     * @return Standard deviation of scores
     */
    private double calculateScoreVariance(java.util.Map<String, Double> scoreBreakdown) {
        if (scoreBreakdown == null || scoreBreakdown.isEmpty()) {
            return 10.0; // Neutral high value
        }

        double mean = scoreBreakdown.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(5.0);

        double variance = scoreBreakdown.values().stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * Remove similar/duplicate localities to encourage diversity.
     * 
     * Rationale: If we have multiple localities from same area,
     * keep only the best one. This ensures diverse recommendations.
     *
     * @param results Ranked results
     * @return Results with duplicates removed
     */
    private List<RecommendationResult> removeSimilarLocalities(List<RecommendationResult> results) {
        List<RecommendationResult> unique = new ArrayList<>();
        List<String> seenAreas = new ArrayList<>();

        for (RecommendationResult result : results) {
            String area = buildAreaSignature(result.getLocality());

            // Only remove true duplicates, not every locality in the same city.
            if (!seenAreas.contains(area)) {
                unique.add(result);
                seenAreas.add(area);
            }
        }

        return unique;
    }

    private String buildAreaSignature(Locality locality) {
        return (locality.getName() + "|" + locality.getCity() + "|" + locality.getState())
                .trim()
                .toLowerCase();
    }
}
