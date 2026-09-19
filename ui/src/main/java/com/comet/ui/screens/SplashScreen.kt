package com.comet.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comet.ui.components.CometGlyph
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.BgPrimary
import com.comet.ui.theme.CometType
import kotlinx.coroutines.delay

/**
 * Splash (S0): black BgPrimary, comet streaks left -> right across the upper third
 * (600ms arc path, cyan head + violet fading trail), settles into the centered logo.
 * "COMET" wordmark below (Display, letterSpacing 8sp).
 *
 * Exit: init done AND >= 600ms elapsed; hard cap 1500ms — engine warmup continues in
 * the background and home shows "Engine warming up…" inline if slow.
 */
@Composable
fun SplashScreen(
    engineReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready by rememberUpdatedState(engineReady)
    val streak = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        streak.animateTo(1f, animationSpec = tween(600))
    }
    LaunchedEffect(Unit) {
        val t0 = android.os.SystemClock.uptimeMillis()
        delay(600)
        while (!ready && android.os.SystemClock.uptimeMillis() - t0 < 1500) {
            delay(50)
        }
        onFinished()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(BgPrimary),
    ) {
        // Streak across the upper third.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter),
        ) {
            val t = streak.value
            val w = size.width
            val h = size.height
            val x = w * (0.05f + 0.9f * t)
            val y = h * 0.6f - (h * 0.35f) * (4 * t * (1 - t))
            val trailLen = w * 0.22f
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, AccentViolet.copy(alpha = 0.75f), AccentCyan),
                ),
                start = Offset((x - trailLen).coerceAtLeast(0f), y),
                end = Offset(x, y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(AccentCyan.copy(alpha = 0.25f), radius = 11.dp.toPx(), center = Offset(x, y))
            drawCircle(AccentCyan, radius = 5.dp.toPx(), center = Offset(x, y))
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo fades in as the streak settles.
            val logoAlpha = streak.value
            androidx.compose.animation.AnimatedVisibility(
                visible = logoAlpha > 0.85f,
                enter = androidx.compose.animation.fadeIn(tween(150)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CometGlyph(size = 56.dp)
                    androidx.compose.foundation.layout.Spacer(
                        Modifier.size(18.dp),
                    )
                    androidx.compose.material3.Text(
                        text = "COMET",
                        style = CometType.Display.copy(letterSpacing = 8.sp),
                        color = com.comet.ui.theme.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun Modifier.androidBackground(): Modifier =
    this.then(Modifier.fillMaxSize().background(BgPrimary))

private fun Modifier.background(color: Color): Modifier =
    androidx.compose.foundation.background(this, color)
