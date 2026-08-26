package com.synsound.sdk.audio

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Audio capture configuration parameters.
 */
data class AudioConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val audioSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION,
    val bufferMultiplier: Int = 2,
    val frameSizeInSamples: Int = 1024
) {
    val channelCount: Int
        get() = if (channelConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1

    val bytesPerSample: Int
        get() = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 2 else 1
}
