package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppBuildInfoRepository
import com.example.data.AudioHardwareStatus
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.domain.AppBuildInfo
import com.example.domain.BenchEventLogEntry
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.service.BenchSessionCoordinator
import com.example.service.BenchTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel технического стенда (Этап 0B + Этап 2 + Плавный таймер + Звуковые наборы).
 * Подключается к единому координатору BenchSessionCoordinator и управляет командами BenchTimerService,
 * слотами дневного плана Room и настройками звуковых наборов DataStore.
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

    val dailySlots: StateFlow<List<DailySlotWithCompletion>> = BenchSessionCoordinator.dailySlots
    val isZeroSlotDemoMode: StateFlow<Boolean> = BenchSessionCoordinator.isZeroSlotDemoMode
    val demoCompletionsCount: StateFlow<Int> = BenchSessionCoordinator.demoCompletionsCount
    val realCompletionsCount: StateFlow<Int> = BenchSessionCoordinator.realCompletionsCount
    val databaseErrorMessage: StateFlow<String?> = BenchSessionCoordinator.databaseErrorMessage

    val availableProtocols: List<DemoProtocolConfig> = BenchSessionCoordinator.availableProtocols

    init {
        BenchSessionCoordinator.ensureInitialized(appContext)
        BenchSessionCoordinator.refreshHardwareStatus()
    }

    fun selectProtocol(protocol: DemoProtocolConfig) {
        BenchSessionCoordinator.selectProtocol(protocol)
    }

    fun selectDailySlot(slot: DailySlotWithCompletion) {
        BenchSessionCoordinator.selectDailySlot(slot)
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

    fun refreshHardwareStatus() {
        BenchSessionCoordinator.refreshHardwareStatus()
        BenchSessionCoordinator.refreshDailySlotsAsync()
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
