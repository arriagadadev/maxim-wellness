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
        SLEEP(1);

        int value;

        SleepWakeDecision(int value) {
            this.value = value;
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
        NOT_CALCULATED(0),
        READY(1);

        int value;

        SleepPhaseOutput(int value) {
            this.value = value;
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
