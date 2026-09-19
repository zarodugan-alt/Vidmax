package com.comet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.engine.VideoInfo
import com.comet.ui.components.Format
import com.comet.ui.components.GlassChip
import com.comet.ui.components.rememberHaptics
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.BgPrimary
import com.comet.ui.theme.CometType
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass

/**
 * Full playlist picker (S3): every entry with checkbox, duration and position;
 * select all / none; "Download n" enqueues exactly the checked items.
 * The selection lives in UI state only — nothing is enqueued until confirm.
 */
@Composable
fun PlaylistPickerScreen(
    info: VideoInfo,
    initialSelection: Set<Int>,
    onConfirm: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val entries = info.playlistEntries
    val selectionSaver = listSaver<Set<Int>, Int>(
        save = { it.toList() },
        restore = { it.toSet() },
    )
    var selected by rememberSaveable(info.url, stateSaver = selectionSaver) {
        mutableStateOf(initialSelection.ifEmpty { entries.indices.toSet() })
    }
    val haptics = rememberHaptics(hapticsEnabled)

    Column(
        modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = Spacing.screen),
    ) {
        // ---- header ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    info.playlistTitle ?: info.title,
                    style = CometType.Title,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${entries.size} videos · ${selected.size} selected",
                    style = CometType.Caption,
                    color = TextTertiary,
                )
            }
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("☰", color = AccentCyan, style = CometType.BodyStrong)
            }
        }

        // ---- select all / none ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip(
                text = "Select all",
                selected = selected.size == entries.size,
                onClick = {
                    haptics.buttonPress()
                    selected = entries.indices.toSet()
                },
            )
            GlassChip(
                text = "None",
                selected = selected.isEmpty(),
                onClick = {
                    haptics.buttonPress()
                    selected = emptySet()
                },
            )
            Spacer(Modifier.weight(1f))
            if (selected.isNotEmpty() && selected.size < entries.size) {
                Text(
                    "${entries.size - selected.size} skipped",
                    style = CometType.Caption,
                    color = TextTertiary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // ---- entry list ----
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
        ) {
            itemsIndexed(entries, key = { index, e -> e.id ?: "idx-$index" }) { index, entry ->
                val checked = index in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(corner = 12.dp)
                        .clickable {
                            haptics.formatSelected()
                            selected = if (checked) selected - index else selected + index
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            haptics.formatSelected()
                            selected = if (checked) selected - index else selected + index
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentCyan,
                            uncheckedColor = TextTertiary,
                        ),
                    )
                    Text(
                        "${index + 1}",
                        style = CometType.Telemetry,
                        color = TextTertiary,
                        modifier = Modifier.width(30.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title ?: "Video ${index + 1}",
                            style = CometType.Body,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        entry.durationSec?.let {
                            Text(
                                Format.duration(it),
                                style = CometType.Caption,
                                color = TextTertiary,
                            )
                        }
                    }
                }
            }
        }

        // ---- confirm ----
        Button(
            onClick = {
                haptics.downloadStarted()
                onConfirm(selected)
            },
            enabled = selected.isNotEmpty(),
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = TextOnAccent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp),
        ) {
            Text(
                if (selected.size == 1) "DOWNLOAD 1 VIDEO" else "DOWNLOAD ${selected.size} VIDEOS",
                style = CometType.Button,
            )
        }
    }
}

