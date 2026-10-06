package com.synsound.sdk.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SynSoundEnvironmentTest {

    @Test
    fun customEnvironmentDefaultsToSecureWebSocket() {
        val environment = SynSoundEnvironment.Custom("https://example.test")
        assertEquals("https://example.test", environment.baseUrl)
        assertEquals(
            "wss://example.test/api/v1/stream",
            environment.wsUrl
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun customEnvironmentRejectsHttp() {
        SynSoundEnvironment.Custom("http://example.test")
    }

    @Test(expected = IllegalArgumentException::class)
    fun customEnvironmentRejectsWs() {
        SynSoundEnvironment.Custom(
            customBaseUrl = "https://example.test",
            customWsUrl = "ws://example.test/api/v1/stream"
        )
    }
}
