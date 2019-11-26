package com.maximintegrated.maximsensorsapp

import com.chibatching.kotpref.KotprefModel

object WhrmSettings : KotprefModel() {
    var sampledModeTimeInterval: Long by longPref(default = 300000)
}