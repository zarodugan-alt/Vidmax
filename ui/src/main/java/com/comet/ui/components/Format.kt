package com.comet.ui.components

import java.util.Locale

/** Telemetry formatting — one source of truth so cards, sheets and notifications agree. */
object Format {

    fun bytes(value: Long): String = when {
        value < 1024 -> "$value B"
        value < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", value / 1024.0)
        value < 1024L * 1024 * 1024 ->
            String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024))

        else -> String.format(Locale.US, "%.1f GB", value / (1024.0 * 1024 * 1024))
    }

    fun speed(bytesPerSec: Long): String = bytes(bytesPerSec) + "/s"

    fun eta(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun duration(seconds: Long): String {
        if (seconds <= 0) return "--:--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun percent(fraction: Float): String = "${(fraction.coerceIn(0f, 1f) * 100).toInt()}%"
}
