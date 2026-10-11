package com.emman.pomodoro.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emman.pomodoro.MainActivity
import com.emman.pomodoro.R
import com.emman.pomodoro.core.util.TimeFormat
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.TimerState
import com.emman.pomodoro.domain.model.TimerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        val phases = NotificationChannel(
            CHANNEL_PHASES,
            context.getString(R.string.notif_channel_phases),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_phases_desc)
            enableVibration(true)
        }
        val goals = NotificationChannel(
            CHANNEL_GOALS,
            context.getString(R.string.notif_channel_goals),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_goals_desc)
        }
        notificationManager.createNotificationChannel(phases)
        notificationManager.createNotificationChannel(goals)
    }

    fun buildTimerNotification(state: TimerState): Notification {
        val phase = state.currentPhase
        val title = when (state.status) {
            TimerStatus.RUNNING -> phaseTitle(phase)
            TimerStatus.PAUSED -> context.getString(R.string.notif_paused)
            TimerStatus.FINISHED -> context.getString(R.string.notif_round_done)
            TimerStatus.IDLE -> context.getString(R.string.app_name)
        }
        val text = when (state.status) {
            TimerStatus.RUNNING, TimerStatus.PAUSED ->
                TimeFormat.mmss(state.remainingSeconds) + " · " +
                    context.getString(R.string.notif_pomodoros_today, state.pomodorosCompletedInRound)
            else -> context.getString(R.string.notif_ready)
        }

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PHASES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(state.status == TimerStatus.RUNNING || state.status == TimerStatus.PAUSED)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        when (state.status) {
            TimerStatus.RUNNING -> {
                builder.addAction(
                    0,
                    context.getString(R.string.notif_action_pause),
                    servicePendingIntent(TimerService.ACTION_PAUSE, 1)
                )
                builder.addAction(
                    0,
                    context.getString(R.string.notif_action_skip),
                    servicePendingIntent(TimerService.ACTION_SKIP, 2)
                )
            }
            TimerStatus.PAUSED -> {
                builder.addAction(
                    0,
                    context.getString(R.string.notif_action_resume),
                    servicePendingIntent(TimerService.ACTION_RESUME, 3)
                )
                builder.addAction(
                    0,
                    context.getString(R.string.notif_action_stop),
                    servicePendingIntent(TimerService.ACTION_STOP, 4)
                )
            }
            else -> Unit
        }
        return builder.build()
    }

    fun updateTimerNotification(state: TimerState) {
        notificationManager.notify(NOTIFICATION_ID_TIMER, buildTimerNotification(state))
    }

    fun notifyPhaseCompleted(phase: Phase, status: SessionStatus) {
        val title = when (status) {
            SessionStatus.COMPLETED -> when (phase.type) {
                PhaseType.WORK -> context.getString(R.string.notif_work_done_title)
                PhaseType.SHORT_BREAK -> context.getString(R.string.notif_break_done_title)
                PhaseType.LONG_BREAK -> context.getString(R.string.notif_long_break_done_title)
            }
            SessionStatus.SKIPPED -> context.getString(R.string.notif_phase_skipped_title)
            SessionStatus.ABORTED -> context.getString(R.string.notif_phase_skipped_title)
        }
        val text = when (status) {
            SessionStatus.COMPLETED -> when (phase.type) {
                PhaseType.WORK -> context.getString(R.string.notif_work_done_text)
                PhaseType.SHORT_BREAK, PhaseType.LONG_BREAK ->
                    context.getString(R.string.notif_break_done_text)
            }
            else -> context.getString(R.string.notif_phase_skipped_text)
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_PHASES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PHASE, notification)
    }

    fun notifyDailyGoalReached(goal: Int) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GOALS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_goal_title))
            .setContentText(context.getString(R.string.notif_goal_text, goal))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(NOTIFICATION_ID_GOAL, notification)
    }

    fun cancelTimerNotification() {
        notificationManager.cancel(NOTIFICATION_ID_TIMER)
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun phaseTitle(phase: Phase?): String = when (phase?.type) {
        PhaseType.WORK -> context.getString(R.string.phase_work)
        PhaseType.SHORT_BREAK -> context.getString(R.string.phase_short_break)
        PhaseType.LONG_BREAK -> context.getString(R.string.phase_long_break)
        null -> context.getString(R.string.app_name)
    }

    companion object {
        const val CHANNEL_PHASES = "phases"
        const val CHANNEL_GOALS = "goals"
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_PHASE = 1002
        const val NOTIFICATION_ID_GOAL = 1003
    }
}
