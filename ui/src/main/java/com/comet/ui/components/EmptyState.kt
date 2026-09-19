package com.comet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.CometType
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary

/**
 * Empty state (Part 3.4): comet streaks across once on entry (600ms arc path),
 * then settles into a static illustration.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sweep.animateTo(1f, animationSpec = tween(600))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(120.dp, 56.dp)) {
            val w = size.width
            val h = size.height
            val t = sweep.value
            // Arc path from left edge to the resting comet at right.
            val startX = 0f
            val endX = w * 0.78f
            val x = startX + (endX - startX) * t
            val y = h * 0.7f - (h * 0.5f) * (4 * t * (1 - t)) // parabola

            // Fading trail (violet -> cyan) behind the head.
            val trailLen = w * 0.28f
            drawLine(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color.Transparent, AccentViolet.copy(alpha = 0.7f), AccentCyan),
                ),
                start = Offset((x - trailLen).coerceAtLeast(0f), y + (x - (x - trailLen)) * 0.02f),
                end = Offset(x, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(AccentCyan, radius = 4.dp.toPx(), center = Offset(x, y))
            if (t >= 1f) {
                drawCircle(AccentCyan.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(x, y))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = CometType.Display, color = TextSecondary, modifier = Modifier.padding(horizontal = 24.dp))
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = CometType.Body,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = com.comet.ui.theme.PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = TextOnAccent,
                ),
            ) {
                Text(actionLabel, style = CometType.Button)
            }
        }
    }
}
