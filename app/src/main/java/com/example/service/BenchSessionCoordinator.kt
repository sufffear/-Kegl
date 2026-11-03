package com.example.service

import android.content.Context
import android.os.SystemClock
import com.example.data.AudioHardwareStatus
import com.example.data.AudioVibrationEngine
import com.example.data.BenchSignalSettings
import com.example.domain.BenchEventLogEntry
import com.example.domain.DEFAULT_DEMO_PROTOCOLS
import com.example.domain.DemoProtocolConfig
import com.example.domain.PhaseEngine
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.SignalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Единый координатор сессии стенда 0B.
 * Хранит единственный экземпляр PhaseEngine, лог событий без личных данных и состояние аппаратных каналов,
 * чтобы пересоздание Activity или сворачивание экрана не создавало второй таймер и не теряло текущий.
 */
object BenchSessionCoordinator {

    private val logIdCounter = AtomicLong(1L)
    private var appContext: Context? = null
    private var audioVibrationEngine: AudioVibrationEngine? = null

    private val _settings = MutableStateFlow(BenchSignalSettings())
    val settings: StateFlow<BenchSignalSettings> = _settings.asStateFlow()

    private val _hardwareStatus = MutableStateFlow(AudioHardwareStatus())
    val hardwareStatus: StateFlow<AudioHardwareStatus> = _hardwareStatus.asStateFlow()

    private val _eventLog = MutableStateFlow<List<BenchEventLogEntry>>(emptyList())
    val eventLog: StateFlow<List<BenchEventLogEntry>> = _eventLog.asStateFlow()

    private val _isWakeLockHeld = MutableStateFlow(false)
    val isWakeLockHeld: StateFlow<Boolean> = _isWakeLockHeld.asStateFlow()

    private val _isServiceForeground = MutableStateFlow(false)
    val isServiceForeground: StateFlow<Boolean> = _isServiceForeground.asStateFlow()

    private val phaseEngine = PhaseEngine(
        clock = { SystemClock.elapsedRealtime() },
        onSignal = { signal ->
            audioVibrationEngine?.playSignal(
                signal = signal,
                settings = _settings.value,
                isSamplePreview = false
            )
        },
        onLog = { category, message ->
            appendLog(category, message)
        }
    )

    private val _engineSnapshot = MutableStateFlow(phaseEngine.currentSnapshot())
    val engineSnapshot: StateFlow<PhaseEngineSnapshot> = _engineSnapshot.asStateFlow()

