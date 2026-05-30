package com.smartcity.model;

/**
 * Represents user preferences and constraints for locality recommendations.
 */
public class UserPreferences {
    private String userId;
    private String selectedCity;
    private double budget;
    private int familySize;
    private String workType; // "Remote" or "On-site"
    
    // Weight preferences (0-10 scale)
    private double jobWeight;
    private double costOfLivingWeight;
    private double healthcareWeight;
    private double transportWeight;
    private double safetyWeight;
    private double environmentWeight;
    private double lifestyleWeight;
    private double minimumSafetyThreshold;
    private double budgetMatchThreshold;

    // Profile type
    private String profileType; // "Student", "Bachelor", "Family", "Custom"

    public UserPreferences() {
        this.jobWeight = 5;
        this.costOfLivingWeight = 5;
        this.healthcareWeight = 5;
        this.transportWeight = 5;
        this.safetyWeight = 5;
        this.environmentWeight = 5;
        this.lifestyleWeight = 5;
        this.minimumSafetyThreshold = 5.0;
        this.budgetMatchThreshold = 70.0;
        this.profileType = "Custom";
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSelectedCity() {
        return selectedCity;
    }

    public void setSelectedCity(String selectedCity) {
        this.selectedCity = selectedCity;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public int getFamilySize() {
        return familySize;
    }

    public void setFamilySize(int familySize) {
        this.familySize = familySize;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public double getJobWeight() {
        return jobWeight;
    }

    public void setJobWeight(double jobWeight) {
        this.jobWeight = Math.max(0, Math.min(10, jobWeight));
    }

    public double getCostOfLivingWeight() {
        return costOfLivingWeight;
    }

    public void setCostOfLivingWeight(double costOfLivingWeight) {
        this.costOfLivingWeight = Math.max(0, Math.min(10, costOfLivingWeight));
    }

    public double getHealthcareWeight() {
        return healthcareWeight;
    }

    public void setHealthcareWeight(double healthcareWeight) {
        this.healthcareWeight = Math.max(0, Math.min(10, healthcareWeight));
    }

    public double getTransportWeight() {
        return transportWeight;
    }

    public void setTransportWeight(double transportWeight) {
        this.transportWeight = Math.max(0, Math.min(10, transportWeight));
    }

    public double getSafetyWeight() {
        return safetyWeight;
    }

    public void setSafetyWeight(double safetyWeight) {
        this.safetyWeight = Math.max(0, Math.min(10, safetyWeight));
    }

    public double getEnvironmentWeight() {
        return environmentWeight;
    }

    public void setEnvironmentWeight(double environmentWeight) {
        this.environmentWeight = Math.max(0, Math.min(10, environmentWeight));
    }

    public double getLifestyleWeight() {
        return lifestyleWeight;
    }

    public void setLifestyleWeight(double lifestyleWeight) {
        this.lifestyleWeight = Math.max(0, Math.min(10, lifestyleWeight));
    }

    public double getMinimumSafetyThreshold() {
        return minimumSafetyThreshold;
    }

    public void setMinimumSafetyThreshold(double minimumSafetyThreshold) {
        this.minimumSafetyThreshold = Math.max(0, Math.min(10, minimumSafetyThreshold));
    }

    public double getBudgetMatchThreshold() {
        return budgetMatchThreshold;
    }

    public void setBudgetMatchThreshold(double budgetMatchThreshold) {
        this.budgetMatchThreshold = Math.max(0, Math.min(100, budgetMatchThreshold));
    }

    public String getProfileType() {
        return profileType;
    }

    public void setProfileType(String profileType) {
        this.profileType = profileType;
    }

    /**
     * Apply preset weights based on profile type
     */
    public void applyProfilePreset(String profileType) {
        this.profileType = profileType;

        switch (profileType) {
            case "Student":
                this.costOfLivingWeight = 9;
                this.lifestyleWeight = 9;
                this.jobWeight = 3;
                this.transportWeight = 7;
                this.healthcareWeight = 5;
                this.safetyWeight = 8;
                this.environmentWeight = 4;
                break;
            case "Bachelor":
                this.jobWeight = 9;
                this.lifestyleWeight = 8;
                this.costOfLivingWeight = 6;
                this.transportWeight = 7;
                this.safetyWeight = 7;
                this.healthcareWeight = 5;
                this.environmentWeight = 5;
                break;
            case "Family":
                this.healthcareWeight = 9;
                this.safetyWeight = 9;
                this.costOfLivingWeight = 8;
                this.jobWeight = 7;
                this.transportWeight = 6;
                this.lifestyleWeight = 6;
                this.environmentWeight = 7;
                break;
            default: // Custom
                // Keep existing weights
                break;
        }
    }
}
