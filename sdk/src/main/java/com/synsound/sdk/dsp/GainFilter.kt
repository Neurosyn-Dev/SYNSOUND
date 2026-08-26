package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame
import kotlin.math.pow
import kotlin.math.tanh

/**
 * Amplifies or attenuates the audio signal with soft-saturation clipping protection.
 */
class GainFilter(
    override var isEnabled: Boolean = true,
    gainDb: Float = 0.0f
) : AudioFilter {

    override val name: String = "Gain / Pre-Amp"

    var gainDb: Float = gainDb
        set(value) {
            field = value
            linearGain = 10.0f.pow(value / 20.0f)
        }

    private var linearGain: Float = 10.0f.pow(gainDb / 20.0f)

    /**
     * Set linear gain multiplier directly (e.g., 2.0 = +6dB).
     */
    fun setLinearMultiplier(multiplier: Float) {
        linearGain = multiplier.coerceAtLeast(0.0f)
    }

    override fun process(frame: AudioFrame): AudioFrame {
        if (!isEnabled || (linearGain >= 0.999f && linearGain <= 1.001f)) {
            return frame
        }

        val input = frame.floatSamples
        val output = FloatArray(input.size)

        for (i in input.indices) {
            val amplified = input[i] * linearGain
            // Soft saturation curve to gracefully handle peaks exceeding +/- 1.0
            output[i] = if (amplified > 1.0f || amplified < -1.0f) {
                tanh(amplified.toDouble()).toFloat()
            } else {
                amplified
            }
        }

        return frame.withFloatSamples(output)
    }

    override fun reset() {
        // Stateless
    }
}
