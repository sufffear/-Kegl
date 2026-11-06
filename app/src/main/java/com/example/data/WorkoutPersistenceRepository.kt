package com.example.data

import com.example.domain.CustomWorkoutProgram
import com.example.domain.DEFAULT_DEMO_PROTOCOLS
import com.example.domain.DemoProtocolConfig
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PlanGroup
import com.example.domain.RunStatus
import com.example.domain.TimeProvider
import com.example.domain.WorkoutDifficulty
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CompletedSessionItem(
    val id: String,
    val titleRu: String,
    val isMandatory: Boolean,
    val durationSeconds: Int,
    val holdSeconds: Int
)

data class WeeklyDaySummary(
    val localDate: String,
    val shortLabelRu: String,
    val dayOfMonthLabel: String,
    val monthShortRu: String = "",
    val isToday: Boolean,
    val completedCount: Int,
    val assignedCount: Int,
    val plannedCompletedCount: Int,
    val freeCompletedCount: Int,
    val completedTitlesRu: List<String>,
    val holdSecondsTotal: Int = 0,
    val totalWorkoutSeconds: Int = 0,
    val closedPeriodsCount: Int = 0,
    val intensityLevel: Int = 0,
    val isFrozen: Boolean = false,
    val completedSessions: List<CompletedSessionItem> = emptyList()
)

data class StreakMilestoneInfo(
    val targetDays: Int,
    val titleRu: String,
    val badgeEmoji: String,
    val descriptionRu: String = "",
    val isUnlocked: Boolean = false,
    val isCurrentTarget: Boolean = false
)

data class WeeklyOverviewState(
    val days: List<WeeklyDaySummary> = emptyList(),
    val heatmapDays: List<WeeklyDaySummary> = emptyList(),
    val calendarDaysByDate: Map<String, WeeklyDaySummary> = emptyMap(),
    val selectedDate: String = "",
    val totalRealCompletionsInWeek: Int = 0,
    val totalRealCompletionsAllTime: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val isTodayActiveInStreak: Boolean = false,
    val isStreakAtRiskToday: Boolean = false,
    val availableFreezeShields: Int = 1,
    val maxFreezeShields: Int = 3,
    val usedFreezeDates: Set<String> = emptySet(),
    val perfectDaysInMonth: Int = 0,
    val activeDaysInMonth: Int = 0,
    val totalHoldSecondsInWeek: Int = 0,
    val totalHoldSecondsAllTime: Int = 0,
    val totalWorkoutSecondsInWeek: Int = 0,
    val totalWorkoutSecondsAllTime: Int = 0,
    val previousWeekCompletions: Int = 0,
    val previousWeekHoldSeconds: Int = 0,
    val weekHoldDeltaSeconds: Int = 0,
    val weekSessionsDelta: Int = 0,
    val weekHoldDeltaPercent: Int = 0,
    val weekDeltaBadgeTextRu: String = "Ждёт старта недели",
    val weekDeltaSubtitleRu: String = "Выполните подход, чтобы сравнить темп с прошлой неделей",
    val rankTitleRu: String = "Старт пути",
    val rankLevel: Int = 1,
    val nextMilestoneDays: Int = 3,
    val nextMilestoneTitleRu: String = "Искра ритма",
    val milestoneProgressFraction: Float = 0f,
    val milestones: List<StreakMilestoneInfo> = emptyList(),
    val dailyQuestTitleRu: String = "Зажечь звено дня",
    val dailyQuestSubtitleRu: String = "Выполните 1 тренировку, чтобы запустить цепочку стабильности",
    val dailyQuestProgress: Float = 0f
) {
    val hasAnyRealData: Boolean
        get() = totalRealCompletionsAllTime > 0 || totalRealCompletionsInWeek > 0

    val selectedDaySummary: WeeklyDaySummary?
        get() = calendarDaysByDate[selectedDate]
            ?: heatmapDays.firstOrNull { it.localDate == selectedDate }
            ?: days.firstOrNull { it.localDate == selectedDate }
            ?: days.lastOrNull()
}

/**
 * Репозиторий хранения планов, дневных снимков (DailyPlan + DailySlot), попыток (WorkoutRun),
 * контрольных точек, атомарных завершений (WorkoutCompletion) и 7/28-дневного обзора в Room.
 */
