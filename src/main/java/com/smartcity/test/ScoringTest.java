package com.smartcity.test;

import com.smartcity.model.Locality;
import com.smartcity.model.RecommendationResult;
import com.smartcity.model.UserPreferences;
import com.smartcity.service.ScoringEngine;

/**
 * Simple test class to verify scoring algorithm
 * Run: java -cp target/classes com.smartcity.test.ScoringTest
 */
public class ScoringTest {
    
    public static void main(String[] args) {
        System.out.println("=== Smart City Scoring Engine Test ===\n");
        
        testScoringAlgorithm();
        testDifferentProfiles();
        testBudgetCalculation();
        testZeroWeightExclusion();
    }
    
    private static void testScoringAlgorithm() {
        System.out.println("TEST 1: Basic Scoring Algorithm");
        System.out.println("--------------------------------");
        
        // Create test locality
        Locality locality = new Locality(
            "TEST001",
            "Test City",
            "Test State",
            "Test State",
            25000,  // avgRent
            8.5,    // jobIndex
            8.0,    // hospitalRating
            7.5,    // transportScore
            7.0,    // safetyScore
            6.0,    // pollutionIndex
            8.5,    // lifestyleScore
            6.0,    // populationDensity
            "Test locality"
        );
        
        // Create user preferences
        UserPreferences prefs = new UserPreferences();
        prefs.setBudget(500000);
        prefs.setJobWeight(8);
        prefs.setCostOfLivingWeight(7);
        prefs.setHealthcareWeight(8);
        prefs.setTransportWeight(6);
        prefs.setSafetyWeight(7);
        prefs.setEnvironmentWeight(5);
        prefs.setLifestyleWeight(8);
        
        // Calculate score
        ScoringEngine engine = new ScoringEngine();
        RecommendationResult result = engine.calculateScore(locality, prefs);
        
        System.out.println("Locality: " + locality.getName());
        System.out.println("Final Score: " + String.format("%.2f/10", result.getFinalScore()));
        System.out.println("\nBreakdown:");
        result.getScoreBreakdown().forEach((key, value) -> 
            System.out.println("  " + key + ": " + String.format("%.2f", value))
        );
        System.out.println("\nExplanation: " + result.getExplanation());
        System.out.println();
    }
    
    private static void testDifferentProfiles() {
        System.out.println("TEST 2: Different User Profiles");
        System.out.println("-------------------------------");
        
        Locality locality = new Locality(
            "TEST002",
            "Urban Hub",
            "Big City",
            "State",
            22000, 9.0, 8.5, 8.0, 7.0, 7.0, 9.0, 8.0, "Urban locality"
        );
        
        ScoringEngine engine = new ScoringEngine();
        String[] profiles = {"Student", "Bachelor", "Family"};
        
        for (String profile : profiles) {
            UserPreferences prefs = new UserPreferences();
            prefs.applyProfilePreset(profile);
            prefs.setBudget(500000);
            
            RecommendationResult result = engine.calculateScore(locality, prefs);
            System.out.println(profile + " Profile Score: " + 
                String.format("%.2f/10", result.getFinalScore()));
        }
        System.out.println();
    }
    
    private static void testBudgetCalculation() {
        System.out.println("TEST 3: Budget-Based Cost Calculation");
        System.out.println("------------------------------------");
        
        Locality locality = new Locality(
            "TEST003",
            "Affordable Area",
            "Tier 2 City",
            "State",
            15000, 6.0, 7.0, 6.0, 8.0, 5.0, 7.0, 5.0, "Affordable"
        );
        
        ScoringEngine engine = new ScoringEngine();
        int[] budgets = {300000, 500000, 800000, 2000000};
        
        for (int budget : budgets) {
            UserPreferences prefs = new UserPreferences();
            prefs.setBudget(budget);
            prefs.setCostOfLivingWeight(10);
            prefs.setJobWeight(0);
            prefs.setHealthcareWeight(0);
            prefs.setTransportWeight(0);
            prefs.setSafetyWeight(0);
            prefs.setEnvironmentWeight(0);
            prefs.setLifestyleWeight(0);
            
            RecommendationResult result = engine.calculateScore(locality, prefs);
            System.out.println("Budget: ₹" + String.format("%,d", budget) + 
                " → Cost Score: " + String.format("%.2f/10", 
                result.getScoreBreakdown().get("Cost of Living")));
        }
        System.out.println();
    }

    private static void testZeroWeightExclusion() {
        System.out.println("TEST 4: Zero Weight Exclusion");
        System.out.println("--------------------------------");

        Locality locality = new Locality(
            "TEST004",
            "Flexible Town",
            "Open State",
            "Open State",
            22000,  // avgRent
            7.0,    // jobIndex
            7.5,    // hospitalRating
            7.0,    // transportScore
            8.0,    // safetyScore
            5.5,    // pollutionIndex
            7.0,    // lifestyleScore
            5.5,    // populationDensity
            "Flexible locality"
        );

        UserPreferences prefs = new UserPreferences();
        prefs.setBudget(500000);
        prefs.setJobWeight(8);
        prefs.setCostOfLivingWeight(7);
        prefs.setHealthcareWeight(8);
        prefs.setTransportWeight(6);
        prefs.setSafetyWeight(0);
        prefs.setEnvironmentWeight(5);
        prefs.setLifestyleWeight(8);

        ScoringEngine engine = new ScoringEngine();
        RecommendationResult result = engine.calculateScore(locality, prefs);

        System.out.println("Safety included in breakdown: " + result.getScoreBreakdown().containsKey("Safety"));
        System.out.println("Breakdown keys: " + result.getScoreBreakdown().keySet());
        System.out.println();
    }
}
