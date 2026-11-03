package com.example.ui

import android.Manifest
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.example.R
import com.example.data.AudioHardwareStatus
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.domain.AppBuildInfo
import com.example.domain.BenchEventLogEntry
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.RunStatus
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.ui.theme.DawnBackgroundBottom
import com.example.ui.theme.DawnBackgroundMid
import com.example.ui.theme.DawnBackgroundTop
import com.example.ui.theme.DawnErrorAccent
import com.example.ui.theme.DawnOnPrimaryAccent
import com.example.ui.theme.DawnOutline
import com.example.ui.theme.DawnPrimaryAccent
import com.example.ui.theme.DawnSecondaryAccent
import com.example.ui.theme.DawnSuccessAccent
import com.example.ui.theme.DawnSurface
import com.example.ui.theme.DawnSurfaceVariant
import com.example.ui.theme.DawnTextPrimary
import com.example.ui.theme.DawnTextSecondary
import com.example.ui.theme.DawnWarningAccent
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun TechCheckScreen(
    buildInfo: AppBuildInfo,
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    hardwareStatus: AudioHardwareStatus,
    eventLog: List<BenchEventLogEntry>,
    isWakeLockHeld: Boolean,
    isServiceForeground: Boolean,
    dailySlots: List<DailySlotWithCompletion>,
    isZeroSlotDemoMode: Boolean,
    demoCompletionsCount: Int,
    realCompletionsCount: Int,
    databaseErrorMessage: String?,
    availableProtocols: List<DemoProtocolConfig>,
    onSelectProtocol: (DemoProtocolConfig) -> Unit,
    onSelectDailySlot: (DailySlotWithCompletion) -> Unit,
    onZeroSlotDemoModeChange: (Boolean) -> Unit,
    onStartDemo: () -> Unit,
    onPauseDemo: () -> Unit,
    onResumeDemo: () -> Unit,
    onRequestStopDemo: () -> Unit,
    onCancelStopDialog: () -> Unit,
    onConfirmStopDemo: () -> Unit,
    onResetDemo: () -> Unit,
    onPlaySample: (SignalType) -> Unit,
    onSoundPackChange: (SoundPack) -> Unit,
    onMetronomeTickStyleChange: (MetronomeTickStyle) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onSignalVolumeChange: (Float) -> Unit,
    onMetronomeVolumeChange: (Float) -> Unit,
    onRefreshHardware: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(DawnBackgroundTop, DawnBackgroundMid, DawnBackgroundBottom)
    )

    // Диалог остановки: «Завершить без зачёта?» -> «Завершить» / «Остаться на паузе» (ТЗ п. 7)
    if (snapshot.isStopDialogVisible) {
        AlertDialog(
            onDismissRequest = onCancelStopDialog,
            containerColor = DawnSurface,
            titleContentColor = DawnTextPrimary,
            textContentColor = DawnTextSecondary,
            title = {
                Text(
                    text = stringResource(R.string.stop_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("stop_dialog_title")
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.stop_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmStopDemo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DawnErrorAccent,
                        contentColor = DawnOnPrimaryAccent
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_confirm_stop")
                ) {
                    Text(
                        text = stringResource(R.string.stop_dialog_confirm),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancelStopDialog,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_cancel_stop")
                ) {
                    Text(
                        text = stringResource(R.string.stop_dialog_cancel),
                        color = DawnTextPrimary
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            MountainSilhouetteBackground(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DemoSafetyBanner()
                    HeaderStageCard(buildInfo = buildInfo)
                    ActiveTimerBenchCard(
                        snapshot = snapshot,
                        hardwareStatus = hardwareStatus,
                        isWakeLockHeld = isWakeLockHeld,
                        isServiceForeground = isServiceForeground,
                        dailySlots = dailySlots,
                        isZeroSlotDemoMode = isZeroSlotDemoMode,
                        availableProtocols = availableProtocols,
                        onSelectProtocol = onSelectProtocol,
                        onStartDemo = onStartDemo,
                        onPauseDemo = onPauseDemo,
                        onResumeDemo = onResumeDemo,
                        onRequestStopDemo = onRequestStopDemo,
                        onResetDemo = onResetDemo
                    )
                    DailyPlanPersistenceCard(
                        snapshot = snapshot,
                        dailySlots = dailySlots,
                        isZeroSlotDemoMode = isZeroSlotDemoMode,
                        demoCompletionsCount = demoCompletionsCount,
                        realCompletionsCount = realCompletionsCount,
                        databaseErrorMessage = databaseErrorMessage,
                        onSelectDailySlot = onSelectDailySlot,
                        onZeroSlotDemoModeChange = onZeroSlotDemoModeChange
                    )
                    SignalSettingsAndSamplesCard(
                        snapshot = snapshot,
                        settings = settings,
                        onSoundPackChange = onSoundPackChange,
                        onMetronomeTickStyleChange = onMetronomeTickStyleChange,
                        onSoundEnabledChange = onSoundEnabledChange,
                        onVibrationEnabledChange = onVibrationEnabledChange,
                        onMetronomeEnabledChange = onMetronomeEnabledChange,
                        onSignalVolumeChange = onSignalVolumeChange,
                        onMetronomeVolumeChange = onMetronomeVolumeChange,
                        onPlaySample = onPlaySample
                    )
                    NotificationAndHardwareCard(
                        hardwareStatus = hardwareStatus,
                        onRefreshHardware = onRefreshHardware
                    )
                    EventLogCard(
                        eventLog = eventLog,
                        onClearLog = onClearLog
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoSafetyBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, DawnWarningAccent.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .testTag("demo_safety_banner"),
        shape = RoundedCornerShape(16.dp),
        color = DawnSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Предупреждение демо-режима",
                tint = DawnWarningAccent,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.demo_safety_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DawnWarningAccent,
                    modifier = Modifier.testTag("demo_safety_title")
                )
                Text(
                    text = stringResource(R.string.demo_safety_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextPrimary
                )
            }
        }
    }
}

@Composable
private fun HeaderStageCard(buildInfo: AppBuildInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DawnOutline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = DawnSurface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DawnSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("stage_badge")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DawnPrimaryAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tech_check_stage_badge),
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnPrimaryAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.tech_check_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DawnTextPrimary,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag("screen_title")
            )

            Text(
                text = stringResource(
                    R.string.tech_check_version_format,
                    buildInfo.versionName,
                    buildInfo.versionCode
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = DawnSecondaryAccent,
                modifier = Modifier.testTag("version_info")
            )

            Text(
                text = "Пакет: ${buildInfo.applicationId} · ${buildInfo.deviceManufacturer} ${buildInfo.deviceModel} (Android ${buildInfo.androidRelease}, API ${buildInfo.androidSdkInt})",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = DawnTextSecondary,
                modifier = Modifier.testTag("package_and_device_summary")
            )
        }
    }
}

@Composable
private fun ActiveTimerBenchCard(
    snapshot: PhaseEngineSnapshot,
    hardwareStatus: AudioHardwareStatus,
    isWakeLockHeld: Boolean,
    isServiceForeground: Boolean,
    dailySlots: List<DailySlotWithCompletion>,
    isZeroSlotDemoMode: Boolean,
    availableProtocols: List<DemoProtocolConfig>,
    onSelectProtocol: (DemoProtocolConfig) -> Unit,
    onStartDemo: () -> Unit,
    onPauseDemo: () -> Unit,
    onResumeDemo: () -> Unit,
    onRequestStopDemo: () -> Unit,
    onResetDemo: () -> Unit
) {
    val selectedSlotCompleted = dailySlots.any { it.slot.id == snapshot.protocol.id && it.isCompleted }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timer_bench_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Индикатор фактического аудиовыхода вверху карточки подхода (ТЗ п. 5)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DawnSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("audio_route_indicator")
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = "Маршрут аудиовыхода",
                    tint = DawnSecondaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Аудиовыход: ${hardwareStatus.outputRouteLabelRu}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnTextPrimary
                )
            }

            // Предупреждение, если ни один канал сигнала (звук или вибрация) не доступен
            if (hardwareStatus.noActiveSignalChannelWarning != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("no_signal_channel_warning"),
                    shape = RoundedCornerShape(12.dp),
                    color = DawnErrorAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = hardwareStatus.noActiveSignalChannelWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnErrorAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Большое плавное кольцо фазы (ТЗ п. 5, п. 7 + плавное движение от единого монотонного источника)
            PhaseRingIndicator(snapshot = snapshot)

            // Заголовок текущей команды (объявляется TalkBack только при смене фазы/состояния, а не каждую секунду)
            // Меняется мгновенно в точный момент перехода фазы вместе со звуковым сигналом
            Text(
                text = snapshot.headlineCommandRu,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when (snapshot.status) {
                    RunStatus.RUNNING -> if (snapshot.currentPhase == PhaseType.CONTRACTION) DawnPrimaryAccent else DawnSecondaryAccent
                    RunStatus.PAUSED, RunStatus.INTERRUPTED -> DawnWarningAccent
                    RunStatus.COMPLETED -> DawnSuccessAccent
                    else -> DawnTextPrimary
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("phase_headline")
            )

            // Счётчик повторений и мелкая строка общего остатка активного времени подхода
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Повтор",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnTextSecondary
                    )
                    Text(
                        text = "Повтор ${snapshot.currentRepetition} из ${snapshot.totalRepetitions}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DawnTextPrimary,
                        modifier = Modifier.testTag("repetition_counter")
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Осталось активного",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnTextSecondary
                    )
                    Text(
                        text = formatWholeSeconds(snapshot.remainingActiveMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = DawnTextPrimary,
                        modifier = Modifier.testTag("remaining_active_time")
                    )
                }
            }

            // Информация о зафиксированном звуковом наборе для активной/приостановленной попытки
            if (snapshot.isActiveOrPaused) {
                Text(
                    text = "Звуковой набор попытки: «${snapshot.soundPackSnapshot.titleRu}» · Метроном: «${snapshot.metronomeStyleSnapshot.titleRu}» · Дата старта: ${snapshot.startDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DawnTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("active_run_snapshot_info")
                )
            }

            // Причина паузы или внешнего прерывания
            if (snapshot.interruptionReason != null &&
                (snapshot.status == RunStatus.PAUSED || snapshot.status == RunStatus.INTERRUPTED)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interruption_reason_box"),
                    shape = RoundedCornerShape(12.dp),
                    color = DawnSurfaceVariant
                ) {
                    Text(
                        text = "Причина паузы: ${snapshot.interruptionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnWarningAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Выбор тестовой последовательности (когда нет активного запуска и не выбран режим N=0)
            if (!snapshot.isActiveOrPaused && !isZeroSlotDemoMode && dailySlots.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DawnSurfaceVariant.copy(alpha = 0.65f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Выбор тестовой последовательности (Демо):",
                        style = MaterialTheme.typography.labelLarge,
                        color = DawnTextSecondary
                    )
                    availableProtocols.forEach { proto ->
                        val selected = proto.id == snapshot.protocol.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectProtocol(proto) }
                                .padding(horizontal = 4.dp)
                                .testTag("protocol_option_${proto.id}")
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectProtocol(proto) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DawnPrimaryAccent,
                                    unselectedColor = DawnTextSecondary
                                )
                            )
                            Text(
                                text = proto.titleRu,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DawnTextPrimary
                            )
                        }
                    }
                }
            }

            // Кнопки управления (не менее 48 dp по высоте, ТЗ п. 4 и п. 5)
            when (snapshot.status) {
                RunStatus.IDLE,
                RunStatus.COMPLETED,
                RunStatus.STOPPED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onStartDemo,
                            enabled = !isZeroSlotDemoMode && !selectedSlotCompleted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DawnPrimaryAccent,
                                contentColor = DawnOnPrimaryAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("btn_start_demo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isZeroSlotDemoMode -> "Сегодня без тренировок"
                                    selectedSlotCompleted -> "Слот уже зачтён ✓"
                                    else -> "Старт демо-сессии"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (snapshot.status == RunStatus.COMPLETED || snapshot.status == RunStatus.STOPPED) {
                            OutlinedButton(
                                onClick = onResetDemo,
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("btn_reset_demo")
                            ) {
                                Text(text = "Сброс", color = DawnTextPrimary)
                            }
                        }
                    }
                }

                RunStatus.PREPARING,
                RunStatus.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPauseDemo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DawnPrimaryAccent,
                                contentColor = DawnOnPrimaryAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("btn_pause_demo")
                        ) {
                            Icon(imageVector = Icons.Filled.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Пауза",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onRequestStopDemo,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("btn_stop_demo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                tint = DawnErrorAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Остановить",
                                style = MaterialTheme.typography.titleSmall,
                                color = DawnErrorAccent
                            )
                        }
                    }
                }

                RunStatus.PAUSED,
                RunStatus.INTERRUPTED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onResumeDemo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DawnSuccessAccent,
                                contentColor = DawnOnPrimaryAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("btn_resume_demo")
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Продолжить",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onRequestStopDemo,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("btn_stop_demo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                tint = DawnErrorAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Остановить",
                                style = MaterialTheme.typography.titleSmall,
                                color = DawnErrorAccent
                            )
                        }
                    }
                }
            }

            // Техническая строка телеметрии планировщика, FGS и WakeLock
            Text(
                text = "FGS: ${if (isServiceForeground) "Активен" else "Выкл"} · " +
                    "WakeLock: ${if (isWakeLockHeld) "Удерживается" else "Освобождён"} · " +
                    "Дрейф тика: ${snapshot.lastSchedulerDriftMs} мс (макс ${snapshot.maxSchedulerDriftMs} мс)",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = DawnTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("telemetry_status_line")
            )
        }
    }
}

