package com.comet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.data.db.DownloadEntity
import com.comet.data.model.DownloadState
import com.comet.download.queue.LiveProgress
import com.comet.engine.EngineStage
import com.comet.ui.theme.CometRadius
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass
import com.comet.ui.theme.glassDanger

private fun stageLabel(stage: EngineStage?): String? = when (stage) {
    EngineStage.DOWNLOADING_VIDEO -> "Downloading video…"
    EngineStage.DOWNLOADING_AUDIO -> "Downloading audio…"
    EngineStage.MERGING -> "Merging…"
    EngineStage.EXTRACTING_AUDIO -> "Extracting audio…"
    EngineStage.EMBEDDING_METADATA -> "Embedding metadata…"
    EngineStage.MOVING -> "Saving…"
    null -> null
}

private fun ringStateFor(entity: DownloadEntity): RingState = when (entity.state) {
    DownloadState.PAUSED -> RingState.PAUSED
    DownloadState.FAILED -> RingState.FAILED
    DownloadState.DONE -> RingState.COMPLETE
    else -> RingState.ACTIVE
}

private fun ringProgressFor(entity: DownloadEntity, live: LiveProgress?): Float? {
    if (entity.state == DownloadState.QUEUED) return null // dim indeterminate orbit
    if (entity.state == DownloadState.DONE) return 1f
    live?.totalBytes?.takeIf { it > 0 }?.let { total ->
        return (live.downloadedBytes.toFloat() / total).coerceIn(0f, 1f)
    }
    return entity.progressFraction()
}

/**
 * Download card (S1/S6, Part 6): glass card, 56dp CometRing with percentage in
 * Telemetry, speed/ETA ticker line, format + byte line, pause/resume/cancel controls.
 */
@Composable
fun DownloadCard(
    entity: DownloadEntity,
    live: LiveProgress?,
    modifier: Modifier = Modifier,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit = {},
) {
    val mode = if (entity.isAudio) RingMode.AUDIO else RingMode.VIDEO
    val ringState = ringStateFor(entity)
    val progress = ringProgressFor(entity, live)
    val pct = progress ?: 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass()
            .padding(Spacing.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CometRing(
                progress = progress,
                mode = mode,
                state = ringState,
                ringSize = 56.dp,
                dim = entity.state == DownloadState.QUEUED,
            )
            if (entity.state != DownloadState.DONE && progress != null) {
                Text(
                    text = Format.percent(pct),
                    style = CometType.Telemetry,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entity.title,
                style = CometType.BodyStrong,
                color = com.comet.ui.theme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val stage = live?.stage?.let { stageLabel(it) }
            when {
                entity.state == DownloadState.PROCESSING && stage != null ->
                    Text(stage, style = CometType.Telemetry, color = TextSecondary)

                entity.state == DownloadState.QUEUED ->
                    Text("Waiting in queue", style = CometType.Telemetry, color = TextTertiary)

                entity.state == DownloadState.PAUSED ->
                    Text("Paused", style = CometType.Telemetry, color = com.comet.ui.theme.Warning)

                else -> SpeedTicker(
                    bytesPerSec = live?.bytesPerSec ?: 0,
                    etaSec = live?.etaSec,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(entity.qualityLabel)
                    val done = live?.downloadedBytes ?: entity.downloadedBytes
                    val total = live?.totalBytes ?: entity.sizeBytes
                    if (total != null && total > 0) {
                        append(" · ")
                        append(Format.bytes(done))
                        append("/")
                        append(Format.bytes(total))
                    }
                },
                style = CometType.Caption,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Controls(
            entity = entity,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun Controls(
    entity: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    when (entity.state) {
        DownloadState.DOWNLOADING, DownloadState.ANALYZING -> Row {
            IconAction(Icons.Rounded.Pause, "Pause", onPause)
            IconAction(Icons.Rounded.Close, "Cancel", onCancel, tint = Danger)
        }

        DownloadState.PROCESSING -> Unit

        DownloadState.PAUSED -> Row {
            IconAction(Icons.Rounded.PlayArrow, "Resume", onResume)
            IconAction(Icons.Rounded.Close, "Cancel", onCancel, tint = Danger)
        }

        DownloadState.QUEUED -> Row {
            IconAction(Icons.Rounded.Close, "Cancel", onCancel, tint = Danger)
        }

        DownloadState.FAILED -> Row {
            IconAction(Icons.Rounded.Refresh, "Retry", onRetry)
            IconAction(Icons.Rounded.Close, "Remove", onCancel, tint = Danger)
        }

        DownloadState.DONE -> Unit
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = TextSecondary,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}

/**
 * Compact queued row (S6): thumb 40dp, title, format chip, position number, drag handle.
 * (Drag reordering lands in phase 2 with the Reorderable library.)
 */
@Composable
fun QueuedRow(
    entity: DownloadEntity,
    position: Int,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (position + 1).toString(),
            style = CometType.Telemetry,
            color = TextTertiary,
            modifier = Modifier.width(24.dp),
        )
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(CometRadius.thumb))
                .background(com.comet.ui.theme.BgElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (entity.isAudio) "♪" else "▶",
                style = CometType.Caption,
                color = if (entity.isAudio) com.comet.ui.theme.AccentViolet else com.comet.ui.theme.AccentCyan,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entity.title,
                style = CometType.Body,
                color = com.comet.ui.theme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entity.qualityLabel,
                style = CometType.Caption,
                color = TextTertiary,
            )
        }
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Reorder (coming in phase 2)",
            tint = TextTertiary,
        )
        IconAction(Icons.Rounded.Close, "Cancel", onCancel, tint = Danger)
    }
}

/** Failed card (S6): red-tinted glass, error caption, [Retry] [Remove]. */
@Composable
fun FailedCard(
    entity: DownloadEntity,
    userMessage: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onCopyError: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassDanger()
            .padding(Spacing.card),
    ) {
        Text(
            entity.title,
            style = CometType.BodyStrong,
            color = com.comet.ui.theme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(userMessage, style = CometType.Caption, color = Danger)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextAction("Retry", onRetry, accent = com.comet.ui.theme.Warning)
            TextAction("Copy error", onCopyError)
            TextAction("Remove", onRemove, tint = Danger)
        }
    }
}

@Composable
private fun TextAction(
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = TextSecondary,
    accent: androidx.compose.ui.graphics.Color? = null,
) {
    Text(
        label,
        style = CometType.Button,
        color = accent ?: tint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background((accent ?: tint).copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick,
        ).let { Modifier },
    )
