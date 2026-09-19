package com.comet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.data.model.AudioFormatPreference
import com.comet.data.model.EngineChannel
import com.comet.data.model.VideoQualityPreference
import com.comet.ui.components.GlassCard
import com.comet.ui.components.GlassChip
import com.comet.ui.components.SectionHeader
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.Success
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary

/** Snapshot for the settings screen (working S8/S9/S10/S11 subset of phase 1). */
data class SettingsUiState(
    val engineVersion: String? = null,
    val engineChannel: EngineChannel = EngineChannel.STABLE,
    val engineAutoUpdate: Boolean = true,
    val engineChecking: Boolean = false,
    val defaultQuality: VideoQualityPreference = VideoQualityPreference.P1080,
    val defaultAudioFormat: AudioFormatPreference = AudioFormatPreference.MP3_320,
    val concurrency: Int = 2,
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val haptics: Boolean = true,
    val filenameTemplate: String = "{title} - {channel} [{quality}]",
    val appVersion: String = "",
    val verboseLogging: Boolean = false,
    val cookiesImportedAt: Long? = null,
    val storageFreeBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
)

/**
 * Settings (S8 root with Engine/Downloads/Appearance live; Storage/Advanced/About
 * sections land with their phases — noted inline).
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onChannelChange: (EngineChannel) -> Unit,
    onCheckEngineUpdate: () -> Unit,
    onEngineAutoUpdateChange: (Boolean) -> Unit,
    onQualityChange: (VideoQualityPreference) -> Unit,
    onAudioFormatChange: (AudioFormatPreference) -> Unit,
    onConcurrencyChange: (Int) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onChargingOnlyChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onTemplateChange: (String) -> Unit,
    onVerboseLoggingChange: (Boolean) -> Unit = {},
    onImportCookies: () -> Unit = {},
    onReinstallEngine: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = CometType.Title, color = TextPrimary)
        }

        // ---- Engine (S9) ----
        SectionHeader("Engine", color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("yt-dlp engine", style = CometType.Body, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("● ${state.engineVersion ?: "bundled"}", style = CometType.Telemetry, color = Success)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Channel", style = CometType.Caption, color = TextTertiary,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("Stable", selected = state.engineChannel == EngineChannel.STABLE, onClick = { onChannelChange(EngineChannel.STABLE) })
                GlassChip("Nightly", selected = state.engineChannel == EngineChannel.NIGHTLY, onClick = { onChannelChange(EngineChannel.NIGHTLY) })
            }
            if (state.engineChannel == EngineChannel.NIGHTLY) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Nightly fixes sites faster but may be unstable.",
                    style = CometType.Caption,
                    color = com.comet.ui.theme.Warning,
                )
            }
            Spacer(Modifier.height(12.dp))
            ToggleRow("Auto-update on Wi-Fi", state.engineAutoUpdate, onEngineAutoUpdateChange)
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.engineChecking) "Checking…" else "Check for update",
                style = CometType.Button,
                color = AccentCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { onCheckEngineUpdate() },
            )
        }

        Spacer(Modifier.height(Spacing.sectionGap / 2))

        // ---- Downloads (S10) ----
        SectionHeader("Downloads", color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            Text("Default video quality", style = CometType.Caption, color = TextTertiary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("Ask", selected = state.defaultQuality == VideoQualityPreference.ASK, onClick = { onQualityChange(VideoQualityPreference.ASK) })
                GlassChip("2160", selected = state.defaultQuality == VideoQualityPreference.P2160, onClick = { onQualityChange(VideoQualityPreference.P2160) })
                GlassChip("1080", selected = state.defaultQuality == VideoQualityPreference.P1080, onClick = { onQualityChange(VideoQualityPreference.P1080) })
                GlassChip("720", selected = state.defaultQuality == VideoQualityPreference.P720, onClick = { onQualityChange(VideoQualityPreference.P720) })
            }
            Spacer(Modifier.height(12.dp))
            Text("Default audio format", style = CometType.Caption, color = TextTertiary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("MP3", selected = state.defaultAudioFormat == AudioFormatPreference.MP3_320, onClick = { onAudioFormatChange(AudioFormatPreference.MP3_320) })
                GlassChip("M4A", selected = state.defaultAudioFormat == AudioFormatPreference.M4A_128, onClick = { onAudioFormatChange(AudioFormatPreference.M4A_128) })
                GlassChip("Opus", accent = com.comet.ui.components.GlassAccent.VIOLET, selected = state.defaultAudioFormat == AudioFormatPreference.OPUS_160, onClick = { onAudioFormatChange(AudioFormatPreference.OPUS_160) })
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Concurrent downloads: ${state.concurrency}",
                style = CometType.Caption,
                color = TextSecondary,
            )
            Slider(
                value = state.concurrency.toFloat(),
                onValueChange = { onConcurrencyChange(it.toInt().coerceIn(1, 3)) },
                valueRange = 1f..3f,
                steps = 1,
            )
            ToggleRow("Wi-Fi only", state.wifiOnly, onWifiOnlyChange)
            ToggleRow("Download only while charging", state.chargingOnly, onChargingOnlyChange)
        }

        Spacer(Modifier.height(Spacing.sectionGap / 2))

        // ---- Appearance (S12-lite: haptics) ----
        SectionHeader("Appearance", color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            ToggleRow("Haptics", state.haptics, onHapticsChange)
            Text(
                "Theme is fixed dark — COMET is a comet at night.",
                style = CometType.Caption,
                color = TextTertiary,
            )
        }

        Spacer(Modifier.height(Spacing.sectionGap / 2))

        // ---- Storage (S11) ----
        SectionHeader("Storage", color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            val usedFrac = if (state.storageTotalBytes > 0) {
                (1f - state.storageFreeBytes.toFloat() / state.storageTotalBytes).coerceIn(0f, 1f)
            } else {
                0f
            }
            Text("Device storage", style = CometType.Body, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { usedFrac },
                color = AccentCyan,
                trackColor = TextTertiary.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${com.comet.ui.components.Format.bytes(state.storageFreeBytes)} free of " +
                    "${com.comet.ui.components.Format.bytes(state.storageTotalBytes)}",
                style = CometType.Telemetry,
                color = TextTertiary,
            )
            Spacer(Modifier.height(14.dp))
            var template by remember(state.filenameTemplate) { mutableStateOf(state.filenameTemplate) }
            OutlinedTextField(
                value = template,
                onValueChange = {
                    template = it
                    onTemplateChange(it)
                },
                label = { Text("Filename template", style = CometType.Caption) },
                supportingText = {
                    Text(
                        "Tokens: {title} {channel} {quality} {date} {id} {site}",
                        style = CometType.Caption,
                        color = TextTertiary,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Folder: Movies/COMET (video) · Music/COMET (audio). SD card and SAF folders ship in phase 4.",
                style = CometType.Caption,
                color = TextTertiary,
            )
        }

        Spacer(Modifier.height(Spacing.sectionGap / 2))

        // ---- Advanced (S12) ----
        SectionHeader("Advanced", color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            ToggleRow("Verbose engine logging", state.verboseLogging, onVerboseLoggingChange)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cookies", style = CometType.Body, color = TextPrimary)
                    Text(
                        if (state.cookiesImportedAt != null) {
                            "Imported — private sites and age-gated videos work"
                        } else {
                            "For private / age-gated videos"
                        },
                        style = CometType.Caption,
                        color = TextTertiary,
                    )
                }
                androidx.compose.material3.OutlinedButton(onClick = onImportCookies) {
                    Text(
                        if (state.cookiesImportedAt != null) "Replace" else "Import",
                        style = CometType.Button,
                        color = AccentCyan,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reinstall engine", style = CometType.Body, color = TextPrimary)
                    Text(
                        "Redeploys the bundled yt-dlp + FFmpeg binaries",
                        style = CometType.Caption,
                        color = TextTertiary,
                    )
                }
                androidx.compose.material3.OutlinedButton(onClick = onReinstallEngine) {
                    Text("Reinstall", style = CometType.Button, color = com.comet.ui.theme.Warning)
                }
            }
        }

        Spacer(Modifier.height(Spacing.sectionGap / 2))

        // ---- About (S13) ----
        SectionHeader("About", color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            InfoLine("App version", state.appVersion)
            InfoLine("Engine", state.engineVersion ?: "bundled")
            InfoLine(
                "Privacy",
                "COMET has no servers and collects nothing.",
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Open the About screen →",
                style = CometType.Button,
                color = AccentCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAbout)
                    .padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}


@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = CometType.Body, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AccentCyan),
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = CometType.Caption, color = TextTertiary, modifier = Modifier.width(96.dp))
        Text(value, style = CometType.Caption, color = TextSecondary)
    }
}
