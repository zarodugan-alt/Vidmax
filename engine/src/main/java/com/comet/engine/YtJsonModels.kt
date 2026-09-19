package com.comet.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * kotlinx.serialization DTOs for `yt-dlp --dump-json` output (Part 7.2).
 * Lenient: yt-dlp emits a large, version-dependent schema — unknown keys are ignored.
 */
object YtJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Serializable
    data class Root(
        val id: String? = null,
        val title: String? = null,
        val channel: String? = null,
        val uploader: String? = null,
        val duration: Double? = null,
        val thumbnail: String? = null,
        @SerialName("extractor_key") val extractorKey: String? = null,
        @SerialName("webpage_url") val webpageUrl: String? = null,
        @SerialName("_type") val type: String? = null,
        @SerialName("is_live") val isLive: Boolean? = null,
        @SerialName("live_status") val liveStatus: String? = null,
        val formats: List<Format> = emptyList(),
        val entries: List<Entry> = emptyList(),
    )

    @Serializable
    data class Format(
        @SerialName("format_id") val formatId: String? = null,
        val ext: String? = null,
        val height: Int? = null,
        val fps: Double? = null,
        val vcodec: String? = null,
        val acodec: String? = null,
        val filesize: Long? = null,
        @SerialName("filesize_approx") val filesizeApprox: Long? = null,
        @SerialName("format_note") val formatNote: String? = null,
        val protocol: String? = null,
        val abr: Double? = null,
        val tbr: Double? = null,
    )

    @Serializable
    data class Entry(
        val id: String? = null,
        val title: String? = null,
        val duration: Double? = null,
        val url: String? = null,
    )

    @Serializable
    data class GithubRelease(
        @SerialName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val body: String? = null,
    )
}
