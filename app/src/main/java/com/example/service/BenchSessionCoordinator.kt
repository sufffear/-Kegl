package com.example.service

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import com.example.data.AudioDiagnosticEntry
import com.example.data.AudioHardwareStatus
import com.example.data.AudioVibrationEngine
import com.example.data.BackupManifest
import com.example.data.BackupPreviewInfo
import com.example.data.BackupRestoreManager
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.data.FreeIntervalTemplateEntity
import com.example.data.PreferencesRepository
import com.example.data.StoryReaderUiState
import com.example.data.StoryRepository
import com.example.data.VelarisDatabase
import com.example.data.WeeklyOverviewState
import com.example.data.WorkoutPersistenceRepository
import java.io.InputStream
import java.io.OutputStream
import com.example.domain.BenchEventLogEntry
import com.example.domain.CheckpointTrigger
import com.example.domain.DEFAULT_DEMO_PROTOCOLS
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngine
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.PlanGroup
import com.example.domain.RunStatus
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
import kotlinx.coroutines.Job
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Единый координатор сессии Velaris (Этап 0B + Этап 2 + Итерация v1.1).
 * Хранит единственный экземпляр PhaseEngine, синхронизирует контрольные точки и завершения с Room,
 * управляет 7-дневным обзором реальных записей, офлайн-читалкой истории, аудио-диагностикой и настройками DataStore.
 */
object BenchSessionCoordinator {

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _workoutEnergy = MutableStateFlow(0)
    val workoutEnergy: StateFlow<Int> = _workoutEnergy.asStateFlow()
    private val logIdCounter = AtomicLong(1L)
    private val timeProvider = SystemTimeProvider()

    private var appContext: Context? = null
    private var audioVibrationEngine: AudioVibrationEngine? = null
    private var preferencesRepository: PreferencesRepository? = null
    private var persistenceRepository: WorkoutPersistenceRepository? = null
    private var storyRepository: StoryRepository? = null
    private var backupRestoreManager: BackupRestoreManager? = null

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

    private val _freeTemplates = MutableStateFlow<List<FreeIntervalTemplateEntity>>(emptyList())
    val freeTemplates: StateFlow<List<FreeIntervalTemplateEntity>> = _freeTemplates.asStateFlow()

    private val _trainingConfigurations = MutableStateFlow<List<com.example.data.TrainingConfiguration>>(emptyList())
    val trainingConfigurations = _trainingConfigurations.asStateFlow()
    private val _extraTrainingCycle = MutableStateFlow(com.example.data.ExtraTrainingCycle())
    val extraTrainingCycle = _extraTrainingCycle.asStateFlow()
    private val _trainingStateReady = MutableStateFlow(false)
    val trainingStateReady = _trainingStateReady.asStateFlow()
    private var activeCustomProgramSnapshot: com.example.domain.CustomWorkoutProgram? = null
    private var trainingDao: com.example.data.VelarisDao? = null

