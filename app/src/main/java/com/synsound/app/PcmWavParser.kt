package com.synsound.app

/**
 * Validates and describes uncompressed PCM 16-bit RIFF/WAVE input.
 * The returned offsets refer to the supplied byte array; audio stays local.
 */
internal object PcmWavParser {

    data class WavInfo(
        val sampleRate: Int,
        val channels: Int,
        val dataOffset: Int,
        val dataLength: Int
    )

    fun parse(bytes: ByteArray): WavInfo {
        if (bytes.size < 44 ||
            ascii(bytes, 0, 4) != "RIFF" ||
            ascii(bytes, 8, 4) != "WAVE"
        ) {
            throw IllegalArgumentException("This file is not a valid RIFF/WAVE file.")
        }

        var offset = 12
        var formatFound = false
        var dataOffset = -1
        var dataLength = 0
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var audioFormat = 0

        while (offset + 8 <= bytes.size) {
            val chunkId = ascii(bytes, offset, 4)
            val chunkSize = readIntLE(bytes, offset + 4).toLong() and 0xFFFFFFFFL
            val chunkStart = offset + 8
            if (chunkSize > bytes.size.toLong() || chunkStart.toLong() + chunkSize > bytes.size.toLong()) {
                throw IllegalArgumentException("The WAV file contains an invalid chunk length.")
            }

            when (chunkId) {
                "fmt " -> {
                    if (chunkSize < 16) throw IllegalArgumentException("The WAV format chunk is incomplete.")
                    audioFormat = readShortLE(bytes, chunkStart).toInt() and 0xFFFF
                    channels = readShortLE(bytes, chunkStart + 2).toInt() and 0xFFFF
                    sampleRate = readIntLE(bytes, chunkStart + 4)
                    bitsPerSample = readShortLE(bytes, chunkStart + 14).toInt() and 0xFFFF
                    formatFound = true
                }
                "data" -> {
                    dataOffset = chunkStart
                    dataLength = chunkSize.toInt()
                }
            }

            offset = chunkStart + chunkSize.toInt() + (chunkSize.toInt() and 1)
            if (formatFound && dataOffset >= 0) break
        }

        if (!formatFound || dataOffset < 0) {
            throw IllegalArgumentException("The WAV file is missing its format or audio-data chunk.")
        }
        if (audioFormat != 1 || bitsPerSample != 16) {
            throw IllegalArgumentException("Only uncompressed PCM 16-bit WAV files are supported.")
        }
        if (channels !in 1..2) {
            throw IllegalArgumentException("Only mono or stereo WAV files are supported.")
        }
        if (sampleRate !in 8000..96000) {
            throw IllegalArgumentException("Sample rates from 8,000 to 96,000 Hz are supported.")
        }
        if (dataLength <= 0 || dataLength % (channels * 2) != 0) {
            throw IllegalArgumentException("The WAV audio-data chunk is empty or incomplete.")
        }

        return WavInfo(sampleRate, channels, dataOffset, dataLength)
    }

    private fun readShortLE(bytes: ByteArray, offset: Int): Short =
        ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort()

    private fun readIntLE(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            (bytes[offset + 3].toInt() shl 24)

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)
}
