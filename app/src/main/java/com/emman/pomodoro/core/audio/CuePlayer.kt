package com.emman.pomodoro.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

enum class CueSound {
    WORK_COMPLETE,
    BREAK_COMPLETE,
    GOAL_REACHED
}

@Singleton
class CuePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var track: AudioTrack? = null

    suspend fun play(cue: CueSound) = withContext(Dispatchers.IO) {
        val notes = when (cue) {
            CueSound.WORK_COMPLETE -> listOf(Note(523, 180), Note(659, 220))
            CueSound.BREAK_COMPLETE -> listOf(Note(659, 180), Note(523, 220))
            CueSound.GOAL_REACHED -> listOf(Note(523, 150), Note(659, 150), Note(784, 300))
        }
        playSequence(notes)
    }

    private fun playSequence(notes: List<Note>) {
        release()
        val sampleRate = 44100
        val totalSamples = notes.sumOf { it.durationMs } * sampleRate / 1000
        val pcm = ShortArray(totalSamples)
        var offset = 0
        for (note in notes) {
            val noteSamples = note.durationMs * sampleRate / 1000
            val attack = (sampleRate * 0.01).toInt().coerceAtLeast(1)
            val releaseLen = (sampleRate * 0.03).toInt().coerceAtLeast(1)
            for (i in 0 until noteSamples) {
                val envelope = when {
                    i < attack -> i.toFloat() / attack
                    i > noteSamples - releaseLen -> (noteSamples - i).toFloat() / releaseLen
                    else -> 1f
                }
                val value = sin(2.0 * PI * note.frequency * i / sampleRate)
                pcm[offset + i] = (value * envelope * Short.MAX_VALUE * 0.5).toInt().toShort()
            }
            offset += noteSamples
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
        track = audioTrack
        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()
    }

    fun release() {
        track?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        track = null
    }

    private data class Note(val frequency: Int, val durationMs: Int)
}
