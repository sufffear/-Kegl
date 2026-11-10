package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.domain.WorkoutDifficulty
import com.example.ui.theme.LocalVelarisPalette
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun statisticsDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val h = safe / 3600; val m = safe % 3600 / 60; val s = safe % 60
    return listOfNotNull(if (h > 0) "$h ч" else null, if (m > 0) "$m мин" else null, if (s > 0 || safe == 0) "$s с" else null).joinToString(" ")
}

/** Use a recorded zone only; never reinterpret old sessions in today's phone zone. */
internal fun journalClock(epochMs: Long?, zone: String?): String? =
    if (epochMs == null || epochMs <= 0 || zone == null) null else runCatching {
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()

@Composable
internal fun VelarisStatisticsScreen(overview: WeeklyOverviewState, onDay: (String) -> Unit, onJournal: () -> Unit, onDetails: () -> Unit) {
    val palette = LocalVelarisPalette.current
    Column(Modifier.fillMaxWidth().testTag("statistics_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = VelarisSpacing.Content).testTag("statistics_metrics"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatisticsMetric("Серия дней", "${overview.currentStreakDays}", "stats_streak", Modifier.weight(1f))
            StatisticsMetric("Чистое удержание", statisticsDuration(overview.totalHoldSecondsAllTime), "stats_hold", Modifier.weight(1f))
        }
        StatisticsCalendar(overview, onDay)
        VelarisSectionLink("Журнал тренировок", "Все реальные занятия и сохранённые попытки", onJournal, Modifier.padding(horizontal = VelarisSpacing.Content).testTag("btn_workout_journal"))
        OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth().padding(horizontal = VelarisSpacing.Content).heightIn(min = 48.dp).testTag("btn_stats_details")) {
            Text("Подробная статистика", color = palette.textPrimary)
        }
    }
}

@Composable
private fun StatisticsMetric(title: String, value: String, tag: String, modifier: Modifier) {
    val palette = LocalVelarisPalette.current
    Surface(modifier.testTag(tag), shape = RoundedCornerShape(16.dp), color = palette.surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }
    }
}

