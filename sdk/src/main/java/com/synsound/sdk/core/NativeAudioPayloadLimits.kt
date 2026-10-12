package com.synsound.sdk.core

/**
 * Bounds checks for PCM frames received over the native WebView audio bridge.
 * Kept free of Android APIs so malformed and oversized payload cases can be unit-tested.
 */
object NativeAudioPayloadLimits {
    const val SAMPLE_RATE = 16_000
    const val MAX_PCM_BYTES = 16_000
    const val MAX_ENCODED_PCM_CHARS = 24_000
    const val MAX_MESSAGE_CHARS = 25_000

    fun isMessageLengthAllowed(length: Int): Boolean =
        length in 1..MAX_MESSAGE_CHARS

    fun isEncodedLengthAllowed(length: Int): Boolean =
        length in 1..MAX_ENCODED_PCM_CHARS

    fun isMetadataAllowed(sampleRate: Int, channels: Int): Boolean =
        sampleRate == SAMPLE_RATE && channels == 1

    fun isPcmByteCountAllowed(byteCount: Int): Boolean =
        byteCount in 2..MAX_PCM_BYTES && byteCount % 2 == 0
}
