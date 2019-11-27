package com.maximintegrated.algorithm_stress;

import com.maximintegrated.algorithm_hrv.HrvAlgorithmOutput;

public class StressAlgorithmInput {

    private HrvAlgorithmOutput hrvAlgorithmOutput;

    public StressAlgorithmInput(HrvAlgorithmOutput hrvAlgorithmOutput) {
        this.hrvAlgorithmOutput = hrvAlgorithmOutput;
    }

    public HrvAlgorithmOutput getHrvAlgorithmOutput() {
        return hrvAlgorithmOutput;
    }

    public void setHrvAlgorithmOutput(HrvAlgorithmOutput hrvAlgorithmOutput) {
        this.hrvAlgorithmOutput = hrvAlgorithmOutput;
    }

    public float[] getFloatArray() {
        return new float[]{
                hrvAlgorithmOutput.getTimeDomainHrvMetrics().getAvnn(),
                hrvAlgorithmOutput.getTimeDomainHrvMetrics().getSdnn(),
                hrvAlgorithmOutput.getTimeDomainHrvMetrics().getRmssd(),
                hrvAlgorithmOutput.getTimeDomainHrvMetrics().getPnn50(),
                hrvAlgorithmOutput.getFreqDomainHrvMetrics().getUlf(),
                hrvAlgorithmOutput.getFreqDomainHrvMetrics().getVlf(),
                hrvAlgorithmOutput.getFreqDomainHrvMetrics().getLf(),
                hrvAlgorithmOutput.getFreqDomainHrvMetrics().getHf(),
                hrvAlgorithmOutput.getFreqDomainHrvMetrics().getTotPwr()
        };
    }
}
