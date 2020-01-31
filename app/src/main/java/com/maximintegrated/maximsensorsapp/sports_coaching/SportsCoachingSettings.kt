package com.maximintegrated.maximsensorsapp.sports_coaching

import com.chibatching.kotpref.KotprefModel

object SportsCoachingSettings : KotprefModel() {
    var currentUserJson by stringPref(default = "")
    var userListJson by stringPref(default = "")
}