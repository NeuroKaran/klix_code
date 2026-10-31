package com.klix.code

import android.app.Application
import com.klix.code.data.telemetry.KlixTelemetry
import io.sentry.android.core.SentryAndroid

class KlixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Sentry with performance & lag/jitter frame metrics and ANR detection
        SentryAndroid.init(this) { options ->
            options.isEnableAutoSessionTracking = true
            options.isAnrEnabled = true
            options.anrTimeoutIntervalMillis = 5000
            options.isEnableFramesTracking = true // Detects slow & frozen frames for UI lag/jitter
            options.tracesSampleRate = 1.0
            options.profilesSampleRate = 1.0
        }

        // Populate hardware, RAM, and OS metadata into Sentry scope
        KlixTelemetry.initEnvironmentMetadata(this)
    }

    companion object {
        lateinit var instance: KlixApplication
            private set
    }
}
