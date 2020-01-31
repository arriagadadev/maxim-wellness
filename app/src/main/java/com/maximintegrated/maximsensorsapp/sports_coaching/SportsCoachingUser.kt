package com.maximintegrated.maximsensorsapp.sports_coaching

enum class Gender {
    MALE,
    FEMALE
}

data class SportsCoachingUser(
    var userName: String = "",
    var birthYear: Int = 0,
    var gender: Gender = Gender.MALE,
    var weight: Int = 0,
    var height: Int = 0,
    var isUnitInMetrics: Boolean = false
)