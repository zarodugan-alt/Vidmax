package com.comet.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens (Part 2.1). Names intentionally match the spec table.
 *
 * Semantic color law (never break): cyan = video, violet = audio — format chips, library
 * cards, notifications, everywhere.
 */

// Backgrounds
val BgPrimary = Color(0xFF070B12)
val BgSecondary = Color(0xFF0D1420)
val BgElevated = Color(0xFF0F1722)

// Glass (API-28: no real blur — gradient + border + shadow fake it, Part 1.4)
val GlassFillTop = Color(0x14FFFFFF)
val GlassFillBottom = Color(0x0DFFFFFF)
val GlassBorder = Color(0x2E00E5FF)

// Accents
val AccentCyan = Color(0xFF00E5FF) // VIDEO · active downloads · primary actions
val AccentCyanDim = Color(0x3300E5FF) // glow halos · progress tracks
val AccentViolet = Color(0xFF7C4DFF) // AUDIO · extraction · audio library cards
val AccentVioletDim = Color(0x337C4DFF)

// Status
val Success = Color(0xFF00E676) // completed · engine healthy
val Warning = Color(0xFFFFD740) // engine update available · paused · low storage
val Danger = Color(0xFFFF5252) // failed · cancel · delete

// Text
val TextPrimary = Color(0xFFE8EEF7)
val TextSecondary = Color(0xFF8A94A6)
val TextTertiary = Color(0xFF5A6478)
val TextOnAccent = Color(0xFF04121A)

/** Dim variant used for glass borders on non-cyan surfaces. */
val GlassBorderViolet = Color(0x2E7C4DFF)
val GlassBorderDanger = Color(0x2EFF5252)
