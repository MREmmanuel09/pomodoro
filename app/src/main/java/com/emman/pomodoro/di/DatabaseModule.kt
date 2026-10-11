package com.emman.pomodoro.di

import android.content.Context
import androidx.room.Room
import com.emman.pomodoro.data.local.AppDatabase
import com.emman.pomodoro.data.local.dao.SessionDao
import com.emman.pomodoro.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pomodoro.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()
}
