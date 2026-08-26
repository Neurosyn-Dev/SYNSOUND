package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 5-Band Forensic Equalizer engineered for speech intelligibility enhancement,
 * HVAC rumble suppression, and high-frequency hiss reduction.
 */
class ForensicEqualizer(
    override var isEnabled: Boolean = true,
    var lowCutGainDb: Float = -6.0f,     // 100 Hz rumble cut
    var lowMidGainDb: Float = -3.0f,     // 300 Hz boxiness cut
    var midClarityGainDb: Float = 4.0f,  // 2.5 kHz speech clarity boost
    var presenceGainDb: Float = 3.0f,    // 5.0 kHz consonants/crispness boost
    var highCutGainDb: Float = -4.0f     // 10 kHz hiss roll-off
) : AudioFilter {

    override val name: String = "Forensic Speech Equalizer"

    private val bands = listOf(
        EqBand(100f, 0.707f),
        EqBand(300f, 1.0f),
        EqBand(2500f, 1.2f),
        EqBand(5000f, 1.0f),
        EqBand(10000f, 0.707f)
    )

    private class EqBand(val frequencyHz: Float, val q: Float) {
        var b0 = 1f; var b1 = 0f; var b2 = 0f
        var a1 = 0f; var a2 = 0f
        var z1 = 0f; var z2 = 0f

        fun update(sampleRate: Int, gainDb: Float) {
            val a = 10.0f.pow(gainDb / 40.0f)
            val omega = (2.0 * PI * frequencyHz.coerceIn(20f, (sampleRate / 2 - 20).toFloat()) / sampleRate).toFloat()
            val sn = sin(omega.toDouble()).toFloat()
            val cs = cos(omega.toDouble()).toFloat()
            val alpha = sn / (2.0f * q)

            // Peaking EQ coefficients
            val b0_temp = 1.0f + alpha * a
            val b1_temp = -2.0f * cs
            val b2_temp = 1.0f - alpha * a
            val a0_temp = 1.0f + alpha / a
            val a1_temp = -2.0f * cs
            val a2_temp = 1.0f - alpha / a

            b0 = b0_temp / a0_temp
            b1 = b1_temp / a0_temp
            b2 = b2_temp / a0_temp
            a1 = a1_temp / a0_temp
            a2 = a2_temp / a0_temp
        }

        fun processSample(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }

        fun reset() {
            z1 = 0f
            z2 = 0f
        }
    }

    private var lastSampleRate = 0

    private fun updateAllBands(sampleRate: Int) {
        lastSampleRate = sampleRate
        bands[0].update(sampleRate, lowCutGainDb)
        bands[1].update(sampleRate, lowMidGainDb)
        bands[2].update(sampleRate, midClarityGainDb)
        bands[3].update(sampleRate, presenceGainDb)
        bands[4].update(sampleRate, highCutGainDb)
    }

    override fun process(frame: AudioFrame): AudioFrame {
        if (!isEnabled) return frame

        if (lastSampleRate != frame.sampleRate) {
            updateAllBands(frame.sampleRate)
        }

        val input = frame.floatSamples
        val output = FloatArray(input.size)

        for (i in input.indices) {
            var sample = input[i]
            for (band in bands) {
                sample = band.processSample(sample)
            }
            output[i] = sample
        }

        return frame.withFloatSamples(output)
    }

    override fun reset() {
        for (band in bands) {
            band.reset()
        }
    }
}
