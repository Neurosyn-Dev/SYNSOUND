package com.synsound.sdk.analysis

import com.synsound.sdk.audio.AudioFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Real-time pattern classifier for detecting forensic acoustic events from audio frames.
 */
class AcousticEventDetector(
    private val analyzer: AcousticAnalyzer = AcousticAnalyzer()
) {

    fun interface EventListener {
        fun onEventDetected(event: AcousticEvent)
    }

    private val _eventFlow = MutableSharedFlow<AcousticEvent>(extraBufferCapacity = 32)
    val eventFlow: SharedFlow<AcousticEvent> = _eventFlow.asSharedFlow()

    private val listeners = CopyOnWriteArrayList<EventListener>()

    var speechThresholdDbfs = -42.0f
    var transientThresholdDbfs = -18.0f
    var alarmThresholdDbfs = -30.0f
    var silenceThresholdDbfs = -75.0f

    private var previousPeak = 0.0f
    private var baselineRms = -55.0f

    fun addListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: EventListener) {
        listeners.remove(listener)
    }

    /**
     * Evaluates an audio frame and emits any detected acoustic events.
     */
    fun process(frame: AudioFrame): List<AcousticEvent> {
        val profile = analyzer.analyze(frame)
        val detected = mutableListOf<AcousticEvent>()

        val rms = profile.rmsDbfs
        val peak = profile.peakAmplitude
        val dominant = profile.dominantFrequencyHz
        val zcr = profile.zeroCrossingRate

        // 1. Check for Loud Transient / Impact (sudden spike)
        val peakDelta = peak - previousPeak
        previousPeak = peak
        if (peakDelta > 0.4f && rms > transientThresholdDbfs) {
            val event = AcousticEvent(
                type = AcousticEventType.LOUD_TRANSIENT,
                confidence = (peakDelta * 1.2f).coerceIn(0.5f, 1.0f),
                peakDbfs = rms,
                spectralProfile = profile,
                metadata = mapOf("peakDelta" to peakDelta.toString())
            )
            detected.add(event)
        }

        // 2. Check for High Pitch Alarm / Siren (narrow band high frequency)
        if (dominant in 2000.0f..8000.0f && (profile.highMidEnergy + profile.trebleEnergy) > 0.65f && rms > alarmThresholdDbfs) {
            val confidence = ((profile.highMidEnergy + profile.trebleEnergy) * 0.9f).coerceIn(0.6f, 0.98f)
            val event = AcousticEvent(
                type = AcousticEventType.HIGH_PITCH_ALARM,
                confidence = confidence,
                peakDbfs = rms,
                spectralProfile = profile,
                metadata = mapOf("dominantFrequencyHz" to dominant.toString())
            )
            detected.add(event)
        }

        // 3. Check for Low-Frequency Rumble
        if (dominant in 20.0f..150.0f && (profile.subBassEnergy + profile.bassEnergy) > 0.70f && rms > -48.0f) {
            val confidence = (profile.subBassEnergy + profile.bassEnergy).coerceIn(0.5f, 0.95f)
            val event = AcousticEvent(
                type = AcousticEventType.LOW_FREQUENCY_RUMBLE,
                confidence = confidence,
                peakDbfs = rms,
                spectralProfile = profile,
                metadata = mapOf("dominantFrequencyHz" to dominant.toString())
            )
            detected.add(event)
        }

        // 4. Check for Human Speech Activity
        if (rms > speechThresholdDbfs &&
            (profile.lowMidEnergy + profile.highMidEnergy) > 0.50f &&
            zcr in 0.03f..0.35f &&
            profile.spectralCentroidHz in 400.0f..4000.0f
        ) {
            val confidence = ((profile.lowMidEnergy + profile.highMidEnergy) * 0.85f).coerceIn(0.5f, 0.92f)
            val event = AcousticEvent(
                type = AcousticEventType.SPEECH_DETECTED,
                confidence = confidence,
                peakDbfs = rms,
                spectralProfile = profile
            )
            detected.add(event)
        }

        // 5. Check for Silence / Signal Loss
        if (rms < silenceThresholdDbfs) {
            val event = AcousticEvent(
                type = AcousticEventType.SILENCE_OR_DROPOUT,
                confidence = 0.95f,
                peakDbfs = rms,
                spectralProfile = profile
            )
            detected.add(event)
        }

        // Dispatch events
        for (event in detected) {
            _eventFlow.tryEmit(event)
            for (listener in listeners) {
                try {
                    listener.onEventDetected(event)
                } catch (_: Exception) {}
            }
        }

        return detected
    }
}
