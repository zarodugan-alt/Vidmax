package com.comet.download.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Filename template renderer (S11). Tokens: {title} {channel} {quality} {date} {id} {site}.
 * Example: "{title} - {channel} [{quality}]" -> "My Video - Channel [1080p].mp4"
 */
object FilenameTemplate {

    private val illegal = Regex("""[\\/:*?"<>|\u0000]""")
    private val collapses = Regex("""\s{2,}""")

    fun render(
        template: String,
        title: String,
        channel: String?,
        quality: String?,
        id: String?,
        site: String?,
        timestamp: Long = System.currentTimeMillis(),
    ): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
        val channelValue = channel?.takeIf { it.isNotBlank() } ?: "Unknown"
        val name = template
            .replace("{title}", sanitize(title.ifBlank { "Untitled" }))
            .replace("{channel}", sanitize(channelValue))
            .replace("{quality}", sanitize(quality ?: ""))
            .replace("{date}", date)
            .replace("{id}", sanitize(id ?: ""))
            .replace("{site}", sanitize(site ?: ""))
        val cleaned = sanitize(name)
            .trim()
            .trimEnd('.', ' ')
            .replace(collapses, " ")
        return cleaned.ifBlank { "Untitled" }
    }

    private fun sanitize(value: String): String = illegal.replace(value, " ")
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace('\t', ' ')
}
