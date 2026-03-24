package ooo.simone.vibescout.core.audio

import ooo.simone.vibescout.core.SongRecFingerprint

class AudioRecognizer {
    companion object {
        fun analyzeAudio(pcmArray: FloatArray): String {
            return SongRecFingerprint.recognizeSong(pcmArray)
        }
    }
}