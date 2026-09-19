package com.comet.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Motion
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass

/** CAPS section header with optional trailing content (S1 "ACTIVE", S2 "VIDEO"). */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = CometType.Section,
            color = color,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * Sliding pill tabs (S1 "QUEUE / LIBRARY"). The pill slides with DefaultSpring;
 * tab content transitions live in HomeScreen (slide + fade, 280ms).
 */
@Composable
fun TabPills(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .glass(corner = 50.dp)
            .padding(4.dp),
    ) {
        val count = tabs.size.coerceAtLeast(1)
        val pillWidth = (maxWidth - 8.dp) / count
        val targetOffset = pillWidth * selected
        val pillOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Motion.DefaultSpring.dampingRatio,
                stiffness = Motion.DefaultSpring.stiffness,
            ),
            label = "tabPill",
        )

        Box(
            Modifier
                .offset(x = pillOffset)
                .width(pillWidth)
                .height(40.dp)
                .clip(PillShape)
                .background(AccentCyan.copy(alpha = 0.16f)),
        )

        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(PillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = CometType.Button,
                        color = if (isSelected) TextPrimary else TextTertiary,
                    )
                }
            }
        }
    }
}
