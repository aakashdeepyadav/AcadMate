package com.acadmate.attendance.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

/**
 * NOVEL FEATURE: Layer 3 - Acoustic Receiver
 * Listens for near-ultrasonic frequencies (18.5kHz) to verify proximity.
 */
class AcousticTokenReceiver @Inject constructor() {
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    @SuppressLint("MissingPermission")
    suspend fun listenForToken(timeoutMs: Long = 5000): Boolean = withContext(Dispatchers.IO) {
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val buffer = ShortArray(bufferSize)
        audioRecord.startRecording()

        val startTime = System.currentTimeMillis()
        var tokenDetected = false

        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    // Very basic frequency detection logic for demo
                    // In a real app, use FFT (Fast Fourier Transform) to detect 18.5kHz spike
                    if (detectFrequencySpike(buffer, read)) {
                        tokenDetected = true
                        break
                    }
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }

        tokenDetected
    }

    private fun detectFrequencySpike(buffer: ShortArray, length: Int): Boolean {
        if (length <= 0) return false
        
        var zeroCrossings = 0
        var maxAmplitude = 0
        for (i in 0 until length - 1) {
            if (buffer[i].toInt() * buffer[i + 1].toInt() < 0) {
                zeroCrossings++
            }
            val amp = abs(buffer[i].toInt())
            if (amp > maxAmplitude) maxAmplitude = amp
        }
        
        // Ensure there is some sound (not silence)
        if (maxAmplitude < 200) return false

        // High zero-crossing rate roughly indicates high frequency
        // For 18.5kHz at 44.1kHz, expected rate is ~0.84
        val rate = zeroCrossings.toFloat() / length
        return rate > 0.60f
    }
}
