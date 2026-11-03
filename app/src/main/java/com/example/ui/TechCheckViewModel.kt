package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppBuildInfoRepository
import com.example.data.AudioDiagnosticEntry
import com.example.data.AudioHardwareStatus
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
 * Навигационные разделы приложения Velaris (Итерация v1.1).
 * Вся техническая диагностика вынесена в отдельный вложенный экран `DIAGNOSTICS` («Настройки → Диагностика»).
 */
enum class VelarisNavDestination(
    val id: String,
    val titleRu: String
) {
    HOME("home", "Главная"),
    PLAN("plan", "План"),
    WORKOUT("workout", "Подход"),
    READER("reader", "Чтение"),
    SETTINGS("settings", "Настройки"),
    DIAGNOSTICS("diagnostics", "Диагностика")
}

/**
 * ViewModel приложения Velaris (v0.4.0-v1.1).
 * Подключается к единому координатору BenchSessionCoordinator и управляет навигацией между экранами,
 * не создавая второго таймера при смене экранов.
 */
class TechCheckViewModel(
    private val appContext: Context,
    private val repository: AppBuildInfoRepository
) : ViewModel() {

    private val _buildInfo = MutableStateFlow(repository.loadBuildInfo())
    val buildInfo: StateFlow<AppBuildInfo> = _buildInfo.asStateFlow()

    private val _currentDestination = MutableStateFlow(VelarisNavDestination.HOME)
    val currentDestination: StateFlow<VelarisNavDestination> = _currentDestination.asStateFlow()

    val engineSnapshot: StateFlow<PhaseEngineSnapshot> = BenchSessionCoordinator.engineSnapshot
    val settings: StateFlow<BenchSignalSettings> = BenchSessionCoordinator.settings
    val hardwareStatus: StateFlow<AudioHardwareStatus> = BenchSessionCoordinator.hardwareStatus
    val audioDiagnostics: StateFlow<List<AudioDiagnosticEntry>> = BenchSessionCoordinator.audioDiagnostics
    val eventLog: StateFlow<List<BenchEventLogEntry>> = BenchSessionCoordinator.eventLog
    val isWakeLockHeld: StateFlow<Boolean> = BenchSessionCoordinator.isWakeLockHeld
    val isServiceForeground: StateFlow<Boolean> = BenchSessionCoordinator.isServiceForeground

    val dailySlots: StateFlow<List<DailySlotWithCompletion>> = BenchSessionCoordinator.dailySlots
    val isZeroSlotDemoMode: StateFlow<Boolean> = BenchSessionCoordinator.isZeroSlotDemoMode
    val demoCompletionsCount: StateFlow<Int> = BenchSessionCoordinator.demoCompletionsCount
    val realCompletionsCount: StateFlow<Int> = BenchSessionCoordinator.realCompletionsCount
    val weeklyOverview: StateFlow<WeeklyOverviewState> = BenchSessionCoordinator.weeklyOverview
    val storyState: StateFlow<StoryReaderUiState> = BenchSessionCoordinator.storyState
    val lastCompletedSlotDifficulty: StateFlow<WorkoutDifficulty?> = BenchSessionCoordinator.lastCompletedSlotDifficulty
    val databaseErrorMessage: StateFlow<String?> = BenchSessionCoordinator.databaseErrorMessage

    val availableProtocols: List<DemoProtocolConfig> = BenchSessionCoordinator.availableProtocols

    init {
        BenchSessionCoordinator.ensureInitialized(appContext)
        BenchSessionCoordinator.refreshHardwareStatus()
    }

    fun navigateTo(destination: VelarisNavDestination) {
        _currentDestination.value = destination
    }

    fun selectProtocol(protocol: DemoProtocolConfig) {
        BenchSessionCoordinator.selectProtocol(protocol)
    }

    fun selectDailySlot(slot: DailySlotWithCompletion) {
        BenchSessionCoordinator.selectDailySlot(slot)
    }

    fun startSlotAndOpenWorkout(slot: DailySlotWithCompletion) {
        BenchSessionCoordinator.selectDailySlot(slot)
        _currentDestination.value = VelarisNavDestination.WORKOUT
        startDemo()
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TechCheckViewModel(appContext, repository) as T
        }
    }
}
