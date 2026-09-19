package com.comet.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comet.data.db.DownloadEntity
import com.comet.data.model.DownloadState
import com.comet.engine.FailureMapper
import com.comet.ui.components.DownloadCard
import com.comet.ui.components.EmptyState
import com.comet.ui.components.FailedCard
import com.comet.ui.components.GlassChip
import com.comet.ui.components.QueuedRow
import com.comet.ui.components.SectionHeader
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextTertiary

/**
 * Queue tab (S1/S6): active cards with CometRing, compact queued rows, swipe-left to
 * cancel (Danger reveal + haptic at threshold), collapsible failed section.
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
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeAndPaused = state.active + state.queued.filter { it.state == DownloadState.PAUSED }
    val queuedOnly = state.queued.filter { it.state == DownloadState.QUEUED }
    val hasContent = activeAndPaused.isNotEmpty() || queuedOnly.isNotEmpty() || state.failed.isNotEmpty()
    val haptics = com.comet.ui.components.rememberHaptics(state.hapticsEnabled)
    var failedCollapsed by rememberSaveable { mutableStateOf(true) }

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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = 12.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
    ) {
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(text = "⏸ All", onClick = { activeAndPaused.forEach { onPause(it.id) } })
                GlassChip(text = "▶ All", onClick = { activeAndPaused.forEach { onResume(it.id) } })
                Spacer(Modifier.weight(1f))
                GlassChip(text = "Clear finished", onClick = { }, accent = com.comet.ui.components.GlassAccent.DANGER)
            }
        }

        items(activeAndPaused, key = { it.id }) { entity ->
            SwipeToCancel(
                haptics = haptics,
                onConfirm = { onCancel(entity.id) },
            ) {
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

        if (queuedOnly.isNotEmpty()) {
            item(key = "queued-header") {
                SectionHeader(text = "Queued (${queuedOnly.size})", color = TextTertiary)
            }
            items(queuedOnly, key = { it.id }) { entity ->
                val index = queuedOnly.indexOf(entity)
                SwipeToCancel(onConfirm = { onCancel(entity.id) }) {
                    QueuedRow(
                        entity = entity,
                        position = index,
                        onCancel = { onCancel(entity.id) },
                    )
                }
            }
        }

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
