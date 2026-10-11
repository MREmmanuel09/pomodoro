package com.emman.pomodoro.core.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.emman.pomodoro.core.audio.CuePlayer
import com.emman.pomodoro.core.audio.CueSound
import com.emman.pomodoro.core.audio.VibrationManager
import com.emman.pomodoro.core.timing.TimerEngine
import com.emman.pomodoro.core.timing.TimerEvent
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.repository.PlantRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var engine: TimerEngine
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var cuePlayer: CuePlayer
    @Inject lateinit var vibrationManager: VibrationManager
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var plantRepository: PlantRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground(engine.state.value)
        serviceScope.launch {
            engine.state.collect { state ->
                notificationHelper.updateTimerNotification(state)
                if (state.status == TimerStatus.IDLE) {
                    stopSelf()
                }
            }
        }
        serviceScope.launch {
            engine.events.collect { event -> handleEvent(event) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            TimerService.ACTION_PAUSE -> engine.pause()
            TimerService.ACTION_RESUME -> engine.resume()
            TimerService.ACTION_SKIP -> engine.skipPhase()
            TimerService.ACTION_STOP -> {
                engine.reset()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground(state: com.emman.pomodoro.domain.model.TimerState) {
        val notification = notificationHelper.buildTimerNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_TIMER,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
        }
    }

    private suspend fun handleEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.PhaseCompleted -> {
                logSession(event)
                val settings = settingsRepository.settings.first()
                if (settings.cueSoundEnabled) {
                    val cue = when {
                        event.status != SessionStatus.COMPLETED -> CueSound.BREAK_COMPLETE
                        event.phase.type == PhaseType.WORK -> CueSound.WORK_COMPLETE
                        else -> CueSound.BREAK_COMPLETE
                    }
                    cuePlayer.play(cue)
                }
                if (settings.vibrationEnabled) {
                    when {
                        event.status != SessionStatus.COMPLETED ->
                            vibrationManager.vibrateBreakComplete()
                        event.phase.type == PhaseType.WORK ->
                            vibrationManager.vibrateWorkComplete()
                        else -> vibrationManager.vibrateBreakComplete()
                    }
                }
                notificationHelper.notifyPhaseCompleted(event.phase, event.status)

                if (event.phase.type == PhaseType.WORK && event.status == SessionStatus.COMPLETED) {
                    if (settings.plantEnabled) {
                        plantRepository.evaluateWither()
                        plantRepository.onPomodoroCompleted()
                    }
                    val todayStart = startOfTodayEpochMs()
                    val count = sessionRepository
                        .observeCompletedWorkCountBetween(todayStart, todayStart + DAY_MS)
                        .first()
                    if (count == settings.dailyGoalPomodoros) {
                        notificationHelper.notifyDailyGoalReached(settings.dailyGoalPomodoros)
                        if (settings.vibrationEnabled) {
                            vibrationManager.vibrateGoalReached()
                        }
                        if (settings.cueSoundEnabled) {
                            cuePlayer.play(CueSound.GOAL_REACHED)
                        }
                    }
                }
            }
            is TimerEvent.RoundCompleted -> {
                stopSelf()
            }
        }
    }

    private fun logSession(event: TimerEvent.PhaseCompleted) {
        val now = System.currentTimeMillis()
        val startedAt = now - event.actualDurationSec * 1000L
        serviceScope.launch {
            sessionRepository.logSession(
                Session(
                    profileId = event.profileId,
                    phaseType = event.phase.type,
                    plannedDurationSec = event.phase.durationSeconds,
                    actualDurationSec = event.actualDurationSec,
                    startedAtEpochMs = startedAt,
                    completedAtEpochMs = now,
                    status = event.status,
                    tagId = event.tagId
                )
            )
        }
    }

    private fun startOfTodayEpochMs(): Long =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    override fun onDestroy() {
        cuePlayer.release()
        serviceScope.cancel()
        notificationHelper.cancelTimerNotification()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.emman.pomodoro.ACTION_PAUSE"
        const val ACTION_RESUME = "com.emman.pomodoro.ACTION_RESUME"
        const val ACTION_SKIP = "com.emman.pomodoro.ACTION_SKIP"
        const val ACTION_STOP = "com.emman.pomodoro.ACTION_STOP"
        private const val DAY_MS = 24L * 60 * 60 * 1000

        fun start(context: Context) {
            val intent = Intent(context, TimerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
