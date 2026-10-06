package com.synsound.sdk.core

/**
 * Supported environments for SynSound Platform endpoints.
 */
sealed class SynSoundEnvironment(val baseUrl: String, val wsUrl: String) {
    object Beta : SynSoundEnvironment(
        baseUrl = "https://synsound-beta.base44.app",
        wsUrl = "wss://synsound-beta.base44.app/api/v1/stream"
    )

    object Production : SynSoundEnvironment(
        baseUrl = "https://api.synsound.com",
        wsUrl = "wss://api.synsound.com/api/v1/stream"
    )

    data class Custom(
        val customBaseUrl: String,
        val customWsUrl: String = customBaseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/api/v1/stream"
    ) : SynSoundEnvironment(customBaseUrl, customWsUrl) {
        init {
            require(customBaseUrl.startsWith("https://", ignoreCase = true)) {
                "Custom SynSound base URL must use HTTPS"
            }
            require(customWsUrl.startsWith("wss://", ignoreCase = true)) {
                "Custom SynSound WebSocket URL must use WSS"
            }
        }
    }
}
