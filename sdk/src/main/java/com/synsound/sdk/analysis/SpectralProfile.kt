package com.synsound.sdk.analysis

/**
 * Detailed spectral features computed from an audio frame.
 */
data class SpectralProfile(
    val rmsDbfs: Float,
    val peakAmplitude: Float,
    val zeroCrossingRate: Float,
    val spectralCentroidHz: Float,
    val spectralRolloffHz: Float,
    val dominantFrequencyHz: Float,
    // 5-Band energy distribution (normalized [0.0 - 1.0])
    val subBassEnergy: Float,     // 20Hz - 60Hz
    val bassEnergy: Float,        // 60Hz - 250Hz
    val lowMidEnergy: Float,      // 250Hz - 1000Hz
    val highMidEnergy: Float,     // 1000Hz - 4000Hz
    val trebleEnergy: Float,      // 4000Hz - 20000Hz
    val spectrumMagnitudes: FloatArray = FloatArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpectralProfile

        if (rmsDbfs != other.rmsDbfs) return false
        if (peakAmplitude != other.peakAmplitude) return false
        if (zeroCrossingRate != other.zeroCrossingRate) return false
        if (spectralCentroidHz != other.spectralCentroidHz) return false
        if (spectralRolloffHz != other.spectralRolloffHz) return false
        if (dominantFrequencyHz != other.dominantFrequencyHz) return false
        if (subBassEnergy != other.subBassEnergy) return false
        if (bassEnergy != other.bassEnergy) return false
        if (lowMidEnergy != other.lowMidEnergy) return false
        if (highMidEnergy != other.highMidEnergy) return false
        if (trebleEnergy != other.trebleEnergy) return false
        if (!spectrumMagnitudes.contentEquals(other.spectrumMagnitudes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rmsDbfs.hashCode()
        result = 31 * result + peakAmplitude.hashCode()
        result = 31 * result + zeroCrossingRate.hashCode()
        result = 31 * result + spectralCentroidHz.hashCode()
        result = 31 * result + spectralRolloffHz.hashCode()
        result = 31 * result + dominantFrequencyHz.hashCode()
        result = 31 * result + subBassEnergy.hashCode()
        result = 31 * result + bassEnergy.hashCode()
        result = 31 * result + lowMidEnergy.hashCode()
        result = 31 * result + highMidEnergy.hashCode()
        result = 31 * result + trebleEnergy.hashCode()
        result = 31 * result + spectrumMagnitudes.contentHashCode()
        return result
    }
}
