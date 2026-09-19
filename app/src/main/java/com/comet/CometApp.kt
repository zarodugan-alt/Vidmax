package com.comet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.comet.browser.BrowserScreen
import com.comet.browser.BrowserViewModel
import com.comet.home.HomeViewModel
import com.comet.player.PlayerScreen
import com.comet.player.PlayerViewModel
import com.comet.settings.SettingsViewModel
import com.comet.engine.EngineStatus
import com.comet.ui.home.HomeScreen
import com.comet.ui.screens.AboutScreen
import com.comet.ui.screens.DisclaimerScreen
import com.comet.ui.screens.LicensesScreen
import com.comet.ui.screens.SettingsScreen
import com.comet.ui.screens.SplashScreen
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometBackground
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary
import java.io.File

/**
 * Root navigation (Part 4): splash -> disclaimer (first run) -> home. Browser and
 * player are stack destinations; the browser is a fully independent download entry
 * (own analyze sheet + playlist picker) and never routes through home.
 */
@Composable
fun CometApp(viewModel: HomeViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val redownloadPrompt by viewModel.redownloadPrompt.collectAsStateWithLifecycle()
    val shareRequest by viewModel.shareRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var disclaimerAccepted by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        disclaimerAccepted = viewModel.isDisclaimerAccepted()
    }

    // Notification [Play] and library taps land here.
    LaunchedEffect(Unit) {
        viewModel.openPlayerRequest.collect { id ->
            if (id != null) {
                navController.navigate("player/$id")
                viewModel.consumeOpenPlayer()
            }
        }
    }

    // Library share: FileProvider uri -> system share sheet.
    LaunchedEffect(shareRequest) {
        val entity = shareRequest ?: return@LaunchedEffect
        val path = entity.finalPath ?: run {
            viewModel.consumeShareRequest()
            return@LaunchedEffect
        }
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(path),
            )
            val mime = if (entity.isAudio) "audio/*" else "video/*"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share via"))
        }
        viewModel.consumeShareRequest()
    }

    /** Opens external links (About/Licenses) with the browser. */
    val openLink: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("splash") {
            SplashScreen(
                engineReady = uiState.engineStatus == EngineStatus.READY,
                onFinished = {
                    navController.navigate(
                        if (disclaimerAccepted == true) "home" else "disclaimer",
                    ) {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }

        composable("disclaimer") {
            CometBackground {
                DisclaimerScreen(
                    onAccept = {
                        viewModel.acceptDisclaimer()
                        navController.navigate("home") {
                            popUpTo("disclaimer") { inclusive = true }
                        }
                    },
                )
            }
        }

        composable("home") {
            CometBackground {
                HomeScreen(
                    state = uiState,
                    onAnalyzeUrl = viewModel::analyzeUrl,
                    onCheckClipboard = viewModel::checkClipboard,
                    onClipboardConsumed = viewModel::consumeClipboard,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onCopyError = viewModel::copyError,
                    onClearFinished = viewModel::clearFinished,
                    onPauseAll = viewModel::pauseAll,
                    onResumeAll = viewModel::resumeAll,
                    onOpenLibraryItem = { viewModel.openPlayer(it.id) },
                    onDeleteLibraryItem = viewModel::deleteLibraryItem,
                    onShareLibraryItem = viewModel::shareEntity,
                    onRedownloadLibraryItem = viewModel::redownload,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenBrowser = { navController.navigate("browser") },
                    onEngineUpdate = viewModel::onEngineUpdateClicked,
                    onReinstallEngine = viewModel::reinstallEngine,
                    onDownload = viewModel::download,
                    onDownloadPlaylist = viewModel::downloadPlaylist,
                    onOpenPlaylistPicker = viewModel::openPlaylistPicker,
                    onConfirmPlaylistPicker = viewModel::confirmPlaylistPicker,
                    onClosePlaylistPicker = viewModel::closePlaylistPicker,
                    onDismissAnalyze = viewModel::dismissAnalyze,
                    onReanalyze = viewModel::reanalyze,
                    onConsumePrefill = viewModel::consumePrefill,
                )
            }
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val settingsToast by settingsViewModel.toast.collectAsStateWithLifecycle()
            val cookiePicker = rememberLauncherForCookies { uri ->
                uri?.let(settingsViewModel::importCookies)
            }
            LaunchedEffect(settingsToast) {
                // Toast is surfaced via snackbar-less inline: simplest is android.widget.Toast.
                settingsToast?.let {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    settingsViewModel.consumeToast()
                }
            }
            CometBackground {
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onChannelChange = settingsViewModel::setChannel,
                    onCheckEngineUpdate = settingsViewModel::checkForUpdate,
                    onEngineAutoUpdateChange = settingsViewModel::setEngineAutoUpdate,
                    onQualityChange = settingsViewModel::setDefaultQuality,
                    onAudioFormatChange = settingsViewModel::setDefaultAudioFormat,
                    onConcurrencyChange = settingsViewModel::setConcurrency,
                    onWifiOnlyChange = settingsViewModel::setWifiOnly,
                    onChargingOnlyChange = settingsViewModel::setChargingOnly,
                    onHapticsChange = settingsViewModel::setHaptics,
                    onTemplateChange = settingsViewModel::setTemplate,
                    onVerboseLoggingChange = settingsViewModel::setVerboseLogging,
                    onImportCookies = { cookiePicker.launch(arrayOf("*/*")) },
                    onReinstallEngine = settingsViewModel::reinstallEngine,
                    onOpenAbout = { navController.navigate("about") },
                )
            }
        }

        composable("about") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
            CometBackground {
                AboutScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onCheckEngineUpdate = settingsViewModel::checkForUpdate,
                    onOpenLicenses = { navController.navigate("licenses") },
                    onOpenLink = openLink,
                )
            }
        }

        composable("licenses") {
            CometBackground {
                LicensesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLink = openLink,
                )
            }
        }

        composable("browser") {
            val browserViewModel: BrowserViewModel = hiltViewModel()
            CometBackground {
                BrowserScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCookiesSettings = { navController.navigate("settings") },
                    viewModel = browserViewModel,
                )
            }
        }

        composable(
            route = "player/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val entity by playerViewModel.entity.collectAsStateWithLifecycle()
            PlayerScreen(
                entity = entity,
                onBack = { navController.popBackStack() },
            )
        }
    }

    // Global "Already downloaded — download again?" prompt (Part 11).
    redownloadPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRedownload,
            containerColor = BgElevated,
            title = {
                Text(
                    "Already downloaded",
                    style = CometType.Title,
                    color = TextPrimary,
                )
            },
            text = {
                Text(
                    "\"${prompt.existing.title}\" is already in your library or queue. " +
                        "Download it again?",
                    style = CometType.Body,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRedownload) {
                    Text("Again", style = CometType.Button, color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRedownload) {
                    Text("Cancel", style = CometType.Button, color = Danger)
                }
            },
        )
    }
}

/** SAF picker for cookies.txt (S12). */
@Composable
private fun rememberLauncherForCookies(
    onPicked: (Uri?) -> Unit,
): androidx.activity.result.ActivityResultLauncher<Array<String>> =
    androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri -> onPicked(uri) },
    )
