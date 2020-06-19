package com.maximintegrated.maximsensorsapp

import com.chibatching.kotpref.KotprefModel

object DeviceSettings : KotprefModel() {
    var scdEnabled: Boolean by booleanPref(default = true)
    var mfioEnabled: Boolean by booleanPref(default = false)
    var scdsmEnabled: Boolean by booleanPref(default = false)
}