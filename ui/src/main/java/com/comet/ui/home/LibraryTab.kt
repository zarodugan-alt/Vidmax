package com.comet.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.comet.data.db.DownloadEntity
import com.comet.ui.components.EmptyState
import com.comet.ui.components.Format
import com.comet.ui.components.GlassChip
import com.comet.ui.components.SiteBadge
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.AccentVioletDim
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometRadius
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Library sort modes (S4). */
enum class LibrarySort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    LARGEST("Largest first"),
    TITLE("Title A–Z"),
}

/**
 * Library tab (S4) — the downloaded-files screen.
 *
 * Search + sort + type filters with live counts; staggered 2-col grid (video thumbs
 * with duration + quality, violet audio cards with waveform); tap opens a details
 * bottom sheet with Play / Share / Re-download / Delete (confirm).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    state: HomeUiState,
    onOpen: (DownloadEntity) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onRedownload: (DownloadEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.NEWEST) }
    var query by rememberSaveable { mutableStateOf("") }
    var detailsItem by remember { mutableStateOf<DownloadEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val videoCount = state.library.count { !it.isAudio }
    val audioCount = state.library.count { it.isAudio }

    val items = remember(state.library, filter, sort, query) {
        val byType = when (filter) {
            1 -> state.library.filter { !it.isAudio }
            2 -> state.library.filter { it.isAudio }
            else -> state.library
        }
        val byQuery = if (query.isBlank()) {
            byType
        } else {
            byType.filter { it.title.contains(query, ignoreCase = true) }
        }
        when (sort) {
            LibrarySort.NEWEST -> byQuery.sortedByDescending { it.completedAt ?: it.createdAt }
            LibrarySort.OLDEST -> byQuery.sortedBy { it.completedAt ?: it.createdAt }
            LibrarySort.LARGEST -> byQuery.sortedByDescending { it.sizeBytes ?: 0L }
            LibrarySort.TITLE -> byQuery.sortedBy { it.title.lowercase(Locale.US) }
        }
    }

    Column(modifier.fillMaxSize()) {
        // ---- search + sort row ----
        Row(
            Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .glass(corner = 24.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Search library",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = CometType.Body.copy(color = TextPrimary),
                        cursorBrush = SolidColor(AccentCyan),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("Search downloads", style = CometType.Body, color = TextTertiary)
                            }
                            inner()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(onClick = { query = "" }),
                        )
                    }
                }
            }
            Box {
                GlassChip(
                    text = sort.label,
                    leading = {
                        Icon(Icons.Rounded.Sort, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                    },
                    onClick = { sortMenuOpen = true },
                )
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    LibrarySort.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    mode.label,
                                    style = CometType.Body,
                                    color = if (mode == sort) AccentCyan else TextSecondary,
                                )
                            },
                            onClick = {
                                sort = mode
                                sortMenuOpen = false
                            },
                        )
                    }
                }
            }
        }

        // ---- filter chips with counts ----
        Row(
            Modifier.padding(horizontal = Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassChip("All ${state.library.size}", selected = filter == 0, onClick = { filter = 0 })
            GlassChip("Videos $videoCount", selected = filter == 1, onClick = { filter = 1 })
            GlassChip(
                "Audio $audioCount",
                selected = filter == 2,
                accent = com.comet.ui.components.GlassAccent.VIOLET,
                onClick = { filter = 2 },
            )
        }

        when {
            state.library.isEmpty() ->
                EmptyState(
                    title = "Nothing downloaded yet",
                    subtitle = "Finished downloads land here automatically.",
                )

            items.isEmpty() ->
                EmptyState(
                    title = if (query.isBlank()) "Nothing here yet" else "No matches",
                    subtitle = if (query.isBlank()) {
                        "This filter is empty."
                    } else {
                        "Nothing matches \"${query.take(40)}\"."
                    },
                )

            else ->
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp),
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                    verticalItemSpacing = Spacing.cardGap,
                ) {
                    items(items, key = { it.id }) { entity ->
                        LibraryCard(
                            entity = entity,
                            onOpen = { detailsItem = entity },
                        )
                    }
                }
        }
    }

    // ---- details bottom sheet ----
    detailsItem?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { detailsItem = null },
            containerColor = BgElevated,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = CometRadius.sheetTop, topEnd = CometRadius.sheetTop),
        ) {
            LibraryDetails(
                entity = item,
                onPlay = {
                    detailsItem = null
                    onOpen(item)
                },
                onShare = {
                    detailsItem = null
                    onShare(item)
                },
                onRedownload = {
                    detailsItem = null
                    onRedownload(item)
                },
                onDelete = {
                    detailsItem = null
                    confirmDelete = item
                },
            )
        }
    }

    // ---- delete confirmation ----
    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = BgElevated,
            title = { Text("Delete file?", style = CometType.Title, color = TextPrimary) },
            text = {
                Text(
                    "\"${item.title}\" will be removed from your library and from storage. " +
                        "This cannot be undone.",
                    style = CometType.Body,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    confirmDelete = null
                }) {
                    Text("Delete", color = Danger, style = CometType.Button)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("Keep", color = TextSecondary, style = CometType.Button)
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryCard(entity: DownloadEntity, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(corner = 16.dp)
            .combinedClickable(onClick = onOpen, onLongClick = onOpen),
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
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteBadge(domain = entity.site, size = 14.dp)
            Spacer(Modifier.width(6.dp))
            entity.qualityLabel.takeIf { it.isNotBlank() }?.let {
                Text(it, style = CometType.Telemetry, color = TextTertiary, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
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
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(28.dp),
                )
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

/** Violet gradient + waveform decoration (S4). Real amplitudes land with the waveform decoder. */
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
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = AccentViolet,
                modifier = Modifier.size(20.dp),
            )
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
        entity.durationSec?.let { duration ->
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BgElevated.copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(Format.duration(duration), style = CometType.Telemetry, color = TextSecondary)
            }
        }
    }
}

/** Details sheet content: preview + metadata + actions. */
@Composable
private fun LibraryDetails(
    entity: DownloadEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = 24.dp),
    ) {
        if (entity.isAudio) {
            AudioCardArt(entity)
        } else {
            VideoCardArt(entity)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            entity.title,
            style = CometType.Title,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SiteBadge(domain = entity.site, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                entity.qualityLabel,
                style = CometType.Caption,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(14.dp))
        DetailLine("Size", entity.sizeBytes?.let { Format.bytes(it) } ?: "—")
        entity.durationSec?.let { DetailLine("Duration", Format.duration(it)) }
        DetailLine(
            "Downloaded",
            entity.completedAt?.let { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.US).format(Date(it)) } ?: "—",
        )
        DetailLine("Saved at", entity.finalPath ?: "App storage")
        Spacer(Modifier.height(18.dp))

        // Play / Share / Re-download / Delete
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.Button(
                onClick = onPlay,
                shape = PillShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (entity.isAudio) AccentViolet else AccentCyan,
                    contentColor = com.comet.ui.theme.TextOnAccent,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play", style = CometType.Button)
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onShare,
                shape = PillShape,
                modifier = Modifier.weight(1f),
            ) {
                Text("Share", style = CometType.Button, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = onRedownload,
                shape = PillShape,
                modifier = Modifier.weight(1f),
            ) {
                Text("Re-download", style = CometType.Button, color = TextSecondary)
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onDelete,
                shape = PillShape,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = Danger,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text("Delete", style = CometType.Button, color = Danger)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = CometType.Caption, color = TextTertiary, modifier = Modifier.width(96.dp))
        Text(
            value,
            style = CometType.Caption,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
