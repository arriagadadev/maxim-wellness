package com.maximintegrated.algorithms;

import com.maximintegrated.algorithms.hrv.HrvAlgorithmInitConfig;
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmInitConfig;
import com.maximintegrated.algorithms.sleep.SleepAlgorithmInitConfig;

public class MaximAlgorithms {

    public static final int FLAG_HRV = (1 << 0);
    public static final int FLAG_RESP = (1 << 1);
    public static final int FLAG_SLEEP = (1 << 2);
    public static final int FLAG_STRESS = (1 << 3);
    public static final int FLAG_SPORTS = (1 << 4);

    static {
        System.loadLibrary("MaximAlgorithms");
    }

    public static boolean init(AlgorithmInitConfig algorithmInitConfig) {
        HrvAlgorithmInitConfig hrv = algorithmInitConfig.getHrvConfig();
        RespiratoryRateAlgorithmInitConfig resp = algorithmInitConfig.getRespConfig();
        SleepAlgorithmInitConfig sleep = algorithmInitConfig.getSleepConfig();
        byte stress = algorithmInitConfig.getStressConfig();
        return init(algorithmInitConfig.getEnableAlgorithmsFlag(), hrv.getSamplingPeriod(), hrv.getWindowSizeInSec(),
                hrv.getWindowShiftSizeInSec(), resp.getSourceOptions().value, resp.getLedCodes().value,
                resp.getSamplingRateOption().value, stress, sleep.getDetectableSleepDuration().value,
                sleep.getUserInfo().getAge(), sleep.getUserInfo().getWeight(), sleep.getUserInfo().getGender().value, sleep.getUserInfo().getRestingHr());
    }

    public static boolean run(AlgorithmInput input, AlgorithmOutput output) {
        return run(input.getSampleCount(), input.getGreen(), input.getGreen2(), input.getIr(), input.getRed(),
                input.getAccelerationX(), input.getAccelerationY(), input.getAccelerationZ(),
                input.getOperationMode(), input.getHr(), input.getHrConfidence(), input.getRr(),
                input.getRrConfidence(), input.getActivity(), input.getR(), input.getWspo2Confidence(),
                input.getSpo2(), input.getWspo2PercentageComplete(), input.getWspo2LowSnr(),
                input.getWspo2Motion(), input.getWspo2LowPi(), input.getWspo2UnreliableR(),
                input.getWspo2State(), input.getScdState(), input.getWalkSteps(),
                input.getRunSteps(), input.getKCal(), input.getTotalActEnergy(), input.getTimeStampUpper(),
                input.getTimeStampLower(), output);
    }

    public static native boolean init(int enableFlag, float hrvSamplingPeriod, int hrvWindowSizeInSec,
                                      int hrvWindowShiftSizeInSec, int respSource, int respLedCode,
                                      int respSamplingRate, int stressConfig, int sleepDuration, int age, int weight, int gender, float restingHr);

    public static native boolean run(int sampleCount, int green, int green2, int ir, int red,
                                     int accelerationX, int accelerationY, int accelerationZ,
                                     int operationMode, int hr, int hrConfidence, int rr,
                                     int rrConfidence, int activity, int r, int wspo2Confidence,
                                     int spo2, int wspo2PercentageComplete, int wspo2LowSnr,
                                     int wspo2Motion, int wspo2LowPi, int wspo2UnreliableR,
                                     int wspo2State, int scdState, int walkSteps, int runSteps,
                                     int kCal, int totalActEnergy, int timestampUpper, int timestampLower, AlgorithmOutput output);


    public static native boolean end(int disableFlag);

    public static native float calculateSQI(float deepInSec, float remInSec, float inSleepWakeInSec, int numberOfWakeInSleep);

    public static native void getVersion(AlgorithmVersion version);

    public static native byte[] getAuthInitials(byte[] authInits);

    public static native void authenticate(byte[] array1, byte[] array2);
}
