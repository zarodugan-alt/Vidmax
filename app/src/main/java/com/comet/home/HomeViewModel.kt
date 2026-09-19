package com.comet.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comet.data.datastore.SettingsRepository
import com.comet.data.db.DownloadEntity
import com.comet.data.db.PlaylistEntity
import com.comet.data.model.DownloadState
import com.comet.data.model.VideoQualityPreference
import com.comet.data.repo.DownloadRepository
import com.comet.download.queue.QueueManager
import com.comet.download.storage.StorageBridge
import com.comet.engine.EngineException
import com.comet.engine.EngineManager
import com.comet.engine.EngineStatus
import com.comet.engine.EngineUpdate
import com.comet.engine.FailureMapper
import com.comet.engine.VideoInfo
import com.comet.engine.VideoEngine
import com.comet.ui.components.FormatCatalog
import com.comet.ui.home.AnalyzeUiState
import com.comet.ui.home.DownloadRequest
import com.comet.ui.home.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** "Already downloaded — download again?" prompt (Part 11). */
data class RedownloadPrompt(val request: DownloadRequest, val existing: DownloadEntity)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val engine: VideoEngine,
    private val engineManager: EngineManager,
    private val queueManager: QueueManager,
    private val repository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
    private val storageBridge: StorageBridge,
) : ViewModel() {

    private val _analyze = MutableStateFlow<AnalyzeUiState?>(null)
    private val _clipboardUrl = MutableStateFlow<String?>(null)
    private val _prefillUrl = MutableStateFlow<String?>(null)
    private val _toast = MutableStateFlow<String?>(null)
    private val _redownloadPrompt = MutableStateFlow<RedownloadPrompt?>(null)
    private val _openPlayerRequest = MutableStateFlow<String?>(null)
    private val _permissionRequest = MutableStateFlow(false)

    @Volatile
    private var pendingAfterPermission: DownloadRequest? = null

    private var lastClipboard: String? = null

    val toast: StateFlow<String?> = _toast.asStateFlow()
    val openPlayerRequest: StateFlow<String?> = _openPlayerRequest.asStateFlow()
    val permissionRequest: StateFlow<Boolean> = _permissionRequest.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        repository.queueFlow(),
        repository.libraryFlow(),
        repository.failedFlow(),
        queueManager.liveProgress,
        engineManager.status,
        engineManager.version,
        engineManager.updateAvailable,
        engineManager.updating,
        queueManager.waitingForWifi,
        settingsRepository.settings,
        _analyze,
        _clipboardUrl,
        _prefillUrl,
        _redownloadPrompt,
    ) { values ->
        val queue = values[0] as? List<DownloadEntity> ?: emptyList()
        val library = values[1] as? List<DownloadEntity> ?: emptyList()
        val failed = values[2] as? List<DownloadEntity> ?: emptyList()
        HomeUiState(
            engineStatus = values[4] as? EngineStatus ?: EngineStatus.COLD,
            engineVersion = values[5] as? String,
            engineUpdateAvailable = values[6] as? EngineUpdate,
            engineUpdating = values[7] as Boolean,
            waitingForWifi = values[8] as Boolean,
            active = queue.filter { it.state.isActive },
            queued = queue.filter { it.state.isPending },
            failed = failed,
            library = library,
            live = values[3] as? Map<String, com.comet.download.queue.LiveProgress> ?: emptyMap(),
            hapticsEnabled = (values[9] as? com.comet.data.datastore.AppSettings)?.haptics ?: true,
            clipboardUrl = values[11] as? String,
            analyze = values[10] as? AnalyzeUiState,
            prefillUrl = values[12] as? String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val redownloadPrompt: StateFlow<RedownloadPrompt?> = _redownloadPrompt.asStateFlow()

    // ------------------------------------------------------------ analyze

    fun analyzeUrl(url: String) {
        _analyze.value = AnalyzeUiState.Loading(url)
        viewModelScope.launch {
            // "Engine warming up…" holds the sheet until ready (S1 states, Part 11).
            if (engineManager.status.value == EngineStatus.WARMING ||
                engineManager.status.value == EngineStatus.COLD
            ) {
                withTimeoutOrNull(15_000) {
                    engineManager.status.first { it == EngineStatus.READY || it == EngineStatus.FAILED }
                }
            }
            if (engineManager.status.value == EngineStatus.FAILED) {
                _analyze.value = AnalyzeUiState.Error(
                    url,
                    FailureMapper.map("Engine failed to load. Reinstall it from Home."),
                )
                return@launch
            }
            engine.analyze(url).fold(
                onSuccess = { info ->
                    val (videoRows, audioRows) = FormatCatalog.build(info)
                    _analyze.value = AnalyzeUiState.Ready(url, info, videoRows, audioRows)
                },
                onFailure = { e ->
                    val raw = (e as? EngineException)?.raw ?: e.message ?: "Unknown error"
                    _analyze.value = AnalyzeUiState.Error(url, FailureMapper.map(raw))
                },
            )
        }
    }

    fun dismissAnalyze() {
        _analyze.value = null
    }

    fun reanalyze(url: String) = analyzeUrl(url)

    // ------------------------------------------------------------ download

    fun download(request: DownloadRequest) {
        viewModelScope.launch {
            // Part 8.4: pre-download space check.
            if (!storageBridge.hasSpaceFor(request.estimatedBytes)) {
                _toast.value = "Not enough free space for this download"
                return@launch
            }
            // Part 10: runtime storage permission on API 28 before the first legacy write.
            if (Build.VERSION.SDK_INT < 29 && !storageBridge.hasLegacyStoragePermission()) {
                pendingAfterPermission = request
                _permissionRequest.value = true
                return@launch
            }
            // Part 11: same URL re-downloaded -> confirm.
            val existing = repository.findExistingByUrl(request.url)
            if (existing != null) {
                _redownloadPrompt.value = RedownloadPrompt(request, existing)
                return@launch
            }
            enqueue(request)
        }
    }

    fun confirmRedownload() {
        val prompt = _redownloadPrompt.value ?: return
        _redownloadPrompt.value = null
        viewModelScope.launch { enqueue(prompt.request) }
    }

    fun dismissRedownload() {
        _redownloadPrompt.value = null
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionRequest.value = false
        val pending = pendingAfterPermission
        pendingAfterPermission = null
        if (!granted) {
            _toast.value = "Storage permission needed on Android 9 — files will stay app-private"
            if (pending != null) viewModelScope.launch { enqueue(pending) }
        } else if (pending != null) {
            viewModelScope.launch { enqueue(pending) }
        }
    }

    private suspend fun enqueue(request: DownloadRequest) {
        runCatching {
            queueManager.enqueue(
                url = request.url,
                title = request.title,
                site = request.site,
                thumbnailPath = request.thumbnailUrl,
                durationSec = request.durationSec,
                formatSelector = request.selector,
                qualityLabel = request.qualityLabel,
                isAudio = request.isAudio,
                sizeBytes = request.estimatedBytes,
                playlistId = request.playlistParentUrl,
            )
        }.onFailure {
            Timber.e(it, "enqueue failed")
            _toast.value = "Could not add the download"
        }
    }

    /** Batch enqueue for playlists (S2/S3-lite, AC #5). */
    fun downloadPlaylist(info: VideoInfo, selected: Set<Int>) {
        viewModelScope.launch {
            if (!storageBridge.hasSpaceFor(null)) {
                _toast.value = "Not enough free space for this download"
                return@launch
            }
            if (Build.VERSION.SDK_INT < 29 && !storageBridge.hasLegacyStoragePermission()) {
                _toast.value = "Storage permission needed on Android 9 — files will stay app-private"
            }
            val settings = settingsRepository.settings.value
            val selector = defaultSelectorFor(settings.defaultVideoQuality)
            val playlistId = UUID.randomUUID().toString()
            repository.upsertPlaylist(
                PlaylistEntity(
                    id = playlistId,
                    url = info.url,
                    title = info.playlistTitle ?: info.title,
                    site = info.site,
                    itemCount = selected.size,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            selected.forEach { index ->
                val entry = info.playlistEntries.getOrNull(index) ?: return@forEach
                val url = entry.url ?: return@forEach
                runCatching {
                    queueManager.enqueue(
                        url = url,
                        title = entry.title ?: "Video ${index + 1}",
                        site = info.site,
                        thumbnailPath = null,
                        durationSec = entry.durationSec,
                        formatSelector = selector,
                        qualityLabel = settings.defaultVideoQuality.label,
                        isAudio = false,
                        sizeBytes = null,
                        playlistId = playlistId,
                    )
                }
            }
        }
    }

    private fun defaultSelectorFor(preference: VideoQualityPreference): String {
        val max = preference.maxHeight ?: 1080
        return "bestvideo[height<=$max]+bestaudio/best[height<=$max]/best"
    }

    // ------------------------------------------------------------ queue controls

    fun pause(id: String) = queueManager.pause(id)
    fun resume(id: String) = queueManager.resume(id)
    fun cancel(id: String) = queueManager.cancel(id)
    fun retry(id: String) = queueManager.retry(id)
    fun clearFinished() = queueManager.clearFinished()

    fun copyError(text: String) {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("COMET error", text))
        _toast.value = "Error copied"
    }

    fun deleteLibraryItem(entity: DownloadEntity) {
        viewModelScope.launch {
            runCatching { repository.deleteLibraryItem(entity.id) }
            entity.finalPath?.let { path ->
                runCatching { java.io.File(path).delete() }
            }
            _toast.value = "Deleted"
        }
    }

    fun openPlayer(id: String) {
        _openPlayerRequest.value = id
    }

    fun consumeOpenPlayer() {
        _openPlayerRequest.value = null
    }

    // ------------------------------------------------------------ engine

    fun onEngineUpdateClicked() {
        viewModelScope.launch {
            val channel = settingsRepository.settings.value.engineChannel
            engineManager.maybeCheckForUpdate(force = true)
            engineManager.applyUpdate(channel)
        }
    }

    fun reinstallEngine() {
        engineManager.reinstallEngineAndRestart()
    }

    // ------------------------------------------------------------ disclaimer gate

    /** Suspend read straight from DataStore (never the in-memory default). */
    suspend fun isDisclaimerAccepted(): Boolean = settingsRepository.disclaimerAcceptedOnce()

    fun acceptDisclaimer() {
        settingsRepository.setDisclaimerAccepted()
    }

    // ------------------------------------------------------------ clipboard watcher

    /** Called on foreground: prefill if the clipboard holds an http(s) URL (never auto-analyze). */
    fun checkClipboard() {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: return
        val url = text.split(Regex("\\s+")).firstOrNull {
            it.startsWith("http://", true) || it.startsWith("https://", true)
        }
        if (url != null && url != lastClipboard) {
            lastClipboard = url
            _clipboardUrl.value = url
        }
    }

    fun consumeClipboard() {
        _clipboardUrl.value = null
    }

    fun consumePrefill() {
        _prefillUrl.value = null
    }

    /** Share/VIEW/PROCESS_TEXT intents land here (Part 10 MainActivity handlers). */
    fun offerExternalUrl(url: String?) {
        if (url != null && (url.startsWith("http://", true) || url.startsWith("https://", true))) {
            _prefillUrl.value = url
            _clipboardUrl.value = null
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
