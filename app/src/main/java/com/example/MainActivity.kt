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
            VelarisTheme {
                val viewModel: TechCheckViewModel = viewModel(
                    factory = TechCheckViewModel.Factory(applicationContext, repository)
                )
                val buildInfo by viewModel.buildInfo.collectAsStateWithLifecycle()
                val snapshot by viewModel.engineSnapshot.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val hardwareStatus by viewModel.hardwareStatus.collectAsStateWithLifecycle()
                val eventLog by viewModel.eventLog.collectAsStateWithLifecycle()
                val isWakeLockHeld by viewModel.isWakeLockHeld.collectAsStateWithLifecycle()
                val isServiceForeground by viewModel.isServiceForeground.collectAsStateWithLifecycle()
                val dailySlots by viewModel.dailySlots.collectAsStateWithLifecycle()
                val isZeroSlotDemoMode by viewModel.isZeroSlotDemoMode.collectAsStateWithLifecycle()
                val demoCompletionsCount by viewModel.demoCompletionsCount.collectAsStateWithLifecycle()
                val realCompletionsCount by viewModel.realCompletionsCount.collectAsStateWithLifecycle()
                val databaseErrorMessage by viewModel.databaseErrorMessage.collectAsStateWithLifecycle()

                TechCheckScreen(
                    buildInfo = buildInfo,
                    snapshot = snapshot,
                    settings = settings,
                    hardwareStatus = hardwareStatus,
                    eventLog = eventLog,
                    isWakeLockHeld = isWakeLockHeld,
                    isServiceForeground = isServiceForeground,
                    dailySlots = dailySlots,
                    isZeroSlotDemoMode = isZeroSlotDemoMode,
                    demoCompletionsCount = demoCompletionsCount,
                    realCompletionsCount = realCompletionsCount,
                    databaseErrorMessage = databaseErrorMessage,
                    availableProtocols = viewModel.availableProtocols,
                    onSelectProtocol = viewModel::selectProtocol,
                    onSelectDailySlot = viewModel::selectDailySlot,
                    onZeroSlotDemoModeChange = viewModel::setZeroSlotDemoMode,
                    onStartDemo = viewModel::startDemo,
                    onPauseDemo = viewModel::pauseDemo,
                    onResumeDemo = viewModel::resumeDemo,
                    onRequestStopDemo = viewModel::requestStopDemo,
                    onCancelStopDialog = viewModel::cancelStopDialog,
                    onConfirmStopDemo = viewModel::confirmStopDemo,
                    onResetDemo = viewModel::resetDemo,
                    onPlaySample = viewModel::playSample,
                    onSoundPackChange = viewModel::setSoundPack,
                    onMetronomeTickStyleChange = viewModel::setMetronomeTickStyle,
                    onSoundEnabledChange = viewModel::setSoundEnabled,
                    onVibrationEnabledChange = viewModel::setVibrationEnabled,
                    onMetronomeEnabledChange = viewModel::setMetronomeEnabled,
                    onSignalVolumeChange = viewModel::setSignalVolume,
                    onMetronomeVolumeChange = viewModel::setMetronomeVolume,
                    onRefreshHardware = viewModel::refreshHardwareStatus,
                    onClearLog = viewModel::clearEventLog
                )
            }
        }
    }
}
