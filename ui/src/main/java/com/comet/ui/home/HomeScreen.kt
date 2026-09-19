package com.comet.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.comet.data.db.DownloadEntity
import com.comet.data.model.DownloadState
import com.comet.engine.VideoInfo
import com.comet.ui.components.CometGlyph
import com.comet.ui.components.CometSearchBar
import com.comet.ui.components.DownloadCard
import com.comet.ui.components.EngineStatusBanner
import com.comet.ui.components.GlassChip
import com.comet.ui.components.GlassAccent
import com.comet.ui.components.SectionHeader
import com.comet.ui.components.TabPills
import com.comet.ui.sheets.AnalyzeSheet
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.Warning
import com.comet.ui.theme.glass

/** Home (S1): paste bar + ACTIVE + sliding QUEUE/LIBRARY tabs, with the analyze sheet over it. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAnalyzeUrl: (String) -> Unit,
    onCheckClipboard: () -> Unit,
    onClipboardConsumed: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCopyError: (String) -> Unit,
    onOpenLibraryItem: (DownloadEntity) -> Unit,
    onDeleteLibraryItem: (DownloadEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBrowser: () -> Unit,
    onEngineUpdate: () -> Unit,
    onReinstallEngine: () -> Unit,
    onClearFinished: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onDownload: (DownloadRequest) -> Unit,
    onDownloadPlaylist: (VideoInfo, Set<Int>) -> Unit,
    onOpenPlaylistPicker: (VideoInfo) -> Unit,
    onConfirmPlaylistPicker: (VideoInfo, Set<Int>) -> Unit,
    onClosePlaylistPicker: () -> Unit,
    onDismissAnalyze: () -> Unit,
    onReanalyze: (String) -> Unit,
    onShareLibraryItem: (DownloadEntity) -> Unit,
    onRedownloadLibraryItem: (DownloadEntity) -> Unit,
    onConsumePrefill: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pasteText by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val haptics = com.comet.ui.components.rememberHaptics(state.hapticsEnabled)

    // Clipboard watcher (S1): on foreground, if the clipboard holds an http(s) URL ->
    // prefill + violet chip. Never auto-analyze without user tap.
    androidx.lifecycle.compose.LifecycleEventEffect(
        event = androidx.lifecycle.Lifecycle.Event.ON_RESUME,
    ) {
        onCheckClipboard()
    }

    // Share intent / browser prefill lands in the paste bar (never auto-analyzes).
    LaunchedEffect(state.prefillUrl) {
        if (state.prefillUrl != null) {
            pasteText = state.prefillUrl
            onConsumePrefill()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Header(
            updateAvailable = state.engineUpdateAvailable != null,
            engineVersion = state.engineVersion,
            onEngineUpdate = onEngineUpdate,
            onOpenSettings = onOpenSettings,
        )

        EngineStatusBanner(
            status = state.engineStatus,
            updating = state.engineUpdating,
            onReinstall = onReinstallEngine,
            modifier = Modifier.padding(horizontal = Spacing.screen),
        )

        if (state.waitingForWifi) {
            WaitingForWifi()
        }

        CometSearchBar(
            text = pasteText,
            onTextChange = {
                pasteText = it
                if (state.clipboardUrl != null && it != state.clipboardUrl) onClipboardConsumed()
            },
            onSubmit = { url ->
                haptics.buttonPress()
                onAnalyzeUrl(url)
            },
            clipboardApplied = state.clipboardUrl != null && pasteText == state.clipboardUrl,
            onClearClipboardChip = {
                pasteText = ""
                onClipboardConsumed()
            },
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 12.dp),
        )

        // ---- ACTIVE section (max 3 cards + overflow chip) ----
        val activeShown = state.active.take(3)
        if (activeShown.isNotEmpty()) {
            SectionHeader(
                text = "Active",
                modifier = Modifier.padding(horizontal = Spacing.screen),
                trailing = {
                    if (state.active.size > 3) {
                        GlassChip(
                            text = "+${state.active.size - 3} more in queue",
                            onClick = { selectedTab = 0 },
                        )
                    }
                },
            )
            Column(
                Modifier.padding(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = 10.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
            ) {
                activeShown.forEach { entity ->
                    DownloadCard(
                        entity = entity,
                        live = state.live[entity.id],
                        onPause = { onPause(entity.id) },
                        onResume = { onResume(entity.id) },
                        onCancel = { onCancel(entity.id) },
                        onRetry = { onRetry(entity.id) },
                    )
                }
            }
        } else if (state.queued.isEmpty() && state.failed.isEmpty() && selectedTab == 0) {
            // Empty queue hint appears inside the queue tab.
        }

        Spacer(Modifier.height(Spacing.sectionGap / 2))

        val queueCount = state.active.size + state.queued.size
        TabPills(
            tabs = listOf("QUEUE ($queueCount)", "LIBRARY (${state.library.size})"),
            selected = selectedTab,
            onSelect = {
                haptics.formatSelected()
                selectedTab = it
            },
            modifier = Modifier.padding(horizontal = Spacing.screen),
        )

        // Tab content: horizontal slide + fade, 280ms (Part 3.3).
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(280)) { it * dir } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(280)) { -it * dir } + fadeOut(tween(280)))
            },
            label = "tabContent",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { tab ->
            when (tab) {
                0 -> QueueTab(
                    state = state,
                    onPause = onPause,
                    onResume = onResume,
                    onCancel = onCancel,
                    onRetry = onRetry,
                    onCopyError = onCopyError,
                    onClearFinished = onClearFinished,
                    onPauseAll = onPauseAll,
                    onResumeAll = onResumeAll,
                    onOpenBrowser = onOpenBrowser,
                )

                else -> LibraryTab(
                    state = state,
                    onOpen = onOpenLibraryItem,
                    onDelete = onDeleteLibraryItem,
                    onShare = onShareLibraryItem,
                    onRedownload = onRedownloadLibraryItem,
                )
            }
        }
    }

    state.analyze?.let { analyzeState ->
        AnalyzeSheet(
            state = analyzeState,
            hapticsEnabled = state.hapticsEnabled,
            onDismiss = onDismissAnalyze,
            onDownload = onDownload,
            onDownloadPlaylist = onDownloadPlaylist,
            onOpenPlaylistPicker = onOpenPlaylistPicker,
            onReanalyze = onReanalyze,
            onCopyError = onCopyError,
            onOpenCookiesSettings = onOpenSettings,
            onUpdateEngine = onEngineUpdate,
        )
    }

    // Full playlist picker (S3) — overlays home when an analyzed playlist is opened.
    state.playlistPicker?.let { picker ->
        com.comet.ui.screens.PlaylistPickerScreen(
            info = picker.info,
            initialSelection = picker.initialSelection,
            onConfirm = { selection -> onConfirmPlaylistPicker(picker.info, selection) },
            onDismiss = onClosePlaylistPicker,
            hapticsEnabled = state.hapticsEnabled,
        )
    }
}

@Composable
private fun Header(
    updateAvailable: Boolean,
    engineVersion: String?,
    onEngineUpdate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CometGlyph(size = 22.dp)
        Spacer(Modifier.width(10.dp))
        Text("COMET", style = CometType.Title, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (engineVersion != null) {
            Text(engineVersion, style = CometType.Telemetry, color = TextTertiary)
            Spacer(Modifier.width(8.dp))
        }
        // Engine update available: Warning dot pulse on the ⟳ icon, 1800ms (Part 3.3).
        IconButton(onClick = onEngineUpdate) {
            if (updateAvailable) {
                val pulse = rememberInfiniteTransition(label = "updatePulse")
                val alpha by pulse.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(1800),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "updateDot",
                )
                Box {
                    Icon(
                        imageVector = Icons.Rounded.Update,
                        contentDescription = "Engine update available",
                        tint = Warning,
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(6.dp)
                            .background(
                                Warning.copy(alpha = alpha),
                                androidx.compose.foundation.shape.CircleShape,
                            ),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.Update,
                    contentDescription = "Engine",
                    tint = TextTertiary,
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
            )
        }
    }
}

@Composable
private fun WaitingForWifi() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .glass(corner = 12.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("•", color = Warning, style = CometType.BodyStrong)
        Spacer(Modifier.width(8.dp))
        Text("Waiting for Wi-Fi — queue is holding", style = CometType.Caption, color = TextSecondary)
    }
}
