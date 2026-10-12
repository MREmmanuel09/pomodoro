package com.emman.pomodoro.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.emman.pomodoro.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
class AmbientSoundPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private var player: MediaPlayer? = null
    private var playingSound: AmbientSound? = null

    val isPlaying: Boolean get() = playingSound != null

    suspend fun play(sound: AmbientSound) {
        stop()
        val rawRes = when (sound) {
            AmbientSound.RAIN -> R.raw.ambient_rain
            AmbientSound.WAVES -> R.raw.ambient_waves
            AmbientSound.FOREST -> R.raw.ambient_forest
        }
        val mediaPlayer = runCatching { MediaPlayer.create(context, rawRes) }
            .getOrNull() ?: return
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(0.8f, 0.8f)
        mediaPlayer.setOnErrorListener { mp, _, _ ->
            runCatching { mp.release() }
            if (player === mp) {
                player = null
                playingSound = null
            }
            true
        }
        mediaPlayer.start()
        player = mediaPlayer
        playingSound = sound
    }

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
        playingSound = null
    }
}
