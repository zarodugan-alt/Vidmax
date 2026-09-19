package com.comet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Spacing/shape/elevation (Part 2.3).
 * Grid 4dp · screen padding 20dp · card padding 16dp · card gaps 12dp · section gap 24dp.
 * Radii: cards 20dp · sheets 28dp top · buttons/chips pill · thumbnails 12dp · format rows 12dp.
 * Elevation: depth = border brightness + shadow (no tonal elevation).
 */
object Spacing {
    val screen = 20.dp
    val card = 16.dp
    val cardGap = 12.dp
    val sectionGap = 24.dp
}

object CometRadius {
    val card = 20.dp
    val sheetTop = 28.dp
    val thumb = 12.dp
    val formatRow = 12.dp
}

val PillShape = RoundedCornerShape(50)

val CometShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(CometRadius.formatRow),
    medium = RoundedCornerShape(CometRadius.card),
    large = RoundedCornerShape(topStart = CometRadius.sheetTop, topEnd = CometRadius.sheetTop),
)
