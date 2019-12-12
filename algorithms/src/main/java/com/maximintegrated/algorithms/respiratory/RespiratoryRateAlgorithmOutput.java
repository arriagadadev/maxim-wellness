package com.maximintegrated.algorithms.respiratory;

import androidx.annotation.NonNull;

public class RespiratoryRateAlgorithmOutput {
    private float respirationRate;
    private float confidenceLevel;

    public RespiratoryRateAlgorithmOutput() {
    }

    public RespiratoryRateAlgorithmOutput(float respirationRate, float confidenceLevel) {
        this.respirationRate = respirationRate;
        this.confidenceLevel = confidenceLevel;
    }

    public float getRespirationRate() {
        return respirationRate;
    }

    public void setRespirationRate(float respirationRate) {
        this.respirationRate = respirationRate;
    }

    public float getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(float confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public void update(float respirationRate, float confidenceLevel) {
        this.respirationRate = respirationRate;
        this.confidenceLevel = confidenceLevel;
    }

    @NonNull
    @Override
    public String toString() {
        return "RespiratoryRateAlgorithmOutput{" +
                "respirationRate=" + respirationRate +
                ", confidenceLevel=" + confidenceLevel +
                '}';
    }
}
