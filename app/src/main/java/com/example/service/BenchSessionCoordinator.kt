package com.example.service

import android.content.Context
import android.os.SystemClock
import com.example.data.AudioHardwareStatus
import com.example.data.AudioVibrationEngine
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.data.PreferencesRepository
import com.example.data.VelarisDatabase
import com.example.data.WorkoutPersistenceRepository
import com.example.domain.BenchEventLogEntry
import com.example.domain.CheckpointTrigger
import com.example.domain.DEFAULT_DEMO_PROTOCOLS
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngine
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.PlanGroup
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.domain.SystemTimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Единый координатор сессии Velaris (Этап 0B + Этап 2: Room, контрольные точки, DataStore и звуковые наборы).
 * Хранит единственный экземпляр PhaseEngine, синхронизирует контрольные точки и завершения с Room,
 * сохраняет пользовательские настройки сигналов в DataStore и восстанавливает прерванную попытку
 * после смерти процесса без начисления времени простоя.
 */
object BenchSessionCoordinator {

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logIdCounter = AtomicLong(1L)
    private val timeProvider = SystemTimeProvider()

    private var appContext: Context? = null
    private var audioVibrationEngine: AudioVibrationEngine? = null
    private var preferencesRepository: PreferencesRepository? = null
    private var persistenceRepository: WorkoutPersistenceRepository? = null

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

    private val _dailySlots = MutableStateFlow<List<DailySlotWithCompletion>>(emptyList())
    val dailySlots: StateFlow<List<DailySlotWithCompletion>> = _dailySlots.asStateFlow()

    private val _isZeroSlotDemoMode = MutableStateFlow(false)
    val isZeroSlotDemoMode: StateFlow<Boolean> = _isZeroSlotDemoMode.asStateFlow()

    private val _demoCompletionsCount = MutableStateFlow(0)
    val demoCompletionsCount: StateFlow<Int> = _demoCompletionsCount.asStateFlow()

    private val _realCompletionsCount = MutableStateFlow(0)
    val realCompletionsCount: StateFlow<Int> = _realCompletionsCount.asStateFlow()

    private val _databaseErrorMessage = MutableStateFlow<String?>(null)
    val databaseErrorMessage: StateFlow<String?> = _databaseErrorMessage.asStateFlow()

    private val phaseEngine = PhaseEngine(
        clock = { SystemClock.elapsedRealtime() },
        onSignal = { signal, packSnapshot, metronomeSnapshot ->
            audioVibrationEngine?.playSignal(
                signal = signal,
                settings = _settings.value,
                isSamplePreview = false,
                packOverride = packSnapshot,
                metronomeStyleOverride = metronomeSnapshot
            )
        },
        onLog = { category, message ->
            appendLog(category, message)
        },
        onCheckpoint = { snapshot, trigger ->
            persistCheckpointAsync(snapshot, trigger)
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

            val prefs = PreferencesRepository(appCtx)
            preferencesRepository = prefs

            val db = VelarisDatabase.getInstance(appCtx)
            val repo = WorkoutPersistenceRepository(db.velarisDao(), timeProvider)
            persistenceRepository = repo

            coordinatorScope.launch {
                prefs.signalSettingsFlow.collect { loadedSettings ->
                    _settings.value = loadedSettings
                    phaseEngine.isMetronomeEnabled = loadedSettings.metronomeEnabled
                    refreshHardwareStatus()
                }
            }

            coordinatorScope.launch {
                initializePersistenceAndRecover(repo)
            }

            refreshHardwareStatus()
            appendLog(
                "INIT",
                "Инициализирован стенд этапа 2 + плавный таймер + звуковые наборы (версия 0.3.0-s2, versionCode 3)"
            )
        }
    }

