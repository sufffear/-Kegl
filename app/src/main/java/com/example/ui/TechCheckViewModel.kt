package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppBuildInfoRepository
import com.example.data.AudioDiagnosticEntry
import com.example.data.AudioHardwareStatus
import com.example.data.BackupPreviewInfo
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.data.StoryReaderUiState
import com.example.data.WeeklyOverviewState
import com.example.domain.AppBuildInfo
import com.example.domain.BenchEventLogEntry
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.ReaderFontSize
import com.example.domain.ReaderLineSpacing
import com.example.domain.ReaderMode
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.domain.WorkoutDifficulty
import com.example.service.BenchSessionCoordinator
import com.example.service.BenchTimerService
import com.example.ui.theme.VelarisThemeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel приложения Velaris (v0.4.0-v1.1).
 * Подключается к единому координатору BenchSessionCoordinator и управляет навигацией между экранами,
 * не создавая второго таймера при смене экранов.
 */
class TechCheckViewModel(
    private val appContext: Context,
    private val repository: AppBuildInfoRepository,
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _buildInfo = MutableStateFlow(repository.loadBuildInfo())
    val buildInfo: StateFlow<AppBuildInfo> = _buildInfo.asStateFlow()

    private var navigation = VelarisNavigation.restore(savedState.get<ArrayList<String>>("velaris_routes"))
    private val _currentDestination = MutableStateFlow(navigation.current)
    val currentDestination: StateFlow<VelarisNavDestination> = _currentDestination.asStateFlow()

    private val _workoutJournal = MutableStateFlow(com.example.data.WorkoutJournalState())
    val workoutJournal: StateFlow<com.example.data.WorkoutJournalState> = _workoutJournal.asStateFlow()
    val statisticsDate = savedState.getStateFlow<String?>("statistics_date", null)
    val journalDate = savedState.getStateFlow<String?>("journal_date", null)
    val journalEntryId = savedState.getStateFlow<String?>("journal_entry", null)
    private var journalJob: kotlinx.coroutines.Job? = null

    fun openStatisticsDay(date: String) {
        savedState["statistics_date"] = date
        selectWeeklyOverviewDate(date)
        navigateTo(VelarisNavDestination.STATISTICS_DAY)
    }
    fun openWorkoutJournal(date: String? = null) {
        savedState["journal_date"] = date
        navigateTo(VelarisNavDestination.WORKOUT_JOURNAL)
    }
    fun openWorkoutEntry(id: String) {
        savedState["journal_entry"] = id
        navigateTo(VelarisNavDestination.WORKOUT_SESSION)
    }
    fun refreshWorkoutJournal() {
        journalJob?.cancel()
        _workoutJournal.value = _workoutJournal.value.copy(loading = true, error = false)
        journalJob = viewModelScope.launch {
            try {
                val entries = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.data.WorkoutJournalRepository(com.example.data.VelarisDatabase.getInstance(appContext)).load()
                }
                _workoutJournal.value = com.example.data.WorkoutJournalState(entries)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _workoutJournal.value = _workoutJournal.value.copy(loading = false, error = true) }
        }
    }

    val engineSnapshot: StateFlow<PhaseEngineSnapshot> = BenchSessionCoordinator.engineSnapshot
    val settings: StateFlow<BenchSignalSettings> = BenchSessionCoordinator.settings
    val hardwareStatus: StateFlow<AudioHardwareStatus> = BenchSessionCoordinator.hardwareStatus
    val audioDiagnostics: StateFlow<List<AudioDiagnosticEntry>> = BenchSessionCoordinator.audioDiagnostics
    val eventLog: StateFlow<List<BenchEventLogEntry>> = BenchSessionCoordinator.eventLog
    val isWakeLockHeld: StateFlow<Boolean> = BenchSessionCoordinator.isWakeLockHeld
    val isServiceForeground: StateFlow<Boolean> = BenchSessionCoordinator.isServiceForeground

    val trainingStateReady = BenchSessionCoordinator.trainingStateReady
    val trainingConfigurations = BenchSessionCoordinator.trainingConfigurations
    val extraTrainingCycle = BenchSessionCoordinator.extraTrainingCycle
    fun saveTrainingConfiguration(value: com.example.data.TrainingConfiguration, onResult: (Boolean)->Unit) =
        BenchSessionCoordinator.saveTrainingConfiguration(value,onResult)
    fun resetTrainingConfiguration(key: String, onResult: (Boolean)->Unit) =
        BenchSessionCoordinator.resetTrainingConfiguration(key,onResult)
    fun trainingDefaultConfiguration(slot: com.example.data.DailySlotEntity) = BenchSessionCoordinator.trainingDefaultConfiguration(slot)

    val dailySlots: StateFlow<List<DailySlotWithCompletion>> = BenchSessionCoordinator.dailySlots
    val freeTemplates: StateFlow<List<com.example.data.FreeIntervalTemplateEntity>> = BenchSessionCoordinator.freeTemplates
    val customPrograms: StateFlow<List<com.example.domain.CustomWorkoutProgram>> = BenchSessionCoordinator.customPrograms
    val isZeroSlotDemoMode: StateFlow<Boolean> = BenchSessionCoordinator.isZeroSlotDemoMode
    val demoCompletionsCount: StateFlow<Int> = BenchSessionCoordinator.demoCompletionsCount
    val workoutEnergy: StateFlow<Int> = BenchSessionCoordinator.workoutEnergy
    val realCompletionsCount: StateFlow<Int> = BenchSessionCoordinator.realCompletionsCount
    val weeklyOverview: StateFlow<WeeklyOverviewState> = BenchSessionCoordinator.weeklyOverview
    val storyState: StateFlow<StoryReaderUiState> = BenchSessionCoordinator.storyState
    val pendingRestorePreview: StateFlow<BackupPreviewInfo?> = BenchSessionCoordinator.pendingRestorePreview
    val backupOperationBanner: StateFlow<String?> = BenchSessionCoordinator.backupOperationBanner
    val backupErrorMessage: StateFlow<String?> = BenchSessionCoordinator.backupErrorMessage
    val lastCompletedSlotDifficulty: StateFlow<WorkoutDifficulty?> = BenchSessionCoordinator.lastCompletedSlotDifficulty
    val celebratedMilestone: StateFlow<com.example.data.StreakMilestoneInfo?> = BenchSessionCoordinator.celebratedMilestone
    val databaseErrorMessage: StateFlow<String?> = BenchSessionCoordinator.databaseErrorMessage

    val availableProtocols: List<DemoProtocolConfig> = BenchSessionCoordinator.availableProtocols

    init {
        BenchSessionCoordinator.ensureInitialized(appContext)
        BenchSessionCoordinator.refreshHardwareStatus()
    }

    private val _chantalReturnToken = kotlinx.coroutines.flow.MutableStateFlow(0)
    val chantalReturnToken: StateFlow<Int> = _chantalReturnToken
    fun openChantalReturn() {
        _chantalReturnToken.value++
        navigateTo(VelarisNavDestination.CHANTAL)
    }

    val mainDestination get() = navigation.mainSection
    val canNavigateBack get() = navigation.canGoBack
    fun navigateTo(destination: VelarisNavDestination) = updateNavigation(navigation.open(destination))
    fun navigateBack() = updateNavigation(navigation.back())
    private fun updateNavigation(next: VelarisNavigation) {
        navigation = next
        savedState["velaris_routes"] = ArrayList(next.routes.map { it.id })
        _currentDestination.value = next.current
    }

    fun selectProtocol(protocol: DemoProtocolConfig) {
        BenchSessionCoordinator.selectProtocol(protocol)
    }

    fun selectDailySlot(slot: DailySlotWithCompletion) {
        BenchSessionCoordinator.selectDailySlot(slot)
    }

    fun startSlotAndOpenWorkout(slot: DailySlotWithCompletion) {
        BenchSessionCoordinator.selectDailySlot(slot)
        navigateTo(VelarisNavDestination.WORKOUT)
        startDemo()
    }

    fun startFreeWorkoutAndOpenWorkout(template: com.example.data.FreeIntervalTemplateEntity) {
        val started = BenchSessionCoordinator.startFreeSession(template)
        if (started) {
            navigateTo(VelarisNavDestination.WORKOUT)
            BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_START_FREE)
        }
    }

    fun saveFreeTemplate(template: com.example.data.FreeIntervalTemplateEntity) {
        BenchSessionCoordinator.saveFreeTemplate(template)
    }

    // --- Продвинутый конструктор тренировок ---

    fun startCustomProgramAndOpenWorkout(program: com.example.domain.CustomWorkoutProgram) {
        val started = BenchSessionCoordinator.startCustomProgramSession(program)
        if (started) {
            navigateTo(VelarisNavDestination.WORKOUT)
            BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_START_FREE)
        }
    }

    fun saveCustomProgram(program: com.example.domain.CustomWorkoutProgram, onResult: ((Boolean)->Unit)? = null) {
        BenchSessionCoordinator.saveCustomProgram(program,onResult)
    }

    fun deleteCustomProgram(id: String) {
        BenchSessionCoordinator.deleteCustomProgram(id)
    }

    fun duplicateCustomProgram(program: com.example.domain.CustomWorkoutProgram) {
        BenchSessionCoordinator.duplicateCustomProgram(program)
    }

    fun reorderCustomPrograms(reordered: List<com.example.domain.CustomWorkoutProgram>) {
        BenchSessionCoordinator.reorderCustomPrograms(reordered)
    }

    fun setZeroSlotDemoMode(enabled: Boolean) {
        BenchSessionCoordinator.setZeroSlotDemoMode(enabled)
    }

    fun startDemo() {
        BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_START_DEMO)
    }

    fun pauseDemo() {
        BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_PAUSE_MANUAL)
    }

    fun resumeDemo() {
        BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_RESUME_MANUAL)
    }

    fun requestStopDemo() {
        BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_REQUEST_STOP)
    }

    fun cancelStopDialog() {
        BenchSessionCoordinator.cancelStopConfirmation()
    }

    fun confirmStopDemo() {
        BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_CONFIRM_STOP)
    }

    fun resetDemo() {
        BenchSessionCoordinator.resetToIdle()
    }

    fun rateLastCompletedSlotDifficulty(difficulty: WorkoutDifficulty?) {
        BenchSessionCoordinator.rateLastCompletedSlotDifficulty(difficulty)
    }

    fun selectWeeklyOverviewDate(localDate: String) {
        BenchSessionCoordinator.selectWeeklyOverviewDate(localDate)
    }

    fun toggleFreezeDate(localDate: String) {
        BenchSessionCoordinator.toggleFreezeDate(localDate)
    }

    fun startStreakRescueBlitzAndOpenWorkout() {
        val started = BenchSessionCoordinator.startStreakRescueBlitzSession()
        if (started) {
            navigateTo(VelarisNavDestination.WORKOUT)
            BenchTimerService.sendCommand(appContext, BenchTimerService.ACTION_START_FREE)
        }
    }

    fun triggerMilestoneSalute(milestone: com.example.data.StreakMilestoneInfo) {
        BenchSessionCoordinator.triggerMilestoneSalute(milestone)
    }

    fun dismissMilestoneSalute() {
        BenchSessionCoordinator.dismissMilestoneSalute()
    }

    // --- Управление офлайн-читателем и импортом Markdown ---

    fun importMarkdownFromUri(uri: Uri) {
        BenchSessionCoordinator.prepareMarkdownImportFromUri(appContext, uri)
    }

    fun updateImportPreviewHeadingLevels(actHeadingLevel: Int, chapterHeadingLevel: Int) {
        BenchSessionCoordinator.updateImportPreviewHeadingLevels(actHeadingLevel, chapterHeadingLevel)
    }

    fun confirmPendingMarkdownImport() {
        BenchSessionCoordinator.confirmPendingMarkdownImport()
    }

    fun cancelPendingMarkdownImport() {
        BenchSessionCoordinator.cancelPendingMarkdownImport()
    }

    fun importNeutralTestMarkdown() {
        BenchSessionCoordinator.importNeutralTestMarkdown()
    }

    fun nextStoryStep() {
        BenchSessionCoordinator.nextStoryStep()
    }

    fun previousStoryStep() {
        BenchSessionCoordinator.previousStoryStep()
    }

    fun nextStoryChapter() {
        BenchSessionCoordinator.nextStoryChapter()
    }

    fun previousStoryChapter() {
        BenchSessionCoordinator.previousStoryChapter()
    }

    fun selectStoryChapter(chapterId: String) {
        BenchSessionCoordinator.selectStoryChapter(chapterId)
    }

    fun saveStoryBlockPosition(blockId: String) {
        BenchSessionCoordinator.saveStoryBlockPosition(blockId)
    }

    fun restartStoryFromBeginning() {
        BenchSessionCoordinator.restartStoryFromBeginning()
    }

    fun removeImportedBook() {
        BenchSessionCoordinator.removeImportedBook()
    }

    // --- Резервное копирование и восстановление (v1.2) ---

    fun createBackupToUri(uri: Uri) {
        BenchSessionCoordinator.createBackupToUri(appContext, uri)
    }

    fun inspectBackupFromUri(uri: Uri) {
        BenchSessionCoordinator.inspectBackupFromUri(appContext, uri)
    }

    fun confirmPendingRestore() {
        BenchSessionCoordinator.confirmPendingRestore(appContext)
    }

    fun cancelPendingRestore() {
        BenchSessionCoordinator.cancelPendingRestore()
    }

    fun dismissBackupBanners() {
        BenchSessionCoordinator.dismissBackupBanners()
    }

    // --- Настройки сигнала и интерфейса ---

    fun playSample(signal: SignalType) {
        BenchSessionCoordinator.playSampleSignal(signal)
    }

    fun setSoundPack(pack: SoundPack) {
        BenchSessionCoordinator.updateSettings { it.copy(soundPack = pack) }
    }

    fun setMetronomeTickStyle(style: MetronomeTickStyle) {
        BenchSessionCoordinator.updateSettings { it.copy(metronomeTickStyle = style) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        BenchSessionCoordinator.updateSettings { it.copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        BenchSessionCoordinator.updateSettings { it.copy(vibrationEnabled = enabled) }
    }

    fun setMetronomeEnabled(enabled: Boolean) {
        BenchSessionCoordinator.updateSettings { it.copy(metronomeEnabled = enabled) }
    }

    fun setSignalVolume(volume: Float) {
        BenchSessionCoordinator.updateSettings { it.copy(signalVolume = volume.coerceIn(0f, 1f)) }
    }

    fun setMetronomeVolume(volume: Float) {
        BenchSessionCoordinator.updateSettings { it.copy(metronomeVolume = volume.coerceIn(0f, 1f)) }
    }

    fun setDuckOtherApps(enabled: Boolean) {
        BenchSessionCoordinator.updateSettings { it.copy(duckOtherApps = enabled) }
    }

    fun setThemeId(themeId: VelarisThemeId) {
        BenchSessionCoordinator.updateSettings { it.copy(themeId = themeId) }
    }

    fun setReaderMode(mode: ReaderMode) {
        BenchSessionCoordinator.updateSettings { it.copy(readerMode = mode) }
    }

    fun setReaderFontSize(size: ReaderFontSize) {
        BenchSessionCoordinator.updateSettings { it.copy(readerFontSize = size) }
    }

    fun setReaderLineSpacing(spacing: ReaderLineSpacing) {
        BenchSessionCoordinator.updateSettings { it.copy(readerLineSpacing = spacing) }
    }

    fun setRealProgramEnabled(enabled: Boolean) {
        BenchSessionCoordinator.updateSettings { it.copy(isRealProgramEnabled = enabled) }
    }

    fun refreshHardwareStatus() {
        BenchSessionCoordinator.refreshHardwareStatus()
        BenchSessionCoordinator.refreshDailySlotsAsync()
    }

    fun clearEventLog() {
        BenchSessionCoordinator.clearLog()
    }

    fun clearAudioDiagnostics() {
        BenchSessionCoordinator.clearAudioDiagnostics()
    }

    class Factory(
        private val appContext: Context,
        private val repository: AppBuildInfoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            TechCheckViewModel(appContext, repository, extras.createSavedStateHandle()) as T

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TechCheckViewModel(appContext, repository) as T
        }
    }
}
