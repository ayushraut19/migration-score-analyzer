package com.smartcity.model;

public enum LiveMetricType {
    JOBS("Jobs API"),
    AQI("AQI API"),
    TRANSPORT("Transport API"),
    HEALTHCARE("Healthcare API"),
    COST_OF_LIVING("Cost API");

    private final String displayName;

    LiveMetricType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
