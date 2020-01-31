package com.maximintegrated.maximsensorsapp.sports_coaching

enum class SportsCoachingSession {
    UNDEFINED,
    READINESS,
    VO2_MAX,
    EPOC_RECOVERY,
    RECOVERY_TIME,
    FITNESS_AGE
}

class SportsCoachingManager {
    companion object {
        var currentSession = SportsCoachingSession.UNDEFINED
    }
}