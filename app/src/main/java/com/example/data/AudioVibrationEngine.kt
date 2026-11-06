package com.example.data

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.MetronomeTickStyle
import com.example.domain.SignalPatternSpec
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.domain.SoundSynthesisSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

data class AudioHardwareStatus(
    val outputRouteLabelRu: String = "Динамик",
    val isHeadphonesConnected: Boolean = false,
    val ringerModeLabelRu: String = "Звук включён",
    val isSilentOrVibrateRinger: Boolean = false,
    val isDndActive: Boolean = false,
    val hasHardwareVibrator: Boolean = true,
    val noActiveSignalChannelWarning: String? = null
)

/**
 * Компактная запись диагностики аудиособытия (без личных данных) для раздела «Настройки → Диагностика».
 */
data class AudioDiagnosticEntry(
    val elapsedMs: Long,
    val eventType: String,
    val signalLabel: String,
    val packTitle: String,
    val isSample: Boolean,
    val effectiveVolume: Float,
    val pcmRms: Float,
    val trackGain: Float,
    val focusHeld: Boolean,
    val outputRoute: String
) {
    fun formatCompactLine(): String {
        return String.format(
            Locale.US,
            "[+%dms][%s] %s (%s%s) vol=%.2f rms=%.3f gain=%.1f focus=%s route=%s",
            elapsedMs,
            eventType,
            signalLabel,
            packTitle,
            if (isSample) ", образец" else "",
            effectiveVolume,
            pcmRms,
            trackGain,
            if (focusHeld) "ON" else "OFF",
            outputRoute
        )
    }
}

/**
 * Управляет локальным синтезом 3 звуковых наборов («Мягкий», «Колокольчики», «Чёткий»)
 * и 2 вариантов тика метронома через PCM AudioTrack (без изменения системной громкости),
 * вибрацией (VibrationEffect), проверкой маршрута аудиовыхода и аудиофокусом.
 *
 * Исправление перепада громкости (v1.1):
 * 1. Единые AudioAttributes (USAGE_MEDIA + CONTENT_TYPE_MUSIC) для AudioFocusRequest и AudioTrack
 *    вместе с setWillPauseWhenDucked(true) и AUDIOFOCUS_GAIN_TRANSIENT: исключает автоматическое
 *    системное приглушение (auto-ducking) собственного потока USAGE_MEDIA после первого сигнала.
 * 2. Приоритет основных команд над метрономом: если звучит команда фазы, пришедший тик метронома
 *    пропускается; если звучит тик метронома и приходит команда фазы, тик немедленно прерывается.
 * 3. На паузе сигнал PAUSED доигрывает без принудительного обрыва дорожки при освобождении фокуса.
 * 4. Единый тракт расчёта громкости и синтеза PCM для образцов в настройках и сигналов сессии.
 */
