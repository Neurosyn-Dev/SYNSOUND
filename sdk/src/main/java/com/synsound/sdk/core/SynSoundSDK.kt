package com.synsound.sdk.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import com.synsound.sdk.analysis.AcousticAnalyzer
import com.synsound.sdk.analysis.AcousticEvent
import com.synsound.sdk.analysis.AcousticEventDetector
import com.synsound.sdk.audio.AudioCaptureEngine
import com.synsound.sdk.audio.AudioConfig
import com.synsound.sdk.audio.AudioFrame
import com.synsound.sdk.cloud.SynSoundClient
import com.synsound.sdk.dsp.AudioEnhancementPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Main entry point for the SynSound Acoustic Intelligence SDK.
 */
class SynSoundSDK private constructor(
    val context: Context,
    val config: SynSoundConfig
) {

    val audioCaptureEngine: AudioCaptureEngine = AudioCaptureEngine(
        context = context,
        config = AudioConfig(
            sampleRate = config.sampleRate,
            frameSizeInSamples = config.frameSize
        )
    )

    val dspPipeline: AudioEnhancementPipeline = AudioEnhancementPipeline().apply {
        if (config.enableRealTimeDsp) {
            applyPreset(AudioEnhancementPipeline.Preset.FORENSIC_SPEECH_ENHANCEMENT)
        }
    }

    val acousticAnalyzer: AcousticAnalyzer = AcousticAnalyzer()

    val eventDetector: AcousticEventDetector = AcousticEventDetector(acousticAnalyzer)

    val cloudClient: SynSoundClient = SynSoundClient(config)

    private val sdkScope = CoroutineScope(Dispatchers.Default)
    private var processingJob: Job? = null

    private val _enhancedFrameFlow = MutableSharedFlow<AudioFrame>(extraBufferCapacity = 64)
    val enhancedFrameFlow: SharedFlow<AudioFrame> = _enhancedFrameFlow.asSharedFlow()

    val eventFlow: SharedFlow<AcousticEvent> = eventDetector.eventFlow

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startAcousticMonitoring(): SynSoundResult<Unit> {
        val result = audioCaptureEngine.start()
        if (result.isFailure) return result

        processingJob?.cancel()
        processingJob = audioCaptureEngine.audioFrameFlow
            .onEach { rawFrame ->
                // 1. Apply DSP enhancement pipeline
                val enhancedFrame = if (config.enableRealTimeDsp) {
                    dspPipeline.process(rawFrame)
                } else {
                    rawFrame
                }

                _enhancedFrameFlow.tryEmit(enhancedFrame)

                // 2. Perform acoustic event analysis
                if (config.enableEventDetection) {
                    eventDetector.process(enhancedFrame)
                }

                // 3. Optional cloud streaming
                if (config.enableCloudSync) {
                    cloudClient.streamFrame(enhancedFrame)
                }
            }
            .launchIn(sdkScope)

        return SynSoundResult.Success(Unit)
    }

    fun stopAcousticMonitoring() {
        processingJob?.cancel()
        processingJob = null
        audioCaptureEngine.stop()
        dspPipeline.reset()
    }

    companion object {
        const val VERSION = "1.0.0"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SynSoundSDK? = null

        @Synchronized
        fun initialize(context: Context, config: SynSoundConfig = SynSoundConfig()): SynSoundSDK {
            val appCtx = context.applicationContext ?: context
            return instance ?: SynSoundSDK(appCtx, config).also { instance = it }
        }

        fun getInstance(): SynSoundSDK {
            return instance ?: throw IllegalStateException(
                "SynSoundSDK has not been initialized. Call SynSoundSDK.initialize(context, config) first."
            )
        }

        fun isInitialized(): Boolean = instance != null
    }
}
