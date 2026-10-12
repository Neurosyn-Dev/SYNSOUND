package com.synsound.sdk.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAudioPayloadLimitsTest {
    @Test
    fun messageLengthRejectsEmptyAndOversizedMessages() {
        assertFalse(NativeAudioPayloadLimits.isMessageLengthAllowed(0))
        assertTrue(NativeAudioPayloadLimits.isMessageLengthAllowed(1))
        assertTrue(NativeAudioPayloadLimits.isMessageLengthAllowed(NativeAudioPayloadLimits.MAX_MESSAGE_CHARS))
        assertFalse(NativeAudioPayloadLimits.isMessageLengthAllowed(NativeAudioPayloadLimits.MAX_MESSAGE_CHARS + 1))
    }

    @Test
    fun encodedPayloadLengthIsBounded() {
        assertFalse(NativeAudioPayloadLimits.isEncodedLengthAllowed(0))
        assertTrue(NativeAudioPayloadLimits.isEncodedLengthAllowed(1))
        assertTrue(NativeAudioPayloadLimits.isEncodedLengthAllowed(NativeAudioPayloadLimits.MAX_ENCODED_PCM_CHARS))
        assertFalse(NativeAudioPayloadLimits.isEncodedLengthAllowed(NativeAudioPayloadLimits.MAX_ENCODED_PCM_CHARS + 1))
    }

    @Test
    fun onlyMonoSixteenKilohertzFramesAreAccepted() {
        assertTrue(NativeAudioPayloadLimits.isMetadataAllowed(16_000, 1))
        assertFalse(NativeAudioPayloadLimits.isMetadataAllowed(44_100, 1))
        assertFalse(NativeAudioPayloadLimits.isMetadataAllowed(16_000, 2))
        assertFalse(NativeAudioPayloadLimits.isMetadataAllowed(0, 1))
    }

    @Test
    fun pcmMustBeNonemptyEvenAndWithinByteLimit() {
        assertFalse(NativeAudioPayloadLimits.isPcmByteCountAllowed(0))
        assertFalse(NativeAudioPayloadLimits.isPcmByteCountAllowed(1))
        assertTrue(NativeAudioPayloadLimits.isPcmByteCountAllowed(2))
        assertTrue(NativeAudioPayloadLimits.isPcmByteCountAllowed(NativeAudioPayloadLimits.MAX_PCM_BYTES))
        assertFalse(NativeAudioPayloadLimits.isPcmByteCountAllowed(NativeAudioPayloadLimits.MAX_PCM_BYTES + 2))
    }
}