class AudioVibrationEngine(
    private val context: Context,
    private val onAudioFocusLost: (reasonRu: String) -> Unit,
    private val onLog: (category: String, message: String) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val playbackMutex = Mutex()

    @Volatile
    private var activeAudioTrack: AudioTrack? = null

    @Volatile
    private var activePlayingSignal: SignalType? = null

    @Volatile
    private var activePlaybackJob: Job? = null

    private var currentFocusRequest: AudioFocusRequest? = null

    @Volatile
    private var hasAudioFocus: Boolean = false

    private val _audioDiagnostics = MutableStateFlow<List<AudioDiagnosticEntry>>(emptyList())
    val audioDiagnostics: StateFlow<List<AudioDiagnosticEntry>> = _audioDiagnostics.asStateFlow()

    private fun buildUnifiedAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Не прерываем тренировку: разрешено смешивание (ducking)
                recordDiagnostic(
                    eventType = "FOCUS_DUCK",
                    signalLabel = "CAN_DUCK",
                    packTitle = "-",
                    isSample = false,
                    effectiveVolume = 0f,
                    pcmRms = 0f,
                    trackGain = 1.0f,
                    outputRoute = "mixing-allowed"
                )
                onLog("AUDIO_FOCUS", "Фокус: CAN_DUCK — фоновое медиа продолжается со смешиванием")
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                val codeName = if (focusChange == AudioManager.AUDIOFOCUS_LOSS) "AUDIOFOCUS_LOSS" else "AUDIOFOCUS_LOSS_TRANSIENT"
                val isCallActive = try {
                    audioManager.mode == AudioManager.MODE_IN_CALL ||
                        audioManager.mode == AudioManager.MODE_RINGTONE ||
                        audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
                } catch (_: Exception) {
                    false
                }

                if (isCallActive) {
                    recordDiagnostic(
                        eventType = "FOCUS_LOST",
                        signalLabel = "$codeName (CALL)",
                        packTitle = "-",
                        isSample = false,
                        effectiveVolume = 0f,
                        pcmRms = 0f,
                        trackGain = 0f,
                        outputRoute = "auto-pause-call"
                    )
                    onAudioFocusLost("Входящий звонок или разговор ($codeName)")
                } else {
                    recordDiagnostic(
                        eventType = "FOCUS_EXTERNAL",
                        signalLabel = codeName,
                        packTitle = "-",
                        isSample = false,
                        effectiveVolume = 0f,
                        pcmRms = 0f,
                        trackGain = 1.0f,
                        outputRoute = "keep-running"
                    )
                    onLog("AUDIO_FOCUS", "Внешнее медиа активно ($codeName) — тренировка продолжается без принудительной паузы YouTube")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                recordDiagnostic(
                    eventType = "FOCUS_GAIN",
                    signalLabel = "AUDIOFOCUS_GAIN",
                    packTitle = "-",
                    isSample = false,
                    effectiveVolume = 0f,
                    pcmRms = 0f,
                    trackGain = 1.0f,
                    outputRoute = "gain"
                )
                onLog("AUDIO_FOCUS", "Аудиофокус возвращён системе (AUDIOFOCUS_GAIN)")
            }
        }
    }

    fun requestSessionAudioFocus(duckOtherApps: Boolean = false): Boolean {
        if (!duckOtherApps) {
            // Режим одновременного воспроизведения со звуком других приложений:
            // Не перехватываем монопольный аудиофокус, чтобы YouTube и плееры не ставились на паузу.
            hasAudioFocus = false
            recordDiagnostic(
                eventType = "FOCUS_BYPASS",
                signalLabel = "SIMULTANEOUS_MIXING",
                packTitle = "USAGE_MEDIA",
                isSample = false,
                effectiveVolume = 1f,
                pcmRms = 0f,
                trackGain = 1.0f,
                outputRoute = "STREAM_MUSIC"
            )
            onLog("AUDIO_FOCUS", "Одновременное воспроизведение со звуком YouTube (без перехвата аудиофокуса)")
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = buildUnifiedAudioAttributes()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            currentFocusRequest = req
            val res = audioManager.requestAudioFocus(req)
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        }
        recordDiagnostic(
            eventType = "FOCUS_REQ",
            signalLabel = if (hasAudioFocus) "GRANTED (MAY_DUCK)" else "DENIED",
            packTitle = "USAGE_MEDIA",
            isSample = false,
            effectiveVolume = 1f,
            pcmRms = 0f,
            trackGain = 1.0f,
            outputRoute = "STREAM_MUSIC"
        )
        onLog(
            "AUDIO_FOCUS",
            if (hasAudioFocus) {
                "Аудиофокус получен (USAGE_MEDIA, AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, willPauseWhenDucked=false)"
            } else {
                "Отказ в получении аудиофокуса (активен звонок или внешний монопольный поток)"
            }
        )
        return hasAudioFocus
    }

    fun abandonSessionAudioFocus() {
        try {
            // Останавливаем немедленно только фоновый тик метронома; сигналу PAUSED даём спокойно доиграть
            if (activePlayingSignal == SignalType.METRONOME_TICK) {
                activePlaybackJob?.cancel()
                stopActiveTrackImmediate()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusChangeListener)
            }
        } catch (_: Exception) {
        }
        hasAudioFocus = false
    }

    private fun stopActiveTrackImmediate() {
        val current = activeAudioTrack ?: return
        activeAudioTrack = null
        activePlayingSignal = null
        try {
            if (current.playState == AudioTrack.PLAYSTATE_PLAYING) {
                current.stop()
            }
        } catch (_: Exception) {
        }
        try {
            current.release()
        } catch (_: Exception) {
        }
    }

    fun inspectHardwareStatus(settings: BenchSignalSettings): AudioHardwareStatus {
        val outputDevices = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: Exception) {
            emptyArray()
        }

        var hasBluetoothHeadset = false
        var hasWiredHeadset = false
        var hasBuiltinSpeaker = false

        for (device in outputDevices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_HEARING_AID -> hasBluetoothHeadset = true

                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> hasWiredHeadset = true

                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> hasBuiltinSpeaker = true
            }
        }

        val ringerMode = try {
            audioManager.ringerMode
        } catch (_: Exception) {
            AudioManager.RINGER_MODE_NORMAL
        }

        val isSilentOrVibrate = ringerMode != AudioManager.RINGER_MODE_NORMAL
        val ringerLabel = when (ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Без звука (Silent)"
            AudioManager.RINGER_MODE_VIBRATE -> "Вибрация (Vibrate)"
            else -> "Обычный (Звук)"
        }

        val interruptionFilter = try {
            notificationManager.currentInterruptionFilter
        } catch (_: Exception) {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        val isDnd = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val hasVibrator = vibrator?.hasVibrator() == true
        val headphonesConnected = hasBluetoothHeadset || hasWiredHeadset

        val routeLabel = when {
            hasBluetoothHeadset -> "Наушники Bluetooth"
            hasWiredHeadset -> "Проводные / USB наушники"
            isSilentOrVibrate -> "Тихий режим (без динамика)"
            hasBuiltinSpeaker -> "Динамик телефона"
            else -> "Маршрут не определён"
        }

        val effectiveAudioAvailable = settings.soundEnabled &&
            settings.signalVolume > 0.02f &&
            (headphonesConnected || !isSilentOrVibrate)

        val effectiveVibrationAvailable = settings.vibrationEnabled &&
            hasVibrator &&
            ringerMode != AudioManager.RINGER_MODE_SILENT &&
            !isDnd

        val warning = if (!effectiveAudioAvailable && !effectiveVibrationAvailable) {
            "Внимание: ни звук, ни вибрация сейчас недоступны (проверьте тихий режим / DND / настройки). Не используйте «невидимый» режим для тренировки."
        } else {
            null
        }

        return AudioHardwareStatus(
            outputRouteLabelRu = routeLabel,
            isHeadphonesConnected = headphonesConnected,
            ringerModeLabelRu = ringerLabel,
            isSilentOrVibrateRinger = isSilentOrVibrate,
            isDndActive = isDnd,
            hasHardwareVibrator = hasVibrator,
            noActiveSignalChannelWarning = warning
        )
    }

    /**
     * Воспроизводит сигнал с использованием единого тракта настроек для образцов и сессии.
     */
    fun playSignal(
        signal: SignalType,
        settings: BenchSignalSettings,
        packOverride: SoundPack = settings.soundPack,
        metronomeStyleOverride: MetronomeTickStyle = settings.metronomeTickStyle,
        isSamplePreview: Boolean = false
    ) {
        val hw = inspectHardwareStatus(settings)
        val allowSound = settings.soundEnabled &&
            (isSamplePreview || hw.isHeadphonesConnected || !hw.isSilentOrVibrateRinger)

        if (allowSound) {
            val shouldPlay = if (signal == SignalType.METRONOME_TICK) {
                settings.metronomeEnabled || isSamplePreview
            } else {
                true
            }
            if (shouldPlay) {
                // ТЗ п. 8: при совпадении тика и команды пропустить тик, не заглушать команду
                val currentPlaying = activePlayingSignal
                if (signal == SignalType.METRONOME_TICK &&
                    !isSamplePreview &&
                    currentPlaying != null &&
                    currentPlaying != SignalType.METRONOME_TICK
                ) {
                    recordDiagnostic(
                        eventType = "TICK_SKIP",
                        signalLabel = signal.name,
                        packTitle = packOverride.titleRu,
                        isSample = false,
                        effectiveVolume = 0f,
                        pcmRms = 0f,
                        trackGain = 1.0f,
                        outputRoute = hw.outputRouteLabelRu
                    )
                } else {
                    if (signal != SignalType.METRONOME_TICK && currentPlaying == SignalType.METRONOME_TICK) {
                        activePlaybackJob?.cancel()
                    }
                    val effectiveVolume = SoundSynthesisSpec.computeEffectiveVolume(
                        signal = signal,
                        signalVolume = settings.signalVolume,
                        metronomeVolume = settings.metronomeVolume
                    )
                    val pattern = SoundSynthesisSpec.resolveSignalPattern(
                        pack = packOverride,
                        signal = signal,
                        metronomeStyle = metronomeStyleOverride
                    )
                    playPatternPcmSerialized(
                        signal = signal,
                        pack = packOverride,
                        pattern = pattern,
                        effectiveVolume = effectiveVolume,
                        isSamplePreview = isSamplePreview,
                        outputRoute = hw.outputRouteLabelRu
                    )
                }
            }
        }

        // Вибрация: не обходим системный DND
        if (settings.vibrationEnabled && hw.hasHardwareVibrator && !hw.isDndActive) {
            triggerVibration(signal)
        }
    }

    private fun triggerVibration(signal: SignalType) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (signal == SignalType.METRONOME_TICK) return

        val timingsAndAmplitudes: Pair<LongArray, IntArray> = when (signal) {
            SignalType.PREP_TICK -> Pair(
                longArrayOf(0L, 65L),
                intArrayOf(0, 185)
            )
            SignalType.CONTRACTION -> Pair(
                longArrayOf(0L, 120L, 65L, 190L),
                intArrayOf(0, 235, 0, 255)
            )
            SignalType.RELAXATION -> Pair(
                longArrayOf(0L, 260L),
                intArrayOf(0, 200)
            )
            SignalType.PAUSED -> Pair(
                longArrayOf(0L, 95L, 75L, 95L),
                intArrayOf(0, 200, 0, 175)
            )
            SignalType.SET_COMPLETED -> Pair(
                longArrayOf(0L, 100L, 55L, 100L, 55L, 230L),
                intArrayOf(0, 200, 0, 225, 0, 255)
            )
            SignalType.GROUP_COMPLETED -> Pair(
                longArrayOf(0L, 110L, 50L, 110L, 50L, 110L, 65L, 300L),
                intArrayOf(0, 200, 0, 220, 0, 240, 0, 255)
            )
            SignalType.METRONOME_TICK -> return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    timingsAndAmplitudes.first,
                    timingsAndAmplitudes.second,
                    -1
                )
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timingsAndAmplitudes.first, -1)
            }
        } catch (e: Exception) {
            onLog("VIBRATOR", "Ошибка вызова вибрации: ${e.javaClass.simpleName}")
        }
    }

    private fun playPatternPcmSerialized(
        signal: SignalType,
        pack: SoundPack,
        pattern: SignalPatternSpec,
        effectiveVolume: Float,
        isSamplePreview: Boolean,
        outputRoute: String
    ) {
        val pcm = SoundSynthesisSpec.synthesizePcm16(pattern, effectiveVolume)
        if (pcm.isEmpty()) return
        val pcmRms = SoundSynthesisSpec.computeActiveRms(pcm)

        recordDiagnostic(
            eventType = if (isSamplePreview) "SAMPLE" else "SIGNAL",
            signalLabel = signal.name,
            packTitle = pack.titleRu,
            isSample = isSamplePreview,
            effectiveVolume = effectiveVolume,
            pcmRms = pcmRms,
            trackGain = 1.0f,
            outputRoute = outputRoute
        )

        activePlaybackJob = audioScope.launch {
            playbackMutex.withLock {
                // Гарантированно освобождаем предыдущий AudioTrack перед созданием нового,
                // чтобы исключить наложение дорожек и системный лимитер
                stopActiveTrackImmediate()
                try {
                    val sampleRate = SoundSynthesisSpec.SAMPLE_RATE_HZ
                    val audioTrack = AudioTrack.Builder()
                        .setAudioAttributes(buildUnifiedAudioAttributes())
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcm.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()

                    // Явно фиксируем единичное усиление AudioTrack (1.0f); уровень громкости уже заложен в PCM
                    audioTrack.setVolume(1.0f)
                    audioTrack.write(pcm, 0, pcm.size)
                    activeAudioTrack = audioTrack
                    activePlayingSignal = signal
                    audioTrack.play()
                    delay(pattern.totalDurationMs.toLong() + 35L)
                } catch (_: Exception) {
                    // В JVM/Robolectric тестах, при прерывании тика командой или без аудиоустройства
                } finally {
                    stopActiveTrackImmediate()
                }
            }
        }
    }

    private fun recordDiagnostic(
        eventType: String,
        signalLabel: String,
        packTitle: String,
        isSample: Boolean,
        effectiveVolume: Float,
        pcmRms: Float,
        trackGain: Float,
        outputRoute: String
    ) {
        val entry = AudioDiagnosticEntry(
            elapsedMs = SystemClock.elapsedRealtime(),
            eventType = eventType,
            signalLabel = signalLabel,
            packTitle = packTitle,
            isSample = isSample,
            effectiveVolume = effectiveVolume,
            pcmRms = pcmRms,
            trackGain = trackGain,
            focusHeld = hasAudioFocus,
            outputRoute = outputRoute
        )
        _audioDiagnostics.value = (listOf(entry) + _audioDiagnostics.value).take(40)
    }

    fun clearAudioDiagnostics() {
        _audioDiagnostics.value = emptyList()
    }
}
