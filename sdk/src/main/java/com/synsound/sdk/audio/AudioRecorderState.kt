package com.synsound.sdk.audio

/**
 * State transitions for the AudioCaptureEngine.
 */
enum class AudioRecorderState {
    UNINITIALIZED,
    READY,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR
}
