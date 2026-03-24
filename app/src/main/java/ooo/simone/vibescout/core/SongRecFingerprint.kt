package ooo.simone.vibescout.core

/**
 * JNI bridge to the songrec_fingerprint Rust library.
 *
 * Provides Shazam-compatible audio fingerprinting and song recognition.
 *
 * Audio input must be: mono, 16 kHz sample rate, 32-bit float PCM.
 * Recommended duration: ~12 seconds for best recognition accuracy.
 */
object SongRecFingerprint {

    init {
        System.loadLibrary("songrec_fingerprint")
    }

    /**
     * Generate a Shazam-compatible fingerprint URI from raw PCM samples.
     *
     * @param samples Mono 16 kHz float PCM audio samples.
     * @return A signature URI string (data:audio/vnd.shazam.sig;base64,...),
     *         or a string starting with "ERROR: " on failure.
     */
    @JvmStatic
    external fun generateFingerprint(samples: FloatArray): String

    /**
     * Generate a fingerprint and immediately send it to the Shazam API for recognition.
     *
     * This is a blocking network call — run it on a background thread.
     *
     * @param samples Mono 16 kHz float PCM audio samples.
     * @return JSON string with Shazam's response containing track info,
     *         or a string starting with "ERROR: " on failure.
     */
    @JvmStatic
    external fun recognizeSong(samples: FloatArray): String

    /**
     * Recognize a song from a previously generated signature URI.
     *
     * Useful when you want to generate the fingerprint once and retry recognition later.
     * This is a blocking network call — run it on a background thread.
     *
     * @param signatureUri A signature URI from [generateFingerprint].
     * @return JSON string with Shazam's response containing track info,
     *         or a string starting with "ERROR: " on failure.
     */
    @JvmStatic
    external fun recognizeFromSignature(signatureUri: String): String
}
