/**
 * ============================================================================
 * DECISION-AWARE SCORING ENGINE - EXAMPLE CALCULATION
 * ============================================================================
 * 
 * This document walks through a complete example of how the upgraded
 * scoring engine calculates a realistic recommendation score.
 * 
 * Real-world scenario:
 *   User: Sarah, moving with 1-year-old child to Bangalore
 *   Profile: Working mother, values safety highly, moderate budget
 *   Preferences: Safety (weight 9), Healthcare (weight 8), Cost (weight 7),
 *                Job (weight 5), Transport (weight 4), Lifestyle (weight 2),
 *                Environment (weight 3)
 * 
 * ============================================================================
 */

// EXAMPLE LOCALITY: "Whitefield, Bangalore"
Locality locality = new Locality(
    "WF001",
    "Whitefield",
    "Bangalore",
    "Karnataka",
    30000,        // avgRent (₹/month)
    7.5,          // jobIndex (0-10)
    8.2,          // hospitalRating (0-10)
    6.8,          // transportScore (0-10)
    7.9,          // safetyScore (0-10)
    45,           // pollutionIndex (0-100)
    7.2,          // lifestyleScore (0-10)
    8500,         // populationDensity (people/sq km)
    "IT hub with good facilities..."
);

UserPreferences preferences = new UserPreferences();
preferences.setBudget(600000);        // ₹600k annual budget
preferences.setFamilySize(2);          // Sarah + baby
preferences.setWorkType("On-site");
preferences.setSafetyWeight(9);
preferences.setHealthcareWeight(8);
preferences.setCostOfLivingWeight(7);
preferences.setJobWeight(5);
preferences.setTransportWeight(4);
preferences.setEnvironmentWeight(3);
preferences.setLifestyleWeight(2);

// ============================================================================
// PHASE 1: NORMALIZATION
// ============================================================================

/*
 * Each metric is normalized to 0-10 scale using appropriate methods:
 */

Map<String, Double> normalized = new HashMap<>();

// Job Opportunities: Already 0-10
normalized.put("Job Opportunities", 7.5);  // No conversion needed

// Cost of Living: Normalize based on budget affordability
// Annual rent: 30,000 × 12 = ₹360,000
// Budget: ₹600,000
// Ratio: 360,000 / 600,000 = 0.60 (60% of budget)
// This is in "Good" range (0.50-0.75), so score = 7.0
normalized.put("Cost of Living", 7.0);

// Healthcare: Already 0-10
normalized.put("Healthcare", 8.2);

// Transport: Already 0-10
normalized.put("Transport", 6.8);

// Safety: Already 0-10
normalized.put("Safety", 7.9);

// Environment: Inverse scoring (lower pollution = higher score)
// Pollution: 45 out of 100 max
// Normalized: 10 - (45/100 × 10) = 10 - 4.5 = 5.5
normalized.put("Environment", 5.5);

// Lifestyle: Already 0-10
normalized.put("Lifestyle", 7.2);

// ============================================================================
// PHASE 2: WEIGHTED BASE SCORE
// ============================================================================

/*
 * Calculate weighted score using Sarah's preferences
 * Formula: Σ (normalized_score × normalized_weight)
 */

double totalWeight = 9 + 8 + 7 + 5 + 4 + 3 + 2 = 38;

double jobContribution      = 7.5 × (5 / 38) = 0.987;
double costContribution     = 7.0 × (7 / 38) = 1.289;
double healthContribution   = 8.2 × (8 / 38) = 1.726;
double transportContribution = 6.8 × (4 / 38) = 0.716;
double safetyContribution   = 7.9 × (9 / 38) = 1.868;
double envContribution      = 5.5 × (3 / 38) = 0.434;
double lifestyleContribution = 7.2 × (2 / 38) = 0.379;

double baseScore = 0.987 + 1.289 + 1.726 + 0.716 + 1.868 + 0.434 + 0.379
                 = 7.40  (on 0-10 scale);

// ============================================================================
// PHASE 3: DECISION RULE MULTIPLIER
// ============================================================================

/*
 * Apply conditional rules based on user context
 */

double multiplier = 1.0;

// Rule 1: Remote Worker
// Sarah is ON-SITE, so no boost
multiplier *= 1.0;

