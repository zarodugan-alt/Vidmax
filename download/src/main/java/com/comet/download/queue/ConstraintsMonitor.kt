package com.comet.download.queue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import com.comet.data.datastore.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Constraint enforcement (Part 8.1): Wi-Fi-only pauses/resumes via ConnectivityManager
 * callbacks; charging-only checks BatteryManager status. The queue observes [allowed].
 *
 * Wi-Fi-only ON + mobile data => [allowed] = false within seconds (AC #10) and the queue
 * holds; reconnecting to Wi-Fi flips it back and downloads resume.
 */
@Singleton
class ConstraintsMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _allowed = MutableStateFlow(true)
    val allowed: StateFlow<Boolean> = _allowed.asStateFlow()

    private val _waitingForWifi = MutableStateFlow(false)
    val waitingForWifi: StateFlow<Boolean> = _waitingForWifi.asStateFlow()

    init {
        registerNetworkCallback()
        registerChargingReceiver()
        // Re-evaluate whenever a constraint setting flips.
        scope.launch { settingsRepository.settings.collect { recompute() } }
        recompute()
    }

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = recompute()
            override fun onLost(network: Network) = recompute()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                recompute()
        })
    }

    private fun registerChargingReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) = recompute()
            },
            filter,
        )
    }

    private fun recompute() {
        val settings = settingsRepository.settings.value
        val networkOk = currentNetworkOk(settings.wifiOnly)
        val chargingOk = currentChargingOk(settings.chargingOnly)
        _allowed.value = networkOk && chargingOk
        _waitingForWifi.value = settings.wifiOnly && !networkOk
    }

    private fun currentNetworkOk(wifiOnly: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork
            ?: // No network at all: only acceptable when the constraint is off (queue would
              // fail on network errors anyway, but keep states distinct from "no wifi").
            return !wifiOnly
        val caps = cm.getNetworkCapabilities(network) ?: return !wifiOnly
        return if (wifiOnly) {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !cm.isActiveNetworkMetered
        } else {
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    private fun currentChargingOk(chargingOnly: Boolean): Boolean {
        if (!chargingOnly) return true
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return true
        return bm.isCharging ||
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ==
            BatteryManager.BATTERY_STATUS_FULL
    }
}
