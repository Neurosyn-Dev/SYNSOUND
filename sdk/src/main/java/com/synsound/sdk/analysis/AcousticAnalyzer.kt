package com.synsound.sdk.analysis

import com.synsound.sdk.audio.AudioFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time acoustic feature extraction engine utilizing Cooley-Tukey FFT
 * and spectral decomposition.
 */
class AcousticAnalyzer {

    /**
     * Computes the complete [SpectralProfile] for an input [AudioFrame].
     */
    fun analyze(frame: AudioFrame): SpectralProfile {
        val samples = frame.floatSamples
        if (samples.isEmpty()) {
            return SpectralProfile(
                rmsDbfs = -100f,
                peakAmplitude = 0f,
                zeroCrossingRate = 0f,
                spectralCentroidHz = 0f,
                spectralRolloffHz = 0f,
                dominantFrequencyHz = 0f,
                subBassEnergy = 0f,
                bassEnergy = 0f,
                lowMidEnergy = 0f,
                highMidEnergy = 0f,
                trebleEnergy = 0f
            )
        }

        val zcr = computeZeroCrossingRate(samples)
        val n = getNextPowerOfTwo(samples.size).coerceAtMost(2048)
        val magnitudes = computeFftMagnitudes(samples, n)

        val sampleRate = frame.sampleRate
        val binWidth = sampleRate.toFloat() / n
        val nyquistBins = magnitudes.size

        var totalMagnitude = 0.0f
        var weightedFreqSum = 0.0f
        var maxMag = 0.0f
        var dominantBin = 0

        for (bin in 0 until nyquistBins) {
            val mag = magnitudes[bin]
            val freq = bin * binWidth
            totalMagnitude += mag
            weightedFreqSum += (mag * freq)

            if (mag > maxMag) {
                maxMag = mag
                dominantBin = bin
            }
        }

        // Spectral Centroid
        val spectralCentroid = if (totalMagnitude > 0.0001f) {
            weightedFreqSum / totalMagnitude
        } else {
            0.0f
        }

        // Spectral Rolloff (85% energy threshold)
        val rolloffThreshold = totalMagnitude * 0.85f
        var cumulativeMag = 0.0f
        var rolloffFreq = 0.0f
        for (bin in 0 until nyquistBins) {
            cumulativeMag += magnitudes[bin]
            if (cumulativeMag >= rolloffThreshold) {
                rolloffFreq = bin * binWidth
                break
            }
        }

        val dominantFreq = dominantBin * binWidth

        // Band Energy Integrations
        val subBass = integrateBandEnergy(magnitudes, binWidth, 20f, 60f)
        val bass = integrateBandEnergy(magnitudes, binWidth, 60f, 250f)
        val lowMid = integrateBandEnergy(magnitudes, binWidth, 250f, 1000f)
        val highMid = integrateBandEnergy(magnitudes, binWidth, 1000f, 4000f)
        val treble = integrateBandEnergy(magnitudes, binWidth, 4000f, 20000f)

        val bandSum = (subBass + bass + lowMid + highMid + treble).coerceAtLeast(0.00001f)

        return SpectralProfile(
            rmsDbfs = frame.rmsDbfs,
            peakAmplitude = frame.peakAmplitude,
            zeroCrossingRate = zcr,
            spectralCentroidHz = spectralCentroid,
            spectralRolloffHz = rolloffFreq,
            dominantFrequencyHz = dominantFreq,
            subBassEnergy = (subBass / bandSum).coerceIn(0f, 1f),
            bassEnergy = (bass / bandSum).coerceIn(0f, 1f),
            lowMidEnergy = (lowMid / bandSum).coerceIn(0f, 1f),
            highMidEnergy = (highMid / bandSum).coerceIn(0f, 1f),
            trebleEnergy = (treble / bandSum).coerceIn(0f, 1f),
            spectrumMagnitudes = magnitudes
        )
    }

    private fun computeZeroCrossingRate(samples: FloatArray): Float {
        if (samples.size < 2) return 0f
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) || (samples[i] < 0 && samples[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / (samples.size - 1)
    }

    private fun integrateBandEnergy(magnitudes: FloatArray, binWidth: Float, minFreq: Float, maxFreq: Float): Float {
        val startBin = (minFreq / binWidth).toInt().coerceIn(0, magnitudes.size - 1)
        val endBin = (maxFreq / binWidth).toInt().coerceIn(startBin, magnitudes.size - 1)
        var sum = 0.0f
        for (i in startBin..endBin) {
            sum += magnitudes[i] * magnitudes[i]
        }
        return sqrt(sum)
    }

    private fun computeFftMagnitudes(samples: FloatArray, n: Int): FloatArray {
        val real = FloatArray(n)
        val imag = FloatArray(n)

        // Apply Hann Window to reduce spectral leakage
        val len = samples.size.coerceAtMost(n)
        for (i in 0 until len) {
            val window = (0.5 * (1.0 - cos(2.0 * PI * i / (len - 1)))).toFloat()
            real[i] = samples[i] * window
        }

        // Cooley-Tukey Radix-2 In-Place FFT
        fft(real, imag, n)

        val halfSize = n / 2
        val magnitudes = FloatArray(halfSize)
        for (i in 0 until halfSize) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i]) / halfSize
        }
        return magnitudes
    }

    private fun fft(real: FloatArray, imag: FloatArray, n: Int) {
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + len / 2] * wR - imag[i + k + len / 2] * wI
                    val vI = real[i + k + len / 2] * wI + imag[i + k + len / 2] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + len / 2] = uR - vR
                    imag[i + k + len / 2] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun getNextPowerOfTwo(value: Int): Int {
        var power = 1
        while (power < value) {
            power = power shl 1
        }
        return power
    }
}
