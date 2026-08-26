package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * Noise Gate filter with smooth attack, hold, and release envelopes to eliminate low-level background noise.
 */
class NoiseGateFilter(
    override var isEnabled: Boolean = true,
    var thresholdDbfs: Float = -45.0f,
    var attackMs: Float = 10.0f,
    var releaseMs: Float = 100.0f,
    var floorDb: Float = -60.0f
) : AudioFilter {

    override val name: String = "Forensic Noise Gate"

    private var currentEnvelope = 0.0f
    private var currentGain = 1.0f

    private val linearFloor: Float
        get() = 10.0f.pow(floorDb / 20.0f)

    private val linearThreshold: Float
        get() = 10.0f.pow(thresholdDbfs / 20.0f)

    override fun process(frame: AudioFrame): AudioFrame {
        if (!isEnabled) return frame

        val input = frame.floatSamples
        val output = FloatArray(input.size)
        val sampleRate = frame.sampleRate

        // Envelope follower coefficients
        val attackCoeff = exp((-1.0 / (sampleRate * (attackMs / 1000.0).coerceAtLeast(0.001)))).toFloat()
        val releaseCoeff = exp((-1.0 / (sampleRate * (releaseMs / 1000.0).coerceAtLeast(0.001)))).toFloat()
        val floor = linearFloor
        val threshold = linearThreshold

        for (i in input.indices) {
            val sampleAbs = abs(input[i])

            // Envelope detection
            currentEnvelope = if (sampleAbs > currentEnvelope) {
                attackCoeff * currentEnvelope + (1.0f - attackCoeff) * sampleAbs
            } else {
                releaseCoeff * currentEnvelope + (1.0f - releaseCoeff) * sampleAbs
            }

            // Target gain based on threshold
            val targetGain = if (currentEnvelope >= threshold) {
                1.0f
            } else {
                floor
            }

            // Smooth gain transition
            currentGain = if (targetGain > currentGain) {
                attackCoeff * currentGain + (1.0f - attackCoeff) * targetGain
            } else {
                releaseCoeff * currentGain + (1.0f - releaseCoeff) * targetGain
            }

            output[i] = input[i] * currentGain
        }

        return frame.withFloatSamples(output)
    }

    override fun reset() {
        currentEnvelope = 0.0f
        currentGain = 1.0f
    }
}
