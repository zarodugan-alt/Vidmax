package com.comet.data.model

import androidx.room.TypeConverter

/**
 * Queue state machine (Part 8.1):
 * QUEUED -> ANALYZING -> DOWNLOADING -> PROCESSING -> DONE | FAILED | PAUSED | CANCELLED
 */
enum class DownloadState {
    QUEUED,
    ANALYZING,
    DOWNLOADING,
    PROCESSING,
    DONE,
    FAILED,
    PAUSED,
    CANCELLED;

    val isActive: Boolean
        get() = this == DOWNLOADING || this == PROCESSING || this == ANALYZING

    val isPending: Boolean
        get() = this == QUEUED || this == PAUSED

    val isTerminal: Boolean
        get() = this == DONE || this == FAILED || this == CANCELLED
}

/** Engine update channel (Part 7.4). */
enum class EngineChannel {
    STABLE,
    NIGHTLY;

    companion object {
        fun fromName(name: String?): EngineChannel =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: STABLE
    }
}

/** Default video quality preference (Part 10 "S10"). "ask" = show the analyze sheet. */
enum class VideoQualityPreference(val label: String, val maxHeight: Int?) {
    ASK("Ask every time", null),
    P2160("2160p", 2160),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480);

    companion object {
        fun fromName(name: String?): VideoQualityPreference =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: P1080
    }
}

/** Default audio extraction format (Part 12 / S10). */
enum class AudioFormatPreference(val label: String) {
    MP3_320("MP3 320k"),
    M4A_128("M4A"),
    OPUS_160("Opus");

    companion object {
        fun fromName(name: String?): AudioFormatPreference =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MP3_320
    }
}

class EnumConverters {
    @TypeConverter
    fun downloadStateToString(state: DownloadState): String = state.name

    @TypeConverter
    fun stringToDownloadState(value: String): DownloadState =
        DownloadState.entries.firstOrNull { it.name == value } ?: DownloadState.QUEUED

    @TypeConverter
    fun engineChannelToString(channel: EngineChannel): String = channel.name

    @TypeConverter
    fun stringToEngineChannel(value: String): EngineChannel = EngineChannel.fromName(value)
}
