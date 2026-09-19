package com.comet.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyanDim
import com.comet.ui.theme.BgElevated

/**
 * Analyze shimmer (Part 3.4): diagonal sweep, 1200ms, AccentCyanDim over BgElevated.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    background(
        Brush.linearGradient(
            colors = listOf(
                BgElevated,
                AccentCyanDim.copy(alpha = 0.35f),
                BgElevated,
            ),
            start = Offset(progress * 600f, 0f),
            end = Offset(progress * 600f + 320f, 220f),
        ),
    )
}

/** Skeleton block used while yt-dlp JSON loads. */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, height: Dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .shimmer(),
    )
}
