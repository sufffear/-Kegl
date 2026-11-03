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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

data class AudioHardwareStatus(
    val outputRouteLabelRu: String = "Динамик",
    val isHeadphonesConnected: Boolean = false,
    val ringerModeLabelRu: String = "Звук включён",
    val isSilentOrVibrateRinger: Boolean = false,
    val isDndActive: Boolean = false,
    val hasHardwareVibrator: Boolean = true,
    val noActiveSignalChannelWarning: String? = null
)

data class BenchSignalSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val metronomeEnabled: Boolean = true,
    val signalVolume: Float = 0.80f,
    val metronomeVolume: Float = 0.35f
)

/**
 * Управляет синтезом звуковых сигналов (через PCM AudioTrack без изменения системной громкости),
 * вибрацией (VibrationEffect), проверкой маршрута аудиовыхода и аудиофокусом.
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

    private var currentFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                val codeName = when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> "AUDIOFOCUS_LOSS"
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "AUDIOFOCUS_LOSS_TRANSIENT"
                    else -> "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
                }
                onAudioFocusLost("Потеря аудиофокуса ($codeName: звонок или внешнее медиа)")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // По ТЗ п. 7: без автовозобновления после возврата аудиофокуса!
                hasAudioFocus = true
                onLog("AUDIO_FOCUS", "Аудиофокус возвращён системой (AUDIOFOCUS_GAIN), автовозобновление отключено")
            }
        }
    }

    fun requestSessionAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
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
        onLog(
            "AUDIO_FOCUS",
            if (hasAudioFocus) "Аудиофокус успешно получен" else "Отказ в получении аудиофокуса (возможно, активен звонок)"
        )
        return hasAudioFocus
    }

    fun abandonSessionAudioFocus() {
        try {
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

    /**
     * Проверяет фактический аудиомаршрут, режим звонка, DND и наличие вибромотора.
     * Не показывает ложного подтверждения наушников, если маршрут не определён.
     */
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
            "Внимание: ни звук, ни вибрация сейчас недоступны (проверьте тихий режим / DND / настройки стенда). Не используйте «невидимый» режим для тренировки."
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
     * Воспроизводит сигнал (и в рабочем цикле, и по нажатию кнопки образца в настройках стенда).
     */
    fun playSignal(
        signal: SignalType,
        settings: BenchSignalSettings,
        isSamplePreview: Boolean = false
    ) {
        val hw = inspectHardwareStatus(settings)
        val allowSound = settings.soundEnabled &&
            (isSamplePreview || hw.isHeadphonesConnected || !hw.isSilentOrVibrateRinger)

        if (allowSound) {
            when (signal) {
                SignalType.METRONOME_TICK -> {
                    if (settings.metronomeEnabled || isSamplePreview) {
                        playSynthesizedToneAsync(
                            frequenciesHz = doubleArrayOf(520.0),
                            durationsMs = intArrayOf(35),
                            volume = settings.metronomeVolume.coerceIn(0f, 1f)
                        )
                    }
                }
                SignalType.PREP_TICK -> {
                    playSynthesizedToneAsync(
                        frequenciesHz = doubleArrayOf(660.0),
                        durationsMs = intArrayOf(75),
                        volume = settings.signalVolume.coerceIn(0f, 1f)
                    )
                }
                SignalType.CONTRACTION -> {
                    // Восходящее двузвучие («Сокращение»)
                    playSynthesizedToneAsync(
                        frequenciesHz = doubleArrayOf(587.33, 880.0),
                        durationsMs = intArrayOf(110, 160),
                        volume = settings.signalVolume.coerceIn(0f, 1f)
                    )
                }
                SignalType.RELAXATION -> {
                    // Спокойное нисходящее двузвучие («Расслабление»)
                    playSynthesizedToneAsync(
                        frequenciesHz = doubleArrayOf(659.25, 440.0),
                        durationsMs = intArrayOf(120, 190),
                        volume = settings.signalVolume.coerceIn(0f, 1f)
                    )
                }
                SignalType.PAUSED -> {
                    playSynthesizedToneAsync(
                        frequenciesHz = doubleArrayOf(440.0, 349.23),
                        durationsMs = intArrayOf(90, 130),
                        volume = settings.signalVolume.coerceIn(0f, 1f)
                    )
                }
                SignalType.SET_COMPLETED -> {
                    // Трёхнотный мягкий аккорд завершения подхода
                    playSynthesizedToneAsync(
                        frequenciesHz = doubleArrayOf(523.25, 659.25, 783.99),
                        durationsMs = intArrayOf(110, 110, 220),
                        volume = settings.signalVolume.coerceIn(0f, 1f)
                    )
                }
                SignalType.GROUP_COMPLETED -> {
                    // Четырёхнотный финальный сигнал завершения группы (заменяет обычный сигнал подхода)
                    playSynthesizedToneAsync(
                        frequenciesHz = doubleArrayOf(523.25, 659.25, 783.99, 1046.50),
                        durationsMs = intArrayOf(100, 100, 120, 280),
                        volume = settings.signalVolume.coerceIn(0f, 1f)
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

        // Для тика метронома вибрацию не подаём, чтобы не разряжать телефон и не забивать основные команды
        if (signal == SignalType.METRONOME_TICK) return

        val timingsAndAmplitudes: Pair<LongArray, IntArray> = when (signal) {
            SignalType.PREP_TICK -> Pair(
                longArrayOf(0L, 60L),
                intArrayOf(0, 140)
            )
            SignalType.CONTRACTION -> Pair(
                // Чёткий двойной импульс на сокращение
                longArrayOf(0L, 120L, 70L, 180L),
                intArrayOf(0, 220, 0, 255)
            )
            SignalType.RELAXATION -> Pair(
                // Один плавный длинный импульс на расслабление
                longArrayOf(0L, 280L),
                intArrayOf(0, 150)
            )
            SignalType.PAUSED -> Pair(
                longArrayOf(0L, 90L, 80L, 90L),
                intArrayOf(0, 180, 0, 150)
            )
            SignalType.SET_COMPLETED -> Pair(
                longArrayOf(0L, 100L, 60L, 100L, 60L, 220L),
                intArrayOf(0, 180, 0, 200, 0, 255)
            )
            SignalType.GROUP_COMPLETED -> Pair(
                longArrayOf(0L, 110L, 50L, 110L, 50L, 110L, 70L, 300L),
                intArrayOf(0, 180, 0, 200, 0, 220, 0, 255)
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

    private fun playSynthesizedToneAsync(
        frequenciesHz: DoubleArray,
        durationsMs: IntArray,
        volume: Float
    ) {
        if (volume <= 0.01f) return
        audioScope.launch {
            try {
                val sampleRate = 24_000
                val totalMs = durationsMs.sum() + (durationsMs.size - 1).coerceAtLeast(0) * 20
                val totalSamples = (sampleRate * totalMs) / 1000
                val pcm = ShortArray(totalSamples)

                var writeIndex = 0
                for (i in frequenciesHz.indices) {
                    val freq = frequenciesHz[i]
                    val durMs = durationsMs[i]
                    val numSamples = (sampleRate * durMs) / 1000
                    val fadeSamples = (numSamples / 6).coerceAtLeast(1)

                    for (s in 0 until numSamples) {
                        if (writeIndex >= pcm.size) break
                        val env = when {
                            s < fadeSamples -> s.toFloat() / fadeSamples
                            s > numSamples - fadeSamples -> (numSamples - s).toFloat() / fadeSamples
                            else -> 1.0f
                        }
                        val angle = 2.0 * PI * s * freq / sampleRate
                        val sampleValue = (sin(angle) * volume * env * Short.MAX_VALUE * 0.75).toInt()
                        pcm[writeIndex++] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    // Небольшая пауза 20 мс между нотами
                    val gapSamples = (sampleRate * 20) / 1000
                    writeIndex = (writeIndex + gapSamples).coerceAtMost(pcm.size)
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
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

                audioTrack.write(pcm, 0, pcm.size)
                audioTrack.play()
                kotlinx.coroutines.delay(totalMs.toLong() + 60L)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // В JVM/Robolectric тестах или без аудиоустройства игнорируем ошибку AudioTrack
            }
        }
    }
}
