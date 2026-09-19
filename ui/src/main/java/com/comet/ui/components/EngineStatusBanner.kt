package com.comet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comet.engine.EngineStatus
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.CometType
import com.comet.ui.theme.PillShape
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.glassDanger

/**
 * Engine status states (Part 6): warming / updating / ready(hidden).
 * A failed engine surfaces as the Part 11 blocking error card with [Reinstall engine].
 */
@Composable
fun EngineStatusBanner(
    status: EngineStatus,
    updating: Boolean,
    onReinstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        updating -> {
            Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                CometRing(progress = null, ringSize = 18.dp, strokeWidth = 2.dp, dim = true)
                Spacer(Modifier.width(10.dp))
                Text("Updating engine…", style = CometType.Caption, color = TextSecondary)
            }
        }

        status == EngineStatus.WARMING || status == EngineStatus.COLD -> {
            Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                CometRing(progress = null, ringSize = 18.dp, strokeWidth = 2.dp, dim = true)
                Spacer(Modifier.width(10.dp))
                Text("Engine warming up…", style = CometType.Caption, color = TextSecondary)
            }
        }

        status == EngineStatus.FAILED -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .glassDanger()
                    .padding(16.dp),
            ) {
                Text(
                    "Engine failed to load",
                    style = CometType.BodyStrong,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "The bundled yt-dlp runtime could not start. Reinstalling re-extracts it from the app package and restarts COMET.",
                    style = CometType.Caption,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onReinstall,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = TextOnAccent,
                    ),
                ) {
                    Text("Reinstall engine", style = CometType.Button)
                }
            }
        }

        else -> Unit // READY — hidden
    }
}
