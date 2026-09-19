package com.comet.ui.home

import com.comet.data.db.DownloadEntity
import com.comet.download.queue.LiveProgress
import com.comet.engine.EngineFailure
import com.comet.engine.EngineStatus
import com.comet.engine.EngineUpdate
import com.comet.engine.VideoInfo
import com.comet.ui.components.FormatRowUi

/** Everything the Home screen (S1) renders, as one immutable snapshot. */
data class HomeUiState(
    val engineStatus: EngineStatus = EngineStatus.COLD,
    val engineVersion: String? = null,
    val engineUpdateAvailable: EngineUpdate? = null,
    val engineUpdating: Boolean = false,
    val waitingForWifi: Boolean = false,
    val active: List<DownloadEntity> = emptyList(),
    val queued: List<DownloadEntity> = emptyList(),
    val failed: List<DownloadEntity> = emptyList(),
    val library: List<DownloadEntity> = emptyList(),
    val live: Map<String, LiveProgress> = emptyMap(),
    val hapticsEnabled: Boolean = true,
    /** http(s) URL detected on the clipboard at foreground (S1 clipboard watcher). */
    val clipboardUrl: String? = null,
    /** Non-null => the analyze sheet (S2) is shown over home. */
    val analyze: AnalyzeUiState? = null,
    /** URL to prefill the paste bar with (share intent / browser). */
    val prefillUrl: String? = null,
    /** Max concurrent downloads (S10) — powers the queue "slots" readout. */
    val concurrency: Int = 2,
    /** Non-null => the full playlist picker (S3) is shown over home. */
    val playlistPicker: PlaylistPickerUiState? = null,
)

/** Full-screen playlist picker (S3) shown when an analyze result is a playlist. */
data class PlaylistPickerUiState(
    val info: VideoInfo,
    /** Indices into [VideoInfo.playlistEntries] preselected when opening. */
    val initialSelection: Set<Int> = emptySet(),
)

sealed interface AnalyzeUiState {
    data class Loading(val url: String) : AnalyzeUiState

    data class Ready(
        val url: String,
        val info: VideoInfo,
        val videoRows: List<FormatRowUi>,
        val audioRows: List<FormatRowUi>,
    ) : AnalyzeUiState

    data class Error(val url: String, val failure: EngineFailure) : AnalyzeUiState
}

/** A single enqueue request produced by the analyze sheet. */
data class DownloadRequest(
    val url: String,
    val title: String,
    val site: String,
    val thumbnailUrl: String?,
    val durationSec: Long?,
    val selector: String,
    val qualityLabel: String,
    val isAudio: Boolean,
    val estimatedBytes: Long?,
    val playlistParentUrl: String? = null,
)
