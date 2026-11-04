package com.example.ui

import android.animation.ValueAnimator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotWithCompletion
import com.example.data.FreeIntervalTemplateEntity
import com.example.data.StoryReaderUiState
import com.example.data.WeeklyDaySummary
import com.example.data.WeeklyOverviewState
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PlanGroup
import com.example.domain.WorkoutDifficulty
import com.example.domain.calculateFreeCycles
import com.example.ui.theme.LocalVelarisPalette
import java.util.Locale

private fun formatSecondsMinSec(totalSec: Int): String {
    val clamped = totalSec.coerceAtLeast(0)
    val min = clamped / 60
    val sec = clamped % 60
    return String.format(Locale.US, "%d:%02d", min, sec)
}

/**
 * Экран «Главная» (Итерация v1.4, раздел 4):
 * - Блок тренировки (возврат к активной тренировке / прогресс плана на сегодня / запуск настроенного шаблона / день отдыха / переход к настройке);
 * - Блок книги (добавить книгу / продолжить чтение с текущей главы / книга прочитана);
 * - Недельный обзор за 7 дней (только реальные подходы, разделение «По плану» и «Мои интервалы», без `0/0` и без `0%`).
 */
@Composable
fun VelarisHomeScreen(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    dailySlots: List<DailySlotWithCompletion>,
    freeTemplates: List<FreeIntervalTemplateEntity>,
    weeklyOverview: WeeklyOverviewState,
    storyState: StoryReaderUiState,
    onOpenWorkout: () -> Unit,
    onStartNextSlot: (DailySlotWithCompletion) -> Unit,
    onStartFreeTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReader: () -> Unit,
    onSelectOverviewDay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val totalSlots = dailySlots.size
    val completedSlots = dailySlots.count { it.isCompleted }
    val nextAvailableSlot = dailySlots.firstOrNull { !it.isCompleted }
    val firstConfiguredTemplate = freeTemplates.firstOrNull { it.isValid }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Главная карточка «Тренировка» с выразительным градиентом
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, palette.primaryAccent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .testTag("home_workout_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(palette.heroGradientStart, palette.heroGradientEnd)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.primaryAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = palette.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (snapshot.isActiveOrPaused) "Текущая тренировка" else "Тренировка сегодня",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (settings.isRealProgramEnabled) {
                                palette.successAccent.copy(alpha = 0.20f)
                            } else {
                                palette.warningAccent.copy(alpha = 0.20f)
                            }
                        ) {
                            Text(
                                text = if (settings.isRealProgramEnabled) "Основной режим" else "Демо-режим",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.isRealProgramEnabled) palette.successAccent else palette.warningAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    when {
                        snapshot.isActiveOrPaused -> {
                            Text(
                                text = snapshot.protocol.titleRu,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${snapshot.headlineCommandRu} · Цикл ${snapshot.currentRepetition} из ${snapshot.totalRepetitions}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = onOpenWorkout,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 54.dp)
                                    .testTag("btn_home_primary_workout")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Вернуться к тренировке",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        totalSlots == 0 -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("zero_slots_calm_message")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Сегодня без тренировок",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "По расписанию на сегодня назначен день отдыха.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            if (firstConfiguredTemplate != null) {
                                Text(
                                    text = "Мои интервалы: «${firstConfiguredTemplate.name}» · ${formatSecondsMinSec(firstConfiguredTemplate.actualDurationSec)} (${firstConfiguredTemplate.calculatedCycles} циклов)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onStartFreeTemplate(firstConfiguredTemplate) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primaryAccent,
                                            contentColor = palette.onPrimaryAccent
                                        ),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .heightIn(min = 52.dp)
                                            .testTag("btn_home_primary_workout")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Начать «${firstConfiguredTemplate.name}»",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onOpenPlan,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 52.dp)
                                            .testTag("btn_home_open_plan")
                                    ) {
                                        Text("Все тренировки", color = palette.textPrimary)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = onOpenPlan,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("btn_home_primary_workout")
                                ) {
                                    Text("Настроить тренировку", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Выполнено сегодня: $completedSlots из $totalSlots",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (nextAvailableSlot != null) "Готово к старту" else "План дня выполнен",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (nextAvailableSlot != null) palette.secondaryAccent else palette.successAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (nextAvailableSlot != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.surfaceElevated.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Следующий подход:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Text(
                                            text = nextAvailableSlot.slot.titleRu,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Удержание ${nextAvailableSlot.slot.contractionMs / 1000} с · Отдых ${nextAvailableSlot.slot.relaxationMs / 1000} с · Циклов: ${nextAvailableSlot.slot.repetitions}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryAccent
                                        )
                                    }
                                }
                            } else if (firstConfiguredTemplate != null) {
                                Text(
                                    text = "Хотите позаниматься дополнительно? Доступен шаблон «${firstConfiguredTemplate.name}» (${formatSecondsMinSec(firstConfiguredTemplate.actualDurationSec)}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (nextAvailableSlot != null) {
                                            onStartNextSlot(nextAvailableSlot)
                                        } else if (firstConfiguredTemplate != null) {
                                            onStartFreeTemplate(firstConfiguredTemplate)
                                        } else {
                                            onOpenPlan()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .heightIn(min = 54.dp)
                                        .testTag("btn_home_primary_workout")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            nextAvailableSlot != null -> "Начать по плану"
                                            firstConfiguredTemplate != null -> "Начать «${firstConfiguredTemplate.name}»"
                                            else -> "К тренировкам"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = onOpenPlan,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 54.dp)
                                        .testTag("btn_home_open_plan")
                                ) {
                                    Text("Тренировки", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Карточка «Книга»
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.outline.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .testTag("home_story_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.secondaryAccent.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = palette.secondaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Книга",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (storyState.hasImportedBook) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (storyState.isCompleted) {
                                palette.successAccent.copy(alpha = 0.20f)
                            } else {
                                palette.primaryAccent.copy(alpha = 0.18f)
                            }
                        ) {
                            Text(
                                text = if (storyState.isCompleted) {
                                    "Книга прочитана"
                                } else {
                                    val chNum = storyState.currentChapter?.chapterNumber ?: 1
                                    "Глава $chNum из ${storyState.chapterCount}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (storyState.isCompleted) palette.successAccent else palette.primaryAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (!storyState.hasImportedBook) {
                    Text(
                        text = "Откройте вашу любимую книгу в формате Markdown для спокойного чтения между подходами.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                } else {
                    Text(
                        text = storyState.displayTitleRu,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    val chapterSubtitle = storyState.currentChapter?.let { ch ->
                        "${ch.actTitle} · ${ch.title}"
                    } ?: ""
                    if (chapterSubtitle.isNotBlank()) {
                        Text(
                            text = chapterSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Button(
                    onClick = onOpenReader,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.secondaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("btn_open_reader")
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !storyState.hasImportedBook -> "Добавить книгу"
                            storyState.isCompleted -> "Открыть книгу"
                            else -> "Продолжить читать"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Недельный обзор (7 последних дней)
        WeeklyOverviewCard(
            overview = weeklyOverview,
            onSelectDay = onSelectOverviewDay
        )
    }
}

/**
 * Компактная полоса недельного обзора за 7 дней (Итерация v1.4 п. 4):
 * - учитываются только реальные завершения (`isDemo = false`);
 * - раздельно показывает плановые («По плану») и свободные («Мои интервалы») тренировки за выбранный день;
 * - без круговых диаграмм, серий дней и без `0/0` или `0%`.
 */
@Composable
fun WeeklyOverviewCard(
    overview: WeeklyOverviewState,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val selectedSummary: WeeklyDaySummary? = overview.days.firstOrNull {
        it.localDate == overview.selectedDate
    } ?: overview.days.lastOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_overview_card"),
        shape = RoundedCornerShape(22.dp),
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
                    text = "Недельный обзор (7 дней)",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                if (overview.hasAnyRealData) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.primaryAccent.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "За 7 дней: ${overview.totalRealCompletionsInWeek}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!overview.hasAnyRealData) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_overview_empty_state")
                ) {
                    Text(
                        text = "Здесь появится история ваших выполненных тренировок по дням недели. Тестовые запуски в статистику не включаются.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_days_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                overview.days.forEach { day ->
                    val isSelected = day.localDate == selectedSummary?.localDate
                    val hasCompletions = day.completedCount > 0
                    val cellBg = when {
                        isSelected && hasCompletions -> palette.primaryAccent
                        isSelected -> palette.selectedContainer
                        hasCompletions -> palette.successAccent.copy(alpha = 0.24f)
                        else -> palette.surfaceElevated
                    }
                    val borderColor = when {
                        isSelected -> palette.selectedBorder
                        day.isToday -> palette.secondaryAccent
                        hasCompletions -> palette.successAccent.copy(alpha = 0.65f)
                        else -> palette.outline.copy(alpha = 0.35f)
                    }
                    val contentColor = if (isSelected && hasCompletions) {
                        palette.onPrimaryAccent
                    } else {
                        palette.textPrimary
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 66.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cellBg)
                            .border(
                                width = if (isSelected || day.isToday) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectDay(day.localDate) }
                            .padding(vertical = 8.dp, horizontal = 2.dp)
                            .semantics {
                                contentDescription = "${day.shortLabelRu} ${day.dayOfMonthLabel}: выполнено ${day.completedCount}"
                            }
                            .testTag("weekly_day_${day.localDate}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.shortLabelRu,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected && hasCompletions) palette.onPrimaryAccent else palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = day.dayOfMonthLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected && hasCompletions -> palette.onPrimaryAccent
                                        hasCompletions -> palette.successAccent
                                        day.isToday -> palette.secondaryAccent.copy(alpha = 0.7f)
                                        else -> palette.outline.copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }
            }

            if (selectedSummary != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_selected_day_details")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedSummary.isToday) {
                                    "Сегодня (${selectedSummary.shortLabelRu}, ${selectedSummary.dayOfMonthLabel})"
                                } else {
                                    "Дата: ${selectedSummary.localDate} (${selectedSummary.shortLabelRu})"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    selectedSummary.assignedCount == 0 &&
                                        selectedSummary.completedCount == 0 &&
                                        overview.hasAnyRealData -> "День отдыха"
                                    selectedSummary.completedCount > 0 -> "Выполнено: ${selectedSummary.completedCount}"
                                    else -> "Нет записей"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedSummary.completedCount > 0) palette.successAccent else palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedSummary.completedCount > 0) {
                            Text(
                                text = "По плану: ${selectedSummary.plannedCompletedCount} · Мои интервалы: ${selectedSummary.freeCompletedCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("weekly_selected_day_breakdown")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Экран «Тренировки» (Итерация v1.4, раздел 5):
 * - верхний переключатель «По плану» / «Мои интервалы»;
 * - плавное переключение между режимами;
 * - 5 редактируемых шаблонов с понятной проверкой полей, сохранением и защитой несохранённых правок.
 */
@Composable
fun VelarisPlanScreen(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    dailySlots: List<DailySlotWithCompletion>,
    freeTemplates: List<FreeIntervalTemplateEntity>,
    onStartSlot: (DailySlotWithCompletion) -> Unit,
    onSelectSlot: (DailySlotWithCompletion) -> Unit,
    onStartFreeTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onSaveFreeTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onReturnToWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val animatorsEnabled = remember {
        runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
    }
    var selectedTab by rememberSaveable { mutableStateOf("scheduled") }
    var editingSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val editingTemplate = remember(editingSlotIndex, freeTemplates) {
        editingSlotIndex?.let { idx -> freeTemplates.firstOrNull { it.slotIndex == idx } }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Верхний сегментированный переключатель «По плану / Мои интервалы»
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .testTag("plan_mode_switcher")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isScheduled = selectedTab == "scheduled"
                Button(
                    onClick = { selectedTab = "scheduled" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScheduled) palette.primaryAccent else palette.surfaceElevated,
                        contentColor = if (isScheduled) palette.onPrimaryAccent else palette.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("plan_tab_scheduled")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "По плану",
                        fontWeight = if (isScheduled) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }

                val isFree = selectedTab == "free"
                Button(
                    onClick = { selectedTab = "free" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFree) palette.primaryAccent else palette.surfaceElevated,
                        contentColor = if (isFree) palette.onPrimaryAccent else palette.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("plan_tab_free")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Мои интервалы",
                        fontWeight = if (isFree) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = if (animatorsEnabled) 160 else 0),
            label = "plan_mode_crossfade"
        ) { mode ->
            if (mode == "free") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (editingTemplate != null) {
                        FreeTemplateEditorCard(
                            template = editingTemplate,
                            onDismiss = { editingSlotIndex = null },
                            onSave = { updated ->
                                onSaveFreeTemplate(updated)
                                editingSlotIndex = null
                            }
                        )
                    }

                    FreeIntervalTemplatesSection(
                        templates = freeTemplates,
                        isSessionLocked = snapshot.isActiveOrPaused,
                        onStartTemplate = onStartFreeTemplate,
                        onEditTemplate = { template -> editingSlotIndex = template.slotIndex }
                    )
                }
            } else {
                val totalSlots = dailySlots.size
                val completedCount = dailySlots.count { it.isCompleted }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_plan_persistence_card"),
                    shape = RoundedCornerShape(22.dp),
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
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (settings.isRealProgramEnabled) {
                                        "Расписание подходов по группам дня"
                                    } else {
                                        "Ознакомительное расписание (демо-режим)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }

                            if (totalSlots > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.selectedContainer
                                ) {
                                    Text(
                                        text = "Выполнено: $completedCount из $totalSlots",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (totalSlots == 0) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("zero_slots_calm_message")
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Сегодня без тренировок",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "По расписанию на сегодня подходы не назначены. Вы можете отдохнуть или перейти во вкладку «Мои интервалы».",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val grouped = dailySlots.groupBy { PlanGroup.fromCode(it.slot.groupCode) }
                            PlanGroup.entries.sortedBy { it.order }.forEach { group ->
                                val groupItems = grouped[group].orEmpty()
                                if (groupItems.isNotEmpty()) {
                                    Text(
                                        text = group.titleRu,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    groupItems.forEach { item ->
                                        val isSelected = snapshot.protocol.id == item.slot.id
                                        val diffLabel = WorkoutDifficulty.fromCode(item.completion?.optionalDifficulty)?.labelRu
                                        val cardContainer = when {
                                            item.isCompleted -> palette.surfaceElevated
                                            isSelected -> palette.selectedContainer
                                            else -> palette.surfaceElevated.copy(alpha = 0.7f)
                                        }
                                        val borderColor = when {
                                            item.isCompleted -> palette.successAccent.copy(alpha = 0.7f)
                                            isSelected -> palette.selectedBorder
                                            else -> palette.outline.copy(alpha = 0.4f)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(cardContainer)
                                                .border(
                                                    width = if (isSelected || item.isCompleted) 1.5.dp else 1.dp,
                                                    color = borderColor,
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable(enabled = !snapshot.isActiveOrPaused) {
                                                    onSelectSlot(item)
                                                }
                                                .padding(14.dp)
                                                .testTag("daily_slot_${item.slot.assignmentId}"),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isCompleted) {
                                                            Icons.Default.CheckCircle
                                                        } else {
                                                            Icons.Default.RadioButtonUnchecked
                                                        },
                                                        contentDescription = if (item.isCompleted) "Выполнено" else "Ожидает выполнения",
                                                        tint = if (item.isCompleted) palette.successAccent else palette.textSecondary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = item.slot.titleRu,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = palette.textPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Удержание ${item.slot.contractionMs / 1000} с · Отдых ${item.slot.relaxationMs / 1000} с · Циклов: ${item.slot.repetitions}" +
                                                                (if (diffLabel != null) " · Оценка: $diffLabel" else ""),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = palette.textSecondary
                                                        )
                                                    }
                                                }

                                                if (item.isCompleted) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = palette.successAccent.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Зачтено",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = palette.successAccent,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (!item.isCompleted && !snapshot.isActiveOrPaused) {
                                                Button(
                                                    onClick = { onStartSlot(item) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = palette.primaryAccent,
                                                        contentColor = palette.onPrimaryAccent
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 48.dp)
                                                        .testTag("btn_start_slot_${item.slot.assignmentId}")
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Начать подход", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Секция «Мои интервалы» с 5 сохраняемыми шаблонами (Итерация v1.3 / v1.4 п. 5).
 */
@Composable
fun FreeIntervalTemplatesSection(
    templates: List<FreeIntervalTemplateEntity>,
    isSessionLocked: Boolean,
    onStartTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onEditTemplate: (FreeIntervalTemplateEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("free_interval_templates_section"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Мои интервалы (5 шаблонов)",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("free_intervals_disclaimer_caption")
            ) {
                Text(
                    text = "Ваши интервалы. Приложение не оценивает подходящую нагрузку",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (isSessionLocked) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.warningAccent.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
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
                            text = "Сейчас уже выполняется или стоит на паузе другая тренировка. Завершите её перед новым запуском.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.warningAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            templates.forEach { template ->
                val calc = calculateFreeCycles(
                    targetMinutes = template.targetDurationMinutes,
                    targetSeconds = template.targetDurationSeconds,
                    contractionSec = template.contractionSec,
                    relaxationSec = template.relaxationSec
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.surfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (calc.isValid) palette.outline.copy(alpha = 0.55f) else palette.errorAccent.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                        .testTag("free_template_card_${template.slotIndex}"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.selectedContainer
                        ) {
                            Text(
                                text = "Шаблон ${template.slotIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Удержание ${template.contractionSec} с · Отдых ${template.relaxationSec} с",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = calc.summaryRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (calc.isValid) palette.secondaryAccent else palette.errorAccent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("free_template_summary_${template.slotIndex}")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartTemplate(template) },
                            enabled = !isSessionLocked && calc.isValid && template.isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = palette.onPrimaryAccent,
                                disabledContainerColor = palette.disabledContainer,
                                disabledContentColor = palette.disabledContent
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_start_free_${template.slotIndex}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Начать", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onEditTemplate(template) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_edit_free_${template.slotIndex}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Изменить", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Редактор шаблона «Мои интервалы» (Итерация v1.4 п. 5):
 * - поля с явными единицами измерения и числовой клавиатурой;
 * - мгновенная проверка ошибок рядом с полем (включая предел 300 циклов без молчаливого сброса);
 * - защита от случайной потери несохранённых правок при нажатии «Отмена».
 */
@Composable
private fun FreeTemplateEditorCard(
    template: FreeIntervalTemplateEntity,
    onDismiss: () -> Unit,
    onSave: (FreeIntervalTemplateEntity) -> Unit
) {
    val palette = LocalVelarisPalette.current

    var nameInput by rememberSaveable(template.slotIndex) { mutableStateOf(template.name) }
    var minutesInput by rememberSaveable(template.slotIndex) { mutableStateOf(template.targetDurationMinutes.toString()) }
    var secondsInput by rememberSaveable(template.slotIndex) { mutableStateOf(template.targetDurationSeconds.toString()) }
    var contractionInput by rememberSaveable(template.slotIndex) { mutableStateOf(template.contractionSec.toString()) }
    var relaxationInput by rememberSaveable(template.slotIndex) { mutableStateOf(template.relaxationSec.toString()) }
    var showUnsavedConfirm by rememberSaveable(template.slotIndex) { mutableStateOf(false) }

    val parsedMin = minutesInput.trim().toIntOrNull()
    val parsedSec = secondsInput.trim().toIntOrNull()
    val parsedCon = contractionInput.trim().toIntOrNull()
    val parsedRel = relaxationInput.trim().toIntOrNull()

    val nameError = if (nameInput.trim().isEmpty()) "Введите название шаблона" else null
    val minutesError = when {
        parsedMin == null -> "Укажите целое число минут (0–90)"
        parsedMin !in 0..90 -> "Минуты должны быть от 0 до 90"
        else -> null
    }
    val secondsError = when {
        parsedSec == null -> "Укажите секунды (0–59)"
        parsedSec !in 0..59 -> "Секунды должны быть от 0 до 59"
        else -> null
    }
    val contractionError = when {
        parsedCon == null -> "Укажите секунды удержания (1–120)"
        parsedCon !in 1..120 -> "Удержание должно быть от 1 до 120 секунд"
        else -> null
    }
    val relaxationError = when {
        parsedRel == null -> "Укажите секунды отдыха (1–120)"
        parsedRel !in 1..120 -> "Отдых должен быть от 1 до 120 секунд"
        else -> null
    }

    val calc = calculateFreeCycles(
        targetMinutes = parsedMin ?: -1,
        targetSeconds = parsedSec ?: -1,
        contractionSec = parsedCon ?: -1,
        relaxationSec = parsedRel ?: -1
    )

    val fieldError = nameError ?: minutesError ?: secondsError ?: contractionError ?: relaxationError
    val isFormValid = fieldError == null && calc.isValid

    val hasUnsavedChanges = nameInput.trim() != template.name ||
        minutesInput.trim() != template.targetDurationMinutes.toString() ||
        secondsInput.trim() != template.targetDurationSeconds.toString() ||
        contractionInput.trim() != template.contractionSec.toString() ||
        relaxationInput.trim() != template.relaxationSec.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .border(1.5.dp, palette.primaryAccent, RoundedCornerShape(22.dp))
            .testTag("free_template_editor_dialog"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Настройка шаблона ${template.slotIndex + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    showUnsavedConfirm = false
                },
                label = { Text("Название шаблона") },
                isError = nameError != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_template_name_input")
            )

            Text(
                text = "Желаемая продолжительность (до 90 минут):",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = {
                        minutesInput = it.filter { ch -> ch.isDigit() }.take(3)
                        showUnsavedConfirm = false
                    },
                    label = { Text("Минуты (0–90)") },
                    isError = minutesError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_template_minutes_input")
                )
                OutlinedTextField(
                    value = secondsInput,
                    onValueChange = {
                        secondsInput = it.filter { ch -> ch.isDigit() }.take(2)
                        showUnsavedConfirm = false
                    },
                    label = { Text("Секунды (0–59)") },
                    isError = secondsError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_template_seconds_input")
                )
            }

            Text(
                text = "Интервалы одного цикла (в секундах):",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = contractionInput,
                    onValueChange = {
                        contractionInput = it.filter { ch -> ch.isDigit() }.take(3)
                        showUnsavedConfirm = false
                    },
                    label = { Text("Удержание (с)") },
                    isError = contractionError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_template_contraction_input")
                )
                OutlinedTextField(
                    value = relaxationInput,
                    onValueChange = {
                        relaxationInput = it.filter { ch -> ch.isDigit() }.take(3)
                        showUnsavedConfirm = false
                    },
                    label = { Text("Отдых (с)") },
                    isError = relaxationError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_template_relaxation_input")
                )
            }

            // Понятный блок расчёта полных циклов или сообщения об ошибке
            val displayMessage = fieldError ?: calc.summaryRu
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isFormValid) {
                    palette.primaryAccent.copy(alpha = 0.16f)
                } else {
                    palette.errorAccent.copy(alpha = 0.18f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isFormValid) "free_template_calc_preview" else "free_template_validation_error")
            ) {
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFormValid) palette.secondaryAccent else palette.errorAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (showUnsavedConfirm && hasUnsavedChanges) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.warningAccent.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unsaved_changes_confirm_bar")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "В шаблоне есть несохранённые изменения. Закрыть без сохранения?",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.warningAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showUnsavedConfirm = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_keep_editing_template")
                            ) {
                                Text("Продолжить", color = palette.textPrimary)
                            }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.errorAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_discard_unsaved_template")
                            ) {
                                Text("Не сохранять", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showUnsavedConfirm = true
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("btn_cancel_free_template")
                    ) {
                        Text("Отмена", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (isFormValid && parsedMin != null && parsedSec != null && parsedCon != null && parsedRel != null) {
                                onSave(
                                    template.copy(
                                        name = nameInput.trim(),
                                        targetDurationMinutes = parsedMin,
                                        targetDurationSeconds = parsedSec,
                                        contractionSec = parsedCon,
                                        relaxationSec = parsedRel,
                                        isConfigured = true,
                                        updatedAtEpochMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryAccent,
                            contentColor = palette.onPrimaryAccent,
                            disabledContainerColor = palette.disabledContainer,
                            disabledContentColor = palette.disabledContent
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 50.dp)
                            .testTag("btn_save_free_template")
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
