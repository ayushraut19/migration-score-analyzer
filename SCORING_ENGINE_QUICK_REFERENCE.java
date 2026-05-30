/**
 * ============================================================================
 * QUICK REFERENCE: Using the Decision-Aware Scoring Engine
 * ============================================================================
 */

// ============================================================================
// 1. BASIC USAGE: Calculate a single locality score
// ============================================================================

import com.smartcity.service.ScoringEngine;
import com.smartcity.model.*;

// Create instances
ScoringEngine engine = new ScoringEngine();
Locality locality = new Locality(...);
UserPreferences preferences = new UserPreferences(...);

// Calculate score
RecommendationResult result = engine.calculateScore(locality, preferences);

// Access results
double finalScore = result.getFinalScore();                              // 5.0
String explanation = result.getExplanation();                           // Full breakdown
Map<String, Double> breakdown = result.getScoreBreakdown();             // Factor scores

// NEW: Access detailed reasoning
List<String> penalties = result.getPenaltyReasons();                    // Why points lost
List<String> bonuses = result.getBonusReasons();                        // Why points earned
double baseScore = result.getBaseScore();                               // Before adjustments
double totalPenalties = result.getTotalPenalties();                     // Total deduction
double totalBonuses = result.getTotalBonuses();                         // Total addition

// ============================================================================
// 2. UNDERSTANDING PENALTIES
// ============================================================================

/*
 * Penalties are real-world constraints that reduce scores.
 * Each penalty has a documented reason.
 * 
 * Example output:
 */
for (String reason : result.getPenaltyReasons()) {
    System.out.println("⚠ " + reason);
}

// Output:
// ⚠ Safety concern: score 3.5 is below acceptable threshold (4.0)
// ⚠ Budget constraint: monthly rent exceeds annual budget by 25%
// ⚠ Air quality: pollution index 65 requires environmental penalty
// ⚠ Population density: 8500 people/km² may feel crowded

/*
 * Penalty types (from PenaltyCalculator):
 * 
 * 1. Safety Penalty (CRITICAL)
 *    - If safety < minimum (4.0): severe reduction
 *    - Rationale: Safety is non-negotiable
 *    - Max penalty: 3.0 points
 * 
 * 2. Budget Overflow Penalty
 *    - If rent > budget: proportional penalty
 *    - Rationale: Financial constraint is real
 *    - Max penalty: 3.0 points per 10% overflow
 * 
 * 3. Pollution Penalty (NON-LINEAR)
 *    - Formula: (pollution/100)^1.3 × maxPenalty
 *    - Rationale: Exponential because bad air compounds
 *    - Max penalty: 2.5 points
 * 
 * 4. Density Penalty
 *    - If population > 5000/km²: increasing penalty
 *    - Rationale: Overcrowding reduces quality of life
 *    - Max penalty: 0.3 points
 * 
 * 5. Transportation Penalty
 *    - If transport score < 4.0: penalty
 *    - Only for on-site workers
 *    - Max penalty: 1.0 point
 * 
 * 6. Job Market Penalty
 *    - If on-site worker with very low jobs
 *    - Max penalty: 1.5 points
 * 
 * 7. Healthcare Mismatch Penalty
 *    - For families > 3 with weak healthcare
 *    - Max penalty: 1.2 points
 */

// ============================================================================
// 3. UNDERSTANDING BONUSES
// ============================================================================

/*
 * Bonuses reward excellent combinations of factors.
 * Each bonus has a documented reason.
 * 
 * Example output:
 */
for (String reason : result.getBonusReasons()) {
    System.out.println("✓ " + reason);
}

// Output:
// ✓ Strong safety (8.5) and healthcare (8.2) combination provides excellent family security
// ✓ Excellent safety and clean environment (pollution: 25) create ideal living conditions
// ✓ Excellent budget match: rent (28% of budget) is comfortable and sustainable
// ✓ Balanced profile: all factors score well with low variance (0.85)

