package com.emman.pomodoro.di

import com.emman.pomodoro.data.repository.PlantRepositoryImpl
import com.emman.pomodoro.data.repository.ProfileRepositoryImpl
import com.emman.pomodoro.data.repository.SessionRepositoryImpl
import com.emman.pomodoro.data.repository.SettingsRepositoryImpl
import com.emman.pomodoro.data.repository.TagRepositoryImpl
import com.emman.pomodoro.data.repository.TimerSnapshotStoreImpl
import com.emman.pomodoro.domain.repository.PlantRepository
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.domain.repository.TagRepository
import com.emman.pomodoro.domain.repository.TimerSnapshotStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindPlantRepository(impl: PlantRepositoryImpl): PlantRepository

    @Binds
    @Singleton
    abstract fun bindTimerSnapshotStore(impl: TimerSnapshotStoreImpl): TimerSnapshotStore
}
