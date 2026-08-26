package com.synsound.sdk.dsp

import com.synsound.sdk.audio.AudioFrame

/**
 * Common contract for DSP audio filters in the SynSound enhancement pipeline.
 */
interface AudioFilter {
    val name: String
    var isEnabled: Boolean

    /**
     * Processes an audio frame and returns a modified [AudioFrame].
     */
    fun process(frame: AudioFrame): AudioFrame

    /**
     * Resets internal filter delay states / history buffers.
     */
    fun reset() {}
}