/*
 * Bonus types (from BonusCalculator):
 * 
 * 1. Safety + Healthcare Bonus (0.8 points)
 *    - When safety >= 8.5 AND healthcare >= 8.0
 *    - Perfect for families
 * 
 * 2. Safety + Environment Bonus (0.7 points)
 *    - When safety >= 8.5 AND pollution < 30
 *    - Comprehensive wellness indicator
 * 
 * 3. Budget Match Bonus (0.5 points)
 *    - When rent is 20-35% of budget
 *    - Sweet spot: comfortable not wasteful
 * 
 * 4. Balance Bonus (0.6 points)
 *    - When std dev of scores < 1.5
 *    - Rewards "good at everything"
 * 
 * 5. Remote Worker Bonus (0.4 points)
 *    - When work is REMOTE AND lifestyle/environment good
 *    - Lifestyle matters more for remote workers
 * 
 * 6. Excellent Overall Bonus (1.0 points)
 *    - When 70%+ of factors score >= 7.5
 *    - Truly outstanding localities
 */

// ============================================================================
// 4. UNDERSTANDING DECISION RULES
// ============================================================================

/*
 * Decision rules apply context-aware multipliers based on user profile.
 * Rules automatically adjust score importance.
 * 
 * From DecisionRulesEngine:
 */

// Rule 1: Remote Worker Rule
// If work type = REMOTE: multiplier = 1.05x
// Rationale: Jobs less important, lifestyle/environment more important
if ("Remote".equalsIgnoreCase(preferences.getWorkType())) {
    // Score will be boosted 5% because remote workers don't need local jobs
}

// Rule 2: Large Family Rule
// If family > 3 AND (healthcare OR safety weak): multiplier = 0.80x
// Rationale: Families need excellent services, vulnerabilities amplified
if (preferences.getFamilySize() > 3 && 
    (locality.getHospitalRating() < 5.0 || locality.getSafetyScore() < 5.0)) {
    // Score will be reduced 20% because family needs matter
}

// Rule 3: Poor Transport Rule
// If work = ON_SITE AND transport < 4.0: multiplier = 0.85x
// Rationale: Commuters suffer with bad transport
if (!"Remote".equalsIgnoreCase(preferences.getWorkType()) && 
    locality.getTransportScore() < 4.0) {
    // Score will be reduced 15% because commuting is painful
}

// Rule 4: Weak Job Market Rule
// If work = ON_SITE AND job < 2.0: multiplier = 0.75x
// Rationale: Job seekers can't afford weak market
if (!"Remote".equalsIgnoreCase(preferences.getWorkType()) && 
    preferences.getJobWeight() >= 7 && locality.getJobIndex() < 2.0) {
    // Score will be reduced 25% because job opportunities are critical
}

// Hard constraints filter out localities before scoring
// - Safety < 4.0: Don't recommend (too unsafe)
// - Rent > 150% of budget: Don't recommend (too expensive)

// ============================================================================
// 5. SCORING FORMULA BREAKDOWN
// ============================================================================

/*
 * Final Score = (BaseScore × 0.70) + (Bonuses × 0.20) - (Penalties × 0.30)
 * 
 * Meaning:
 * - 70% weight on core metrics (most important)
 * - 20% bonus for exceptional combinations (positive feedback)
 * - 30% penalty for constraints (negative feedback)
 * 
 * Example with numbers:
 */

double baseScore = result.getBaseScore();                   // e.g., 7.40
double penalties = result.getTotalPenalties();             // e.g., 0.998
double bonuses = result.getTotalBonuses();                 // e.g., 0.60

double finalCalculation = (baseScore * 0.70) + (bonuses * 0.20) - (penalties * 0.30);
//                       = (7.40 * 0.70) + (0.60 * 0.20) - (0.998 * 0.30)
//                       = 5.18 + 0.12 - 0.299
//                       = 5.00

// ============================================================================
// 6. ACCESSING SCORE BREAKDOWN BY FACTOR
// ============================================================================

Map<String, Double> breakdown = result.getScoreBreakdown();