/**
 * Плавный индикатор фазы:
 * - Непрерывное движение кольца во время видимой активной сессии (`PREPARING` / `RUNNING`)
 *   от того же единственного монотонного источника `SystemClock.elapsedRealtime()`.
 * - Цифры показывают только целые секунды без десятых долей и имеют фиксированную ширину (`tnum` + `Monospace`),
 *   чтобы числа не прыгали при смене цифр.
 * - Цвет кольца мягко переходит за 180 мс, тогда как текст фазы и звуковой сигнал меняются мгновенно.
 * - При паузе кольцо останавливается мгновенно без инерции; при смене фазы нет обратного пролёта через весь круг.
 * - Учитывает системное отключение анимаций (`ValueAnimator.areAnimatorsEnabled()`) и прекращает кадровые
 *   обновления, когда экран скрыт или сессия не активна.
 */
@Composable
private fun PhaseRingIndicator(snapshot: PhaseEngineSnapshot) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val isScreenVisible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val animationsEnabled = remember(snapshot.status, snapshot.currentPhase) {
        runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
    }

    var frameElapsedRealtimeMs by remember(
        snapshot.status,
        snapshot.currentPhase,
        snapshot.currentRepetition,
        snapshot.snapshotElapsedRealtimeMs
    ) {
        mutableLongStateOf(snapshot.snapshotElapsedRealtimeMs)
    }

    // Кадровый цикл работает ТОЛЬКО когда экран видим, сессия активна и анимации разрешены системой
    LaunchedEffect(
        isScreenVisible,
        snapshot.isRunningOrPreparing,
        animationsEnabled,
        snapshot.snapshotElapsedRealtimeMs
    ) {
        if (isScreenVisible && snapshot.isRunningOrPreparing && animationsEnabled) {
            frameElapsedRealtimeMs = SystemClock.elapsedRealtime()
            while (isActive) {
                withInfiniteAnimationFrameMillis {
                    frameElapsedRealtimeMs = SystemClock.elapsedRealtime()
                }
            }
        } else {
            frameElapsedRealtimeMs = snapshot.snapshotElapsedRealtimeMs
        }
    }

    val effectiveElapsedMs = if (isScreenVisible && snapshot.isRunningOrPreparing && animationsEnabled) {
        frameElapsedRealtimeMs
    } else {
        snapshot.snapshotElapsedRealtimeMs
    }

    val progress = snapshot.computeVisualRingProgress(effectiveElapsedMs)
    val displaySeconds = snapshot.computeDisplayWholeSeconds(effectiveElapsedMs)

    val targetRingColor = when (snapshot.status) {
        RunStatus.PREPARING -> DawnWarningAccent
        RunStatus.RUNNING -> if (snapshot.currentPhase == PhaseType.CONTRACTION) DawnPrimaryAccent else DawnSecondaryAccent
        RunStatus.PAUSED, RunStatus.INTERRUPTED -> DawnWarningAccent
        RunStatus.COMPLETED -> DawnSuccessAccent
        else -> DawnOutline
    }

    val animatedRingColor by animateColorAsState(
        targetValue = targetRingColor,
        animationSpec = tween(durationMillis = if (animationsEnabled) 180 else 0),
        label = "phase_ring_color_transition"
    )

    // Важно по ТЗ п. 4: не озвучивать каждое обновление секунды через TalkBack
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(196.dp)
            .testTag("phase_ring_box")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = DawnSurfaceVariant,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            drawArc(
                color = animatedRingColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%d с", displaySeconds),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = DawnTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 116.dp)
                    .testTag("phase_remaining_seconds")
            )
            Text(
                text = when (snapshot.status) {
                    RunStatus.PREPARING -> "подготовка"
                    RunStatus.RUNNING -> if (snapshot.currentPhase == PhaseType.CONTRACTION) "сокращение" else "расслабление"
                    RunStatus.PAUSED, RunStatus.INTERRUPTED -> "на паузе"
                    RunStatus.COMPLETED -> "выполнено"
                    RunStatus.STOPPED -> "остановлено"
                    RunStatus.IDLE -> "ожидание"
                },
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSecondary
            )
        }
    }
}

