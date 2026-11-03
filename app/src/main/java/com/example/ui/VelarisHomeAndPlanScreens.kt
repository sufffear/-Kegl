package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.data.StoryReaderUiState
import com.example.data.WeeklyDaySummary
import com.example.data.WeeklyOverviewState
import com.example.domain.AppBuildInfo
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PlanGroup
import com.example.domain.WorkoutDifficulty
import com.example.ui.theme.LocalVelarisPalette

/**
 * Спокойная статичная шапка с тонкими силуэтами гор и умеренными световыми акцентами (ТЗ п. 4, Update v1.1 п. 2).
 * Без постоянных частиц, пульсирующего фона и тяжёлых эффектов.
 */
@Composable
fun VelarisMountainHeader(
    buildInfo: AppBuildInfo,
    subtitleRu: String,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val mountainDesc = stringResource(R.string.mountain_illustration_desc)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .semantics { contentDescription = mountainDesc }
            ) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.backgroundTop,
                            palette.backgroundMid,
                            palette.backgroundBottom
                        )
                    )
                )

                // Мягкое статичное свечение горизонта
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primaryAccent.copy(alpha = 0.18f),
                            palette.primaryAccent.copy(alpha = 0.0f)
                        ),
                        center = Offset(w * 0.76f, h * 0.34f),
                        radius = w * 0.42f
                    ),
                    center = Offset(w * 0.76f, h * 0.34f),
                    radius = w * 0.42f
                )

                // Несколько тонких статичных звёздных точек
                val starColor = palette.textPrimary.copy(alpha = 0.40f)
                drawCircle(color = starColor, radius = 1.6f, center = Offset(w * 0.18f, h * 0.22f))
                drawCircle(color = starColor, radius = 2.1f, center = Offset(w * 0.46f, h * 0.16f))
                drawCircle(color = starColor, radius = 1.8f, center = Offset(w * 0.82f, h * 0.20f))

                // Дальний хребет
                val backPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.68f)
                    lineTo(w * 0.22f, h * 0.44f)
                    lineTo(w * 0.45f, h * 0.62f)
                    lineTo(w * 0.72f, h * 0.36f)
                    lineTo(w, h * 0.58f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = backPath, color = palette.mountainBackLayer)

                // Средний хребет
                val midPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.78f)
                    lineTo(w * 0.15f, h * 0.58f)
                    lineTo(w * 0.38f, h * 0.74f)
                    lineTo(w * 0.60f, h * 0.48f)
                    lineTo(w * 0.86f, h * 0.68f)
                    lineTo(w, h * 0.62f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = midPath, color = palette.mountainMidLayer)

                // Ближний силуэт
                val frontPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.85f)
                    lineTo(w * 0.28f, h * 0.70f)
                    lineTo(w * 0.54f, h * 0.84f)
                    lineTo(w * 0.80f, h * 0.66f)
                    lineTo(w, h * 0.79f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = frontPath, color = palette.mountainFrontLayer)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Velaris",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .semantics { heading() }
                            .testTag("screen_title")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.tech_check_version_format,
                            buildInfo.versionName,
                            buildInfo.versionCode
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primaryAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("version_subtitle")
                    )
                }

                if (onOpenSettings != null) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceVariant.copy(alpha = 0.75f))
                            .testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Открыть настройки",
                            tint = palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DemoSafetyBanner(modifier: Modifier = Modifier) {
    val palette = LocalVelarisPalette.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.warningAccent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .testTag("demo_safety_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Предупреждение о демо-режиме",
                tint = palette.warningAccent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.demo_safety_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.warningAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Активирован демонстрационный режим. В реальную недельную статистику демо-записи не включаются.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun ActiveSessionReturnBanner(
    snapshot: PhaseEngineSnapshot,
    onReturnToWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, palette.primaryAccent, RoundedCornerShape(18.dp))
            .testTag("active_session_return_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Активная тренировка: ${snapshot.status.labelRu}",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.primaryAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${snapshot.protocol.titleRu} · Повтор ${snapshot.currentRepetition}/${snapshot.totalRepetitions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onReturnToWorkout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_return_to_workout")
            ) {
                Text("Вернуться к тренировке", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Экран 1: Главная (Update v1.1 п. 2).
 * Показывает:
 * - карточку «Тренировка»: прогресс «Сегодня X из N» (или «Сегодня без тренировок» при N=0 без 0/0),
 *   пометку «Демонстрация» при невыбранной реальной программе, следующее упражнение и переход к плану/старту;
 * - карточку «История»: название главы, сохранённое место и кнопку «Продолжить»;
 * - компактный недельный обзор на 7 дней (только по реальным записям);
 * - переход в Настройки.
 */
@Composable
fun VelarisHomeScreen(
    buildInfo: AppBuildInfo,
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    dailySlots: List<DailySlotWithCompletion>,
    isZeroSlotDemoMode: Boolean,
    weeklyOverview: WeeklyOverviewState,
    storyState: StoryReaderUiState,
    onStartOrOpenWorkout: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectWeeklyDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val totalSlots = dailySlots.size
    val completedSlots = dailySlots.count { it.isCompleted }
    val nextSlot = dailySlots.firstOrNull { !it.isCompleted }
    val isDemoMode = !settings.isRealProgramEnabled || isZeroSlotDemoMode

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        VelarisMountainHeader(
            buildInfo = buildInfo,
            subtitleRu = "Спокойный ритм тренировок и интерактивная история",
            onOpenSettings = onOpenSettings
        )

        if (isDemoMode) {
            DemoSafetyBanner()
        }

        if (snapshot.isActiveOrPaused) {
            ActiveSessionReturnBanner(
                snapshot = snapshot,
                onReturnToWorkout = onStartOrOpenWorkout
            )
        }

        // Карточка «Тренировка»
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_workout_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Тренировка",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDemoMode) {
                            palette.warningAccent.copy(alpha = 0.18f)
                        } else {
                            palette.successAccent.copy(alpha = 0.18f)
                        }
                    ) {
                        Text(
                            text = if (isDemoMode) "Демонстрация" else "Базовый план",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDemoMode) palette.warningAccent else palette.successAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("home_workout_mode_badge")
                        )
                    }
                }

                // Прогресс «Сегодня X из N» или «Сегодня без тренировок» без 0/0
                val progressText = if (totalSlots == 0) {
                    "Сегодня без тренировок"
                } else {
                    "Сегодня $completedSlots из $totalSlots"
                }
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("home_today_progress")
                )

                val nextSummary = when {
                    totalSlots == 0 -> "На сегодня подходы не назначены. Спокойный день отдыха."
                    nextSlot != null -> {
                        val s = nextSlot.slot
                        "Следующее: ${s.titleRu} · ${s.contractionMs / 1000}с / ${s.relaxationMs / 1000}с × ${s.repetitions}"
                    }
                    else -> "Все назначенные подходы на сегодня спокойно выполнены."
                }
                Text(
                    text = nextSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (totalSlots > 0 && (nextSlot != null || snapshot.isActiveOrPaused)) {
                        Button(
                            onClick = onStartOrOpenWorkout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = palette.onPrimaryAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_home_start_workout")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (snapshot.isActiveOrPaused) "К подходу" else "Начать подход",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenPlan,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_open_plan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = palette.secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "К плану дня",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Карточка «История» (Update v1.1 п. 3, п. 7: если книга не импортирована — «Добавить книгу»)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_story_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = palette.secondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "История",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.surfaceVariant
                    ) {
                        Text(
                            text = if (storyState.hasImportedBook) {
                                "${storyState.chapterCount} гл."
                            } else {
                                "Офлайн Markdown"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = if (storyState.hasImportedBook) {
                        storyState.displayTitleRu
                    } else {
                        "Книга ещё не импортирована"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("home_story_book_title")
                )

                Text(
                    text = if (storyState.hasImportedBook) {
                        storyState.savedPositionSummaryRu
                    } else {
                        "Откройте локальный файл романа (.md / .markdown) для чтения в режимах «Книга» и «Пошаговый»."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.testTag("home_story_saved_position")
                )

                FilledTonalButton(
                    onClick = onOpenReader,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.surfaceVariant,
                        contentColor = palette.primaryAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_open_reader")
                ) {
                    Text(
                        text = if (!storyState.hasImportedBook) {
                            "Добавить книгу"
                        } else {
                            "Продолжить"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Простой недельный обзор на 7 дней (Update v1.1 п. 3)
        WeeklyOverviewCard(
            weeklyOverview = weeklyOverview,
            onSelectDate = onSelectWeeklyDate
        )
    }
}

/**
 * Простой недельный обзор на 7 дней (Update v1.1 п. 3):
 * - количество выполненных подходов за каждый из последних 7 дней;
 * - выделение сегодняшнего дня;
 * - короткая сводка при нажатии на день;
 * - строится строго по реальным сохранённым записям (`isDemo = false`);
 * - честное спокойное пустое состояние, если реальных записей ещё нет.
 */
@Composable
fun WeeklyOverviewCard(
    weeklyOverview: WeeklyOverviewState,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val selectedDay: WeeklyDaySummary? = weeklyOverview.selectedDaySummary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Недельный обзор · 7 дней",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "За неделю: ${weeklyOverview.totalRealCompletionsInWeek}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("weekly_total_real_count")
                )
            }

            if (!weeklyOverview.hasAnyRealData) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_empty_state")
                ) {
                    Text(
                        text = "Реальных завершённых подходов пока нет. Демо-результаты учитываются отдельно и не попадают в недельную статистику.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weeklyOverview.days.forEach { day ->
                    val isSelected = day.localDate == weeklyOverview.selectedDate
                    val borderColor = when {
                        isSelected -> palette.primaryAccent
                        day.isToday -> palette.secondaryAccent.copy(alpha = 0.75f)
                        else -> palette.outline.copy(alpha = 0.35f)
                    }
                    val containerColor = when {
                        day.completedCount > 0 -> palette.successAccent.copy(alpha = 0.16f)
                        isSelected -> palette.surfaceVariant
                        else -> palette.backgroundTop.copy(alpha = 0.55f)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .border(
                                width = if (isSelected || day.isToday) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDate(day.localDate) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("weekly_day_${day.localDate}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.shortLabelRu,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day.isToday) palette.primaryAccent else palette.textSecondary,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = day.dayOfMonthLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${day.completedCount}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (day.completedCount > 0) palette.successAccent else palette.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (selectedDay != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_selected_day_summary")
                ) {
                    val dayHeader = if (selectedDay.isToday) {
                        "Сегодня (${selectedDay.localDate})"
                    } else {
                        "${selectedDay.shortLabelRu}, ${selectedDay.localDate}"
                    }
                    val details = if (selectedDay.completedCount == 0) {
                        "Выполненных подходов: 0."
                    } else {
                        "Выполнено подходов: ${selectedDay.completedCount} (${selectedDay.completedTitlesRu.joinToString(", ")})"
                    }
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = dayHeader,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Экран 2: План дня (Update v1.1 п. 2).
 * Показывает группы «Утро / Обед / Вечер», упражнения выбранного плана,
 * выполненные карточки со спокойной галочкой и подписью «Завершено»,
 * ручной запуск без штрафов и «долгов» за пропуски.
 * Напоминания и кнопка «Как выполнять» отсутствуют по решению пользователя.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VelarisPlanScreen(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    dailySlots: List<DailySlotWithCompletion>,
    isZeroSlotDemoMode: Boolean,
    demoCompletionsCount: Int,
    realCompletionsCount: Int,
    databaseErrorMessage: String?,
    onSelectDailySlot: (DailySlotWithCompletion) -> Unit,
    onStartSelectedSlot: (DailySlotWithCompletion) -> Unit,
    onReturnToActiveWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val totalSlots = dailySlots.size
    val completedSlots = dailySlots.count { it.isCompleted }
    val isDemoMode = !settings.isRealProgramEnabled || isZeroSlotDemoMode

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isDemoMode) {
            DemoSafetyBanner()
        }

        if (snapshot.isActiveOrPaused) {
            ActiveSessionReturnBanner(
                snapshot = snapshot,
                onReturnToWorkout = onReturnToActiveWorkout
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_plan_persistence_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "План на сегодня",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDemoMode) {
                                "Демонстрационный план · Запуск вручную без штрафов за пропуски"
                            } else {
                                "${settings.realProgramSource} · Запуск вручную в спокойном темпе"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceVariant
                    ) {
                        val badgeText = if (totalSlots == 0) {
                            "Сегодня без тренировок"
                        } else {
                            "Сегодня $completedSlots из $totalSlots"
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (completedSlots == totalSlots && totalSlots > 0) {
                                palette.successAccent
                            } else {
                                palette.primaryAccent
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("daily_plan_progress_badge")
                        )
                    }
                }

                if (databaseErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.errorAccent.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = databaseErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.errorAccent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (totalSlots == 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zero_slots_rest_day_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Сегодня без тренировок",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.primaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "В плане на сегодня назначено 0 подходов. Отдыхайте или откройте интерактивную историю.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val grouped = dailySlots.groupBy { PlanGroup.fromCode(it.slot.groupCode) }
                    PlanGroup.entries.forEach { group ->
                        val groupSlots = grouped[group].orEmpty()
                        if (groupSlots.isNotEmpty()) {
                            Text(
                                text = group.titleRu,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            groupSlots.forEach { item ->
                                val slot = item.slot
                                val isSelected = snapshot.protocol.id == slot.id
                                val isCompleted = item.isCompleted
                                val difficultyLabel = WorkoutDifficulty.fromCode(item.completion?.optionalDifficulty)?.labelRu

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = when {
                                                isCompleted -> palette.successAccent.copy(alpha = 0.65f)
                                                isSelected -> palette.primaryAccent
                                                else -> palette.outline.copy(alpha = 0.4f)
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable(enabled = !snapshot.isActiveOrPaused) {
                                            onSelectDailySlot(item)
                                        }
                                        .testTag("daily_slot_${slot.sortOrder}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            palette.surfaceVariant
                                        } else {
                                            palette.backgroundTop.copy(alpha = 0.65f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (!snapshot.isActiveOrPaused) {
                                                            onSelectDailySlot(item)
                                                        }
                                                    },
                                                    enabled = !snapshot.isActiveOrPaused,
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = palette.primaryAccent,
                                                        unselectedColor = palette.textSecondary
                                                    )
                                                )
                                                Column {
                                                    Text(
                                                        text = slot.titleRu,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = palette.textPrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = "Сокращение ${slot.contractionMs / 1000} с · Расслабление ${slot.relaxationMs / 1000} с · Повторов: ${slot.repetitions}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }

                                            if (isCompleted) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = palette.successAccent.copy(alpha = 0.18f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Завершено",
                                                            tint = palette.successAccent,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (difficultyLabel != null) {
                                                                "Завершено · $difficultyLabel"
                                                            } else {
                                                                "Завершено"
                                                            },
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = palette.successAccent,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (!isCompleted && !snapshot.isActiveOrPaused) {
                                            Button(
                                                onClick = { onStartSelectedSlot(item) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = palette.primaryAccent,
                                                    contentColor = palette.onPrimaryAccent
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("btn_start_slot_${slot.sortOrder}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Начать этот подход",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Учтено в демо: $demoCompletionsCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        modifier = Modifier.testTag("demo_completions_counter")
                    )
                    Text(
                        text = "Учтено в реальной статистике: $realCompletionsCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        modifier = Modifier.testTag("real_completions_counter")
                    )
                }
            }
        }
    }
}
