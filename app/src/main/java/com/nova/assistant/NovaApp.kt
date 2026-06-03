package com.nova.assistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovaApp : Application() {
    companion object {
        lateinit var instance: NovaApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
