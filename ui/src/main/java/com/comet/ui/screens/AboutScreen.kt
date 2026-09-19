package com.comet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.ui.components.CometGlyph
import com.comet.ui.components.SectionHeader
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.BgPrimary
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.Success
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass

/**
 * About screen (S13): app + engine identity, update check, repository and licenses
 * links. Reuses the settings snapshot so versions stay in one place.
 */
@Composable
fun AboutScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onCheckEngineUpdate: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Text("About", style = CometType.Title, color = TextPrimary)
        }

        // ---- identity ----
        Column(
            Modifier
                .fillMaxWidth()
                .glass(corner = 18.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CometGlyph(size = 44.dp)
            Spacer(Modifier.height(10.dp))
            Text("COMET", style = CometType.Display, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Universal media downloader",
                style = CometType.Caption,
                color = TextTertiary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                AboutStat("APP", state.appVersion.ifBlank { "0.1.0" })
                AboutStat("ENGINE", state.engineVersion ?: "bundled")
                AboutStat("CHANNEL", state.engineChannel.name.lowercase())
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- engine ----
        SectionHeader("Engine", color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        GlassRow(
            title = "Check for engine update",
            subtitle = if (state.engineChecking) "Checking…" else "yt-dlp releases (stable / nightly)",
            accent = Success,
            onClick = onCheckEngineUpdate,
        )
        Spacer(Modifier.height(12.dp))

        // ---- project ----
        SectionHeader("Project", color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        GlassRow(
            title = "Source code",
            subtitle = "github.com/zarodugan-alt/Vidmax",
            accent = AccentCyan,
            onClick = { onOpenLink("https://github.com/zarodugan-alt/Vidmax") },
        )
        Spacer(Modifier.height(8.dp))
        GlassRow(
            title = "Open-source licenses",
            subtitle = "GPL-3.0 · see the full list",
            accent = AccentCyan,
            onClick = onOpenLicenses,
        )
        Spacer(Modifier.height(8.dp))
        GlassRow(
            title = "yt-dlp project",
            subtitle = "The engine that powers COMET",
            accent = AccentCyan,
            onClick = { onOpenLink("https://github.com/yt-dlp/yt-dlp") },
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "COMET is a general-purpose media downloader. Users are responsible for " +
                "respecting the terms of service of the sites they use and the copyright " +
                "of the content they download.",
            style = CometType.Caption,
            color = TextTertiary,
            modifier = Modifier.padding(bottom = 28.dp),
        )
    }
}

@Composable
private fun AboutStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = CometType.Telemetry, color = TextTertiary)
        Text(
            value,
            style = CometType.BodyStrong,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GlassRow(
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(corner = 12.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = CometType.Body, color = TextPrimary)
            Text(subtitle, style = CometType.Caption, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
        )
    }
}
