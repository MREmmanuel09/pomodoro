package com.emman.pomodoro

import android.app.Application
import com.emman.pomodoro.core.notification.TimerService
import com.emman.pomodoro.core.timing.ApplicationScope
import com.emman.pomodoro.core.timing.TimerEngine
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.TimerSnapshotStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PomodoroApp : Application() {

    @Inject lateinit var engine: TimerEngine
    @Inject lateinit var snapshotStore: TimerSnapshotStore
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val snapshot = snapshotStore.load() ?: return@launch
            val profile = profileRepository.profiles.first()
                .find { it.id == snapshot.profileId }
            if (profile == null) {
                snapshotStore.clear()
                return@launch
            }
            when (snapshot.status) {
                TimerStatus.RUNNING.name -> TimerService.start(this@PomodoroApp)
                TimerStatus.PAUSED.name -> engine.restore(snapshot, profile)
                else -> snapshotStore.clear()
            }
        }
    }
}
