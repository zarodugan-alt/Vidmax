package com.comet.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.comet.data.model.AudioFormatPreference
import com.comet.data.model.EngineChannel
import com.comet.data.model.VideoQualityPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cometDataStore by preferencesDataStore(name = "comet_settings")

/** DataStore keys per Part 9.2. */
object SettingsKeys {
    val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
    val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
    val CONCURRENCY = intPreferencesKey("concurrency")
    val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    val CHARGING_ONLY = booleanPreferencesKey("charging_only")
    val METERED_WARNING = booleanPreferencesKey("metered_warning")
    val AUTO_DOWNLOAD_ON_SHARE = booleanPreferencesKey("auto_download_on_share")
    val ENGINE_CHANNEL = stringPreferencesKey("engine_channel")
    val ENGINE_AUTO_UPDATE = booleanPreferencesKey("engine_auto_update")
    val FILENAME_TEMPLATE = stringPreferencesKey("filename_template")
    val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
    val EMBED_METADATA = booleanPreferencesKey("embed_metadata")
    val EMBED_THUMBNAILS = booleanPreferencesKey("embed_thumbnails")
    val SPONSORBLOCK = booleanPreferencesKey("sponsorblock")
    val SCHEDULE_TIME = stringPreferencesKey("schedule_time")
    val VERBOSE_LOGGING = booleanPreferencesKey("verbose_logging")
    val HAPTICS = booleanPreferencesKey("haptics")
    val COOKIES_IMPORTED_AT = longPreferencesKey("cookies_imported_at")
}

/**
 * All user settings in one snapshot. Defaults follow the spec
 * (S10/S11: 1080p, MP3, concurrency 2, Wi-Fi only ON, haptics ON, etc.).
 */
data class AppSettings(
    val disclaimerAccepted: Boolean = false,
    val defaultVideoQuality: VideoQualityPreference = VideoQualityPreference.P1080,
    val defaultAudioFormat: AudioFormatPreference = AudioFormatPreference.MP3_320,
    val concurrency: Int = 2,
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val meteredWarning: Boolean = false,
    val autoDownloadOnShare: Boolean = false,
    val engineChannel: EngineChannel = EngineChannel.STABLE,
    val engineAutoUpdate: Boolean = true,
    val filenameTemplate: String = DEFAULT_FILENAME_TEMPLATE,
    val confirmDelete: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedThumbnails: Boolean = true,
    val sponsorblock: Boolean = false,
    val scheduleTime: String? = null,
    val verboseLogging: Boolean = false,
    val haptics: Boolean = true,
    val cookiesImportedAt: Long? = null,
) {
    companion object {
        const val DEFAULT_FILENAME_TEMPLATE = "{title} - {channel} [{quality}]"
    }
}

