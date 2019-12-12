package com.maximintegrated.algorithms;

import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig;
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmInitConfig;
import com.maximintegrated.algorithms.sleep.SleepAlgorithmInitConfig;

public class AlgorithmInitConfig {

    private int enableAlgorithmsFlag;
    private HrvAlgorithmInitConfig hrvConfig;
    private RespiratoryRateAlgorithmInitConfig respConfig;
    private byte stressConfig;
    private SleepAlgorithmInitConfig sleepConfig;

    public AlgorithmInitConfig() {
        enableAlgorithmsFlag = 0;
        hrvConfig = new HrvAlgorithmInitConfig();
        respConfig = new RespiratoryRateAlgorithmInitConfig();
        stressConfig = 0;
        sleepConfig = new SleepAlgorithmInitConfig();
    }

    public int getEnableAlgorithmsFlag() {
        return enableAlgorithmsFlag;
    }

    public void setEnableAlgorithmsFlag(int enableAlgorithmsFlag) {
        this.enableAlgorithmsFlag = enableAlgorithmsFlag;
    }

    public HrvAlgorithmInitConfig getHrvConfig() {
        return hrvConfig;
    }

    public void setHrvConfig(HrvAlgorithmInitConfig hrvConfig) {
        this.hrvConfig = hrvConfig;
    }

    public RespiratoryRateAlgorithmInitConfig getRespConfig() {
        return respConfig;
    }

    public void setRespConfig(RespiratoryRateAlgorithmInitConfig respConfig) {
        this.respConfig = respConfig;
    }

    public byte getStressConfig() {
        return stressConfig;
    }

    public void setStressConfig(byte stressConfig) {
        this.stressConfig = stressConfig;
    }

    public SleepAlgorithmInitConfig getSleepConfig() {
        return sleepConfig;
    }

    public void setSleepConfig(SleepAlgorithmInitConfig sleepConfig) {
        this.sleepConfig = sleepConfig;
    }
}
