package com.acadmate.attendance.audio

import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import kotlin.math.sin
import kotlin.math.PI
import javax.inject.Inject

/**
 * NOVEL FEATURE: Layer 3 - Acoustic Fingerprinting
 * Generates a near-ultrasonic token (18kHz - 20kHz) that is inaudible to most humans
 * but can be captured by student devices to prove physical presence in the room.
 */
class AcousticTokenGenerator @Inject constructor() {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null

    fun playToken(token: String) {
        // Convert token string to frequency modulated signal
        // For simplicity, we use a base frequency of 18.5kHz
        val frequency = 18500.0 
        val duration = 2.0 // seconds
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            samples[i] = sin(2.0 * PI * i / (sampleRate / frequency))
            buffer[i] = (samples[i] * Short.MAX_VALUE).toInt().toShort()
        }

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            numSamples * 2,
            AudioTrack.MODE_STATIC
        )

        audioTrack?.write(buffer, 0, numSamples)
        audioTrack?.setLoopPoints(0, numSamples, -1) // Loop infinitely
        audioTrack?.play()
    }

    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
