package com.maximintegrated.algorithms;

import com.maximintegrated.algorithms.hrv.HrvAlgorithmOutput;
import com.maximintegrated.algorithms.respiratory.RespiratoryRateAlgorithmOutput;
import com.maximintegrated.algorithms.sleep.SleepAlgorithmOutput;
import com.maximintegrated.algorithms.stress.StressAlgorithmOutput;

public class AlgorithmOutput {
    private HrvAlgorithmOutput hrv;
    private RespiratoryRateAlgorithmOutput respiratory;
    private StressAlgorithmOutput stress;
    private SleepAlgorithmOutput sleep;

    public AlgorithmOutput(HrvAlgorithmOutput hrv, RespiratoryRateAlgorithmOutput respiratory, StressAlgorithmOutput stress, SleepAlgorithmOutput sleep) {
        this.hrv = hrv;
        this.respiratory = respiratory;
        this.stress = stress;
        this.sleep = sleep;
    }

    public AlgorithmOutput() {
        hrv = new HrvAlgorithmOutput();
        respiratory = new RespiratoryRateAlgorithmOutput();
        stress = new StressAlgorithmOutput();
        sleep = new SleepAlgorithmOutput();
    }

    public HrvAlgorithmOutput getHrv() {
        return hrv;
    }

    public void setHrv(HrvAlgorithmOutput hrv) {
        this.hrv = hrv;
    }

    public RespiratoryRateAlgorithmOutput getRespiratory() {
        return respiratory;
    }

    public void setRespiratory(RespiratoryRateAlgorithmOutput respiratory) {
        this.respiratory = respiratory;
    }

    public StressAlgorithmOutput getStress() {
        return stress;
    }

    public void setStress(StressAlgorithmOutput stress) {
        this.stress = stress;
    }

    public SleepAlgorithmOutput getSleep() {
        return sleep;
    }

    public void setSleep(SleepAlgorithmOutput sleep) {
        this.sleep = sleep;
    }

    public void hrvUpdate(float avnn,
                          float sdnn,
                          float rmssd,
                          float pnn50,

                          float ulf,
                          float vlf,
                          float lf,
                          float hf,
                          float lfOverHf,
                          float totPwr, int percentCompleted, boolean isHrvCalculated) {
        hrv.update(avnn, sdnn, rmssd, pnn50, ulf, vlf, lf, hf, lfOverHf, totPwr, percentCompleted, isHrvCalculated);
    }

    public void respiratoryUpdate(float respirationRate, float confidenceLevel) {
        respiratory.update(respirationRate, confidenceLevel);
    }

    public void stressUpdate(boolean stressClass, int stressScore, float stressScorePrc) {
        stress.update(stressClass, stressScore, stressScorePrc);
    }

    public void sleepUpdate(int sleepWakeDecisionStatus, int sleepWakeDecision, int sleepWakeDetentionLatency, float sleepWakeOutputConfLevel, int sleepPhaseOutputStatus, int sleepPhaseOutput, float sleepPhaseOutputConfLevel, float hr, float accMag, float ibi) {
        sleep.update(sleepWakeDecisionStatus, sleepWakeDecision, sleepWakeDetentionLatency, sleepWakeOutputConfLevel, sleepPhaseOutputStatus, sleepPhaseOutput, sleepPhaseOutputConfLevel, hr, accMag, ibi);
    }
}
