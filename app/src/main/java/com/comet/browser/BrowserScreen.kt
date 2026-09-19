package com.comet.browser

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comet.ui.components.SiteBadge
import com.comet.ui.sheets.AnalyzeSheet
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.CometType
import com.comet.ui.theme.TextOnAccent
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import com.comet.ui.theme.TextTertiary

private const val HOME_URL = "https://duckduckgo.com"

/**
 * In-app browser (S7) — an independent entry into downloading, not a detour through
 * Home. Own URL bar with live load progress, back/forward/reload/home chrome, desktop
 * UA toggle, and a media FAB that opens the browser's own analyze sheet. Downloads
 * enqueue straight into the shared queue.
 */
@Composable
fun BrowserScreen(
    onBack: () -> Unit,
    onOpenCookiesSettings: () -> Unit,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val analyzeState by viewModel.analyze.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var urlInput by rememberSaveable { mutableStateOf(HOME_URL) }
    var currentUrl by rememberSaveable { mutableStateOf(HOME_URL) }
    var loadProgress by rememberSaveable { mutableIntStateOf(0) }
    var desktopMode by rememberSaveable { mutableStateOf(false) }
    var canGoBack by rememberSaveable { mutableStateOf(false) }
    var canGoForward by rememberSaveable { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let {
                        currentUrl = it
                        urlInput = it
                    }
                    canGoBack = view?.canGoBack() ?: false
                    canGoForward = view?.canGoForward() ?: false
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    loadProgress = newProgress
                }
            }
        }
    }

    // Desktop UA toggle (S7): swap the user agent and reload.
    LaunchedEffect(desktopMode) {
        val ua = if (desktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
        } else {
            webView.settings.userAgentString
        }
        webView.settings.userAgentString = ua
    }
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }
    LaunchedEffect(toast) {
        toast?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    Column(modifier.fillMaxSize()) {
        // ---- URL bar ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Leave browser", tint = TextPrimary)
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
                trailingIcon = if (urlInput.isNotEmpty()) {
                    {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { urlInput = "" },
                        )
                    }
                } else {
                    null
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

        // ---- load progress ----
        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = AccentCyan,
                trackColor = TextTertiary.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        // ---- page ----
        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
            LaunchedEffect(Unit) { webView.loadUrl(currentUrl) }

            // Media-detected FAB — pulses cyan; analysis runs in the browser's own sheet.
            val pulse = rememberInfiniteTransition(label = "fabPulse")
            val scale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "fabScale",
            )
            IconButton(
                onClick = { viewModel.analyzeUrl(webView.url ?: currentUrl) },
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

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ---- browser chrome ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward", tint = if (canGoForward) TextSecondary else TextTertiary)
            }
            IconButton(onClick = { if (webView.canGoForward()) webView.goForward() }, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward", tint = if (canGoForward) TextSecondary else TextTertiary)
            }
            IconButton(onClick = { webView.reload() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = TextSecondary)
            }
            IconButton(onClick = {
                currentUrl = HOME_URL
                urlInput = HOME_URL
                webView.loadUrl(HOME_URL)
            }) {
                Icon(Icons.Rounded.Home, contentDescription = "Home", tint = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { desktopMode = !desktopMode }) {
                Icon(
                    Icons.Rounded.DesktopWindows,
                    contentDescription = if (desktopMode) "Desktop mode on" else "Desktop mode off",
                    tint = if (desktopMode) AccentCyan else TextTertiary,
                )
            }
        }
    }

    // ---- the browser's own analyze sheet + playlist picker (independent of Home) ----
    var playlistInfo by remember { mutableStateOf<com.comet.engine.VideoInfo?>(null) }

    analyzeState?.let { state ->
        AnalyzeSheet(
            state = state,
            hapticsEnabled = true,
            onDismiss = viewModel::dismissAnalyze,
            onDownload = viewModel::download,
            onDownloadPlaylist = viewModel::downloadPlaylist,
            onOpenPlaylistPicker = { info -> playlistInfo = info },
            onReanalyze = viewModel::reanalyze,
            onCopyError = viewModel::copyError,
            onOpenCookiesSettings = onOpenCookiesSettings,
            onUpdateEngine = viewModel::updateEngine,
        )
    }

    playlistInfo?.let { info ->
        com.comet.ui.screens.PlaylistPickerScreen(
            info = info,
            initialSelection = emptySet(),
            onConfirm = { selection ->
                viewModel.downloadPlaylist(info, selection)
                playlistInfo = null
            },
            onDismiss = { playlistInfo = null },
            hapticsEnabled = true,
        )
    }
}

private fun normalize(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://duckduckgo.com/?q=" + android.net.Uri.encode(trimmed)
    }
}

private fun hostOf(url: String): String? = runCatching {
    android.net.Uri.parse(url).host
}.getOrNull()