double jobScore = breakdown.get("Job Opportunities");        // 0-10
double costScore = breakdown.get("Cost of Living");          // 0-10
double healthScore = breakdown.get("Healthcare");            // 0-10
double transportScore = breakdown.get("Transport");          // 0-10
double safetyScore = breakdown.get("Safety");                // 0-10
double envScore = breakdown.get("Environment");              // 0-10
double lifestyleScore = breakdown.get("Lifestyle");          // 0-10

// These are normalized (0-10), independent of original data ranges
System.out.println("All factors on 0-10 scale:");
for (String factor : breakdown.keySet()) {
    System.out.printf("%s: %.1f\n", factor, breakdown.get(factor));
}

// ============================================================================
// 7. GENERATING DETAILED EXPLANATIONS FOR UI
// ============================================================================

// Option 1: Simple explanation (what's shown now)
String simple = result.getExplanation();

// Option 2: Detailed breakdown for tooltip/details panel
StringBuilder detailed = new StringBuilder();
detailed.append("Score Calculation:\n");
detailed.append("  Base: ").append(String.format("%.2f", result.getBaseScore())).append("\n");
detailed.append("  Bonuses: +").append(String.format("%.2f", result.getTotalBonuses())).append("\n");
detailed.append("  Penalties: -").append(String.format("%.2f", result.getTotalPenalties())).append("\n");
detailed.append("  Final: ").append(String.format("%.2f", result.getFinalScore())).append("/10\n");

if (!result.getPenaltyReasons().isEmpty()) {
    detailed.append("\nConcerns:\n");
    for (String reason : result.getPenaltyReasons()) {
        detailed.append("  ⚠ ").append(reason).append("\n");
    }
}

if (!result.getBonusReasons().isEmpty()) {
    detailed.append("\nStrengths:\n");
    for (String reason : result.getBonusReasons()) {
        detailed.append("  ✓ ").append(reason).append("\n");
    }
}

// ============================================================================
// 8. NORMALIZING INPUT METRICS
// ============================================================================

import com.smartcity.utils.NormalizationUtil;

// If you have metrics in different scales, normalize them:

// Metric 1: Already 0-10
double normalized1 = NormalizationUtil.normalize0To10(8.5);  // Returns 8.5

// Metric 2: Linear scaling from custom range
double normalized2 = NormalizationUtil.normalizeLinear(
    15,      // value
    0,       // minValue
    30       // maxValue
);  // Returns 5.0 (15 is midpoint of 0-30)

// Metric 3: Rent affordability (special case)
double normalized3 = NormalizationUtil.normalizeBudgetAffordability(
    30000,   // monthlyRent
    600000   // annualBudget
);  // Returns 7.0 (60% of budget is "good")

// Metric 4: Pollution (inverse scoring)
double normalized4 = NormalizationUtil.normalizePollution(
    45,      // pollutionIndex
    100      // maxPollution
);  // Returns 5.5 (lower is better)

// Metric 5: Population density (inverse scoring)
double normalized5 = NormalizationUtil.normalizeDensity(
    8500,    // populationDensity
    15000    // maxDensity
);  // Returns 4.3 (lower density is better)

// ============================================================================
// 9. TUNING THE SCORING ENGINE
// ============================================================================

import com.smartcity.service.ScoringConfig;

/*
 * All tuning parameters are in ScoringConfig.
 * To adjust scoring behavior, modify these constants:
 */

// Example 1: Make penalties less aggressive
// Current: POLLUTION_PENALTY_EXPONENT = 1.3
// Change to: POLLUTION_PENALTY_EXPONENT = 1.0

// Example 2: Require higher minimum safety
// Current: MINIMUM_SAFETY_THRESHOLD = 4.0
// Change to: MINIMUM_SAFETY_THRESHOLD = 5.0

// Example 3: Increase remote worker bonus
// Current: REMOTE_WORKER_BONUS_MULTIPLIER = 1.05
// Change to: REMOTE_WORKER_BONUS_MULTIPLIER = 1.10

// Example 4: Make balance more important
// Current: BALANCED_SCORE_BONUS = 0.6
// Change to: BALANCED_SCORE_BONUS = 1.0

// Print current configuration:
System.out.println(ScoringConfig.getConfigurationSummary());

