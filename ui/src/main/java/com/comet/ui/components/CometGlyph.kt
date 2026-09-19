package com.comet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet

/**
 * Static comet glyph: cyan head with violet-fading trail — the header logo mark.
 * (The animated version lives in the splash screen.)
 */
@Composable
fun CometGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    headColor: Color = AccentCyan,
    trailColor: Color = AccentViolet,
) {
    Canvas(modifier.size(size)) {
        val w = size.width
        val h = size.height
        val head = Offset(w * 0.72f, h * 0.28f)
        val tail = Offset(w * 0.12f, h * 0.88f)

        drawLine(
            brush = Brush.linearGradient(
                listOf(Color.Transparent, trailColor.copy(alpha = 0.65f), headColor),
            ),
            start = tail,
            end = head,
            strokeWidth = (size.toPx() / 7f).coerceAtLeast(2.dp.toPx()),
            cap = StrokeCap.Round,
        )
        drawCircle(headColor.copy(alpha = 0.22f), radius = size.toPx() / 6f, center = head)
        drawCircle(headColor, radius = size.toPx() / 12f, center = head)
    }
}
