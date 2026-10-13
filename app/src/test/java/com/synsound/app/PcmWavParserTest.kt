package com.synsound.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmWavParserTest {

    @Test
    fun parsesMonoPcm16Wav() {
        val bytes = makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = byteArrayOf(0, 0, 0xFF.toByte(), 0x7F))
        val parsed = PcmWavParser.parse(bytes)

        assertEquals(16000, parsed.sampleRate)
        assertEquals(1, parsed.channels)
        assertEquals(44, parsed.dataOffset)
        assertEquals(4, parsed.dataLength)
    }

    @Test
    fun parsesStereoPcm16Wav() {
        val bytes = makeWav(channels = 2, sampleRate = 48000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(8))
        val parsed = PcmWavParser.parse(bytes)

        assertEquals(48000, parsed.sampleRate)
        assertEquals(2, parsed.channels)
        assertEquals(8, parsed.dataLength)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCompressedWav() {
        PcmWavParser.parse(makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 3, data = ByteArray(4)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDataChunkLength() {
        val bytes = makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(4))
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(40, Int.MAX_VALUE)
        PcmWavParser.parse(bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDeclaredRiffLengthBeyondAvailableBytes() {
        val bytes = makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(4))
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, bytes.size + 100)
        PcmWavParser.parse(bytes)
    }

    @Test
    fun parsesOnlyTheValidPortionOfAnExpandedReadBuffer() {
        val wav = makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(4))
        val expanded = wav.copyOf(wav.size + 4096)
        val parsed = PcmWavParser.parse(expanded, wav.size)
        assertEquals(4, parsed.dataLength)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRiffTruncatedByLogicalLength() {
        val wav = makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(4))
        PcmWavParser.parse(wav.copyOf(wav.size + 4096), wav.size - 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedChannelCount() {
        PcmWavParser.parse(makeWav(channels = 3, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(6)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSampleRateOutsideSupportedRange() {
        PcmWavParser.parse(makeWav(channels = 1, sampleRate = 192000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(4)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyAudioData() {
        PcmWavParser.parse(makeWav(channels = 1, sampleRate = 16000, bitsPerSample = 16, audioFormat = 1, data = ByteArray(0)))
    }

    private fun makeWav(
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        audioFormat: Int,
        data: ByteArray
    ): ByteArray {
        val bytes = ByteArray(44 + data.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(bytes.size - 8)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(audioFormat.toShort())
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * bitsPerSample / 8)
        buffer.putShort((channels * bitsPerSample / 8).toShort())
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(data.size)
        buffer.put(data)
        return bytes
    }
}
