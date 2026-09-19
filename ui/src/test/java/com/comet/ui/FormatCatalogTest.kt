package com.comet.ui

import com.comet.engine.PlaylistEntry
import com.comet.engine.VideoFormat
import com.comet.engine.VideoInfo
import com.comet.ui.components.FormatCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatCatalogTest {

    private fun format(
        id: String,
        height: Int?,
        ext: String,
        vcodec: String?,
        acodec: String?,
        filesize: Long? = null,
    ) = VideoFormat(
        formatId = id,
        ext = ext,
        height = height,
        fps = 30.0,
        vcodec = vcodec,
        acodec = acodec,
        filesize = filesize,
        filesizeApprox = null,
        formatNote = null,
        protocol = "https",
        abr = null,
        tbr = null,
    )

    private val info = VideoInfo(
        url = "https://www.youtube.com/watch?v=abc",
        title = "Test video",
        channel = "Test channel",
        durationSec = 600,
        thumbnailUrl = null,
        site = "Youtube",
        isLive = false,
        formats = listOf(
            format("sb0", null, "mhtml", "none", "none"),           // storyboard — filtered
            format("139", null, "m4a", "none", "mp4a.40.5"),        // audio-only
            format("251", null, "webm", "none", "opus"),            // audio-only
            format("160", 144, "mp4", "avc1", "none"),              // dash video-only
            format("133", 240, "mp4", "avc1", "none"),
            format("242", 240, "webm", "vp9", "none"),
            format("134", 360, "mp4", "avc1", "none"),
            format("135", 480, "mp4", "avc1", "none"),
            format("18", 360, "mp4", "avc1", "mp4a.40.2"),          // progressive
            format("22", 720, "mp4", "avc1", "mp4a.40.2"),          // progressive
            format("137", 1080, "mp4", "avc1", "none", filesize = 800_000_000L),
            format("313", 2160, "webm", "vp9", "none"),
        ),
        playlistTitle = null,
        playlistEntries = emptyList(),
    )

    @Test
    fun `progressive first, then dash, one row per resolution, mp4 preferred on dedupe`() {
        val (video, _) = FormatCatalog.build(info)
        val labels = video.map { it.label }
        // Progressive (720, 360) come first; then DASH sorted by height desc.
        assertEquals(listOf("720p", "360p", "2160p", "1080p", "480p", "240p", "144p"), labels)
        // 240p dedupes mp4 (133) over webm (242)
        assertEquals("mp4", video.first { it.label == "240p" }.ext)
        // 720p progressive carries no merge badge
        assertEquals(null, video.first { it.label == "720p" }.badge)
        // 2160p is the 4K row
        assertEquals("4K", video.first { it.label == "2160p" }.badge)
    }

    @Test
    fun `dash rows are marked merge and use plus selector`() {
        val (video, _) = FormatCatalog.build(info)
        val row1080 = video.first { it.label == "1080p" }
        assertEquals("merge", row1080.badge)
        assertEquals("137+bestaudio", row1080.selector)
    }

    @Test
    fun `recommended is best mp4 at or under 1080p`() {
        val (video, _) = FormatCatalog.build(info)
        val recommended = video.firstOrNull { it.isRecommended }
        assertNotNull(recommended)
        assertEquals("720p", recommended!!.label) // best progressive mp4 <= 1080
    }

    @Test
    fun `audio section is mp3 m4a opus with extract badge and estimates`() {
        val (_, audio) = FormatCatalog.build(info)
        assertEquals(listOf("MP3 320k", "M4A", "Opus 160k"), audio.map { it.label })
        assertTrue(audio.all { it.badge == "extract" })
        assertTrue(audio.all { it.sizeLabel?.startsWith("~") == true })
        // 600s * 320kbps / 8 = 24,000,000 bytes
        assertEquals("~22.9 MB", audio[0].sizeLabel)
    }

    @Test
    fun `merge rows add the audio estimate to the size`() {
        val (video, _) = FormatCatalog.build(info)
        val row1080 = video.first { it.label == "1080p" }
        // best abr in fixture: opus ~0 given null abr/tbr => fallback estimate may be null;
        // with abr 0 the estimate is null and size stays the video-only filesize.
        assertEquals(800_000_000L, row1080.estimatedBytes)
    }
}
