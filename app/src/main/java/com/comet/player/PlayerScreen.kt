package com.comet.player

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.comet.data.db.DownloadEntity
import com.comet.ui.components.Format
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.AccentVioletDim
import com.comet.ui.theme.BgPrimary
import com.comet.ui.theme.CometType
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import java.io.File
import kotlinx.coroutines.delay

/**
 * Player (S5): Media3 ExoPlayer with custom Compose controls — play/pause, scrubber
 * with real timestamps, auto-hiding overlay for video; violet artwork panel for audio.
 * Mini-player + PiP land with phase 3 polish.
 */
@Composable
fun PlayerScreen(
    entity: DownloadEntity?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val path = entity?.finalPath

    val exoPlayer = remember(path) {
        if (path == null) return@remember null
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    // Position poller — real values only, never a fake bar.
    LaunchedEffect(exoPlayer) {
        while (true) {
            exoPlayer?.let { p ->
                if (!scrubbing) positionMs = p.currentPosition
                durationMs = p.duration.coerceAtLeast(0L)
                isPlaying = p.isPlaying
            }
            delay(250)
        }
    }
    // Auto-hide the video overlay after 3s of playback.
    LaunchedEffect(isPlaying, controlsVisible, positionMs) {
        if (controlsVisible && isPlaying && !scrubbing) {
            delay(3000)
            controlsVisible = false
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
    ) {
        // ---- header ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                entity?.title ?: "Player",
                style = CometType.BodyStrong,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (entity?.isAudio == true) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = "Audio",
                    tint = AccentViolet,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (exoPlayer == null || entity == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "File not found — still downloading?",
                    style = CometType.Body,
                    color = TextTertiary,
                )
            }
            return@Column
        }

        if (entity.isAudio) {
            // ---- audio mode: violet artwork + center controls ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .size(220.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(AccentVioletDim.copy(alpha = 0.6f), BgPrimary),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AccentViolet,
                            modifier = Modifier.size(84.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        entity.title,
                        style = CometType.Title,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 28.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entity.qualityLabel.ifBlank { "Audio" },
                        style = CometType.Caption,
                        color = TextTertiary,
                    )
                    Spacer(Modifier.height(28.dp))
                    PlayerControls(
                        exoPlayer = exoPlayer,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onScrubStart = { scrubbing = true },
                        onScrubEnd = { scrubbing = false },
                        accent = AccentViolet,
                    )
                }
            }
        } else {
            // ---- video mode: surface + auto-hiding overlay ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                // Tap anywhere toggles the overlay.
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable { controlsVisible = !controlsVisible },
                )
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(BgPrimary.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        PlayerControls(
                            exoPlayer = exoPlayer,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onScrubStart = { scrubbing = true },
                            onScrubEnd = { scrubbing = false },
                            accent = com.comet.ui.theme.AccentCyan,
                        )
                    }
                }
            }
        }
    }
}

/** Shared control bar: play/pause + scrubber + timestamps (S5). */
@Composable
private fun PlayerControls(
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    accent: androidx.compose.ui.graphics.Color,
) {
    val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
    var scrubValue by remember { mutableStateOf<Float?>(null) }
    Column {
        Slider(
            value = scrubValue ?: (positionMs / safeDuration).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                onScrubStart()
                scrubValue = fraction
            },
            onValueChangeFinished = {
                scrubValue?.let { exoPlayer.seekTo((it * safeDuration).toLong()) }
                scrubValue = null
                onScrubEnd()
            },
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = TextTertiary.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
            }) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = accent,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                Format.duration((scrubValue?.times(safeDuration)?.toLong() ?: positionMs) / 1000),
                style = CometType.Telemetry,
                color = TextSecondary,
            )
            Text(
                " / ${if (durationMs > 0) Format.duration(durationMs / 1000) else "—"}",
                style = CometType.Telemetry,
                color = TextTertiary,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

