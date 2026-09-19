package com.comet.ui.sheets

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.comet.engine.EngineFailure
import com.comet.engine.FailureAction
import com.comet.engine.VideoInfo
import com.comet.ui.components.Format
import com.comet.ui.components.FormatCatalog
import com.comet.ui.components.FormatRow
import com.comet.ui.components.FormatRowUi
import com.comet.ui.components.SectionHeader
import com.comet.ui.components.ShimmerBlock
import com.comet.ui.components.SiteBadge
import com.comet.ui.components.rememberHaptics
import com.comet.ui.home.AnalyzeUiState
import com.comet.ui.home.DownloadRequest
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.BgPrimary
import com.comet.ui.theme.CometRadius
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.Warning
import com.comet.ui.theme.glass
import com.comet.ui.theme.glassDanger

/**
 * Analyze sheet (S2): modal bottom sheet over home, ~90% height. Shimmer skeletons while
 * yt-dlp -J loads; VIDEO (cyan) and AUDIO ONLY (violet) format sections; playlist chip;
 * error cards follow the Part 7.5 failure taxonomy with deep-link actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeSheet(
    state: AnalyzeUiState,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onDownload: (DownloadRequest) -> Unit,
    onDownloadPlaylist: (VideoInfo, Set<Int>) -> Unit,
    onReanalyze: (String) -> Unit,
    onCopyError: (String) -> Unit,
    onOpenCookiesSettings: () -> Unit,
    onUpdateEngine: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = rememberHaptics(hapticsEnabled)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgPrimary,
        shape = RoundedCornerShape(topStart = CometRadius.sheetTop, topEnd = CometRadius.sheetTop),
    ) {
        when (state) {
            is AnalyzeUiState.Loading -> LoadingContent()
            is AnalyzeUiState.Error -> ErrorContent(
                state = state,
                onReanalyze = onReanalyze,
                onCopyError = onCopyError,
                onOpenCookiesSettings = onOpenCookiesSettings,
                onUpdateEngine = onUpdateEngine,
            )

            is AnalyzeUiState.Ready -> ReadyContent(
                state = state,
                haptics = haptics,
                onDismiss = onDismiss,
                onDownload = onDownload,
                onDownloadPlaylist = onDownloadPlaylist,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        Modifier
            .fillMaxHeight(0.9f)
            .padding(horizontal = Spacing.screen),
    ) {
        ShimmerBlock(height = 190.dp)
        Spacer(Modifier.height(16.dp))
        ShimmerBlock(height = 24.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBlock(height = 14.dp)
        Spacer(Modifier.height(20.dp))
        repeat(5) {
            ShimmerBlock(height = 48.dp)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ErrorContent(
    state: AnalyzeUiState.Error,
    onReanalyze: (String) -> Unit,
    onCopyError: (String) -> Unit,
    onOpenCookiesSettings: () -> Unit,
    onUpdateEngine: () -> Unit,
) {
    val failure: EngineFailure = state.failure
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = 32.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .glassDanger()
                .padding(16.dp),
        ) {
            Column {
                Text(
                    "Couldn't analyze this link",
                    style = CometType.BodyStrong,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(failure.userMessage, style = CometType.Body, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
        when (failure.action) {
            FailureAction.IMPORT_COOKIES -> ErrorAction("Import cookies") { onOpenCookiesSettings() }
            FailureAction.UPDATE_ENGINE -> ErrorAction("Update engine") { onUpdateEngine() }
            FailureAction.RETRY -> ErrorAction("Try again") { onReanalyze(state.url) }
            else -> ErrorAction("Copy error") { onCopyError(failure.rawMessage) }
        }
        Spacer(Modifier.height(10.dp))
        if (failure.action != FailureAction.RETRY) {
            ErrorAction("Try again", secondary = true) { onReanalyze(state.url) }
        }
    }
}

@Composable
private fun ErrorAction(label: String, secondary: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = PillShape,
        modifier = Modifier.fillMaxWidth(),
        colors = if (secondary) {
            ButtonDefaults.buttonColors(
                containerColor = BgElevated,
                contentColor = TextSecondary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = TextOnAccent,
            )
        },
    ) {
        Text(label, style = CometType.Button)
    }
}

@Composable
private fun ReadyContent(
    state: AnalyzeUiState.Ready,
    haptics: com.comet.ui.components.Haptics,
    onDismiss: () -> Unit,
    onDownload: (DownloadRequest) -> Unit,
    onDownloadPlaylist: (VideoInfo, Set<Int>) -> Unit,
) {
    val info = state.info

    var selectedVideo by rememberSaveable(info.url) {
        mutableIntStateOf(FormatCatalog.recommendedIndex(state.videoRows) ?: -1)
    }
    var selectedAudio by rememberSaveable(info.url) { mutableStateOf<Int?>(null) }
    var playlistExpanded by rememberSaveable(info.url) { mutableStateOf(false) }
    var playlistSelection by rememberSaveable(
        info.url,
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(info.playlistEntries.indices.toSet()) }

    val audioSelected = selectedAudio != null

    Column(
        Modifier
            .fillMaxHeight(0.92f)
            .padding(horizontal = Spacing.screen),
    ) {
        LazyColumn(
            Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Thumbnail(info) }
            item { TitleBlock(info) }

            if (info.isLive) {
                item {
                    Text(
                        "Live streams are not supported yet — recording ships in a later phase.",
                        style = CometType.Caption,
                        color = Warning,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                if (state.videoRows.isNotEmpty()) {
                    item { SectionHeader("Video", color = AccentCyan, modifier = Modifier.padding(top = 12.dp)) }
                    itemsIndexed(state.videoRows) { index, row ->
                        FormatRow(
                            row = row,
                            selected = selectedVideo == index && !audioSelected,
                            onClick = {
                                haptics.formatSelected()
                                selectedVideo = index
                                selectedAudio = null
                            },
                        )
                    }
                }
                item { SectionHeader("Audio only", color = AccentViolet, modifier = Modifier.padding(top = 14.dp)) }
                itemsIndexed(state.audioRows) { index, row ->
                    FormatRow(
                        row = row,
                        selected = audioSelected && selectedAudio == index,
                        onClick = {
                            haptics.formatSelected()
                            selectedAudio = if (selectedAudio == index) null else index
                        },
                    )
                }

                if (info.isPlaylist) {
                    item { PlaylistSection(info, playlistExpanded, playlistSelection, onToggleExpand = { playlistExpanded = !playlistExpanded }, onToggle = { idx -> playlistSelection = if (idx in playlistSelection) playlistSelection - idx else playlistSelection + idx }) }
                }
            }
        }

        // ---- DOWNLOAD button ----
        val buttonLabel = when {
            info.isLive -> "LIVE — UNAVAILABLE"
            info.isPlaylist -> "DOWNLOAD ${playlistSelection.size}"
            audioSelected -> "EXTRACT AUDIO"
            else -> "DOWNLOAD"
        }
        val accent = if (audioSelected) AccentViolet else AccentCyan
        Button(
            onClick = {
                haptics.downloadStarted()
                if (info.isPlaylist) {
                    onDownloadPlaylist(info, playlistSelection)
                } else {
                    val row: FormatRowUi? = when {
                        audioSelected -> state.audioRows.getOrNull(selectedAudio ?: -1)
                        selectedVideo >= 0 -> state.videoRows.getOrNull(selectedVideo)
                        else -> null
                    }
                    if (row != null) {
                        onDownload(
                            DownloadRequest(
                                url = info.url,
                                title = info.title,
                                site = info.site,
                                thumbnailUrl = info.thumbnailUrl,
                                durationSec = info.durationSec,
                                selector = row.selector,
                                qualityLabel = row.qualityLabel,
                                isAudio = row.isAudio,
                                estimatedBytes = row.estimatedBytes,
                            ),
                        )
                    }
                }
                onDismiss()
            },
            enabled = !info.isLive &&
                (info.isPlaylist || selectedVideo >= 0 || audioSelected),
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = TextOnAccent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp),
        ) {
            Text(buttonLabel, style = CometType.Button)
        }
    }
}

@Composable
private fun Thumbnail(info: VideoInfo) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(CometRadius.thumb)),
    ) {
        AsyncImage(
            model = info.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        val duration = info.durationSec
        if (duration != null && duration > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BgElevated.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    Format.duration(duration),
                    style = CometType.Telemetry,
                    color = TextPrimary,
                )
            }
        }
        if (info.isLive) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Danger.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("LIVE", style = CometType.Button, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun TitleBlock(info: VideoInfo) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(
            info.title,
            style = CometType.BodyStrong,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SiteBadge(domain = Uri.parse(info.url).host)
            Spacer(Modifier.width(6.dp))
            Text(
                buildString {
                    info.channel?.let { append(it); append(" · ") }
                    append(info.site)
                },
                style = CometType.Caption,
                color = TextTertiary,
            )
        }
    }
}

/** S2 playlist chip + inline selection list (full S3 picker ships in phase 2). */
@Composable
private fun PlaylistSection(
    info: VideoInfo,
    expanded: Boolean,
    selection: Set<Int>,
    onToggleExpand: () -> Unit,
    onToggle: (Int) -> Unit,
) {
    Column(Modifier.padding(top = 12.dp)) {
        androidx.compose.material3.OutlinedButton(onClick = onToggleExpand) {
            Text(
                (if (expanded) "−" else "+") + " Playlist: ${info.playlistEntries.size} videos",
                style = CometType.Button,
                color = TextSecondary,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            info.playlistEntries.take(50).forEachIndexed { index, entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = index in selection,
                        onCheckedChange = { onToggle(index) },
                        colors = CheckboxDefaults.colors(checkedColor = AccentCyan),
                    )
                    Text(
                        "${index + 1}. ${entry.title ?: entry.id ?: "Untitled"}",
                        style = CometType.Body,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        entry.durationSec?.let { Format.duration(it) } ?: "",
                        style = CometType.Telemetry,
                        color = TextTertiary,
                    )
                }
            }
            if (info.playlistEntries.size > 50) {
                Text(
                    "…and ${info.playlistEntries.size - 50} more (full picker ships in phase 2)",
                    style = CometType.Caption,
                    color = TextTertiary,
                )
            }
            if (selection.size > 100) {
                Text(
                    "This will take a while.",
                    style = CometType.Caption,
                    color = Warning,
                )
            }
        }
    }
}
