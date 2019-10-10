package com.maximintegrated.algorithm_hrv;

import androidx.annotation.NonNull;

public class HrvAlgorithmInput {

    private float ibi;
    private int ibiConfidence;
    private boolean isIbiValid;

    public HrvAlgorithmInput() {
    }

    public HrvAlgorithmInput(float ibi, int ibiConfig, boolean isIbiValid) {
        this.ibi = ibi;
        this.ibiConfidence = ibiConfig;
        this.isIbiValid = isIbiValid;
    }

    public float getIbi() {
        return ibi;
    }

    public void setIbi(float ibi) {
        this.ibi = ibi;
    }

    public int getIbiConfidence() {
        return ibiConfidence;
    }

    public void setIbiConfidence(int ibiConfidence) {
        this.ibiConfidence = ibiConfidence;
    }

    public boolean isIbiValid() {
        return isIbiValid;
    }

    public void setIbiValid(boolean ibiValid) {
        isIbiValid = ibiValid;
    }

    @NonNull
    @Override
    public String toString() {
        return "HrvAlgorithmInput{" +
                "ibi=" + ibi +
                ", ibiConfidence=" + ibiConfidence +
                ", isIbiValid=" + isIbiValid +
                '}';
    }
}
