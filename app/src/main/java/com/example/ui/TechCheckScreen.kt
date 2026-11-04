package com.example.ui

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
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
import com.example.domain.PhaseType
import com.example.domain.ReaderFontSize
import com.example.domain.ReaderLineSpacing
import com.example.domain.ReaderMode
import com.example.domain.RunStatus
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.domain.WorkoutDifficulty
import com.example.ui.theme.LocalVelarisPalette
import com.example.ui.theme.VelarisThemeId
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun TechCheckScreen(
    buildInfo: AppBuildInfo,
    currentDestination: VelarisNavDestination,
    onNavigate: (VelarisNavDestination) -> Unit,
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    hardwareStatus: AudioHardwareStatus,
    audioDiagnostics: List<AudioDiagnosticEntry>,
    eventLog: List<BenchEventLogEntry>,
    isWakeLockHeld: Boolean,
    isServiceForeground: Boolean,
    dailySlots: List<DailySlotWithCompletion>,
    freeTemplates: List<com.example.data.FreeIntervalTemplateEntity> = emptyList(),
    isZeroSlotDemoMode: Boolean,
    demoCompletionsCount: Int,
    realCompletionsCount: Int,
    weeklyOverview: WeeklyOverviewState,
    storyState: StoryReaderUiState,
    lastCompletedSlotDifficulty: WorkoutDifficulty?,
    databaseErrorMessage: String?,
    availableProtocols: List<DemoProtocolConfig>,
    onSelectProtocol: (DemoProtocolConfig) -> Unit,
    onSelectDailySlot: (DailySlotWithCompletion) -> Unit,
    onStartSlotAndOpenWorkout: (DailySlotWithCompletion) -> Unit,
    onStartFreeWorkout: (com.example.data.FreeIntervalTemplateEntity) -> Unit = {},
    onSaveFreeTemplate: (com.example.data.FreeIntervalTemplateEntity) -> Unit = {},
    onZeroSlotDemoModeChange: (Boolean) -> Unit,
    onStartDemo: () -> Unit,
    onPauseDemo: () -> Unit,
    onResumeDemo: () -> Unit,
    onRequestStopDemo: () -> Unit,
    onCancelStopDialog: () -> Unit,
    onConfirmStopDemo: () -> Unit,
    onResetDemo: () -> Unit,
    onRateDifficulty: (WorkoutDifficulty?) -> Unit,
    onSelectWeeklyDate: (String) -> Unit,
    onImportMarkdownUri: (Uri) -> Unit,
    onUpdatePreviewHeadingLevels: (Int, Int) -> Unit,
    onConfirmPendingImport: () -> Unit,
    onCancelPendingImport: () -> Unit,
    onImportNeutralTestMarkdown: () -> Unit,
    onNextStoryStep: () -> Unit,
    onPreviousStoryStep: () -> Unit,
    onNextStoryChapter: () -> Unit,
    onPreviousStoryChapter: () -> Unit,
    onSelectStoryChapter: (String) -> Unit,
    onSaveStoryBlockPosition: (String) -> Unit,
    onRestartStoryFromBeginning: () -> Unit,
    onRemoveImportedBook: () -> Unit,
    pendingRestorePreview: BackupPreviewInfo? = null,
    backupOperationBanner: String? = null,
    backupErrorMessage: String? = null,
    onCreateBackupUri: (Uri) -> Unit = {},
    onInspectBackupUri: (Uri) -> Unit = {},
    onConfirmRestore: () -> Unit = {},
    onCancelRestore: () -> Unit = {},
    onDismissBackupBanners: () -> Unit = {},
    onPlaySample: (SignalType) -> Unit,
    onThemeChange: (VelarisThemeId) -> Unit,
    onSoundPackChange: (SoundPack) -> Unit,
    onMetronomeTickStyleChange: (MetronomeTickStyle) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onSignalVolumeChange: (Float) -> Unit,
    onMetronomeVolumeChange: (Float) -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit,
    onReaderFontSizeChange: (ReaderFontSize) -> Unit,
    onReaderLineSpacingChange: (ReaderLineSpacing) -> Unit,
    onRealProgramEnabledChange: (Boolean) -> Unit = {},
    onRefreshHardware: () -> Unit,
    onClearAudioDiagnostics: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(palette.backgroundTop, palette.backgroundMid, palette.backgroundBottom)
    )

    // Обработка кнопки «Назад» без остановки таймера
    BackHandler(enabled = currentDestination != VelarisNavDestination.HOME) {
        if (currentDestination == VelarisNavDestination.DIAGNOSTICS) {
            onNavigate(VelarisNavDestination.SETTINGS)
        } else {
            onNavigate(VelarisNavDestination.HOME)
        }
    }

    // Диалог остановки: «Завершить без зачёта?» -> «Завершить» / «Остаться на паузе» (ТЗ п. 7)
    if (snapshot.isStopDialogVisible) {
        AlertDialog(
            onDismissRequest = onCancelStopDialog,
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
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
                        containerColor = palette.errorAccent,
                        contentColor = palette.onPrimaryAccent
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
                        color = palette.textPrimary
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = palette.backgroundTop,
        bottomBar = {
            NavigationBar(
                containerColor = palette.surface,
                contentColor = palette.textPrimary
            ) {
                val mainTabs = listOf(
                    Triple(VelarisNavDestination.HOME, Icons.Default.Home, "Главная"),
                    Triple(VelarisNavDestination.PLAN, Icons.Default.CalendarMonth, "План"),
                    Triple(VelarisNavDestination.WORKOUT, Icons.Default.Timer, "Подход"),
                    Triple(VelarisNavDestination.READER, Icons.Default.AutoStories, "Чтение"),
                    Triple(VelarisNavDestination.SETTINGS, Icons.Default.Settings, "Настройки")
                )
                mainTabs.forEach { (dest, icon, label) ->
                    val selected = currentDestination == dest ||
                        (currentDestination == VelarisNavDestination.DIAGNOSTICS && dest == VelarisNavDestination.SETTINGS)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(dest) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.onPrimaryAccent,
                            selectedTextColor = palette.primaryAccent,
                            indicatorColor = palette.primaryAccent,
                            unselectedIconColor = palette.textSecondary,
                            unselectedTextColor = palette.textSecondary
                        ),
                        modifier = Modifier.testTag("nav_${dest.id}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentDestination) {
                        VelarisNavDestination.HOME -> {
                            VelarisHomeScreen(
                                buildInfo = buildInfo,
                                snapshot = snapshot,
                                settings = settings,
                                dailySlots = dailySlots,
                                isZeroSlotDemoMode = isZeroSlotDemoMode,
                                weeklyOverview = weeklyOverview,
                                storyState = storyState,
                                onStartOrOpenWorkout = {
                                    onNavigate(VelarisNavDestination.WORKOUT)
                                    if (!snapshot.isActiveOrPaused) {
                                        onStartDemo()
                                    }
                                },
                                onOpenPlan = { onNavigate(VelarisNavDestination.PLAN) },
                                onOpenReader = { onNavigate(VelarisNavDestination.READER) },
                                onOpenSettings = { onNavigate(VelarisNavDestination.SETTINGS) },
                                onSelectWeeklyDate = onSelectWeeklyDate
                            )
                        }
                        VelarisNavDestination.PLAN -> {
                            VelarisPlanScreen(
                                snapshot = snapshot,
                                settings = settings,
                                dailySlots = dailySlots,
                                freeTemplates = freeTemplates,
                                isZeroSlotDemoMode = isZeroSlotDemoMode,
                                demoCompletionsCount = demoCompletionsCount,
                                realCompletionsCount = realCompletionsCount,
                                databaseErrorMessage = databaseErrorMessage,
                                onSelectDailySlot = onSelectDailySlot,
                                onStartSelectedSlot = onStartSlotAndOpenWorkout,
                                onStartFreeWorkout = onStartFreeWorkout,
                                onSaveFreeTemplate = onSaveFreeTemplate,
                                onReturnToActiveWorkout = { onNavigate(VelarisNavDestination.WORKOUT) }
                            )
                        }
                        VelarisNavDestination.WORKOUT -> {
                            VelarisActiveWorkoutScreen(
                                snapshot = snapshot,
                                settings = settings,
                                hardwareStatus = hardwareStatus,
                                isZeroSlotDemoMode = isZeroSlotDemoMode,
                                lastCompletedSlotDifficulty = lastCompletedSlotDifficulty,
                                onStartDemo = onStartDemo,
                                onPauseDemo = onPauseDemo,
                                onResumeDemo = onResumeDemo,
                                onRequestStopDemo = onRequestStopDemo,
                                onResetDemo = onResetDemo,
                                onRateDifficulty = onRateDifficulty,
                                onOpenPlan = { onNavigate(VelarisNavDestination.PLAN) },
                                onOpenReader = { onNavigate(VelarisNavDestination.READER) }
                            )
                        }
                        VelarisNavDestination.READER -> {
                            VelarisReaderScreen(
                                storyState = storyState,
                                settings = settings,
                                snapshot = snapshot,
                                onImportMarkdownUri = onImportMarkdownUri,
                                onUpdatePreviewHeadingLevels = onUpdatePreviewHeadingLevels,
                                onConfirmPendingImport = onConfirmPendingImport,
                                onCancelPendingImport = onCancelPendingImport,
                                onReaderModeChange = onReaderModeChange,
                                onReaderFontSizeChange = onReaderFontSizeChange,
                                onReaderLineSpacingChange = onReaderLineSpacingChange,
                                onNextStep = onNextStoryStep,
                                onPreviousStep = onPreviousStoryStep,
                                onNextChapter = onNextStoryChapter,
                                onPreviousChapter = onPreviousStoryChapter,
                                onSelectChapter = onSelectStoryChapter,
                                onSaveBlockPosition = onSaveStoryBlockPosition,
                                onRestartFromBeginning = onRestartStoryFromBeginning,
                                onReturnToWorkout = { onNavigate(VelarisNavDestination.WORKOUT) }
                            )
                        }
                        VelarisNavDestination.SETTINGS -> {
                            VelarisSettingsScreen(
                                settings = settings,
                                snapshot = snapshot,
                                storyState = storyState,
                                availableProtocols = availableProtocols,
                                onSelectProtocol = onSelectProtocol,
                                onThemeChange = onThemeChange,
                                onSoundPackChange = onSoundPackChange,
                                onMetronomeTickStyleChange = onMetronomeTickStyleChange,
                                onSoundEnabledChange = onSoundEnabledChange,
                                onVibrationEnabledChange = onVibrationEnabledChange,
                                onMetronomeEnabledChange = onMetronomeEnabledChange,
                                onSignalVolumeChange = onSignalVolumeChange,
                                onMetronomeVolumeChange = onMetronomeVolumeChange,
                                onPlaySample = onPlaySample,
                                onReaderModeChange = onReaderModeChange,
                                onReaderFontSizeChange = onReaderFontSizeChange,
                                onReaderLineSpacingChange = onReaderLineSpacingChange,
                                onImportMarkdownUri = onImportMarkdownUri,
                                onRemoveImportedBook = onRemoveImportedBook,
                                pendingRestorePreview = pendingRestorePreview,
                                backupOperationBanner = backupOperationBanner,
                                backupErrorMessage = backupErrorMessage,
                                onCreateBackupUri = onCreateBackupUri,
                                onInspectBackupUri = onInspectBackupUri,
                                onConfirmRestore = onConfirmRestore,
                                onCancelRestore = onCancelRestore,
                                onDismissBackupBanners = onDismissBackupBanners,
                                onOpenDiagnostics = { onNavigate(VelarisNavDestination.DIAGNOSTICS) },
                                onReturnToWorkout = { onNavigate(VelarisNavDestination.WORKOUT) }
                            )
                        }
                        VelarisNavDestination.DIAGNOSTICS -> {
                            VelarisDiagnosticsScreen(
                                buildInfo = buildInfo,
                                snapshot = snapshot,
                                hardwareStatus = hardwareStatus,
                                audioDiagnostics = audioDiagnostics,
                                eventLog = eventLog,
                                isWakeLockHeld = isWakeLockHeld,
                                isServiceForeground = isServiceForeground,
                                isZeroSlotDemoMode = isZeroSlotDemoMode,
                                demoCompletionsCount = demoCompletionsCount,
                                realCompletionsCount = realCompletionsCount,
                                onZeroSlotDemoModeChange = onZeroSlotDemoModeChange,
                                onImportNeutralTestMarkdown = onImportNeutralTestMarkdown,
                                onRefreshHardware = onRefreshHardware,
                                onClearAudioDiagnostics = onClearAudioDiagnostics,
                                onClearEventLog = onClearLog,
                                onBackToSettings = { onNavigate(VelarisNavDestination.SETTINGS) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Экран 3: Активный подход и экран завершения (Update v1.1 п. 2).
 * - Крупное плавное кольцо постоянного размера (`268.dp`), целые секунды без десятых долей
 *   и без «прыжков» ширины текста (`FontFamily.Monospace` + `tnum`).
 * - Текущая фаза «Сокращение» или «Расслабление», счётчик повторов и оставшееся активное время.
 * - Крупные кнопки «Пауза» и «Остановить», небольшой индикатор аудиомаршрута.
 * - После завершения: спокойная отметка, необязательная оценка ощущения, кнопки «К плану» и «Почитать».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VelarisActiveWorkoutScreen(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    hardwareStatus: AudioHardwareStatus,
    isZeroSlotDemoMode: Boolean,
    lastCompletedSlotDifficulty: WorkoutDifficulty?,
    onStartDemo: () -> Unit,
    onPauseDemo: () -> Unit,
    onResumeDemo: () -> Unit,
    onRequestStopDemo: () -> Unit,
    onResetDemo: () -> Unit,
    onRateDifficulty: (WorkoutDifficulty?) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val isDemoMode = !settings.isRealProgramEnabled || isZeroSlotDemoMode

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        onStartDemo()
    }

    var renderNowElapsedMs by remember(snapshot.snapshotElapsedRealtimeMs, snapshot.status) {
        mutableLongStateOf(snapshot.snapshotElapsedRealtimeMs)
    }

    val animationsEnabled = remember(isResumed) {
        try {
            ValueAnimator.areAnimatorsEnabled()
        } catch (_: Throwable) {
            true
        }
    }

    LaunchedEffect(
        isResumed,
        snapshot.isRunningOrPreparing,
        snapshot.snapshotElapsedRealtimeMs,
        animationsEnabled
    ) {
        renderNowElapsedMs = SystemClock.elapsedRealtime()
        if (isResumed && snapshot.isRunningOrPreparing && animationsEnabled) {
            while (isActive) {
                withInfiniteAnimationFrameMillis {
                    renderNowElapsedMs = SystemClock.elapsedRealtime()
                }
            }
        }
    }

    val visualProgress = snapshot.computeVisualRingProgress(renderNowElapsedMs)
    val visualRemainingMs = snapshot.computeVisualRemainingMs(renderNowElapsedMs)
    val displaySeconds = ((visualRemainingMs + 999L) / 1000L).coerceAtLeast(0L)

    val activeElapsedVisualMs = when (snapshot.status) {
        RunStatus.RUNNING -> {
            val delta = (renderNowElapsedMs - snapshot.snapshotElapsedRealtimeMs).coerceIn(0L, 2_500L)
            (snapshot.confirmedActiveMs + delta).coerceAtMost(snapshot.totalActiveDurationMs)
        }
        else -> snapshot.confirmedActiveMs
    }
    val activeRemainingTotalSec =
        ((snapshot.totalActiveDurationMs - activeElapsedVisualMs + 999L) / 1000L).coerceAtLeast(0L)

    val targetRingColor = when (snapshot.status) {
        RunStatus.PREPARING -> palette.warningAccent
        RunStatus.RUNNING -> if (snapshot.currentPhase == PhaseType.CONTRACTION) {
            palette.primaryAccent
        } else {
            palette.secondaryAccent
        }
        RunStatus.PAUSED, RunStatus.INTERRUPTED -> palette.warningAccent
        RunStatus.COMPLETED -> palette.successAccent
        RunStatus.STOPPED, RunStatus.IDLE -> palette.outline
    }
    val ringColor by animateColorAsState(
        targetValue = targetRingColor,
        animationSpec = tween(durationMillis = if (animationsEnabled) 260 else 0),
        label = "WorkoutRingColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_workout_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isDemoMode) {
            DemoSafetyBanner()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timer_display_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Верхняя строка: статус + небольшой индикатор аудиомаршрута
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceVariant
                    ) {
                        Text(
                            text = snapshot.status.labelRu,
                            style = MaterialTheme.typography.labelLarge,
                            color = ringColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("run_status_badge")
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceVariant,
                        modifier = Modifier.testTag("workout_audio_route_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hardwareStatus.isHeadphonesConnected) {
                                    Icons.Default.Headphones
                                } else {
                                    Icons.Default.VolumeUp
                                },
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hardwareStatus.isHeadphonesConnected) "Наушники" else "Динамик",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = snapshot.protocol.titleRu,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Крупное плавное кольцо постоянного размера (268.dp)
                val phaseTitle = when (snapshot.status) {
                    RunStatus.IDLE -> "Готов к старту"
                    RunStatus.PREPARING -> "Подготовка"
                    RunStatus.RUNNING, RunStatus.PAUSED, RunStatus.INTERRUPTED -> snapshot.currentPhase.labelRu
                    RunStatus.COMPLETED -> "Подход завершён"
                    RunStatus.STOPPED -> "Остановлено без зачёта"
                }

                Box(
                    modifier = Modifier
                        .size(268.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Фаза: $phaseTitle, осталось $displaySeconds секунд"
                        }
                        .testTag("smooth_timer_ring"),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = palette.surfaceVariant
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 15.dp.toPx()
                        val glowStrokePx = 24.dp.toPx()
                        val inset = glowStrokePx / 2f + 4.dp.toPx()
                        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                        val topLeft = Offset(inset, inset)

                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        val sweep = (visualProgress.coerceIn(0f, 1f) * 360f)
                        if (sweep > 0.5f) {
                            drawArc(
                                color = ringColor.copy(alpha = 0.20f),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = phaseTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = ringColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("phase_title_text")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = String.format(Locale.US, "%02d с", displaySeconds),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontFeatureSettings = "tnum"
                            ),
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("countdown_seconds_text")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Повтор ${snapshot.currentRepetition} / ${snapshot.totalRepetitions}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontFeatureSettings = "tnum"
                            ),
                            color = palette.textSecondary,
                            modifier = Modifier.testTag("repetition_counter_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Осталось активного времени: $activeRemainingTotalSec с · Выполнено: ${snapshot.confirmedActiveMs / 1000} / ${snapshot.totalActiveDurationMs / 1000} с",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum"
                    ),
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("confirmed_active_time_text")
                )

                if (snapshot.interruptionReason != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.warningAccent.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interruption_reason_banner")
                    ) {
                        Text(
                            text = snapshot.interruptionReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.warningAccent,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Крупные кнопки управления сессией
                when (snapshot.status) {
                    RunStatus.IDLE, RunStatus.STOPPED -> {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onStartDemo()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = palette.onPrimaryAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("btn_start_demo")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Начать подход",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    RunStatus.PREPARING, RunStatus.RUNNING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPauseDemo,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.warningAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 54.dp)
                                    .testTag("btn_pause_demo")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Пауза", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onRequestStopDemo,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 54.dp)
                                    .testTag("btn_stop_demo")
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = palette.errorAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Остановить", color = palette.errorAccent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    RunStatus.PAUSED, RunStatus.INTERRUPTED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onResumeDemo,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.successAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 54.dp)
                                    .testTag("btn_resume_demo")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Продолжить", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onRequestStopDemo,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 54.dp)
                                    .testTag("btn_stop_demo")
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = palette.errorAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Остановить", color = palette.errorAccent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    RunStatus.COMPLETED -> {
                        // Состояние завершения (Update v1.1 п. 2):
                        // спокойная отметка, необязательная оценка ощущения, кнопки «К плану» и «Почитать»
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.successAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                                .testTag("workout_completion_card"),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.successAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Подход спокойно зачтён",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.successAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Необязательная оценка ощущения:",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WorkoutDifficulty.entries.forEach { diff ->
                                    val selected = lastCompletedSlotDifficulty == diff
                                    FilledTonalButton(
                                        onClick = {
                                            onRateDifficulty(if (selected) null else diff)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (selected) palette.primaryAccent else palette.surfaceVariant,
                                            contentColor = if (selected) palette.onPrimaryAccent else palette.textPrimary
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_rate_${diff.code.lowercase(Locale.ROOT)}")
                                    ) {
                                        Text(
                                            text = diff.labelRu,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenPlan,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_completion_to_plan")
                                ) {
                                    Text("К плану", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onOpenReader,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_completion_to_reader")
                                ) {
                                    Text("Почитать", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = onResetDemo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_reset_demo")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("К следующему свободному подходу", color = palette.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
