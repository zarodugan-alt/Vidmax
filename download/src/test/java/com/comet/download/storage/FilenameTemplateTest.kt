package com.comet.download.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameTemplateTest {

    @Test
    fun `renders spec example`() {
        val name = FilenameTemplate.render(
            template = "{title} - {channel} [{quality}]",
            title = "My Video",
            channel = "Channel",
            quality = "1080p",
            id = "abc123",
            site = "youtube",
        )
        assertEquals("My Video - Channel [1080p]", name)
    }

    @Test
    fun `sanitizes filesystem hostile characters`() {
        val name = FilenameTemplate.render(
            template = "{title}",
            title = "What? Is: This/That \\ Stuff *",
            channel = null,
            quality = null,
            id = null,
            site = null,
        )
        assertTrue(!name.contains("/") && !name.contains(":") && !name.contains("?"))
    }

    @Test
    fun `falls back when title blank`() {
        val name = FilenameTemplate.render(
            template = "{title} [{quality}]",
            title = "  ",
            channel = null,
            quality = null,
            id = null,
            site = null,
        )
        assertEquals("Untitled", name)
    }

    @Test
    fun `unknown channel becomes placeholder`() {
        val name = FilenameTemplate.render(
            template = "{channel}",
            title = "t",
            channel = null,
            quality = null,
            id = null,
            site = null,
        )
        assertEquals("Unknown", name)
    }

    @Test
    fun `date token uses iso format`() {
        val name = FilenameTemplate.render(
            template = "{date}",
            title = "t",
            channel = "c",
            quality = "720p",
            id = "i",
            site = "s",
            timestamp = 1_710_000_000_000L, // 2024-03-09 UTC (locale-tz dependent, assert shape)
        )
        assertTrue(Regex("""\d{4}-\d{2}-\d{2}""").matches(name))
    }

    @Test
    fun `collapses excess whitespace and trims dots`() {
        val name = FilenameTemplate.render(
            template = "{title}...",
            title = "hello   world..",
            channel = null,
            quality = null,
            id = null,
            site = null,
        )
        assertEquals("hello world", name)
    }
}
