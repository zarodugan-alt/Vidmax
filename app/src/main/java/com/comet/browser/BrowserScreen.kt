package com.comet.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.comet.ui.components.SiteBadge
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometType
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextTertiary

/**
 * In-app browser (S7-lite, phase 1): WebView + URL bar + media-detected FAB (pulses
 * cyan; tap -> analyze the current page URL). Desktop-UA toggle, context-menu download
 * and direct-file interception (aria2 path) ship in phase 3.
 */
@Composable
fun BrowserScreen(
    onBack: () -> Unit,
    onAnalyzeUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var urlInput by rememberSaveable { mutableStateOf("https://duckduckgo.com") }
    var currentUrl by rememberSaveable { mutableStateOf("https://duckduckgo.com") }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { currentUrl = it }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                singleLine = true,
                textStyle = CometType.Caption.copy(color = TextPrimary),
                placeholder = { Text("Search or enter URL", style = CometType.Caption, color = TextTertiary) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val target = normalize(urlInput)
                        urlInput = target
                        currentUrl = target
                        webView.loadUrl(target)
                    },
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SiteBadge(domain = hostOf(currentUrl), size = 20.dp)
            Spacer(Modifier.width(10.dp))
        }

        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
            LaunchedLoad(webView, currentUrl)

            // Media-detected FAB — pulses cyan (S7).
            val pulse = rememberInfiniteTransition(label = "fabPulse")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "fabScale",
            )
            IconButton(
                onClick = { onAnalyzeUrl(webView.url ?: currentUrl) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(AccentCyan),
            ) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = "Download from this page",
                    tint = TextOnAccent,
                )
            }
        }
    }
}

@Composable
private fun LaunchedLoad(webView: WebView, currentUrl: String) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        webView.loadUrl(currentUrl)
    }
}

private fun normalize(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        // Treat bare text as a DuckDuckGo search (not a general browser — Part 4 note).
        "https://duckduckgo.com/?q=" + android.net.Uri.encode(trimmed)
    }
}

private fun hostOf(url: String): String? = runCatching {
    android.net.Uri.parse(url).host
}.getOrNull()
