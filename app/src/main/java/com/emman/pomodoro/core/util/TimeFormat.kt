package com.emman.pomodoro.core.util

object TimeFormat {
    fun mmss(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val minutes = s / 60
        val seconds = s % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun hms(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun minutesLabel(totalSeconds: Int): String {
        val minutes = (totalSeconds + 30) / 60
        return "${minutes} min"
    }
}
