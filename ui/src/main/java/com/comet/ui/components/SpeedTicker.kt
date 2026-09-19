package com.comet.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.CometType
import com.comet.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Speed ticker (Part 3.4): speed/ETA text updates throttled to 500ms with
 * animateContentSize on the row — no layout jitter, honest numbers.
 */
@Composable
fun SpeedTicker(
    bytesPerSec: Long,
    etaSec: Int?,
    modifier: Modifier = Modifier,
    style: TextStyle = CometType.Telemetry,
    color: androidx.compose.ui.graphics.Color = TextSecondary,
) {
    var shownSpeed by remember { mutableStateOf(bytesPerSec) }
    var shownEta by remember { mutableStateOf(etaSec) }
    var lastCommit by remember { mutableLongStateOf(0L) }

    LaunchedEffect(bytesPerSec, etaSec) {
        val now = System.currentTimeMillis()
        val remaining = 500 - (now - lastCommit)
        if (remaining > 0) delay(remaining)
        shownSpeed = bytesPerSec
        shownEta = etaSec
        lastCommit = System.currentTimeMillis()
    }

    Row(
        modifier = modifier.animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shownSpeed > 0) {
            Text(text = Format.speed(shownSpeed), style = style, color = color)
        }
        if (shownSpeed > 0 && shownEta != null && shownEta!! > 0) {
            Spacer(Modifier.width(6.dp))
            Text(text = "·", style = style, color = color)
            Spacer(Modifier.width(6.dp))
            Text(text = "ETA ${Format.eta(shownEta!!)}", style = style, color = color)
        }
    }
}