    fun saveTrainingConfiguration(configuration: com.example.data.TrainingConfiguration, onResult: (Boolean) -> Unit) {
        coordinatorScope.launch {
            val success = runCatching {
                require(configuration.isValid)
                require(configuration.key.startsWith("mandatory:") || configuration.key in setOf("extra:101","extra:102","extra:103"))
                requireNotNull(trainingDao).saveTrainingConfiguration(configuration)
                _trainingConfigurations.value = requireNotNull(trainingDao).getTrainingConfigurations()
                refreshConfiguredDailySlots()
            }.isSuccess
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun resetTrainingConfiguration(key: String, onResult: (Boolean) -> Unit) {
        coordinatorScope.launch {
            val success = runCatching {
                requireNotNull(trainingDao).resetTrainingConfiguration(key)
                _trainingConfigurations.value = requireNotNull(trainingDao).getTrainingConfigurations()
                refreshConfiguredDailySlots()
            }.isSuccess
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    private suspend fun refreshConfiguredDailySlots() {
        _dailySlots.value = persistenceRepository?.getOrCreateDailySlots(activeDateKey(),activePlanId(),isCurrentModeDemo()).orEmpty()
    }

    @Volatile private var trainingFactories: Map<String,com.example.data.TrainingConfiguration> = emptyMap()
    private fun updateTrainingFactories(assignments: List<com.example.data.PlanAssignmentEntity>) {
        trainingFactories = assignments.associate { a ->
            val key="mandatory:${a.id}"
            key to com.example.data.TrainingConfiguration(key,(a.contractionMs/1000).toInt(),(a.relaxationMs/1000).toInt(),a.repetitions)
        }
    }
    fun trainingDefaultConfiguration(item: com.example.data.DailySlotEntity): com.example.data.TrainingConfiguration =
        trainingFactories[com.example.data.TrainingConfiguration.from(item).key] ?: com.example.data.TrainingConfiguration.from(item)

    private val _customPrograms = MutableStateFlow<List<com.example.domain.CustomWorkoutProgram>>(emptyList())
    val customPrograms: StateFlow<List<com.example.domain.CustomWorkoutProgram>> = _customPrograms.asStateFlow()

    private val _pendingRestorePreview = MutableStateFlow<BackupPreviewInfo?>(null)
    val pendingRestorePreview: StateFlow<BackupPreviewInfo?> = _pendingRestorePreview.asStateFlow()

    private val _backupOperationBanner = MutableStateFlow<String?>(null)
    val backupOperationBanner: StateFlow<String?> = _backupOperationBanner.asStateFlow()

    private val _backupErrorMessage = MutableStateFlow<String?>(null)
    val backupErrorMessage: StateFlow<String?> = _backupErrorMessage.asStateFlow()

    private var pendingRestoreUri: Uri? = null

    private val _lastCompletedSlotDifficulty = MutableStateFlow<WorkoutDifficulty?>(null)
    val lastCompletedSlotDifficulty: StateFlow<WorkoutDifficulty?> = _lastCompletedSlotDifficulty.asStateFlow()

    private val _celebratedMilestone = MutableStateFlow<com.example.data.StreakMilestoneInfo?>(null)
    val celebratedMilestone: StateFlow<com.example.data.StreakMilestoneInfo?> = _celebratedMilestone.asStateFlow()

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
            com.example.data.RestoreStartupGate.ensureRecovered(appCtx)
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
            trainingDao = db.velarisDao()
            coordinatorScope.launch { db.velarisDao().observePlanAssignments().collect { updateTrainingFactories(it) } }
            coordinatorScope.launch {
                db.velarisDao().observeTrainingConfigurations().collect { configurations ->
                    _trainingConfigurations.value = configurations
                    refreshConfiguredDailySlots()
                }
            }
            coordinatorScope.launch {
                db.velarisDao().observeExtraTrainingCycle().collect { _extraTrainingCycle.value = it ?: com.example.data.ExtraTrainingCycle() }
            }
            coordinatorScope.launch {
                db.velarisDao().observeExtraTrainingCycle().collect { cycle ->
                    if(cycle?.mask == 7) {
                        // Max 3 seconds even after wall-clock changes; a killed celebration recovers here.
                        kotlinx.coroutines.delay((cycle.celebrationUntilEpochMs-timeProvider.currentWallTimeMs()).coerceIn(0L,3000L))
                        db.velarisDao().finishExtraTrainingCelebration(cycle.round)
                    }
                }
            }
            coordinatorScope.launch { db.velarisDao().observeWorkoutEnergy().collect { _workoutEnergy.value = it } }

            val storyRepo = StoryRepository(appCtx, db.velarisDao(), timeProvider)
            storyRepository = storyRepo

            val backupManager = BackupRestoreManager(
                context = appCtx,
                dao = db.velarisDao(),
                preferencesRepository = prefs,
                storyRepository = storyRepo,
                timeProvider = timeProvider
            )
            backupRestoreManager = backupManager

            coordinatorScope.launch {
                audioEngine.audioDiagnostics.collect { list ->
                    _audioDiagnostics.value = list
                }
            }

            coordinatorScope.launch {
                prefs.signalSettingsFlow.collect { loadedSettings ->
                    val prevRealEnabled = _settings.value.isRealProgramEnabled
                    val prevFrozen = _settings.value.frozenDates
                    _settings.value = loadedSettings
                    phaseEngine.isMetronomeEnabled = loadedSettings.metronomeEnabled
                    refreshHardwareStatus()
                    if (prevRealEnabled != loadedSettings.isRealProgramEnabled || prevFrozen != loadedSettings.frozenDates) {
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
                "Инициализировано приложение Velaris v0.7.0-v1.4 (versionCode 7)"
            )
        }
    }

    private suspend fun initializePersistenceAndRecover(
        repo: WorkoutPersistenceRepository,
        storyRepo: StoryRepository
    ) {
        try {
            _trainingConfigurations.value = requireNotNull(trainingDao).getTrainingConfigurations()
            _extraTrainingCycle.value = requireNotNull(trainingDao).getExtraTrainingCycle() ?: com.example.data.ExtraTrainingCycle()
            val isDemo = isCurrentModeDemo()
            val slots = repo.getOrCreateDailySlots(
                localDate = activeDateKey(),
                planId = activePlanId(),
                isDemo = isDemo
            )
            updateTrainingFactories(requireNotNull(trainingDao).getAllPlanAssignments())
            _trainingStateReady.value = true
            _dailySlots.value = slots
            _demoCompletionsCount.value = repo.countDemoCompletions()
            _realCompletionsCount.value = repo.countRealCompletions()
            _weeklyOverview.value = repo.loadWeeklyOverview(
                todayDateStr = timeProvider.currentLocalDate(),
                selectedDateStr = _weeklyOverview.value.selectedDate.ifEmpty { null },
                manualFrozenDates = _settings.value.frozenDates
            )

            // Загружаем офлайн-читалку независимо (ошибки читалки изолированы внутри StoryRepository)
            _storyState.value = storyRepo.loadReaderState()

            // Инициализируем 5 слотов свободного режима
            repo.ensureFreeTemplatesSeeded()
            _freeTemplates.value = repo.getAllFreeTemplates()
            // Seed legacy starter programs once, never resurrect a deliberately emptied library.
            val libraryPrefs=requireNotNull(appContext).getSharedPreferences("velaris_training_library",Context.MODE_PRIVATE)
            if(!libraryPrefs.getBoolean("factory_seeded_v1",false)) {
                repo.seedCustomProgramsIfEmpty()
                check(libraryPrefs.edit().putBoolean("factory_seeded_v1",true).commit())
            }
            _customPrograms.value = repo.getAllCustomPrograms()

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
                        isFreeMode = recovered.isFreeMode,
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
        return _isZeroSlotDemoMode.value
    }

    private fun activeDateKey(): String {
        val today = timeProvider.currentLocalDate()
        return if (_isZeroSlotDemoMode.value) "${today}_rest" else today
    }

    private fun activePlanId(): String {
        return if (_isZeroSlotDemoMode.value) {
            WorkoutPersistenceRepository.DEMO_PLAN_ZERO_SLOTS
        } else {
            WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3
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
                    selectedDateStr = _weeklyOverview.value.selectedDate.ifEmpty { null },
                    manualFrozenDates = _settings.value.frozenDates
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
                    selectedDateStr = localDate,
                    manualFrozenDates = _settings.value.frozenDates
                )
            }
        }
    }

    fun toggleFreezeDate(localDate: String) {
        val currentFrozen = _settings.value.frozenDates
        val updatedFrozen = if (localDate in currentFrozen) {
            currentFrozen - localDate
        } else {
            currentFrozen + localDate
        }
        updateSettings { it.copy(frozenDates = updatedFrozen) }
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            runCatching {
                _weeklyOverview.value = repo.loadWeeklyOverview(
                    todayDateStr = timeProvider.currentLocalDate(),
                    selectedDateStr = localDate,
                    manualFrozenDates = updatedFrozen
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

    // --- Резервное копирование и атомарное восстановление данных (v1.2 п. 1–3) ---

    fun createBackupToUri(context: Context, uri: Uri) {
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            _backupErrorMessage.value = "Нельзя сохранять резервную копию во время активной тренировки. Завершите или остановите подход."
            return
        }
        val manager = backupRestoreManager ?: return
        coordinatorScope.launch {
            try {
                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalArgumentException("Не удалось открыть файл для записи резервной копии.")
                val manifestResult = manager.createBackup(out)
                manifestResult.fold(
                    onSuccess = { manifest ->
                        val bookInfo = if (manifest.hasBook) ", книга: «${manifest.bookTitle}»" else ""
                        _backupOperationBanner.value = "Резервная копия успешно сохранена (${manifest.workoutCount} тренировок$bookInfo)."
                        _backupErrorMessage.value = null
                        appendLog(
                            "BACKUP",
                            "Резервная копия сохранена (тренировок=${manifest.workoutCount}, книга=${manifest.bookTitle ?: "нет"})"
                        )
                    },
                    onFailure = { err ->
                        _backupErrorMessage.value = "Ошибка сохранения копии: ${err.message ?: "неизвестный сбой"}"
                        _backupOperationBanner.value = null
                        appendLog("BACKUP_ERROR", "Ошибка создания копии: ${err.message}")
                    }
                )
            } catch (e: Exception) {
                _backupErrorMessage.value = "Ошибка записи файла копии: ${e.message}"
                _backupOperationBanner.value = null
                appendLog("BACKUP_ERROR", "Исключение при записи копии: ${e.message}")
            }
        }
    }

    fun inspectBackupFromUri(context: Context, uri: Uri) {
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            _backupErrorMessage.value = "Нельзя восстанавливать данные во время активной тренировки. Завершите или остановите подход."
            return
        }
        val manager = backupRestoreManager ?: return
        coordinatorScope.launch {
            try {
                var sizeBytes = 0L
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && cursor.moveToFirst()) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Не удалось открыть выбранный файл резервной копии.")
                val previewResult = manager.inspectBackup(inputStream, sizeBytes)
                previewResult.fold(
                    onSuccess = { preview ->
                        pendingRestoreUri = uri
                        _pendingRestorePreview.value = preview
                        _backupErrorMessage.value = null
                        appendLog(
                            "BACKUP_INSPECT",
                            "Прочитан архив резервной копии от ${preview.createdAtRu} (тренировок=${preview.workoutCount}, книга=${preview.bookTitle ?: "нет"})"
                        )
                    },
                    onFailure = { err ->
                        pendingRestoreUri = null
                        _pendingRestorePreview.value = null
                        _backupErrorMessage.value = "Файл не является корректной резервной копией Velaris: ${err.message ?: "некорректный формат"}"
                        appendLog("BACKUP_ERROR", "Ошибка проверки архива копии: ${err.message}")
                    }
                )
            } catch (e: Exception) {
                pendingRestoreUri = null
                _pendingRestorePreview.value = null
                _backupErrorMessage.value = "Не удалось открыть архив: ${e.message}"
                appendLog("BACKUP_ERROR", "Исключение при открытии архива: ${e.message}")
            }
        }
    }

    fun confirmPendingRestore(context: Context) {
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            _backupErrorMessage.value = "Нельзя восстанавливать данные во время активной тренировки."
            return
        }
        val targetUri = pendingRestoreUri ?: return
        val manager = backupRestoreManager ?: return
        coordinatorScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(targetUri)
                    ?: throw IllegalArgumentException("Не удалось прочитать файл резервной копии.")
                val restoreResult = manager.restoreBackup(inputStream, isWorkoutActiveOrPaused = false)
                restoreResult.fold(
                    onSuccess = { manifest ->
                        pendingRestoreUri = null
                        _pendingRestorePreview.value = null
                        _backupErrorMessage.value = null

                        // Перезагружаем тренировочные данные и читатель
                        val isDemo = isCurrentModeDemo()
                        val repo = persistenceRepository
                        if (repo != null) {
                            _dailySlots.value = repo.getOrCreateDailySlots(activeDateKey(), activePlanId(), isDemo)
                            _demoCompletionsCount.value = repo.countDemoCompletions()
                            _realCompletionsCount.value = repo.countRealCompletions()
                            _weeklyOverview.value = repo.loadWeeklyOverview(timeProvider.currentLocalDate())
                            _freeTemplates.value = repo.getAllFreeTemplates()
                            _customPrograms.value = repo.getAllCustomPrograms()
                        }
                        val storyRepo = storyRepository
                        if (storyRepo != null) {
                            _storyState.value = storyRepo.loadReaderState()
                        }

                        val bookInfo = if (manifest.hasBook) ", книга: «${manifest.bookTitle}»" else ""
                        _backupOperationBanner.value = "Данные успешно восстановлены из копии от ${manifest.createdAtRu} (${manifest.workoutCount} тренировок$bookInfo)."
                        appendLog(
                            "RESTORE",
                            "Данные восстановлены из копии от ${manifest.createdAtRu}: тренировок=${manifest.workoutCount}, книга=${manifest.bookTitle ?: "нет"}"
                        )
                    },
                    onFailure = { err ->
                        _backupErrorMessage.value = "Восстановление не удалось: ${err.message ?: "ошибка данных"}."
                        _backupOperationBanner.value = null
                        appendLog("RESTORE_ERROR", "Ошибка восстановления: ${err.message}")
                    }
                )
            } catch (e: Exception) {
                _backupErrorMessage.value = "Восстановление прервано: ${e.message}."
                _backupOperationBanner.value = null
                appendLog("RESTORE_ERROR", "Исключение при восстановлении: ${e.message}")
            }
        }
    }

