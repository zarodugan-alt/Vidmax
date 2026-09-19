package com.comet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressParserTest {

    @Test
    fun `parses typical yt-dlp progress line`() {
        val line = "[download]  62.3% of  136.00MiB at    4.20MiB/s ETA 00:21"
        val parsed = ProgressParser.parse(line) as ParsedLine.Progress

        assertEquals(62.3f, parsed.percent, 0.01f)
        assertEquals(136.00 * 1024 * 1024, parsed.totalBytes!!.toDouble(), 1.0)
        assertEquals(136.00 * 1024 * 1024 * 0.623, parsed.downloadedBytes!!.toDouble(), 2000.0)
        assertEquals(4.20 * 1024 * 1024, parsed.bytesPerSec!!.toDouble(), 1.0)
        assertEquals(21, parsed.etaSec)
    }

    @Test
    fun `parses completed line without speed or eta`() {
        val line = "[download] 100.0% of   45.20MiB in 00:32"
        val parsed = ProgressParser.parse(line) as ParsedLine.Progress

        assertEquals(100f, parsed.percent, 0.01f)
        assertEquals(45.20 * 1024 * 1024, parsed.totalBytes!!.toDouble(), 1.0)
        assertNull(parsed.bytesPerSec)
        assertNull(parsed.etaSec)
    }

    @Test
    fun `parses approximate total with tilde and unknown speed`() {
        val line = "[download]   4.2% of ~ 12.34MiB at Unknown B/s ETA Unknown"
        val parsed = ProgressParser.parse(line) as ParsedLine.Progress

        assertEquals(4.2f, parsed.percent, 0.01f)
        assertEquals(12.34 * 1024 * 1024, parsed.totalBytes!!.toDouble(), 1.0)
        assertNull(parsed.bytesPerSec)
        assertNull(parsed.etaSec)
    }

    @Test
    fun `parses hours eta`() {
        val line = "[download]  10.0% of    1.50GiB at    2.00MiB/s ETA 01:02:03"
        val parsed = ProgressParser.parse(line) as ParsedLine.Progress

        assertEquals(3723, parsed.etaSec)
        assertEquals(1.5 * 1024L * 1024 * 1024, parsed.totalBytes!!.toDouble(), 2.0)
    }

    @Test
    fun `parses destination line`() {
        val parsed = ProgressParser.parse(
            "[download] Destination: /data/user/0/com.comet/files/work/abc/My Video.f137.mp4"
        )
        assertTrue(parsed is ParsedLine.Destination)
        assertEquals(
            "/data/user/0/com.comet/files/work/abc/My Video.f137.mp4",
            (parsed as ParsedLine.Destination).path,
        )
    }

    @Test
    fun `parses already downloaded line as destination`() {
        val parsed = ProgressParser.parse(
            "[download] /data/user/0/com.comet/files/work/abc/My Video.mp4 has already been downloaded"
        )
        assertTrue(parsed is ParsedLine.Destination)
    }

    @Test
    fun `parses merger line with quotes`() {
        val parsed = ProgressParser.parse(
            "[Merger] Merging formats into \"/data/user/0/com.comet/files/work/abc/My Video.mp4\""
        )
        val merge = parsed as ParsedLine.Merge
        assertEquals("/data/user/0/com.comet/files/work/abc/My Video.mp4", merge.path)
    }

    @Test
    fun `parses extract audio line`() {
        val parsed = ProgressParser.parse(
            "[ExtractAudio] Destination: /data/user/0/com.comet/files/work/abc/My Video.mp3"
        )
        assertTrue(parsed is ParsedLine.ExtractAudio)
    }

    @Test
    fun `parses metadata and thumbnail lines`() {
        assertTrue(
            ProgressParser.parse("[Metadata] Adding metadata to \"/data/x.mp4\"") is ParsedLine.Metadata
        )
        assertTrue(
            ProgressParser.parse("[EmbedThumbnail] ffmpeg: Embedding thumbnail") is ParsedLine.EmbedThumbnail
        )
    }

    @Test
    fun `parses aria2c summary line`() {
        val line = "[#8c9d1a 10MiB/20MiB(50%) CN:16 DL:4.2MiB ETA:1m30s]"
        val parsed = ProgressParser.parse(line) as ParsedLine.Progress

        assertEquals(50f, parsed.percent, 0.01f)
        assertEquals(10L * 1024 * 1024, parsed.downloadedBytes)
        assertEquals(20L * 1024 * 1024, parsed.totalBytes)
        assertEquals(4.2 * 1024 * 1024, parsed.bytesPerSec!!.toDouble(), 1.0)
        assertEquals(90, parsed.etaSec)
    }

    @Test
    fun `ignores noise lines`() {
        assertNull(ProgressParser.parse("[youtube] Extracting URL: https://youtu.be/x"))
        assertNull(ProgressParser.parse("[info] xyz: Downloading 1 format(s): 137+140"))
        assertNull(ProgressParser.parse(""))
        assertNull(ProgressParser.parse("WARNING: some notice"))
    }
}
