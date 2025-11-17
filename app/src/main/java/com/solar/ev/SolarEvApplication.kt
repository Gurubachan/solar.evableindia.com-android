package com.solar.ev

import android.app.Application
import android.content.Context

class SolarEvApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
