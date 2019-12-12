package com.maximintegrated.algorithms.sleep;

public class SleepUserInfo {

    public enum Gender {
        MALE(0),
        FEMALE(1);

        public final int value;

        Gender(int value) {
            this.value = value;
        }
    }

    private int age;
    private int weight;
    private Gender gender;
    private float restingHr;

    public SleepUserInfo(int age, int weight, Gender gender, float restingHr) {
        this.age = age;
        this.weight = weight;
        this.gender = gender;
        this.restingHr = restingHr;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public float getRestingHr() {
        return restingHr;
    }

    public void setRestingHr(float restingHr) {
        this.restingHr = restingHr;
    }
}
