package com.emman.pomodoro.di

import com.emman.pomodoro.core.timing.SystemTicker
import com.emman.pomodoro.core.timing.Ticker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimingModule {

    @Binds
    @Singleton
    abstract fun bindTicker(impl: SystemTicker): Ticker
}
