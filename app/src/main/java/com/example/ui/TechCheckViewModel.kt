package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppBuildInfoRepository
import com.example.data.AudioHardwareStatus
import com.example.data.BenchSignalSettings
import com.example.domain.AppBuildInfo
import com.example.domain.BenchEventLogEntry
import com.example.domain.DemoProtocolConfig
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.SignalType
import com.example.service.BenchSessionCoordinator
import com.example.service.BenchTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel технического стенда этапа 0B.
 * Подключается к единому координатору BenchSessionCoordinator и управляет командами BenchTimerService.
 */
class TechCheckViewModel(
    private val appContext: Context,
    private val repository: AppBuildInfoRepository
) : ViewModel() {

    private val _buildInfo = MutableStateFlow(repository.loadBuildInfo())
    val buildInfo: StateFlow<AppBuildInfo> = _buildInfo.asStateFlow()

    val engineSnapshot: StateFlow<PhaseEngineSnapshot> = BenchSessionCoordinator.engineSnapshot
    val settings: StateFlow<BenchSignalSettings> = BenchSessionCoordinator.settings
    val hardwareStatus: StateFlow<AudioHardwareStatus> = BenchSessionCoordinator.hardwareStatus
    val eventLog: StateFlow<List<BenchEventLogEntry>> = BenchSessionCoordinator.eventLog
    val isWakeLockHeld: StateFlow<Boolean> = BenchSessionCoordinator.isWakeLockHeld
    val isServiceForeground: StateFlow<Boolean> = BenchSessionCoordinator.isServiceForeground

    val availableProtocols: List<DemoProtocolConfig> = BenchSessionCoordinator.availableProtocols

    init {
        BenchSessionCoordinator.ensureInitialized(appContext)
        BenchSessionCoordinator.refreshHardwareStatus()
    }

    fun selectProtocol(protocol: DemoProtocolConfig) {
        BenchSessionCoordinator.selectProtocol(protocol)
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

    fun playSample(signal: SignalType) {
        BenchSessionCoordinator.playSampleSignal(signal)
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

    fun refreshHardwareStatus() {
        BenchSessionCoordinator.refreshHardwareStatus()
    }

    fun clearEventLog() {
        BenchSessionCoordinator.clearLog()
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
