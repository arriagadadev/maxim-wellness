package com.maximintegrated.algorithms.sleep;

public class SleepAlgorithmOutput {

    public enum SleepWakeDecisionStatus {
        NOT_CALCULATED(0),
        CALCULATED(1);

        int value;

        SleepWakeDecisionStatus(int value) {
            this.value = value;
        }
    }

    public enum SleepWakeDecision {
        WAKE(0),
        RESTLESS(1),
        SLEEP(2);

        int value;

        SleepWakeDecision(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum SleepPhaseOutputStatus {
        NOT_CALCULATED(0),
        READY(1);

        int value;

        SleepPhaseOutputStatus(int value) {
            this.value = value;
        }
    }

    public enum SleepPhaseOutput {
        UNDEFINED(-1),
        WAKE(0),
        REM(2),
        LIGHT(3),
        DEEP(4);
        int value;

        SleepPhaseOutput(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private int sleepWakeDecisionStatus;
    private int sleepWakeDecision;
    private int sleepWakeDetentionLatency;
    private float sleepWakeOutputConfLevel;
    private int sleepPhaseOutputStatus;
    private int sleepPhaseOutput;
    private float sleepPhaseOutputConfLevel;
    private float hr;
    private float accMag;
    private float ibi;

    public SleepAlgorithmOutput(int sleepWakeDecisionStatus, int sleepWakeDecision, int sleepWakeDetentionLatency, float sleepWakeOutputConfLevel, int sleepPhaseOutputStatus, int sleepPhaseOutput, float sleepPhaseOutputConfLevel, float hr, float accMag, float ibi) {
        this.sleepWakeDecisionStatus = sleepWakeDecisionStatus;
        this.sleepWakeDecision = sleepWakeDecision;
        this.sleepWakeDetentionLatency = sleepWakeDetentionLatency;
        this.sleepWakeOutputConfLevel = sleepWakeOutputConfLevel;
        this.sleepPhaseOutputStatus = sleepPhaseOutputStatus;
        this.sleepPhaseOutput = sleepPhaseOutput;
        this.sleepPhaseOutputConfLevel = sleepPhaseOutputConfLevel;
        this.hr = hr;
        this.accMag = accMag;
        this.ibi = ibi;
    }

    public SleepAlgorithmOutput() {

    }

    public int getSleepWakeDecisionStatus() {
        return sleepWakeDecisionStatus;
    }

    public void setSleepWakeDecisionStatus(int sleepWakeDecisionStatus) {
        this.sleepWakeDecisionStatus = sleepWakeDecisionStatus;
    }

    public int getSleepWakeDecision() {
        return sleepWakeDecision;
    }

    public void setSleepWakeDecision(int sleepWakeDecision) {
        this.sleepWakeDecision = sleepWakeDecision;
    }

    public int getSleepWakeDetentionLatency() {
        return sleepWakeDetentionLatency;
    }

    public void setSleepWakeDetentionLatency(int sleepWakeDetentionLatency) {
        this.sleepWakeDetentionLatency = sleepWakeDetentionLatency;
    }

    public float getSleepWakeOutputConfLevel() {
        return sleepWakeOutputConfLevel;
    }

    public void setSleepWakeOutputConfLevel(float sleepWakeOutputConfLevel) {
        this.sleepWakeOutputConfLevel = sleepWakeOutputConfLevel;
    }

    public int getSleepPhaseOutputStatus() {
        return sleepPhaseOutputStatus;
    }

    public void setSleepPhaseOutputStatus(int sleepPhaseOutputStatus) {
        this.sleepPhaseOutputStatus = sleepPhaseOutputStatus;
    }

    public int getSleepPhaseOutput() {
        return sleepPhaseOutput;
    }

    public void setSleepPhaseOutput(int sleepPhaseOutput) {
        this.sleepPhaseOutput = sleepPhaseOutput;
    }

    public float getSleepPhaseOutputConfLevel() {
        return sleepPhaseOutputConfLevel;
    }

    public void setSleepPhaseOutputConfLevel(float sleepPhaseOutputConfLevel) {
        this.sleepPhaseOutputConfLevel = sleepPhaseOutputConfLevel;
    }

    public float getHr() {
        return hr;
    }

    public void setHr(float hr) {
        this.hr = hr;
    }

    public float getAccMag() {
        return accMag;
    }

    public void setAccMag(float accMag) {
        this.accMag = accMag;
    }

    public float getIbi() {
        return ibi;
    }

    public void setIbi(float ibi) {
        this.ibi = ibi;
    }

    public void update(int sleepWakeDecisionStatus, int sleepWakeDecision, int sleepWakeDetentionLatency, float sleepWakeOutputConfLevel, int sleepPhaseOutputStatus, int sleepPhaseOutput, float sleepPhaseOutputConfLevel, float hr, float accMag, float ibi) {
        this.sleepWakeDecisionStatus = sleepWakeDecisionStatus;
        this.sleepWakeDecision = sleepWakeDecision;
        this.sleepWakeDetentionLatency = sleepWakeDetentionLatency;
        this.sleepWakeOutputConfLevel = sleepWakeOutputConfLevel;
        this.sleepPhaseOutputStatus = sleepPhaseOutputStatus;
        this.sleepPhaseOutput = sleepPhaseOutput;
        this.sleepPhaseOutputConfLevel = sleepPhaseOutputConfLevel;
        this.hr = hr;
        this.accMag = accMag;
        this.ibi = ibi;
    }
}