@Composable
private fun DailyPlanPersistenceCard(
    snapshot: PhaseEngineSnapshot,
    dailySlots: List<DailySlotWithCompletion>,
    isZeroSlotDemoMode: Boolean,
    demoCompletionsCount: Int,
    realCompletionsCount: Int,
    databaseErrorMessage: String?,
    onSelectDailySlot: (DailySlotWithCompletion) -> Unit,
    onZeroSlotDemoModeChange: (Boolean) -> Unit
) {
    val completedToday = dailySlots.count { it.isCompleted }
    val totalToday = dailySlots.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_plan_persistence_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = DawnPrimaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Этап 2 · Дневной план и база Room",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DawnTextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
            }

            if (databaseErrorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("db_error_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = DawnErrorAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = databaseErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnErrorAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Сводка «Сегодня X из N» или «Сегодня без тренировок» при N=0 (ТЗ п. 5, п. 6)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DawnSurfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (totalToday == 0) {
                        Text(
                            text = "Сегодня без тренировок",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DawnSecondaryAccent,
                            modifier = Modifier.testTag("zero_slots_banner")
                        )
                        Text(
                            text = "В активном плане на сегодня назначено 0 подходов (проверка переменного числа слотов N=0 без ошибки деления на ноль).",
                            style = MaterialTheme.typography.bodySmall,
                            color = DawnTextSecondary
                        )
                    } else {
                        Text(
                            text = "Сегодня $completedToday из $totalToday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (completedToday == totalToday) DawnSuccessAccent else DawnPrimaryAccent,
                            modifier = Modifier.testTag("daily_plan_progress_title")
                        )
                        Text(
                            text = "Завершённые слоты защищены уникальным индексом Room (повторный зачёт в ту же дату невозможен).",
                            style = MaterialTheme.typography.bodySmall,
                            color = DawnTextSecondary
                        )
                    }
                }
            }

            // Список слотов дневного плана
            if (dailySlots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dailySlots.forEach { item ->
                        val isSelected = item.slot.id == snapshot.protocol.id
                        val isCompleted = item.isCompleted
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = when {
                                        isCompleted -> DawnSuccessAccent.copy(alpha = 0.7f)
                                        isSelected -> DawnPrimaryAccent
                                        else -> DawnOutline.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !snapshot.isActiveOrPaused) {
                                    onSelectDailySlot(item)
                                }
                                .testTag("daily_slot_${item.slot.sortOrder}"),
                            color = DawnSurfaceVariant.copy(alpha = if (isSelected) 0.85f else 0.45f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Слот выполнен",
                                            tint = DawnSuccessAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                if (!snapshot.isActiveOrPaused) {
                                                    onSelectDailySlot(item)
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = DawnPrimaryAccent,
                                                unselectedColor = DawnTextSecondary
                                            ),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.slot.titleRu,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = DawnTextPrimary
                                        )
                                        val slotActiveSec =
                                            ((item.slot.contractionMs + item.slot.relaxationMs) * item.slot.repetitions) / 1000L
                                        Text(
                                            text = if (isCompleted) {
                                                "Зачтено в Room · Активное время: $slotActiveSec с"
                                            } else {
                                                "Ожидает выполнения · Контрольные точки каждые 5 с"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCompleted) DawnSuccessAccent else DawnTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Переключатель тестового режима N=0 («Сегодня без тренировок»)
            SettingSwitchRow(
                label = "Проверить режим N=0 («Сегодня без тренировок»)",
                checked = isZeroSlotDemoMode,
                onCheckedChange = onZeroSlotDemoModeChange,
                tag = "switch_zero_slot_mode"
            )

            Text(
                text = "Разделение записей в Room: Демо-зачётов = $demoCompletionsCount · Реальных зачётов = $realCompletionsCount (признак isDemo)",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = DawnTextSecondary,
                modifier = Modifier.testTag("room_demo_vs_real_counter")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignalSettingsAndSamplesCard(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    onSoundPackChange: (SoundPack) -> Unit,
    onMetronomeTickStyleChange: (MetronomeTickStyle) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onSignalVolumeChange: (Float) -> Unit,
    onMetronomeVolumeChange: (Float) -> Unit,
    onPlaySample: (SignalType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signal_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = DawnSecondaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Звуковые наборы, метроном и образцы (DataStore)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DawnTextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
            }

            // Выбор одного из 3 локальных звуковых наборов: «Мягкий», «Колокольчики», «Чёткий»
            Text(
                text = "Звуковой набор команд фаз:",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SoundPack.entries.forEach { pack ->
                    val selected = settings.soundPack == pack
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) DawnPrimaryAccent else DawnOutline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSoundPackChange(pack) }
                            .testTag("sound_pack_${pack.id}"),
                        color = DawnSurfaceVariant.copy(alpha = if (selected) 0.85f else 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSoundPackChange(pack) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DawnPrimaryAccent,
                                    unselectedColor = DawnTextSecondary
                                )
                            )
                            Column {
                                Text(
                                    text = pack.titleRu,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = DawnTextPrimary
                                )
                                Text(
                                    text = pack.descriptionRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DawnTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Пояснение: смена звукового набора применяется со следующего подхода
            if (snapshot.isActiveOrPaused && settings.soundPack != snapshot.soundPackSnapshot) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sound_pack_next_run_notice"),
                    shape = RoundedCornerShape(10.dp),
                    color = DawnWarningAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "В текущем подходе остаётся набор «${snapshot.soundPackSnapshot.titleRu}». Выбранный набор «${settings.soundPack.titleRu}» применится со следующего подхода.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnWarningAccent,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Text(
                    text = "Выбор сохраняется в DataStore. Изменение набора во время начатой попытки применяется со следующего подхода.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DawnTextSecondary,
                    modifier = Modifier.testTag("sound_pack_rule_caption")
                )
            }

            // Выбор варианта тика метронома: «Мягкий» или «Чёткий»
            Text(
                text = "Вариант тика метронома внутри фазы:",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetronomeTickStyle.entries.forEach { style ->
                    val selected = settings.metronomeTickStyle == style
                    OutlinedButton(
                        onClick = { onMetronomeTickStyleChange(style) },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) DawnSecondaryAccent else DawnOutline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("metronome_style_${style.id}")
                    ) {
                        Text(
                            text = "Тик: ${style.titleRu}",
                            color = if (selected) DawnSecondaryAccent else DawnTextPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            SettingSwitchRow(
                label = "Звуковые сигналы фаз",
                checked = settings.soundEnabled,
                onCheckedChange = onSoundEnabledChange,
                tag = "switch_sound_enabled"
            )
            SettingSwitchRow(
                label = "Вибрация (тактильные сигналы)",
                checked = settings.vibrationEnabled,
                onCheckedChange = onVibrationEnabledChange,
                tag = "switch_vibration_enabled"
            )
            SettingSwitchRow(
                label = "Тихий метроном внутри фазы",
                checked = settings.metronomeEnabled,
                onCheckedChange = onMetronomeEnabledChange,
                tag = "switch_metronome_enabled"
            )

            // Отдельные уровни громкости внутри приложения (не меняют системную громкость телефона, ТЗ п. 8)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Уровень громкости сигналов (внутри приложения): ${(settings.signalVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnTextSecondary
                )
                Slider(
                    value = settings.signalVolume,
                    onValueChange = onSignalVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = DawnPrimaryAccent,
                        activeTrackColor = DawnPrimaryAccent
                    ),
                    modifier = Modifier.testTag("slider_signal_volume")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Уровень тихого метронома (внутри приложения): ${(settings.metronomeVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnTextSecondary
                )
                Slider(
                    value = settings.metronomeVolume,
                    onValueChange = onMetronomeVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = DawnSecondaryAccent,
                        activeTrackColor = DawnSecondaryAccent
                    ),
                    modifier = Modifier.testTag("slider_metronome_volume")
                )
            }

            Text(
                text = if (snapshot.isRunningOrPreparing) {
                    "Образцы временно заблокированы во время активной фазы, чтобы не мешать командам:"
                } else {
                    "Прослушать образцы выбранного набора «${settings.soundPack.titleRu}»:"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (snapshot.isRunningOrPreparing) DawnWarningAccent else DawnTextPrimary
            )

            val samplesEnabled = !snapshot.isRunningOrPreparing
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleSignalChip(
                    label = "Сокращение",
                    tag = "sample_contraction",
                    enabled = samplesEnabled,
                    onClick = { onPlaySample(SignalType.CONTRACTION) }
                )
                SampleSignalChip(
                    label = "Расслабление",
                    tag = "sample_relaxation",
                    enabled = samplesEnabled,
                    onClick = { onPlaySample(SignalType.RELAXATION) }
                )
                SampleSignalChip(
                    label = "Подход завершён",
                    tag = "sample_set_completed",
                    enabled = samplesEnabled,
                    onClick = { onPlaySample(SignalType.SET_COMPLETED) }
                )
                SampleSignalChip(
                    label = "Группа завершена",
                    tag = "sample_group_completed",
                    enabled = samplesEnabled,
                    onClick = { onPlaySample(SignalType.GROUP_COMPLETED) }
                )
                SampleSignalChip(
                    label = "Тик метронома",
                    tag = "sample_metronome",
                    enabled = samplesEnabled,
                    onClick = { onPlaySample(SignalType.METRONOME_TICK) }
                )
            }
        }
    }
}

@Composable
private fun NotificationAndHardwareCard(
    hardwareStatus: AudioHardwareStatus,
    onRefreshHardware: () -> Unit
) {
    val context = LocalContext.current
    var hasNotifPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hardware_and_permissions_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = DawnPrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Уведомления и каналы устройства",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DawnTextPrimary,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Обновить состояние аудио и датчиков",
                    tint = DawnSecondaryAccent,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            hasNotifPermission = checkNotificationPermission(context)
                            onRefreshHardware()
                        }
                        .padding(12.dp)
                        .testTag("btn_refresh_hardware")
                )
            }

            Text(
                text = if (hasNotifPermission) {
                    "Разрешение POST_NOTIFICATIONS: Выдано (уведомление «Velaris · Сессия» с кнопкой «Пауза» отображается в шторке и на экране блокировки)"
                } else {
                    "Разрешение POST_NOTIFICATIONS: Не выдано. На Android 13–17 фоновая служба продолжает работать, но баннер с кнопкой «Пауза» в шторке скрыт системой."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (hasNotifPermission) DawnSuccessAccent else DawnWarningAccent,
                modifier = Modifier.testTag("notification_permission_status")
            )

            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FilledTonalButton(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_request_notification_permission")
                ) {
                    Text("Разрешить показ уведомления «Velaris · Сессия»")
                }
            }

            Text(
                text = "Звуковой профиль: ${hardwareStatus.ringerModeLabelRu} · " +
                    "Не беспокоить (DND): ${if (hardwareStatus.isDndActive) "Включён" else "Выкл"} · " +
                    "Вибромотор: ${if (hardwareStatus.hasHardwareVibrator) "Доступен" else "Отсутствует"}",
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSecondary
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun SampleSignalChip(
    label: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = Icons.Filled.Vibration,
            contentDescription = null,
            tint = if (enabled) DawnPrimaryAccent else DawnTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) DawnTextPrimary else DawnTextSecondary
        )
    }
}

