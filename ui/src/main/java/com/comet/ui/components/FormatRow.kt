package com.comet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.engine.VideoFormat
import com.comet.engine.VideoInfo
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometRadius
import com.comet.ui.theme.CometType
import com.comet.ui.theme.GlassBorder
import com.comet.ui.theme.GlassBorderViolet
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.Warning

/** A selectable format row as shown in the analyze sheet (video or audio). */
data class FormatRowUi(
    val label: String,
    val badge: String?,
    val sizeLabel: String?,
    val ext: String,
    val isAudio: Boolean,
    /** yt-dlp `-f` selector expression used at download time. */
    val selector: String,
    /** Human quality label persisted on the queue entity. */
    val qualityLabel: String,
    val estimatedBytes: Long?,
    val isRecommended: Boolean = false,
)

/**
 * Builds the S2 format list: filtered + sorted — progressive mp4/webm first, then DASH
 * video-only (marked "merge"), audio-only formats in the violet section. Unavailable
 * formats (storyboards, images) are filtered out silently by the engine layer.
 */
object FormatCatalog {

    const val SELECTOR_BESTAUDIO = "bestaudio/best"
    const val SELECTOR_BESTAUDIO_M4A = "bestaudio[ext=m4a]/bestaudio"
    const val SELECTOR_BESTAUDIO_WEBM = "bestaudio[ext=webm]/bestaudio"

    fun build(info: VideoInfo): Pair<List<FormatRowUi>, List<FormatRowUi>> {
        val videoRows = buildVideoRows(info)
        val audioRows = buildAudioRows(info)
        val recommended = pickRecommended(videoRows)
        if (recommended != null) {
            videoRows[recommended] = videoRows[recommended].copy(isRecommended = true)
        }
        return videoRows to audioRows
    }

    fun recommendedIndex(videoRows: List<FormatRowUi>): Int? = pickRecommended(videoRows)

    private fun buildVideoRows(info: VideoInfo): MutableList<FormatRowUi> {
        val withVideo = info.formats.filter { f ->
            (f.isProgressive || f.isVideoOnly) && (f.height ?: 0) > 0
        }
        val progressive = withVideo.filter { it.isProgressive }
            .sortedWith(
                compareByDescending<VideoFormat> { it.height }
                    .thenBy { if (it.ext == "mp4") 0 else 1 },
            )
        val videoOnly = withVideo.filter { it.isVideoOnly }
            .sortedWith(
                compareByDescending<VideoFormat> { it.height }
                    .thenBy { if (it.ext == "mp4") 0 else 1 },
            )

        val seen = HashSet<Int>()
        val rows = mutableListOf<FormatRowUi>()
        (progressive + videoOnly).forEach { f ->
            val height = f.height ?: return@forEach
            if (!seen.add(height)) return@forEach // one row per resolution
            val isMerge = f.isVideoOnly
            val size = when {
                isMerge -> f.approxOrExactSize?.plus(audioEstimateBytes(info) ?: 0L)
                else -> f.approxOrExactSize
            }
            rows.add(
                FormatRowUi(
                    label = "${height}p",
                    badge = when {
                        height >= 2160 -> "4K"
                        height >= 1080 -> "HD"
                        isMerge -> "merge"
                        else -> null
                    },
                    sizeLabel = size?.let {
                        (if (f.isApproxSize || isMerge) "~" else "") + Format.bytes(it)
                    },
                    ext = f.ext ?: "mp4",
                    isAudio = false,
                    selector = if (isMerge) "${f.formatId}+bestaudio" else f.formatId,
                    qualityLabel = "${height}p ${f.ext ?: "mp4"}",
                    estimatedBytes = size,
                ),
            )
        }
        return rows
    }

