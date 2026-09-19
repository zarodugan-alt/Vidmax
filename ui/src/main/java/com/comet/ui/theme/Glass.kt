package com.comet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Glassmorphism (Part 2.4) — faked on API 28: vertical gradient fill + 1dp border.
 * `Modifier.blur()`/`RenderEffect` are API 31+, so glass is never real blur (Part 1.4).
 */
fun Modifier.glass(corner: Dp = CometRadius.card, borderColor: Color = GlassBorder): Modifier =
    this
        .clip(RoundedCornerShape(corner))
        .background(Brush.verticalGradient(listOf(GlassFillTop, GlassFillBottom)))
        .border(1.dp, borderColor, RoundedCornerShape(corner))

/** Violet-tinted glass for audio surfaces. */
fun Modifier.glassViolet(corner: Dp = CometRadius.card): Modifier =
    glass(corner, GlassBorderViolet)

/** Danger-tinted glass for failed cards / destructive surfaces. */
fun Modifier.glassDanger(corner: Dp = CometRadius.card): Modifier =
    glass(corner, GlassBorderDanger)

/** Solid elevated surface — sheets over busy content like video thumbnails. */
fun Modifier.glassSolid(corner: Dp = CometRadius.card): Modifier =
    this
        .clip(RoundedCornerShape(corner))
        .background(BgElevated)
        .border(1.dp, TextTertiary.copy(alpha = 0.25f), RoundedCornerShape(corner))
