# 🎧 SYNSOUND
## INFRASONIC AUDIO INTELLIGENCE SUITE

**Hear More. See More. Know More.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Android%20Language-Kotlin%202.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue.svg)](https://developer.android.com/)
[![PWA](https://img.shields.io/badge/Web-PWA-5A0FC8.svg?logo=pwa&logoColor=white)](https://web.dev/progressive-web-apps/)
[![Base44](https://img.shields.io/badge/Built%20With-Base44-111111.svg)](https://base44.com/)
[![Web Audio](https://img.shields.io/badge/Audio-Web%20Audio%20API-orange.svg)](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API)
[![AudioWorklet](https://img.shields.io/badge/Real--Time-AudioWorklet-yellow.svg)](https://developer.mozilla.org/en-US/docs/Web/API/AudioWorklet)
[![F-Droid](https://img.shields.io/f-droid/v/com.synsound.app.svg?logo=f-droid)](https://f-droid.org/packages/com.synsound.app/)
[![Build](https://github.com/Neurosyn-Dev/SYNSOUND/actions/workflows/build.yml/badge.svg)](https://github.com/Neurosyn-Dev/SYNSOUND/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.synsound.app/)

SYNSOUND is a professional real-time acoustic intelligence workstation designed for Android and modern web browsers. It combines continuous audio capture, live DSP, audio enhancement, speech intelligence, recording, forensic workflows, spectral analysis, visualization, mastering, AI-assisted analysis, environmental monitoring, and infrasonic/ultrasonic investigation in one unified system.

SYNSOUND is a hybrid platform: the web/PWA application is developed through Base44 and modern browser audio technologies, while the Android application provides a dedicated native environment and Kotlin-based Android integration.

---

# Table of Contents

- [What Is SYNSOUND?](#what-is-synsound)
- [Core Capabilities](#core-capabilities)
- [Architecture](#architecture)
- [Persistent Audio Architecture](#persistent-audio-architecture)
- [Audio Session & Device Management](#audio-session--device-management)
- [Audio Format Normalization](#audio-format-normalization)
- [Buffer Management](#buffer-management)
- [Priority Processing](#priority-processing)
- [Pipeline Isolation](#pipeline-isolation)
- [Modular Audio Graph](#modular-audio-graph)
- [DSP Pipeline](#dsp-pipeline)
- [Parametric EQ](#parametric-eq)
- [Dynamic EQ](#dynamic-eq)
- [Graphic EQ](#graphic-eq)
- [Frequency Targeting](#frequency-targeting)
- [Mastering](#mastering)
- [Speech Enhancement & Forensic Audio](#speech-enhancement--forensic-audio)
- [AI Speech Recovery Engine](#ai-speech-recovery-engine)
- [AI Learning & Adaptation](#ai-learning--adaptation)
- [Acoustic Analysis](#acoustic-analysis)
- [Infrasonic & Ultrasonic Analysis](#infrasonic--ultrasonic-analysis)
- [Acoustic Event Detection](#acoustic-event-detection)
- [SYN-ENGINE](#syn-engine)
- [Speech Engine Manager](#speech-engine-manager)
- [Live Transcription](#live-transcription)
- [Transcription Context & Vocabulary](#transcription-context--vocabulary)
- [Speech Models](#speech-models)
- [Recording](#recording)
- [Import & Playback](#import--playback)
- [Monitoring Dashboard](#monitoring-dashboard)
- [Workspaces](#workspaces)
- [Presets](#presets)
- [Projects](#projects)
- [Global Search](#global-search)
- [Diagnostics & Developer Tools](#diagnostics--developer-tools)
- [Android Application](#android-application)
- [Android WebView](#android-webview)
- [Security & Privacy](#security--privacy)
- [Accessibility](#accessibility)
- [Technology Stack](#technology-stack)
- [Project Specifications](#project-specifications)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Development Philosophy](#development-philosophy)
- [Reliability & Regression Requirements](#reliability--regression-requirements)
- [Performance](#performance)
- [Development Status](#development-status)
- [Roadmap](#roadmap)
- [Links](#links)
- [License](#license)

---

# What Is SYNSOUND?

SYNSOUND turns incoming sound into information through four primary goals:

**Capture → Analysis → Enhancement → Understanding**

Unlike a conventional audio editor, SYNSOUND is designed around continuous live processing. Recording, monitoring, DSP, visualization, speech recognition, event detection, and analysis can operate simultaneously without requiring the user to constantly switch modes.

The platform is intended for:

- Real-time acoustic and environmental monitoring
- Audio engineering
- Field recording
- Speech enhancement
- Audio restoration
- Forensic audio review
- Acoustic research
- Environmental sound analysis
- Audio experimentation
- Portable/mobile monitoring
- Professional and educational audio workflows

The project is designed to evolve into a scalable professional acoustic intelligence workstation while preserving the stable core audio engine.

---

# Core Capabilities

SYNSOUND includes or is designed to support:

- Persistent microphone monitoring
- Real-time recording
- Raw and processed recording paths
- Live headphone monitoring
- Gain control
- Noise reduction
- Speech isolation
- Speech enhancement
- Dynamic range processing
- Graphic EQ
- Parametric EQ
- Dynamic EQ
- Frequency targeting
- Mastering
- Multiband processing
- Limiting
- True-peak limiting where supported
- Maximization
- Transient shaping
- De-essing
- Harmonic enhancement
- Spectral shaping
- Stereo imaging
- Mid/Side processing where appropriate
- FFT spectrum analysis
- Spectrograms
- Waveform visualization
- Oscilloscope visualization
- Frequency tracking
- Frequency activity monitoring
- Signal-quality monitoring
- Infrasonic analysis
- Ultrasonic analysis
- Acoustic event detection
- Environmental analysis
- Speech-to-text
- Live transcription
- Offline-capable speech processing
- SYN-ENGINE
- AI-assisted enhancement
- AI-assisted analysis
- Forensic audio workflows
- Presets
- Projects
- Automation
- Diagnostics
- Performance monitoring
- Modular plugin-ready architecture

---

# Architecture

SYNSOUND follows a modular architecture based on separation of concerns, real-time performance, non-destructive processing, predictable state management, extensibility, and platform independence where practical.

High-level architecture:

```text
                         USER INTERFACE
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Application         │
                    │ Controller          │
                    └──────────┬──────────┘
                               │
             ┌─────────────────┼─────────────────┐
             ▼                 ▼                 ▼
       ┌───────────┐     ┌───────────┐     ┌───────────┐
       │ Audio     │     │ DSP       │     │ AI        │
       │ Engine    │     │ Engine    │     │ Engine    │
       └─────┬─────┘     └─────┬─────┘     └─────┬─────┘
             └─────────────────┼─────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │ Event Bus / Router  │
                    └──────────┬──────────┘
                               │
             ┌─────────────────┼─────────────────┐
             ▼                 ▼                 ▼
       ┌───────────┐     ┌────────────┐    ┌────────────┐
       │ Recording │     │Visualization│    │ Storage    │
       └───────────┘     └────────────┘    └─────┬──────┘
                                                  ▼
                                          ┌────────────┐
                                          │ Export     │
                                          │ Services   │
                                          └────────────┘
```

The Event Bus/Router minimizes direct dependencies between modules. Individual subsystems should not become responsible for unrelated functionality.

---

# Persistent Audio Architecture

The permanent foundation of SYNSOUND is a centralized audio architecture consisting of:

- Audio Input Manager
- Audio Bus
- Audio Session Manager
- Audio Device Manager
- Audio normalization
- Modular processing graph
- Shared timestamps
- Priority scheduling
- Pipeline isolation

## Audio Input Manager

The Audio Input Manager is the single source of truth for live microphone audio.

The microphone should be opened once during normal operation.

No recorder, analyzer, visualization, speech engine, AI component, DSP module, or plugin should independently access the microphone.

## Audio Bus

The Audio Bus continuously distributes synchronized audio frames to enabled consumers.

Potential consumers include:

- Speech recognition
- SYN-ENGINE
- Recording
- Live monitoring
- DSP
- Parametric EQ
- Dynamic EQ
- Graphic EQ
- Gain
- Dynamics
- Noise reduction
- Speech isolation
- Speech enhancement
- Spectrum analyzer
- Spectrogram
- Oscilloscope
- AI analysis
- Event detection
- Infrasonic analysis
- Ultrasonic analysis
- Future plugins

Every enabled subsystem receives audio from the shared source.

## Synchronized Timestamps

Audio frames should carry synchronized timestamps so that:

- Transcripts
- Recordings
- Waveforms
- Spectrograms
- Spectrum analysis
- AI detections
- Events
- Future editing tools

remain synchronized.

---

# Audio Session & Device Management

## Audio Session Manager

The Audio Session Manager controls the lifecycle of audio processing.

Responsibilities include:

- Initialize audio
- Request microphone permissions
- Manage active sessions
- Detect microphone changes
- Handle device switching
- Recover from interruptions
- Safely shut down audio processes
- Handle sleep/wake transitions

It should gracefully handle:

- Built-in microphones
- Bluetooth microphones
- Wired headsets
- USB microphones
- Permission changes
- Device interruptions

## Audio Device Manager

The Audio Device Manager should expose:

- Device name
- Input source
- Sample rate
- Channel count
- Availability
- Active device

Input devices should be switchable without unnecessarily restarting SYNSOUND.

The architecture is prepared for future:

- Multiple microphones
- Stereo inputs
- External sensors
- Audio interfaces

---

# Audio Format Normalization

Incoming audio passes through a common normalization layer before downstream processing.

The normalization layer handles:

- Sample-rate conversion
- Channel conversion
- Bit-depth conversion
- Buffer normalization
- Audio-frame timing

This gives downstream DSP and speech systems a consistent internal representation.

---

# Buffer Management

SYNSOUND is designed to balance low latency with stable processing.

Dynamic buffer management should help prevent:

- Audio crackling
- Dropouts
- Buffer underruns
- Delayed transcription
- Desynchronization

Buffer behavior may adapt to device performance while protecting continuous monitoring and recording.

---

# Priority Processing

Latency-sensitive operations receive priority.

Highest-priority operations include:

- Live monitoring
- Speech recognition
- Recording

Less time-sensitive work can execute asynchronously where practical:

- AI analysis
- Advanced feature extraction
- Future machine learning

Heavy processing must not interrupt live monitoring or recording.

---

# Pipeline Isolation

SYNSOUND modules remain independent.

A failure in one subsystem must not unnecessarily disable unrelated systems.

For example:

If speech recognition fails:

- Recording continues.
- DSP continues.
- Visualization continues.
- Audio monitoring continues.
- Another speech engine may be selected.

If AI analysis fails:

- Recording continues.
- DSP continues.
- Speech recognition continues.

The system should degrade gracefully and provide useful diagnostics.

---

# Modular Audio Graph

Processing modules can be dynamically enabled or disabled.

Disabled modules should release unnecessary CPU and memory resources.

New modules should integrate through the Audio Bus and processing graph instead of creating new microphone streams.

This architecture is intended to remain scalable as SYNSOUND grows.

---

# DSP Pipeline

The primary processing concept is:

```text
Input Gain
   ↓
Noise Reduction
   ↓
Speech Isolation
   ↓
Enhancement
   ↓
Dynamics
   ↓
EQ
   ↓
Master
```

DSP modules should remain modular, independently bypassable, and compatible with the centralized audio architecture.

Processing should be non-destructive wherever practical.

---

# Parametric EQ

The **Parametric EQ is a core SYNSOUND feature**.

Each band supports:

- Frequency
- Gain
- Q
- Filter type
- Enable/bypass

Filter types include:

- Bell / Peaking
- Low Shelf
- High Shelf
- High-Pass
- Low-Pass
- Notch
- Band-Pass

The interface supports or is designed for:

- Draggable control points
- Numerical controls
- Frequency labels
- Live FFT overlays
- Live spectrum visualization
- Presets
- Undo
- Redo
- A/B comparison
- Reset
- Copy
- Paste
- Import
- Export
- AI-assisted recommendations

The Parametric EQ is integrated into the dedicated Mastering workspace while retaining its functionality for applicable live and imported-audio workflows.

---

# Dynamic EQ

SYNSOUND includes a professional adaptive Dynamic Equalizer architecture.

Dynamic bands can expose:

- Frequency
- Gain
- Q
- Threshold
- Ratio
- Attack
- Release
- Knee
- Makeup gain
- Amount
- Adaptive detection

Additional architecture includes:

- Intelligent automation
- Sidechain-ready design
- Stereo linking where appropriate
- Real-time gain-reduction meters
- Dynamic frequency visualization
- Live FFT overlays
- Automatic resonance suppression
- Adaptive speech enhancement
- AI-assisted optimization

Dynamic EQ should respond to incoming audio without introducing unstable DSP combinations.

---

# Graphic EQ

The Graphic Equalizer supports:

- 10-band mode
- 15-band mode
- 31-band mode
- Adjustable gain
- Reset
- Copy
- Paste
- Import
- Export
- Preset management
- Intelligent recommendations
- FFT overlays
- Live visualization
- Smoothing
- Optional high-resolution mode

---

# Frequency Targeting

SYNSOUND includes a dedicated Frequency Targeting concept for focused frequency investigation and processing.

Frequency Targeting works alongside:

- Parametric EQ
- Dynamic EQ
- Graphic EQ
- FFT analysis
- Spectrogram analysis
- Speech enhancement
- Forensic workflows

The goal is to allow users to identify and work on meaningful frequency regions without losing access to the broader live analysis system.

---

# Mastering

SYNSOUND provides a dedicated Mastering workspace with an original professional workflow influenced by modern mastering environments while tailored to acoustic intelligence and speech enhancement.

Modules include:

- Input trim
- Output trim
- Parametric EQ
- Dynamic EQ
- Graphic EQ
- Compressor
- Multiband compressor
- Adaptive compressor
- Expander
- Gate
- Limiter
- True-peak limiter where supported
- Maximizer
- Transient shaping
- De-esser
- Harmonic enhancement
- Harmonic exciter
- Spectral shaping
- Adaptive tonal balancing
- Loudness optimization
- Match-style spectral comparison
- Stereo imaging
- Stereo width
- Mid/Side processing where appropriate
- Dynamic-range optimization
- Clipping prevention
- Crest-factor monitoring
- Output-ceiling management
- Phase analysis
- Stereo-correlation analysis
- LUFS
- RMS
- Peak metering
- Frequency-balance analysis
- Harmonic-balance analysis
- Signal-integrity monitoring
- Noise-floor analysis
- Clipping analysis
- Transient analysis
- Spectrum comparison
- Intelligent mastering recommendations

Processing modules should support, where applicable:

- Enable
- Disable
- Bypass
- Reset
- Presets
- Undo
- Redo
- A/B comparison
- Intelligent suggestions
- Optional automatic optimization
- Seamless integration with the existing DSP chain

---

# Speech Enhancement & Forensic Audio

SYNSOUND has a major focus on recovering difficult-to-hear speech.

The forensic enhancement workflow is intended for difficult recordings such as:

- Distant voices
- Faint speech
- Muffled conversations
- Whispers
- Speech obscured by environmental sound
- Noisy recordings
- Low-SNR recordings
- Outdoor recordings
- Reverberant indoor recordings
- Radio communications
- Body-worn recordings
- Low-quality microphone recordings

The processing chain can combine:

- Noise reduction
- Speech isolation
- Gain optimization
- Dynamic processing
- EQ
- Frequency targeting
- Spectral shaping
- De-essing
- AI-assisted enhancement
- Visualization
- Transcription

Processing should improve intelligibility without falsely presenting reconstructed information as original recorded content.

---

# AI Speech Recovery Engine

The AI Speech Recovery Engine focuses specifically on difficult-to-hear speech.

Its intended applications include:

- Recovering faint speech
- Improving distant speech
- Improving muffled speech
- Reducing environmental masking
- Improving low-SNR recordings
- Improving reverberant speech
- Assisting speech recognition
- Supporting forensic review

AI enhancement should be conservative and gradual. The system should not invent speech or present generated content as authentic source audio.

---

# AI Learning & Adaptation

The AI system is designed to learn characteristics of the recording environment over time.

Potential learned characteristics include:

- Microphone characteristics
- Room acoustics
- Recurring environmental sounds
- Background-noise profiles
- Speech characteristics
- Microphone distance
- Reverberation
- Spectral masking
- Frequency balance
- Clipping tendencies
- Signal-to-noise ratio
- Dynamic range
- Voice presence
- Speech clarity
- Recording quality
- Long-term listening patterns

The system should adapt gradually rather than making abrupt processing changes.

Auto mode may recommend or safely apply enhancements while avoiding unstable DSP combinations.

---

# Real-Time Audio Quality Monitoring

SYNSOUND monitors incoming audio quality for:

- Signal level
- Noise floor
- Clipping
- Distortion
- Signal-to-noise ratio
- Input saturation
- Silence

The application may warn users when input conditions compromise processing or transcription, for example:

- `Input level too low`
- `Background noise affecting transcription`
- `Microphone clipping detected`

---

# Voice Activity Detection

Advanced VAD distinguishes among:

- Speech
- Silence
- Background noise
- Non-speech events

VAD can reduce unnecessary processing while maintaining real-time responsiveness.

---

# Acoustic Analysis

## Spectrum Analyzer

FFT-based analysis provides:

- Frequency
- Amplitude
- Peak hold
- Freeze
- Zoom
- Cursor inspection
- Frequency tracking
- Live frequency activity

## Spectrogram

The spectrogram provides time-frequency visualization for observing changing acoustic content.

## Waveform

Waveform visualization provides time-domain inspection of live and recorded audio.

## Oscilloscope

The oscilloscope provides detailed waveform inspection for periodicity and signal behavior.

---

# Infrasonic & Ultrasonic Analysis

SYNSOUND specifically supports investigation of acoustic content outside conventional audible listening workflows.

Analysis must always respect:

- Microphone bandwidth
- Audio-interface bandwidth
- Device hardware
- Sample rate
- Nyquist limits
- Recording format

The application must never present unsupported frequencies as genuine measurements.

Ultrasonic analysis is emphasized where scientifically and technically appropriate, while infrasonic analysis remains an important SYNSOUND capability.

---

# Acoustic Event Detection

The acoustic intelligence system can detect and classify meaningful audio characteristics and events.

Potential detections include:

- Speech
- Silence
- Transients
- Tones
- Frequency events
- Environmental sounds
- Future configurable acoustic events

Event detection should remain isolated from recording, monitoring, DSP, and visualization so failures do not cascade.

---

# SYN-ENGINE

**SYN-ENGINE** is SYNSOUND's experimental native speech recognition framework.

Its long-term goal is to become the default SYNSOUND speech-recognition and speech-understanding system.

External open-source engines may serve as recognition backends while SYN-ENGINE develops independently.

The architecture is:

```text
Audio Input Manager
        ↓
Audio Bus
        ↓
Noise Reduction
        ↓
Speech Isolation
        ↓
Automatic Gain Optimization
        ↓
Voice Activity Detection
        ↓
Audio Feature Extraction
        ↓
Recognition Core
        ↓
Language Processing
        ↓
Context Correction
        ↓
Punctuation Restoration
        ↓
Capitalization
        ↓
Confidence Analysis
        ↓
Timestamp Generation
        ↓
Live Transcript Output
```

Each stage remains modular and replaceable.

---

# SYN-ENGINE Future Capabilities

The architecture is designed to support:

- Custom acoustic models
- Custom language models
- Adaptive speech learning
- Dynamic vocabulary learning
- Keyword boosting
- Speaker identification
- Speaker separation
- Wake-word detection
- Multiple-language recognition
- Real-time streaming recognition
- Context-aware correction
- Specialized vocabulary packs
- Environmental sound adaptation
- Confidence-based correction
- Automatic error recovery

---

# Speech Engine Manager

SYNSOUND uses a modular Speech Engine Manager.

Engine targets include:

- `AUTO`
- `SYN-ENGINE` experimental
- `Vosk`
- `Coqui STT`
- `sherpa-onnx`
- `PocketSphinx`
- Browser Web Speech API fallback where available

The intended engine pool prioritizes engines that are:

- Completely free
- Usable indefinitely
- Free from subscriptions
- Free from usage quotas
- Compatible with SYNSOUND

Paid APIs, subscription-only services, trials, credit-based services, and services that stop working after usage limits are not part of the intended local speech-engine architecture.

---

# AUTO Speech Engine

AUTO mode selects an appropriate available engine based on:

- Device performance
- CPU
- RAM
- Browser compatibility
- Installed models
- Offline availability
- Language
- Estimated latency

AUTO should dynamically select the best available engine without unnecessary user intervention.

---

# Speech Engine Plugin Interface

Every engine should expose a consistent interface:

```text
Initialize()
LoadModel()
UnloadModel()
Start()
Stop()
Pause()
Resume()
Transcribe()
GetConfidence()
GetStatus()
GetSupportedLanguages()
GetInstalledModels()
```

This allows compatible engines to be swapped without redesigning the rest of SYNSOUND.

---

# Engine Status & Recovery

Speech engines should expose meaningful states such as:

```text
Loading
Initializing
Ready
Listening
Processing
Offline
Model Missing
Downloading
Failed
Recovering
```

If an engine fails, SYNSOUND should attempt an appropriate fallback when another compatible engine is available.

---

# Live Transcription

SYNSOUND is designed for continuous streaming transcription.

The transcript should update progressively while the user is speaking rather than requiring long pauses.

Features include:

- Real-time word updates
- Sentence formation
- Automatic punctuation
- Capitalization
- Confidence indicators
- Timestamp markers
- Error correction
- Search
- Copy
- Export

---

# Timestamp Synchronization

Every transcript segment should contain:

- Start time
- End time
- Duration
- Associated audio position

These timestamps remain synchronized with:

- Recordings
- Waveforms
- Spectrograms
- Spectrum analysis
- Audio events
- Future editing tools

---

# Transcription Context & Vocabulary

## Context Memory

Optional short-term context awareness improves:

- Punctuation
- Sentence formation
- Vocabulary awareness
- Nearby-word relationships
- Transcription correction

This should not require cloud AI.

## Custom Vocabulary

Users can add:

- Names
- Technical terms
- Music terminology
- Scientific terminology
- Project-specific terms
- Custom commands

SYN-ENGINE can prioritize these terms during recognition.

---

# Speech Confidence

Recognized segments should internally calculate confidence.

Confidence information can be displayed where appropriate and used to identify low-confidence transcript sections for review.

---

# Speech Models

Model management supports:

- Installed models
- Available models
- Language
- Version
- Model size
- RAM requirements
- Estimated accuracy
- Status

Where supported, users can:

- Download models
- Remove models
- Update models
- Verify model integrity
- Detect missing models
- Switch models at runtime
- Manage multiple languages

Local speech recognition should operate offline whenever the selected engine supports it.

---

# Recording

Recording controls remain available while live analysis continues.

Recording features include:

- Start
- Stop
- Pause
- Resume
- Recording timer
- Raw recording
- Processed recording
- Waveform
- Peak/RMS meters
- Clipping detection
- Markers
- Notes
- Rolling buffer
- Pre-record buffer
- Recording metadata
- Timestamp synchronization
- Export
- Recording management

Recording must not disable live DSP, visualization, monitoring, or analysis.

---

# Import & Playback

Supported formats may include:

- WAV
- MP3
- FLAC
- M4A
- AAC
- Other browser/device-supported formats

Playback supports:

- Play
- Pause
- Stop
- Seek
- Speed control
- Loop
- A/B playback

Imported tracks can be analyzed, processed, mastered, and transcribed where supported.

---

# Monitoring Dashboard

The primary workspace is a dedicated real-time monitoring dashboard centered around live analysis rather than settings.

It can display:

- Spectrogram
- Spectrum analyzer
- Waveform
- Frequency activity
- Recording status
- Monitoring status
- AI processing status
- Speech detection status
- Event timeline
- Live transcript
- Signal quality
- Microphone status
- Processing latency
- Sample rate
- Active preset
- Active workspace
- CPU usage
- Memory usage
- Other live diagnostics

Users can customize visible widgets, resize panels, rearrange monitoring components, and save workspace layouts.

The design principle is:

> Everything important should remain visible while recording.

---

# Workspaces

Expandable workspaces include:

- Audio
- Enhancement
- DSP
- Equalizers
- Mastering
- Voice Processing
- Speech Intelligence
- Recording
- Transcription
- Analysis
- Diagnostics
- Presets
- Projects
- Automation
- AI
- Settings

Workspace behavior includes:

- Remember expanded/collapsed state
- Instant searching
- Pin favorites
- Responsive controls
- No interruption to recording
- No interruption to audio processing

Workspace profiles include:

- Monitoring
- Analysis
- Mastering
- Forensics
- Transcription
- Recording
- Minimal
- Custom

---

# Presets

Presets store reusable processing chains and configurations.

Example presets include:

- Podcast
- Interview
- Environmental Monitoring
- Speech Recovery
- Forensic Review
- Music Monitoring

Presets can cover DSP, recording, visualization, AI, theme, and layout configuration where applicable.

---

# Projects

Projects can contain:

- Recordings
- Imported audio
- Processed audio
- Transcripts
- Markers
- Notes
- DSP configurations
- EQ presets
- Sessions
- Analysis information

Original source audio should remain protected whenever practical.

---

# Global Search

Global Search is intended to search:

- Recordings
- Notes
- Markers
- Profiles
- Projects
- Settings
- Future AI events

The goal is to make large SYNSOUND projects easier to navigate without interrupting active audio workflows.

---

# Diagnostics & Developer Tools

Developer Mode provides advanced diagnostics including:

- Performance Monitor
- Buffer Inspector
- Latency Graph
- Logging
- Debug Overlay
- Experimental Features

Audio diagnostics can include:

- Active engine
- Engine version
- Model
- Model version
- Language
- Audio status
- Audio duration
- Sample rate
- Format
- Processing latency
- Confidence
- Failed processing stage
- Error details
- Recovery attempts

Pipeline diagnostics can include:

- Microphone availability
- Input device
- Stream health
- Frame timing
- Buffer status
- Dropped frames
- Processing delay
- CPU usage
- Memory usage
- Module failures

---

# Performance Benchmarking

Speech engines can be benchmarked for:

- Accuracy
- Latency
- Processing speed
- CPU usage
- RAM usage
- Confidence
- Words per second
- Real-time processing factor
- Stability

SYNSOUND's broader performance objectives are:

- Real-time operation
- Low latency
- Efficient memory usage
- Minimal CPU overhead
- Stable long-duration sessions
- Continuous operation

---

# Android Application

The Android application provides a dedicated native environment for SYNSOUND.

It is an Android application wrapper around the SYNSOUND web/PWA experience while allowing native Android capabilities where necessary.

It provides:

- Dedicated application identity
- Browser-free application experience
- Fullscreen presentation
- Android microphone permissions
- Web Audio integration
- Native file selection
- File downloads
- Authentication/session persistence
- Android lifecycle handling
- Native Back navigation
- Network recovery
- WebView crash recovery
- Secure WebView configuration
- Adaptive launcher resources

The Android layer is intentionally lightweight.

**SYNSOUND Web/PWA = Core application**

**SYNSOUND Android = Native Android environment**

---

# Android WebView

The Android WebView is configured for modern web applications with support for:

- JavaScript
- DOM storage
- Local/session storage
- Cookies
- Hardware acceleration
- Web Audio
- Modern web APIs
- Microphone media permissions

Where browser technology cannot reliably provide required low-latency native audio processing, SYNSOUND can use an appropriate native integration, plugin, Web Audio, AudioWorklet, or bridge.

---

# Android Specifications

| Specification | Value |
|---|---|
| Application Name | `SynSound` |
| Package ID | `com.synsound.app` |
| Target URL | `https://synsound-beta.base44.app` |
| Minimum SDK | `26` / Android 8.0 Oreo |
| Target SDK | `35` / Android 15 |
| Compile SDK | `35` |
| Android Language | Kotlin 2.0 |
| Primary Android Technology | Android WebView |
| Required Permission | `RECORD_AUDIO` |
| Version | `1.0.0` |
| Version Code | `1` |

---

# Android Wrapper Features

The wrapper provides:

- High-performance WebView configuration
- Edge-to-edge layout
- Android window-inset handling
- Gesture/navigation compatibility
- Display-cutout handling
- Web Audio and microphone integration
- Native file uploads
- Native downloads
- Authentication/session persistence
- Connection-error recovery
- Retry functionality
- WebView renderer crash recovery
- Secure WebView configuration
- Android lifecycle integration
- Native Back behavior
- Modern splash-screen support
- Adaptive launcher icons

---

# Security & Privacy

Security principles include:

- HTTPS-only primary communication
- Normal Android/WebView certificate validation
- Cleartext traffic disabled
- Minimal permissions
- Runtime microphone permission handling
- No unnecessary JavaScript bridges
- No unnecessary native APIs exposed to web content
- No hardcoded credentials
- No hardcoded API keys
- No password storage by the wrapper
- No authentication-token logging
- WebView debugging disabled in release builds
- Android application sandboxing

SYNSOUND does not intentionally bypass Android or WebView security controls to make functionality work.

---

# Microphone Privacy

Microphone access is controlled through the standard Android permission system.

Microphone access may be used for:

- Live monitoring
- Recording
- DSP
- Speech recognition
- Visualization
- Acoustic analysis

Permission should be requested when functionality actually requires it rather than unnecessarily at startup.

Users can manage microphone access through Android system settings.

---

# Accessibility

SYNSOUND is designed to support:

- Large text
- High contrast
- Reduced animations
- Touch optimization
- Keyboard navigation on desktop

Accessibility should remain compatible with live monitoring and processing.

---

# Technology Stack

SYNSOUND is a hybrid platform rather than a single-language application.

| Layer | Technology |
|---|---|
| Web Application | Progressive Web App |
| Web Platform | Base44 |
| Browser Audio | Web Audio API |
| Real-Time Web Processing | AudioWorklet where supported |
| Android Application | Native Android |
| Android Language | Kotlin 2.0 |
| Android UI Environment | Android WebView |
| Speech Architecture | Modular Speech Engine Manager |
| Speech Framework | SYN-ENGINE |
| Audio Architecture | Persistent Audio Input Manager + Audio Bus |
| DSP Architecture | Modular Processing Pipeline |
| Application Coordination | Application Controller + Event Bus / Router |

The exact implementation language of individual Base44-generated web components may evolve. Kotlin 2.0 specifically identifies the Android layer.

---

# Project Specifications

| Specification | Value |
|---|---|
| Application | `SynSound` |
| Package ID | `com.synsound.app` |
| Web Application | `https://synsound-beta.base44.app` |
| Platform | Android + modern web browsers |
| Web Architecture | PWA |
| Android Architecture | Native Android wrapper |
| Android Language | Kotlin 2.0 |
| Minimum SDK | 26 |
| Target SDK | 35 |
| Compile SDK | 35 |
| Version | `1.0.0` |
| Version Code | `1` |
| Primary Permission | `RECORD_AUDIO` |
| Android Technology | WebView + native integration |

---

# Project Structure

```text
synsound-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/synsound/app/
│       │   └── MainActivity.kt
│       └── res/
│           ├── drawable/
│           ├── layout/
│           ├── mipmap-*/
│           ├── values/
│           └── xml/
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

The web/PWA portion is maintained through the SYNSOUND/Base44 development environment.

---

# Getting Started

## Requirements

Android development requires:

- Android Studio
- Android SDK 35
- Compatible JDK
- Android SDK Build Tools
- Appropriate Gradle tooling
- Android device or emulator

## Debug Build

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK output:

```text
app/build/outputs/apk/
```

## Release Build

```bash
./gradlew assembleRelease
```

Windows:

```powershell
.\gradlew.bat assembleRelease
```

Release signing credentials must be securely managed and must never be committed to the repository.

---

# Development Philosophy

SYNSOUND follows these principles:

### Real Audio First

All audio measurements, processing, transcription, visualization, and detection must operate on actual audio data.

### No Fake Functionality

The application must never present fake meters, waveforms, transcription, spectral measurements, events, or analysis as real results.

### One Persistent Audio Source

The microphone is owned by the centralized Audio Input Manager and distributed through the Audio Bus.

### Hardware-Aware Analysis

Frequency analysis must respect actual device and sample-rate limitations.

### Non-Destructive Processing

Original recordings should remain protected whenever practical.

### Modular DSP

Processors remain independently controllable and replaceable.

### Offline-First Where Practical

Free and local processing should be preferred when technically feasible.

### Graceful Failure

Individual subsystem failures must not unnecessarily crash unrelated systems.

### Extensibility

Future modules should integrate through existing architecture rather than creating competing pipelines.

### Stability Before Refactoring

The proven persistent microphone, shared audio pipeline, monitoring architecture, recording engine, DSP framework, and processing chain should not be unnecessarily redesigned or replaced.

---

# Reliability & Regression Requirements

SYNSOUND development is cumulative.

Existing functionality should not be removed, simplified, or regressed unless explicitly superseded.

Before significant changes, the project should audit:

- Original specification
- Previous patches
- Feature requests
- Bug fixes
- Existing implementations
- Hidden/disabled functionality
- Placeholder systems
- Inconsistent functionality

The goal is to complete the platform rather than repeatedly rebuild it.

## Regression Validation

Validate:

- Monitoring
- Recording
- Importing
- Exporting
- Transcription
- Parametric EQ
- Dynamic EQ
- Graphic EQ
- DSP modules
- AI enhancement
- Presets
- Diagnostics
- Visualization
- Automation
- Projects
- Workspaces
- UI controls

Every visible:

- Button
- Switch
- Slider
- Dropdown
- Graph
- Analyzer
- Menu
- Preset
- Workspace

should perform its intended function.

## DSP Combination Testing

Every DSP module should be tested:

- Individually
- In combinations
- With every applicable preset
- With `ARM ALL`

Testing should verify:

- Stable routing
- Predictable gain staging
- Acceptable latency
- Correct processing order
- No feedback
- No oscillation
- No clipping
- No dropouts
- No unexpected silence
- No distortion
- No crashes
- No unstable interactions

---

# Performance

SYNSOUND is designed for:

- Real-time operation
- Low latency
- Efficient memory usage
- Minimal CPU overhead
- Stable long-duration sessions
- Continuous operation

Mobile optimization should consider:

- Battery usage
- RAM
- CPU
- Thermal behavior
- Audio stability
- Visualization performance
- Processing latency

Disabled processing modules should release unnecessary resources.

Heavy asynchronous work must not interrupt recording or monitoring.

---

# Current Development Status

**Branch:** Beta  
**Stage:** Active Beta Development

The Beta branch contains the newest architectural improvements and feature development.

Current priorities include:

- Stability
- Performance
- User experience
- DSP expansion
- AI integration
- Documentation
- Advanced equalization
- Speech intelligence
- Forensic enhancement
- SYN-ENGINE
- Mobile optimization

Beta builds may contain incomplete or experimental functionality, changing layouts, rendering issues, and ongoing performance tuning.

---

# Bug Reporting

When reporting a SYNSOUND issue, provide:

- Device model
- Android version
- Browser version
- Sample rate
- Buffer size
- Feature being used
- Reproduction steps
- Expected behavior
- Actual behavior
- Screenshots or recordings where applicable
- Console logs where available

This information improves troubleshooting and future development.

---

# Upgrading Between Beta Releases

Before upgrading:

1. Export important recordings.
2. Save custom presets.
3. Read the latest changelog.
4. Restart the application after updating.
5. Review documentation for newly introduced features.

---

# Roadmap

Potential future development includes:

- Expanded native Android audio processing
- Improved background audio behavior
- Additional Bluetooth support
- USB audio improvements
- Native sharing
- Android deep links
- Additional media integrations
- Device-specific optimization
- Advanced AI analysis
- Expanded forensic workflows
- Additional DSP processors
- Improved infrasonic/ultrasonic visualization
- Expanded SYN-ENGINE
- Additional speech models
- Custom acoustic models
- Custom language models
- Adaptive speech learning
- Plugin architecture
- Preset sharing
- Cloud synchronization where appropriate
- GPU-accelerated visualization
- Desktop version
- Broader Android distribution
- Google Play distribution
- Continued F-Droid development

The architecture is intentionally prepared for future expansion without requiring new modules to redesign the core Audio Input Manager, Audio Bus, processing graph, or SYN-ENGINE framework.

---

# Future Architecture

The long-term architecture is intended to support:

- Multiple simultaneous audio processors
- Modular DSP
- Real-time speech recognition
- Offline-first operation
- Expandable AI systems
- Advanced acoustic analysis
- Custom SYN-ENGINE development
- Long-term scalability
- Future plugins
- Future multi-source audio
- External audio interfaces
- Advanced hardware integrations

All future features should build upon the centralized audio architecture.

---

# Links

**SYNSOUND Web Application:**  
https://synsound-beta.base44.app/

**Base44:**  
https://base44.com/

**Android Project:**  
This repository

**F-Droid:**  
https://f-droid.org/packages/com.synsound.app/

---

# License

**Proprietary**

SYNSOUND and its associated source code, application architecture, DSP systems, audio-processing systems, branding, designs, interfaces, and intellectual property are proprietary unless explicitly stated otherwise in individual files or repository documentation.

Third-party open-source engines and libraries remain subject to their respective licenses.

Unauthorized copying, redistribution, modification, or commercial use of proprietary SYNSOUND components may be restricted.

---

# About SYNSOUND

**SYNSOUND — INFRASONIC AUDIO INTELLIGENCE SUITE**

SYNSOUND is designed to turn sound into information.

From persistent microphone capture and real-time DSP to Parametric EQ, Dynamic EQ, Graphic EQ, Frequency Targeting, mastering, FFT analysis, spectrograms, transcription, acoustic event detection, forensic speech recovery, AI-assisted enhancement, environmental analysis, and infrasonic/ultrasonic investigation, SYNSOUND brings multiple audio-intelligence workflows into one unified workstation.

**Capture. Analyze. Enhance. Understand.**

**Hear More. See More. Know More.**
