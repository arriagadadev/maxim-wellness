package com.maximintegrated.algorithm_hrv;

public final class HrvAlgorithm {

    static {
        System.loadLibrary("HrvAlgorithm");
    }

    public HrvAlgorithm() {
    }

    public static boolean init(HrvAlgorithmInitConfig initConfig){
        return init(initConfig.getSamplingPeriod(), initConfig.getWindowSizeInSec(), initConfig.getWindowShiftSizeInSec());
    }

    public static boolean run(HrvAlgorithmInput hrvAlgorithmInput, HrvAlgorithmOutput hrvAlgorithmOutput){
        return run(hrvAlgorithmInput.getIbi(), hrvAlgorithmInput.getIbiConfidence(), hrvAlgorithmInput.isIbiValid(), hrvAlgorithmOutput);
    }

    public static native boolean init(float samplingPeriod, short windowSizeInSec, short windowShiftSizeInSec);

    public static native boolean run(float ibi, int ibiConfig, boolean isIbiValid, HrvAlgorithmOutput output);

    public static native boolean end();
}
