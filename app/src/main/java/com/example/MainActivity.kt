package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppBuildInfoRepository
import com.example.ui.TechCheckScreen
import com.example.ui.TechCheckViewModel
import com.example.ui.theme.VelarisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AppBuildInfoRepository(applicationContext)

        setContent {
            val viewModel: TechCheckViewModel = viewModel(
                factory = TechCheckViewModel.Factory(applicationContext, repository)
            )
            val buildInfo by viewModel.buildInfo.collectAsStateWithLifecycle()
            val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
            val snapshot by viewModel.engineSnapshot.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val hardwareStatus by viewModel.hardwareStatus.collectAsStateWithLifecycle()
            val audioDiagnostics by viewModel.audioDiagnostics.collectAsStateWithLifecycle()
            val eventLog by viewModel.eventLog.collectAsStateWithLifecycle()
            val isWakeLockHeld by viewModel.isWakeLockHeld.collectAsStateWithLifecycle()
            val isServiceForeground by viewModel.isServiceForeground.collectAsStateWithLifecycle()
            val dailySlots by viewModel.dailySlots.collectAsStateWithLifecycle()
            val isZeroSlotDemoMode by viewModel.isZeroSlotDemoMode.collectAsStateWithLifecycle()
            val demoCompletionsCount by viewModel.demoCompletionsCount.collectAsStateWithLifecycle()
            val realCompletionsCount by viewModel.realCompletionsCount.collectAsStateWithLifecycle()
            val weeklyOverview by viewModel.weeklyOverview.collectAsStateWithLifecycle()
            val storyState by viewModel.storyState.collectAsStateWithLifecycle()
            val lastCompletedSlotDifficulty by viewModel.lastCompletedSlotDifficulty.collectAsStateWithLifecycle()
            val databaseErrorMessage by viewModel.databaseErrorMessage.collectAsStateWithLifecycle()

            VelarisTheme(themeId = settings.themeId) {
                TechCheckScreen(
                    buildInfo = buildInfo,
                    currentDestination = currentDestination,
                    onNavigate = viewModel::navigateTo,
                    snapshot = snapshot,
                    settings = settings,
                    hardwareStatus = hardwareStatus,
                    audioDiagnostics = audioDiagnostics,
                    eventLog = eventLog,
                    isWakeLockHeld = isWakeLockHeld,
                    isServiceForeground = isServiceForeground,
                    dailySlots = dailySlots,
                    isZeroSlotDemoMode = isZeroSlotDemoMode,
                    demoCompletionsCount = demoCompletionsCount,
                    realCompletionsCount = realCompletionsCount,
                    weeklyOverview = weeklyOverview,
                    storyState = storyState,
                    lastCompletedSlotDifficulty = lastCompletedSlotDifficulty,
                    databaseErrorMessage = databaseErrorMessage,
                    availableProtocols = viewModel.availableProtocols,
                    onSelectProtocol = viewModel::selectProtocol,
                    onSelectDailySlot = viewModel::selectDailySlot,
                    onStartSlotAndOpenWorkout = viewModel::startSlotAndOpenWorkout,
                    onZeroSlotDemoModeChange = viewModel::setZeroSlotDemoMode,
                    onStartDemo = viewModel::startDemo,
                    onPauseDemo = viewModel::pauseDemo,
                    onResumeDemo = viewModel::resumeDemo,
                    onRequestStopDemo = viewModel::requestStopDemo,
                    onCancelStopDialog = viewModel::cancelStopDialog,
                    onConfirmStopDemo = viewModel::confirmStopDemo,
                    onResetDemo = viewModel::resetDemo,
                    onRateDifficulty = viewModel::rateLastCompletedSlotDifficulty,
                    onSelectWeeklyDate = viewModel::selectWeeklyOverviewDate,
                    onImportMarkdownUri = viewModel::importMarkdownFromUri,
                    onUpdatePreviewHeadingLevels = viewModel::updateImportPreviewHeadingLevels,
                    onConfirmPendingImport = viewModel::confirmPendingMarkdownImport,
                    onCancelPendingImport = viewModel::cancelPendingMarkdownImport,
                    onImportNeutralTestMarkdown = viewModel::importNeutralTestMarkdown,
                    onNextStoryStep = viewModel::nextStoryStep,
                    onPreviousStoryStep = viewModel::previousStoryStep,
                    onNextStoryChapter = viewModel::nextStoryChapter,
                    onPreviousStoryChapter = viewModel::previousStoryChapter,
                    onSelectStoryChapter = viewModel::selectStoryChapter,
                    onSaveStoryBlockPosition = viewModel::saveStoryBlockPosition,
                    onRestartStoryFromBeginning = viewModel::restartStoryFromBeginning,
                    onRemoveImportedBook = viewModel::removeImportedBook,
                    onPlaySample = viewModel::playSample,
                    onThemeChange = viewModel::setThemeId,
                    onSoundPackChange = viewModel::setSoundPack,
                    onMetronomeTickStyleChange = viewModel::setMetronomeTickStyle,
                    onSoundEnabledChange = viewModel::setSoundEnabled,
                    onVibrationEnabledChange = viewModel::setVibrationEnabled,
                    onMetronomeEnabledChange = viewModel::setMetronomeEnabled,
                    onSignalVolumeChange = viewModel::setSignalVolume,
                    onMetronomeVolumeChange = viewModel::setMetronomeVolume,
                    onReaderModeChange = viewModel::setReaderMode,
                    onReaderFontSizeChange = viewModel::setReaderFontSize,
                    onReaderLineSpacingChange = viewModel::setReaderLineSpacing,
                    onRealProgramEnabledChange = viewModel::setRealProgramEnabled,
                    onRefreshHardware = viewModel::refreshHardwareStatus,
                    onClearAudioDiagnostics = viewModel::clearAudioDiagnostics,
                    onClearLog = viewModel::clearEventLog
                )
            }
        }
    }
}