@Composable
private fun StatisticsCalendar(overview: WeeklyOverviewState, onDay: (String) -> Unit) {
    val palette = LocalVelarisPalette.current
    val today = runCatching { LocalDate.parse(overview.days.lastOrNull()?.localDate) }.getOrElse { LocalDate.now() }
    var savedMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val month = savedMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.from(today)
    val latest = remember(overview.calendarDaysByDate, today) {
        overview.calendarDaysByDate.keys.mapNotNull { runCatching { YearMonth.from(LocalDate.parse(it)) }.getOrNull() }
            .maxOrNull()?.coerceAtLeast(YearMonth.from(today)) ?: YearMonth.from(today)
    }
    val cells = ((month.atDay(1).dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7) * 7
    Surface(Modifier.fillMaxWidth().testTag("monthly_calendar_section"), color = palette.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().testTag("monthly_calendar_header"), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { savedMonth = month.minusMonths(1).toString() }, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp).testTag("btn_calendar_prev_month").semantics { contentDescription = "Предыдущий месяц" }) { Text("‹") }
                Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru", "RU"))).replaceFirstChar { it.titlecase(Locale("ru", "RU")) },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("monthly_calendar_title"), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = { savedMonth = month.plusMonths(1).toString() }, enabled = month < latest, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp).testTag("btn_calendar_next_month").semantics { contentDescription = "Следующий месяц" }) { Text("›") }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Preserve 48dp targets rather than squeezing seven columns on a narrow phone.
                val narrow = maxWidth < 336.dp
                val gridWidth = maxOf(maxWidth, 336.dp)
                val horizontal = rememberScrollState()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.horizontalScroll(horizontal).width(gridWidth).testTag("statistics_calendar_grid")) {
                    Row(Modifier.fillMaxWidth()) { listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = palette.textSecondary) } }
                    repeat(cells / 7) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val day = row * 7 + col - month.atDay(1).dayOfWeek.value + 2
                                if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).heightIn(min = 56.dp)) else {
                                    val date = month.atDay(day).toString()
                                    val summary = overview.calendarDaysByDate[date]
                                    val count = summary?.completedCount ?: 0
                                    val frozen = summary?.isFrozen == true || date in overview.usedFreezeDates
                                    val selected = date == overview.selectedDate
                                    Column(Modifier.weight(1f).heightIn(min = 56.dp)
                                        .border(if (date == today.toString() || selected) 1.dp else 0.dp, if (selected) palette.primaryAccent else palette.outline, RoundedCornerShape(8.dp))
                                        .background(if (selected) palette.primaryAccent.copy(alpha = 0.15f) else palette.surface, RoundedCornerShape(8.dp))
                                        .selectable(selected, role = Role.Button, onClick = { onDay(date) })
                                        .testTag("calendar_day_$date").semantics { contentDescription = "$date. Завершено $count" + if (frozen) ". Заморозка" else "" }
                                        .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$day", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                        Text("$count", color = if (count > 0) palette.primaryAccent else palette.textSecondary, style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.testTag("calendar_count_$date"))
                                        if (frozen) Text("❄", style = MaterialTheme.typography.labelSmall, color = palette.secondaryAccent)
                                    }
                                }
                            }
                        }
                    }
                }
                if (narrow) Text("Календарь прокручивается по горизонтали", style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun VelarisWorkoutJournal(state: WorkoutJournalState, date: String?, onAllDates: () -> Unit, onEntry: (String) -> Unit, onRetry: () -> Unit) {
    val palette = LocalVelarisPalette.current
    var visible by rememberSaveable(date) { mutableIntStateOf(50) }
    val entries = if (date == null) state.entries else state.entries.filter { it.localDate == date }
    Column(Modifier.fillMaxWidth().testTag("workout_journal_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(date?.let { "Журнал · $it" } ?: "Журнал тренировок", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        if (date != null) TextButton(onClick = onAllDates, modifier = Modifier.testTag("journal_all_dates")) { Text("Весь журнал") }
        JournalLoadStatus(state, onRetry)
        if (!state.loading && !state.error && entries.isEmpty()) Text("Занятий пока нет", color = palette.textSecondary, modifier = Modifier.testTag("journal_empty"))
        entries.take(visible).forEach { entry ->
            VelarisSectionLink(entry.title, "${entry.localDate}${journalClock(entry.run?.startedAtEpochMs, entry.run?.zoneId)?.let { " · $it" }.orEmpty()} · ${if (entry.isFree) "Дополнительное" else "По программе"} · ${entry.statusRu}",
                { onEntry(entry.id) }, Modifier.testTag("journal_entry_${entry.id}"))
        }
        if (entries.size > visible) OutlinedButton(onClick = { visible += 50 }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("journal_more")) {
            Text("Показать ещё · сейчас $visible из ${entries.size}")
        }
    }
}

@Composable
internal fun VelarisStatisticsDay(overview: WeeklyOverviewState, date: String, onJournal: () -> Unit) {
    val p = LocalVelarisPalette.current
    val day = overview.calendarDaysByDate[date]
    Column(Modifier.fillMaxWidth().testTag("statistics_day_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(date, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
        Text("Завершено: ${day?.completedCount ?: 0}", color = p.textPrimary)
        Text("Завершено по программе: ${day?.plannedCompletedCount ?: 0} · Дополнительных: ${day?.freeCompletedCount ?: 0}", color = p.textSecondary)
        Text("Чистое удержание: ${statisticsDuration(day?.holdSecondsTotal ?: 0)}", color = p.textPrimary)
        if (day?.isFrozen == true || date in overview.usedFreezeDates) Text("День защищён заморозкой", color = p.secondaryAccent)
        VelarisSectionLink("Занятия за этот день", "Завершённые и сохранённые попытки", onJournal, Modifier.testTag("btn_day_journal"))
    }
}

@Composable
internal fun VelarisWorkoutSession(state: WorkoutJournalState, id: String?, overview: WeeklyOverviewState, onRetry: () -> Unit) {
    val p = LocalVelarisPalette.current
    val entry = state.entries.firstOrNull { it.id == id }
    Column(Modifier.fillMaxWidth().testTag("workout_session_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JournalLoadStatus(state, onRetry)
        if (entry == null) {
            if (!state.loading && !state.error) Text("Запись недоступна. Вернись в журнал и обнови список.", color = p.textSecondary)
        } else {
            Text(entry.title, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
            Text(entry.localDate, color = p.textSecondary)
            Text(entry.statusRu, color = p.primaryAccent, modifier = Modifier.testTag("session_status"))
            Text(if (entry.isFree) "Дополнительная тренировка" else "По программе", color = p.textSecondary)
            val run = entry.run; val slot = entry.slot
            val hold = run?.contractionMs ?: slot?.contractionMs
            val rest = run?.relaxationMs ?: slot?.relaxationMs
            val repetitions = run?.repetitions ?: slot?.repetitions
            if (hold != null && rest != null) Text("Удержание: ${hold / 1000} с · отдых: ${rest / 1000} с · повторов: $repetitions", color = p.textPrimary)
            val summary = overview.calendarDaysByDate[entry.localDate]?.completedSessions?.firstOrNull { it.id == entry.id }
            if (summary != null) {
                if (run == null && slot == null) Text("Длительность старой записи восстановлена приблизительно", color = p.textSecondary)
                Text("Чистое удержание: ${statisticsDuration(summary.holdSeconds)}", color = p.textPrimary)
            }
            if (run != null) {
                journalClock(run.startedAtEpochMs, run.zoneId)?.let { Text("Начато: ${run.startDate} $it", color = p.textSecondary) }
                journalClock(entry.completion?.completedAtEpochMs, run.zoneId)?.let { Text("Завершено: ${entry.localDate} $it", color = p.textSecondary) }
                Text("Подтверждено активности: ${statisticsDuration((run.confirmedActiveMs / 1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())}", color = p.textSecondary)
                Text("Часовой пояс: ${run.zoneId}", color = p.textSecondary)
                run.interruptionReason?.let { Text(it, color = p.textSecondary) }
            }
            entry.completion?.optionalDifficulty?.let { code -> Text("Нагрузка: ${WorkoutDifficulty.entries.firstOrNull { it.code == code }?.labelRu ?: code}", color = p.textSecondary) }
        }
    }
}

@Composable
private fun JournalLoadStatus(state: WorkoutJournalState, onRetry: () -> Unit) {
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("journal_loading"))
    if (state.error) {
        Text("Не удалось обновить журнал. Сохранения не изменены.", color = LocalVelarisPalette.current.errorAccent)
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp).testTag("journal_retry")) { Text("Повторить") }
    }
}
