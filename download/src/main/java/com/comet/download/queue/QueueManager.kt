package com.comet.download.queue

import android.content.Context
import com.comet.data.datastore.SettingsRepository
import com.comet.data.db.DownloadEntity
import com.comet.data.model.DownloadState
import com.comet.data.repo.DownloadRepository
import com.comet.download.notif.Notifications
import com.comet.download.postprocess.PostProcessor
import com.comet.download.service.DownloadService
import com.comet.download.storage.StorageBridge
import com.comet.engine.DownloadTask
import com.comet.engine.EngineEvent
import com.comet.engine.EngineManager
import com.comet.engine.EngineStage
import com.comet.engine.EngineStatus
import com.comet.engine.FailureMapper
import com.comet.engine.VideoEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

/** Live, in-memory progress merged over the Room snapshot by the UI (no DB write storms). */
data class LiveProgress(
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val bytesPerSec: Long = 0,
    val etaSec: Int? = null,
    val stage: EngineStage? = null,
)

/**
 * Room-backed queue manager (Part 8.1).
 *
 * States: QUEUED -> ANALYZING -> DOWNLOADING -> PROCESSING -> DONE | FAILED | PAUSED | CANCELLED.
 * Concurrency: N = 1..3 parallel downloads (setting), FIFO within user reorder.
 * Survives process death: interrupted (non-paused) tasks re-enqueue on service start and
 * resume via yt-dlp `--continue`; user-paused tasks stay paused across restarts.
 */
