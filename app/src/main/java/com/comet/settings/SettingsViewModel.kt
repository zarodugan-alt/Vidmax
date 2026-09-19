package com.comet.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comet.BuildConfig
import com.comet.data.datastore.SettingsRepository
import com.comet.data.model.AudioFormatPreference
import com.comet.data.model.EngineChannel
import com.comet.data.model.VideoQualityPreference
import com.comet.engine.EngineManager
import com.comet.ui.screens.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val engineManager: EngineManager,
) : ViewModel() {

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

    fun checkForUpdate() {
        viewModelScope.launch { engineManager.maybeCheckForUpdate(force = true) }
    }
}
