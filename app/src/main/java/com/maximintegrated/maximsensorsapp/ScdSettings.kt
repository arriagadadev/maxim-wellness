package com.maximintegrated.maximsensorsapp

import android.content.Context
import com.chibatching.kotpref.KotprefModel

object ScdSettings: KotprefModel(){
    var scdEnabled: Boolean by booleanPref(default = true)
}