package com.comet.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comet.BuildConfig
import com.comet.data.datastore.SettingsRepository
import com.comet.data.model.AudioFormatPreference
import com.comet.data.model.EngineChannel
import com.comet.data.model.VideoQualityPreference
import com.comet.download.storage.StorageBridge
import com.comet.engine.EngineManager
import com.comet.ui.screens.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val engineManager: EngineManager,
    private val storageBridge: StorageBridge,
) : ViewModel() {

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        engineManager.version,
        engineManager.updating,
        engineManager.status,
    ) { settings, version, updating, _ ->
        SettingsUiState(
            engineVersion = version,
            engineChannel = settings.engineChannel,
            engineAutoUpdate = settings.engineAutoUpdate,
            defaultQuality = settings.defaultVideoQuality,
            defaultAudioFormat = settings.defaultAudioFormat,
            concurrency = settings.concurrency,
            wifiOnly = settings.wifiOnly,
            chargingOnly = settings.chargingOnly,
            haptics = settings.haptics,
            filenameTemplate = settings.filenameTemplate,
            appVersion = BuildConfig.VERSION_NAME,
            verboseLogging = settings.verboseLogging,
            cookiesImportedAt = settings.cookiesImportedAt,
            storageFreeBytes = storageBridge.freeBytes(),
            storageTotalBytes = storageBridge.totalBytes(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setChannel(channel: EngineChannel) = settingsRepository.setEngineChannel(channel)
    fun setEngineAutoUpdate(value: Boolean) = settingsRepository.setEngineAutoUpdate(value)
    fun setDefaultQuality(value: VideoQualityPreference) = settingsRepository.setDefaultVideoQuality(value)
    fun setDefaultAudioFormat(value: AudioFormatPreference) = settingsRepository.setDefaultAudioFormat(value)
    fun setConcurrency(value: Int) = settingsRepository.setConcurrency(value)
    fun setWifiOnly(value: Boolean) = settingsRepository.setWifiOnly(value)
    fun setChargingOnly(value: Boolean) = settingsRepository.setChargingOnly(value)
    fun setHaptics(value: Boolean) = settingsRepository.setHaptics(value)
    fun setTemplate(value: String) = settingsRepository.setFilenameTemplate(value)
    fun setVerboseLogging(value: Boolean) = settingsRepository.setVerboseLogging(value)

    fun checkForUpdate() {
        viewModelScope.launch { engineManager.maybeCheckForUpdate(force = true) }
    }

    fun reinstallEngine() {
        engineManager.reinstallEngineAndRestart()
    }

    /**
     * Cookies import (S12): copies the picked netscape-format cookies.txt into the
     * engine's files dir and records the import time. YtDlpEngine passes --cookies
     * on analyze/download once the file exists.
     */
    fun importCookies(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val target = File(appContext.filesDir, "cookies.txt")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open the selected file")
                if (target.length() == 0L) error("The selected file is empty")
                target
            }
            result.fold(
                onSuccess = {
                    settingsRepository.setCookiesImportedAt(System.currentTimeMillis())
                    _toast.value = "Cookies imported"
                },
                onFailure = {
                    Timber.e(it, "cookies import failed")
                    _toast.value = "Import failed — is it a cookies.txt file?"
                },
            )
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

}
