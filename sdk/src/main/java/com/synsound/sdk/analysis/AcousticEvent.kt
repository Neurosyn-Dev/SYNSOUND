package com.synsound.sdk.analysis

/**
 * An acoustic event detected in real-time by the SynSound event classifier.
 */
data class AcousticEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: AcousticEventType,
    val confidence: Float,               // [0.0 - 1.0]
    val timestampMs: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val peakDbfs: Float = 0.0f,
    val spectralProfile: SpectralProfile? = null,
    val metadata: Map<String, String> = emptyMap()
)
