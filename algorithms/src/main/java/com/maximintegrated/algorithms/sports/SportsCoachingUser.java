package com.maximintegrated.algorithms.sports;

public class SportsCoachingUser {

    public enum Gender {
        MALE(0),
        FEMALE(1);
        public final int value;

        Gender(int value) {
            this.value = value;
        }
    }

    private String userName = "";
    private int birthYear = 1970;
    private Gender gender = Gender.MALE;
    private int weight = 80;
    private int height = 180;
    private boolean isMetric = true;

    public SportsCoachingUser() {

    }

    public SportsCoachingUser(String userName, int birthYear, Gender gender, int weight, int height, boolean isMetric) {
        this.userName = userName;
        this.birthYear = birthYear;
        this.gender = gender;
        this.weight = weight;
        this.height = height;
        this.isMetric = isMetric;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isMetric() {
        return isMetric;
    }

    public void setMetric(boolean metric) {
        isMetric = metric;
    }
}
