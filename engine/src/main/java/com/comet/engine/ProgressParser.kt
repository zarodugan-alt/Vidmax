package com.comet.engine

/**
 * Pure parser for yt-dlp (`--newline`) and aria2c summary stdout lines (Part 7.2 / 7.3).
 * Kept free of Android dependencies so it is unit-testable on the JVM.
 *
 * Example inputs:
 *   [download]  62.3% of  136.00MiB at    4.20MiB/s ETA 00:21
 *   [download] 100.0% of   45.20MiB in 00:32
 *   [download] Destination: /data/…/Title [137].f137.mp4
 *   [Merger] Merging formats into "/data/…/Title [137].mp4"
 *   [ExtractAudio] Destination: /data/…/Title.webm
 *   [#8c9d1a 10MiB/20MiB(50%) CN:16 DL:4.2MiB ETA:1m30s]
 */
sealed interface ParsedLine {
    data class Progress(
        val percent: Float,
        val downloadedBytes: Long?,
        val totalBytes: Long?,
        val bytesPerSec: Long?,
        val etaSec: Int?,
    ) : ParsedLine

    data class Destination(val path: String) : ParsedLine
    data class Merge(val path: String) : ParsedLine
    data class ExtractAudio(val path: String) : ParsedLine
    data object Metadata : ParsedLine
    data object EmbedThumbnail : ParsedLine
}

object ProgressParser {

    private val ytProgressHead = Regex(
        """^\[download\]\s+([\d.]+)%\s+of\s+~?\s*([\d.]+)(B|KiB|MiB|GiB|TiB)"""
    )
    private val ytSpeed = Regex("""\sat\s+([\d.]+)(B|KiB|MiB|GiB|TiB)/s""")
    private val ytEta = Regex("""\sETA\s+(\d+):(\d{2})(?::(\d{2}))?$""")
    private val ytDestination = Regex("""^\[download\]\s+Destination:\s+(.+)$""")
    private val ytAlreadyDownloaded = Regex("""^\[download\]\s+(.+?) has already been downloaded""")
    private val merger = Regex("""^\[Merger\]\s+Merging formats into\s+"?(.+?)"?$""")
    private val extractAudio = Regex("""^\[ExtractAudio\]\s+Destination:\s+(.+)$""")
    private val metadata = Regex("""^\[Metadata\]""")
    private val embedThumbnail = Regex("""^\[EmbedThumbnail\]""")

    private val aria2Progress = Regex(
        """^\[#[0-9a-fA-F]+\s+([\d.]+)(B|KiB|MiB|GiB|TiB)/([\d.]+)(B|KiB|MiB|GiB|TiB)\((\d+)%\]?\)?"""
    )
    private val aria2Dl = Regex("""DL:([\d.]+)(B|KiB|MiB|GiB|TiB)""")
    private val aria2Eta = Regex("""ETA:(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?(?=\]|$)""")

    fun parse(rawLine: String): ParsedLine? {
        val line = rawLine.trimEnd('\r', '\n')
        if (line.isBlank()) return null

        // --- yt-dlp lines ---
        ytProgressHead.find(line)?.let { m ->
            val percent = m.groupValues[1].toFloatOrNull() ?: return null
            val totalBytes = toBytes(m.groupValues[2].toDoubleOrNull(), m.groupValues[3])
            val downloadedBytes = totalBytes?.let { (it * percent / 100f).toLong() }
            val bytesPerSec = ytSpeed.find(line)?.let { s ->
                toBytes(s.groupValues[1].toDoubleOrNull(), s.groupValues[2])
            }
            val etaSec = ytEta.find(line)?.let { e ->
                val mm = e.groupValues[1].toIntOrNull() ?: 0
                val ss = e.groupValues[2].toIntOrNull() ?: 0
                val hh = e.groupValues[3].toIntOrNull() ?: 0
                hh * 3600 + mm * 60 + ss
            }
            return ParsedLine.Progress(percent, downloadedBytes, totalBytes, bytesPerSec, etaSec)
        }

        ytDestination.find(line)?.let { return ParsedLine.Destination(it.groupValues[1].trim()) }
        ytAlreadyDownloaded.find(line)?.let { return ParsedLine.Destination(it.groupValues[1].trim()) }
        merger.find(line)?.let { return ParsedLine.Merge(it.groupValues[1].trim()) }
        extractAudio.find(line)?.let { return ParsedLine.ExtractAudio(it.groupValues[1].trim()) }
        if (metadata.containsMatchIn(line)) return ParsedLine.Metadata
        if (embedThumbnail.containsMatchIn(line)) return ParsedLine.EmbedThumbnail

        // --- aria2c summary lines ---
        aria2Progress.find(line)?.let { m ->
            val done = toBytes(m.groupValues[1].toDoubleOrNull(), m.groupValues[2])
            val total = toBytes(m.groupValues[3].toDoubleOrNull(), m.groupValues[4])
            val percent = m.groupValues[5].toFloatOrNull() ?: return null
            val speed = aria2Dl.find(line)?.let { s ->
                toBytes(s.groupValues[1].toDoubleOrNull(), s.groupValues[2])
            }
            val eta = aria2Eta.find(line)?.let { e ->
                val h = e.groupValues[1].toIntOrNull() ?: 0
                val mm = e.groupValues[2].toIntOrNull() ?: 0
                val s = e.groupValues[3].toIntOrNull() ?: 0
                if (h == 0 && mm == 0 && s == 0) null else h * 3600 + mm * 60 + s
            }
            return ParsedLine.Progress(percent, done, total, speed, eta)
        }

        return null
    }

    /** "of N/A" or other oddities produce no total — progress still reports percent. */
    fun parsePercentOnly(line: String): Float? {
        val m = Regex("""^\[download\]\s+([\d.]+)%""").find(line) ?: return null
        return m.groupValues[1].toFloatOrNull()
    }

    private fun toBytes(value: Double?, unit: String): Long? {
        if (value == null) return null
        val factor = when (unit) {
            "B" -> 1.0
            "KiB" -> 1024.0
            "MiB" -> 1024.0 * 1024
            "GiB" -> 1024.0 * 1024 * 1024
            "TiB" -> 1024.0 * 1024 * 1024 * 1024
            else -> return null
        }
        return (value * factor).toLong()
    }
}
