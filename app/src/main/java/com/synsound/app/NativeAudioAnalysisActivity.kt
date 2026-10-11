package com.synsound.app

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.synsound.sdk.analysis.AcousticAnalyzer
import com.synsound.sdk.analysis.AcousticEventDetector
import com.synsound.sdk.audio.AudioFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Offline, on-device analysis of 16-bit PCM WAV files.
 *
 * This feature does not open the microphone or upload audio. Its event indicators
 * are heuristic and must not be treated as forensic conclusions.
 */
class NativeAudioAnalysisActivity : AppCompatActivity() {

    private lateinit var chooseButton: MaterialButton
    private lateinit var progress: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var statusText: TextView

    private val openAudioDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) analyzeSelectedFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF000000.toInt())
            setPadding(dp(20), dp(16), dp(20), dp(20))
        }

        val title = TextView(this).apply {
            text = "Native Audio Analysis"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Analyze a PCM WAV file locally on this device. No microphone access or upload is used."
            textSize = 14f
            setTextColor(0xFFB8C0CC.toInt())
            setPadding(0, dp(8), 0, dp(16))
        }
        root.addView(subtitle, matchWrap())

        chooseButton = MaterialButton(this).apply {
            text = "Choose WAV file"
            isAllCaps = false
            setOnClickListener {
                openAudioDocument.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream"))
            }
        }
        root.addView(chooseButton, matchWrap())

        progress = ProgressBar(this).apply {
            visibility = View.GONE
            isIndeterminate = true
        }
        root.addView(progress, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
            topMargin = dp(16)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        })

        statusText = TextView(this).apply {
            text = "Supported input: uncompressed PCM, 16-bit, mono or stereo WAV."
            textSize = 13f
            setTextColor(0xFF00E5FF.toInt())
            setPadding(0, dp(16), 0, dp(8))
        }
        root.addView(statusText, matchWrap())

        resultText = TextView(this).apply {
            text = "Choose a file to calculate signal level, peak amplitude, and heuristic acoustic-event indicators."
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, dp(8), 0, dp(16))
            setTextIsSelectable(true)
        }

        val scroll = ScrollView(this).apply {
            addView(resultText)
            clipToPadding = false
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val closeButton = MaterialButton(this).apply {
            text = "Back to SynSound"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        root.addView(closeButton, matchWrap())

        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                dp(20) + bars.left,
                dp(16) + bars.top,
                dp(20) + bars.right,
                dp(20) + bars.bottom
            )
            insets
        }
    }

    private fun analyzeSelectedFile(uri: Uri) {
        chooseButton.isEnabled = false
        progress.visibility = View.VISIBLE
        statusText.text = "Reading file…"
        resultText.text = ""

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val displayName = getDisplayName(contentResolver, uri)
                    val bytes = readBounded(contentResolver, uri, MAX_FILE_BYTES)
                    analyzeWav(bytes, displayName)
                }
                statusText.text = "Analysis complete · processed locally"
                resultText.text = result
            } catch (error: Exception) {
                statusText.text = "Analysis could not be completed"
                resultText.text = error.message ?: "Unsupported or unreadable audio file."
                Toast.makeText(
                    this@NativeAudioAnalysisActivity,
                    "Could not analyze this WAV file",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                chooseButton.isEnabled = true
                progress.visibility = View.GONE
            }
        }
    }

    private suspend fun analyzeWav(bytes: ByteArray, displayName: String): String {
        val wav = parsePcmWav(bytes)
        val analyzer = AcousticAnalyzer()
        val detector = AcousticEventDetector(analyzer)
        val eventCounts = linkedMapOf<String, Int>()
        var sampleCount = 0L
        var sumSquares = 0.0
        var peak = 0.0
        var frameCount = 0

        val dataEnd = wav.dataOffset + wav.dataLength
        val bytesPerSampleFrame = wav.channels * 2
        val totalSamples = wav.dataLength / bytesPerSampleFrame
        val frameSamples = 1024
        var sampleOffset = wav.dataOffset

        while (sampleOffset + bytesPerSampleFrame <= dataEnd) {
            coroutineContextEnsureActive()
            val samplesThisFrame = minOf(frameSamples, (dataEnd - sampleOffset) / bytesPerSampleFrame)
            if (samplesThisFrame <= 0) break

            val monoPcm = ByteBuffer.allocate(samplesThisFrame * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until samplesThisFrame) {
                val samplePosition = sampleOffset + i * bytesPerSampleFrame
                val left = readShortLE(bytes, samplePosition).toInt()
                val mono = if (wav.channels == 2) {
                    val right = readShortLE(bytes, samplePosition + 2).toInt()
                    ((left + right) / 2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                } else {
                    left
                }
                monoPcm.putShort(mono.toShort())
                val normalized = mono / 32768.0
                sumSquares += normalized * normalized
                peak = max(peak, kotlin.math.abs(normalized))
                sampleCount++
            }

            val frame = AudioFrame(
                pcmData = monoPcm.array(),
                sampleRate = wav.sampleRate,
                channelCount = 1,
                timestampMs = (sampleCount * 1000L) / wav.sampleRate,
                frameIndex = frameCount.toLong()
            )
            if (frame.sampleCount >= 16) {
                detector.process(frame).forEach { event ->
                    val key = event.type.displayName
                    eventCounts[key] = (eventCounts[key] ?: 0) + 1
                }
            }
            frameCount++
            sampleOffset += samplesThisFrame * bytesPerSampleFrame
        }

        if (sampleCount == 0L) throw IllegalArgumentException("The WAV file contains no audio samples.")
        val rms = sqrt(sumSquares / sampleCount)
        val rmsDbfs = if (rms <= 0.00001) -100.0 else 20.0 * kotlin.math.log10(rms)
        val peakDbfs = if (peak <= 0.00001) -100.0 else 20.0 * kotlin.math.log10(peak)
        val durationSeconds = totalSamples.toDouble() / wav.sampleRate
        val duration = "%d:%02d".format((durationSeconds / 60).toInt(), durationSeconds.toInt() % 60)
        val indicators = if (eventCounts.isEmpty()) {
            "No event indicators were raised by the heuristic detector."
        } else {
            eventCounts.entries
                .sortedByDescending { it.value }
                .joinToString("\n") { "• \${it.key}: \${it.value} frame(s)" }
        }

        return buildString {
            appendLine(displayName)
            appendLine()
            appendLine("FILE")
            appendLine("Duration: $duration")
            appendLine("Sample rate: \${wav.sampleRate} Hz")
            appendLine("Channels: \${wav.channels} (downmixed to mono for analysis)")
            appendLine("Encoding: PCM 16-bit")
            appendLine()
            appendLine("SIGNAL METRICS")
            appendLine("Overall RMS: %.1f dBFS".format(rmsDbfs))
            appendLine("Peak level: %.1f dBFS".format(peakDbfs))
            appendLine("Frames analyzed: $frameCount")
            appendLine()
            appendLine("HEURISTIC EVENT INDICATORS")
            appendLine(indicators)
            appendLine()
            append("Caution: these are automated signal indicators, not verified event identification or forensic conclusions. Review the original recording and context before drawing conclusions.")
        }
    }

    private fun parsePcmWav(bytes: ByteArray): WavInfo {
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

    private fun readBounded(resolver: ContentResolver, uri: Uri, limit: Int): ByteArray {
        val input = resolver.openInputStream(uri) ?: throw IllegalArgumentException("The selected file could not be opened.")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > limit) {
                    throw IllegalArgumentException("This first version supports WAV files up to 64 MB.")
                }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }

    private fun getDisplayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index) ?: "Selected WAV file"
            }
        }
        return uri.lastPathSegment ?: "Selected WAV file"
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

    private suspend fun coroutineContextEnsureActive() {
        coroutineContext.ensureActive()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private data class WavInfo(
        val sampleRate: Int,
        val channels: Int,
        val dataOffset: Int,
        val dataLength: Int
    )

    companion object {
        private const val MAX_FILE_BYTES = 64 * 1024 * 1024
    }
}
