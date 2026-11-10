package com.muddassir.safina

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Soft tap click synthesised on first use and played through SoundPool.
 * No audio files are needed in res/raw.
 */
class TapSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private var soundId = 0
    private var ready = false

    init {
        pool.setOnLoadCompleteListener { _, _, status -> ready = status == 0 }
        soundId = pool.load(wavFile(context).absolutePath, 1)
    }

    fun play() {
        if (ready) pool.play(soundId, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun release() = pool.release()

    private fun wavFile(context: Context): File {
        val file = File(context.cacheDir, "tap.wav")
        if (!file.exists()) file.writeBytes(buildTapWav())
        return file
    }

    /** Short, quiet two-partial blip with a fast attack and soft decay. */
    private fun buildTapWav(): ByteArray {
        val rate = 44100
        val samples = (rate * 0.09f).toInt()
        val pcm = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val time = i / rate.toFloat()
            val attack = (time / 0.004f).coerceAtMost(1f)
            val decay = exp(-time / 0.022f)
            val wave = sin(2 * PI * 520 * time) * 0.7 + sin(2 * PI * 1040 * time) * 0.3
            val v = (wave * attack * decay * 0.30 * Short.MAX_VALUE).toInt()
                .coerceIn(-32768, 32767)
            pcm[i * 2] = (v and 0xFF).toByte()
            pcm[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }

        val dataSize = pcm.size
        val header = ByteArray(44)
        fun putAscii(offset: Int, text: String) = text.forEachIndexed { i, c ->
            header[offset + i] = c.code.toByte()
        }
        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putAscii(0, "RIFF")
        putInt(4, 36 + dataSize)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)            // fmt chunk size
        putShort(20, 1)           // PCM
        putShort(22, 1)           // mono
        putInt(24, rate)
        putInt(28, rate * 2)      // byte rate
        putShort(32, 2)           // block align
        putShort(34, 16)          // bits per sample
        putAscii(36, "data")
        putInt(40, dataSize)

        return header + pcm
    }
}
