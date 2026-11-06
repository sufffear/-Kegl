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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BenchSignalSettings
import com.example.data.DailySlotEntity
import com.example.data.DailySlotWithCompletion
import com.example.data.FreeIntervalTemplateEntity
import com.example.data.StoryReaderUiState
import com.example.data.StreakMilestoneInfo
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

private fun resolveWorkoutSubtitleRu(slot: DailySlotEntity): String {
    val cSec = (slot.contractionMs / 1000L).toInt()
    return when {
        cSec <= 2 && slot.repetitions >= 50 -> "Тренировка быстрых мышц · Быстрые сжимания"
        cSec in 8..15 -> "Медленная тренировка · Контроль тонуса"
        cSec >= 20 -> "Очень медленные сжимания · Глубокое удержание"
        else -> "Интервальная сессия по расписанию"
    }
}

/**
 * Экран «Главная»:
 * Показывает только:
 * 1) Карточку «Активная сессия» (прогресс Утро / Обед / Вечер и быстрый запуск следующей сессии);
 * 2) Сводную статистику и красивый 7-дневный недельный обзор.
 */
@Composable
fun VelarisHomeScreen(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    dailySlots: List<DailySlotWithCompletion>,
    freeTemplates: List<FreeIntervalTemplateEntity>,
    weeklyOverview: WeeklyOverviewState,
    storyState: StoryReaderUiState = StoryReaderUiState(),
    onOpenWorkout: () -> Unit,
    onStartNextSlot: (DailySlotWithCompletion) -> Unit,
    onStartFreeTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReader: () -> Unit = {},
    onSelectOverviewDay: (String) -> Unit,
    onToggleFreezeDate: (String) -> Unit = {},
    onStartStreakRescueBlitz: () -> Unit = {},
    celebratedMilestone: StreakMilestoneInfo? = null,
    onTriggerMilestoneSalute: (StreakMilestoneInfo) -> Unit = {},
    onDismissMilestoneSalute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val totalSlots = dailySlots.size
    val completedSlots = dailySlots.count { it.isCompleted }
    val nextAvailableSlot = dailySlots.firstOrNull { !it.isCompleted }
    val firstConfiguredTemplate = freeTemplates.firstOrNull { it.isValid }

    val groupedByPeriod = remember(dailySlots) {
        dailySlots.groupBy { PlanGroup.fromCode(it.slot.groupCode) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Карточка «Активная сессия»
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (snapshot.isActiveOrPaused) 1.5.dp else 1.dp,
                    color = if (snapshot.isActiveOrPaused) palette.primaryAccent.copy(alpha = 0.65f) else palette.primaryAccent.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag("home_workout_card"),
            shape = RoundedCornerShape(20.dp),
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
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Шапка карточки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.primaryAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = palette.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Активная сессия",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        when {
                            snapshot.isRunningOrPreparing -> {
                                StatusPillBadge(
                                    text = "В процессе",
                                    accentColor = palette.primaryAccent
                                )
                            }
                            snapshot.isActiveOrPaused -> {
                                StatusPillBadge(
                                    text = "На паузе",
                                    accentColor = palette.phasePauseAccent
                                )
                            }
                            totalSlots > 0 -> {
                                StatusPillBadge(
                                    text = "$completedSlots из $totalSlots за день",
                                    accentColor = if (completedSlots == totalSlots) palette.successAccent else palette.secondaryAccent
                                )
                            }
                        }
                    }

                    // Компактная полоска прогресса по периодам дня: Утро · Обед · Вечер
                    if (totalSlots > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlanGroup.entries.sortedBy { it.order }.forEach { group ->
                                val items = groupedByPeriod[group].orEmpty()
                                val doneCount = items.count { it.isCompleted }
                                val isAllDone = items.isNotEmpty() && doneCount == items.size
                                val isCurrentGroup = nextAvailableSlot?.let {
                                    PlanGroup.fromCode(it.slot.groupCode) == group
                                } ?: false

                                val pillBg = when {
                                    isAllDone -> palette.successAccent.copy(alpha = 0.20f)
                                    isCurrentGroup -> palette.primaryAccent.copy(alpha = 0.18f)
                                    else -> palette.surfaceElevated.copy(alpha = 0.65f)
                                }
                                val pillBorder = when {
                                    isAllDone -> palette.successAccent.copy(alpha = 0.65f)
                                    isCurrentGroup -> palette.primaryAccent.copy(alpha = 0.50f)
                                    else -> palette.outline.copy(alpha = 0.25f)
                                }
                                val textColor = when {
                                    isAllDone -> palette.successAccent
                                    isCurrentGroup -> palette.primaryAccent
                                    else -> palette.textSecondary
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(pillBg)
                                        .border(1.dp, pillBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.titleRu,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (isAllDone) "✓ ${doneCount}/${items.size}" else "${doneCount}/${items.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    when {
                        snapshot.isActiveOrPaused -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = snapshot.protocol.titleRu,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        WorkoutMetricMiniTile(
                                            label = "Статус",
                                            value = snapshot.headlineCommandRu,
                                            accent = palette.primaryAccent,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        WorkoutMetricMiniTile(
                                            label = "Цикл",
                                            value = "${snapshot.currentRepetition}/${snapshot.totalRepetitions}",
                                            accent = palette.secondaryAccent,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                        WorkoutMetricMiniTile(
                                            label = "Осталось",
                                            value = formatSecondsMinSec(((snapshot.remainingActiveMs + 999L) / 1000L).toInt()),
                                            accent = palette.textPrimary,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onOpenWorkout,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("btn_home_primary_workout")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Вернуться к сессии",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        totalSlots == 0 -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.82f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("zero_slots_calm_message")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Сегодня без тренировок",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "По расписанию назначен день отдыха.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (firstConfiguredTemplate != null) {
                                        onStartFreeTemplate(firstConfiguredTemplate)
                                    } else {
                                        onOpenPlan()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("btn_home_primary_workout")
                            ) {
                                Text(
                                    text = if (firstConfiguredTemplate != null) {
                                        "Начать «${firstConfiguredTemplate.name}»"
                                    } else {
                                        "Открыть тренировки"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        else -> {
                            if (nextAvailableSlot != null) {
                                val nextGroup = PlanGroup.fromCode(nextAvailableSlot.slot.groupCode)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.surfaceElevated.copy(alpha = 0.85f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${nextGroup.titleRu} · ${nextAvailableSlot.slot.titleRu}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val totalSec = ((nextAvailableSlot.slot.contractionMs + nextAvailableSlot.slot.relaxationMs) * nextAvailableSlot.slot.repetitions / 1000L).toInt()
                                            Text(
                                                text = formatSecondsMinSec(totalSec),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.secondaryAccent,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            WorkoutMetricMiniTile(
                                                label = "Сжатие",
                                                value = "${nextAvailableSlot.slot.contractionMs / 1000} сек",
                                                accent = palette.primaryAccent,
                                                modifier = Modifier.weight(1f)
                                            )
                                            WorkoutMetricMiniTile(
                                                label = "Отдых",
                                                value = "${nextAvailableSlot.slot.relaxationMs / 1000} сек",
                                                accent = palette.secondaryAccent,
                                                modifier = Modifier.weight(1f)
                                            )
                                            WorkoutMetricMiniTile(
                                                label = "Сжиманий",
                                                value = "${nextAvailableSlot.slot.repetitions}",
                                                accent = palette.textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.successAccent.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.successAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.successAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Все тренировки на сегодня выполнены",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.successAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Утро, Обед и Вечер полностью закрыты.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (nextAvailableSlot != null) {
                                            onStartNextSlot(nextAvailableSlot)
                                        } else {
                                            onOpenPlan()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_home_primary_workout")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (nextAvailableSlot != null) "Запустить" else "К расписанию",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = onOpenPlan,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_home_open_plan")
                                ) {
                                    Text(
                                        text = "Все сессии",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Календарь «Цепочка стабильности», мотивация и Недельный/Месячный обзор
        WeeklyOverviewCard(
            overview = weeklyOverview,
            isWorkoutActiveOrPaused = snapshot.isActiveOrPaused,
            onSelectDay = onSelectOverviewDay,
            onToggleFreezeDate = onToggleFreezeDate,
            onStartStreakRescueBlitz = onStartStreakRescueBlitz,
            celebratedMilestone = celebratedMilestone,
            onTriggerMilestoneSalute = onTriggerMilestoneSalute,
            onDismissMilestoneSalute = onDismissMilestoneSalute
        )
    }
}

@Composable
private fun StatusPillBadge(
    text: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.16f),
        modifier = Modifier.border(
            1.dp,
            accentColor.copy(alpha = 0.45f),
            RoundedCornerShape(999.dp)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun WorkoutMetricMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface.copy(alpha = 0.82f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatHoldDurationCompactRu(holdSeconds: Int): String {
    if (holdSeconds <= 0) return "0 с"
    val min = holdSeconds / 60
    val sec = holdSeconds % 60
    return when {
        min == 0 -> "${sec} с"
        sec == 0 -> "${min} мин"
        else -> "${min}м ${sec}с"
    }
}

/**
 * Календарь «Цепочка стабильности» и 7/28-дневный обзор:
 * - Живая серия дней (Streak) + Рекорд + Кристаллы заморозки (❄️ до 3 шт.);
 * - Коллекционная витрина «Жетоны рубежей» с тактильным салютом и фанфарой (3 → 7 → 14 → 21 → 30 → 60 → 100 дн.);
 * - Экстренный микро-протокол «⚡ Блиц-спасение серии за 60 сек» (30 сжиманий 1с/1с);
 * - Сравнение «Эта неделя против прошлой» (дельта прогресса в % и секундах чистого сжатия);
 * - Переключатель «Неделя (7 дн.)» ↔ «Тепловая карта (28 дн.)» с 4 уровнями свечения и заморозкой пропусков.
 */
@Composable
fun WeeklyOverviewCard(
    overview: WeeklyOverviewState,
    isWorkoutActiveOrPaused: Boolean = false,
    onSelectDay: (String) -> Unit,
    onToggleFreezeDate: (String) -> Unit = {},
    onStartStreakRescueBlitz: () -> Unit = {},
    celebratedMilestone: StreakMilestoneInfo? = null,
    onTriggerMilestoneSalute: (StreakMilestoneInfo) -> Unit = {},
    onDismissMilestoneSalute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    var showMonthHeatmap by rememberSaveable { mutableStateOf(false) }
    var inspectedMilestoneDays by rememberSaveable { mutableStateOf<Int?>(null) }

    val selectedSummary: WeeklyDaySummary? = overview.selectedDaySummary
    val activeDaysCount = overview.days.count { it.completedCount > 0 }
    val maxDayCount = overview.days.maxOfOrNull { it.completedCount }?.coerceAtLeast(1) ?: 1

    val activeSaluteMilestone: StreakMilestoneInfo? = celebratedMilestone
        ?: overview.milestones.firstOrNull { it.targetDays == inspectedMilestoneDays }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.outline.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
            .testTag("weekly_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Шапка карточки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Цепочка стабильности",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Серия, заморозка и тепловая карта тонуса",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                StatusPillBadge(
                    text = "$activeDaysCount/7 дн.",
                    accentColor = palette.primaryAccent
                )
            }

            // 1. Мотивационный блок «Серия + Щиты заморозки + Ранг»
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surfaceElevated.copy(alpha = 0.88f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (overview.isTodayActiveInStreak) {
                            palette.successAccent.copy(alpha = 0.45f)
                        } else if (overview.isStreakAtRiskToday) {
                            palette.phasePauseAccent.copy(alpha = 0.55f)
                        } else {
                            palette.primaryAccent.copy(alpha = 0.28f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("streak_motivation_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    palette.heroGradientStart.copy(alpha = 0.75f),
                                    palette.heroGradientEnd.copy(alpha = 0.55f)
                                )
                            )
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Слева: Огонь серии и рекорд
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val streakBadgeColor = when {
                                overview.isTodayActiveInStreak -> palette.successAccent
                                overview.isStreakAtRiskToday -> palette.phasePauseAccent
                                else -> palette.primaryAccent
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(streakBadgeColor.copy(alpha = 0.16f))
                                    .border(1.dp, streakBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (overview.isTodayActiveInStreak) "🔥" else if (overview.currentStreakDays > 0) "⚡" else "🌱",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Серия: ${overview.currentStreakDays} дн.",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.testTag("streak_current_days_text")
                                    )
                                    if (overview.bestStreakDays > 0) {
                                        Text(
                                            text = "· Рекорд ${overview.bestStreakDays}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryAccent,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Text(
                                    text = "Ранг: ${overview.rankTitleRu}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Справа: Кристаллы заморозки серии
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surface.copy(alpha = 0.90f),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    palette.secondaryAccent.copy(alpha = 0.38f),
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("streak_freeze_shields_badge")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "❄️ ${overview.availableFreezeShields}/${overview.maxFreezeShields}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Заморозка",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Прогресс-бар до следующего рубежа серии
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("streak_milestone_progress_row")
                    ) {
                        val remainingToMilestone = (overview.nextMilestoneDays - overview.currentStreakDays).coerceAtLeast(1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Следующий титул: «${overview.nextMilestoneTitleRu}» (${overview.nextMilestoneDays} дн.)",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ещё $remainingToMilestone дн.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primaryAccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.surface)
                        ) {
                            val barFill = overview.milestoneProgressFraction.coerceIn(0.06f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barFill)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(palette.primaryAccent, palette.successAccent)
                                        )
                                    )
                            )
                        }
                    }

                    // Коллекционная витрина «Жетоны рубежей» (7 жетонов от 3 до 100 дней)
                    if (overview.milestones.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("milestone_tokens_showcase_section")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Витрина жетонов рубежей",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val unlockedCount = overview.milestones.count { it.isUnlocked }
                                Text(
                                    text = "Открыто: $unlockedCount/${overview.milestones.size} (нажмите для салюта)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryAccent,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("milestone_tokens_showcase_row"),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                overview.milestones.forEach { badge ->
                                    val isInspected = activeSaluteMilestone?.targetDays == badge.targetDays
                                    val tokenBg = when {
                                        badge.isUnlocked -> palette.successAccent.copy(alpha = 0.18f)
                                        badge.isCurrentTarget -> palette.primaryAccent.copy(alpha = 0.14f)
                                        else -> palette.surface.copy(alpha = 0.72f)
                                    }
                                    val tokenBorder = when {
                                        isInspected -> palette.selectedBorder
                                        badge.isUnlocked -> palette.successAccent.copy(alpha = 0.65f)
                                        badge.isCurrentTarget -> palette.primaryAccent.copy(alpha = 0.50f)
                                        else -> palette.outline.copy(alpha = 0.22f)
                                    }
                                    val labelColor = when {
                                        badge.isUnlocked -> palette.successAccent
                                        badge.isCurrentTarget -> palette.primaryAccent
                                        else -> palette.textSecondary
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tokenBg)
                                            .border(
                                                width = if (isInspected || badge.isUnlocked) 1.5.dp else 1.dp,
                                                color = tokenBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                inspectedMilestoneDays = badge.targetDays
                                                onTriggerMilestoneSalute(badge)
                                            }
                                            .padding(vertical = 5.dp, horizontal = 2.dp)
                                            .testTag("milestone_token_${badge.targetDays}"),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = badge.badgeEmoji,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (badge.isUnlocked) "✓${badge.targetDays}д" else "${badge.targetDays}д",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = labelColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Карточка выбранного жетона и Тактильного салюта рубежа
                    if (activeSaluteMilestone != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (activeSaluteMilestone.isUnlocked) {
                                palette.successAccent.copy(alpha = 0.14f)
                            } else {
                                palette.surface.copy(alpha = 0.92f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (activeSaluteMilestone.isUnlocked) {
                                        palette.successAccent.copy(alpha = 0.55f)
                                    } else {
                                        palette.primaryAccent.copy(alpha = 0.40f)
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("milestone_salute_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${activeSaluteMilestone.badgeEmoji} Жетон «${activeSaluteMilestone.titleRu}» (${activeSaluteMilestone.targetDays} дн.)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusPillBadge(
                                        text = if (activeSaluteMilestone.isUnlocked) "Получен ✓" else "Рубеж",
                                        accentColor = if (activeSaluteMilestone.isUnlocked) palette.successAccent else palette.primaryAccent
                                    )
                                }

                                if (activeSaluteMilestone.descriptionRu.isNotBlank()) {
                                    Text(
                                        text = activeSaluteMilestone.descriptionRu,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onTriggerMilestoneSalute(activeSaluteMilestone) },
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .heightIn(min = 34.dp)
                                            .testTag("btn_play_milestone_salute")
                                    ) {
                                        Text(
                                            text = "🎉 Фанфара и салют",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.primaryAccent,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            inspectedMilestoneDays = null
                                            onDismissMilestoneSalute()
                                        },
                                        shape = RoundedCornerShape(9.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .heightIn(min = 34.dp)
                                            .testTag("btn_close_milestone_salute")
                                    ) {
                                        Text(
                                            text = "Скрыть",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ежедневный квест / Микро-цель дня + Экстренный 60-секундный Блиц-протокол спасения серии
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surface.copy(alpha = 0.88f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.outline.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                            .testTag("daily_motivation_quest_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = overview.dailyQuestTitleRu,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (overview.dailyQuestProgress >= 1f) palette.successAccent else palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${(overview.dailyQuestProgress * 100).toInt().coerceIn(0, 100)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (overview.dailyQuestProgress >= 1f) palette.successAccent else palette.primaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = overview.dailyQuestSubtitleRu,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Кнопка «⚡ Блиц-спасение серии за 60 секунд» (доступна, когда нет активного таймера)
                            if (!isWorkoutActiveOrPaused) {
                                val isUrgentRescue = !overview.isTodayActiveInStreak
                                Button(
                                    onClick = onStartStreakRescueBlitz,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isUrgentRescue) {
                                            palette.primaryAccent
                                        } else {
                                            palette.secondaryAccent.copy(alpha = 0.16f)
                                        },
                                        contentColor = if (isUrgentRescue) {
                                            palette.onPrimaryAccent
                                        } else {
                                            palette.secondaryAccent
                                        }
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 38.dp)
                                        .testTag("btn_streak_rescue_blitz")
                                ) {
                                    Text(
                                        text = if (isUrgentRescue) {
                                            "⚡ Блиц-спасение серии за 60 сек (30×1с)"
                                        } else {
                                            "⚡ Быстрый блиц-подход +60 сек в копилку"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. 4 компактные KPI-плитки статистики (включая Чистое сжатие и Идеальные дни)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatKpiTile(
                    valueText = overview.totalRealCompletionsInWeek.toString(),
                    labelText = "За 7 дней",
                    accentColor = palette.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
                StatKpiTile(
                    valueText = formatHoldDurationCompactRu(
                        if (overview.totalHoldSecondsInWeek > 0) overview.totalHoldSecondsInWeek else overview.totalHoldSecondsAllTime
                    ),
                    labelText = "Чистое сжатие",
                    accentColor = palette.secondaryAccent,
                    modifier = Modifier.weight(1f)
                )
                StatKpiTile(
                    valueText = "${overview.perfectDaysInMonth}/28",
                    labelText = "Сильных дн.",
                    accentColor = palette.successAccent,
                    modifier = Modifier.weight(1f)
                )
                StatKpiTile(
                    valueText = overview.totalRealCompletionsAllTime.toString(),
                    labelText = "За всё время",
                    accentColor = palette.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2Б. Сравнение «Эта неделя против прошлой» (Дельта прогресса в % и секундах)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceElevated.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (overview.weekHoldDeltaSeconds >= 0 && overview.totalHoldSecondsInWeek > 0) {
                            palette.successAccent.copy(alpha = 0.38f)
                        } else {
                            palette.outline.copy(alpha = 0.25f)
                        },
                        RoundedCornerShape(14.dp)
                    )
                    .testTag("week_vs_prev_week_banner")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Эта неделя против прошлой",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val deltaColor = when {
                            overview.totalHoldSecondsInWeek > 0 && overview.weekHoldDeltaSeconds >= 0 -> palette.successAccent
                            overview.weekHoldDeltaSeconds < 0 -> palette.secondaryAccent
                            else -> palette.primaryAccent
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = deltaColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .border(1.dp, deltaColor.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                                .testTag("week_delta_badge")
                        ) {
                            Text(
                                text = overview.weekDeltaBadgeTextRu,
                                style = MaterialTheme.typography.labelSmall,
                                color = deltaColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Сравнительная шкала 2 недель
                    val maxHoldComparison = maxOf(overview.totalHoldSecondsInWeek, overview.previousWeekHoldSeconds, 60)
                    val curWeekFraction = (overview.totalHoldSecondsInWeek.toFloat() / maxHoldComparison.toFloat()).coerceIn(0.06f, 1f)
                    val prevWeekFraction = (overview.previousWeekHoldSeconds.toFloat() / maxHoldComparison.toFloat()).coerceIn(0.06f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Эта",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(48.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(curWeekFraction)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(palette.primaryAccent, palette.successAccent)
                                            )
                                        )
                                )
                            }
                            Text(
                                text = formatHoldDurationCompactRu(overview.totalHoldSecondsInWeek),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.successAccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Прошл.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                modifier = Modifier.width(48.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(prevWeekFraction)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(palette.secondaryAccent.copy(alpha = 0.55f))
                                )
                            }
                            Text(
                                text = formatHoldDurationCompactRu(overview.previousWeekHoldSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text(
                        text = overview.weekDeltaSubtitleRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!overview.hasAnyRealData) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_overview_empty_state")
                ) {
                    Text(
                        text = "Завершите любую тренировку — цепочка стабильности и тепловая карта зажгутся автоматически.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 3. Переключатель вида календаря: «Неделя (7 дн.)» ↔ «Тепловая карта (28 дн.)»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceElevated)
                    .padding(3.dp)
                    .testTag("calendar_mode_toggle_row"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isWeekMode = !showMonthHeatmap
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isWeekMode) palette.surface else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { showMonthHeatmap = false }
                        .testTag("calendar_mode_week")
                ) {
                    Text(
                        text = "Неделя · 7 дн.",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isWeekMode) palette.primaryAccent else palette.textSecondary,
                        fontWeight = if (isWeekMode) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (showMonthHeatmap) palette.surface else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { showMonthHeatmap = true }
                        .testTag("calendar_mode_month")
                ) {
                    Text(
                        text = "Тепловая карта · 28 дн.",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (showMonthHeatmap) palette.primaryAccent else palette.textSecondary,
                        fontWeight = if (showMonthHeatmap) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                    )
                }
            }

            if (!showMonthHeatmap) {
                // 7-дневная гистограмма активности с индикаторами этапов и заморозки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_days_row"),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    overview.days.forEach { day ->
                        val isSelected = day.localDate == selectedSummary?.localDate
                        val hasCompletions = day.completedCount > 0
                        val barFraction = when {
                            hasCompletions -> (day.completedCount.toFloat() / maxDayCount.toFloat()).coerceIn(0.28f, 1f)
                            day.isFrozen -> 0.45f
                            else -> 0.12f
                        }

                        val cellBg = when {
                            isSelected -> palette.selectedContainer
                            day.isFrozen -> palette.secondaryAccent.copy(alpha = 0.12f)
                            hasCompletions -> palette.surfaceElevated
                            else -> palette.surfaceElevated.copy(alpha = 0.65f)
                        }
                        val borderColor = when {
                            isSelected -> palette.selectedBorder
                            day.isToday -> palette.secondaryAccent.copy(alpha = 0.70f)
                            day.isFrozen -> palette.secondaryAccent.copy(alpha = 0.50f)
                            hasCompletions -> palette.successAccent.copy(alpha = 0.40f)
                            else -> palette.outline.copy(alpha = 0.22f)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cellBg)
                                .border(
                                    width = if (isSelected || day.isToday) 1.5.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectDay(day.localDate) }
                                .padding(vertical = 6.dp, horizontal = 2.dp)
                                .semantics {
                                    contentDescription = "${day.shortLabelRu} ${day.dayOfMonthLabel}: выполнено ${day.completedCount}"
                                }
                                .testTag("weekly_day_${day.localDate}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = day.shortLabelRu,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) palette.primaryAccent else palette.textSecondary,
                                fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )

                            Text(
                                text = day.dayOfMonthLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )

                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.backgroundTop.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((28f * barFraction).dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            when {
                                                hasCompletions && isSelected -> Brush.verticalGradient(
                                                    listOf(palette.secondaryAccent, palette.primaryAccent)
                                                )
                                                hasCompletions -> Brush.verticalGradient(
                                                    listOf(palette.primaryAccent, palette.successAccent)
                                                )
                                                day.isFrozen -> Brush.verticalGradient(
                                                    listOf(
                                                        palette.secondaryAccent.copy(alpha = 0.75f),
                                                        palette.primaryAccent.copy(alpha = 0.55f)
                                                    )
                                                )
                                                day.isToday -> Brush.verticalGradient(
                                                    listOf(
                                                        palette.secondaryAccent.copy(alpha = 0.45f),
                                                        palette.secondaryAccent.copy(alpha = 0.45f)
                                                    )
                                                )
                                                else -> Brush.verticalGradient(
                                                    listOf(
                                                        palette.outline.copy(alpha = 0.25f),
                                                        palette.outline.copy(alpha = 0.25f)
                                                    )
                                                )
                                            }
                                        )
                                )
                            }

                            Text(
                                text = when {
                                    hasCompletions && day.intensityLevel >= 3 -> "${day.completedCount}★"
                                    hasCompletions -> "${day.completedCount}"
                                    day.isFrozen -> "❄️"
                                    else -> "·"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    hasCompletions -> palette.successAccent
                                    day.isFrozen -> palette.secondaryAccent
                                    else -> palette.textSecondary
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            } else {
                // 28-дневная тепловая матрица «Цепочка стабильности» (4 недели × 7 дней)
                val weeks = remember(overview.heatmapDays) {
                    overview.heatmapDays.chunked(7)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stability_heatmap_grid"),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    weeks.forEachIndexed { weekIndex, weekDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            weekDays.forEach { day ->
                                val isSelected = day.localDate == selectedSummary?.localDate
                                val cellColor = when {
                                    day.intensityLevel >= 3 -> palette.successAccent.copy(alpha = 0.28f)
                                    day.intensityLevel == 2 -> palette.successAccent.copy(alpha = 0.18f)
                                    day.intensityLevel == 1 -> palette.primaryAccent.copy(alpha = 0.14f)
                                    day.isFrozen -> palette.secondaryAccent.copy(alpha = 0.15f)
                                    else -> palette.surfaceElevated.copy(alpha = 0.65f)
                                }
                                val cellBorder = when {
                                    isSelected -> palette.selectedBorder
                                    day.isToday -> palette.primaryAccent
                                    day.intensityLevel >= 3 -> palette.successAccent.copy(alpha = 0.70f)
                                    day.intensityLevel == 2 -> palette.successAccent.copy(alpha = 0.45f)
                                    day.intensityLevel == 1 -> palette.primaryAccent.copy(alpha = 0.38f)
                                    day.isFrozen -> palette.secondaryAccent.copy(alpha = 0.55f)
                                    else -> palette.outline.copy(alpha = 0.20f)
                                }
                                val badgeLabel = when {
                                    day.intensityLevel >= 3 -> "★${day.completedCount}"
                                    day.completedCount > 0 -> "${day.completedCount}"
                                    day.isFrozen -> "❄️"
                                    else -> "·"
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellColor)
                                        .border(
                                            width = if (isSelected || day.isToday) 1.5.dp else 1.dp,
                                            color = cellBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectDay(day.localDate) }
                                        .padding(vertical = 5.dp, horizontal = 2.dp)
                                        .testTag("heatmap_day_${day.localDate}"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    if (weekIndex == 0) {
                                        Text(
                                            text = day.shortLabelRu,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Text(
                                        text = day.dayOfMonthLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textPrimary,
                                        fontWeight = if (isSelected || day.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            day.intensityLevel >= 2 -> palette.successAccent
                                            day.intensityLevel == 1 -> palette.primaryAccent
                                            day.isFrozen -> palette.secondaryAccent
                                            else -> palette.textSecondary
                                        },
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Компактная легенда уровней тепловой карты
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Уровни дня: · 0   1–2 старт   3–5 этап   ★ 6–9 идеал   ❄️ щит",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Детальная карточка выбранного дня + Управление Щитом Заморозки
            if (selectedSummary != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.outline.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .testTag("weekly_selected_day_details")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dateSuffix = if (selectedSummary.monthShortRu.isNotBlank()) {
                            "${selectedSummary.dayOfMonthLabel} ${selectedSummary.monthShortRu}"
                        } else {
                            selectedSummary.dayOfMonthLabel
                        }
                        val dayHeader = if (selectedSummary.isToday) {
                            "Сегодня (${selectedSummary.shortLabelRu}, $dateSuffix)"
                        } else {
                            "${selectedSummary.shortLabelRu}, $dateSuffix"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayHeader,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            val statusColor = when {
                                selectedSummary.completedCount > 0 -> palette.successAccent
                                selectedSummary.isFrozen -> palette.secondaryAccent
                                else -> palette.textSecondary
                            }
                            Text(
                                text = when {
                                    selectedSummary.completedCount > 0 -> "Сессий: ${selectedSummary.completedCount}"
                                    selectedSummary.isFrozen -> "❄️ Заморожен"
                                    else -> "Без записей"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (selectedSummary.completedCount == 0) {
                            Text(
                                text = if (selectedSummary.isFrozen) {
                                    "В этот день тренировок не было, но серия защищена кристаллом заморозки ❄️."
                                } else {
                                    "В выбранный день завершённых тренировок не было."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            // Кнопка ручного включения/снятия щита заморозки для пропущенного прошлого дня
                            if (!selectedSummary.isToday && (selectedSummary.isFrozen || overview.availableFreezeShields > 0)) {
                                OutlinedButton(
                                    onClick = { onToggleFreezeDate(selectedSummary.localDate) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 38.dp)
                                        .testTag("btn_toggle_freeze_day")
                                ) {
                                    Text(
                                        text = if (selectedSummary.isFrozen) {
                                            "Снять щит заморозки с этого дня"
                                        } else {
                                            "❄️ Защитить пропуск щитом (${overview.availableFreezeShields} дост.)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "По расписанию: ${selectedSummary.plannedCompletedCount} · Дополнительно: ${selectedSummary.freeCompletedCount} · Сжатие: ${formatHoldDurationCompactRu(selectedSummary.holdSecondsTotal)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("weekly_selected_day_breakdown")
                            )

                            if (selectedSummary.completedTitlesRu.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.testTag("weekly_completed_titles_list")
                                ) {
                                    selectedSummary.completedTitlesRu.forEach { title ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = palette.successAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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

@Composable
private fun StatKpiTile(
    valueText: String,
    labelText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        modifier = modifier.border(
            1.dp,
            accentColor.copy(alpha = 0.25f),
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Экран «Тренировка»:
 * - Три горизонтальные вкладки на самом верху: «Утро», «Обед», «Вечер».
 * - Когда все 3 тренировки во вкладке «Утро» выполнены, вкладка «Утро» загорается зелёным
 *   и экран автоматически переключается на «Обед» (а после «Обеда» — на «Вечер»).
 * - После закрытия и повторного открытия приложения автоматически открывается текущая
 *   незавершённая вкладка («Обед», если «Утро» уже выполнено).
 * - В каждой вкладке («Утро», «Обед», «Вечер») ровно 3 тренировки:
 *   1) Быстрая тренировка (100 сжиманий: 1 с сжатие / 1 с отдых);
 *   2) Силовой контроль / Медленная тренировка (10 сжиманий: 10 с сжатие / 5 с отдых);
 *   3) Выносливость / Очень медленные сжимания (10 сжиманий: 25 с удержание / 10 с отдых).
 */
@Composable
fun VelarisPlanScreen(
    snapshot: PhaseEngineSnapshot,
    settings: BenchSignalSettings,
    dailySlots: List<DailySlotWithCompletion>,
    freeTemplates: List<FreeIntervalTemplateEntity>,
    customPrograms: List<com.example.domain.CustomWorkoutProgram> = emptyList(),
    onStartSlot: (DailySlotWithCompletion) -> Unit,
    onSelectSlot: (DailySlotWithCompletion) -> Unit,
    onStartFreeTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onSaveFreeTemplate: (FreeIntervalTemplateEntity) -> Unit,
    onStartCustomProgram: (com.example.domain.CustomWorkoutProgram) -> Unit = {},
    onSaveCustomProgram: (com.example.domain.CustomWorkoutProgram) -> Unit = {},
    onDeleteCustomProgram: (String) -> Unit = {},
    onDuplicateCustomProgram: (com.example.domain.CustomWorkoutProgram) -> Unit = {},
    onReorderCustomPrograms: (List<com.example.domain.CustomWorkoutProgram>) -> Unit = {},
    onReturnToWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val animatorsEnabled = remember {
        runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
    }

    val groupedSlots = remember(dailySlots) {
        dailySlots.groupBy { PlanGroup.fromCode(it.slot.groupCode) }
    }
    val morningSlots = groupedSlots[PlanGroup.MORNING].orEmpty()
    val noonSlots = groupedSlots[PlanGroup.NOON].orEmpty()
    val eveningSlots = groupedSlots[PlanGroup.EVENING].orEmpty()

    val isMorningCompleted = morningSlots.isNotEmpty() && morningSlots.all { it.isCompleted }
    val isNoonCompleted = noonSlots.isNotEmpty() && noonSlots.all { it.isCompleted }
    val isEveningCompleted = eveningSlots.isNotEmpty() && eveningSlots.all { it.isCompleted }

    // Автоматический расчёт целевой вкладки:
    // пока Утро не закрыто -> Утро; когда Утро закрыто -> Обед; когда Обед закрыт -> Вечер.
    val targetAutoGroup = when {
        !isMorningCompleted -> PlanGroup.MORNING
        !isNoonCompleted -> PlanGroup.NOON
        else -> PlanGroup.EVENING
    }

    var selectedGroupCode by rememberSaveable { mutableStateOf(targetAutoGroup.code) }
    var lastAutoTargetGroupCode by rememberSaveable { mutableStateOf("") }

    // Автоматический переход при завершении всех сессий вкладки и при входе в приложение после закрытия
    LaunchedEffect(targetAutoGroup.code, dailySlots.isNotEmpty()) {
        if (dailySlots.isNotEmpty() && targetAutoGroup.code != lastAutoTargetGroupCode) {
            selectedGroupCode = targetAutoGroup.code
            lastAutoTargetGroupCode = targetAutoGroup.code
        }
    }

    var selectedMode by rememberSaveable { mutableStateOf("scheduled") }
    var editingSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val editingTemplate = remember(editingSlotIndex, freeTemplates) {
        editingSlotIndex?.let { idx -> freeTemplates.firstOrNull { it.slotIndex == idx } }
    }

    val activeGroup = PlanGroup.fromCode(selectedGroupCode)
    val activeGroupSlots = groupedSlots[activeGroup].orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedMode == "scheduled") {
            val totalSlots = dailySlots.size
            val totalCompleted = dailySlots.count { it.isCompleted }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                    .testTag("daily_plan_persistence_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок карточки расписания
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ежедневная программа",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Три этапа в день по 3 тренировки",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (totalSlots > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPillBadge(
                                text = "$totalCompleted / $totalSlots",
                                accentColor = if (totalCompleted == totalSlots) palette.successAccent else palette.primaryAccent
                            )
                        }
                    }

                    // ТРИ ГОРИЗОНТАЛЬНЫЕ ВКЛАДКИ: УТРО | ОБЕД | ВЕЧЕР
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.backgroundTop.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .testTag("plan_group_tabs_row")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PlanGroup.entries.sortedBy { it.order }.forEach { group ->
                                val groupItems = groupedSlots[group].orEmpty()
                                val groupCompletedCount = groupItems.count { it.isCompleted }
                                val isGroupDone = groupItems.isNotEmpty() && groupCompletedCount == groupItems.size
                                val isSelected = activeGroup == group

                                PeriodHorizontalTab(
                                    group = group,
                                    completedCount = groupCompletedCount,
                                    totalCount = groupItems.size,
                                    isCompleted = isGroupDone,
                                    isSelected = isSelected,
                                    onClick = { selectedGroupCode = group.code },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (totalSlots == 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("zero_slots_calm_message")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Сегодня без тренировок",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.secondaryAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "По расписанию на сегодня сессии не назначены.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Баннер выполненной группы, если все 3 сессии в выбранной вкладке закрыты
                        val activeGroupAllDone = activeGroupSlots.isNotEmpty() && activeGroupSlots.all { it.isCompleted }
                        if (activeGroupAllDone) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.successAccent.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.successAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .testTag("group_completed_banner_${activeGroup.code}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.successAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${activeGroup.titleRu}: все 3 тренировки выполнены",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.successAccent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Список из 3 тренировок выбранной вкладки (Утро / Обед / Вечер)
                        Crossfade(
                            targetState = activeGroup,
                            animationSpec = tween(durationMillis = if (animatorsEnabled) 150 else 0),
                            label = "period_slots_crossfade"
                        ) { currentGroup ->
                            val itemsForGroup = groupedSlots[currentGroup].orEmpty()
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsForGroup.forEachIndexed { index, item ->
                                    PeriodWorkoutCard(
                                        stepNumber = index + 1,
                                        item = item,
                                        isSelected = snapshot.protocol.id == item.slot.id,
                                        isSessionLocked = snapshot.isActiveOrPaused,
                                        onSelectSlot = { onSelectSlot(item) },
                                        onStartSlot = { onStartSlot(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedMode == "free") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            WorkoutConstructorTabContent(
                programs = customPrograms,
                onStartProgram = onStartCustomProgram,
                onSaveProgram = onSaveCustomProgram,
                onDeleteProgram = onDeleteCustomProgram,
                onDuplicateProgram = onDuplicateCustomProgram,
                onReorderPrograms = onReorderCustomPrograms
            )
        }

        // Компактный нижний переключатель дополнительных режимов (Расписание / Интервалы / Конструктор)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.outline.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .testTag("plan_mode_switcher")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactModeChip(
                    title = "Расписание",
                    selected = selectedMode == "scheduled",
                    onClick = { selectedMode = "scheduled" },
                    testTag = "plan_tab_scheduled",
                    modifier = Modifier.weight(1f)
                )
                CompactModeChip(
                    title = "Интервалы",
                    selected = selectedMode == "free",
                    onClick = { selectedMode = "free" },
                    testTag = "plan_tab_free",
                    modifier = Modifier.weight(1f)
                )
                CompactModeChip(
                    title = "Конструктор",
                    selected = selectedMode == "constructor",
                    onClick = { selectedMode = "constructor" },
                    testTag = "plan_tab_constructor",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Горизонтальная вкладка периода дня («Утро», «Обед», «Вечер»):
 * - Когда все 3 тренировки периода выполнены (`isCompleted = true`), вкладка загорается зелёным;
 * - Текст всегда в одну строку без вертикальных иконок и переносов по буквам.
 */
@Composable
private fun PeriodHorizontalTab(
    group: PlanGroup,
    completedCount: Int,
    totalCount: Int,
    isCompleted: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current

    val backgroundBrush = when {
        isCompleted && isSelected -> Brush.horizontalGradient(
            listOf(
                Color(0xFF10B981),
                Color(0xFF059669)
            )
        )
        isCompleted -> Brush.verticalGradient(
            listOf(
                Color(0xFFD1FAE5),
                Color(0xFFA7F3D0)
            )
        )
        isSelected -> Brush.horizontalGradient(
            listOf(palette.buttonGradientStart, palette.buttonGradientEnd)
        )
        else -> Brush.verticalGradient(
            listOf(palette.surfaceElevated, palette.surfaceElevated)
        )
    }

    val borderColor = when {
        isCompleted -> Color(0xFF059669)
        isSelected -> palette.primaryAccent
        else -> palette.outline.copy(alpha = 0.45f)
    }

    val titleColor = when {
        isCompleted && isSelected -> Color.White
        isCompleted -> Color(0xFF065F46)
        isSelected -> palette.onPrimaryAccent
        else -> palette.textPrimary
    }

    val badgeColor = when {
        isCompleted && isSelected -> Color.White.copy(alpha = 0.95f)
        isCompleted -> Color(0xFF047857)
        isSelected -> palette.onPrimaryAccent.copy(alpha = 0.92f)
        else -> palette.textSecondary
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(backgroundBrush)
            .border(
                width = if (isCompleted || isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp)
            .semantics {
                contentDescription = "${group.titleRu}: выполнено $completedCount из $totalCount" +
                    if (isCompleted) ", завершено" else ""
            }
            .testTag("plan_group_tab_${group.code}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = group.titleRu,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isCompleted) "✓ $completedCount/$totalCount" else "$completedCount из $totalCount",
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = if (isCompleted || isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Карточка одной из 3 тренировок внутри вкладки «Утро», «Обед» или «Вечер».
 */
@Composable
private fun PeriodWorkoutCard(
    stepNumber: Int,
    item: DailySlotWithCompletion,
    isSelected: Boolean,
    isSessionLocked: Boolean,
    onSelectSlot: () -> Unit,
    onStartSlot: () -> Unit
) {
    val palette = LocalVelarisPalette.current
    val slot = item.slot
    val isCompleted = item.isCompleted
    val diffLabel = WorkoutDifficulty.fromCode(item.completion?.optionalDifficulty)?.labelRu
    val totalDurationSec = (((slot.contractionMs + slot.relaxationMs) * slot.repetitions) / 1000L).toInt()
    val subtitleRu = resolveWorkoutSubtitleRu(slot)
    val contractionSec = slot.contractionMs / 1000L
    val relaxationSec = slot.relaxationMs / 1000L

    val cardBg = when {
        isCompleted -> palette.successAccent.copy(alpha = 0.10f)
        isSelected -> palette.selectedContainer
        else -> palette.surfaceElevated.copy(alpha = 0.85f)
    }
    val borderColor = when {
        isCompleted -> palette.successAccent.copy(alpha = 0.65f)
        isSelected -> palette.selectedBorder
        else -> palette.outline.copy(alpha = 0.26f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = if (isCompleted || isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isSessionLocked) { onSelectSlot() }
            .padding(12.dp)
            .testTag("daily_slot_${slot.assignmentId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Верхняя строка: номер шага + название тренировки + бейдж длительности или выполнения
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) {
                                palette.successAccent.copy(alpha = 0.22f)
                            } else {
                                palette.primaryAccent.copy(alpha = 0.18f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Выполнено",
                            tint = palette.successAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.titleRu,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleRu + (if (diffLabel != null) " · $diffLabel" else ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCompleted) {
                StatusPillBadge(
                    text = "Выполнено",
                    accentColor = palette.successAccent
                )
            } else {
                StatusPillBadge(
                    text = formatSecondsMinSec(totalDurationSec),
                    accentColor = palette.secondaryAccent
                )
            }
        }

        // Средняя строка: 3 аккуратные плитки параметров (без вертикального растягивания)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WorkoutMetricMiniTile(
                label = if (contractionSec >= 20L) "Удержание" else "Сжатие",
                value = "$contractionSec сек",
                accent = if (isCompleted) palette.successAccent else palette.primaryAccent,
                modifier = Modifier.weight(1f)
            )
            WorkoutMetricMiniTile(
                label = "Отдых",
                value = "$relaxationSec сек",
                accent = if (isCompleted) palette.successAccent else palette.secondaryAccent,
                modifier = Modifier.weight(1f)
            )
            WorkoutMetricMiniTile(
                label = "Сжиманий",
                value = "${slot.repetitions}",
                accent = if (isCompleted) palette.successAccent else palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Кнопка запуска (показывается, если подход ещё не выполнен)
        if (!isCompleted && !isSessionLocked) {
            Button(
                onClick = onStartSlot,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                ),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp)
                    .testTag("btn_start_slot_${slot.assignmentId}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Начать тренировку",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun CompactModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val bgColor = if (selected) palette.primaryAccent else palette.surfaceElevated
    val contentColor = if (selected) palette.onPrimaryAccent else palette.textSecondary

    Box(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Секция «Мои интервалы» с 5 сохраняемыми шаблонами.
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
            .border(1.dp, palette.outline.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .testTag("free_interval_templates_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Мои интервалы (5 шаблонов)",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("free_intervals_disclaimer_caption")
            ) {
                Text(
                    text = "Ваши интервалы. Приложение не оценивает подходящую нагрузку",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            if (isSessionLocked) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.warningAccent.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = palette.warningAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Сейчас уже выполняется или стоит на паузе другая тренировка.",
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (calc.isValid) palette.outline.copy(alpha = 0.30f) else palette.errorAccent.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
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
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPillBadge(
                            text = "#${template.slotIndex + 1}",
                            accentColor = palette.secondaryAccent
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorkoutMetricMiniTile(
                            label = "Удержание",
                            value = "${template.contractionSec} сек",
                            accent = palette.primaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        WorkoutMetricMiniTile(
                            label = "Отдых",
                            value = "${template.relaxationSec} сек",
                            accent = palette.secondaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = calc.summaryRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (calc.isValid) palette.secondaryAccent else palette.errorAccent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("free_template_summary_${template.slotIndex}")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 40.dp)
                                .testTag("btn_start_free_${template.slotIndex}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Начать", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { onEditTemplate(template) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .testTag("btn_edit_free_${template.slotIndex}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Изменить",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Редактор шаблона «Мои интервалы».
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
            .border(1.5.dp, palette.primaryAccent, RoundedCornerShape(18.dp))
            .testTag("free_template_editor_dialog"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Настройка шаблона ${template.slotIndex + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
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
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = {
                        minutesInput = it.filter { ch -> ch.isDigit() }.take(3)
                        showUnsavedConfirm = false
                    },
                    label = { Text("Мин (0–90)") },
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
                    label = { Text("Сек (0–59)") },
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
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            val displayMessage = fieldError ?: calc.summaryRu
            Surface(
                shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (showUnsavedConfirm && hasUnsavedChanges) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.warningAccent.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unsaved_changes_confirm_bar")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
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
                                    .heightIn(min = 40.dp)
                            ) {
                                Text("Остаться", color = palette.textPrimary)
                            }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.errorAccent,
                                    contentColor = palette.onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("btn_discard_unsaved_template")
                            ) {
                                Text("Сбросить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        .heightIn(min = 42.dp)
                        .testTag("btn_cancel_free_template")
                ) {
                    Text("Отмена", color = palette.textPrimary)
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
                                    isConfigured = true
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
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("btn_save_free_template")
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
