package com.example.service

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import com.example.data.AudioDiagnosticEntry
import com.example.data.AudioHardwareStatus
import com.example.data.AudioVibrationEngine
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.data.PreferencesRepository
import com.example.data.StoryReaderUiState
import com.example.data.StoryRepository
import com.example.data.VelarisDatabase
import com.example.data.WeeklyOverviewState
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
import com.example.domain.WorkoutDifficulty
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
 * Единый координатор сессии Velaris (Этап 0B + Этап 2 + Итерация v1.1).
 * Хранит единственный экземпляр PhaseEngine, синхронизирует контрольные точки и завершения с Room,
 * управляет 7-дневным обзором реальных записей, офлайн-читалкой истории, аудио-диагностикой и настройками DataStore.
 */
object BenchSessionCoordinator {

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logIdCounter = AtomicLong(1L)
    private val timeProvider = SystemTimeProvider()

    private var appContext: Context? = null
    private var audioVibrationEngine: AudioVibrationEngine? = null
    private var preferencesRepository: PreferencesRepository? = null
    private var persistenceRepository: WorkoutPersistenceRepository? = null
    private var storyRepository: StoryRepository? = null

    private val _settings = MutableStateFlow(BenchSignalSettings())
    val settings: StateFlow<BenchSignalSettings> = _settings.asStateFlow()

    private val _hardwareStatus = MutableStateFlow(AudioHardwareStatus())
    val hardwareStatus: StateFlow<AudioHardwareStatus> = _hardwareStatus.asStateFlow()

    private val _audioDiagnostics = MutableStateFlow<List<AudioDiagnosticEntry>>(emptyList())
    val audioDiagnostics: StateFlow<List<AudioDiagnosticEntry>> = _audioDiagnostics.asStateFlow()

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

    private val _weeklyOverview = MutableStateFlow(WeeklyOverviewState())
    val weeklyOverview: StateFlow<WeeklyOverviewState> = _weeklyOverview.asStateFlow()

    private val _storyState = MutableStateFlow(StoryReaderUiState())
    val storyState: StateFlow<StoryReaderUiState> = _storyState.asStateFlow()

    private val _lastCompletedSlotDifficulty = MutableStateFlow<WorkoutDifficulty?>(null)
    val lastCompletedSlotDifficulty: StateFlow<WorkoutDifficulty?> = _lastCompletedSlotDifficulty.asStateFlow()

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
            val audioEngine = AudioVibrationEngine(
                context = appCtx,
                onAudioFocusLost = { reasonRu ->
                    onExternalInterruption(reasonRu)
                },
                onLog = { category, message ->
                    appendLog(category, message)
                }
            )
            audioVibrationEngine = audioEngine

            val prefs = PreferencesRepository(appCtx)
            preferencesRepository = prefs

            val db = VelarisDatabase.getInstance(appCtx)
            val repo = WorkoutPersistenceRepository(db.velarisDao(), timeProvider)
            persistenceRepository = repo

            val storyRepo = StoryRepository(appCtx, db.velarisDao(), timeProvider)
            storyRepository = storyRepo

            coordinatorScope.launch {
                audioEngine.audioDiagnostics.collect { list ->
                    _audioDiagnostics.value = list
                }
            }

            coordinatorScope.launch {
                prefs.signalSettingsFlow.collect { loadedSettings ->
                    val prevRealEnabled = _settings.value.isRealProgramEnabled
                    _settings.value = loadedSettings
                    phaseEngine.isMetronomeEnabled = loadedSettings.metronomeEnabled
                    refreshHardwareStatus()
                    if (prevRealEnabled != loadedSettings.isRealProgramEnabled) {
                        refreshDailySlotsAsync()
                    }
                }
            }

            coordinatorScope.launch {
                initializePersistenceAndRecover(repo, storyRepo)
            }

