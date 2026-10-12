package com.emman.pomodoro.core.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerServiceStarter @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TimerServiceController {
    override fun start() = TimerService.start(context)
}
