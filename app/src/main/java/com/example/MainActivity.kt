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

                TechCheckScreen(
                    buildInfo = buildInfo,
                    snapshot = snapshot,
                    settings = settings,
                    hardwareStatus = hardwareStatus,
                    eventLog = eventLog,
                    isWakeLockHeld = isWakeLockHeld,
                    isServiceForeground = isServiceForeground,
                    availableProtocols = viewModel.availableProtocols,
                    onSelectProtocol = viewModel::selectProtocol,
                    onStartDemo = viewModel::startDemo,
                    onPauseDemo = viewModel::pauseDemo,
                    onResumeDemo = viewModel::resumeDemo,
                    onRequestStopDemo = viewModel::requestStopDemo,
                    onCancelStopDialog = viewModel::cancelStopDialog,
                    onConfirmStopDemo = viewModel::confirmStopDemo,
                    onResetDemo = viewModel::resetDemo,
                    onPlaySample = viewModel::playSample,
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
