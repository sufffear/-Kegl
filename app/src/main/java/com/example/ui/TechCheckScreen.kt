package com.example.ui

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AudioHardwareStatus
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.RunStatus
import com.example.domain.WorkoutDifficulty
import com.example.ui.theme.LocalVelarisPalette
import java.util.Locale

private val MAIN_NAV_DESTINATIONS = listOf(
    VelarisNavDestination.HOME,
    VelarisNavDestination.PLAN,
    VelarisNavDestination.CHANTAL,
    VelarisNavDestination.STATISTICS
)

private fun resolveRingColor(status: RunStatus, currentPhase: PhaseType, palette: com.example.ui.theme.VelarisPalette): Color = when (status) {
    RunStatus.IDLE -> palette.outline
    RunStatus.PREPARING -> palette.phasePrepAccent
    RunStatus.RUNNING -> if (currentPhase == PhaseType.CONTRACTION) {
        palette.phaseHoldAccent
    } else {
        palette.phaseRelaxAccent
    }
    RunStatus.PAUSED,
    RunStatus.INTERRUPTED -> palette.phasePauseAccent
    RunStatus.COMPLETED -> palette.successAccent
    RunStatus.STOPPED -> palette.errorAccent
}

/**
 * Изолированное кольцо таймера active workout.
 * Обновление frameNowMs через withFrameMillis происходит только внутри этого компонента,
 * предотвращая рекомпозицию верхнеуровневых экранов и карточек каждый кадр (60/120 FPS).
 */
