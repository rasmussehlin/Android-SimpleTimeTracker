package com.example.util.simpletimetracker.feature_notification.goalTime.scheduler

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.util.simpletimetracker.core.utils.PendingIntents
import com.example.util.simpletimetracker.feature_notification.recevier.NotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationGoalTimeScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(durationMillis: Long, typeId: Long) {
        val timestamp = System.currentTimeMillis() + durationMillis

        scheduleAtTime(timestamp, typeId)
    }

    fun cancelSchedule(typeId: Long) {
        alarmManager?.cancel(getPendingIntent(typeId))
    }

    private fun scheduleAtTime(timestamp: Long, typeId: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setAndAllowWhileIdle(RTC_WAKEUP, timestamp, getPendingIntent(typeId))
        } else {
            alarmManager?.set(RTC_WAKEUP, timestamp, getPendingIntent(typeId))
        }
    }

    private fun getPendingIntent(typeId: Long): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_GOAL_TIME_REMINDER
            putExtra(NotificationReceiver.EXTRA_GOAL_TIME_TYPE_ID, typeId)
        }

        return PendingIntent.getBroadcast(
            context,
            typeId.toInt(),
            intent,
            PendingIntents.getFlags(),
        )
    }
}