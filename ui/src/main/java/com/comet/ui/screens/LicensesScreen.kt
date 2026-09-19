package com.comet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Spacing
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary
import com.comet.ui.theme.glass

/** One bundled/linked component and its license (S13). */
data class LicenseEntry(
    val name: String,
    val coordinates: String,
    val license: String,
    val note: String? = null,
)

/** Mirrors THIRD_PARTY_NOTICES.md — the single curated list (S13, AC #14). */
val LICENSES: List<LicenseEntry> = listOf(
    LicenseEntry(
        "youtubedl-android",
        "io.github.junkfood02.youtubedl-android 0.18.1",
        "GPL-3.0",
        "Ships yt-dlp — the reason COMET itself is GPL-3.0",
    ),
    LicenseEntry("yt-dlp", "bundled binary", "Unlicense (public domain)"),
    LicenseEntry("Kotlin / Coroutines", "org.jetbrains.kotlin", "Apache-2.0"),
    LicenseEntry("Jetpack Compose + Material 3", "androidx.compose", "Apache-2.0"),
    LicenseEntry("AndroidX (core, activity, lifecycle, navigation, Room, DataStore)", "androidx", "Apache-2.0"),
    LicenseEntry("Media3 / ExoPlayer", "androidx.media3", "Apache-2.0"),
    LicenseEntry("Coil", "io.coil-kt:coil-compose", "Apache-2.0"),
    LicenseEntry("Hilt / Dagger", "com.google.dagger", "Apache-2.0"),
    LicenseEntry("Timber", "com.jakewharton.timber", "Apache-2.0"),
    LicenseEntry("Space Grotesk (bundled font)", "res/font", "OFL-1.1"),
    LicenseEntry("Inter (bundled font)", "res/font", "OFL-1.1"),
    LicenseEntry("JetBrains Mono (bundled font)", "res/font", "OFL-1.1"),
)

/**
 * Licenses screen (S13): every library the app links or bundles, with the GPL
 * deviation note up top. Hand-rolled instead of AboutLibraries so the list stays
 * exactly in sync with the shipped dependency set.
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(com.comet.ui.theme.BgPrimary)
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
            Text("Open-source licenses", style = CometType.Title, color = TextPrimary)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .glass(corner = 14.dp)
                .padding(14.dp),
        ) {
            Column {
                Text(
                    "COMET is GPL-3.0",
                    style = CometType.BodyStrong,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "The engine runtime (youtubedl-android) is GPL-3.0 and links as a " +
                        "library, so the whole app ships under GPL-3.0. Source code: " +
                        "github.com/zarodugan-alt/Vidmax",
                    style = CometType.Caption,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "View repository",
                    style = CometType.Button,
                    color = com.comet.ui.theme.AccentCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenLink("https://github.com/zarodugan-alt/Vidmax") },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LICENSES) { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(corner = 12.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        entry.license.startsWith("GPL") -> com.comet.ui.theme.Warning
                                        entry.license.startsWith("OFL") -> com.comet.ui.theme.AccentViolet
                                        else -> com.comet.ui.theme.AccentCyan
                                    },
                                ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            entry.name,
                            style = CometType.BodyStrong,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            entry.license,
                            style = CometType.Telemetry,
                            color = TextSecondary,
                        )
                    }
                    entry.coordinates.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = CometType.Telemetry, color = TextTertiary)
                    }
                    entry.note?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = CometType.Caption, color = TextTertiary)
                    }
                }
            }
        }
    }
}

