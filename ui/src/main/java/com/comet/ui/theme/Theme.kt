package com.comet.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.comet.ui.R

/**
 * COMET theme: fixed dark, high-tech. Background recipe (Part 2.1): BgPrimary +
 * radial gradient (center-top 40% -> BgSecondary) + static 4%-opacity starfield PNG
 * (one asset, never animated).
 */
private val CometColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = TextOnAccent,
    secondary = AccentViolet,
    onSecondary = TextPrimary,
    tertiary = Success,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgElevated,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = TextOnAccent,
    outline = TextTertiary,
    outlineVariant = GlassBorder,
)

@Composable
fun CometTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CometColorScheme,
        typography = CometTypography,
        shapes = CometShapes,
        content = content,
    )
}

/** The app-wide background: base color, radial glow, starfield, then content. */
@Composable
fun CometBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
    ) {
        // Radial gradient: BgSecondary bleeding in from the edges, center-top at 40% height.
        Box(
            Modifier
                .matchParentSize()
                .cometGradient(),
        )
        Image(
            painter = painterResource(R.drawable.starfield),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
        content()
    }
}

private fun Modifier.cometGradient(): Modifier = this.then(
    androidx.compose.ui.draw.drawBehind {
        val radius = size.maxDimension * 0.95f
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color.Transparent,
                    BgSecondary.copy(alpha = 0.9f),
                ),
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.5f,
                    size.height * 0.40f,
                ),
                radius = radius,
            ),
        )
    },
)