class WorkoutPersistenceRepository(
    private val dao: VelarisDao,
    private val timeProvider: TimeProvider
) {

    suspend fun ensureBuiltinPlansSeeded() {
        val existingStandardDemo = dao.getTrainingPlanById(DEMO_PLAN_STANDARD_3)
        if (existingStandardDemo == null) {
            dao.upsertTrainingPlan(
                TrainingPlanEntity(
                    id = DEMO_PLAN_STANDARD_3,
                    version = 1,
                    titleRu = "Демо-план: 3 подхода (Утро / Обед / Вечер)",
                    source = "DEMO_BUILTIN_TESTBED",
                    effectiveFromDate = "2026-01-01",
                    isDemo = true
                )
            )
            val assignments = DEFAULT_DEMO_PROTOCOLS.mapIndexed { idx, proto ->
                PlanAssignmentEntity(
                    id = proto.id,
                    planId = DEMO_PLAN_STANDARD_3,
                    planVersion = 1,
                    groupCode = proto.group.code,
                    sortOrder = idx + 1,
                    titleRu = proto.titleRu,
                    contractionMs = proto.contractionMs,
                    relaxationMs = proto.relaxationMs,
                    repetitions = proto.repetitions,
                    isDemo = true
                )
            }
            dao.upsertPlanAssignments(assignments)
        }

        val existingRestDay = dao.getTrainingPlanById(DEMO_PLAN_ZERO_SLOTS)
        if (existingRestDay == null) {
            dao.upsertTrainingPlan(
                TrainingPlanEntity(
                    id = DEMO_PLAN_ZERO_SLOTS,
                    version = 1,
                    titleRu = "Демо-план: Сегодня без тренировок (N=0)",
                    source = "DEMO_BUILTIN_REST_DAY",
                    effectiveFromDate = "2026-01-01",
                    isDemo = true
                )
            )
        }

        val existingRealPlan = dao.getTrainingPlanById(REAL_PLAN_STANDARD_3)
        if (existingRealPlan == null || existingRealPlan.version < REAL_PLAN_CURRENT_VERSION) {
            dao.upsertTrainingPlan(
                TrainingPlanEntity(
                    id = REAL_PLAN_STANDARD_3,
                    version = REAL_PLAN_CURRENT_VERSION,
                    titleRu = "Ежедневный план: Утро, Обед и Вечер (по 3 тренировки)",
                    source = BenchSignalSettings.DEFAULT_REAL_PROGRAM_SOURCE,
                    effectiveFromDate = "2026-01-01",
                    isDemo = false
                )
            )
            val groups = listOf(PlanGroup.MORNING, PlanGroup.NOON, PlanGroup.EVENING)
            val realAssignments = mutableListOf<PlanAssignmentEntity>()
            var orderCounter = 1
            for (group in groups) {
                val gKey = group.code.lowercase()
                // 1. Быстрая тренировка: 100 сжиманий (1 с сжатие / 1 с отдых)
                realAssignments.add(
                    PlanAssignmentEntity(
                        id = "real_v2_${gKey}_fast",
                        planId = REAL_PLAN_STANDARD_3,
                        planVersion = REAL_PLAN_CURRENT_VERSION,
                        groupCode = group.code,
                        sortOrder = orderCounter++,
                        titleRu = "Быстрая тренировка",
                        contractionMs = 1_000L,
                        relaxationMs = 1_000L,
                        repetitions = 100,
                        isDemo = false
                    )
                )
                // 2. Медленная тренировка (Силовой контроль): 10 сжиманий (10 с сжатие / 5 с отдых)
                realAssignments.add(
                    PlanAssignmentEntity(
                        id = "real_v2_${gKey}_slow",
                        planId = REAL_PLAN_STANDARD_3,
                        planVersion = REAL_PLAN_CURRENT_VERSION,
                        groupCode = group.code,
                        sortOrder = orderCounter++,
                        titleRu = "Силовой контроль",
                        contractionMs = 10_000L,
                        relaxationMs = 5_000L,
                        repetitions = 10,
                        isDemo = false
                    )
                )
                // 3. Очень медленные сжимания (Выносливость): 10 сжиманий (25 с удержание / 10 с отдых)
                realAssignments.add(
                    PlanAssignmentEntity(
                        id = "real_v2_${gKey}_endurance",
                        planId = REAL_PLAN_STANDARD_3,
                        planVersion = REAL_PLAN_CURRENT_VERSION,
                        groupCode = group.code,
                        sortOrder = orderCounter++,
                        titleRu = "Выносливость",
                        contractionMs = 25_000L,
                        relaxationMs = 10_000L,
                        repetitions = 10,
                        isDemo = false
                    )
                )
            }
            dao.upsertPlanAssignments(realAssignments)
        }
    }

    suspend fun ensureBuiltinDemoPlansSeeded() {
        ensureBuiltinPlansSeeded()
    }

    suspend fun getOrCreateDailySlots(
        localDate: String = timeProvider.currentLocalDate(),
        planId: String = DEMO_PLAN_STANDARD_3,
        isDemo: Boolean = true
    ): List<DailySlotWithCompletion> {
        ensureBuiltinPlansSeeded()

        val plan = dao.getTrainingPlanById(planId) ?: TrainingPlanEntity(
            id = planId,
            version = if (planId == REAL_PLAN_STANDARD_3) REAL_PLAN_CURRENT_VERSION else 1,
            titleRu = if (isDemo) "Демо-план" else "Ежедневный план",
            source = if (isDemo) "DEMO_BUILTIN" else BenchSignalSettings.DEFAULT_REAL_PROGRAM_SOURCE,
            effectiveFromDate = localDate,
            isDemo = isDemo
        )
        val assignments = dao.getAssignmentsForPlan(plan.id, plan.version)

        var dailyPlan = dao.getDailyPlan(localDate = localDate, isDemo = isDemo)
        val needsSlotSync = dailyPlan == null ||
            dailyPlan.planId != plan.id ||
            dailyPlan.planVersion != plan.version

        if (dailyPlan == null) {
            val dailyPlanId = "${if (isDemo) "demo" else "real"}_${localDate}"
            val newDailyPlan = DailyPlanEntity(
                id = dailyPlanId,
                localDate = localDate,
                planId = plan.id,
                planVersion = plan.version,
                isDemo = isDemo
            )
            dao.insertDailyPlanIgnore(newDailyPlan)
            dailyPlan = dao.getDailyPlan(localDate = localDate, isDemo = isDemo) ?: newDailyPlan
        } else if (needsSlotSync) {
            val updatedDailyPlan = dailyPlan.copy(
                planId = plan.id,
                planVersion = plan.version
            )
            dao.insertDailyPlans(listOf(updatedDailyPlan))
            dailyPlan = updatedDailyPlan
        }

        if (needsSlotSync && assignments.isNotEmpty()) {
            val maxOrderByGroup = assignments.groupBy { it.groupCode }
                .mapValues { (_, list) -> list.maxOf { it.sortOrder } }

            val slotsToInsert = assignments.map { a ->
                DailySlotEntity(
                    id = "${dailyPlan.id}_${a.id}",
                    dailyPlanId = dailyPlan.id,
                    localDate = localDate,
                    assignmentId = a.id,
                    groupCode = a.groupCode,
                    sortOrder = a.sortOrder,
                    titleRu = a.titleRu,
                    contractionMs = a.contractionMs,
                    relaxationMs = a.relaxationMs,
                    repetitions = a.repetitions,
                    isLastInGroup = (maxOrderByGroup[a.groupCode] == a.sortOrder),
                    isDemo = isDemo
                )
            }
            dao.insertDailySlotsIgnore(slotsToInsert)
        }

        val rawSlots = dao.getSlotsForDailyPlan(dailyPlan.id)
        val validAssignmentIds = assignments.map { it.id }.toSet()
        val slots = if (plan.id == DEMO_PLAN_ZERO_SLOTS) {
            emptyList()
        } else if (validAssignmentIds.isNotEmpty()) {
            rawSlots.filter { it.assignmentId in validAssignmentIds }
        } else {
            rawSlots
        }

        val completionsBySlot = dao.getCompletionsForDate(localDate = localDate, isDemo = isDemo)
            .associateBy { it.slotId }

        return slots.map { slot ->
            DailySlotWithCompletion(
                slot = slot,
                completion = completionsBySlot[slot.id]
            )
        }
    }

    /**
     * Формирует 7-дневный и 28-дневный обзор «Цепочка стабильности» строго по реальным
     * сохранённым записям (`isDemo = false`). Демо-результаты (`isDemo = true`) не попадают в статистику.
     */
    suspend fun loadWeeklyOverview(
        todayDateStr: String = timeProvider.currentLocalDate(),
        selectedDateStr: String? = null,
        manualFrozenDates: Set<String> = emptySet()
    ): WeeklyOverviewState {
        val baseDate = runCatching { LocalDate.parse(todayDateStr) }.getOrElse { LocalDate.now() }
        val heatmapDates = (27 downTo 0).map { offset -> baseDate.minusDays(offset.toLong()) }
        val heatmapKeys = heatmapDates.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }

        val allRealCompletions = dao.getRealWorkoutCompletions()
        val allRealSlots = dao.getRealDailySlots()
        val allRealRuns = dao.getRealWorkoutRuns()

        val allSlotsMap = allRealSlots.associateBy { it.id }
        val allRunsMap = allRealRuns.associateBy { it.id }
        val realSlotsByDate = allRealSlots.groupBy { it.localDate }
        val realCompletionsByDate = allRealCompletions.groupBy { it.localDate }

        val activeDatesSet = realCompletionsByDate.filterValues { it.isNotEmpty() }.keys

        // Расчёт кристаллов «Заморозка серии» (1 базовый + 1 за каждые 3 активных дня)
        val totalEarnedShields = 1 + (activeDatesSet.size / 3)
        val effectiveFrozenDates = mutableSetOf<String>()

        // 1) Учитываем вручную замороженные пользователем пропущенные дни (где 0 тренировок)
        manualFrozenDates.sortedDescending().forEach { dateStr ->
            if (dateStr !in activeDatesSet && dateStr != todayDateStr && effectiveFrozenDates.size < totalEarnedShields) {
                effectiveFrozenDates.add(dateStr)
            }
        }

        // 2) Автоматическая защита одиночного пропуска в текущей цепочке, если остались щиты
        for (offset in 1..14) {
            if (effectiveFrozenDates.size >= totalEarnedShields) break
            val d = baseDate.minusDays(offset.toLong())
            val dKey = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val prevKey = d.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nextKey = d.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isSingleDayBridge = dKey !in activeDatesSet &&
                dKey !in effectiveFrozenDates &&
                prevKey in activeDatesSet &&
                (nextKey in activeDatesSet || dKey == baseDate.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
            if (isSingleDayBridge) {
                effectiveFrozenDates.add(dKey)
            }
        }

        val availableFreezeShields = (totalEarnedShields - effectiveFrozenDates.size).coerceIn(0, 3)

        fun computeCompletionHoldSeconds(comp: WorkoutCompletionEntity): Int {
            val slot = allSlotsMap[comp.slotId]
            if (slot != null) {
                return ((slot.contractionMs * slot.repetitions) / 1000L).toInt().coerceAtLeast(1)
            }
            val run = allRunsMap[comp.runId]
            if (run != null) {
                return ((run.contractionMs * run.repetitions) / 1000L).toInt().coerceAtLeast(1)
            }
            return 100
        }

        fun computeCompletionTotalSeconds(comp: WorkoutCompletionEntity): Int {
            val slot = allSlotsMap[comp.slotId]
            if (slot != null) {
                return (((slot.contractionMs + slot.relaxationMs) * slot.repetitions) / 1000L).toInt().coerceAtLeast(1)
            }
            val run = allRunsMap[comp.runId]
            if (run != null) {
                val fromProto = (((run.contractionMs + run.relaxationMs) * run.repetitions) / 1000L).toInt()
                val fromConfirmed = (run.confirmedActiveMs / 1000L).toInt()
                return maxOf(fromProto, fromConfirmed, 1)
            }
            return 200
        }

        fun resolveCompletionTitleRu(comp: WorkoutCompletionEntity): String {
            return if (comp.isFreeMode) {
                val runTitle = allRunsMap[comp.runId]?.titleRu
                runTitle?.takeIf { it.isNotBlank() } ?: "Свободный подход"
            } else {
                val slotEntity = allSlotsMap[comp.slotId]
                if (slotEntity != null) {
                    val groupTitle = PlanGroup.fromCode(slotEntity.groupCode).titleRu
                    if (slotEntity.titleRu.startsWith(groupTitle)) {
                        slotEntity.titleRu
                    } else {
                        "$groupTitle · ${slotEntity.titleRu}"
                    }
                } else {
                    "Подход выполнен"
                }
            }
        }

        fun buildSummaryForDate(date: LocalDate): WeeklyDaySummary {
            val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val daySlots = realSlotsByDate[key].orEmpty()
            val dayCompletions = realCompletionsByDate[key].orEmpty()
            val plannedCompletions = dayCompletions.filter { !it.isFreeMode }
            val freeCompletions = dayCompletions.filter { it.isFreeMode }
            val completedSlotIds = dayCompletions.map { it.slotId }.toSet()

            val completedSessions = dayCompletions.map { comp ->
                CompletedSessionItem(
                    id = comp.runId.ifBlank { comp.slotId },
                    titleRu = resolveCompletionTitleRu(comp),
                    isMandatory = !comp.isFreeMode,
                    durationSeconds = computeCompletionTotalSeconds(comp),
                    holdSeconds = computeCompletionHoldSeconds(comp)
                )
            }
            val completedTitles = completedSessions.map { it.titleRu }

            val holdSecondsForDay = completedSessions.sumOf { it.holdSeconds }
            val totalSecondsForDay = completedSessions.sumOf { it.durationSeconds }

            val closedPeriods = if (daySlots.isNotEmpty()) {
                daySlots.groupBy { it.groupCode }.count { (_, groupSlots) ->
                    groupSlots.isNotEmpty() && groupSlots.all { it.id in completedSlotIds }
                }
            } else {
                (dayCompletions.size / 3).coerceAtMost(3)
            }

            val isDayFrozen = (key in effectiveFrozenDates) && dayCompletions.isEmpty()

            val intensity = when {
                dayCompletions.size >= 6 || (daySlots.isNotEmpty() && dayCompletions.size >= daySlots.size && daySlots.size >= 3) -> 3
                dayCompletions.size >= 3 || closedPeriods >= 1 -> 2
                dayCompletions.isNotEmpty() -> 1
                else -> 0
            }

            return WeeklyDaySummary(
                localDate = key,
                shortLabelRu = dayOfWeekShortRu(date.dayOfWeek.value),
                dayOfMonthLabel = date.dayOfMonth.toString(),
                monthShortRu = monthShortRu(date.monthValue),
                isToday = (key == todayDateStr),
                completedCount = dayCompletions.size,
                assignedCount = daySlots.size.coerceAtLeast(9),
                plannedCompletedCount = plannedCompletions.size,
                freeCompletedCount = freeCompletions.size,
                completedTitlesRu = completedTitles,
                holdSecondsTotal = holdSecondsForDay,
                totalWorkoutSeconds = totalSecondsForDay,
                closedPeriodsCount = closedPeriods,
                intensityLevel = intensity,
                isFrozen = isDayFrozen,
                completedSessions = completedSessions
            )
        }

        val heatmapSummaries = heatmapDates.map { date -> buildSummaryForDate(date) }

        val calendarMap = mutableMapOf<String, WeeklyDaySummary>()
        val earliestMonthStart = baseDate.withDayOfMonth(1).minusMonths(5)
        val latestMonthEnd = baseDate.withDayOfMonth(baseDate.lengthOfMonth())
        var calCursor = earliestMonthStart
        while (!calCursor.isAfter(latestMonthEnd)) {
            val summary = buildSummaryForDate(calCursor)
            calendarMap[summary.localDate] = summary
            calCursor = calCursor.plusDays(1)
        }
        (realCompletionsByDate.keys + effectiveFrozenDates + listOfNotNull(selectedDateStr)).forEach { dateStr ->
            if (dateStr !in calendarMap) {
                runCatching { LocalDate.parse(dateStr) }.getOrNull()?.let { parsed ->
                    calendarMap[dateStr] = buildSummaryForDate(parsed)
                }
            }
        }

        val last7Days = heatmapSummaries.takeLast(7)
        val prev7Days = if (heatmapSummaries.size >= 14) {
            heatmapSummaries.subList(heatmapSummaries.size - 14, heatmapSummaries.size - 7)
        } else {
            emptyList()
        }
        val totalWeek = last7Days.sumOf { it.completedCount }
        val prevWeekCompletions = prev7Days.sumOf { it.completedCount }
        val totalAllTime = allRealCompletions.size
        val totalHoldWeek = last7Days.sumOf { it.holdSecondsTotal }
        val prevWeekHoldSeconds = prev7Days.sumOf { it.holdSecondsTotal }
        val totalHoldAllTime = allRealCompletions.sumOf { computeCompletionHoldSeconds(it) }
        val totalWorkoutWeek = last7Days.sumOf { it.totalWorkoutSeconds }
        val totalWorkoutAllTime = allRealCompletions.sumOf { computeCompletionTotalSeconds(it) }

        val weekHoldDeltaSec = totalHoldWeek - prevWeekHoldSeconds
        val weekSessDelta = totalWeek - prevWeekCompletions
        val weekHoldDeltaPct = when {
            prevWeekHoldSeconds > 0 -> (((totalHoldWeek - prevWeekHoldSeconds) * 100.0) / prevWeekHoldSeconds).toInt()
            totalHoldWeek > 0 -> 100
            else -> 0
        }

        fun formatHoldShortRu(sec: Int): String {
            val s = kotlin.math.abs(sec)
            if (s == 0) return "0 с"
            val m = s / 60
            val rem = s % 60
            return when {
                m == 0 -> "${rem}с"
                rem == 0 -> "${m}м"
                else -> "${m}м ${rem}с"
            }
        }

        val weekDeltaBadge = when {
            prevWeekHoldSeconds > 0 && weekHoldDeltaPct > 0 -> "+${weekHoldDeltaPct}% к прошлой нед."
            prevWeekHoldSeconds > 0 && weekHoldDeltaPct == 0 -> "100% темп прошлой нед."
            prevWeekHoldSeconds > 0 && weekHoldDeltaPct < 0 -> "${weekHoldDeltaPct}% к прошлой нед."
            totalHoldWeek > 0 -> "+100% старт недели"
            else -> "Сравнение недель"
        }

        val weekDeltaSubtitle = when {
            totalWeek == 0 && prevWeekCompletions == 0 ->
                "Завершите подход, чтобы запустить сравнение с прошлой неделей."
            weekHoldDeltaSec > 0 ->
                "Прирост +${formatHoldShortRu(weekHoldDeltaSec)} чистого сжатия (эта нед.: ${totalWeek} сесс. / ${formatHoldShortRu(totalHoldWeek)}, прошлая: ${prevWeekCompletions} сесс. / ${formatHoldShortRu(prevWeekHoldSeconds)})."
            weekHoldDeltaSec == 0 ->
                "Точное совпадение с прошлой неделей: ${totalWeek} сесс. (${formatHoldShortRu(totalHoldWeek)} сжатия)."
            else ->
                "До рекорда прошлой недели осталось ${formatHoldShortRu(-weekHoldDeltaSec)} сжатия (сейчас ${formatHoldShortRu(totalHoldWeek)} из ${formatHoldShortRu(prevWeekHoldSeconds)})."
        }

        // Расчёт текущей непрерывной серии (Current Streak)
        val isTodayActive = todayDateStr in activeDatesSet
        var currentStreak = 0
        var cursorDate = if (isTodayActive) baseDate else baseDate.minusDays(1)
        while (true) {
            val key = cursorDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            when {
                key in activeDatesSet -> {
                    currentStreak++
                    cursorDate = cursorDate.minusDays(1)
                }
                key in effectiveFrozenDates -> {
                    cursorDate = cursorDate.minusDays(1)
                }
                else -> break
            }
        }

        // Расчёт лучшей серии за всё время (Best Streak)
        val allKnownDates = (activeDatesSet + effectiveFrozenDates).mapNotNull {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        var bestStreak = currentStreak
        if (allKnownDates.isNotEmpty()) {
            val minDate = allKnownDates.minOrNull() ?: baseDate
            var d = minDate
            var running = 0
            while (!d.isAfter(baseDate)) {
                val k = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                when {
                    k in activeDatesSet -> {
                        running++
                        if (running > bestStreak) bestStreak = running
                    }
                    k in effectiveFrozenDates -> {
                        // Замороженный день удерживает цепь, но не накручивает ложные активные дни сам по себе
                    }
                    else -> {
                        running = 0
                    }
                }
                d = d.plusDays(1)
            }
        }

        val isStreakAtRisk = !isTodayActive && currentStreak > 0
        val perfectDaysMonth = heatmapSummaries.count { it.intensityLevel >= 2 }
        val activeDaysMonth = heatmapSummaries.count { it.completedCount > 0 }

        // Коллекционная витрина «Жетоны рубежей»
        val rawMilestones = listOf(
            StreakMilestoneInfo(
                targetDays = 3,
                titleRu = "Искра ритма",
                badgeEmoji = "🔥",
                descriptionRu = "3 дня подряд: запуск нейромышечного отклика и включение базовой привычки."
            ),
            StreakMilestoneInfo(
                targetDays = 7,
                titleRu = "Железная неделя",
                badgeEmoji = "⚡",
                descriptionRu = "7 дней подряд: полная неделя дисциплины и уверенный контроль быстрых сжиманий."
            ),
            StreakMilestoneInfo(
                targetDays = 14,
                titleRu = "Двойной контур",
                badgeEmoji = "🛡️",
                descriptionRu = "14 дней подряд: синхронное укрепление быстрых и медленных мышечных волокон."
            ),
            StreakMilestoneInfo(
                targetDays = 21,
                titleRu = "Нейро-привычка",
                badgeEmoji = "💎",
                descriptionRu = "21 день подряд: тренировки стали автоматическим утренним, дневным и вечерним ритуалом."
            ),
            StreakMilestoneInfo(
                targetDays = 30,
                titleRu = "Мастер контроля",
                badgeEmoji = "👑",
                descriptionRu = "30 дней подряд: месяц без обрыва цепи и высокая выносливость на 25-секундных удержаниях."
            ),
            StreakMilestoneInfo(
                targetDays = 60,
                titleRu = "Титан тонуса",
                badgeEmoji = "🏆",
                descriptionRu = "60 дней подряд: глубокий мышечный тонус и эталонная стабильность."
            ),
            StreakMilestoneInfo(
                targetDays = 100,
                titleRu = "Легенда Velaris",
                badgeEmoji = "🌟",
                descriptionRu = "100 дней подряд: высший титул дисциплины и мастерства Velaris."
            )
        )
        val nextMilestoneRaw = rawMilestones.firstOrNull { it.targetDays > currentStreak } ?: rawMilestones.last()
        val maxUnlockedStreak = maxOf(currentStreak, bestStreak)
        val milestones = rawMilestones.map { m ->
            m.copy(
                isUnlocked = maxUnlockedStreak >= m.targetDays,
                isCurrentTarget = m.targetDays == nextMilestoneRaw.targetDays && maxUnlockedStreak < m.targetDays
            )
        }

        val nextMilestone = milestones.firstOrNull { it.targetDays > currentStreak } ?: milestones.last()
        val prevMilestoneDays = milestones.lastOrNull { it.targetDays <= currentStreak }?.targetDays ?: 0
        val achievedMilestone = milestones.lastOrNull { it.targetDays <= currentStreak }

        val rankTitle = when {
            achievedMilestone != null -> achievedMilestone.titleRu
            currentStreak >= 1 || totalAllTime >= 1 -> "Первое звено"
            else -> "Старт пути"
        }
        val rankLevel = milestones.indexOfFirst { it.targetDays > currentStreak }
            .let { if (it == -1) milestones.size else it + 1 }

        val span = (nextMilestone.targetDays - prevMilestoneDays).coerceAtLeast(1)
        val progressInSpan = (currentStreak - prevMilestoneDays).coerceAtLeast(0)
        val milestoneFraction = (progressInSpan.toFloat() / span.toFloat()).coerceIn(0f, 1f)

        // Ежедневный квест на сегодня
        val todaySummary = last7Days.lastOrNull()
        val todayCount = todaySummary?.completedCount ?: 0
        val (questTitle, questSubtitle, questProgress) = when {
            todayCount == 0 && isStreakAtRisk -> Triple(
                "Спасите серию $currentStreak дн.!",
                "Выполните 1 подход или запустите «⚡ Блиц-спасение за 60 сек», чтобы сохранить цепочку.",
                0.08f
            )
            todayCount == 0 -> Triple(
                "Зажечь сегодняшнее звено",
                "Сделайте 1 тренировку по плану или «⚡ Блиц за 60 сек», чтобы открыть серию дня.",
                0.05f
            )
            todayCount in 1..2 -> Triple(
                "Цель: закрыть 1-й этап ($todayCount из 3)",
                "Осталось ${3 - todayCount} сессии до полного закрытия этапа (Утро/Обед/Вечер).",
                (todayCount / 3f).coerceIn(0.15f, 0.95f)
            )
            todayCount in 3..5 -> Triple(
                "Золотой экватор ($todayCount из 6)",
                "Первый этап закрыт! Выполните ещё ${6 - todayCount} сессии для двойного тонуса.",
                (todayCount / 6f).coerceIn(0.45f, 0.95f)
            )
            todayCount in 6..8 -> Triple(
                "Рывок до Абсолютного дня ($todayCount из 9)",
                "Осталось ${9 - todayCount} сессии, чтобы закрыть все 3 вкладки дня на 100%!",
                (todayCount / 9f).coerceIn(0.65f, 0.98f)
            )
            else -> Triple(
                "Абсолютный день выполнен! ★",
                "Все 3 этапа (Утро, Обед и Вечер) полностью закрыты. Идеальная дисциплина!",
                1.0f
            )
        }

        val resolvedSelected = if (!selectedDateStr.isNullOrBlank() && selectedDateStr in calendarMap) {
            selectedDateStr
        } else {
            todayDateStr
        }

        return WeeklyOverviewState(
            days = last7Days,
            heatmapDays = heatmapSummaries,
            calendarDaysByDate = calendarMap,
            selectedDate = resolvedSelected,
            totalRealCompletionsInWeek = totalWeek,
            totalRealCompletionsAllTime = totalAllTime,
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            isTodayActiveInStreak = isTodayActive,
            isStreakAtRiskToday = isStreakAtRisk,
            availableFreezeShields = availableFreezeShields,
            maxFreezeShields = 3,
            usedFreezeDates = effectiveFrozenDates,
            perfectDaysInMonth = perfectDaysMonth,
            activeDaysInMonth = activeDaysMonth,
            totalHoldSecondsInWeek = totalHoldWeek,
            totalHoldSecondsAllTime = totalHoldAllTime,
            totalWorkoutSecondsInWeek = totalWorkoutWeek,
            totalWorkoutSecondsAllTime = totalWorkoutAllTime,
            previousWeekCompletions = prevWeekCompletions,
            previousWeekHoldSeconds = prevWeekHoldSeconds,
            weekHoldDeltaSeconds = weekHoldDeltaSec,
            weekSessionsDelta = weekSessDelta,
            weekHoldDeltaPercent = weekHoldDeltaPct,
            weekDeltaBadgeTextRu = weekDeltaBadge,
            weekDeltaSubtitleRu = weekDeltaSubtitle,
            rankTitleRu = rankTitle,
            rankLevel = rankLevel,
            nextMilestoneDays = nextMilestone.targetDays,
            nextMilestoneTitleRu = nextMilestone.titleRu,
            milestoneProgressFraction = milestoneFraction,
            milestones = milestones,
            dailyQuestTitleRu = questTitle,
            dailyQuestSubtitleRu = questSubtitle,
            dailyQuestProgress = questProgress
        )
    }

    private fun dayOfWeekShortRu(isoDayOfWeek: Int): String = when (isoDayOfWeek) {
        1 -> "Пн"
        2 -> "Вт"
        3 -> "Ср"
        4 -> "Чт"
        5 -> "Пт"
        6 -> "Сб"
        7 -> "Вс"
        else -> "Дн"
    }

    private fun monthShortRu(month: Int): String = when (month) {
        1 -> "янв."
        2 -> "февр."
        3 -> "мар."
        4 -> "апр."
        5 -> "мая"
        6 -> "июн."
        7 -> "июл."
        8 -> "авг."
        9 -> "сент."
        10 -> "окт."
        11 -> "нояб."
        12 -> "дек."
        else -> ""
    }

    suspend fun updateCompletionDifficulty(slotId: String, difficulty: WorkoutDifficulty?) {
        dao.updateCompletionDifficulty(slotId = slotId, difficulty = difficulty?.code)
    }

    suspend fun isSlotCompleted(slotId: String): Boolean {
        return dao.getCompletionForSlot(slotId) != null
    }

    suspend fun ensureFreeTemplatesSeeded() {
        val existing = dao.getAllFreeTemplates()
        if (existing.isEmpty()) {
            dao.upsertFreeTemplates(FreeIntervalTemplateEntity.createInitialDefaults())
        }
    }

    suspend fun getAllFreeTemplates(): List<FreeIntervalTemplateEntity> {
        val list = dao.getAllFreeTemplates()
        return if (list.isEmpty()) {
            val defaults = FreeIntervalTemplateEntity.createInitialDefaults()
            dao.upsertFreeTemplates(defaults)
            defaults
        } else {
            list
        }
    }

    suspend fun saveFreeTemplate(template: FreeIntervalTemplateEntity) {
        dao.upsertFreeTemplate(template.copy(updatedAtEpochMs = timeProvider.currentWallTimeMs()))
    }

    // --- Продвинутый конструктор тренировок (программы из 4 типов блоков) ---

    suspend fun getAllCustomPrograms(): List<CustomWorkoutProgram> {
        val entities = dao.getAllCustomPrograms()
        if (entities.isEmpty()) {
            val defaults = CustomProgramJsonParser.createDefaultPrograms()
            dao.upsertCustomPrograms(defaults.mapIndexed { idx, prog -> CustomProgramJsonParser.toEntity(prog, idx) })
            return defaults
        }
        return entities.map { CustomProgramJsonParser.toDomain(it) }
    }

    suspend fun saveCustomProgram(program: CustomWorkoutProgram, sortOrder: Int = 0) {
        val updated = program.copy(updatedAtEpochMs = timeProvider.currentWallTimeMs())
        dao.upsertCustomProgram(CustomProgramJsonParser.toEntity(updated, sortOrder))
    }

    suspend fun deleteCustomProgram(id: String) {
        dao.deleteCustomProgram(id)
    }

    suspend fun saveRunCheckpoint(snapshot: PhaseEngineSnapshot) {
        val runId = snapshot.runId ?: return
        val slotId = snapshot.slotId ?: return
        val existing = dao.getRunById(runId)
        val nowWall = timeProvider.currentWallTimeMs()

        val entity = WorkoutRunEntity(
            id = runId,
            slotId = slotId,
            status = snapshot.status.name,
            startedAtEpochMs = existing?.startedAtEpochMs ?: nowWall,
            startDate = snapshot.startDate,
            zoneId = snapshot.zoneId,
            planVersion = existing?.planVersion ?: 1,
            titleRu = snapshot.protocol.titleRu,
            groupCode = snapshot.protocol.group.code,
            contractionMs = snapshot.protocol.contractionMs,
            relaxationMs = snapshot.protocol.relaxationMs,
            repetitions = snapshot.protocol.repetitions,
            isLastInGroup = snapshot.protocol.isLastInGroup,
            soundPackIdSnapshot = snapshot.soundPackSnapshot.id,
            metronomeStyleIdSnapshot = snapshot.metronomeStyleSnapshot.id,
            confirmedActiveMs = snapshot.confirmedActiveMs,
            currentRepetition = snapshot.currentRepetition,
            currentPhase = snapshot.currentPhase.name,
            phaseRemainingMs = snapshot.phaseRemainingMs,
            checkpointWallEpochMs = nowWall,
            bootSessionId = timeProvider.bootSessionId(),
            interruptionReason = snapshot.interruptionReason,
            isDemo = snapshot.isDemo,
            isFreeMode = snapshot.isFreeMode
        )
        if (existing == null) {
            dao.insertRun(entity)
        } else {
            dao.updateRun(entity)
        }
    }

    suspend fun completeRunAtomically(
        snapshot: PhaseEngineSnapshot,
        optionalDifficulty: String? = null
    ): Boolean {
        saveRunCheckpoint(snapshot)
        val runId = snapshot.runId ?: return false
        return dao.completeRunAtomically(
            runId = runId,
            completedAtEpochMs = timeProvider.currentWallTimeMs(),
            optionalDifficulty = optionalDifficulty
        )
    }

    suspend fun recoverInterruptedRunAfterProcessDeath(): WorkoutRunEntity? {
        val candidates = dao.getActiveOrInterruptedRuns()
        if (candidates.isEmpty()) return null

        val primary = candidates.first()
        for (extra in candidates.drop(1)) {
            dao.updateRun(
                extra.copy(
                    status = RunStatus.STOPPED.name,
                    interruptionReason = "Закрыто при проверке целостности единственной активной попытки"
                )
            )
        }

        if (dao.getCompletionForSlot(primary.slotId) != null) {
            dao.updateRun(primary.copy(status = RunStatus.COMPLETED.name))
            return null
        }

        val recovered = if (primary.status == RunStatus.PREPARING.name || primary.status == RunStatus.RUNNING.name) {
            val reason = if (primary.bootSessionId != timeProvider.bootSessionId()) {
                "Устройство перезагружалось — попытка сохранена на последней подтверждённой точке без автозапуска"
            } else {
                "Процесс приложения был выгружен системой — восстановлена последняя контрольная точка без времени простоя"
            }
            val updated = primary.copy(
                status = RunStatus.INTERRUPTED.name,
                interruptionReason = reason,
                checkpointWallEpochMs = timeProvider.currentWallTimeMs()
            )
            dao.updateRun(updated)
            updated
        } else {
            primary
        }

        return recovered
    }

    suspend fun countRealCompletions(): Int = dao.countCompletionsByDemoFlag(isDemo = false)
    suspend fun countDemoCompletions(): Int = dao.countCompletionsByDemoFlag(isDemo = true)

    fun slotToProtocolConfig(slot: DailySlotEntity): DemoProtocolConfig {
        val group = PlanGroup.fromCode(slot.groupCode)
        val displayTitle = if (slot.titleRu.startsWith(group.titleRu)) {
            slot.titleRu
        } else {
            "${group.titleRu} · ${slot.titleRu}"
        }
        return DemoProtocolConfig(
            id = slot.id,
            titleRu = displayTitle,
            contractionMs = slot.contractionMs,
            relaxationMs = slot.relaxationMs,
            repetitions = slot.repetitions,
            isLastInGroup = slot.isLastInGroup,
            group = group
        )
    }

    companion object {
        const val DEMO_PLAN_STANDARD_3 = "demo_plan_standard_3"
        const val DEMO_PLAN_ZERO_SLOTS = "demo_plan_zero_slots"
        const val REAL_PLAN_STANDARD_3 = "real_plan_standard_3"
        const val REAL_PLAN_CURRENT_VERSION = 2
    }
}
