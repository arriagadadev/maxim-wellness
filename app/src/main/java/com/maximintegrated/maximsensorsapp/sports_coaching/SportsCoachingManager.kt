package com.maximintegrated.maximsensorsapp.sports_coaching

import com.maximintegrated.algorithms.sports.SportsCoachingSession
import com.maximintegrated.algorithms.sports.SportsCoachingUser

class SportsCoachingManager {
    companion object {
        var currentSession = SportsCoachingSession.UNDEFINED
        var currentUser: SportsCoachingUser? = null
    }
}