package com.synsound.sdk.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.synsound.sdk.analysis.SpectralProfile
import com.synsound.sdk.audio.AudioFrame
import kotlin.math.abs

/**
 * Real-time hardware-accelerated visualizer for live audio waveforms and frequency spectra.
 */
class SynSoundVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class VisualizationMode {
        WAVEFORM,
        SPECTRUM_BARS,
        CIRCULAR_OSCILLOSCOPE
    }

    var mode: VisualizationMode = VisualizationMode.WAVEFORM

    var primaryColor: Int = Color.parseColor("#00E5FF")
        set(value) {
            field = value
            updatePaints()
            invalidate()
        }

    var secondaryColor: Int = Color.parseColor("#7C4DFF")
        set(value) {
            field = value
            updatePaints()
            invalidate()
        }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val wavePath = Path()
    private var currentSamples: FloatArray? = null
    private var currentSpectrum: FloatArray? = null

    init {
        updatePaints()
    }

    private fun updatePaints() {
        wavePaint.color = primaryColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val gradient = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                primaryColor, secondaryColor, Shader.TileMode.CLAMP
            )
            wavePaint.shader = gradient
            barPaint.shader = gradient
        }
    }

    fun updateAudioFrame(frame: AudioFrame) {
        currentSamples = frame.floatSamples
        postInvalidateOnAnimation()
    }

    fun updateSpectralProfile(profile: SpectralProfile) {
        currentSpectrum = profile.spectrumMagnitudes
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (mode) {
            VisualizationMode.WAVEFORM -> drawWaveform(canvas, w, h)
            VisualizationMode.SPECTRUM_BARS -> drawSpectrumBars(canvas, w, h)
            VisualizationMode.CIRCULAR_OSCILLOSCOPE -> drawWaveform(canvas, w, h)
        }
    }

    private fun drawWaveform(canvas: Canvas, w: Float, h: Float) {
        val samples = currentSamples ?: return
        if (samples.isEmpty()) return

        wavePath.reset()
        val midY = h / 2f
        val step = (samples.size.toFloat() / w).coerceAtLeast(1f).toInt()

        var firstPoint = true
        var x = 0f
        val dx = w / (samples.size / step)

        for (i in samples.indices step step) {
            val y = midY - (samples[i] * (h / 2.2f))
            if (firstPoint) {
                wavePath.moveTo(x, y)
                firstPoint = false
            } else {
                wavePath.lineTo(x, y)
            }
            x += dx
        }

        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawSpectrumBars(canvas: Canvas, w: Float, h: Float) {
        val spectrum = currentSpectrum ?: return
        if (spectrum.isEmpty()) return

        val numBars = 32.coerceAtMost(spectrum.size)
        val barWidth = (w / numBars) * 0.75f
        val gap = (w / numBars) * 0.25f
        val binStep = spectrum.size / numBars

        for (i in 0 until numBars) {
            val binIndex = (i * binStep).coerceIn(0, spectrum.size - 1)
            val magnitude = spectrum[binIndex]
            val barHeight = (magnitude * h * 4f).coerceIn(4f, h)

            val left = i * (barWidth + gap) + gap / 2f
            val top = h - barHeight
            val right = left + barWidth
            val bottom = h

            canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, barPaint)
        }
    }
}
