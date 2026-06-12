package com.smartcity.service;

import org.junit.Assert;
import org.junit.Test;

public class DataAggregatorTest {

    @Test
    public void blendMetricUsesLocalityAndLiveValues() {
        double blended = DataAggregator.blendMetric(8.0, 9.2);

        Assert.assertEquals(8.36, blended, 0.001);
    }

    @Test
    public void blendMetricFallsBackToLocalityValueWhenLiveValueIsUnavailable() {
        double blended = DataAggregator.blendMetric(8.0, -1.0);

        Assert.assertEquals(8.0, blended, 0.001);
    }
}
