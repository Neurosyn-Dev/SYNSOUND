package com.synsound.sdk.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Encapsulates a single captured / processed audio frame with acoustic telemetry metrics.
 */
data class AudioFrame(
    val pcmData: ByteArray,
    val sampleRate: Int,
    val channelCount: Int = 1,
    val timestampMs: Long = System.currentTimeMillis(),
    val frameIndex: Long = 0
) {
    val sampleCount: Int = pcmData.size / (2 * channelCount)

    /**
     * Lazily converts 16-bit PCM little-endian byte array to normalized float samples [-1.0f, 1.0f].
     */
    val floatSamples: FloatArray by lazy {
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val floats = FloatArray(shortBuffer.remaining())
        for (i in floats.indices) {
            floats[i] = shortBuffer.get(i) / 32768.0f
        }
        floats
    }

    /**
     * Calculates the Root Mean Square (RMS) energy across the frame.
     */
    val rms: Float by lazy {
        val samples = floatSamples
        if (samples.isEmpty()) return@lazy 0.0f
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += (sample * sample)
        }
        sqrt(sumSquares / samples.size).toFloat()
    }

    /**
     * Calculates RMS in decibels relative to full scale (dBFS), ranging from -100 dBFS to 0 dBFS.
     */
    val rmsDbfs: Float by lazy {
        val r = rms
        if (r <= 0.00001f) -100.0f else (20.0 * log10(r.toDouble())).toFloat()
    }

    /**
     * Peak absolute amplitude in range [0.0f, 1.0f].
     */
    val peakAmplitude: Float by lazy {
        val samples = floatSamples
        var peak = 0.0f
        for (sample in samples) {
            val absSample = kotlin.math.abs(sample)
            if (absSample > peak) {
                peak = absSample
            }
        }
        peak
    }

    /**
     * Creates a new AudioFrame with modified float samples, re-encoding them to 16-bit PCM.
     */
    fun withFloatSamples(newSamples: FloatArray): AudioFrame {
        val byteBuffer = ByteBuffer.allocate(newSamples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in newSamples) {
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            val shortVal = (clamped * 32767.0f).toInt().toShort()
            byteBuffer.putShort(shortVal)
        }
        return copy(pcmData = byteBuffer.array())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioFrame

        if (!pcmData.contentEquals(other.pcmData)) return false
        if (sampleRate != other.sampleRate) return false
        if (channelCount != other.channelCount) return false
        if (timestampMs != other.timestampMs) return false
        if (frameIndex != other.frameIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pcmData.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + frameIndex.hashCode()
        return result
    }
}
