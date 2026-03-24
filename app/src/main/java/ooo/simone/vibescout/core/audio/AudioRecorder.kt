package ooo.simone.vibescout.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Records audio from the device microphone and returns raw PCM samples
 * ready for use with SongRecFingerprint.
 *
 * Requires [Manifest.permission.RECORD_AUDIO] permission.
 */
object AudioRecorder {

    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    private const val DEFAULT_DURATION_SECONDS = 12

    /**
     * Record audio from the microphone for the given duration.
     *
     * This is a **blocking** call — run it on a background thread (e.g. via
     * `Dispatchers.IO` or an `Executor`).
     *
     * @param durationSeconds How many seconds to record (default 12, which is
     *        optimal for Shazam recognition).
     * @return A [FloatArray] of mono 16 kHz PCM samples, or `null` if recording
     *         could not be started.
     */
    //@RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @SuppressLint("MissingPermission")
    fun recordAudio(durationSeconds: Int = DEFAULT_DURATION_SECONDS): FloatArray? {
        val totalSamples = SAMPLE_RATE * durationSeconds

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            totalSamples * 4 // 4 bytes per float sample
        )

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }

        val samples = FloatArray(totalSamples)
        var samplesRead = 0

        try {
            recorder.startRecording()

            while (samplesRead < totalSamples) {
                val remaining = totalSamples - samplesRead
                val read = recorder.read(
                    samples,
                    samplesRead,
                    remaining,
                    AudioRecord.READ_BLOCKING
                )

                if (read < 0) {
                    // Error occurred
                    break
                }

                samplesRead += read
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        return if (samplesRead > 0) {
            if (samplesRead < totalSamples) {
                samples.copyOf(samplesRead)
            } else {
                samples
            }
        } else {
            null
        }
    }
}