/**
 * Settings repository backed by Preferences DataStore.
 *
 * Reads are exposed as a hot [StateFlow] (initial value [AppSettings] defaults) so UI code
 * never has to suspend; writes are fire-and-forget on an internal scope.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        scope.launch { cometDataStore().data.collect { prefs -> _settings.value = read(prefs) } }
    }

    private fun cometDataStore() = appContext.cometDataStore

    private fun read(prefs: androidx.datastore.preferences.core.Preferences): AppSettings =
        AppSettings(
            disclaimerAccepted = prefs[SettingsKeys.DISCLAIMER_ACCEPTED] ?: false,
            defaultVideoQuality = VideoQualityPreference.fromName(
                prefs[SettingsKeys.DEFAULT_VIDEO_QUALITY]
            ),
            defaultAudioFormat = AudioFormatPreference.fromName(
                prefs[SettingsKeys.DEFAULT_AUDIO_FORMAT]
            ),
            concurrency = (prefs[SettingsKeys.CONCURRENCY] ?: 2).coerceIn(1, 3),
            wifiOnly = prefs[SettingsKeys.WIFI_ONLY] ?: true,
            chargingOnly = prefs[SettingsKeys.CHARGING_ONLY] ?: false,
            meteredWarning = prefs[SettingsKeys.METERED_WARNING] ?: false,
            autoDownloadOnShare = prefs[SettingsKeys.AUTO_DOWNLOAD_ON_SHARE] ?: false,
            engineChannel = EngineChannel.fromName(prefs[SettingsKeys.ENGINE_CHANNEL]),
            engineAutoUpdate = prefs[SettingsKeys.ENGINE_AUTO_UPDATE] ?: true,
            filenameTemplate = prefs[SettingsKeys.FILENAME_TEMPLATE]
                ?: AppSettings.DEFAULT_FILENAME_TEMPLATE,
            confirmDelete = prefs[SettingsKeys.CONFIRM_DELETE] ?: true,
            embedMetadata = prefs[SettingsKeys.EMBED_METADATA] ?: true,
            embedThumbnails = prefs[SettingsKeys.EMBED_THUMBNAILS] ?: true,
            sponsorblock = prefs[SettingsKeys.SPONSORBLOCK] ?: false,
            scheduleTime = prefs[SettingsKeys.SCHEDULE_TIME],
            verboseLogging = prefs[SettingsKeys.VERBOSE_LOGGING] ?: false,
            haptics = prefs[SettingsKeys.HAPTICS] ?: true,
            cookiesImportedAt = prefs[SettingsKeys.COOKIES_IMPORTED_AT],
        )

    /** Synchronous first-run check used before showing the disclaimer gate. */
    fun isDisclaimerAcceptedBlocking(): Boolean = runBlocking {
        val prefs = cometDataStore().data.first()
        prefs[SettingsKeys.DISCLAIMER_ACCEPTED] ?: false
    }

    /** Suspend read straight from DataStore — never the in-memory defaults snapshot. */
    suspend fun disclaimerAcceptedOnce(): Boolean =
        cometDataStore().data.first()[SettingsKeys.DISCLAIMER_ACCEPTED] ?: false

    fun setDisclaimerAccepted() = edit { it[SettingsKeys.DISCLAIMER_ACCEPTED] = true }
    fun setDefaultVideoQuality(v: VideoQualityPreference) =
        edit { it[SettingsKeys.DEFAULT_VIDEO_QUALITY] = v.name }
    fun setDefaultAudioFormat(v: AudioFormatPreference) =
        edit { it[SettingsKeys.DEFAULT_AUDIO_FORMAT] = v.name }
    fun setConcurrency(v: Int) = edit { it[SettingsKeys.CONCURRENCY] = v.coerceIn(1, 3) }
    fun setWifiOnly(v: Boolean) = edit { it[SettingsKeys.WIFI_ONLY] = v }
    fun setChargingOnly(v: Boolean) = edit { it[SettingsKeys.CHARGING_ONLY] = v }
    fun setMeteredWarning(v: Boolean) = edit { it[SettingsKeys.METERED_WARNING] = v }
    fun setAutoDownloadOnShare(v: Boolean) = edit { it[SettingsKeys.AUTO_DOWNLOAD_ON_SHARE] = v }
    fun setEngineChannel(v: EngineChannel) = edit { it[SettingsKeys.ENGINE_CHANNEL] = v.name }
    fun setEngineAutoUpdate(v: Boolean) = edit { it[SettingsKeys.ENGINE_AUTO_UPDATE] = v }
    fun setFilenameTemplate(v: String) = edit { it[SettingsKeys.FILENAME_TEMPLATE] = v }
    fun setConfirmDelete(v: Boolean) = edit { it[SettingsKeys.CONFIRM_DELETE] = v }
    fun setEmbedMetadata(v: Boolean) = edit { it[SettingsKeys.EMBED_METADATA] = v }
    fun setEmbedThumbnails(v: Boolean) = edit { it[SettingsKeys.EMBED_THUMBNAILS] = v }
    fun setSponsorblock(v: Boolean) = edit { it[SettingsKeys.SPONSORBLOCK] = v }
    fun setScheduleTime(v: String?) = edit {
        if (v == null) it.remove(SettingsKeys.SCHEDULE_TIME) else it[SettingsKeys.SCHEDULE_TIME] = v
    }
    fun setVerboseLogging(v: Boolean) = edit { it[SettingsKeys.VERBOSE_LOGGING] = v }
    fun setHaptics(v: Boolean) = edit { it[SettingsKeys.HAPTICS] = v }
    fun setCookiesImportedAt(v: Long?) = edit {
        if (v == null) it.remove(SettingsKeys.COOKIES_IMPORTED_AT)
        else it[SettingsKeys.COOKIES_IMPORTED_AT] = v
    }

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { cometDataStore().edit(block) }
    }
}
