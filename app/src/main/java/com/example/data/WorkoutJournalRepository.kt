package com.example.data

import androidx.room.withTransaction

/** Read-only projection: a completed run is credited ONLY when a real completion exists. */
data class WorkoutJournalEntry(val run: WorkoutRunEntity?, val completion: WorkoutCompletionEntity?, val slot: DailySlotEntity?) {
    val id: String get() = completion?.runId ?: requireNotNull(run).id
    val localDate: String get() = completion?.localDate ?: requireNotNull(run).startDate
    val title: String get() = run?.titleRu?.takeIf { it.isNotBlank() } ?: slot?.titleRu ?: "Сохранённое занятие"
    val credited: Boolean get() = completion != null
    val isFree: Boolean get() = completion?.isFreeMode ?: run?.isFreeMode ?: false
    val timestamp: Long get() = completion?.completedAtEpochMs ?: run?.checkpointWallEpochMs?.takeIf { it > 0 } ?: run?.startedAtEpochMs ?: 0
    val statusRu: String get() = if (credited) "Завершено" else when (run?.status) {
        "RUNNING", "PREPARING" -> "В процессе"
        "PAUSED" -> "Пауза"
        "INTERRUPTED" -> "Прервано"
        "STOPPED" -> "Остановлено"
        "COMPLETED" -> "Завершение не подтверждено"
        else -> "Сохранённая попытка"
    }
}

class WorkoutJournalRepository(private val database: VelarisDatabase) {
    suspend fun load(): List<WorkoutJournalEntry> = database.withTransaction {
        val dao = database.velarisDao()
        project(dao.getRealWorkoutRuns(), dao.getRealWorkoutCompletions(), dao.getRealDailySlots())
    }
    companion object {
        internal fun project(runs: List<WorkoutRunEntity>, completions: List<WorkoutCompletionEntity>, slots: List<DailySlotEntity>): List<WorkoutJournalEntry> {
            val realRuns = runs.filterNot { it.isDemo }.associateBy { it.id }
            val receipts = completions.filterNot { it.isDemo }.associateBy { it.runId }
            val actualSlots = slots.filterNot { it.isDemo }.associateBy { it.id }
            return (realRuns.keys + receipts.keys).map { id ->
                val run = realRuns[id]; val completion = receipts[id]
                WorkoutJournalEntry(run, completion, actualSlots[completion?.slotId ?: run?.slotId])
            }.sortedWith(compareByDescending<WorkoutJournalEntry> { it.localDate }.thenByDescending { it.timestamp }.thenBy { it.id })
        }
    }
}

data class WorkoutJournalState(val entries: List<WorkoutJournalEntry> = emptyList(), val loading: Boolean = false, val error: Boolean = false)
