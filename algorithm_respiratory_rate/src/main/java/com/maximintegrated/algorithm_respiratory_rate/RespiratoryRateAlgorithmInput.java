package com.maximintegrated.algorithm_respiratory_rate;

import androidx.annotation.NonNull;

public class RespiratoryRateAlgorithmInput {
    private float ppg;
    private float ibi;
    private float ibiConfidence;
    private boolean ppgUpdateFlag;
    private boolean ibiUpdateFlag;

    public RespiratoryRateAlgorithmInput() {
    }

    public RespiratoryRateAlgorithmInput(float ppg, float ibi, float ibiConfidence, boolean ppgUpdateFlag, boolean ibiUpdateFlag) {
        this.ppg = ppg;
        this.ibi = ibi;
        this.ibiConfidence = ibiConfidence;
        this.ppgUpdateFlag = ppgUpdateFlag;
        this.ibiUpdateFlag = ibiUpdateFlag;
    }

    public float getPpg() {
        return ppg;
    }

    public void setPpg(float ppg) {
        this.ppg = ppg;
    }

    public float getIbi() {
        return ibi;
    }

    public void setIbi(float ibi) {
        this.ibi = ibi;
    }

    public float getIbiConfidence() {
        return ibiConfidence;
    }

    public void setIbiConfidence(float ibiConfidence) {
        this.ibiConfidence = ibiConfidence;
    }

    public boolean isPpgUpdateFlag() {
        return ppgUpdateFlag;
    }

    public void setPpgUpdateFlag(boolean ppgUpdateFlag) {
        this.ppgUpdateFlag = ppgUpdateFlag;
    }

    public boolean isIbiUpdateFlag() {
        return ibiUpdateFlag;
    }

    public void setIbiUpdateFlag(boolean ibiUpdateFlag) {
        this.ibiUpdateFlag = ibiUpdateFlag;
    }

    @NonNull
    @Override
    public String toString() {
        return "RespiratoryRateAlgorithmInput{" +
                "ppg=" + ppg +
                "ibi=" + ibi +
                ", ibiConfidence=" + ibiConfidence +
                ", ppgUpdateFlag=" + ppgUpdateFlag +
                ", ibiUpdateFlag=" + ibiUpdateFlag +
                '}';
    }
}
