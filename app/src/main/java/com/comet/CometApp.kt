package com.comet

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.comet.browser.BrowserScreen
import com.comet.home.HomeViewModel
import com.comet.player.PlayerScreen
import com.comet.player.PlayerViewModel
import com.comet.settings.SettingsViewModel
import com.comet.engine.EngineStatus
import com.comet.ui.home.HomeScreen
import com.comet.ui.screens.DisclaimerScreen
import com.comet.ui.screens.SettingsScreen
import com.comet.ui.screens.SplashScreen
import com.comet.ui.theme.BgElevated
import com.comet.ui.theme.CometBackground
import com.comet.ui.theme.CometType
import com.comet.ui.theme.Danger
import com.comet.ui.theme.TextPrimary
import com.comet.ui.theme.TextSecondary

/**
 * Root navigation (Part 4): splash -> disclaimer (first run) -> home, with the analyze
 * sheet shown over home; browser and player as stack destinations.
 */
@Composable
fun CometApp(viewModel: HomeViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val redownloadPrompt by viewModel.redownloadPrompt.collectAsStateWithLifecycle()

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
                    onOpenLibraryItem = { viewModel.openPlayer(it.id) },
                    onDeleteLibraryItem = viewModel::deleteLibraryItem,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenBrowser = { navController.navigate("browser") },
                    onEngineUpdate = viewModel::onEngineUpdateClicked,
                    onReinstallEngine = viewModel::reinstallEngine,
                    onDownload = viewModel::download,
                    onDownloadPlaylist = viewModel::downloadPlaylist,
                    onDismissAnalyze = viewModel::dismissAnalyze,
                    onReanalyze = viewModel::reanalyze,
                    onConsumePrefill = viewModel::consumePrefill,
                )
            }
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
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
                )
            }
        }

        composable("browser") {
            CometBackground {
                BrowserScreen(
                    onBack = { navController.popBackStack() },
                    onAnalyzeUrl = { url ->
                        // S7: the FAB analyzes the current page — home hosts the sheet (P1).
                        viewModel.analyzeUrl(url)
                        navController.navigate("home") {
                            launchSingleTop = true
                            popUpTo("home") { inclusive = false }
                        }
                    },
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