    private suspend fun initializePersistenceAndRecover(repo: WorkoutPersistenceRepository) {
        try {
            val slots = repo.getOrCreateDailySlots(
                localDate = activeDateKey(),
                planId = activePlanId(),
                isDemo = true
            )
            _dailySlots.value = slots
            _demoCompletionsCount.value = repo.countDemoCompletions()
            _realCompletionsCount.value = repo.countRealCompletions()

            // Синхронизируем выбранный протокол с первым незавершённым слотом дня (если двигатель в IDLE)
            val firstTargetSlot = slots.firstOrNull { !it.isCompleted }?.slot ?: slots.firstOrNull()?.slot
            if (firstTargetSlot != null) {
                val proto = repo.slotToProtocolConfig(firstTargetSlot)
                synchronized(this@BenchSessionCoordinator) {
                    if (phaseEngine.selectProtocolIfIdle(proto)) {
                        _engineSnapshot.value = phaseEngine.currentSnapshot()
                    }
                }
            }

            // Проверяем, не осталась ли прерванная попытка после выгрузки процесса или перезагрузки
            val recovered = repo.recoverInterruptedRunAfterProcessDeath()
            if (recovered != null) {
                val restoredProto = DemoProtocolConfig(
                    id = recovered.slotId,
                    titleRu = recovered.titleRu,
                    contractionMs = recovered.contractionMs,
                    relaxationMs = recovered.relaxationMs,
                    repetitions = recovered.repetitions,
                    isLastInGroup = recovered.isLastInGroup,
                    group = PlanGroup.fromCode(recovered.groupCode)
                )
                val restoredPhase = runCatching { PhaseType.valueOf(recovered.currentPhase) }
                    .getOrDefault(PhaseType.CONTRACTION)
                synchronized(this@BenchSessionCoordinator) {
                    val snap = phaseEngine.restoreInterruptedFromCheckpoint(
                        protocol = restoredProto,
                        runId = recovered.id,
                        slotId = recovered.slotId,
                        startDate = recovered.startDate,
                        zoneId = recovered.zoneId,
                        isDemo = recovered.isDemo,
                        soundPackSnapshot = SoundPack.fromId(recovered.soundPackIdSnapshot),
                        metronomeStyleSnapshot = MetronomeTickStyle.fromId(recovered.metronomeStyleIdSnapshot),
                        currentRepetition = recovered.currentRepetition,
                        currentPhase = restoredPhase,
                        phaseRemainingMs = recovered.phaseRemainingMs,
                        confirmedActiveMs = recovered.confirmedActiveMs,
                        interruptionReason = recovered.interruptionReason
                            ?: "Восстановлена прерванная попытка без начисления времени простоя"
                    )
                    _engineSnapshot.value = snap
                }
            }
            _databaseErrorMessage.value = null
        } catch (e: Exception) {
            val errMsg = "Ошибка локальной базы данных: ${e.javaClass.simpleName}. Данные сессии защищены в памяти."
            _databaseErrorMessage.value = errMsg
            appendLog("DB_ERR", errMsg)
        }
    }

    private fun activeDateKey(): String {
        val today = timeProvider.currentLocalDate()
        return if (_isZeroSlotDemoMode.value) "${today}_rest" else today
    }

    private fun activePlanId(): String {
        return if (_isZeroSlotDemoMode.value) {
            WorkoutPersistenceRepository.DEMO_PLAN_ZERO_SLOTS
        } else {
            WorkoutPersistenceRepository.DEMO_PLAN_STANDARD_3
        }
    }