    val availableProtocols: List<DemoProtocolConfig> = DEFAULT_DEMO_PROTOCOLS

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (appContext == null) {
            val appCtx = context.applicationContext
            appContext = appCtx
            audioVibrationEngine = AudioVibrationEngine(
                context = appCtx,
                onAudioFocusLost = { reasonRu ->
                    onExternalInterruption(reasonRu)
                },
                onLog = { category, message ->
                    appendLog(category, message)
                }
            )
            refreshHardwareStatus()
            appendLog(
                "INIT",
                "Инициализирован технический стенд 0B (Демо-режим, версия 0.2.0-0b, versionCode 2)"
            )
        }
    }

    fun refreshHardwareStatus() {
        val engine = audioVibrationEngine ?: return
        _hardwareStatus.value = engine.inspectHardwareStatus(_settings.value)
    }

    fun updateSettings(transform: (BenchSignalSettings) -> BenchSignalSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        phaseEngine.isMetronomeEnabled = updated.metronomeEnabled
        refreshHardwareStatus()
    }

    fun playSampleSignal(signal: SignalType) {
        refreshHardwareStatus()
        appendLog("SAMPLE", "Проверка образца сигнала: ${signal.labelRu}")
        audioVibrationEngine?.playSignal(
            signal = signal,
            settings = _settings.value,
            isSamplePreview = true
        )
    }

    @Synchronized
    fun selectProtocol(protocol: DemoProtocolConfig) {
        if (phaseEngine.selectProtocolIfIdle(protocol)) {
            _engineSnapshot.value = phaseEngine.currentSnapshot()
        }
    }

    @Synchronized
    fun startDemoSession(): Boolean {
        refreshHardwareStatus()
        val hw = _hardwareStatus.value
        if (_settings.value.soundEnabled && (!hw.isSilentOrVibrateRinger || hw.isHeadphonesConnected)) {
            val focusGranted = audioVibrationEngine?.requestSessionAudioFocus() ?: true
            if (!focusGranted) {
                appendLog(
                    "WARN",
                    "Аудиофокус недоступен при старте (активен звонок или внешний поток). Старт со звуком отклонён."
                )
                return false
            }
        }
        val started = phaseEngine.start()
        _engineSnapshot.value = phaseEngine.currentSnapshot()
        return started
    }

    @Synchronized
    fun onServiceTick(): PhaseEngineSnapshot {
        val snap = phaseEngine.tick()
        _engineSnapshot.value = snap
        return snap
    }

    @Synchronized
    fun pauseSession(reason: String = "Пауза нажата вручную"): PhaseEngineSnapshot {
        val snap = phaseEngine.pause(reason)
        _engineSnapshot.value = snap
        audioVibrationEngine?.abandonSessionAudioFocus()
        return snap
    }

    @Synchronized
    fun onExternalInterruption(reason: String): PhaseEngineSnapshot {
        val snap = phaseEngine.interrupt(reason)
        _engineSnapshot.value = snap
        audioVibrationEngine?.abandonSessionAudioFocus()
        return snap
    }

    @Synchronized
    fun resumeSession(): Boolean {
        refreshHardwareStatus()
        val hw = _hardwareStatus.value
        if (_settings.value.soundEnabled && (!hw.isSilentOrVibrateRinger || hw.isHeadphonesConnected)) {
            val focusGranted = audioVibrationEngine?.requestSessionAudioFocus() ?: true
            if (!focusGranted) {
                appendLog(
                    "WARN",
                    "Не удалось возобновить со звуком: аудиофокус занят звонком или внешним приложением."
                )
                return false
            }
        }
        val resumed = phaseEngine.resume()
        _engineSnapshot.value = phaseEngine.currentSnapshot()
        return resumed
    }

    @Synchronized
    fun requestStopWithConfirmation(): PhaseEngineSnapshot {
        val snap = phaseEngine.requestStop()
        _engineSnapshot.value = snap
        audioVibrationEngine?.abandonSessionAudioFocus()
        return snap
    }

    @Synchronized
    fun cancelStopConfirmation(): PhaseEngineSnapshot {
        val snap = phaseEngine.cancelStopDialog()
        _engineSnapshot.value = snap
        return snap
    }

    @Synchronized
    fun confirmStopWithoutCredit(): PhaseEngineSnapshot {
        val snap = phaseEngine.confirmStop()
        _engineSnapshot.value = snap
        audioVibrationEngine?.abandonSessionAudioFocus()
        return snap
    }

    @Synchronized
    fun resetToIdle(): PhaseEngineSnapshot {
        val snap = phaseEngine.resetToIdle()
        _engineSnapshot.value = snap
        audioVibrationEngine?.abandonSessionAudioFocus()
        return snap
    }

    fun setWakeLockHeld(held: Boolean) {
        if (_isWakeLockHeld.value != held) {
            _isWakeLockHeld.value = held
            appendLog(
                "WAKELOCK",
                if (held) "PARTIAL_WAKE_LOCK захвачен на время активных фаз" else "PARTIAL_WAKE_LOCK освобождён"
            )
        }
    }

    fun setServiceForeground(active: Boolean) {
        if (_isServiceForeground.value != active) {
            _isServiceForeground.value = active
            appendLog(
                "FGS",
                if (active) "Foreground Service (specialUse) активен" else "Foreground Service остановлен"
            )
        }
    }

    fun appendLog(category: String, message: String) {
        val entry = BenchEventLogEntry(
            id = logIdCounter.getAndIncrement(),
            elapsedMs = SystemClock.elapsedRealtime(),
            category = category,
            message = message
        )
        val current = _eventLog.value
        _eventLog.value = (listOf(entry) + current).take(80)
    }

    fun clearLog() {
        _eventLog.value = emptyList()
        appendLog("LOG", "Журнал событий очищен пользователем")
    }
}
