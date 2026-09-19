package com.comet.engine

import com.comet.data.model.EngineChannel
import kotlinx.coroutines.flow.Flow

/**
 * Engine layer contract (Part 7.1). The engine is the product — every implementation detail
 * of yt-dlp/FFmpeg/aria2 stays behind this interface.
 */
interface VideoEngine {
    /** One-time warmup (extracts the bundled Python + yt-dlp + FFmpeg + aria2 runtimes). ~1s. */
    suspend fun init()

    /** `yt-dlp -J` — metadata + formats, or playlist entries for playlist URLs. */
    suspend fun analyze(url: String): Result<VideoInfo>

    /** Runs a download; progress/stage/completion arrive as a cold Flow of [EngineEvent]. */
    fun download(task: DownloadTask): Flow<EngineEvent>

    /** Kills the engine process for a task. Partial `.part` files are retained for resume. */
    suspend fun cancel(taskId: String)

    /** Returns a pending engine update, or null when already on the latest release. */
    suspend fun checkUpdate(channel: EngineChannel): EngineUpdate?

    /** Downloads + atomically swaps the yt-dlp bundle. */
    suspend fun applyUpdate(channel: EngineChannel): Boolean

    /** Current yt-dlp release name, e.g. "2024.03.10". */
    suspend fun version(): String?
}

/** Pipeline stages surfaced by the UI under the progress ring (Part 7.1 / 8.3). */
enum class EngineStage {
    DOWNLOADING_VIDEO,
    DOWNLOADING_AUDIO,
    MERGING,
    EXTRACTING_AUDIO,
    EMBEDDING_METADATA,
    MOVING,
}

/** A single downloadable format row as extracted from yt-dlp JSON. */
data class VideoFormat(
    val formatId: String,
    val ext: String?,
    val height: Int?,
    val fps: Double?,
    val vcodec: String?,
    val acodec: String?,
    val filesize: Long?,
    val filesizeApprox: Long?,
    val formatNote: String?,
    val protocol: String?,
    val abr: Double?,
    val tbr: Double?,
) {
    val isProgressive: Boolean
        get() = hasVideo && hasAudio

    val isVideoOnly: Boolean
        get() = hasVideo && !hasAudio

    val isAudioOnly: Boolean
        get() = !hasVideo && hasAudio

    private val hasVideo: Boolean get() = vcodec != null && vcodec != "none"
    private val hasAudio: Boolean get() = acodec != null && acodec != "none"

    /** filesize, or filesize_approx when only the estimate exists. */
    val approxOrExactSize: Long? get() = filesize ?: filesizeApprox

    val isApproxSize: Boolean get() = filesize == null && filesizeApprox != null
}

/** Entry from a `--flat-playlist` fetch. */
data class PlaylistEntry(
    val id: String?,
    val title: String?,
    val durationSec: Long?,
    val url: String?,
)

/** Analyze result consumed by the format sheet (S2). */
data class VideoInfo(
    val url: String,
    val title: String,
    val channel: String?,
    val durationSec: Long?,
    val thumbnailUrl: String?,
    val site: String,
    val isLive: Boolean,
    val formats: List<VideoFormat>,
    val playlistTitle: String? = null,
    val playlistEntries: List<PlaylistEntry> = emptyList(),
) {
    val isPlaylist: Boolean get() = playlistEntries.isNotEmpty()
}

/** A single unit of work handed to the engine by the queue manager. */
data class DownloadTask(
    /** Stable id — doubles as the yt-dlp process id so pause/cancel can target it. */
    val id: String,
    val url: String,
    /** yt-dlp `-f` selector expression, e.g. "137+140" or "bestaudio". */
    val formatSelector: String,
    val isAudio: Boolean,
    /** mp3 / m4a / opus when [isAudio]. */
    val audioFormat: String? = null,
    val audioQualityKbps: Int? = null,
    val workDir: String,
    val title: String,
    val embedMetadata: Boolean = true,
    /** Only passed for formats yt-dlp can embed without mutagen (m4a/opus). See README. */
    val embedThumbnail: Boolean = false,
)

sealed interface EngineEvent {
    /** Real bytes, real speed, real ETA — never faked (Product Pillar 4). */
    data class Progress(
        val downloadedBytes: Long,
        val totalBytes: Long?,
        val bytesPerSec: Long,
        val etaSec: Int?,
    ) : EngineEvent

    data class Stage(val stage: EngineStage) : EngineEvent

    data class Done(val filePath: String?) : EngineEvent

    data class Failed(val code: Int, val message: String) : EngineEvent

    /** The engine process was killed before finishing (user pause/cancel, or scope death). */
    data object Cancelled : EngineEvent
}

/** An available engine (yt-dlp) update discovered by the two-level updater. */
data class EngineUpdate(
    val newVersion: String,
    val changelog: String?,
)

/** Warmup state machine (S0 / S1 "Engine warming up…" states). */
enum class EngineStatus {
    COLD,
    WARMING,
    READY,
    FAILED,
}

/** Raw engine failure carried to the UI layer; the taxonomy mapping happens in FailureMapper. */
class EngineException(val raw: String) : Exception(raw)
