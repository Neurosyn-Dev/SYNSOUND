package com.synsound.sdk.cloud

import com.synsound.sdk.audio.AudioFrame
import com.synsound.sdk.cloud.models.StreamMetadata
import com.synsound.sdk.cloud.models.TelemetryPayload
import com.synsound.sdk.core.SynSoundConfig

/**
 * Unified Cloud Client for REST requests and live audio streaming.
 */
class SynSoundClient(
    val config: SynSoundConfig,
    val rest: SynSoundRestClient = SynSoundRestClient(config),
    val stream: SynSoundWebSocketClient = SynSoundWebSocketClient(config)
) {
    private var activeSessionId: String? = null

    fun startLiveSession(sessionId: String = java.util.UUID.randomUUID().toString()) {
        activeSessionId = sessionId
        val metadata = StreamMetadata(
            sessionId = sessionId,
            deviceId = config.deviceIdentifier ?: "android_device",
            sampleRate = config.sampleRate,
            channels = 1
        )
        stream.connect(metadata)
    }

    fun streamFrame(frame: AudioFrame) {
        stream.sendAudioFrame(frame.pcmData)
    }

    fun streamTelemetry(telemetry: TelemetryPayload) {
        stream.sendTelemetry(telemetry)
    }

    fun stopLiveSession() {
        stream.disconnect()
        activeSessionId = null
    }
}
