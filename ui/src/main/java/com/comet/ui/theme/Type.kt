package com.comet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.comet.ui.R

/**
 * Typography (Part 2.2). TTFs bundled in res/font (all OFL — see licenses/).
 * Space Grotesk (display/title/section) · Inter (body/caption/button) · JetBrains Mono (telemetry).
 */

val FontSpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
)

val FontInter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

val FontJetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
)

object CometType {
    /** Display 28/34 Medium — empty states, big numbers. */
    val Display = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    )

    /** Title 20/28 Medium — screen titles. */
    val Title = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    )

    /** Section 13/16 Medium, letterSpacing 1.5sp, CAPS — "ACTIVE", "VIDEO", "AUDIO ONLY". */
    val Section = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp,
    )

    /** Body 15/22 Normal — titles, descriptions. */
    val Body = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    /** BodyStrong 15/22 Medium — emphasis. */
    val BodyStrong = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    /** Caption 12/16 Normal — metadata, site names. */
    val Caption = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Telemetry 12/16 JetBrains Mono — speeds, sizes, ETA, percentages. */
    val Telemetry = TextStyle(
        fontFamily = FontJetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Button 14/20 Medium, letterSpacing 0.5sp. */
    val Button = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    )
}

/** Material slots mapped onto the spec styles for Material component defaults. */
val CometTypography = Typography(
    displayMedium = CometType.Display,
    titleLarge = CometType.Title,
    titleSmall = CometType.Section,
    bodyLarge = CometType.Body,
    bodyMedium = CometType.BodyStrong,
    bodySmall = CometType.Caption,
    labelLarge = CometType.Button,
    labelSmall = CometType.Telemetry,
)
