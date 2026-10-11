package com.emman.pomodoro.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.emman.pomodoro.data.local.datastore.PomodoroDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
            )
        ) {
            context.preferencesDataStoreFile(PomodoroDataStore.DATA_STORE_FILE_NAME)
        }

    @Provides
    @Singleton
    fun providePomodoroDataStore(
        dataStore: DataStore<Preferences>
    ): PomodoroDataStore = PomodoroDataStore(dataStore)
}
