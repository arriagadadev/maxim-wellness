package com.maximintegrated.maximsensorsapp

import android.app.Application
import com.chibatching.kotpref.Kotpref

class MaximSensorsApp : Application(){

    override fun onCreate() {
        super.onCreate()
        Kotpref.init(this)
    }
}