// Rule 2: Large Family
// Sarah has familySize = 2, which is <= FAMILY_SIZE_CRITICAL_THRESHOLD (3)
// So no penalty from family rule
multiplier *= 1.0;

// Rule 3: Poor Transportation
// Transport score is 6.8, which is >= 4.0
// So no penalty from transport rule
multiplier *= 1.0;

// Rule 4: Weak Job Market
// Job score is 7.5, weight is 5 (not >= 6)
// So no penalty from job rule
multiplier *= 1.0;

baseScore *= multiplier;  // baseScore is still 7.40

// ============================================================================
// PHASE 4: CALCULATE PENALTIES
// ============================================================================

/*
 * Penalties reduce score based on real-world constraints
 */

double totalPenalties = 0.0;
List<String> penaltyReasons = new ArrayList<>();

// Safety Penalty:
// Safety score is 7.9, which is >= MINIMUM_SAFETY_THRESHOLD (4.0)
// Also >= 5.0 (not in "warning" zone)
// Result: No safety penalty (0.0)
totalPenalties += 0.0;

// Budget Overflow Penalty:
// Annual rent: 360,000
// Budget: 600,000
// Ratio: 0.60 (60% of budget, GOOD fit, not over)
// Result: No penalty (0.0)
totalPenalties += 0.0;

// Pollution Penalty:
// Pollution index: 45 out of 100
// Normalized: 0.45
// With exponent 1.3: 0.45^1.3 = 0.357
// Penalty: 0.357 × MAX_POLLUTION_PENALTY (2.5) = 0.893
totalPenalties += 0.893;
penaltyReasons.add("Air quality: pollution index 45 requires environmental penalty");

// Density Penalty:
// Density: 8500 people/sq km
// Threshold: 5000
// Excess: 3500
// Max excess: 15000 - 5000 = 10000
// Fraction: 3500 / 10000 = 0.35
// Penalty: 0.35 × HIGH_DENSITY_PENALTY (0.3) = 0.105
totalPenalties += 0.105;
penaltyReasons.add("Population density: 8500 people/km² may feel crowded");

// Transportation Penalty:
// Transport score: 6.8, which is >= 4.0
// Result: No transport penalty (0.0)
totalPenalties += 0.0;

// Job Mismatch Penalty:
// Work type is ON-SITE
// Job score is 7.5, which is >= 3.0
// Job weight is 5, which is < 6
// Result: No job penalty (0.0)
totalPenalties += 0.0;

// Healthcare Mismatch Penalty:
// Family size: 2, which is <= FAMILY_SIZE_CRITICAL_THRESHOLD (3)
// Result: No healthcare penalty (0.0)
totalPenalties += 0.0;

System.out.println("Total Penalties: " + totalPenalties + " (0.998)");

// ============================================================================
// PHASE 5: CALCULATE BONUSES
// ============================================================================

/*
 * Bonuses reward excellent factor combinations
 */

double totalBonuses = 0.0;
List<String> bonusReasons = new ArrayList<>();

// Safety + Healthcare Bonus:
// Safety: 7.9, Healthcare: 8.2
// Safety must be >= SAFETY_BONUS_THRESHOLD (8.5): NO (7.9 < 8.5)
// Result: No bonus (0.0)
totalBonuses += 0.0;

// Safety + Environment Bonus:
// Safety: 7.9, Pollution: 45
// Safety must be >= SAFETY_BONUS_THRESHOLD (8.5): NO
// Result: No bonus (0.0)
totalBonuses += 0.0;

// Budget Match Bonus:
// Rent as % of budget: 60% (0.60)
// Target range: 20-35% (0.20-0.35)
// 60% is outside range (too high)
// Result: No bonus (0.0)
totalBonuses += 0.0;

// Balance Bonus:
// Calculate variance of scores:
//   Scores: [7.5, 7.0, 8.2, 6.8, 7.9, 5.5, 7.2]
//   Mean: 7.3
//   Variance: (0.04 + 0.09 + 0.81 + 0.25 + 0.36 + 3.24 + 0.01) / 7 = 0.697
//   Std Dev: 0.835
// Balance threshold: 1.5
// Std Dev (0.835) < Threshold (1.5): YES, scores are balanced!
// Result: BONUS awarded (0.6)
totalBonuses += 0.6;
bonusReasons.add("Balanced profile: all factors score well with low variance (0.84)");