// ============================================================================
// 10. HANDLING EDGE CASES
// ============================================================================

// Edge case 1: No budget constraint
preferences.setBudget(0);  // Signal "no budget limit"
// Cost of living score will be neutral (5.0)

// Edge case 2: Very low safety score
locality.setSafetyScore(2.0);
// Hard constraint will filter out immediately
// Won't even calculate full score

// Edge case 3: Extremely high pollution
locality.setPollutionIndex(95);
// Non-linear penalty: (0.95^1.3)^1.3 = aggressive penalty

// Edge case 4: Perfect score across all factors
// All factors = 10.0
// Excellent Overall Bonus will be applied (+1.0)
// Final score can exceed 10, will be clamped to 10

// Edge case 5: Single weak factor (e.g., safety = 3.0, others = 9.0)
// Safety penalty will significantly reduce score
// Won't get Balance Bonus (high variance)
// Result: Fair punishment for critical weakness

// ============================================================================
// 11. COMMON FORMULAS FOR UI DISPLAY
// ============================================================================

// Show score as bar chart
double score = result.getFinalScore();
int barLength = (int) score;
System.out.println("Score: [" + "█".repeat(barLength) + "░".repeat(10 - barLength) + "] " + score);

// Show factor breakdown as mini-bars
Map<String, Double> breakdown = result.getScoreBreakdown();
for (String factor : breakdown.keySet()) {
    double factorScore = breakdown.get(factor);
    int factorBar = (int) factorScore;
    System.out.printf("%s: [%s%s] %.1f\n", 
        factor, 
        "█".repeat(factorBar), 
        "░".repeat(10 - factorBar), 
        factorScore);
}

// Show score change from base
double baseScore = result.getBaseScore();
double finalScore = result.getFinalScore();
double change = finalScore - baseScore;
String changeSymbol = change >= 0 ? "+" : "";
System.out.printf("Base: %.2f %s %s%.2f = Final: %.2f\n", 
    baseScore, 
    (change >= 0 ? "↑" : "↓"), 
    changeSymbol, 
    change, 
    finalScore);

// ============================================================================
// 12. TESTING DIFFERENT USER PROFILES
// ============================================================================

// Test Profile 1: Young mother (family priorities)
UserPreferences family = new UserPreferences();
family.setFamilySize(3);
family.setSafetyWeight(9);
family.setHealthcareWeight(8);
family.setCostOfLivingWeight(7);
family.setJobWeight(5);
// Expected: High penalties for weak healthcare/safety

// Test Profile 2: Remote worker (lifestyle priorities)
UserPreferences remote = new UserPreferences();
remote.setWorkType("Remote");
remote.setLifestyleWeight(8);
remote.setEnvironmentWeight(7);
remote.setJobWeight(2);  // Jobs not important
// Expected: Good remote worker bonus, location with good lifestyle

// Test Profile 3: Budget-constrained job seeker
UserPreferences jobSeeker = new UserPreferences();
jobSeeker.setBudget(300000);  // Very tight budget
jobSeeker.setJobWeight(9);
jobSeeker.setSafetyWeight(6);
jobSeeker.setCostOfLivingWeight(8);
jobSeeker.setWorkType("On-site");
// Expected: Heavy penalties for budget overruns, weak job markets

// Test Profile 4: Wealthy professional
UserPreferences wealthy = new UserPreferences();
wealthy.setBudget(2000000);  // High budget
wealthy.setLifestyleWeight(8);
wealthy.setEnvironmentWeight(9);
wealthy.setSafetyWeight(7);
wealthy.setJobWeight(5);
// Expected: Cost of living less of an issue, environment matters more

// ============================================================================
// CONCLUSION
// 
// The Decision-Aware Scoring Engine:
// 1. Normalizes all metrics to consistent 0-10 scale
// 2. Calculates weighted base score
// 3. Applies context-aware decision rules as multipliers
// 4. Applies penalties for real-world constraints
// 5. Applies bonuses for excellent combinations
// 6. Combines all factors using hybrid formula
// 7. Provides full explainability
// 
// Result: Realistic, human-like decision making
// ============================================================================
