package com.maximintegrated.algorithm_sleep_qa;

public class SleepQaAlgorithmInput {
    private String inputPath;
    private String outputPath;
    private int age;
    private int height;
    private int weight;
    private int gender;
    private float restingHr;

    public SleepQaAlgorithmInput() {
    }

    public SleepQaAlgorithmInput(String inputPath, String outputPath, int age, int height, int weight, int gender, float restingHr) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.gender = gender;
        this.restingHr = restingHr;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public float getRestingHr() {
        return restingHr;
    }

    public void setRestingHr(float restingHr) {
        this.restingHr = restingHr;
    }

    public String getInputFileName() {
        String[] temp = this.inputPath.split("/");
        return temp[temp.length - 1];
    }
}
