package com.maximintegrated.algorithm_sleep_qa;

public class SleepQaAlgorithm {

    static {
        System.loadLibrary("SleepQaAlgorithm");
    }

    public SleepQaAlgorithm() {
    }

    public static boolean run(SleepQaAlgorithmInput sleepQaAlgorithmInput) {
        return run(sleepQaAlgorithmInput.getInputPath(), sleepQaAlgorithmInput.getOutputPath() + "/" + sleepQaAlgorithmInput.getInputFileName(), sleepQaAlgorithmInput.getAge(),
                sleepQaAlgorithmInput.getHeight(), sleepQaAlgorithmInput.getWeight(), sleepQaAlgorithmInput.getGender(), sleepQaAlgorithmInput.getRestingHr());
    }

    public static native boolean run(String inputPath, String outputPath, int age, int height, int weight, int gender, float restingHr);

}
