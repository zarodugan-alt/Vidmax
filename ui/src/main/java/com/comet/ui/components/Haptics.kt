package com.comet.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Haptics map (Part 2.6). Master toggle lives in Settings -> Appearance (default ON).
 *
 * | Event                        | Haptic                                     |
 * |------------------------------|--------------------------------------------|
 * | Button press                 | CONTEXT_CLICK                              |
 * | Format selected              | KEYBOARD_TAP                               |
 * | Download started             | VibrationEffect.createOneShot(20ms, 100)   |
 * | Download complete            | two 20ms pulses                            |
 * | Download failed              | createOneShot(120ms, 200)                  |
 * | Swipe-to-delete threshold    | LONG_PRESS                                 |
 */
class Haptics(private val context: Context, private val enabled: Boolean) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        if (!enabled) return
        runCatching { vibrator?.vibrate(effect) }
    }

    fun buttonPress() = simpleTap()

    fun formatSelected() = simpleTap()

    private fun simpleTap() {
        if (!enabled) return
        vibrate(VibrationEffect.createOneShot(15, 90))
    }

    fun downloadStarted() = vibrate(VibrationEffect.createOneShot(20, 100))

    fun downloadComplete() = vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 20, 90, 20),
            intArrayOf(0, 100, 0, 100),
            -1,
        ),
    )

    fun downloadFailed() = vibrate(VibrationEffect.createOneShot(120, 200))

    fun swipeThreshold() = vibrate(VibrationEffect.createOneShot(25, 140))
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val context = LocalContext.current
    return remember(context, enabled) { Haptics(context, enabled) }
}
