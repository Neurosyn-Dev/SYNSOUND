package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class DspFilterTest {

    private fun generateSineWave(frequencyHz: Float, sampleRate: Int = 16000, durationSeconds: Float = 0.1f, amplitude: Float = 0.5f): AudioFrame {
        val numSamples = (sampleRate * durationSeconds).toInt()
        val floatSamples = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            floatSamples[i] = (amplitude * sin(2.0 * PI * frequencyHz * i / sampleRate)).toFloat()
        }
        val emptyPcm = ByteArray(numSamples * 2)
        return AudioFrame(emptyPcm, sampleRate).withFloatSamples(floatSamples)
    }

    @Test
    fun testGainFilter_amplifiesSignal() {
        val filter = GainFilter(isEnabled = true, gainDb = 6.0f) // ~2.0x linear
        val frame = generateSineWave(1000f, amplitude = 0.2f)

        val processed = filter.process(frame)

        assertTrue("Processed peak should be greater than original", processed.peakAmplitude > frame.peakAmplitude)
        assertEquals(0.4f, processed.peakAmplitude, 0.05f)
    }

    @Test
    fun testNoiseGateFilter_attenuatesBelowThreshold() {
        val filter = NoiseGateFilter(
            isEnabled = true,
            thresholdDbfs = -20.0f,
            floorDb = -60.0f,
            attackMs = 1.0f,
            releaseMs = 1.0f
        )
        // Very quiet frame at -40 dBFS
        val quietFrame = generateSineWave(1000f, amplitude = 0.01f)
        val processed = filter.process(quietFrame)

        assertTrue("Quiet frame should be attenuated by noise gate", processed.rms < quietFrame.rms)
    }

    @Test
    fun testBandpassFilter_filtersFrequencies() {
        val filter = BandpassFilter(
            isEnabled = true,
            type = BandpassFilter.FilterType.LOW_PASS,
            centerFrequencyHz = 500f,
            qFactor = 0.707f
        )

        // 4000 Hz high frequency should be heavily attenuated by 500 Hz low-pass filter
        val highFreqFrame = generateSineWave(4000f, amplitude = 0.8f)
        val processed = filter.process(highFreqFrame)

        assertTrue("High frequency should be attenuated by low-pass filter", processed.peakAmplitude < highFreqFrame.peakAmplitude * 0.4f)
    }

    @Test
    fun testAudioEnhancementPipeline_processesThroughFilters() {
        val pipeline = AudioEnhancementPipeline()
            .applyPreset(AudioEnhancementPipeline.Preset.FORENSIC_SPEECH_ENHANCEMENT)

        val frame = generateSineWave(1500f, amplitude = 0.3f)
        val enhanced = pipeline.process(frame)

        assertNotEquals(0f, enhanced.rms)
        assertTrue(enhanced.peakAmplitude > 0f)
    }
}