    private fun buildAudioRows(info: VideoInfo): List<FormatRowUi> {
        val duration = info.durationSec ?: 0L
        val bestAbr = info.formats.filter { it.isAudioOnly }
            .maxOfOrNull { it.abr ?: it.tbr ?: 0.0 } ?: 0.0

        fun est(kbps: Int): Long? =
            if (duration > 0) (duration * kbps * 1000L / 8L) else null

        val m4aKbps = bestAbr.toInt().coerceIn(96, 256)

        return listOf(
            FormatRowUi(
                label = "MP3 320k",
                badge = "extract",
                sizeLabel = est(320)?.let { "~${Format.bytes(it)}" },
                ext = "mp3",
                isAudio = true,
                selector = SELECTOR_BESTAUDIO,
                qualityLabel = "MP3 320k",
                estimatedBytes = est(320),
            ),
            FormatRowUi(
                label = "M4A",
                badge = "extract",
                sizeLabel = est(m4aKbps)?.let { "~${Format.bytes(it)}" },
                ext = "m4a",
                isAudio = true,
                selector = SELECTOR_BESTAUDIO_M4A,
                qualityLabel = "M4A",
                estimatedBytes = est(m4aKbps),
            ),
            FormatRowUi(
                label = "Opus 160k",
                badge = "extract",
                sizeLabel = est(160)?.let { "~${Format.bytes(it)}" },
                ext = "opus",
                isAudio = true,
                selector = SELECTOR_BESTAUDIO_WEBM,
                qualityLabel = "Opus 160k",
                estimatedBytes = est(160),
            ),
        )
    }

    /** Best mp4 <= 1080p progressive, falling back to DASH merge — preselected (S2). */
    private fun pickRecommended(rows: List<FormatRowUi>): Int? {
        val progressiveMp4 = rows.indexOfFirst {
            it.badge != "merge" && it.ext == "mp4" && heightOf(it) <= 1080
        }
        if (progressiveMp4 >= 0) return progressiveMp4
        val anyProgressive = rows.indexOfFirst { it.badge != "merge" && heightOf(it) <= 1080 }
        if (anyProgressive >= 0) return anyProgressive
        val merge = rows.indexOfFirst { heightOf(it) <= 1080 }
        if (merge >= 0) return merge
        return rows.lastIndex.takeIf { it >= 0 }
    }

    private fun heightOf(row: FormatRowUi): Int =
        row.label.removeSuffix("p").toIntOrNull() ?: 0

    private fun audioEstimateBytes(info: VideoInfo): Long? {
        val duration = info.durationSec ?: return null
        val abr = info.formats.filter { it.isAudioOnly }
            .maxOfOrNull { it.abr ?: it.tbr ?: 0.0 } ?: return null
        if (abr <= 0.0) return null
        return (duration * abr * 1000L / 8L).toLong()
    }
}

/**
 * Format row (S2 / Part 3.3): flat row with 12dp radius, ext + size on the right in
 * Telemetry; selecting glows the border cyan (video) / violet (audio) and slides a
 * check in from the right, 150ms.
 */
@Composable
fun FormatRow(
    row: FormatRowUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent: Color = if (row.isAudio) AccentViolet else AccentCyan
    val borderColor =
        if (selected) accent.copy(alpha = 0.55f)
        else if (row.isAudio) GlassBorderViolet else GlassBorder
    val shape = RoundedCornerShape(CometRadius.formatRow)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BgElevated.copy(alpha = 0.55f))
            .border(1.dp, borderColor, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp)
            .heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.label,
                        style = CometType.BodyStrong,
                        color = if (selected) accent else TextPrimary,
                    )
                    if (row.badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            row.badge.uppercase(),
                            style = CometType.Caption,
                            color = if (row.badge == "merge") Warning else TextTertiary,
                        )
                    }
                    if (row.isRecommended && !selected) {
                        Spacer(Modifier.width(6.dp))
                        Text("✓", style = CometType.Caption, color = TextTertiary)
                    }
                }
            }
            Text(
                text = listOfNotNull(row.sizeLabel, row.ext).joinToString("  "),
                style = CometType.Telemetry,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            AnimatedVisibility(
                visible = selected,
                enter = slideInHorizontally(tween(150)) { it / 2 } + fadeIn(tween(150)),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
