package com.emman.pomodoro.core.timing

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTicker @Inject constructor() : Ticker {
    override fun nowMs(): Long = SystemClock.elapsedRealtime()
}
