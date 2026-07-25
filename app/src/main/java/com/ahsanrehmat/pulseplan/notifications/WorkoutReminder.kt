package com.ahsanrehmat.pulseplan.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ahsanrehmat.pulseplan.MainActivity
import com.ahsanrehmat.pulseplan.R
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WorkoutReminder {
    private const val CHANNEL_ID = "daily_workout"
    private const val WORK_NAME = "daily_workout_reminder"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily workout",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "A daily reminder for your planned workout"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun schedule(
        context: Context,
        hour: Int,
        minute: Int,
    ) {
        val now = ZonedDateTime.now()
        val initialDelay = delayUntilNextReminder(now, hour, minute)

        val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(
            24,
            TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    internal fun delayUntilNextReminder(
        now: ZonedDateTime,
        hour: Int,
        minute: Int,
    ): Duration {
        require(hour in 0..23) { "Reminder hour must be between 0 and 23." }
        require(minute in 0..59) { "Reminder minute must be between 0 and 59." }

        var nextRun = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (!nextRun.isAfter(now)) nextRun = nextRun.plusDays(1)
        return Duration.between(now, nextRun)
    }

    internal fun show(context: Context) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Your PulsePlan is ready")
            .setContentText("A small workout today keeps the streak moving.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}

class WorkoutReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        WorkoutReminder.show(applicationContext)
        return Result.success()
    }
}
