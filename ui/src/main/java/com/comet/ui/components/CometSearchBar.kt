package com.comet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.CometType
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass

private val URL_PATTERN = Regex("""^https?://\S+\.\S+""", RegexOption.IGNORE_CASE)

fun isValidUrl(text: String): Boolean = URL_PATTERN.matches(text.trim())

/**
 * Paste bar (S1): glass bar with link glyph, placeholder "Paste link…", violet
 * "Pasted from clipboard" chip, and the [→] submit arrow (disabled until the URL is valid).
 * Never auto-analyzes — the user taps the arrow (Product Pillar 1).
 */
@Composable
fun CometSearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    clipboardApplied: Boolean = false,
    onClearClipboardChip: () -> Unit = {},
) {
    val valid = isValidUrl(text)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass(corner = 50.dp)
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.ContentPaste,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.padding(start = 10.dp).size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = CometType.Body.copy(color = TextPrimary),
                cursorBrush = SolidColor(AccentCyan),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            "Paste link…",
                            style = CometType.Body,
                            color = TextTertiary,
                        )
                    } else {
                        inner()
                    }
                },
            )
        }
        if (clipboardApplied && text.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(AccentViolet.copy(alpha = 0.18f))
                    .clickable { onClearClipboardChip() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pasted",
                    style = TextStyle(
                        fontFamily = CometType.Button.fontFamily,
                        fontWeight = CometType.Button.fontWeight,
                        fontSize = CometType.Button.fontSize,
                        letterSpacing = CometType.Button.letterSpacing,
                    ),
                    color = AccentViolet,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear pasted chip",
                    tint = AccentViolet,
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        IconButton(
            onClick = { if (valid) onSubmit(text.trim()) },
            enabled = valid,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (valid) AccentCyan else AccentCyan.copy(alpha = 0.15f),
                    PillShape,
                ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Analyze link",
                tint = if (valid) TextOnAccent else TextTertiary,
            )
        }
    }
}
