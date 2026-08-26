package com.synsound.sdk.core

/**
 * Immutable configuration for SynSound SDK initialization.
 */
data class SynSoundConfig(
    val apiKey: String? = null,
    val environment: SynSoundEnvironment = SynSoundEnvironment.Beta,
    val sampleRate: Int = 16000,
    val frameSize: Int = 1024,
    val enableRealTimeDsp: Boolean = true,
    val enableEventDetection: Boolean = true,
    val enableCloudSync: Boolean = false,
    val enableLogging: Boolean = false,
    val deviceIdentifier: String? = null
) {
    class Builder {
        private var apiKey: String? = null
        private var environment: SynSoundEnvironment = SynSoundEnvironment.Beta
        private var sampleRate: Int = 16000
        private var frameSize: Int = 1024
        private var enableRealTimeDsp: Boolean = true
        private var enableEventDetection: Boolean = true
        private var enableCloudSync: Boolean = false
        private var enableLogging: Boolean = false
        private var deviceIdentifier: String? = null

        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun environment(environment: SynSoundEnvironment) = apply { this.environment = environment }
        fun sampleRate(sampleRate: Int) = apply { this.sampleRate = sampleRate }
        fun frameSize(frameSize: Int) = apply { this.frameSize = frameSize }
        fun enableRealTimeDsp(enable: Boolean) = apply { this.enableRealTimeDsp = enable }
        fun enableEventDetection(enable: Boolean) = apply { this.enableEventDetection = enable }
        fun enableCloudSync(enable: Boolean) = apply { this.enableCloudSync = enable }
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }
        fun deviceIdentifier(id: String) = apply { this.deviceIdentifier = id }

        fun build(): SynSoundConfig = SynSoundConfig(
            apiKey = apiKey,
            environment = environment,
            sampleRate = sampleRate,
            frameSize = frameSize,
            enableRealTimeDsp = enableRealTimeDsp,
            enableEventDetection = enableEventDetection,
            enableCloudSync = enableCloudSync,
            enableLogging = enableLogging,
            deviceIdentifier = deviceIdentifier
        )
    }
}
