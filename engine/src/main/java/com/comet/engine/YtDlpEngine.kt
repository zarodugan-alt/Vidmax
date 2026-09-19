package com.comet.engine

import android.content.Context
import com.comet.data.model.EngineChannel
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * yt-dlp integration via youtubedl-android (Part 7.2).
 *
 * The library bundles Python + yt-dlp + FFmpeg + aria2c per ABI; this class is the only
 * place that knows those internals. All engine calls run on Dispatchers.IO.
 */
@Singleton
class YtDlpEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoEngine {

    @Volatile
    private var initialized = false

    private val cachedVersion = AtomicReference<String?>(null)

    // ---------------------------------------------------------------- init

    override suspend fun init() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        try {
            FFmpeg.getInstance().init(context)
            Aria2c.getInstance().init(context)
            YoutubeDL.getInstance().init(context)
            initialized = true
        } catch (e: Exception) {
            Timber.e(e, "engine init failed")
            throw EngineException(e.message ?: "Engine failed to initialize")
        }
    }

    // ---------------------------------------------------------------- analyze

    override suspend fun analyze(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-json")
                // Fast item list for playlist URLs; single videos are unaffected.
                addOption("--flat-playlist")
                // Degrade gracefully when individual playlist entries fail.
                addOption("--ignore-errors")
            }
            val response = YoutubeDL.getInstance().execute(request, null, null)
            parseDumpJson(response.out, url)
        }.onFailure { Timber.e(it, "analyze failed for %s", url) }
    }

    private fun parseDumpJson(out: String, requestUrl: String): VideoInfo {
        val trimmed = out.trim()
        val primaryLine = trimmed.lineSequence().firstOrNull { it.isNotBlank() }
            ?: throw EngineException("Engine returned no data")

        val root = runCatching { YtJson.json.decodeFromString(YtJson.Root.serializer(), primaryLine) }
            .getOrElse {
                YtJson.json.decodeFromString(YtJson.Root.serializer(), trimmed)
            }

        val site = root.extractorKey ?: hostOf(requestUrl)
        val isLive = root.isLive == true || root.liveStatus == "is_live"

        if (root.type == "playlist" && root.entries.isNotEmpty()) {
            val entries = root.entries.map { e ->
                PlaylistEntry(
                    id = e.id,
                    title = e.title,
                    durationSec = e.duration?.toLong(),
                    url = e.url ?: e.id?.let { id ->
                        if (site.contains("youtube", ignoreCase = true)) {
                            "https://www.youtube.com/watch?v=$id"
                        } else {
                            null
                        }
                    },
                )
            }
            return VideoInfo(
                url = root.webpageUrl ?: requestUrl,
                title = root.title ?: "Playlist",
                channel = root.channel ?: root.uploader,
                durationSec = null,
                thumbnailUrl = root.thumbnail,
                site = site,
                isLive = false,
                formats = emptyList(),
                playlistTitle = root.title,
                playlistEntries = entries,
            )
        }

        val formats = root.formats
            .mapNotNull { f ->
                val id = f.formatId ?: return@mapNotNull null
                VideoFormat(
                    formatId = id,
                    ext = f.ext,
                    height = f.height,
                    fps = f.fps,
                    vcodec = f.vcodec,
                    acodec = f.acodec,
                    filesize = f.filesize,
                    filesizeApprox = f.filesizeApprox,
                    formatNote = f.formatNote,
                    protocol = f.protocol,
                    abr = f.abr,
                    tbr = f.tbr,
                )
            }
            // Storyboards, slideshows, image formats — filtered out silently (S2).
            .filterNot { it.vcodec == "none" && it.acodec == "none" }
            .filterNot { it.ext == "mhtml" }
            .filterNot { it.formatNote?.contains("storyboard", ignoreCase = true) == true }

        return VideoInfo(
            url = root.webpageUrl ?: requestUrl,
            title = root.title ?: "Untitled",
            channel = root.channel ?: root.uploader,
            durationSec = root.duration?.toLong(),
            thumbnailUrl = root.thumbnail,
            site = site,
            isLive = isLive,
            formats = formats,
        )
    }

    private fun hostOf(url: String): String = runCatching {
        URL(url).host.removePrefix("www.")
    }.getOrDefault(url.take(40))

    // ---------------------------------------------------------------- download

    override fun download(task: DownloadTask): Flow<EngineEvent> = callbackFlow {
        val finalPath = AtomicReference<String?>(null)
        val destinationCount = AtomicInteger(0)

        fun handleLine(line: String) {
            when (val parsed = ProgressParser.parse(line)) {
                is ParsedLine.Progress -> trySend(
                    EngineEvent.Progress(
                        downloadedBytes = parsed.downloadedBytes ?: 0L,
                        totalBytes = parsed.totalBytes,
                        bytesPerSec = parsed.bytesPerSec ?: 0L,
                        etaSec = parsed.etaSec,
                    )
                )

                is ParsedLine.Destination -> {
                    finalPath.set(parsed.path)
                    val stage = when {
                        task.isAudio -> EngineStage.DOWNLOADING_AUDIO
                        destinationCount.incrementAndGet() <= 1 -> EngineStage.DOWNLOADING_VIDEO
                        else -> EngineStage.DOWNLOADING_AUDIO
                    }
                    trySend(EngineEvent.Stage(stage))
                }

                is ParsedLine.Merge -> {
                    finalPath.set(parsed.path)
                    trySend(EngineEvent.Stage(EngineStage.MERGING))
                }

                is ParsedLine.ExtractAudio -> {
                    finalPath.set(parsed.path)
                    trySend(EngineEvent.Stage(EngineStage.EXTRACTING_AUDIO))
                }

                ParsedLine.Metadata, ParsedLine.EmbedThumbnail ->
                    trySend(EngineEvent.Stage(EngineStage.EMBEDDING_METADATA))

                null -> Unit
            }
        }

        launch(Dispatchers.IO) {
            try {
                val request = buildRequest(task)
                YoutubeDL.getInstance().execute(
                    request,
                    task.id,
                    callback = { _, _, line ->
                        if (!line.isNullOrBlank()) handleLine(line)
                    },
                )
                trySend(EngineEvent.Done(finalPath.get() ?: scanWorkDirForOutput(task)))
                close()
            } catch (e: YoutubeDL.CanceledException) {
                // Process was destroyed (pause/cancel). .part files are kept for resume.
                trySend(EngineEvent.Cancelled)
                close()
            } catch (e: YoutubeDLException) {
                Timber.e(e, "download failed: %s", task.title)
                trySend(EngineEvent.Failed(code = 1, message = e.message ?: "Engine error"))
                close()
            } catch (e: InterruptedException) {
                trySend(EngineEvent.Cancelled)
                close()
            } catch (e: Exception) {
                Timber.e(e, "download crashed: %s", task.title)
                trySend(EngineEvent.Failed(code = 1, message = e.message ?: "Engine error"))
                close()
            }
        }

        awaitClose {
            // Downstream went away — make sure the engine process does too.
            runCatching { YoutubeDL.getInstance().destroyProcessById(task.id) }
        }
    }

    private fun buildRequest(task: DownloadTask): YoutubeDLRequest {
        val request = YoutubeDLRequest(task.url)
        request.addOption("--newline") // line-delimited progress (Part 7.2)
        request.addOption("--continue") // resume partial .part files
        request.addOption("--no-mtime")
        request.addOption("--no-playlist") // playlist items are enqueued individually
        request.addOption("-o", File(task.workDir, OUTPUT_TEMPLATE).absolutePath)
        request.addOption("-f", task.formatSelector)

        if (task.isAudio) {
            request.addOption("-x")
            task.audioFormat?.let { request.addOption("--audio-format", it) }
            task.audioQualityKbps?.let { request.addOption("--audio-quality", "${it}K") }
        }
        if (task.embedMetadata) {
            request.addOption("--embed-metadata")
        }
        if (task.embedThumbnail) {
            // mp3 embedding needs the optional `mutagen` python module which the bundled
            // runtime does not ship — see README "Known deviations".
            request.addOption("--embed-thumbnail")
        }
        return request
    }

    private fun scanWorkDirForOutput(task: DownloadTask): String? =
        File(task.workDir).walkTopDown()
            .filter { it.isFile && it.length() > 0 && !it.name.endsWith(".part") }
            .maxByOrNull { it.length() }
            ?.absolutePath

    // ---------------------------------------------------------------- cancel / update / version

    override suspend fun cancel(taskId: String): Unit = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().destroyProcessById(taskId)
    }

    override suspend fun checkUpdate(channel: EngineChannel): EngineUpdate? =
        withContext(Dispatchers.IO) {
            runCatching {
                val apiUrl = when (channel) {
                    EngineChannel.STABLE ->
                        "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"

                    EngineChannel.NIGHTLY ->
                        "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest"
                }
                val release = fetchJson(apiUrl) ?: return@runCatching null
                val current = cachedVersion.get()
                    ?: YoutubeDL.getInstance().versionName(context)
                    ?: YoutubeDL.getInstance().version(context)
                val latest = release.name ?: release.tagName ?: return@runCatching null
                if (latest == current) null else EngineUpdate(newVersion = latest, changelog = release.body?.take(2000))
            }.onFailure { Timber.e(it, "engine update check failed") }
                .getOrNull()
        }

    override suspend fun applyUpdate(channel: EngineChannel): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val libChannel = when (channel) {
                    EngineChannel.STABLE -> YoutubeDL.UpdateChannel.STABLE
                    EngineChannel.NIGHTLY -> YoutubeDL.UpdateChannel.NIGHTLY
                }
                // Downloads the new yt-dlp bundle and swaps it atomically.
                YoutubeDL.getInstance().updateYoutubeDL(context, libChannel)
            }.onFailure { Timber.e(it, "engine update failed") }
                .map { status ->
                    if (status == YoutubeDL.UpdateStatus.DONE) {
                        cachedVersion.set(null)
                        true
                    } else {
                        false
                    }
                }
                .getOrDefault(false)
        }

    override suspend fun version(): String? = withContext(Dispatchers.IO) {
        cachedVersion.get()?.let { return@withContext it }
        runCatching {
            val request = YoutubeDLRequest(emptyList<String>()).apply { addOption("--version") }
            val response = YoutubeDL.getInstance().execute(request, null, null)
            response.out.trim().takeIf { it.isNotBlank() }
        }.onFailure { Timber.e(it, "version probe failed") }
            .getOrNull()
            ?.also { cachedVersion.set(it) }
            ?: YoutubeDL.getInstance().versionName(context)
            ?: YoutubeDL.getInstance().version(context)
    }

    private fun fetchJson(url: String): YtJson.GithubRelease? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "COMET")
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            YtJson.json.decodeFromString(YtJson.GithubRelease.serializer(), body)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    companion object {
        /** Stable across retries so `--continue` can resume (Part 7.2). */
        const val OUTPUT_TEMPLATE = "%(title).100B.%(ext)s"
    }
}
