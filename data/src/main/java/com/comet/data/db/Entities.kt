package com.comet.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.comet.data.model.DownloadState
import com.comet.data.model.EngineChannel

/**
 * Room entities per Part 9.1. DB: comet.db, unencrypted (no secrets — cookies live app-private, not in the DB).
 */
@Entity(
    tableName = "downloads",
    indices = [Index("state"), Index("playlistId"), Index("position")],
)
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val site: String,
    val thumbnailPath: String?,
    val durationSec: Long?,
    /** yt-dlp `-f` format selector expression for this task. */
    val formatId: String,
    /** Human label shown on cards/notifications, e.g. "1080p mp4" or "MP3 320k". */
    val qualityLabel: String,
    val isAudio: Boolean,
    val sizeBytes: Long?,
    val downloadedBytes: Long,
    val state: DownloadState,
    val errorMessage: String?,
    /** App-private work dir for this task (partial files live here until post-processing). */
    val workPath: String?,
    /** Public location after MediaStore insert, e.g. /storage/emulated/0/Movies/COMET/x.mp4. */
    val finalPath: String?,
    val playlistId: String?,
    /** FIFO order within the queue (smaller = earlier). */
    val position: Int,
    val createdAt: Long,
    val completedAt: Long?,
) {
    fun progressFraction(): Float? {
        val total = sizeBytes ?: return null
        if (total <= 0L) return null
        return (downloadedBytes.toFloat() / total).coerceIn(0f, 1f)
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val site: String,
    val itemCount: Int,
    val createdAt: Long,
)

@Entity(tableName = "engine_state")
data class EngineStateEntity(
    @PrimaryKey val id: Int = 1,
    /** yt-dlp release name, e.g. 2024.03.10 */
    val version: String,
    val channel: EngineChannel,
    val lastCheckAt: Long,
    val lastUpdateAt: Long,
)

/** Audio waveform amplitudes for library cards (Part 9.1). IntArray persisted as JSON. */
@Entity(tableName = "waveforms")
data class WaveformEntity(
    @PrimaryKey val downloadId: String,
    val amplitudes: IntArray,
)

data class PlaylistWithDownloads(
    @Embedded val playlist: PlaylistEntity,
    @Relation(parentColumn = "id", entityColumn = "playlistId")
    val children: List<DownloadEntity>,
)

/**
 * History = completed/failed log — a view over downloads, not a table (Part 9.1).
 */
object HistoryView {
    const val SQL = "SELECT * FROM downloads WHERE state IN ('DONE','FAILED') ORDER BY completedAt DESC"
}
