package com.maximintegrated.algorithm_respiratory_rate;

public class RespiratoryRateAlgorithm {

    public static final int CHANNEL_COUNT = 3;
    public static final int CHANNEL_ORDER_IR = 1;
    public static final int CHANNEL_ORDER_RED = 2;
    public static final int CHANNEL_ORDER_GREEN = 0;

    static {
        System.loadLibrary("RespiratoryRateAlgorithm");
    }

    public RespiratoryRateAlgorithm() {
    }

    public static boolean init(RespiratoryRateAlgorithmInitConfig initConfig) {
        return init(initConfig.getSourceOptions().value, initConfig.getLedCodes().value, initConfig.getSamplingRateOption().value);
    }

    public static boolean run(RespiratoryRateAlgorithmInput respiratoryRateAlgorithmInput, RespiratoryRateAlgorithmOutput respiratoryRateAlgorithmOutput) {
        return run(respiratoryRateAlgorithmInput.getPpg(), respiratoryRateAlgorithmInput.getIbi(), respiratoryRateAlgorithmInput.getIbiConfidence(),
                respiratoryRateAlgorithmInput.isPpgUpdateFlag(), respiratoryRateAlgorithmInput.isIbiUpdateFlag(),respiratoryRateAlgorithmOutput);
    }

    public static native boolean init(int sourceOptions, int ledCodes, int samplingRateOption);

    public static native boolean run(float ppg, float ibi, float ibiConfidence,boolean ppgUpdateFlag, boolean ibiUpdateFlag, RespiratoryRateAlgorithmOutput respiratoryRateAlgorithmOutput);

    public static native boolean end();
}