@Composable
fun ActiveWorkoutTimerRing(
    snapshot: PhaseEngineSnapshot,
    palette: com.example.ui.theme.VelarisPalette,
    modifier: Modifier = Modifier
) {
    val animatorsEnabled = remember {
        runCatching { (android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()) }.getOrDefault(true) &&
            !android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }

    var frameNowMs by remember(snapshot.runId, snapshot.status, snapshot.snapshotElapsedRealtimeMs) {
        mutableLongStateOf(snapshot.snapshotElapsedRealtimeMs)
    }

    LaunchedEffect(snapshot.isRunningOrPreparing, snapshot.snapshotElapsedRealtimeMs, animatorsEnabled) {
        if (snapshot.isRunningOrPreparing && animatorsEnabled) {
            val baseEngineMs = snapshot.snapshotElapsedRealtimeMs
            val baseLocalMs = SystemClock.elapsedRealtime()
            while (true) {
                withFrameMillis {
                    val elapsedSinceSnapshot = (SystemClock.elapsedRealtime() - baseLocalMs).coerceAtLeast(0L)
                    frameNowMs = baseEngineMs + elapsedSinceSnapshot
                }
            }
        } else {
            frameNowMs = snapshot.snapshotElapsedRealtimeMs
        }
    }

    val ringProgress = snapshot.computeVisualRingProgress(frameNowMs)
    val displayWholeSeconds = snapshot.computeDisplayWholeSeconds(frameNowMs)
    val ringColor = resolveRingColor(snapshot.status, snapshot.currentPhase, palette)

    Box(
        modifier = modifier
            .size(240.dp)
            .padding(6.dp)
            .testTag("smooth_timer_ring"),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = palette.surfaceElevated
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 15.dp.toPx()
            val glowStrokePx = 24.dp.toPx()
            val diameter = size.minDimension - glowStrokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val sweep = 360f * ringProgress.coerceIn(0f, 1f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Мягкое внешнее свечение активной дуги
            if (sweep > 0.5f) {
                drawArc(
                    color = ringColor.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
                )
            }

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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format(Locale.US, "%d", displayWholeSeconds),
                style = MaterialTheme.typography.displayLarge.merge(
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontFeatureSettings = "tnum"
                    )
                ),
                color = palette.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("timer_whole_seconds")
            )
            Text(
                text = "секунд",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (snapshot.isCustomProgram && snapshot.currentBlockName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.primaryAccent.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = snapshot.currentBlockName,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceElevated
            ) {
                Text(
                    text = "Цикл ${snapshot.currentRepetition} из ${snapshot.totalRepetitions}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = palette.secondaryAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Осталось всего: ${formatDurationCompact(snapshot.remainingActiveMs)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )
        }
    }
}

/**
 * Корневой экран приложения Velaris (Итерация v1.4, версия 0.7.0-v1.4, versionCode 7).
 * Реализует:
 * - 4 основных раздела в нижней навигации: Главная, Тренировки, Книга, Настройки;
 * - отдельный экран активной тренировки (WORKOUT) и компактную плашку текущей тренировки над нижней панелью;
 * - отсутствие технического шума на пользовательских экранах (вся диагностика в «Настройки → Диагностика»);
 * - спокойную однотонную поверхность чтения без горного силуэта и ярких градиентов под текстом.
 */
@Composable
fun TechCheckScreen(
    viewModel: TechCheckViewModel,
    modifier: Modifier = Modifier
) {
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
    val trainingStateReady by viewModel.trainingStateReady.collectAsStateWithLifecycle()
    val trainingConfigurations by viewModel.trainingConfigurations.collectAsStateWithLifecycle()
    val extraTrainingCycle by viewModel.extraTrainingCycle.collectAsStateWithLifecycle()
    val configuredExtras = com.example.data.ExtraWorkoutPresets.all.map { template ->
        trainingConfigurations.firstOrNull { it.key == "extra:${template.slotIndex}" }?.applyTo(template) ?: template
    }
    val freeTemplates by viewModel.freeTemplates.collectAsStateWithLifecycle()
    val customPrograms by viewModel.customPrograms.collectAsStateWithLifecycle()
    val isZeroSlotDemoMode by viewModel.isZeroSlotDemoMode.collectAsStateWithLifecycle()
    val demoCompletionsCount by viewModel.demoCompletionsCount.collectAsStateWithLifecycle()
    val chantalReturnToken by viewModel.chantalReturnToken.collectAsStateWithLifecycle()
    val workoutEnergy by viewModel.workoutEnergy.collectAsStateWithLifecycle()
    val realCompletionsCount by viewModel.realCompletionsCount.collectAsStateWithLifecycle()
    val weeklyOverview by viewModel.weeklyOverview.collectAsStateWithLifecycle()
    val workoutJournal by viewModel.workoutJournal.collectAsStateWithLifecycle()
    val statisticsDate by viewModel.statisticsDate.collectAsStateWithLifecycle()
    val journalDate by viewModel.journalDate.collectAsStateWithLifecycle()
    val journalEntryId by viewModel.journalEntryId.collectAsStateWithLifecycle()
    LaunchedEffect(currentDestination, weeklyOverview.totalRealCompletionsAllTime, snapshot.runId, snapshot.status) {
        if (currentDestination in listOf(VelarisNavDestination.WORKOUT_JOURNAL, VelarisNavDestination.WORKOUT_SESSION)) viewModel.refreshWorkoutJournal()
    }
    val storyState by viewModel.storyState.collectAsStateWithLifecycle()
    val pendingRestorePreview by viewModel.pendingRestorePreview.collectAsStateWithLifecycle()
    val backupOperationBanner by viewModel.backupOperationBanner.collectAsStateWithLifecycle()
    val backupErrorMessage by viewModel.backupErrorMessage.collectAsStateWithLifecycle()
    val lastCompletedDifficulty by viewModel.lastCompletedSlotDifficulty.collectAsStateWithLifecycle()
    val celebratedMilestone by viewModel.celebratedMilestone.collectAsStateWithLifecycle()
    val databaseErrorMessage by viewModel.databaseErrorMessage.collectAsStateWithLifecycle()

    val palette = LocalVelarisPalette.current
    val compactBottomNav = LocalConfiguration.current.screenWidthDp < 480 && LocalDensity.current.fontScale > 1.3f
    val isTrainingScreen = currentDestination == VelarisNavDestination.PLAN
    var trainingDetailsId by rememberSaveable { mutableStateOf<String?>(null) }
    var constructorEntry by rememberSaveable { mutableIntStateOf(0) }
    val trainingContext = androidx.compose.ui.platform.LocalContext.current
    val trainingWindow = remember(trainingContext) {
        generateSequence(trainingContext) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()?.window
    }
    val trainingSystemBars = trainingWindow?.let { androidx.core.view.WindowCompat.getInsetsController(it,it.decorView) }
    androidx.compose.runtime.SideEffect { if(isTrainingScreen) trainingSystemBars?.isAppearanceLightStatusBars = false }
    androidx.compose.runtime.DisposableEffect(isTrainingScreen) {
        onDispose { if(isTrainingScreen) trainingSystemBars?.isAppearanceLightStatusBars = palette.surface.luminance() > .179f }
    }
    val routeState = rememberSaveableStateHolder()
    var chantalChildBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    var chantalPageName by rememberSaveable { mutableStateOf(ChantalPage.HOME.name) }
    val chantalScrollStates = ChantalPage.entries.associateWith { child ->
        androidx.compose.runtime.key("chantal_scroll_${child.name}") { rememberScrollState() }
    }
    fun openPage(destination: VelarisNavDestination) = viewModel.navigateTo(destination)
    fun backPage() = viewModel.navigateBack()
    fun openTrainingConstructor() {
        routeState.removeState("${VelarisNavDestination.OTHER_WORKOUTS.id}:$constructorEntry")
        constructorEntry++; openPage(VelarisNavDestination.OTHER_WORKOUTS)
    }
    fun openTrainingDetails(id: String) {
        routeState.removeState("${VelarisNavDestination.TRAINING_DETAILS.id}:$id")
        trainingDetailsId=id
        openPage(VelarisNavDestination.TRAINING_DETAILS)
    }
    // Native dialog Back handlers take precedence over route navigation.
    BackHandler(enabled = viewModel.canNavigateBack) { backPage() }

    // Единый диалог подтверждения остановки (открывается поверх любого экрана, если запрошена остановка)
    if (snapshot.isStopDialogVisible) {
        StopWorkoutConfirmDialog(
            onStayPaused = { viewModel.cancelStopDialog() },
            onConfirmStop = { viewModel.confirmStopDemo() }
        )
    }

    val isReaderScreen = currentDestination == VelarisNavDestination.READER
    val showCompactWorkoutBar = snapshot.isActiveOrPaused && currentDestination != VelarisNavDestination.WORKOUT

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = if(isTrainingScreen) WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal) else WindowInsets.safeDrawing,
        containerColor = if (isReaderScreen) palette.readerSurface else palette.backgroundTop,
        topBar = {
            if (!isReaderScreen && !isTrainingScreen) Column(Modifier.background(palette.surface).statusBarsPadding()) {
                VelarisMountainHeader(
                    subtitleRu = if (currentDestination == VelarisNavDestination.CHANTAL)
                        runCatching { ChantalPage.valueOf(chantalPageName).title }.getOrDefault(currentDestination.titleRu)
                        else currentDestination.titleRu,
                    isDemoActive = !settings.isRealProgramEnabled,
                    onBack = if (currentDestination == VelarisNavDestination.CHANTAL) chantalChildBack
                        else if (!currentDestination.isMainSection) ({ backPage() }) else null,
                    onOpenSettings = if (!currentDestination.id.startsWith("settings") &&
                        currentDestination != VelarisNavDestination.DIAGNOSTICS)
                        ({ openPage(VelarisNavDestination.SETTINGS) }) else null
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.outline.copy(alpha = 0.45f))
                )
                if (showCompactWorkoutBar) {
                    CompactActiveWorkoutBar(
                        snapshot = snapshot,
                        onPauseWorkout = { viewModel.pauseDemo() },
                        onResumeWorkout = { viewModel.resumeDemo() },
                        onReturnToWorkout = { openPage(VelarisNavDestination.WORKOUT) }
                    )
                }

                NavigationBar(
                    containerColor = palette.surface,
                    contentColor = palette.textPrimary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fantasyFrame(palette.outline).testTag("velaris_bottom_nav")
                ) {
                    MAIN_NAV_DESTINATIONS.forEach { dest ->
                        val selected = viewModel.mainDestination == dest
                        NavigationBarItem(
                            selected = selected,
                            onClick = { openPage(dest) },
                            icon = {
                                Icon(
                                    imageVector = when (dest) {
                                        VelarisNavDestination.HOME -> Icons.Default.Home
                                        VelarisNavDestination.PLAN -> Icons.Default.CalendarMonth
                                        VelarisNavDestination.CHANTAL -> Icons.Default.Pets
                                        VelarisNavDestination.STATISTICS -> Icons.Default.BarChart
                                        else -> Icons.Default.Home
                                    },
                                    contentDescription = null
                                )
                            },
                            label = if (compactBottomNav) null else { {
                                Text(
                                    text = dest.titleRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.onPrimaryAccent,
                                selectedTextColor = palette.primaryAccent,
                                indicatorColor = palette.primaryAccent,
                                unselectedIconColor = palette.textSecondary,
                                unselectedTextColor = palette.textSecondary
                            ),
                            modifier = Modifier.testTag("nav_${dest.id}").semantics { contentDescription = dest.titleRu }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val bgModifier = if (isReaderScreen) {
            Modifier.background(palette.readerSurface)
        } else {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.backgroundTop,
                        palette.backgroundMid,
                        palette.backgroundBottom
                    )
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if(isTrainingScreen || currentDestination == VelarisNavDestination.TRAINING_DETAILS) Modifier.background(com.example.ui.theme.ReferencePlum.Background) else bgModifier)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Dispose the outgoing page before measuring the next one. Simultaneous
            // Crossfade pages can remeasure a scrolled viewport while its state is saved.
            val destination = currentDestination
            routeState.SaveableStateProvider(when(destination) {
                VelarisNavDestination.OTHER_WORKOUTS -> "${destination.id}:$constructorEntry"
                VelarisNavDestination.TRAINING_DETAILS -> "${destination.id}:$trainingDetailsId"
                else -> destination.id
            }) {
                    val defaultScrollState = rememberScrollState()
                    val scrollState = if (destination == VelarisNavDestination.CHANTAL)
                        chantalScrollStates[runCatching { ChantalPage.valueOf(chantalPageName) }.getOrDefault(ChantalPage.HOME)] ?: defaultScrollState
                        else defaultScrollState
                    VelarisScreen(scrollState, Modifier.testTag("route_${destination.id}"),
                        horizontalPadding = if (destination in listOf(VelarisNavDestination.STATISTICS,VelarisNavDestination.PLAN)) 0.dp else VelarisSpacing.Content,
                        verticalPadding = if(destination == VelarisNavDestination.PLAN) 0.dp else VelarisSpacing.Content,
                        maxContentWidth = if(destination == VelarisNavDestination.PLAN) androidx.compose.ui.unit.Dp.Infinity else VelarisSpacing.MaxContentWidth) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (databaseErrorMessage != null) {
                                DatabaseWarningBanner(message = databaseErrorMessage!!)
                            }

                            when (destination) {
                                VelarisNavDestination.HOME,
                                VelarisNavDestination.READER -> {
                                    VelarisHomeScreen(
                                        snapshot = snapshot,
                                        dailySlots = dailySlots,
                                        weeklyOverview = weeklyOverview,
                                        onOpenWorkout = { openPage(VelarisNavDestination.WORKOUT) },
                                        onStartNextSlot = { viewModel.startSlotAndOpenWorkout(it) },
                                        onOpenPlan = { openPage(VelarisNavDestination.PLAN) },
                                        onPause = { viewModel.pauseDemo() },
                                        onResume = { viewModel.resumeDemo() },
                                        dataError = databaseErrorMessage,
                                        onRetry = { viewModel.refreshHardwareStatus() }
                                    )
                                }

                                VelarisNavDestination.STATISTICS -> {
                                    VelarisStatisticsScreen(weeklyOverview,
                                        onDay = { viewModel.openStatisticsDay(it) },
                                        onJournal = { viewModel.openWorkoutJournal() },
                                        onDetails = { openPage(VelarisNavDestination.STATISTICS_DETAILS) })
                                }
                                VelarisNavDestination.STATISTICS_DAY -> {
                                    val date = statisticsDate ?: weeklyOverview.selectedDate
                                    VelarisStatisticsDay(weeklyOverview, date, { viewModel.openWorkoutJournal(date) })
                                }
                                VelarisNavDestination.WORKOUT_JOURNAL -> {
                                    VelarisWorkoutJournal(workoutJournal, journalDate, { viewModel.openWorkoutJournal() },
                                        { viewModel.openWorkoutEntry(it) }, { viewModel.refreshWorkoutJournal() })
                                }
                                VelarisNavDestination.WORKOUT_SESSION -> {
                                    VelarisWorkoutSession(workoutJournal, journalEntryId, weeklyOverview, { viewModel.refreshWorkoutJournal() })
                                }
                                VelarisNavDestination.STATISTICS_DETAILS -> {
                                    WeeklyOverviewCard(
                                        overview = weeklyOverview,
                                        isWorkoutActiveOrPaused = snapshot.isActiveOrPaused,
                                        onSelectDay = { viewModel.selectWeeklyOverviewDate(it) },
                                        onToggleFreezeDate = { viewModel.toggleFreezeDate(it) },
                                        onStartStreakRescueBlitz = { viewModel.startStreakRescueBlitzAndOpenWorkout() },
                                        celebratedMilestone = celebratedMilestone,
                                        onTriggerMilestoneSalute = { viewModel.triggerMilestoneSalute(it) },
                                        onDismissMilestoneSalute = { viewModel.dismissMilestoneSalute() }
                                    )
                                }

                                VelarisNavDestination.PLAN -> {
                                    VelarisTrainingOverview(snapshot, dailySlots,
                                        onStartSlot = { viewModel.startSlotAndOpenWorkout(it) },
                                        onOpenDetails = { openTrainingDetails(it.slot.id) },
                                        onOpenConstructor = { openTrainingConstructor() },
                                        onSaveProgram = { viewModel.saveCustomProgram(it) },
                                        onOpenSettings = { openPage(VelarisNavDestination.SETTINGS) },
                                        isDemo = !settings.isRealProgramEnabled,
                                        extraTemplates=configuredExtras,extraCycle=extraTrainingCycle,extrasReady=trainingStateReady,customPrograms=customPrograms,
                                        onOpenExtra={openTrainingDetails("extra:${it.slotIndex}")},
                                        onStartExtra={viewModel.startFreeWorkoutAndOpenWorkout(it)},
                                        onOpenProgram={openTrainingDetails("program:${it.id}")},
                                        onStartProgram={viewModel.startCustomProgramAndOpenWorkout(it)})
                                }
                                VelarisNavDestination.TRAINING_DETAILS -> {
                                    val extra = configuredExtras.firstOrNull { "extra:${it.slotIndex}" == trainingDetailsId }
                                    val slot = dailySlots.firstOrNull { it.slot.id==trainingDetailsId }
                                    if(trainingDetailsId?.startsWith("program:")==true) {
                                        TrainingProgramDetails(customPrograms.firstOrNull {"program:${it.id}"==trainingDetailsId},snapshot.isActiveOrPaused,
                                            onSave={viewModel.saveCustomProgram(it)},onDelete={viewModel.deleteCustomProgram(it)},
                                            onStart={viewModel.startCustomProgramAndOpenWorkout(it)})
                                    } else if(extra!=null || slot!=null) {
                                        val configuration=if(extra!=null) com.example.data.TrainingConfiguration.from(extra)
                                            else com.example.data.TrainingConfiguration.from(slot!!.slot)
                                        val defaults=if(extra!=null) com.example.data.TrainingConfiguration.from(com.example.data.ExtraWorkoutPresets.all.first {it.slotIndex==extra.slotIndex})
                                            else viewModel.trainingDefaultConfiguration(slot!!.slot)
                                        TrainingSettingsDetails(if(extra!=null) when(extra.slotIndex) {101->"Быстрые сокращения";102->"Силовые сокращения";else->"Длительные удержания"} else slot!!.slot.titleRu,
                                            configuration,defaults,snapshot.isActiveOrPaused || !trainingStateReady,
                                            if(extra!=null) extraTrainingCycle.isLocked(extra.slotIndex) else slot!!.isCompleted,
                                            onSave={value,result->viewModel.saveTrainingConfiguration(value,result)},
                                            onReset={key,result->viewModel.resetTrainingConfiguration(key,result)},
                                            onStart={if(extra!=null) viewModel.startFreeWorkoutAndOpenWorkout(extra) else viewModel.startSlotAndOpenWorkout(slot!!)})
                                    } else TrainingExerciseDetails(null,snapshot.isActiveOrPaused,{})
                                }

                                VelarisNavDestination.PLAN_DAY,
                                VelarisNavDestination.OTHER_WORKOUTS -> {
                                    VelarisPlanScreen(
                                        snapshot = snapshot,
                                        settings = settings,
                                        dailySlots = dailySlots,
                                        freeTemplates = freeTemplates,
                                        customPrograms = customPrograms,
                                        onStartSlot = { slot -> viewModel.startSlotAndOpenWorkout(slot) },
                                        onSelectSlot = { slot -> viewModel.selectDailySlot(slot) },
                                        onStartFreeTemplate = { template -> viewModel.startFreeWorkoutAndOpenWorkout(template) },
                                        onSaveFreeTemplate = { template -> viewModel.saveFreeTemplate(template) },
                                        onStartCustomProgram = { prog -> viewModel.startCustomProgramAndOpenWorkout(prog) },
                                        onSaveCustomProgram = { prog -> viewModel.saveCustomProgram(prog) {ok->if(ok) openPage(VelarisNavDestination.PLAN)} },
                                        onDeleteCustomProgram = { id -> viewModel.deleteCustomProgram(id) },
                                        onDuplicateCustomProgram = { prog -> viewModel.duplicateCustomProgram(prog) },
                                        onReorderCustomPrograms = { list -> viewModel.reorderCustomPrograms(list) },
                                        onReturnToWorkout = { openPage(VelarisNavDestination.WORKOUT) },
                                        otherModesOnly = destination == VelarisNavDestination.OTHER_WORKOUTS,
                                        initialMode = if(destination == VelarisNavDestination.OTHER_WORKOUTS) "constructor" else null
                                    )
                                }

                                VelarisNavDestination.CHANTAL -> {
                                    androidx.compose.runtime.key(chantalReturnToken) {
                                    VelarisChantalScreen(
                                        completedWorkoutsCount = realCompletionsCount,
                                        earnedWorkoutEnergy = workoutEnergy,
                                        currentStreakDays = weeklyOverview.currentStreakDays,
                                        onOpenWorkoutPlan = { openPage(VelarisNavDestination.PLAN) },
                                        onPlaySampleTone = { sig -> viewModel.playSample(sig) },
                                    onPageChanged = { chantalPageName = it },
                                    onChildBackChanged = { chantalChildBack = it }
                                    )
                                    }
                                }

                                VelarisNavDestination.WORKOUT -> {
                                    VelarisActiveWorkoutScreen(
                                        snapshot = snapshot,
                                        hardwareStatus = hardwareStatus,
                                        soundEnabled = settings.soundEnabled,
                                        lastCompletedDifficulty = lastCompletedDifficulty,
                                        onStart = { viewModel.startDemo() },
                                        onPause = { viewModel.pauseDemo() },
                                        onResume = { viewModel.resumeDemo() },
                                        onRequestStop = { viewModel.requestStopDemo() },
                                        onReset = { viewModel.resetDemo() },
                                        onRateDifficulty = { diff -> viewModel.rateLastCompletedSlotDifficulty(diff) },
                                        onNavigateToPlan = { openPage(VelarisNavDestination.PLAN) },
                                        onNavigateToReader = { openPage(VelarisNavDestination.HOME) }
                                    )
                                }

                                VelarisNavDestination.SETTINGS, VelarisNavDestination.SETTINGS_APPEARANCE,
                                VelarisNavDestination.SETTINGS_SOUND, VelarisNavDestination.SETTINGS_SAMPLES,
                                VelarisNavDestination.SETTINGS_BACKUP, VelarisNavDestination.SETTINGS_ABOUT -> {
                                    VelarisSettingsScreen(
                                        settingsCategory = destination.id.removePrefix("settings_").takeIf { destination != VelarisNavDestination.SETTINGS },
                                        onOpenSettingsCategory = { category -> openPage(VelarisNavDestination.entries.first { it.id == "settings_$category" }) },
                                        settings = settings,
                                        snapshot = snapshot,
                                        storyState = storyState,
                                        pendingRestorePreview = pendingRestorePreview,
                                        backupOperationBanner = backupOperationBanner,
                                        backupErrorMessage = backupErrorMessage,
                                        availableProtocols = viewModel.availableProtocols,
                                        onThemeChange = { viewModel.setThemeId(it) },
                                        onSoundPackChange = { viewModel.setSoundPack(it) },
                                        onMetronomeTickStyleChange = { viewModel.setMetronomeTickStyle(it) },
                                        onSoundEnabledChange = { viewModel.setSoundEnabled(it) },
                                        onVibrationEnabledChange = { viewModel.setVibrationEnabled(it) },
                                        onMetronomeEnabledChange = { viewModel.setMetronomeEnabled(it) },
                                        onDuckOtherAppsChange = { viewModel.setDuckOtherApps(it) },
                                        onSignalVolumeChange = { viewModel.setSignalVolume(it) },
                                        onMetronomeVolumeChange = { viewModel.setMetronomeVolume(it) },
                                        onPlaySample = { viewModel.playSample(it) },
                                        onReaderModeChange = { viewModel.setReaderMode(it) },
                                        onReaderFontSizeChange = { viewModel.setReaderFontSize(it) },
                                        onReaderLineSpacingChange = { viewModel.setReaderLineSpacing(it) },
                                        onSelectProtocol = { viewModel.selectProtocol(it) },
                                        onImportMarkdownUri = { uri -> viewModel.importMarkdownFromUri(uri) },
                                        onRemoveImportedBook = { viewModel.removeImportedBook() },
                                        onCreateBackupUri = { uri -> viewModel.createBackupToUri(uri) },
                                        onInspectBackupUri = { uri -> viewModel.inspectBackupFromUri(uri) },
                                        onConfirmRestore = { viewModel.confirmPendingRestore() },
                                        onCancelRestore = { viewModel.cancelPendingRestore() },
                                        onDismissBackupBanners = { viewModel.dismissBackupBanners() },
                                        onOpenDiagnostics = { openPage(VelarisNavDestination.DIAGNOSTICS) },
                                        onReturnToWorkout = { openPage(VelarisNavDestination.WORKOUT) }
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
                                        onZeroSlotDemoModeChange = { viewModel.setZeroSlotDemoMode(it) },
                                        onImportNeutralTestMarkdown = { viewModel.importNeutralTestMarkdown() },
                                        onRefreshHardware = { viewModel.refreshHardwareStatus() },
                                        onClearAudioDiagnostics = { viewModel.clearAudioDiagnostics() },
                                        onClearEventLog = { viewModel.clearEventLog() },
                                        onBackToSettings = { openPage(VelarisNavDestination.SETTINGS) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
            }
            ExtraCycleFireworks(extraTrainingCycle)
        }
    }
}

/**
 * Компактная плашка текущей тренировки над нижней панелью навигации (Итерация v1.4 п. 3).
 * Никогда не перекрывает текст книги, кнопки или системные отступы.
 * Показывает текущую фазу/паузу, оставшееся время вживую, кнопку паузы и возврат к таймеру.
 */
@Composable
fun CompactActiveWorkoutBar(
    snapshot: PhaseEngineSnapshot,
    onPauseWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onReturnToWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val animatorsEnabled = remember {
        runCatching { (android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()) }.getOrDefault(true) &&
            !android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    }
    var frameNowMs by remember(snapshot.runId, snapshot.status, snapshot.snapshotElapsedRealtimeMs) {
        mutableLongStateOf(snapshot.snapshotElapsedRealtimeMs)
    }

    LaunchedEffect(snapshot.isRunningOrPreparing, snapshot.snapshotElapsedRealtimeMs, animatorsEnabled) {
        if (snapshot.isRunningOrPreparing && animatorsEnabled) {
            val baseEngineMs = snapshot.snapshotElapsedRealtimeMs
            val baseLocalMs = SystemClock.elapsedRealtime()
            while (true) {
                withFrameMillis {
                    val elapsedSinceSnapshot = (SystemClock.elapsedRealtime() - baseLocalMs).coerceAtLeast(0L)
                    frameNowMs = baseEngineMs + elapsedSinceSnapshot
                }
            }
        } else {
            frameNowMs = snapshot.snapshotElapsedRealtimeMs
        }
    }

    val phaseSec = snapshot.computeDisplayWholeSeconds(frameNowMs)
    val phaseBadgeColor = when (snapshot.status) {
        RunStatus.PREPARING -> palette.phasePrepAccent
        RunStatus.RUNNING -> if (snapshot.currentPhase == PhaseType.CONTRACTION) {
            palette.phaseHoldAccent
        } else {
            palette.phaseRelaxAccent
        }
        else -> palette.phasePauseAccent
    }
    val statusTitle = when (snapshot.status) {
        RunStatus.PREPARING -> "Подготовка · ${phaseSec} с"
        RunStatus.RUNNING -> "${snapshot.currentPhase.labelRu} · ${phaseSec} с"
        else -> "Пауза — расслабьтесь"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, phaseBadgeColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .clickable { onReturnToWorkout() }
            .testTag("active_session_return_banner"),
        color = palette.surfaceElevated,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(phaseBadgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Цикл ${snapshot.currentRepetition}/${snapshot.totalRepetitions} · Осталось ${formatDurationCompact(snapshot.remainingActiveMs)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (snapshot.isRunningOrPreparing) {
                    FilledTonalButton(
                        onClick = onPauseWorkout,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.surfaceVariant,
                            contentColor = palette.textPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_compact_pause")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Пауза",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onResumeWorkout,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = palette.surfaceVariant,
                            contentColor = palette.primaryAccent
                        ),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_compact_resume")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Продолжить",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Button(
                    onClick = onReturnToWorkout,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("btn_return_to_workout")
                ) {
                    Text(
                        text = "Открыть",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Компактная воздушная шапка с мягким силуэтом гор (88.dp) без визуального перегруза.
 */
@Composable
fun VelarisMountainHeader(
    subtitleRu: String,
    isDemoActive: Boolean,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val palette = LocalVelarisPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.heroGradientStart, palette.heroGradientEnd)
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .semantics { contentDescription = "Декоративный горный пейзаж Velaris" }
        ) {
            val w = size.width
            val h = size.height

            val farMountain = Path().apply {
                moveTo(0f, h * 0.80f)
                lineTo(w * 0.22f, h * 0.40f)
                lineTo(w * 0.45f, h * 0.66f)
                lineTo(w * 0.72f, h * 0.34f)
                lineTo(w, h * 0.70f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(farMountain, color = palette.mountainFar)

            val midMountain = Path().apply {
                moveTo(0f, h * 0.86f)
                lineTo(w * 0.15f, h * 0.60f)
                lineTo(w * 0.38f, h * 0.78f)
                lineTo(w * 0.61f, h * 0.48f)
                lineTo(w * 0.86f, h * 0.74f)
                lineTo(w, h * 0.58f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(midMountain, color = palette.mountainMid)

            val nearMountain = Path().apply {
                moveTo(0f, h * 0.92f)
                lineTo(w * 0.28f, h * 0.70f)
                lineTo(w * 0.53f, h * 0.86f)
                lineTo(w * 0.80f, h * 0.66f)
                lineTo(w, h * 0.84f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(nearMountain, color = palette.mountainNear)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier
                .size(VelarisSpacing.TouchTarget).testTag("toolbar_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = palette.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Velaris",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = subtitleRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryAccent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onOpenSettings != null) IconButton(onClick = onOpenSettings, modifier = Modifier
                .size(VelarisSpacing.TouchTarget).testTag("open_settings")) {
                Icon(Icons.Default.Settings, contentDescription = "Открыть настройки", tint = palette.textPrimary)
            }
        }
    }
}

@Composable
private fun DatabaseWarningBanner(message: String) {
    val palette = LocalVelarisPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.errorAccent.copy(alpha = 0.16f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("db_error_banner")
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.errorAccent,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Отдельный экран активной тренировки с плавным кольцом и крупными секундами.
 */
@Composable
fun VelarisActiveWorkoutScreen(
    snapshot: PhaseEngineSnapshot,
    hardwareStatus: AudioHardwareStatus,
    soundEnabled: Boolean,
    lastCompletedDifficulty: WorkoutDifficulty?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestStop: () -> Unit,
    onReset: () -> Unit,
    onRateDifficulty: (WorkoutDifficulty?) -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val ringColor = resolveRingColor(snapshot.status, snapshot.currentPhase, palette)
    var showDetails by rememberSaveable(snapshot.runId) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_workout_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (snapshot.status == RunStatus.COMPLETED) {
            WorkoutCompletionRewardCard(
                snapshot = snapshot,
                selectedDifficulty = lastCompletedDifficulty,
                onSelectDifficulty = onRateDifficulty,
                onNavigateToPlan = onNavigateToPlan,
                onNavigateToReader = onNavigateToReader
            )
            return@Column
        }

        // Компактный индикатор звука без вылезания за правый край
        CompactAudioStatusRow(
            hardwareStatus = hardwareStatus,
            soundEnabled = soundEnabled
        )

        // Основная карточка таймера
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.outline.copy(alpha = 0.30f), RoundedCornerShape(24.dp))
                .testTag("timer_state_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snapshot.protocol.titleRu,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToPlan,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_minimize_workout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "К списку тренировок",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Свернуть",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Фиксированный по высоте блок заголовка фазы
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = snapshot.headlineCommandRu,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ringColor,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("headline_command_text")
                    )
                }

                // Изолированное кольцо таймера со свечением
                ActiveWorkoutTimerRing(
                    snapshot = snapshot,
                    palette = palette
                )

                if (snapshot.interruptionReason != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.warningAccent.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = snapshot.interruptionReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.warningAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(10.dp)
                                .testTag("interruption_reason_text")
                        )
                    }
                }

                WorkoutControlButtons(
                    status = snapshot.status,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onRequestStop = onRequestStop,
                    onReset = onReset
                )

                TextButton(onClick = { showDetails = !showDetails }, modifier = Modifier.testTag("btn_workout_details")) {
                    Text(if (showDetails) "Скрыть параметры" else "Параметры упражнения")
                }
                if (showDetails) Text(
                    "Удержание ${snapshot.protocol.contractionMs / 1000} с · отдых ${snapshot.protocol.relaxationMs / 1000} с · ${snapshot.totalRepetitions} повторов",
                    color = palette.textSecondary, modifier = Modifier.testTag("workout_protocol_details"))
            }
        }


    }
}

@Composable
private fun CompactAudioStatusRow(
    hardwareStatus: AudioHardwareStatus,
    soundEnabled: Boolean
) {
    val palette = LocalVelarisPalette.current
    val showSilentWarning = soundEnabled &&
        hardwareStatus.isSilentOrVibrateRinger &&
        !hardwareStatus.isHeadphonesConnected

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_audio_status_chip")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            !soundEnabled -> Icons.AutoMirrored.Filled.VolumeOff
                            hardwareStatus.isHeadphonesConnected -> Icons.Default.Headphones
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = null,
                        tint = palette.secondaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (soundEnabled) {
                            hardwareStatus.outputRouteLabelRu
                        } else {
                            "Звук выключен (вибрация)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = hardwareStatus.ringerModeLabelRu,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (showSilentWarning) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.warningAccent.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("silent_mode_warning")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = palette.warningAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Телефон в режиме «Без звука» или «Вибрация» без наушников: ориентируйтесь на вибрацию или включите звук.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.warningAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutCompletionRewardCard(
    snapshot: PhaseEngineSnapshot,
    selectedDifficulty: WorkoutDifficulty?,
    onSelectDifficulty: (WorkoutDifficulty?) -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToReader: () -> Unit
) {
    val palette = LocalVelarisPalette.current
    var ratingExpanded by rememberSaveable(snapshot.runId) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, palette.successAccent.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .testTag("workout_completion_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = palette.successAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Тренировка завершена",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.successAccent,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Выполнено: ${formatDurationCompact(snapshot.totalActiveDurationMs)} · Циклов: ${snapshot.totalRepetitions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            TextButton(onClick = { ratingExpanded = !ratingExpanded }, modifier = Modifier.testTag("btn_rate_workout")) {
                Text(if (ratingExpanded) "Скрыть оценку" else selectedDifficulty?.let { "Нагрузка: ${it.labelRu} · изменить" } ?: "Оценить нагрузку · по желанию")
            }
            if (ratingExpanded) {
            Text(
                text = "Как ощущалась нагрузка? (по желанию)",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutDifficulty.entries.forEach { diff ->
                    val isSelected = selectedDifficulty == diff
                    FilledTonalButton(
                        onClick = {
                            onSelectDifficulty(if (isSelected) null else diff)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) palette.primaryAccent else palette.surfaceVariant,
                            contentColor = if (isSelected) palette.onPrimaryAccent else palette.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("difficulty_btn_${diff.code.lowercase(Locale.ROOT)}")
                    ) {
                        Text(
                            text = diff.labelRu,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            }

            Button(
                onClick = onNavigateToPlan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("btn_completion_to_plan")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("К тренировкам", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WorkoutControlButtons(
    status: RunStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestStop: () -> Unit,
    onReset: () -> Unit
) {
    val palette = LocalVelarisPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (status) {
            RunStatus.IDLE,
            RunStatus.COMPLETED,
            RunStatus.STOPPED -> {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("btn_start_demo")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Начать тренировку",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (status == RunStatus.COMPLETED || status == RunStatus.STOPPED) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_reset_demo")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сбросить таймер", color = palette.textPrimary)
                    }
                }
            }

            RunStatus.PREPARING,
            RunStatus.RUNNING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.phasePauseAccent,
                            contentColor = palette.onPrimaryAccent
                        ),
                        modifier = Modifier
                            .weight(1.25f)
                            .heightIn(min = 56.dp)
                            .testTag("btn_pause_demo")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Пауза",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onRequestStop,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .testTag("btn_stop_demo")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = palette.errorAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Стоп", color = palette.errorAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            RunStatus.PAUSED,
            RunStatus.INTERRUPTED -> {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("btn_resume_demo")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Продолжить (через 3 сигнала)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onRequestStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("btn_stop_demo")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = palette.errorAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Остановить без зачёта", color = palette.errorAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StopWorkoutConfirmDialog(
    onStayPaused: () -> Unit,
    onConfirmStop: () -> Unit
) {
    val palette = LocalVelarisPalette.current
    AlertDialog(
        onDismissRequest = onStayPaused,
        title = {
            Text(
                text = "Завершить без зачёта?",
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        },
        text = {
            Text(
                text = "Тренировка сейчас на паузе («Пауза — расслабьтесь»). При остановке текущая попытка не будет засчитана в выполненные.",
                color = palette.textSecondary
            )
        },
        containerColor = palette.surfaceElevated,
        confirmButton = {
            Button(
                onClick = onConfirmStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.errorAccent,
                    contentColor = palette.onPrimaryAccent
                ),
                modifier = Modifier.testTag("btn_confirm_stop")
            ) {
                Text("Завершить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onStayPaused,
                modifier = Modifier.testTag("btn_stay_paused")
            ) {
                Text("Остаться на паузе", color = palette.textPrimary)
            }
        },
        modifier = Modifier.testTag("stop_confirm_dialog")
    )
}

internal fun formatDurationCompact(ms: Long): String {
    val totalSec = ((ms + 999L) / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", min, sec)
}