            refreshHardwareStatus()
            appendLog(
                "INIT",
                "Инициализировано приложение Velaris v0.4.0-v1.1 (versionCode 4): стабилизация громкости, полный интерфейс, 7-дневный обзор и офлайн-читалка"
            )
        }
    }

    private suspend fun initializePersistenceAndRecover(
        repo: WorkoutPersistenceRepository,
        storyRepo: StoryRepository
    ) {
        try {
            val isDemo = isCurrentModeDemo()
            val slots = repo.getOrCreateDailySlots(
                localDate = activeDateKey(),
                planId = activePlanId(),
                isDemo = isDemo
            )
            _dailySlots.value = slots
            _demoCompletionsCount.value = repo.countDemoCompletions()
            _realCompletionsCount.value = repo.countRealCompletions()
            _weeklyOverview.value = repo.loadWeeklyOverview(
                todayDateStr = timeProvider.currentLocalDate(),
                selectedDateStr = _weeklyOverview.value.selectedDate.ifEmpty { null }
            )

            // Загружаем офлайн-читалку независимо (ошибки читалки изолированы внутри StoryRepository)
            _storyState.value = storyRepo.loadReaderState()

            val firstTargetSlot = slots.firstOrNull { !it.isCompleted }?.slot ?: slots.firstOrNull()?.slot
            if (firstTargetSlot != null) {
                val proto = repo.slotToProtocolConfig(firstTargetSlot)
                synchronized(this@BenchSessionCoordinator) {
                    if (phaseEngine.selectProtocolIfIdle(proto)) {
                        _engineSnapshot.value = phaseEngine.currentSnapshot()
                    }
                }
            }

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

    private fun isCurrentModeDemo(): Boolean {
        return _isZeroSlotDemoMode.value || !_settings.value.isRealProgramEnabled
    }

    private fun activeDateKey(): String {
        val today = timeProvider.currentLocalDate()
        return if (_isZeroSlotDemoMode.value) "${today}_rest" else today
    }

    private fun activePlanId(): String {
        return when {
            _isZeroSlotDemoMode.value -> WorkoutPersistenceRepository.DEMO_PLAN_ZERO_SLOTS
            _settings.value.isRealProgramEnabled -> WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3
            else -> WorkoutPersistenceRepository.DEMO_PLAN_STANDARD_3
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
                "Включён план дня (3 подхода)"
            }
        )
        refreshDailySlotsAsync()
    }

    fun refreshDailySlotsAsync() {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            try {
                val isDemo = isCurrentModeDemo()
                val slots = repo.getOrCreateDailySlots(
                    localDate = activeDateKey(),
                    planId = activePlanId(),
                    isDemo = isDemo
                )
                _dailySlots.value = slots
                _demoCompletionsCount.value = repo.countDemoCompletions()
                _realCompletionsCount.value = repo.countRealCompletions()
                _weeklyOverview.value = repo.loadWeeklyOverview(
                    todayDateStr = timeProvider.currentLocalDate(),
                    selectedDateStr = _weeklyOverview.value.selectedDate.ifEmpty { null }
                )

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

    fun selectWeeklyOverviewDate(localDate: String) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            runCatching {
                _weeklyOverview.value = repo.loadWeeklyOverview(
                    todayDateStr = timeProvider.currentLocalDate(),
                    selectedDateStr = localDate
                )
            }
        }
    }

    fun rateLastCompletedSlotDifficulty(difficulty: WorkoutDifficulty?) {
        val slotId = _engineSnapshot.value.slotId ?: return
        val repo = persistenceRepository ?: return
        _lastCompletedSlotDifficulty.value = difficulty
        coordinatorScope.launch {
            runCatching {
                repo.updateCompletionDifficulty(slotId = slotId, difficulty = difficulty)
                refreshDailySlotsAsync()
            }
        }
    }

    fun prepareMarkdownImportFromUri(context: Context, uri: Uri) {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            try {
                val resolver = context.contentResolver
                var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "book.md"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        val queried = cursor.getString(nameIndex)
                        if (!queried.isNullOrBlank()) {
                            displayName = queried
                        }
                    }
                }
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Не удалось открыть выбранный документ.")
                val newState = repo.prepareImportPreview(rawBytes = bytes, fileName = displayName)
                _storyState.value = newState
                val preview = newState.pendingImportPreview
                if (preview != null) {
                    appendLog(
                        "BOOK",
                        "Подготовлен предпросмотр файла «${preview.fileName}»: актов=${preview.actCount}, глав=${preview.chapterCount}, sha256=${preview.sha256Hex.take(12)}..."
                    )
                }
            } catch (e: Exception) {
                _storyState.value = repo.loadReaderState(
                    errorBannerRu = "Не удалось прочитать выбранный файл (${e.javaClass.simpleName}). Текущая книга сохранена."
                )
            }
        }
    }

    fun prepareMarkdownImportBytes(rawBytes: ByteArray, fileName: String) {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.prepareImportPreview(rawBytes = rawBytes, fileName = fileName)
        }
    }

    fun updateImportPreviewHeadingLevels(actHeadingLevel: Int, chapterHeadingLevel: Int) {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.updatePreviewHeadingLevels(actHeadingLevel, chapterHeadingLevel)
        }
    }

    fun confirmPendingMarkdownImport() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            val state = repo.confirmPendingImport()
            _storyState.value = state
            if (state.hasImportedBook) {
                appendLog(
                    "BOOK",
                    "Импортирована книга (файл=${state.fileName}, актов=${state.actCount}, глав=${state.chapterCount}, sha256=${state.sha256Hex.take(12)}...)"
                )
            }
        }
    }

    fun cancelPendingMarkdownImport() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.cancelPendingImport()
        }
    }

    fun importNeutralTestMarkdown() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            val state = repo.importNeutralTestSample()
            _storyState.value = state
            appendLog(
                "BOOK",
                "Импортирован нейтральный тестовый макет Markdown (${state.actCount} акта, ${state.chapterCount} глав)"
            )
        }
    }

    fun nextStoryStep() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.nextStep()
        }
    }

    fun previousStoryStep() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.previousStep()
        }
    }

    fun nextStoryChapter() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.nextChapter()
        }
    }

    fun previousStoryChapter() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.previousChapter()
        }
    }

    fun selectStoryChapter(chapterId: String) {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.selectChapter(chapterId)
        }
    }

    fun saveStoryBlockPosition(blockId: String) {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.saveBlockPosition(blockId)
        }
    }

    fun restartStoryFromBeginning() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.restartFromBeginning()
        }
    }

    fun removeImportedBook() {
        val repo = storyRepository ?: return
        coordinatorScope.launch {
            _storyState.value = repo.removeImportedBook()
            appendLog("BOOK", "Импортированная книга удалена из внутреннего хранилища по подтверждению пользователя")
        }
    }

    private fun persistCheckpointAsync(snapshot: PhaseEngineSnapshot, trigger: CheckpointTrigger) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            try {
                if (trigger == CheckpointTrigger.COMPLETE) {
                    _lastCompletedSlotDifficulty.value = null
                    val inserted = repo.completeRunAtomically(snapshot)
                    if (inserted) {
                        appendLog(
                            "DB_COMMIT",
                            "Атомарный зачёт подхода сохранён в Room: slotId=${snapshot.slotId}, дата=${snapshot.startDate}, isDemo=${snapshot.isDemo}"
                        )
                    } else {
                        appendLog(
                            "DB_DEDUP",
                            "Повторный зачёт слота ${snapshot.slotId} заблокирован уникальным ограничением Room"
                        )
                    }
                    val isDemo = isCurrentModeDemo()
                    val slots = repo.getOrCreateDailySlots(
                        localDate = activeDateKey(),
                        planId = activePlanId(),
                        isDemo = isDemo
                    )
                    _dailySlots.value = slots
                    _demoCompletionsCount.value = repo.countDemoCompletions()
                    _realCompletionsCount.value = repo.countRealCompletions()
                    _weeklyOverview.value = repo.loadWeeklyOverview(
                        todayDateStr = timeProvider.currentLocalDate(),
                        selectedDateStr = _weeklyOverview.value.selectedDate.ifEmpty { null }
                    )
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

    fun clearAudioDiagnostics() {
        audioVibrationEngine?.clearAudioDiagnostics()
    }

    fun updateSettings(transform: (BenchSignalSettings) -> BenchSignalSettings) {
        val previous = _settings.value
        val updated = transform(previous)
        if (previous.isRealProgramEnabled != updated.isRealProgramEnabled &&
            _engineSnapshot.value.isActiveOrPaused
        ) {
            appendLog("PLAN", "Переключение режима программы заблокировано на время активного подхода")
            return
        }

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

        if (previous.isRealProgramEnabled != updated.isRealProgramEnabled) {
            refreshDailySlotsAsync()
        }

        val prefs = preferencesRepository
        if (prefs != null) {
            coordinatorScope.launch {
                try {
                    prefs.updateSettings(updated)
                } catch (e: Exception) {
                    appendLog("PREFS_ERR", "Не удалось сохранить настройки: ${e.javaClass.simpleName}")
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
        val isDemoSlot = matchingSlot?.slot?.isDemo ?: isCurrentModeDemo()
        val runId = "run_${UUID.randomUUID()}"
        val startDate = timeProvider.currentLocalDate()
        val zoneId = timeProvider.currentZoneId()
        val currentSettings = _settings.value
        _lastCompletedSlotDifficulty.value = null

        val started = phaseEngine.start(
            runId = runId,
            slotId = slotIdToUse,
            startDate = startDate,
            zoneId = zoneId,
            isDemo = isDemoSlot,
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
