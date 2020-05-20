package com.maximintegrated.maximsensorsapp

import android.app.Application
import com.chibatching.kotpref.Kotpref
import com.rohitss.uceh.UCEHandler

class MaximSensorsApp : Application(){

    override fun onCreate() {
        super.onCreate()
        Kotpref.init(this)
        UCEHandler.Builder(applicationContext).addCommaSeparatedEmailAddresses("mlkshckr@gmail.com").build()
    }
}