package com.comet.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.comet.data.datastore.SettingsRepository
import com.comet.data.model.EngineChannel
import com.comet.data.repo.EngineStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Engine lifecycle facade (Part 7 / S0 / S9):
 *  - warmup at app start (non-blocking, continues while splash/home show)
 *  - 24h-cached update checks against the yt-dlp release channel
 *  - atomic engine updates that never block in-flight downloads
 */
@Singleton
class EngineManager @Inject constructor(
    private val engine: VideoEngine,
    private val engineStateRepository: EngineStateRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _status = MutableStateFlow(EngineStatus.COLD)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _version = MutableStateFlow<String?>(null)
    val version: StateFlow<String?> = _version.asStateFlow()

    private val _updateAvailable = MutableStateFlow<EngineUpdate?>(null)
    val updateAvailable: StateFlow<EngineUpdate?> = _updateAvailable.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    /** Called from Application.onCreate — engine warmup is the slow part (~1s). */
    fun warmup() {
        if (_status.value != EngineStatus.COLD) return
        _status.value = EngineStatus.WARMING
        scope.launch { doInit() }
    }

    private suspend fun doInit() {
        try {
            engine.init()
            _status.value = EngineStatus.READY
            refreshVersion()
            maybeCheckForUpdate()
        } catch (e: Exception) {
            Timber.e(e, "engine failed to load")
            _status.value = EngineStatus.FAILED
        }
    }

    private suspend fun refreshVersion() {
        val v = engine.version()
        _version.value = v
        val state = engineStateRepository.get()
        if (state == null) {
            engineStateRepository.recordCheck(
                settingsRepository.settings.value.engineChannel,
                v ?: "bundled",
            )
        }
    }

    /**
     * Update check with a 24h cache (Part 7.4). Auto-applies on Wi-Fi when the user
     * opted in (default ON). Manual checks pass [force].
     */
    suspend fun maybeCheckForUpdate(force: Boolean = false) {
        if (_updating.value) return
        val settings = settingsRepository.settings.value
        val state = engineStateRepository.get()
        val lastCheck = state?.lastCheckAt ?: 0L
        val stale = System.currentTimeMillis() - lastCheck > CHECK_INTERVAL_MS

        if (!force) {
            if (!stale) return
            // Auto checks only happen on unmetered networks.
            if (!isOnUnmeteredWifi()) return
        }

        val update = engine.checkUpdate(settings.engineChannel)
        engineStateRepository.recordCheck(
            settings.engineChannel,
            _version.value ?: state?.version ?: "bundled",
        )
        _updateAvailable.value = update

        if (update != null && settings.engineAutoUpdate && isOnUnmeteredWifi()) {
            applyUpdate(settings.engineChannel)
        }
    }

    /** Downloads and atomically swaps the yt-dlp bundle (never blocks in-flight downloads). */
    suspend fun applyUpdate(channel: EngineChannel): Boolean {
        if (_updating.value) return false
        _updating.value = true
        try {
            val ok = engine.applyUpdate(channel)
            if (ok) {
                _version.value = engine.version()
                _updateAvailable.value = null
                engineStateRepository.recordUpdate(channel, _version.value ?: "unknown")
                Timber.i("engine updated to %s", _version.value)
            }
            return ok
        } finally {
            _updating.value = false
        }
    }

    /**
     * "Reinstall engine" recovery (Part 11: Engine broken/missing).
     * Wipes the extracted runtime so the next launch re-extracts from APK assets.
     * The current process must die for the swap to be safe — callers should finish afterwards.
     */
    fun reinstallEngineAndRestart() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                File(context.noBackupFilesDir, "youtubedl-android").deleteRecursively()
            }.onFailure { Timber.e(it, "engine wipe failed") }
            Timber.i("engine runtime wiped — process will exit for re-extraction")
            Runtime.getRuntime().exit(0)
        }
    }

    fun isOnUnmeteredWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !cm.isActiveNetworkMetered
    }

    companion object {
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
