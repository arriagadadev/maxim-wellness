package com.maximintegrated.algorithm_stress;

public class StressAlgorithm {

    static {
        System.loadLibrary("StressAlgorithm");
    }

    public StressAlgorithm() {

    }

    public static boolean init() {
        return stress_init();
    }

    public static boolean run(StressAlgorithmInput input, StressAlgorithmOutput output) {
        return stress_run(input.getFloatArray(), output);
    }

    public static boolean end() {
        return stress_end();
    }

    public static native boolean stress_init();

    public static native boolean stress_run(float[] input, StressAlgorithmOutput output);

    public static native boolean stress_end();
}
