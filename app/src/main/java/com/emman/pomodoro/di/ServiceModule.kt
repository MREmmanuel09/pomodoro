package com.emman.pomodoro.di

import com.emman.pomodoro.core.notification.TimerServiceController
import com.emman.pomodoro.core.notification.TimerServiceStarter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindTimerServiceController(impl: TimerServiceStarter): TimerServiceController
}