@Singleton
class QueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
    private val engine: VideoEngine,
    private val engineManager: EngineManager,
    private val settingsRepository: SettingsRepository,
    private val constraintsMonitor: ConstraintsMonitor,
    private val postProcessor: PostProcessor,
    private val storageBridge: StorageBridge,
    private val notifications: Notifications,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** id -> runner job */
    private val running = ConcurrentHashMap<String, Job>()

    /** Paused manually by the user — never auto-resumed. */
    private val userPaused = ConcurrentHashMap.newKeySet<String>()

    /** Paused by constraints (Wi-Fi lost etc.) — auto-resumed when constraints clear. */
    private val constraintPaused = ConcurrentHashMap.newKeySet<String>()

    private val _liveProgress = MutableStateFlow<Map<String, LiveProgress>>(emptyMap())
    val liveProgress: StateFlow<Map<String, LiveProgress>> = _liveProgress.asStateFlow()

    private val _anyActive = MutableStateFlow(false)
    val anyActive: StateFlow<Boolean> = _anyActive.asStateFlow()

    val waitingForWifi: StateFlow<Boolean> get() = constraintsMonitor.waitingForWifi

    private var observeJob: Job? = null

    @Volatile
    private var restoredOnce = false

    // ------------------------------------------------------------------ lifecycle

    /** Idempotent; called by the service on every start. */
    fun ensureStarted() {
        if (observeJob?.isActive == true) return
        observeJob = scope.launch {
            if (!restoredOnce) {
                restoredOnce = true
                // Process death / crash recovery: interrupted tasks resume via --continue.
                runCatching { repository.requeueInterrupted() }
                    .onFailure { Timber.e(it, "queue restore failed") }
            }
            observeSlots()
        }
    }

    private suspend fun observeSlots() {
        combine(
            repository.queueFlow(),
            constraintsMonitor.allowed,
            settingsRepository.settings,
            engineManager.status,
        ) { queue, allowed, settings, engineStatus ->
            SlotSnapshot(queue, allowed, settings.concurrency, engineStatus)
        }.collect { snap ->
            if (!snap.allowed) {
                // Network/charging constraint tripped: pause everything, keep the queue
                // (Wi-Fi-only ON + mobile data => held here within seconds, AC #10).
                running.keys.toList().forEach { id -> pauseInternal(id, byUser = false) }
                return@collect
            }
            // Auto-resume only constraint-paused items — never user-paused ones.
            snap.queue
                .filter { it.state == DownloadState.PAUSED && constraintPaused.contains(it.id) }
                .forEach { item ->
                    constraintPaused.remove(item.id)
                    runCatching { repository.updateState(item.id, DownloadState.QUEUED) }
                }

            if (snap.engineStatus == EngineStatus.READY) {
                var free = snap.concurrency - running.size
                val queued = snap.queue
                    .filter { it.state == DownloadState.QUEUED }
                    .sortedBy { it.position }
                for (item in queued) {
                    if (free <= 0) break
                    if (startRunner(item)) free--
                }
            }
        }
    }

    private data class SlotSnapshot(
        val queue: List<DownloadEntity>,
        val allowed: Boolean,
        val concurrency: Int,
        val engineStatus: EngineStatus,
    )

    // ------------------------------------------------------------------ enqueue

    /**
     * Creates a queue entry and kicks the foreground service. The pre-download space
     * check (Part 8.4) happens in the UI before calling this.
     */
    suspend fun enqueue(
        url: String,
        title: String,
        site: String,
        thumbnailPath: String?,
        durationSec: Long?,
        formatSelector: String,
        qualityLabel: String,
        isAudio: Boolean,
        sizeBytes: Long?,
        playlistId: String? = null,
    ): DownloadEntity {
        val id = UUID.randomUUID().toString()
        val entity = repository.enqueue(
            id = id,
            workPath = storageBridge.workDirFor(id).absolutePath,
            url = url,
            title = title,
            site = site,
            thumbnailPath = thumbnailPath,
            durationSec = durationSec,
            formatId = formatSelector,
            qualityLabel = qualityLabel,
            isAudio = isAudio,
            sizeBytes = sizeBytes,
            playlistId = playlistId,
        )
        DownloadService.start(context)
        return entity
    }

    // ------------------------------------------------------------------ runner

    private fun startRunner(entity: DownloadEntity): Boolean {
        if (running.containsKey(entity.id)) return false
        val job = scope.launch { runTask(entity) }
        running[entity.id] = job
        syncAnyActive()
        return true
    }

    private suspend fun runTask(entity: DownloadEntity) {
        val settings = settingsRepository.settings.value
        val workDir = entity.workPath?.let(::File)
        if (workDir == null) {
            fail(entity, "Missing work directory")
            return
        }
        workDir.mkdirs()
        val audioFormat = audioFormatFor(entity)
        val task = DownloadTask(
            id = entity.id,
            url = entity.url,
            formatSelector = entity.formatId,
            isAudio = entity.isAudio,
            audioFormat = audioFormat,
            audioQualityKbps = if (audioFormat == "mp3") 320 else null,
            workDir = workDir.absolutePath,
            title = entity.title,
            embedMetadata = settings.embedMetadata,
            // mp3 thumbnail embedding needs the optional `mutagen` python module, which the
            // bundled runtime does not ship — see README "Known deviations".
            embedThumbnail = settings.embedThumbnails && audioFormat != null && audioFormat != "mp3",
        )
        runCatching { repository.updateState(entity.id, DownloadState.DOWNLOADING) }
        notifications.notifyActive(entity, LiveProgress())

        var lastWrite = 0L
        try {
            engine.download(task).collect { event ->
                when (event) {
                    is EngineEvent.Progress -> {
                        val live = LiveProgress(
                            downloadedBytes = event.downloadedBytes,
                            totalBytes = event.totalBytes,
                            bytesPerSec = event.bytesPerSec,
                            etaSec = event.etaSec,
                            stage = _liveProgress.value[entity.id]?.stage,
                        )
                        _liveProgress.value = _liveProgress.value + (entity.id to live)
                        val now = System.currentTimeMillis()
                        if (now - lastWrite >= 500) {
                            lastWrite = now
                            runCatching {
                                repository.updateProgress(
                                    entity.id,
                                    event.downloadedBytes,
                                    event.totalBytes,
                                )
                            }
                            notifications.notifyActive(entity, live)
                        }
                    }

                    is EngineEvent.Stage -> {
                        val previous = _liveProgress.value[entity.id]
                        val live = LiveProgress(
                            downloadedBytes = previous?.downloadedBytes ?: 0,
                            totalBytes = previous?.totalBytes,
                            bytesPerSec = previous?.bytesPerSec ?: 0,
                            etaSec = previous?.etaSec,
                            stage = event.stage,
                        )
                        _liveProgress.value = _liveProgress.value + (entity.id to live)
                        if (event.stage == EngineStage.MERGING ||
                            event.stage == EngineStage.EXTRACTING_AUDIO ||
                            event.stage == EngineStage.EMBEDDING_METADATA
                        ) {
                            runCatching { repository.updateState(entity.id, DownloadState.PROCESSING) }
                        }
                        notifications.notifyActive(entity, live)
                    }

                    is EngineEvent.Done -> finalize(entity, event.filePath)

                    is EngineEvent.Failed -> fail(entity, event.message)

                    EngineEvent.Cancelled -> Unit // state already set by the pause/cancel caller
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "runner crashed for %s", entity.title)
            fail(entity, e.message ?: "Unexpected error")
        } finally {
            running.remove(entity.id)
            syncAnyActive()
        }
    }

    private suspend fun finalize(entity: DownloadEntity, filePath: String?) {
        runCatching { repository.updateState(entity.id, DownloadState.PROCESSING) }
        val finalPath = runCatching {
            postProcessor.process(entity, filePath)
        }.getOrElse {
            Timber.e(it, "post-process failed for %s", entity.title)
            null
        }
        if (finalPath != null) {
            runCatching {
                repository.updateFinalPath(entity.id, finalPath)
                repository.updateState(entity.id, DownloadState.DONE)
            }
            notifications.cancel(entity.id)
            notifications.notifyCompleted(entity.copy(finalPath = finalPath))
            _liveProgress.value = _liveProgress.value - entity.id
        } else {
            fail(entity, "Could not save the file to your library")
        }
    }

    private suspend fun fail(entity: DownloadEntity, rawMessage: String) {
        val failure = FailureMapper.map(rawMessage)
        Timber.w("download failed (%s): %s", failure.kind, rawMessage.take(200))
        runCatching { repository.updateState(entity.id, DownloadState.FAILED, rawMessage) }
        notifications.cancel(entity.id)
        notifications.notifyFailed(entity, failure.userMessage)
        _liveProgress.value = _liveProgress.value - entity.id
    }

    private fun audioFormatFor(entity: DownloadEntity): String? {
        if (!entity.isAudio) return null
        return when {
            entity.qualityLabel.contains("MP3", ignoreCase = true) -> "mp3"
            entity.qualityLabel.contains("M4A", ignoreCase = true) -> "m4a"
            entity.qualityLabel.contains("OPUS", ignoreCase = true) -> "opus"
            else -> "mp3"
        }
    }

    // ------------------------------------------------------------------ controls

    fun pause(id: String) {
        scope.launch {
            userPaused.add(id)
            constraintPaused.remove(id)
            pauseInternal(id, byUser = true)
        }
    }

    fun resume(id: String) {
        scope.launch {
            userPaused.remove(id)
            constraintPaused.remove(id)
            repository.resume(id)
        }
    }

    fun retry(id: String) {
        scope.launch {
            userPaused.remove(id)
            repository.retry(id)
        }
    }

    fun cancel(id: String) {
        scope.launch {
            userPaused.remove(id)
            constraintPaused.remove(id)
            val entity = repository.byId(id)
            engine.cancel(id)
            running[id]?.cancelAndJoin()
            if (entity != null) {
                runCatching { repository.cancel(id) }
                entity.workPath?.let { path -> runCatching { File(path).deleteRecursively() } }
            }
            notifications.cancel(id)
            _liveProgress.value = _liveProgress.value - id
            syncAnyActive()
        }
    }

    fun pauseAll() {
        scope.launch {
            repository.queueSnapshot()
                .filter { it.state == DownloadState.QUEUED || it.state == DownloadState.DOWNLOADING }
                .forEach { pause(it.id) }
        }
    }

    fun resumeAll() {
        scope.launch {
            repository.queueSnapshot()
                .filter { it.state == DownloadState.PAUSED }
                .forEach { resume(it.id) }
        }
    }

    fun clearFinished() {
        scope.launch { repository.clearFinished() }
    }

    private suspend fun pauseInternal(id: String, byUser: Boolean) {
        val job = running[id]
        val entity = repository.byId(id)
        // Never interrupt post-processing — the file would corrupt.
        if (entity?.state == DownloadState.PROCESSING) return
        runCatching { repository.updateState(id, DownloadState.PAUSED) }
        if (!byUser) constraintPaused.add(id)
        if (job != null) {
            engine.cancel(id) // destroys the engine process; .part files are kept for resume
            job.cancelAndJoin()
        }
        val fresh = repository.byId(id)
        if (fresh != null) {
            notifications.notifyActive(fresh, _liveProgress.value[id])
        }
    }

    private fun syncAnyActive() {
        _anyActive.value = running.isNotEmpty()
    }
}
