# SynSound Android SDK

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20\(Android%208.0\)-orange.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20\(Android%2015\)-blue.svg)](https://developer.android.com)

The official **SynSound Android SDK** (`com.synsound.sdk`) provides acoustic intelligence, real-time audio stream capture, forensic DSP enhancement filtering, automated acoustic event classification, cloud streaming telemetry, and embeddable UI components for Android applications.

---

## Key Features

* **Low-Latency Audio Capture Engine**
  Asynchronous, coroutine `Flow`-based real-time capture using Android `AudioRecord` supporting 16kHz, 44.1kHz, and 48kHz 16-bit PCM.
* **Forensic DSP Audio Enhancement Pipeline**
  Modular 2nd-order Biquad Bandpass/Low-pass/High-pass filters, soft-limiting Pre-Amp Gain (+6dB to +18dB), Forensic Noise Gate, and 5-Band Speech Intelligibility Equalizer.
* **Acoustic Intelligence & Feature Extraction**
  Real-time Radix-2 Cooley-Tukey FFT, RMS energy, peak amplitude, Zero-Crossing Rate (ZCR), Spectral Centroid, Spectral Rolloff, and 5-band energy distribution.
* **Automated Acoustic Event Classifier**
  Edge detection for speech activity, high-pitch sirens/alarms, loud transient impacts (e.g. gunshots/door slams), low-frequency rumble, and audio dropouts.
* **Cloud REST & WebSocket Streaming Client**
  Device authentication, forensic audio snapshot uploads, and full-duplex binary audio and JSON telemetry streaming over WebSockets.
* **Embeddable `SynSoundView` & `SynSoundVisualizerView`**
  Drop-in custom views for embedding the full SynSound web platform with Web Audio permissions and real-time waveform/spectrogram rendering.
* **Headless Background Monitoring Service**
  Android Foreground Service integration for 24/7 continuous acoustic intelligence monitoring with microphone service type and notifications.

---

## Installation

### Gradle (Kotlin DSL)

Add the project dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sdk"))
    // Or when published as an AAR / Maven artifact:
    // implementation("com.synsound:sdk:1.0.0")
}
```

---

## Quick Start

### 1. Initialize the SDK

Initialize `SynSoundSDK` in your `Application` or `MainActivity`:

```kotlin
import com.synsound.sdk.core.SynSoundConfig
import com.synsound.sdk.core.SynSoundEnvironment
import com.synsound.sdk.core.SynSoundSDK

val config = SynSoundConfig.Builder()
    .apiKey("YOUR_SYNSOUND_API_KEY")
    .environment(SynSoundEnvironment.Beta)
    .sampleRate(16000)
    .enableRealTimeDsp(true)
    .enableEventDetection(true)
    .build()

val sdk = SynSoundSDK.initialize(context, config)
```

### 2. Start Acoustic Monitoring

```kotlin
// Ensure Manifest.permission.RECORD_AUDIO is granted
lifecycleScope.launch {
    sdk.startAcousticMonitoring()

    // Listen to enhanced audio frames
    sdk.enhancedFrameFlow.collect { frame ->
        val rmsDbfs = frame.rmsDbfs
        val peak = frame.peakAmplitude
        visualizerView.updateAudioFrame(frame)
    }
}

// Listen to real-time detected acoustic events
lifecycleScope.launch {
    sdk.eventFlow.collect { event ->
        when (event.type) {
            AcousticEventType.SPEECH_DETECTED -> Log.d("SynSound", "Speech: ${event.confidence}")
            AcousticEventType.HIGH_PITCH_ALARM -> Log.w("SynSound", "Alarm detected!")
            AcousticEventType.LOUD_TRANSIENT -> Log.w("SynSound", "Impact/Gunshot detected!")
            else -> {}
        }
    }
}
```

---

## DSP Enhancement Pipeline

Configure custom DSP filters or apply forensic presets:

```kotlin
val pipeline = sdk.dspPipeline

// Apply preset
pipeline.applyPreset(AudioEnhancementPipeline.Preset.FORENSIC_SPEECH_ENHANCEMENT)

// Or add custom filters
pipeline.addFilter(BandpassFilter(
    type = BandpassFilter.FilterType.BAND_PASS,
    centerFrequencyHz = 1600f,
    qFactor = 0.707f
))
pipeline.addFilter(NoiseGateFilter(thresholdDbfs = -42f, floorDb = -40f))
pipeline.addFilter(GainFilter(gainDb = 6f))
```

---

## Real-Time Audio Visualizer

Add `SynSoundVisualizerView` directly in XML or Kotlin:

```xml
<com.synsound.sdk.ui.SynSoundVisualizerView
    android:id="@+id/visualizerView"
    android:layout_width="match_parent"
    android:layout_height="180dp" />
```

```kotlin
visualizerView.mode = SynSoundVisualizerView.VisualizationMode.WAVEFORM
visualizerView.updateAudioFrame(frame)
```

---

## Embeddable `SynSoundView`

Add the full SynSound web platform view directly in XML:

```xml
<com.synsound.sdk.ui.SynSoundView
    android:id="@+id/synSoundView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
synSoundView.load("https://synsound-beta.base44.app")
```

---

## Headless 24/7 Monitoring Service

Start or stop continuous background acoustic intelligence monitoring:

```kotlin
import com.synsound.sdk.service.SynSoundMonitoringService

// Start foreground monitoring service
SynSoundMonitoringService.start(context)

// Stop foreground monitoring service
SynSoundMonitoringService.stop(context)
```

---

## Testing

Run the SDK test suite:

```powershell
.\gradlew.bat :sdk:test
```