@Composable
private fun EventLogCard(
    eventLog: List<BenchEventLogEntry>,
    onClearLog: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_log_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Технический лог событий (без личных данных)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DawnTextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )

                Row {
                    TextButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val formatted = eventLog.joinToString("\n") {
                                "[+${it.elapsedMs}ms][${it.category}] ${it.message}"
                            }
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText("Velaris S2 Log", formatted)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_copy_log")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Копировать лог",
                            tint = DawnSecondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Копировать", color = DawnSecondaryAccent)
                    }

                    TextButton(
                        onClick = onClearLog,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_clear_log")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Очистить лог",
                            tint = DawnTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DawnSurfaceVariant.copy(alpha = 0.7f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (eventLog.isEmpty()) {
                    Text(
                        text = "Лог пуст",
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnTextSecondary
                    )
                } else {
                    eventLog.take(25).forEach { entry ->
                        Text(
                            text = "[${entry.category}] ${entry.message}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = DawnTextPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun formatWholeSeconds(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val seconds = if (clamped == 0L) 0L else ((clamped + 999L) / 1000L)
    return String.format(Locale.US, "%d с", seconds)
}

@Composable
private fun MountainSilhouetteBackground(modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.mountain_illustration_desc)
    Canvas(
        modifier = modifier.semantics { contentDescription = desc }
    ) {
        val w = size.width
        val h = size.height

        val farRidge = Path().apply {
            moveTo(0f, h * 0.75f)
            lineTo(w * 0.22f, h * 0.35f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.74f, h * 0.25f)
            lineTo(w, h * 0.60f)
        }
        drawPath(
            path = farRidge,
            color = DawnPrimaryAccent.copy(alpha = 0.16f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val nearRidge = Path().apply {
            moveTo(0f, h * 0.88f)
            lineTo(w * 0.34f, h * 0.48f)
            lineTo(w * 0.61f, h * 0.72f)
            lineTo(w * 0.86f, h * 0.44f)
            lineTo(w, h * 0.78f)
        }
        drawPath(
            path = nearRidge,
            color = DawnSecondaryAccent.copy(alpha = 0.20f),
            style = Stroke(width = 1.8.dp.toPx())
        )

        drawLine(
            color = DawnOutline.copy(alpha = 0.25f),
            start = Offset(0f, h * 0.92f),
            end = Offset(w, h * 0.92f),
            strokeWidth = 1.dp.toPx()
        )
    }
}
