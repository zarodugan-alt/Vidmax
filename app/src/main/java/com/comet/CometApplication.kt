package com.comet

import android.app.Application
import com.comet.download.notif.Notifications
import com.comet.engine.EngineManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * App init (S0 parallel init): notification channels (API 26+ requirement, Part 1.4),
 * Timber in debug, and the non-blocking engine warmup (~1s — the slow part). Room
 * queue restore happens on the first DownloadService start.
 */
@AndroidEntryPoint
class CometApplication : Application() {

    @Inject lateinit var engineManager: EngineManager
    @Inject lateinit var notifications: Notifications

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannels()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        engineManager.warmup()
    }
}
