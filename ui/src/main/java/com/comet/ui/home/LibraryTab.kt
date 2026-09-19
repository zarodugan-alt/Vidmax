package com.comet.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.comet.data.db.DownloadEntity
import com.comet.ui.components.EmptyState
import com.comet.ui.components.Format
import com.comet.ui.components.GlassChip
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.AccentVioletDim
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometRadius
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass

/**
 * Library tab (S4): 2-col staggered grid, video thumbnails / violet audio cards with
 * waveform decoration, filter chips, long-press item info sheet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryTab(
    state: HomeUiState,
    onOpen: (DownloadEntity) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var infoItem by remember { mutableStateOf<DownloadEntity?>(null) }

    val items = when (filter) {
        1 -> state.library.filter { !it.isAudio }
        2 -> state.library.filter { it.isAudio }
        else -> state.library
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = Spacing.screen, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassChip("All", selected = filter == 0, onClick = { filter = 0 })
            GlassChip("Videos", selected = filter == 1, onClick = { filter = 1 })
            GlassChip("Audio", selected = filter == 2, accent = com.comet.ui.components.GlassAccent.VIOLET, onClick = { filter = 2 })
        }

        if (state.library.isEmpty()) {
            EmptyState(
                title = "Nothing downloaded yet",
                subtitle = "Finished downloads land here automatically.",
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screen),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                verticalItemSpacing = Spacing.cardGap,
            ) {
                items(items, key = { it.id }) { entity ->
                    LibraryCard(
                        entity = entity,
                        onOpen = { onOpen(entity) },
                        onLongPress = { infoItem = entity },
                    )
                }
            }
        }
    }

    infoItem?.let { item ->
        ItemInfoDialog(
            entity = item,
            onDismiss = { infoItem = null },
            onDelete = {
                onDelete(item)
                infoItem = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryCard(
    entity: DownloadEntity,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(corner = 16.dp)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        if (entity.isAudio) {
            AudioCardArt(entity)
        } else {
            VideoCardArt(entity)
        }
        Text(
            entity.title,
            style = CometType.Body,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun VideoCardArt(entity: DownloadEntity) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(CometRadius.thumb)),
    ) {
        if (entity.thumbnailPath != null) {
            AsyncImage(
                model = entity.thumbnailPath,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(BgElevated), contentAlignment = Alignment.Center) {
                Text("▶", color = AccentCyan, style = CometType.BodyStrong)
            }
        }
        entity.durationSec?.let { duration ->
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BgElevated.copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    Format.duration(duration),
                    style = CometType.Telemetry,
                    color = TextSecondary,
                )
            }
        }
    }
}

/** Violet gradient + generated waveform decoration (S4). Real amplitudes land in phase 2. */
@Composable
private fun AudioCardArt(entity: DownloadEntity) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(
                Brush.verticalGradient(
                    listOf(AccentVioletDim.copy(alpha = 0.55f), AccentVioletDim.copy(alpha = 0.12f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("♪", color = AccentViolet, style = CometType.Display)
            Spacer(Modifier.width(8.dp))
            repeat(16) { i ->
                val h = 8 + ((entity.title.hashCode().ushr(i)) % 5) * 7
                Box(
                    Modifier
                        .size(width = 3.dp, height = h.dp)
                        .background(AccentViolet.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

/** Item info sheet (S4, P1 subset): metadata + delete. */
@Composable
private fun ItemInfoDialog(
    entity: DownloadEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgElevated,
        title = {
            Text(entity.title, style = CometType.Title, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column {
                InfoRow("Format", entity.qualityLabel)
                InfoRow("Size", entity.sizeBytes?.let { Format.bytes(it) } ?: "—")
                InfoRow("Source", entity.site)
                InfoRow("Path", entity.finalPath ?: "—")
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = Danger, style = CometType.Button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, style = CometType.Button)
            }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = CometType.Caption, color = TextTertiary, modifier = Modifier.width(64.dp))
        Text(
            value,
            style = CometType.Caption,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
