package com.comet.download.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.comet.download.notif.Notifications
import com.comet.download.queue.QueueManager
import com.comet.data.repo.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Foreground download service (Part 8.2).
 *
 * - startForeground() within 5s of start (Part 1.4) with a summary notification.
 * - Persistent per-download progress notifications (downloads channel, LOW, silent).
 * - Partial wake lock held only while downloads run, released 60s after going idle.
 * - START_STICKY: process death -> restart -> queue restore -> resume via --continue.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var queueManager: QueueManager
    @Inject lateinit var repository: DownloadRepository
    @Inject lateinit var notifications: Notifications

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannels()
        observeIdle()
        holdWakeLockWhileActive()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground within 5s — required by the platform and Part 1.4.
        startForeground(
            Notifications.SUMMARY_ID,
            notifications.summary(activeCount(), queueManager.waitingForWifi.value),
        )

        when (intent?.action) {
            ACTION_PAUSE -> intent.getStringExtra(EXTRA_ID)?.let { queueManager.pause(it) }
            ACTION_RESUME -> intent.getStringExtra(EXTRA_ID)?.let { queueManager.resume(it) }
            ACTION_CANCEL -> intent.getStringExtra(EXTRA_ID)?.let { queueManager.cancel(it) }
            ACTION_RETRY -> intent.getStringExtra(EXTRA_ID)?.let { queueManager.retry(it) }
            ACTION_PAUSE_ALL -> queueManager.pauseAll()
            ACTION_RESUME_ALL -> queueManager.resumeAll()
            ACTION_CLEAR_FINISHED -> queueManager.clearFinished()
            else -> queueManager.ensureStarted()
        }
        refreshSummary()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        super.onDestroy()
    }

    private fun activeCount(): Int = queueManager.liveProgress.value.size

    /** Keeps the foreground summary fresh and stops the service once nothing is pending. */
    private fun observeIdle() {
        scope.launch {
            launch {
                queueManager.anyActive.collect {
                    refreshSummary()
                }
            }
            launch {
                repository.queueFlow().collect { queue ->
                    refreshSummary()
                    val busy = queue.any { !it.state.isTerminal }
                    if (!busy && !queueManager.anyActive.value) {
                        Timber.d("queue idle — stopping service")
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun refreshSummary() {
        val active = activeCount()
        val notification = notifications.summary(active, queueManager.waitingForWifi.value)
        val nm = androidx.core.app.NotificationManagerCompat.from(this)
        runCatching { nm.notify(Notifications.SUMMARY_ID, notification) }
    }

    /** Partial wake lock: held only while >= 1 active download; 60s grace before release (Part 8.1). */
    private fun holdWakeLockWhileActive() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "comet:downloads").apply {
            setReferenceCounted(false)
        }
        scope.launch {
            var idleTicks = 0
            while (isActive) {
                if (queueManager.anyActive.value) {
                    idleTicks = 0
                    val lock = wakeLock
                    if (lock != null && !lock.isHeld) {
                        runCatching { lock.acquire(WAKE_TIMEOUT_MS) }
                    }
                } else {
                    idleTicks++
                    if (idleTicks >= IDLE_TICKS_BEFORE_RELEASE) {
                        val lock = wakeLock
                        if (lock != null && lock.isHeld) {
                            runCatching { lock.release() }
                            Timber.d("wake lock released after idle grace")
                        }
                    }
                }
                delay(TICK_MS)
            }
        }
    }

    companion object {
        const val ACTION_START = "com.comet.download.action.START"
        const val ACTION_PAUSE = "com.comet.download.action.PAUSE"
        const val ACTION_RESUME = "com.comet.download.action.RESUME"
        const val ACTION_CANCEL = "com.comet.download.action.CANCEL"
        const val ACTION_RETRY = "com.comet.download.action.RETRY"
        const val ACTION_PAUSE_ALL = "com.comet.download.action.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.comet.download.action.RESUME_ALL"
        const val ACTION_CLEAR_FINISHED = "com.comet.download.action.CLEAR_FINISHED"

        const val EXTRA_ID = "extra_id"
        const val EXTRA_OPEN_DOWNLOAD_ID = "extra_open_download_id"

        private const val TICK_MS = 5_000L
        private const val IDLE_TICKS_BEFORE_RELEASE = 12 // 60s grace
        private const val WAKE_TIMEOUT_MS = 30L * 60 * 1000

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DownloadService::class.java).setAction(ACTION_START),
            )
        }

        fun pause(context: Context, id: String) =
            send(context, Intent(context, DownloadService::class.java).setAction(ACTION_PAUSE).putExtra(EXTRA_ID, id))

        fun resume(context: Context, id: String) =
            send(context, Intent(context, DownloadService::class.java).setAction(ACTION_RESUME).putExtra(EXTRA_ID, id))

        fun cancel(context: Context, id: String) =
            send(context, Intent(context, DownloadService::class.java).setAction(ACTION_CANCEL).putExtra(EXTRA_ID, id))

        fun retry(context: Context, id: String) =
            send(context, Intent(context, DownloadService::class.java).setAction(ACTION_RETRY).putExtra(EXTRA_ID, id))

        private fun send(context: Context, intent: Intent) {
            runCatching { context.startService(intent) }
        }
    }
}
