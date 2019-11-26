package com.maximintegrated.maximsensorsapp

import com.chibatching.kotpref.KotprefModel

object ScdSettings : KotprefModel() {
    var scdEnabled: Boolean by booleanPref(default = true)
}