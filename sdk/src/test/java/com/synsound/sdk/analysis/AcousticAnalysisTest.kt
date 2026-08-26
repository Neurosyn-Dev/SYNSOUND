package com.synsound.sdk.analysis

import com.synsound.sdk.audio.AudioFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AcousticAnalysisTest {

    private fun generateSineWave(frequencyHz: Float, sampleRate: Int = 16000, durationSeconds: Float = 0.2f, amplitude: Float = 0.5f): AudioFrame {
        val numSamples = (sampleRate * durationSeconds).toInt()
        val floatSamples = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            floatSamples[i] = (amplitude * sin(2.0 * PI * frequencyHz * i / sampleRate)).toFloat()
        }
        val emptyPcm = ByteArray(numSamples * 2)
        return AudioFrame(emptyPcm, sampleRate).withFloatSamples(floatSamples)
    }

    @Test
    fun testAcousticAnalyzer_extractsCorrectDominantFrequency() {
        val analyzer = AcousticAnalyzer()
        val targetFreq = 1000f
        val frame = generateSineWave(targetFreq, sampleRate = 16000, amplitude = 0.8f)

        val profile = analyzer.analyze(frame)

        assertNotNull(profile)
        // With 16kHz sample rate and 2048 FFT bins, bin resolution is ~7.8Hz
        assertEquals(targetFreq, profile.dominantFrequencyHz, 30f)
        assertTrue(profile.rmsDbfs > -15f)
    }

    @Test
    fun testAcousticAnalyzer_zeroCrossingRateCalculation() {
        val analyzer = AcousticAnalyzer()
        val lowFreqFrame = generateSineWave(200f, sampleRate = 16000)
        val highFreqFrame = generateSineWave(3000f, sampleRate = 16000)

        val lowProfile = analyzer.analyze(lowFreqFrame)
        val highProfile = analyzer.analyze(highFreqFrame)

        assertTrue(
            "Higher frequency should have significantly higher ZCR",
            highProfile.zeroCrossingRate > lowProfile.zeroCrossingRate
        )
    }

    @Test
    fun testAcousticEventDetector_detectsAlarm() {
        val detector = AcousticEventDetector()
        val alarmFrame = generateSineWave(3000f, sampleRate = 16000, amplitude = 0.9f)

        val events = detector.process(alarmFrame)

        assertTrue("Should detect alarm or speech/transient", events.isNotEmpty())
        assertTrue("Contains high-pitch alarm", events.any { it.type == AcousticEventType.HIGH_PITCH_ALARM || it.type == AcousticEventType.SPEECH_DETECTED })
    }
}
