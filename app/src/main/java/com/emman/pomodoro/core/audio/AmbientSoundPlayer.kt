package com.emman.pomodoro.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

enum class AmbientSound(val id: String) {
    RAIN("rain"),
    WAVES("waves"),
    FOREST("forest");

    companion object {
        fun fromId(id: String): AmbientSound =
            entries.firstOrNull { it.id == id } ?: RAIN
    }
}

@Singleton
class AmbientSoundPlayer @Inject constructor() {

    private var track: AudioTrack? = null
    private var playingSound: AmbientSound? = null

    val isPlaying: Boolean get() = playingSound != null

    suspend fun play(sound: AmbientSound) = withContext(Dispatchers.IO) {
        stop()
        val pcm = when (sound) {
            AmbientSound.RAIN -> synthesizeRain()
            AmbientSound.WAVES -> synthesizeWaves()
            AmbientSound.FOREST -> synthesizeForest()
        }
        val sampleRate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.setLoopPoints(0, pcm.size, -1)
        audioTrack.play()
        track = audioTrack
        playingSound = sound
    }

    fun stop() {
        track?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        track = null
        playingSound = null
    }

    private fun synthesizeRain(): ShortArray {
        val sampleRate = 22050
        val seconds = 8
        val samples = sampleRate * seconds
        val pcm = ShortArray(samples)
        val random = Random(42)
        var previous = 0.0
        for (i in 0 until samples) {
            val white = random.nextDouble(-1.0, 1.0)
            previous = previous * 0.85 + white * 0.15
            pcm[i] = (previous * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        return pcm
    }

    private fun synthesizeWaves(): ShortArray {
        val sampleRate = 22050
        val seconds = 12
        val samples = sampleRate * seconds
        val pcm = ShortArray(samples)
        val random = Random(7)
        var previous = 0.0
        for (i in 0 until samples) {
            val white = random.nextDouble(-1.0, 1.0)
            previous = previous * 0.9 + white * 0.1
            val time = i.toDouble() / sampleRate
            val lfo = 0.35 + 0.65 * (0.5 + 0.5 * sin(2.0 * PI * time / 8.0))
            pcm[i] = (previous * lfo * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        return pcm
    }

    private fun synthesizeForest(): ShortArray {
        val sampleRate = 22050
        val seconds = 10
        val samples = sampleRate * seconds
        val pcm = ShortArray(samples)
        val random = Random(11)
        var background = 0.0
        for (i in 0 until samples) {
            val white = random.nextDouble(-1.0, 1.0)
            background = background * 0.97 + white * 0.03
            pcm[i] = (background * Short.MAX_VALUE * 0.15).toInt().toShort()
        }
        var t = random.nextInt(sampleRate, sampleRate * 2)
        while (t < samples - sampleRate / 2) {
            val chirpFreq = 1800.0 + random.nextDouble(0.0, 1200.0)
            val chirpLen = sampleRate / 20 + random.nextInt(sampleRate / 20)
            val amplitude = Short.MAX_VALUE * 0.25
            for (j in 0 until chirpLen) {
                val index = t + j
                if (index >= samples) break
                val envelope = sin(PI * j / chirpLen)
                val value = sin(2.0 * PI * chirpFreq * j / sampleRate)
                pcm[index] = (pcm[index] + value * envelope * amplitude).toInt().toShort()
            }
            t += sampleRate * 2 + random.nextInt(sampleRate * 3)
        }
        return pcm
    }
}
