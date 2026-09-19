package com.comet.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.CometType
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.glassSolid

/**
 * Disclaimer (S0.5, first run only): glass solid card, centered, one-time acceptance.
 * No dark patterns, no forced scrolling.
 */
@Composable
fun DisclaimerScreen(
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .glassSolid()
                .padding(Spacing.card + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Before you download",
                style = CometType.Title,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "COMET is a tool. Download only content you have the right to download — " +
                    "your own content, Creative Commons, or with permission.",
                style = CometType.Body,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "COMET has no servers, no accounts, and collects nothing. " +
                    "You are responsible for what you download.",
                style = CometType.Caption,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onAccept,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = TextOnAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("I understand", style = CometType.Button)
            }
        }
    }
}
