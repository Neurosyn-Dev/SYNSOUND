package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2nd-Order Biquad filter supporting Low-Pass, High-Pass, and Band-Pass filtering.
 * Essential for forensic speech isolation (e.g. 300Hz - 3400Hz) and ambient noise reduction.
 */
class BandpassFilter(
    override var isEnabled: Boolean = true,
    var type: FilterType = FilterType.BAND_PASS,
    var centerFrequencyHz: Float = 1000.0f,
    var bandwidthOctaves: Float = 1.5f,
    var qFactor: Float = 0.707f
) : AudioFilter {

    override val name: String = "Biquad Filter (${type.name})"

    enum class FilterType {
        LOW_PASS,
        HIGH_PASS,
        BAND_PASS,
        NOTCH
    }

    // Biquad coefficients
    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    // Filter state delays for up to 2 channels
    private var z1_0 = 0.0f
    private var z2_0 = 0.0f
    private var z1_1 = 0.0f
    private var z2_1 = 0.0f

    private var lastSampleRate = 0

    private fun updateCoefficients(sampleRate: Int) {
        if (sampleRate <= 0) return
        lastSampleRate = sampleRate

        val omega = (2.0 * PI * centerFrequencyHz.coerceIn(20.0f, (sampleRate / 2 - 10).toFloat()) / sampleRate).toFloat()
        val sn = sin(omega.toDouble()).toFloat()
        val cs = cos(omega.toDouble()).toFloat()
        val alpha = sn / (2.0f * qFactor.coerceAtLeast(0.1f))

        var a0 = 1.0f

        when (type) {
            FilterType.LOW_PASS -> {
                b0 = (1.0f - cs) / 2.0f
                b1 = 1.0f - cs
                b2 = (1.0f - cs) / 2.0f
                a0 = 1.0f + alpha
                a1 = -2.0f * cs
                a2 = 1.0f - alpha
            }
            FilterType.HIGH_PASS -> {
                b0 = (1.0f + cs) / 2.0f
                b1 = -(1.0f + cs)
                b2 = (1.0f + cs) / 2.0f
                a0 = 1.0f + alpha
                a1 = -2.0f * cs
                a2 = 1.0f - alpha
            }
            FilterType.BAND_PASS -> {
                b0 = alpha
                b1 = 0.0f
                b2 = -alpha
                a0 = 1.0f + alpha
                a1 = -2.0f * cs
                a2 = 1.0f - alpha
            }
            FilterType.NOTCH -> {
                b0 = 1.0f
                b1 = -2.0f * cs
                b2 = 1.0f
                a0 = 1.0f + alpha
                a1 = -2.0f * cs
                a2 = 1.0f - alpha
            }
        }

        // Normalize coefficients by a0
        b0 /= a0
        b1 /= a0
        b2 /= a0
        a1 /= a0
        a2 /= a0
    }

    override fun process(frame: AudioFrame): AudioFrame {
        if (!isEnabled) return frame

        if (lastSampleRate != frame.sampleRate) {
            updateCoefficients(frame.sampleRate)
        }

        val input = frame.floatSamples
        val output = FloatArray(input.size)
        val channels = frame.channelCount

        if (channels == 1) {
            for (i in input.indices) {
                val x = input[i]
                val y = b0 * x + z1_0
                z1_0 = b1 * x - a1 * y + z2_0
                z2_0 = b2 * x - a2 * y
                output[i] = y
            }
        } else {
            // Stereo processing
            for (i in input.indices step 2) {
                val x0 = input[i]
                val y0 = b0 * x0 + z1_0
                z1_0 = b1 * x0 - a1 * y0 + z2_0
                z2_0 = b2 * x0 - a2 * y0
                output[i] = y0

                if (i + 1 < input.size) {
                    val x1 = input[i + 1]
                    val y1 = b0 * x1 + z1_1
                    z1_1 = b1 * x1 - a1 * y1 + z2_1
                    z2_1 = b2 * x1 - a2 * y1
                    output[i + 1] = y1
                }
            }
        }

        return frame.withFloatSamples(output)
    }

    override fun reset() {
        z1_0 = 0.0f
        z2_0 = 0.0f
        z1_1 = 0.0f
        z2_1 = 0.0f
    }
}
