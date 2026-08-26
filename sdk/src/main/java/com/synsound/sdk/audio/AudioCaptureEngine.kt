package com.synsound.sdk.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.synsound.sdk.core.SynSoundResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance, low-latency audio capture engine utilizing Android [AudioRecord]
 * with Kotlin Coroutines Flow streaming.
 */
class AudioCaptureEngine(
    private val context: Context,
    private val config: AudioConfig = AudioConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun interface AudioFrameListener {
        fun onAudioFrame(frame: AudioFrame)
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(dispatcher)

    private val _state = MutableStateFlow(AudioRecorderState.UNINITIALIZED)
    val state: StateFlow<AudioRecorderState> = _state.asStateFlow()

    private val _audioFrameFlow = MutableSharedFlow<AudioFrame>(extraBufferCapacity = 64)
    val audioFrameFlow: SharedFlow<AudioFrame> = _audioFrameFlow.asSharedFlow()

    private val listeners = mutableListOf<AudioFrameListener>()
    private val isPaused = AtomicBoolean(false)
    private val frameIndexSequence = AtomicLong(0)

    val isRecording: Boolean
        get() = _state.value == AudioRecorderState.RECORDING

    init {
        _state.value = AudioRecorderState.READY
    }

    fun addListener(listener: AudioFrameListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: AudioFrameListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Synchronized
    fun start(): SynSoundResult<Unit> {
        if (!hasRecordPermission()) {
            _state.value = AudioRecorderState.ERROR
            return SynSoundResult.Failure(
                SecurityException("android.permission.RECORD_AUDIO is not granted")
            )
        }

        if (_state.value == AudioRecorderState.RECORDING) {
            return SynSoundResult.Success(Unit)
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                config.sampleRate,
                config.channelConfig,
                config.audioFormat
            )

            if (minBufferSize <= 0) {
                _state.value = AudioRecorderState.ERROR
                return SynSoundResult.Failure(
                    IllegalStateException("Unsupported audio hardware configuration for sample rate ${config.sampleRate}")
                )
            }

            val bufferSize = (minBufferSize * config.bufferMultiplier).coerceAtLeast(
                config.frameSizeInSamples * config.channelCount * config.bytesPerSample
            )

            audioRecord = AudioRecord(
                config.audioSource,
                config.sampleRate,
                config.channelConfig,
                config.audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                _state.value = AudioRecorderState.ERROR
                return SynSoundResult.Failure(
                    IllegalStateException("Failed to initialize native AudioRecord")
                )
            }

            audioRecord?.startRecording()
            _state.value = AudioRecorderState.RECORDING
            isPaused.set(false)

            startCaptureLoop(bufferSize)
            return SynSoundResult.Success(Unit)
        } catch (e: Exception) {
            _state.value = AudioRecorderState.ERROR
            return SynSoundResult.Failure(e)
        }
    }

    private fun startCaptureLoop(bufferSize: Int) {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val chunkBytes = config.frameSizeInSamples * config.channelCount * config.bytesPerSample
            val readBuffer = ByteArray(chunkBytes)

            while (isActive && _state.value == AudioRecorderState.RECORDING) {
                if (isPaused.get()) {
                    kotlinx.coroutines.delay(10)
                    continue
                }

                val record = audioRecord ?: break
                val bytesRead = record.read(readBuffer, 0, readBuffer.size)

                if (bytesRead > 0) {
                    val frameBytes = if (bytesRead == readBuffer.size) {
                        readBuffer.clone()
                    } else {
                        readBuffer.copyOf(bytesRead)
                    }

                    val frame = AudioFrame(
                        pcmData = frameBytes,
                        sampleRate = config.sampleRate,
                        channelCount = config.channelCount,
                        timestampMs = System.currentTimeMillis(),
                        frameIndex = frameIndexSequence.incrementAndGet()
                    )

                    _audioFrameFlow.tryEmit(frame)

                    synchronized(listeners) {
                        for (listener in listeners) {
                            try {
                                listener.onAudioFrame(frame)
                            } catch (_: Exception) {}
                        }
                    }
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    _state.value = AudioRecorderState.ERROR
                    break
                }
            }
        }
    }

    @Synchronized
    fun pause() {
        if (_state.value == AudioRecorderState.RECORDING) {
            isPaused.set(true)
            _state.value = AudioRecorderState.PAUSED
        }
    }

    @Synchronized
    fun resume() {
        if (_state.value == AudioRecorderState.PAUSED) {
            isPaused.set(false)
            _state.value = AudioRecorderState.RECORDING
        }
    }

    @Synchronized
    fun stop() {
        try {
            _state.value = AudioRecorderState.STOPPED
            recordingJob?.cancel()
            recordingJob = null

            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            audioRecord = null
            _state.value = AudioRecorderState.READY
        } catch (_: Exception) {
            _state.value = AudioRecorderState.ERROR
        }
    }

    fun release() {
        stop()
        synchronized(listeners) {
            listeners.clear()
        }
    }
}
