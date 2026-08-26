package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages an ordered chain of [AudioFilter] instances to clean, enhance, and equalize real-time audio.
 */
class AudioEnhancementPipeline {

    enum class Preset {
        RAW_PASSTHROUGH,
        FORENSIC_SPEECH_ENHANCEMENT,
        NOISE_SUPPRESSION_ONLY,
        ACOUSTIC_SURVEILLANCE,
        HIGH_GAIN_BOOST
    }

    private val filters = CopyOnWriteArrayList<AudioFilter>()

    val filterCount: Int get() = filters.size

    val activeFilters: List<AudioFilter> get() = filters.toList()

    fun addFilter(filter: AudioFilter): AudioEnhancementPipeline = apply {
        filters.add(filter)
    }

    fun removeFilter(filter: AudioFilter): AudioEnhancementPipeline = apply {
        filters.remove(filter)
    }

    fun clearFilters(): AudioEnhancementPipeline = apply {
        filters.clear()
    }

    fun reset() {
        for (filter in filters) {
            filter.reset()
        }
    }

    fun applyPreset(preset: Preset): AudioEnhancementPipeline = apply {
        clearFilters()
        when (preset) {
            Preset.RAW_PASSTHROUGH -> {
                // No filters added
            }
            Preset.FORENSIC_SPEECH_ENHANCEMENT -> {
                // 1. Rumble cut & Bandpass (300Hz - 3400Hz standard telephonic/forensic voice band)
                addFilter(BandpassFilter(
                    type = BandpassFilter.FilterType.BAND_PASS,
                    centerFrequencyHz = 1600f,
                    qFactor = 0.6f
                ))
                // 2. Forensic Equalizer for vowel & consonant definition
                addFilter(ForensicEqualizer(
                    lowCutGainDb = -8.0f,
                    lowMidGainDb = -4.0f,
                    midClarityGainDb = 5.0f,
                    presenceGainDb = 4.0f,
                    highCutGainDb = -6.0f
                ))
                // 3. Noise gate to cut breathing and low room hum
                addFilter(NoiseGateFilter(
                    thresholdDbfs = -42.0f,
                    attackMs = 8.0f,
                    releaseMs = 80.0f,
                    floorDb = -40.0f
                ))
                // 4. Preamp gain boost (+6 dB) with soft clipping limiter
                addFilter(GainFilter(
                    gainDb = 6.0f
                ))
            }
            Preset.NOISE_SUPPRESSION_ONLY -> {
                addFilter(NoiseGateFilter(
                    thresholdDbfs = -38.0f,
                    attackMs = 12.0f,
                    releaseMs = 120.0f,
                    floorDb = -50.0f
                ))
            }
            Preset.ACOUSTIC_SURVEILLANCE -> {
                addFilter(BandpassFilter(
                    type = BandpassFilter.FilterType.HIGH_PASS,
                    centerFrequencyHz = 120f,
                    qFactor = 0.707f
                ))
                addFilter(GainFilter(
                    gainDb = 12.0f
                ))
            }
            Preset.HIGH_GAIN_BOOST -> {
                addFilter(GainFilter(
                    gainDb = 18.0f
                ))
            }
        }
    }

    /**
     * Executes all enabled filters sequentially on the input [AudioFrame].
     */
    fun process(frame: AudioFrame): AudioFrame {
        if (filters.isEmpty()) return frame

        var currentFrame = frame
        for (filter in filters) {
            if (filter.isEnabled) {
                currentFrame = filter.process(currentFrame)
            }
        }
        return currentFrame
    }
}
