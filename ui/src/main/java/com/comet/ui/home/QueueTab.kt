package com.comet.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.data.db.DownloadEntity
import com.comet.data.model.DownloadState
import com.comet.engine.FailureMapper
import com.comet.download.queue.LiveProgress
import com.comet.ui.components.DownloadCard
import com.comet.ui.components.EmptyState
import com.comet.ui.components.FailedCard
import com.comet.ui.components.Format
import com.comet.ui.components.GlassChip
import com.comet.ui.components.QueuedRow
import com.comet.ui.components.SectionHeader
import com.comet.ui.components.rememberHaptics
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.Warning
import com.comet.ui.theme.glass

/**
 * Queue tab (S1/S6) — the download management screen.
 *
 * Layout: stats readout (slots, Wi-Fi gate) → batch actions → ACTIVE (ring cards with
 * expandable live details) → QUEUED (numbered rows) → FAILED (collapsible, swipe to
 * remove). Every row swipes left to cancel/remove with a Danger reveal + threshold
 * haptic. "Clear finished" actually clears (bug fixed here).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueTab(
    state: HomeUiState,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCopyError: (String) -> Unit,
    onClearFinished: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeAndPaused = state.active + state.queued.filter { it.state == DownloadState.PAUSED }
    val queuedOnly = state.queued.filter { it.state == DownloadState.QUEUED }
    val downloading = activeAndPaused.count { it.state == DownloadState.DOWNLOADING }
    val hasContent = activeAndPaused.isNotEmpty() || queuedOnly.isNotEmpty() || state.failed.isNotEmpty()
    val haptics = rememberHaptics(state.hapticsEnabled)
    var failedCollapsed by rememberSaveable { mutableStateOf(true) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    if (!hasContent) {
        Box(modifier.fillMaxSize()) {
            EmptyState(
                title = "Share a link to get started",
                subtitle = "Paste a video or audio link above, or share it to COMET from any app.",
                actionLabel = "Open browser",
                onAction = onOpenBrowser,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = 12.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
    ) {
        // ---- stats readout ----
        item(key = "stats") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(corner = 12.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        buildString {
                            append("$downloading downloading")
                            if (queuedOnly.isNotEmpty()) append(" · ${queuedOnly.size} queued")
                            if (state.failed.isNotEmpty()) append(" · ${state.failed.size} failed")
                        },
                        style = CometType.BodyStrong,
                        color = TextSecondary,
                    )
                    Text(
                        "Slots ${downloading.coerceAtMost(state.concurrency)}/${state.concurrency}" +
                            if (state.waitingForWifi) " · paused — Wi-Fi only" else "",
                        style = CometType.Telemetry,
                        color = if (state.waitingForWifi) Warning else TextTertiary,
                    )
                }
                if (downloading > 0) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentCyan),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("live", style = CometType.Telemetry, color = AccentCyan)
                }
            }
        }

        // ---- batch actions ----
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(
                    text = "⏸ All",
                    enabled = activeAndPaused.isNotEmpty(),
                    onClick = onPauseAll,
                )
                GlassChip(
                    text = "▶ All",
                    enabled = activeAndPaused.isNotEmpty() || queuedOnly.isNotEmpty(),
                    onClick = onResumeAll,
                )
                Spacer(Modifier.weight(1f))
                GlassChip(
                    text = "Clear finished",
                    accent = com.comet.ui.components.GlassAccent.DANGER,
                    onClick = {
                        haptics.buttonPress()
                        onClearFinished()
                    },
                )
            }
        }

        // ---- active / paused ----
        items(activeAndPaused, key = { it.id }) { entity ->
            SwipeToCancel(haptics = haptics, onConfirm = { onCancel(entity.id) }) {
                Column {
                    DownloadCard(
                        entity = entity,
                        live = state.live[entity.id],
                        onPause = { onPause(entity.id) },
                        onResume = { onResume(entity.id) },
                        onCancel = { onCancel(entity.id) },
                        onRetry = { onRetry(entity.id) },
                    )
                    ExpandToggle(
                        expanded = expandedId == entity.id,
                        onToggle = { expandedId = if (expandedId == entity.id) null else entity.id },
                    )
                    AnimatedVisibility(visible = expandedId == entity.id) {
                        LiveDetails(entity = entity, live = state.live[entity.id])
                    }
                }
            }
        }

        // ---- queued ----
        if (queuedOnly.isNotEmpty()) {
            item(key = "queued-header") {
                SectionHeader(text = "Queued (${queuedOnly.size})", color = TextTertiary)
            }
            items(queuedOnly, key = { it.id }) { entity ->
                val index = queuedOnly.indexOf(entity)
                SwipeToCancel(haptics = haptics, onConfirm = { onCancel(entity.id) }) {
                    QueuedRow(
                        entity = entity,
                        position = index,
                        onCancel = { onCancel(entity.id) },
                    )
                }
            }
        }

        // ---- failed ----
        if (state.failed.isNotEmpty()) {
            item(key = "failed-header") {
                SectionHeader(
                    text = "Failed (${state.failed.size})",
                    color = Danger,
                    trailing = {
                        GlassChip(
                            text = if (failedCollapsed) "Show" else "Hide",
                            onClick = { failedCollapsed = !failedCollapsed },
                        )
                    },
                )
            }
            if (!failedCollapsed) {
                items(state.failed, key = { it.id }) { entity ->
                    val userMessage = FailureMapper.map(entity.errorMessage).userMessage
                    SwipeToCancel(haptics = haptics, onConfirm = { onCancel(entity.id) }) {
                        FailedCard(
                            entity = entity,
                            userMessage = userMessage,
                            onRetry = { onRetry(entity.id) },
                            onCopyError = { onCopyError(entity.errorMessage ?: "") },
                            onRemove = { onCancel(entity.id) },
                        )
                    }
                }
            }
        }
    }
}

/** Chevron row for expanding live details under an active card. */
@Composable
private fun ExpandToggle(expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Hide details" else "Show details",
            tint = TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Byte-accurate live detail panel (never faked — real bytes/speed/ETA only). */
@Composable
private fun LiveDetails(entity: DownloadEntity, live: LiveProgress?) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(corner = 12.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DetailRow("Format", entity.formatId)
        DetailRow("Quality", entity.qualityLabel)
        live?.let { l ->
            l.totalBytes?.let { total ->
                DetailRow("Transferred", "${Format.bytes(l.downloadedBytes)} / ${Format.bytes(total)}")
            }
            if (l.bytesPerSec > 0) DetailRow("Speed", Format.speed(l.bytesPerSec))
            l.etaSec?.let { DetailRow("ETA", Format.eta(it)) }
            l.stage?.let { DetailRow("Stage", it.name.lowercase().replace('_', ' ')) }
        }
        entity.sizeBytes?.let { DetailRow("Expected size", Format.bytes(it)) }
        DetailRow("Source", entity.url)
        entity.workPath?.let { DetailRow("Work dir", it) }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = CometType.Caption,
            color = TextTertiary,
            modifier = Modifier.width(110.dp),
        )
        Text(
            value,
            style = CometType.Caption,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Swipe-left to cancel: Danger background reveal; haptic fires when the threshold trips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToCancel(
    haptics: com.comet.ui.components.Haptics,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onConfirm()
                true
            } else {
                false
            }
        },
        positionalThreshold = { distance -> distance * 0.45f },
    )
    // Haptic at threshold (Part 2.6).
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            haptics.swipeThreshold()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(Danger.copy(alpha = 0.22f))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Cancel download",
                    tint = Danger,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        content = { content() },
    )
}