    fun cancelPendingRestore() {
        pendingRestoreUri = null
        _pendingRestorePreview.value = null
    }

    fun dismissBackupBanners() {
        _backupOperationBanner.value = null
        _backupErrorMessage.value = null
    }

    suspend fun createBackupDirect(outputStream: OutputStream): Result<BackupManifest> {
        val manager = backupRestoreManager ?: return Result.failure(IllegalStateException("Менеджер резервного копирования не инициализирован"))
        return manager.createBackup(outputStream)
    }

    suspend fun inspectBackupDirect(inputStream: InputStream, sizeBytes: Long = 0L): Result<BackupPreviewInfo> {
        val manager = backupRestoreManager ?: return Result.failure(IllegalStateException("Менеджер резервного копирования не инициализирован"))
        return manager.inspectBackup(inputStream, sizeBytes)
    }

    suspend fun restoreBackupDirect(inputStream: InputStream): Result<BackupManifest> {
        val manager = backupRestoreManager ?: return Result.failure(IllegalStateException("Менеджер резервного копирования не инициализирован"))
        val active = phaseEngine.currentSnapshot().isActiveOrPaused
        val res = manager.restoreBackup(inputStream, isWorkoutActiveOrPaused = active)
        if (res.isSuccess) {
            val isDemo = isCurrentModeDemo()
            persistenceRepository?.let { repo ->
                _dailySlots.value = repo.getOrCreateDailySlots(activeDateKey(), activePlanId(), isDemo)
                _demoCompletionsCount.value = repo.countDemoCompletions()
                _realCompletionsCount.value = repo.countRealCompletions()
                _weeklyOverview.value = repo.loadWeeklyOverview(timeProvider.currentLocalDate())
                _freeTemplates.value = repo.getAllFreeTemplates()
                _customPrograms.value = repo.getAllCustomPrograms()
            }
            storyRepository?.let { storyRepo ->
                _storyState.value = storyRepo.loadReaderState()
            }
        }
        return res
    }

