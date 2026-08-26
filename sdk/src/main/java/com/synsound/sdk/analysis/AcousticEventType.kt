package com.synsound.sdk.analysis

/**
 * Types of acoustic events detected by the SynSound Acoustic Event Classifier.
 */
enum class AcousticEventType(val displayName: String, val description: String) {
    SPEECH_DETECTED("Speech Activity", "Human vocalization or conversational speech detected"),
    LOUD_TRANSIENT("Loud Transient / Impact", "Sharp acoustic spike such as a door slam, gunshot, or impact"),
    HIGH_PITCH_ALARM("Alarm / Siren", "High-frequency tone or oscillating siren pattern detected"),
    LOW_FREQUENCY_RUMBLE("Low-Frequency Rumble", "Continuous low-frequency vibration, motor, or heavy machinery"),
    SILENCE_OR_DROPOUT("Silence / Signal Loss", "Audio signal dropped below baseline noise floor"),
    ANOMALOUS_ACOUSTIC_ACTIVITY("Acoustic Anomaly", "Uncharacteristic energy pattern deviating from ambient baseline")
}