    fun setZeroSlotDemoMode(enabled: Boolean) {
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            appendLog("PLAN", "Нельзя переключать режим плана во время активной или приостановленной попытки")
            return
        }
        _isZeroSlotDemoMode.value = enabled
        appendLog(
            "PLAN",
            if (enabled) {
                "Включён тестовый режим N=0 («Сегодня без тренировок»)"
            } else {
                "Включён стандартный демо-план дня (3 подхода)"
            }
        )
        refreshDailySlotsAsync()
    }

    fun refreshDailySlotsAsync() {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            try {
                val slots = repo.getOrCreateDailySlots(
                    localDate = activeDateKey(),
                    planId = activePlanId(),
                    isDemo = true
                )
                _dailySlots.value = slots
                _demoCompletionsCount.value = repo.countDemoCompletions()
                _realCompletionsCount.value = repo.countRealCompletions()

                // Если текущий выбранный протокол не входит в список слотов и двигатель свободен, выбираем первый доступный
                if (slots.isNotEmpty() && !phaseEngine.currentSnapshot().isActiveOrPaused) {
                    val currentId = phaseEngine.currentSnapshot().protocol.id
                    if (slots.none { it.slot.id == currentId }) {
                        val target = slots.firstOrNull { !it.isCompleted }?.slot ?: slots.first().slot
                        val proto = repo.slotToProtocolConfig(target)
                        synchronized(this@BenchSessionCoordinator) {
                            if (phaseEngine.selectProtocolIfIdle(proto)) {
                                _engineSnapshot.value = phaseEngine.currentSnapshot()
                            }
                        }
                    }
                }
                _databaseErrorMessage.value = null
            } catch (e: Exception) {
                val errMsg = "Не удалось обновить слоты плана: ${e.javaClass.simpleName}"
                _databaseErrorMessage.value = errMsg
                appendLog("DB_ERR", errMsg)
            }
        }
    }

    private fun persistCheckpointAsync(snapshot: PhaseEngineSnapshot, trigger: CheckpointTrigger) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            try {
                if (trigger == CheckpointTrigger.COMPLETE) {
                    val inserted = repo.completeRunAtomically(snapshot)
                    if (inserted) {
                        appendLog(
                            "DB_COMMIT",
                            "Атомарный зачёт подхода сохранён в Room: slotId=${snapshot.slotId}, дата=${snapshot.startDate}"
                        )
                    } else {
                        appendLog(
                            "DB_DEDUP",
                            "Повторный зачёт слота ${snapshot.slotId} заблокирован уникальным ограничением Room"
                        )
                    }
                    val slots = repo.getOrCreateDailySlots(
                        localDate = activeDateKey(),
                        planId = activePlanId(),
                        isDemo = true
                    )
                    _dailySlots.value = slots
                    _demoCompletionsCount.value = repo.countDemoCompletions()
                    _realCompletionsCount.value = repo.countRealCompletions()
                } else {
                    repo.saveRunCheckpoint(snapshot)
                }
                _databaseErrorMessage.value = null
            } catch (e: Exception) {
                val errMsg = "Ошибка записи контрольной точки (${trigger.name}): ${e.javaClass.simpleName}"
                _databaseErrorMessage.value = errMsg
                appendLog("DB_ERR", errMsg)
            }
        }
    }

    fun refreshHardwareStatus() {
        val engine = audioVibrationEngine ?: return
        _hardwareStatus.value = engine.inspectHardwareStatus(_settings.value)
    }

    fun updateSettings(transform: (BenchSignalSettings) -> BenchSignalSettings) {
        val previous = _settings.value
        val updated = transform(previous)
        _settings.value = updated
        phaseEngine.isMetronomeEnabled = updated.metronomeEnabled
        refreshHardwareStatus()

        if (previous.soundPack != updated.soundPack || previous.metronomeTickStyle != updated.metronomeTickStyle) {
            val activeNote = if (_engineSnapshot.value.isActiveOrPaused) {
                " (применится со следующего запуска подхода)"
            } else {
                ""
            }
            appendLog(
                "SOUND_PACK",
                "Выбран набор «${updated.soundPack.titleRu}», метроном «${updated.metronomeTickStyle.titleRu}»$activeNote"
            )
        }

        val prefs = preferencesRepository
        if (prefs != null) {
            coordinatorScope.launch {
                try {
                    prefs.updateSettings(updated)
                } catch (e: Exception) {
                    appendLog("PREFS_ERR", "Не удалось сохранить настройки сигналов: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    fun playSampleSignal(signal: SignalType) {
        if (_engineSnapshot.value.isRunningOrPreparing) {
            appendLog(
                "SAMPLE_SKIP",
                "Прослушивание образцов отключено во время активной фазы, чтобы не мешать командам подхода"
            )
            return
        }
        refreshHardwareStatus()
        appendLog(
            "SAMPLE",
            "Образец «${signal.labelRu}» (набор «${_settings.value.soundPack.titleRu}»)"
        )
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
    fun selectDailySlot(slotWithCompletion: DailySlotWithCompletion) {
        val repo = persistenceRepository ?: return
        val proto = repo.slotToProtocolConfig(slotWithCompletion.slot)
        if (phaseEngine.selectProtocolIfIdle(proto)) {
            _engineSnapshot.value = phaseEngine.currentSnapshot()
            appendLog("SLOT", "Выбран слот плана: ${slotWithCompletion.slot.titleRu}")
        }
    }

    @Synchronized
    fun startDemoSession(): Boolean {
        if (_isZeroSlotDemoMode.value && _dailySlots.value.isEmpty()) {
            appendLog("WARN", "Сегодня день без тренировок (0 подходов в плане). Запуск подхода не требуется.")
            return false
        }

        val currentProto = phaseEngine.currentSnapshot().protocol
        val matchingSlot = _dailySlots.value.firstOrNull { it.slot.id == currentProto.id }
            ?: _dailySlots.value.firstOrNull { !it.isCompleted }

        if (matchingSlot != null && matchingSlot.isCompleted) {
            appendLog(
                "GUARD",
                "Слот «${matchingSlot.slot.titleRu}» уже выполнен сегодня. Повторный запуск заблокирован (ТЗ п. 5, 9)."
            )
            return false
        }

        if (matchingSlot != null && matchingSlot.slot.id != currentProto.id) {
            val repo = persistenceRepository
            if (repo != null) {
                phaseEngine.selectProtocolIfIdle(repo.slotToProtocolConfig(matchingSlot.slot))
            }
        }

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

        val slotIdToUse = matchingSlot?.slot?.id ?: phaseEngine.currentSnapshot().protocol.id
        val runId = "run_${UUID.randomUUID()}"
        val startDate = timeProvider.currentLocalDate()
        val zoneId = timeProvider.currentZoneId()
        val currentSettings = _settings.value

        val started = phaseEngine.start(
            runId = runId,
            slotId = slotIdToUse,
            startDate = startDate,
            zoneId = zoneId,
            isDemo = true,
            soundPackSnapshot = currentSettings.soundPack,
            metronomeStyleSnapshot = currentSettings.metronomeTickStyle
        )
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