    private var checkpointJob: Job? = null

    @Synchronized
    private fun persistCheckpointAsync(snapshot: PhaseEngineSnapshot, trigger: CheckpointTrigger) {
        val repo = persistenceRepository ?: return
        val previous = checkpointJob
        val programSnapshot = activeCustomProgramSnapshot?.takeIf { snapshot.isCustomProgram && it.id == snapshot.protocol.id }
        checkpointJob = coordinatorScope.launch {
            previous?.join()
            try {
                if (trigger == CheckpointTrigger.COMPLETE) {
                    val prevStreak = _weeklyOverview.value.currentStreakDays
                    _lastCompletedSlotDifficulty.value = null
                    val inserted = repo.completeRunAtomically(snapshot,program=programSnapshot)
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
                    val updatedOverview = repo.loadWeeklyOverview(
                        todayDateStr = timeProvider.currentLocalDate(),
                        selectedDateStr = _weeklyOverview.value.selectedDate.ifEmpty { null },
                        manualFrozenDates = _settings.value.frozenDates
                    )
                    _weeklyOverview.value = updatedOverview
                    if (inserted && updatedOverview.currentStreakDays > prevStreak) {
                        val unlockedNow = updatedOverview.milestones.firstOrNull {
                            it.targetDays == updatedOverview.currentStreakDays
                        }
                        if (unlockedNow != null) {
                            _celebratedMilestone.value = unlockedNow
                            audioVibrationEngine?.playSignal(
                                signal = SignalType.GROUP_COMPLETED,
                                settings = _settings.value,
                                isSamplePreview = true
                            )
                        }
                    }
                } else {
                    repo.saveRunCheckpoint(snapshot,programSnapshot)
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
        val latest = _dailySlots.value.firstOrNull { it.slot.id == slotWithCompletion.slot.id } ?: slotWithCompletion
        val proto = repo.slotToProtocolConfig(latest.slot)
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
            audioVibrationEngine?.requestSessionAudioFocus(_settings.value.duckOtherApps)
        }

        val slotIdToUse = matchingSlot?.slot?.id ?: phaseEngine.currentSnapshot().protocol.id
        val isDemoSlot = matchingSlot?.slot?.isDemo ?: false
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

    // --- Продвинутый конструктор тренировок (управление и запуск) ---

    fun saveCustomProgram(program: com.example.domain.CustomWorkoutProgram, onResult: ((Boolean)->Unit)? = null) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            val success = runCatching {
                repo.saveCustomProgram(program)
                _customPrograms.value = repo.getAllCustomPrograms()
                appendLog("PROGRAM", "Сохранена программа конструктора: «${program.title}» (${program.blocks.size} блоков)")
            }.isSuccess
            if(!success) _databaseErrorMessage.value = "Не удалось сохранить программу. Проверь параметры и повтори."
            kotlinx.coroutines.withContext(Dispatchers.Main) {onResult?.invoke(success)}
        }
    }

    fun deleteCustomProgram(id: String) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            repo.deleteCustomProgram(id)
            _customPrograms.value = repo.getAllCustomPrograms()
            appendLog("PROGRAM", "Удалена программа конструктора (ID: $id)")
        }
    }

    fun duplicateCustomProgram(program: com.example.domain.CustomWorkoutProgram) {
        val copy = program.copy(
            id = UUID.randomUUID().toString(),
            title = "${program.title} (Копия)",
            createdAtEpochMs = timeProvider.currentWallTimeMs(),
            updatedAtEpochMs = timeProvider.currentWallTimeMs()
        )
        saveCustomProgram(copy)
    }

    fun reorderCustomPrograms(reordered: List<com.example.domain.CustomWorkoutProgram>) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            reordered.forEachIndexed { index, prog ->
                repo.saveCustomProgram(prog, sortOrder = index)
            }
            _customPrograms.value = reordered
            appendLog("PROGRAM", "Обновлён порядок программ конструктора")
        }
    }

    @Synchronized
    fun startCustomProgramSession(program: com.example.domain.CustomWorkoutProgram): Boolean {
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            appendLog(
                "GUARD",
                "Нельзя запустить программу: уже активна другая попытка (${phaseEngine.currentSnapshot().status})"
            )
            return false
        }
        if (program.blocks.isEmpty()) {
            appendLog("GUARD", "Нельзя запустить пустую программу: добавьте хотя бы один блок")
            return false
        }

        refreshHardwareStatus()
        val hw = _hardwareStatus.value
        if (_settings.value.soundEnabled && (!hw.isSilentOrVibrateRinger || hw.isHeadphonesConnected)) {
            audioVibrationEngine?.requestSessionAudioFocus(_settings.value.duckOtherApps)
        }

        val runId = "prog_run_${UUID.randomUUID()}"
        val startDate = timeProvider.currentLocalDate()
        val zoneId = timeProvider.currentZoneId()
        val currentSettings = _settings.value
        val isDemoRun = false
        _lastCompletedSlotDifficulty.value = null

        activeCustomProgramSnapshot = program.copy(blocks=program.blocks.toList())
        val started = phaseEngine.startCustomProgram(
            program = program,
            runId = runId,
            startDate = startDate,
            zoneId = zoneId,
            soundPackSnapshot = currentSettings.soundPack,
            metronomeStyleSnapshot = currentSettings.metronomeTickStyle,
            isDemo = isDemoRun
        )
        _engineSnapshot.value = phaseEngine.currentSnapshot()
        if (started) {
            appendLog(
                "PROGRAM_START",
                "Запущена программа конструктора «${program.title}» (${program.blocks.size} блоков, ${program.formattedTotalDuration()})"
            )
        }
        return started
    }

    fun saveFreeTemplate(template: FreeIntervalTemplateEntity) {
        val repo = persistenceRepository ?: return
        coordinatorScope.launch {
            repo.saveFreeTemplate(template)
            _freeTemplates.value = repo.getAllFreeTemplates()
            appendLog(
                "TEMPLATE",
                "Сохранён шаблон «${template.name}» (слот ${template.slotIndex}: ${template.targetDurationMinutes}м ${template.targetDurationSeconds}с, удержание ${template.contractionSec}с, расслабление ${template.relaxationSec}с)"
            )
        }
    }

    @Synchronized
    fun startFreeSession(requestedTemplate: FreeIntervalTemplateEntity): Boolean {
        if (requestedTemplate.slotIndex in 101..103 &&
            (!_trainingStateReady.value || _extraTrainingCycle.value.isLocked(requestedTemplate.slotIndex) || checkpointJob?.isActive == true)) return false
        val template = if(requestedTemplate.slotIndex in 101..103) {
            val factory = com.example.data.ExtraWorkoutPresets.all.first { it.slotIndex == requestedTemplate.slotIndex }
            _trainingConfigurations.value.firstOrNull { it.key == "extra:${requestedTemplate.slotIndex}" }?.applyTo(factory) ?: factory
        } else requestedTemplate
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            appendLog(
                "GUARD",
                "Нельзя запустить свободный подход: уже активна или приостановлена другая попытка (${phaseEngine.currentSnapshot().status})"
            )
            return false
        }
        if (!template.isValid) {
            appendLog("GUARD", "Попытка запуска незаполненного или некорректного шаблона «${template.name}» заблокирована")
            return false
        }

        refreshHardwareStatus()
        val hw = _hardwareStatus.value
        if (_settings.value.soundEnabled && (!hw.isSilentOrVibrateRinger || hw.isHeadphonesConnected)) {
            audioVibrationEngine?.requestSessionAudioFocus(_settings.value.duckOtherApps)
        }

        val proto = DemoProtocolConfig(
            id = "free_slot_${template.slotIndex}",
            titleRu = "Мои интервалы · ${template.name}",
            contractionMs = template.contractionSec * 1000L,
            relaxationMs = template.relaxationSec * 1000L,
            repetitions = template.calculatedCycles,
            isLastInGroup = true,
            group = PlanGroup.MORNING
        )

        val runId = "free_run_${UUID.randomUUID()}"
        val slotId = "free_${template.slotIndex}_${UUID.randomUUID()}"
        val startDate = timeProvider.currentLocalDate()
        val zoneId = timeProvider.currentZoneId()
        val currentSettings = _settings.value
        val isDemoRun = false
        _lastCompletedSlotDifficulty.value = null

        val started = phaseEngine.start(
            protocol = proto,
            runId = runId,
            slotId = slotId,
            startDate = startDate,
            zoneId = zoneId,
            isDemo = isDemoRun,
            isFreeMode = true,
            soundPackSnapshot = currentSettings.soundPack,
            metronomeStyleSnapshot = currentSettings.metronomeTickStyle
        )
        _engineSnapshot.value = phaseEngine.currentSnapshot()
        if (started) {
            appendLog(
                "FREE_START",
                "Запущен свободный подход «${template.name}» (${proto.repetitions} циклов по ${template.contractionSec}с/${template.relaxationSec}с, факт=${template.actualDurationSec}с, demo=$isDemoRun)"
            )
        }
        return started
    }

    /**
     * Экстренный 60-секундный микро-протокол «⚡ Блиц-спасение серии (60 сек)»:
     * 30 быстрых циклов по 1 с сжатия / 1 с отдыха (ровно 60 секунд).
     * Записывается как реальная завершённая тренировка за сегодня и мгновенно зажигает звено серии.
     */
    @Synchronized
    fun startStreakRescueBlitzSession(): Boolean {
        if (phaseEngine.currentSnapshot().isActiveOrPaused) {
            appendLog(
                "GUARD",
                "Нельзя запустить блиц-спасение серии: уже активна другая попытка (${phaseEngine.currentSnapshot().status})"
            )
            return false
        }

        refreshHardwareStatus()
        val hw = _hardwareStatus.value
        if (_settings.value.soundEnabled && (!hw.isSilentOrVibrateRinger || hw.isHeadphonesConnected)) {
            audioVibrationEngine?.requestSessionAudioFocus(_settings.value.duckOtherApps)
        }

        val proto = DemoProtocolConfig(
            id = "streak_rescue_blitz_60s",
            titleRu = "⚡ Блиц-спасение серии (60 сек)",
            contractionMs = 1_000L,
            relaxationMs = 1_000L,
            repetitions = 30,
            isLastInGroup = true,
            group = PlanGroup.EVENING
        )

        val runId = "blitz_run_${UUID.randomUUID()}"
        val slotId = "blitz_60s_${UUID.randomUUID()}"
        val startDate = timeProvider.currentLocalDate()
        val zoneId = timeProvider.currentZoneId()
        val currentSettings = _settings.value
        _lastCompletedSlotDifficulty.value = null

        val started = phaseEngine.start(
            protocol = proto,
            runId = runId,
            slotId = slotId,
            startDate = startDate,
            zoneId = zoneId,
            isDemo = false,
            isFreeMode = true,
            soundPackSnapshot = currentSettings.soundPack,
            metronomeStyleSnapshot = currentSettings.metronomeTickStyle
        )
        _engineSnapshot.value = phaseEngine.currentSnapshot()
        if (started) {
            appendLog(
                "BLITZ_START",
                "Запущен 60-секундный протокол «Блиц-спасение серии» (30 циклов по 1с/1с)"
            )
        }
        return started
    }

    fun triggerMilestoneSalute(milestone: com.example.data.StreakMilestoneInfo) {
        _celebratedMilestone.value = milestone
        if (!_engineSnapshot.value.isRunningOrPreparing) {
            refreshHardwareStatus()
            audioVibrationEngine?.playSignal(
                signal = SignalType.GROUP_COMPLETED,
                settings = _settings.value,
                isSamplePreview = true
            )
        }
        appendLog(
            "MILESTONE_SALUTE",
            "Салют жетона рубежа: ${milestone.badgeEmoji} «${milestone.titleRu}» (${milestone.targetDays} дн.)"
        )
    }

    fun dismissMilestoneSalute() {
        _celebratedMilestone.value = null
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
            audioVibrationEngine?.requestSessionAudioFocus(_settings.value.duckOtherApps)
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
        if (snap.status == RunStatus.STOPPED) audioVibrationEngine?.abandonSessionAudioFocus()
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
