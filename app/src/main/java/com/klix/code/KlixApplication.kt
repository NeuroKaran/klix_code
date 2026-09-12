package com.klix.code

import android.app.Application

class KlixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KlixApplication
            private set
    }
}
