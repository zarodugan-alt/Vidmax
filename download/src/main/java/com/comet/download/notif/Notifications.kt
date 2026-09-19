package com.comet.download.notif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.comet.data.db.DownloadEntity
import com.comet.data.model.DownloadState
import com.comet.download.R
import com.comet.download.queue.LiveProgress
import com.comet.download.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Notification plumbing (Part 8.2). Channels `downloads` (LOW, silent) and `completed`
 * (default importance) are created at app init and re-asserted here.
 */
@Singleton
class Notifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val downloads = NotificationChannel(
            CHANNEL_DOWNLOADS,
            context.getString(R.string.notif_channel_downloads),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notif_channel_downloads_desc)
            setShowBadge(false)
        }
        val completed = NotificationChannel(
            CHANNEL_COMPLETED,
            context.getString(R.string.notif_channel_completed),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_completed_desc)
        }
        nm.createNotificationChannel(downloads)
        nm.createNotificationChannel(completed)
    }

    /** Foreground/summary notification — the service must call startForeground with this within 5s (Part 1.4). */
    fun summary(activeCount: Int, waitingForWifi: Boolean): Notification {
        val text = when {
            waitingForWifi -> "Waiting for Wi-Fi"
            activeCount > 0 -> context.getString(R.string.notif_queue_active) +
                " · $activeCount item${if (activeCount == 1) "" else "s"}"

            else -> context.getString(R.string.notif_queue_idle)
        }
        return baseBuilder(CHANNEL_DOWNLOADS)
            .setContentTitle("COMET")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(launchPendingIntent(null))
            .build()
    }

    fun active(entity: DownloadEntity, live: LiveProgress?): Notification {
        val pct = progressPercent(entity, live)
        val builder = baseBuilder(CHANNEL_DOWNLOADS)
            .setContentTitle(entity.title)
            .setContentText(progressLine(entity, live))
            .setProgress(100, ((pct ?: 0f) * 100).toInt(), pct == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(launchPendingIntent(entity.id))

        if (entity.state == DownloadState.DOWNLOADING) {
            builder.addAction(
                R.drawable.ic_stat_comet,
                context.getString(R.string.notif_action_pause),
                servicePendingIntent(DownloadService.ACTION_PAUSE, entity.id),
            )
        } else if (entity.state == DownloadState.PAUSED) {
            builder.addAction(
                R.drawable.ic_stat_comet,
                context.getString(R.string.notif_action_resume),
                servicePendingIntent(DownloadService.ACTION_RESUME, entity.id),
            )
        }
        builder.addAction(
            R.drawable.ic_stat_comet,
            context.getString(R.string.notif_action_cancel),
            servicePendingIntent(DownloadService.ACTION_CANCEL, entity.id),
        )
        return builder.build()
    }

    fun completed(entity: DownloadEntity): Notification =
        baseBuilder(CHANNEL_COMPLETED)
            .setContentTitle(context.getString(R.string.notif_done, entity.title))
            .setContentText(entity.qualityLabel)
            .setAutoCancel(true)
            .setContentIntent(launchPendingIntent(entity.id))
            .addAction(
                R.drawable.ic_stat_comet,
                context.getString(R.string.notif_action_play),
                launchPendingIntent(entity.id),
            )
            .build()

    fun failed(entity: DownloadEntity, userMessage: String): Notification =
        baseBuilder(CHANNEL_COMPLETED)
            .setContentTitle(context.getString(R.string.notif_failed, entity.title))
            .setContentText(userMessage)
            .setAutoCancel(true)
            .setContentIntent(launchPendingIntent(entity.id))
            .addAction(
                R.drawable.ic_stat_comet,
                context.getString(R.string.notif_action_retry),
                servicePendingIntent(DownloadService.ACTION_RETRY, entity.id),
            )
            .build()

    fun notifyActive(entity: DownloadEntity, live: LiveProgress?) =
        safeNotify(entity.id.hashCode(), active(entity, live))

    fun notifyCompleted(entity: DownloadEntity) =
        safeNotify(entity.id.hashCode(), completed(entity))

    fun notifyFailed(entity: DownloadEntity, userMessage: String) =
        safeNotify(entity.id.hashCode(), failed(entity, userMessage))

    fun cancel(id: String) = NotificationManagerCompat.from(context).cancel(id.hashCode())

    private fun progressPercent(entity: DownloadEntity, live: LiveProgress?): Float? {
        live?.totalBytes?.let { total ->
            if (total > 0) return (live.downloadedBytes.toFloat() / total).coerceIn(0f, 1f)
        }
        return entity.progressFraction()
    }

    private fun progressLine(entity: DownloadEntity, live: LiveProgress?): String {
        val parts = mutableListOf<String>()
        progressPercent(entity, live)?.let { parts.add("${(it * 100).toInt()}%") }
        live?.bytesPerSec?.takeIf { it > 0 }?.let { parts.add(formatSpeed(it)) }
        live?.etaSec?.takeIf { it in 0..86_400 }?.let { parts.add("ETA ${formatEta(it)}") }
        if (entity.state == DownloadState.PROCESSING) parts.add("Processing…")
        return if (parts.isEmpty()) entity.qualityLabel else parts.joinToString(" · ")
    }

    private fun baseBuilder(channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_comet)

    /** Opens the app; when [downloadId] is set the player screen is restored (Part 8.2 [Play]). */
    private fun launchIntent(downloadId: String?): Intent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        downloadId?.let { intent.putExtra(DownloadService.EXTRA_OPEN_DOWNLOAD_ID, it) }
        return intent
    }

    private fun launchPendingIntent(downloadId: String?): PendingIntent =
        PendingIntent.getActivity(
            context,
            downloadId?.hashCode() ?: 0,
            launchIntent(downloadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun servicePendingIntent(action: String, id: String): PendingIntent =
        PendingIntent.getService(
            context,
            ("$action:$id").hashCode(),
            Intent(context, DownloadService::class.java)
                .setAction(action)
                .putExtra(DownloadService.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun safeNotify(id: Int, notification: Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
            .onFailure { Timber.w(it, "notify failed") }
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads"
        const val CHANNEL_COMPLETED = "completed"
        const val SUMMARY_ID = 1001

        fun formatSpeed(bytesPerSec: Long): String = when {
            bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / (1024f * 1024))
            bytesPerSec >= 1024 -> "%.1f KB/s".format(bytesPerSec / 1024f)
            else -> "$bytesPerSec B/s"
        }

        fun formatEta(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%d:%02d".format(m, s)
        }
    }
}
