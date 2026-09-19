package com.comet.player

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.comet.data.db.DownloadEntity
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.BgPrimary
import com.comet.ui.theme.CometType
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextTertiary
import java.io.File

/**
 * Player (S5-lite, phase 1): Media3 ExoPlayer in an AndroidView with stock controls.
 * Full custom controls, mini-player and PiP ship with phase 2/3 polish.
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

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                )
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

        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
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
        }
    }
}
