package com.synsound.sdk.cloud.models

import org.json.JSONObject

/**
 * Metadata accompanying an audio streaming session.
 */
data class StreamMetadata(
    val sessionId: String,
    val deviceId: String,
    val sampleRate: Int,
    val channels: Int,
    val encoding: String = "PCM_16BIT",
    val dspPreset: String = "FORENSIC_SPEECH_ENHANCEMENT",
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("deviceId", deviceId)
            put("sampleRate", sampleRate)
            put("channels", channels)
            put("encoding", encoding)
            put("dspPreset", dspPreset)
            put("timestampMs", timestampMs)
        }.toString()
    }
}

/**
 * Telemetry message containing computed acoustic features sent periodically.
 */
data class TelemetryPayload(
    val sessionId: String,
    val timestampMs: Long,
    val rmsDbfs: Float,
    val peakAmplitude: Float,
    val dominantFrequencyHz: Float,
    val spectralCentroidHz: Float,
    val zeroCrossingRate: Float,
    val eventsDetected: List<String> = emptyList()
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("timestampMs", timestampMs)
            put("rmsDbfs", rmsDbfs.toDouble())
            put("peakAmplitude", peakAmplitude.toDouble())
            put("dominantFrequencyHz", dominantFrequencyHz.toDouble())
            put("spectralCentroidHz", spectralCentroidHz.toDouble())
            put("zeroCrossingRate", zeroCrossingRate.toDouble())
            put("eventsDetected", org.json.JSONArray(eventsDetected))
        }.toString()
    }
}

/**
 * Remote cloud response or acoustic classification feedback.
 */
data class CloudInferenceResult(
    val eventId: String,
    val eventClass: String,
    val confidence: Float,
    val recommendation: String,
    val timestampMs: Long
) {
    companion object {
        fun fromJson(jsonStr: String): CloudInferenceResult? {
            return try {
                val json = JSONObject(jsonStr)
                CloudInferenceResult(
                    eventId = json.optString("eventId", ""),
                    eventClass = json.optString("eventClass", "UNKNOWN"),
                    confidence = json.optDouble("confidence", 0.0).toFloat(),
                    recommendation = json.optString("recommendation", ""),
                    timestampMs = json.optLong("timestampMs", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
