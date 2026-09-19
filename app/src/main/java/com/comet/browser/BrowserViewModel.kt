package com.comet.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comet.data.datastore.SettingsRepository
import com.comet.data.model.VideoQualityPreference
import com.comet.download.queue.QueueManager
import com.comet.download.storage.StorageBridge
import com.comet.engine.EngineException
import com.comet.engine.EngineManager
import com.comet.engine.EngineStatus
import com.comet.engine.FailureMapper
import com.comet.engine.VideoEngine
import com.comet.ui.components.FormatCatalog
import com.comet.ui.home.AnalyzeUiState
import com.comet.ui.home.DownloadRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Browser ViewModel (S7) — fully independent of the Home/paste flow.
 * The browser analyzes and enqueues through its own pipeline; downloads land in the
 * same Room queue and show up in Home's QUEUE tab, but no navigation to Home happens.
 */
@HiltViewModel
class BrowserViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val engine: VideoEngine,
    private val engineManager: EngineManager,
    private val queueManager: QueueManager,
    private val settingsRepository: SettingsRepository,
    private val storageBridge: StorageBridge,
) : ViewModel() {

    private val _analyze = MutableStateFlow<AnalyzeUiState?>(null)
    val analyze: StateFlow<AnalyzeUiState?> = _analyze.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val engineStatus: StateFlow<EngineStatus> = engineManager.status
    val engineVersion: StateFlow<String?> = engineManager.version

    fun analyzeUrl(url: String) {
        _analyze.value = AnalyzeUiState.Loading(url)
        viewModelScope.launch {
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

    /** Enqueue straight from the browser sheet (space check first, same rules as Home). */
    fun download(request: DownloadRequest) {
        viewModelScope.launch {
            if (!storageBridge.hasSpaceFor(request.estimatedBytes)) {
                _toast.value = "Not enough free space for this download"
                return@launch
            }
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
                )
            }.onSuccess {
                _toast.value = "Added to queue — see it on the Queue tab"
            }.onFailure {
                Timber.e(it, "browser enqueue failed")
                _toast.value = "Could not add the download"
            }
        }
    }

    /** Playlists picked in the browser enqueues with the default quality selector. */
    fun downloadPlaylist(info: com.comet.engine.VideoInfo, selected: Set<Int>) {
        viewModelScope.launch {
            if (!storageBridge.hasSpaceFor(null)) {
                _toast.value = "Not enough free space for this download"
                return@launch
            }
            val settings = settingsRepository.settings.value
            val max = settings.defaultVideoQuality.maxHeight ?: 1080
            val selector = "bestvideo[height<=$max]+bestaudio/best[height<=$max]/best"
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
                    )
                }
            }
            _toast.value = "${selected.size} videos added to the queue"
        }
    }

    fun copyError(text: String) {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("COMET error", text))
        _toast.value = "Error copied"
    }

    /** Engine update from the browser error sheet (age-gate → update engine flow). */
    fun updateEngine() {
        viewModelScope.launch {
            val channel = settingsRepository.settings.value.engineChannel
            engineManager.maybeCheckForUpdate(force = true)
            engineManager.applyUpdate(channel)
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
