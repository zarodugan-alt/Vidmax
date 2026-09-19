package com.comet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FailureMapperTest {

    @Test
    fun `age gate maps to cookies action`() {
        val failure = FailureMapper.map(
            "ERROR: [youtube] xyz: Sign in to confirm your age\n. This video may be inappropriate for some users."
        )
        assertEquals(FailureKind.AGE_GATE, failure.kind)
        assertEquals("This video needs sign-in", failure.userMessage)
        assertEquals(FailureAction.IMPORT_COOKIES, failure.action)
    }

    @Test
    fun `rate limit maps to retry`() {
        val failure = FailureMapper.map("ERROR: unable to download video data: HTTP Error 429: Too Many Requests")
        assertEquals(FailureKind.RATE_LIMITED, failure.kind)
        assertEquals(FailureAction.RETRY, failure.action)
    }

    @Test
    fun `unsupported url maps to copy error`() {
        val failure = FailureMapper.map("ERROR: Unsupported URL: https://example.com/")
        assertEquals(FailureKind.UNSUPPORTED, failure.kind)
        assertEquals(FailureAction.COPY_ERROR, failure.action)
    }

    @Test
    fun `extractor breakage maps to update engine`() {
        val failure = FailureMapper.map("ERROR: unable to extract initial data player response")
        assertEquals(FailureKind.EXTRACTOR_BROKEN, failure.kind)
        assertEquals(FailureAction.UPDATE_ENGINE, failure.action)
    }

    @Test
    fun `geo block has no action`() {
        val failure = FailureMapper.map("ERROR: This video is not available in your country")
        assertEquals(FailureKind.GEO_BLOCKED, failure.kind)
        assertEquals(FailureAction.NONE, failure.action)
    }

    @Test
    fun `disk full maps to storage settings`() {
        val failure = FailureMapper.map("OSError: [Errno 28] No space left on device")
        assertEquals(FailureKind.NO_SPACE, failure.kind)
        assertEquals(FailureAction.OPEN_STORAGE_SETTINGS, failure.action)
    }

    @Test
    fun `network failures map to retry`() {
        val failure = FailureMapper.map("ERROR: unable to download webpage: <urlopen error [Errno -2] Name or service not known>")
        assertEquals(FailureKind.NETWORK, failure.kind)
        assertEquals("Connection failed", failure.userMessage)
        assertEquals(FailureAction.RETRY, failure.action)
    }

    @Test
    fun `unknown failures surface raw first line with copy action`() {
        val failure = FailureMapper.map("ERROR: something exotic happened\nsecond line")
        assertEquals(FailureKind.UNKNOWN, failure.kind)
        assertTrue(failure.userMessage.contains("something exotic"))
        assertEquals(FailureAction.COPY_ERROR, failure.action)
    }
}
