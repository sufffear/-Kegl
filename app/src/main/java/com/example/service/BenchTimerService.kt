package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.RunStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service технического стенда этапа 0B.
 * - Тип: specialUse (работает и в звуковом, и в чисто вибрационном режиме без фиктивных сенсоров и тишины в аудиопотоке).
 * - Монотонный цикл тиков каждые 100 мс.
 * - Минимальный PARTIAL_WAKE_LOCK удерживается ТОЛЬКО в состояниях PREPARING и RUNNING с жёстким таймаутом
 *   и гарантированно освобождается на паузе, остановке, завершении или уничтожении сервиса.
 * - Отслеживает отключение наушников (ACTION_AUDIO_BECOMING_NOISY + AudioDeviceCallback) и ставит сессию на паузу.
 * - Показывает нейтральное уведомление «Velaris · Сессия» с кнопкой «Пауза».
 */
class BenchTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var noisyReceiverRegistered = false
    private var audioDeviceCallbackRegistered = false
    private var lastObservedNoticeText: String = ""

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                BenchSessionCoordinator.refreshHardwareStatus()
                val snap = BenchSessionCoordinator.onExternalInterruption(
                    "Отключены наушники (ACTION_AUDIO_BECOMING_NOISY)"
                )
                releaseWakeLockSafely()
                updateForegroundNotification(snap, force = true)
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            BenchSessionCoordinator.refreshHardwareStatus()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            BenchSessionCoordinator.refreshHardwareStatus()
            val removedHeadset = removedDevices?.any { dev ->
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    dev.type == AudioDeviceInfo.TYPE_USB_HEADSET
            } == true

            if (removedHeadset) {
                val current = BenchSessionCoordinator.engineSnapshot.value
                if (current.status == RunStatus.PREPARING || current.status == RunStatus.RUNNING) {
                    val snap = BenchSessionCoordinator.onExternalInterruption(
                        "Отключены наушники (AudioDeviceCallback)"
                    )
                    releaseWakeLockSafely()
                    updateForegroundNotification(snap, force = true)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        BenchSessionCoordinator.ensureInitialized(applicationContext)
        ensureNotificationChannel()
        registerHeadphoneMonitors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        BenchSessionCoordinator.ensureInitialized(applicationContext)

        when (intent?.action) {
            ACTION_START_DEMO -> {
                val started = BenchSessionCoordinator.startDemoSession()
                if (started) {
                    promoteToForeground(BenchSessionCoordinator.engineSnapshot.value)
                    acquireWakeLockForActiveRun()
                    startMonotonicLoop()
                }
            }

            ACTION_START_FREE -> {
                val snap = BenchSessionCoordinator.engineSnapshot.value
                promoteToForeground(snap)
                acquireWakeLockForActiveRun()
                startMonotonicLoop()
            }

            ACTION_PAUSE_FROM_NOTIFICATION,
            ACTION_PAUSE_MANUAL -> {
                val reason = if (intent.action == ACTION_PAUSE_FROM_NOTIFICATION) {
                    "Нажата кнопка «Пауза» в уведомлении"
                } else {
                    "Пауза нажата на экране стенда"
                }
                val snap = BenchSessionCoordinator.pauseSession(reason)
                releaseWakeLockSafely()
                updateForegroundNotification(snap, force = true)
            }

            ACTION_RESUME_MANUAL -> {
                val resumed = BenchSessionCoordinator.resumeSession()
                if (resumed) {
                    val snap = BenchSessionCoordinator.engineSnapshot.value
                    promoteToForeground(snap)
                    acquireWakeLockForActiveRun()
                    startMonotonicLoop()
                }
            }

            ACTION_REQUEST_STOP -> {
                val snap = BenchSessionCoordinator.requestStopWithConfirmation()
                releaseWakeLockSafely()
                updateForegroundNotification(snap, force = true)
            }

            ACTION_CONFIRM_STOP -> {
                val snap = BenchSessionCoordinator.confirmStopWithoutCredit()
                if (snap.status == RunStatus.STOPPED) stopLoopAndLeaveForeground()
            }

            else -> {
                // Если система перезапустила сервис после нехватки памяти (process death recovery)
                val current = BenchSessionCoordinator.engineSnapshot.value
                if (!current.isActiveOrPaused) {
                    BenchSessionCoordinator.appendLog(
                        "RECOVERY",
                        "Сервис перезапущен системой после выгрузки процесса -> восстановлен статус INTERRUPTED"
                    )
                    BenchSessionCoordinator.onExternalInterruption(
                        "Процесс был выгружен системой (восстановлено состояние прерывания)"
                    )
                    stopLoopAndLeaveForeground()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Обработка системного таймаута FGS на Android 15+ (API 35–37) во избежание ANR
        BenchSessionCoordinator.appendLog(
            "FGS_TIMEOUT",
            "Получен системный callback onTimeout(fgsType=$fgsType). Безопасный переход в паузу и остановка FGS."
        )
        BenchSessionCoordinator.onExternalInterruption("Системный таймаут фоновой службы ОС")
        stopLoopAndLeaveForeground()
    }

    private fun startMonotonicLoop() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val snap = BenchSessionCoordinator.onServiceTick()
                when (snap.status) {
                    RunStatus.PREPARING,
                    RunStatus.RUNNING -> {
                        updateForegroundNotification(snap, force = false)
                    }

                    RunStatus.PAUSED,
                    RunStatus.INTERRUPTED -> {
                        releaseWakeLockSafely()
                        updateForegroundNotification(snap, force = true)
                        break
                    }

                    RunStatus.COMPLETED -> {
                        releaseWakeLockSafely()
                        updateForegroundNotification(snap, force = true)
                        // Оставляем финальное уведомление на 1.5 сек и завершаем foreground-режим
                        delay(1_500L)
                        stopLoopAndLeaveForeground()
                        break
                    }

                    RunStatus.STOPPED,
                    RunStatus.IDLE -> {
                        stopLoopAndLeaveForeground()
                        break
                    }
                }
                delay(50L)
            }
        }
    }

    private fun promoteToForeground(snapshot: PhaseEngineSnapshot) {
        val notification = buildSessionNotification(snapshot)
        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            BenchSessionCoordinator.setServiceForeground(true)
        } catch (e: Exception) {
            BenchSessionCoordinator.appendLog(
                "FGS_ERROR",
                "Не удалось перевести сервис в foreground: ${e.javaClass.simpleName}"
            )
        }
    }

    private fun updateForegroundNotification(snapshot: PhaseEngineSnapshot, force: Boolean) {
        val contentText = formatNotificationBody(snapshot)
        if (!force && contentText == lastObservedNoticeText) return
        lastObservedNoticeText = contentText

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            nm.notify(NOTIFICATION_ID, buildSessionNotification(snapshot))
        } catch (_: Exception) {
        }
    }

    private fun formatNotificationBody(snapshot: PhaseEngineSnapshot): String {
        return when (snapshot.status) {
            RunStatus.PREPARING -> {
                val sec = ((snapshot.prepRemainingMs + 999L) / 1000L).coerceAtLeast(1L)
                val prefix = if (snapshot.isCustomProgram) "Программа" else "Тренировка"
                "$prefix · Подготовка ($sec сек)"
            }
            RunStatus.RUNNING -> {
                val sec = ((snapshot.phaseRemainingMs + 999L) / 1000L).coerceAtLeast(1L)
                if (snapshot.isCustomProgram && snapshot.currentBlockName.isNotBlank()) {
                    "${snapshot.currentBlockName} · ${snapshot.currentPhase.labelRu} · $sec с"
                } else {
                    "Цикл ${snapshot.currentRepetition}/${snapshot.totalRepetitions} · ${snapshot.currentPhase.labelRu} · $sec с"
                }
            }
            RunStatus.PAUSED,
            RunStatus.INTERRUPTED -> {
                "Пауза — расслабьтесь · Откройте приложение для продолжения"
            }
            RunStatus.COMPLETED -> "Сессия завершена"
            RunStatus.STOPPED -> "Сессия остановлена"
            RunStatus.IDLE -> "Готов к запуску"
        }
    }

    private fun buildSessionNotification(snapshot: PhaseEngineSnapshot): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(formatNotificationBody(snapshot))
            .setContentIntent(openPendingIntent)
            .setOngoing(snapshot.isActiveOrPaused)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            // Нейтральное уведомление без интимных подробностей и без текста истории на экране блокировки
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (snapshot.status == RunStatus.PREPARING || snapshot.status == RunStatus.RUNNING) {
            val pauseIntent = Intent(this, BenchTimerService::class.java).apply {
                action = ACTION_PAUSE_FROM_NOTIFICATION
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                101,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_action_pause),
                pausePendingIntent
            )
        }

        return builder.build()
    }

    private fun acquireWakeLockForActiveRun() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Velaris:BenchTimerWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                // Динамический таймаут WakeLock, согласованный с оставшейся продолжительностью подхода (Итерация v1.3 п. 6)
                val remainingActiveMs = BenchSessionCoordinator.engineSnapshot.value.remainingActiveMs
                val lockTimeoutMs = (remainingActiveMs + 60_000L).coerceAtLeast(5 * 60 * 1000L)
                wakeLock?.acquire(lockTimeoutMs)
                BenchSessionCoordinator.setWakeLockHeld(true)
            }
        } catch (e: Exception) {
            BenchSessionCoordinator.appendLog(
                "WAKELOCK_ERR",
                "Ошибка захвата WakeLock: ${e.javaClass.simpleName}"
            )
        }
    }

    private fun releaseWakeLockSafely() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        BenchSessionCoordinator.setWakeLockHeld(false)
    }

    private fun stopLoopAndLeaveForeground() {
        tickerJob?.cancel()
        tickerJob = null
        releaseWakeLockSafely()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        BenchSessionCoordinator.setServiceForeground(false)
        stopSelf()
    }

    private fun registerHeadphoneMonitors() {
        if (!noisyReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(becomingNoisyReceiver, filter)
            }
            noisyReceiverRegistered = true
        }
        if (!audioDeviceCallbackRegistered) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.registerAudioDeviceCallback(audioDeviceCallback, null)
            audioDeviceCallbackRegistered = true
        }
    }

    private fun unregisterHeadphoneMonitors() {
        if (noisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {
            }
            noisyReceiverRegistered = false
        }
        if (audioDeviceCallbackRegistered) {
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.unregisterAudioDeviceCallback(audioDeviceCallback)
            } catch (_: Exception) {
            }
            audioDeviceCallbackRegistered = false
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        releaseWakeLockSafely()
        unregisterHeadphoneMonitors()
        BenchSessionCoordinator.setServiceForeground(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "velaris_session_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_DEMO = "com.velaris.app.action.START_DEMO"
        const val ACTION_START_FREE = "com.velaris.app.action.START_FREE"
        const val ACTION_PAUSE_MANUAL = "com.velaris.app.action.PAUSE_MANUAL"
        const val ACTION_PAUSE_FROM_NOTIFICATION = "com.velaris.app.action.PAUSE_FROM_NOTIFICATION"
        const val ACTION_RESUME_MANUAL = "com.velaris.app.action.RESUME_MANUAL"
        const val ACTION_REQUEST_STOP = "com.velaris.app.action.REQUEST_STOP"
        const val ACTION_CONFIRM_STOP = "com.velaris.app.action.CONFIRM_STOP"

        fun sendCommand(context: Context, action: String) {
            val intent = Intent(context, BenchTimerService::class.java).apply {
                this.action = action
            }
            if (action == ACTION_START_DEMO || action == ACTION_START_FREE || action == ACTION_RESUME_MANUAL) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }
    }
}
