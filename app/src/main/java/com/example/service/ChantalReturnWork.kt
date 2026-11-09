package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.MainActivity
import com.example.data.ChantalProgressRepository
import com.example.domain.ChantalJourneyClock
import java.util.concurrent.TimeUnit

object ChantalReturnScheduler {
    const val OPEN_CHANTAL = "com.velaris.OPEN_CHANTAL_RETURN"
    private const val WORK = "chantal-return"
    const val NOTIFICATION_ID = 3401
    fun sync(context: Context, start: Long, now: Long = System.currentTimeMillis()) {
        val state = context.getSharedPreferences("chantal_notifications", Context.MODE_PRIVATE)
        val previous = state.getLong("scheduledStart", 0)
        if (previous == start) return
        val manager = WorkManager.getInstance(context)
        if (start <= 0 || ChantalJourneyClock.ready(start, now)) {
            manager.cancelUniqueWork(WORK)
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        } else {
            val request = OneTimeWorkRequestBuilder<ChantalReturnWorker>()
                .setInitialDelay(ChantalJourneyClock.remainingMs(start, now), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("start" to start)).build()
            manager.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        }
        state.edit().putLong("scheduledStart", start).apply()
    }
}

class ChantalReturnWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (runCatching { com.example.data.RestoreStartupGate.ensureRecovered(applicationContext) }.isFailure) return Result.retry()
        val start = inputData.getLong("start", 0)
        val snapshot = runCatching { ChantalProgressRepository(applicationContext).load() }.getOrElse { return Result.failure() }
        if (start <= 0 || snapshot.optLong("activeJourneyStartMs") != start) return Result.success()
        // Wall-clock changes can make a scheduled worker run before the journey is ready.
        // Keep this work pending instead of permanently losing the return reminder.
        if (!ChantalJourneyClock.ready(start, System.currentTimeMillis())) return Result.retry()
        val prefs = applicationContext.getSharedPreferences("chantal_notifications", Context.MODE_PRIVATE)
        if (prefs.getLong("notifiedStart", 0) == start) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("chantal_return", "Возвращение Шанталь", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext, MainActivity::class.java).setAction(ChantalReturnScheduler.OPEN_CHANTAL)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(applicationContext, 3401, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(ChantalReturnScheduler.NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, "chantal_return")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Шанталь вернулась домой")
            .setContentText("Поход в «${snapshot.optString("activeRegionTitleRu", "лес") }» завершён. Встреть её у очага!")
            .setContentIntent(pending).setAutoCancel(true).build())
        prefs.edit().putLong("notifiedStart", start).commit()
        return Result.success()
    }
}