// Remote Worker Bonus:
// Work type is ON-SITE
// Result: No bonus (0.0)
totalBonuses += 0.0;

// Excellent Locality Bonus:
// Count scores >= 7.5: Job(7.5), Healthcare(8.2), Safety(7.9), Lifestyle(7.2)
// That's 4 out of 7 = 57% (not >= 70%)
// Result: No bonus (0.0)
totalBonuses += 0.0;

System.out.println("Total Bonuses: " + totalBonuses + " (0.6)");

// ============================================================================
// PHASE 6: APPLY HYBRID FORMULA
// ============================================================================

/*
 * Combine base score, bonuses, penalties using weighted formula
 * Formula:
 *   finalScore = (baseScore × 0.70) + (bonuses × 0.20) - (penalties × 0.30)
 */

double weighted = baseScore * 0.70;           // 7.40 × 0.70 = 5.18
double bonusComponent = totalBonuses * 0.20;  // 0.60 × 0.20 = 0.12
double penaltyComponent = totalPenalties * 0.30;  // 0.998 × 0.30 = 0.299

double finalScore = weighted + bonusComponent - penaltyComponent;
                  = 5.18 + 0.12 - 0.299
                  = 5.001

// Clamp to [0, 10]
finalScore = Math.max(0, Math.min(10, 5.001)) = 5.00

// ============================================================================
// FINAL RESULT
// ============================================================================

System.out.println("\n========== SCORING BREAKDOWN ==========");
System.out.println("Base Score (weighted):     7.40/10");
System.out.println("Decision Rules Multiplier: 1.0x");
System.out.println("Total Penalties:          -0.998");
System.out.println("  - Pollution: -0.893");
System.out.println("  - Density: -0.105");
System.out.println("Total Bonuses:            +0.60");
System.out.println("  - Balanced Profile: +0.60");
System.out.println("");
System.out.println("Hybrid Formula:");
System.out.println("  = (7.40 × 0.7) + (0.60 × 0.2) - (0.998 × 0.3)");
System.out.println("  = 5.18 + 0.12 - 0.299");
System.out.println("  = 5.00");
System.out.println("");
System.out.println("FINAL SCORE:              5.00/10");
System.out.println("");
System.out.println("========== EXPLANATION ==========");
System.out.println("Recommendation Summary:");
System.out.println("");
System.out.println("Strengths:");
System.out.println("• Excellent healthcare facilities");
System.out.println("• Very safe neighborhood");
System.out.println("• Affordable living costs");
System.out.println("• Vibrant lifestyle and entertainment");
System.out.println("");
System.out.println("Bonuses Earned:");
System.out.println("✓ Balanced profile: all factors score well");
System.out.println("");
System.out.println("Concerns:");
System.out.println("⚠ Air quality: pollution index 45 requires environmental penalty");
System.out.println("⚠ Population density: 8500 people/km² may feel crowded");

// ============================================================================
// INTERPRETATION
// ============================================================================

/*
 * Score of 5.0 means:
 * 
 * VERDICT: MODERATE - CONSIDER WITH CAUTION
 * 
 * WHY:
 * Whitefield has many positives (safety, healthcare, affordability, balance)
 * but environmental quality (pollution, density) is a concern for a young family.
 * 
 * RECOMMENDATION:
 * - Not a top choice due to air quality issues
 * - Would benefit from comparison with cleaner areas
 * - If Sarah doesn't mind pollution/density, this is acceptable
 * - Healthcare and safety make it workable for a family
 * 
 * COMPARISON:
 * If scoring multiple localities:
 *   - Whitefield (Moderate pollution):  5.0
 *   - Indiranagar (Better air quality): ~6.2
 *   - Electronic City (Lower density):  ~6.5
 *   - Koramangala (Excellent balance):  ~7.1
 * 
 * DECISION-AWARE:
 * The system identified that Sarah is:
 * - A young mother (family size 2) → prioritizes safety and healthcare ✓
 * - On-site worker → needs decent jobs and transport
 * - Budget-conscious → value for money
 * 
 * And Whitefield delivered on 1 & 3 well, but environment is weak.
 * This realistic scoring would help Sarah make an informed decision.
 * 
 * ============================================================================
 */
