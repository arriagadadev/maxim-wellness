package com.maximintegrated.algorithms.sports;

public class SportsCoachingAlgorithmInitConfig {
    private int samplingRate = 25;
    private SportsCoachingSession session = SportsCoachingSession.UNDEFINED;
    private SportsCoachingUser user = new SportsCoachingUser();
    private SportsCoachingHistory history = new SportsCoachingHistory(0);
    private SportsCoachingEpocConfig epocConfig = new SportsCoachingEpocConfig();
    private SportsCoachingRecoveryTimeConfig recoveryConfig = new SportsCoachingRecoveryTimeConfig();

    public SportsCoachingAlgorithmInitConfig() {

    }

    public SportsCoachingAlgorithmInitConfig(int samplingRate, SportsCoachingSession session, SportsCoachingUser user, SportsCoachingHistory history, SportsCoachingEpocConfig epocConfig, SportsCoachingRecoveryTimeConfig recoveryConfig) {
        this.samplingRate = samplingRate;
        this.session = session;
        this.user = user;
        this.history = history;
        this.epocConfig = epocConfig;
        this.recoveryConfig = recoveryConfig;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    public SportsCoachingSession getSession() {
        return session;
    }

    public void setSession(SportsCoachingSession session) {
        this.session = session;
    }

    public SportsCoachingUser getUser() {
        return user;
    }

    public void setUser(SportsCoachingUser user) {
        this.user = user;
    }

    public SportsCoachingHistory getHistory() {
        return history;
    }

    public void setHistory(SportsCoachingHistory history) {
        this.history = history;
    }

    public SportsCoachingEpocConfig getEpocConfig() {
        return epocConfig;
    }

    public void setEpocConfig(SportsCoachingEpocConfig epocConfig) {
        this.epocConfig = epocConfig;
    }

    public SportsCoachingRecoveryTimeConfig getRecoveryConfig() {
        return recoveryConfig;
    }

    public void setRecoveryConfig(SportsCoachingRecoveryTimeConfig recoveryConfig) {
        this.recoveryConfig = recoveryConfig;
    }
}
