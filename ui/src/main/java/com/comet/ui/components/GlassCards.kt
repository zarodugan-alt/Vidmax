package com.comet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentCyanDim
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.AccentVioletDim
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometRadius
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.GlassBorder
import com.comet.ui.theme.GlassBorderDanger
import com.comet.ui.theme.GlassBorderViolet
import com.comet.ui.theme.GlassFillBottom
import com.comet.ui.theme.GlassFillTop
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

enum class GlassAccent { CYAN, VIOLET, DANGER, NEUTRAL }

internal fun GlassAccent.borderColor(): Color = when (this) {
    GlassAccent.CYAN -> GlassBorder
    GlassAccent.VIOLET -> GlassBorderViolet
    GlassAccent.DANGER -> GlassBorderDanger
    GlassAccent.NEUTRAL -> GlassBorder
}

internal fun GlassAccent.solidColor(): Color = when (this) {
    GlassAccent.CYAN -> AccentCyan
    GlassAccent.VIOLET -> AccentViolet
    GlassAccent.DANGER -> Danger
    GlassAccent.NEUTRAL -> AccentCyan
}

/** Glass card (Part 6): gradient fill + 1dp border, no tonal elevation. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: GlassAccent = GlassAccent.NEUTRAL,
    corner: Dp = CometRadius.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .glass(corner, accent.borderColor())
            .padding(Spacing.card),
        content = content,
    )
}

/** Pill chip (Part 6): format chips, filter chips, clipboard chip. Semantic color law applies. */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: GlassAccent = GlassAccent.NEUTRAL,
    selected: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val border = if (selected) accent.solidColor().copy(alpha = 0.6f) else accent.borderColor()
    val fill = if (selected) {
        when (accent) {
            GlassAccent.CYAN -> AccentCyanDim
            GlassAccent.VIOLET -> AccentVioletDim
            else -> GlassFillTop
        }
    } else {
        Color.Transparent
    }
    Row(
        modifier = modifier
            .clip(PillShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(32.dp)
            .background(
                if (selected) Brush.verticalGradient(listOf(fill, fill)) else
                    Brush.verticalGradient(listOf(GlassFillTop, GlassFillBottom))
            )
            .border(1.dp, border, PillShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (leading != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        }
        Text(
            text = text,
            style = CometType.Button,
            color = when {
                !enabled -> TextTertiary.copy(alpha = 0.4f)
                selected -> TextPrimary
                else -> TextTertiary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
