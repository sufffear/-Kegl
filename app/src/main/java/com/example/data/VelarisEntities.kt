package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * План тренировок (ТЗ п. 6, п. 9):
 * TrainingPlan(id, version, source, effectiveFrom, assignments).
 */
@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val titleRu: String,
    val source: String,
    val effectiveFromDate: String,
    val isDemo: Boolean
)

/**
 * Назначение внутри плана тренировок. Число назначений в плане переменное (может быть 0, 2, 3 и т.д.).
 */
@Entity(
    tableName = "plan_assignments",
    indices = [Index(value = ["planId", "planVersion"])]
)
data class PlanAssignmentEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val planVersion: Int,
    val groupCode: String,
    val sortOrder: Int,
    val titleRu: String,
    val contractionMs: Long,
    val relaxationMs: Long,
    val repetitions: Int,
    val isDemo: Boolean
)

/**
 * Снимок назначений конкретного календарного дня (ТЗ п. 6, п. 9):
 * DailyPlan(id, localDate, planVersion).
 * Демо-данные отделены признаком `isDemo`.
 */
@Entity(
    tableName = "daily_plans",
    indices = [Index(value = ["localDate", "isDemo"], unique = true)]
)
data class DailyPlanEntity(
    @PrimaryKey val id: String,
    val localDate: String,
    val planId: String,
    val planVersion: Int,
    val isDemo: Boolean
)

/**
 * Слот дневного плана (ТЗ п. 9):
 * DailySlot(id, dailyPlanId, assignmentId, group, protocolSnapshot);
 * уникальность `dailyPlanId + assignmentId`.
 */
@Entity(
    tableName = "daily_slots",
    indices = [
        Index(value = ["dailyPlanId", "assignmentId"], unique = true),
        Index(value = ["dailyPlanId"])
    ]
)
data class DailySlotEntity(
    @PrimaryKey val id: String,
    val dailyPlanId: String,
    val localDate: String,
    val assignmentId: String,
    val groupCode: String,
    val sortOrder: Int,
    val titleRu: String,
    val contractionMs: Long,
    val relaxationMs: Long,
    val repetitions: Int,
    val isLastInGroup: Boolean,
    val isDemo: Boolean
)

/**
 * Попытка выполнения подхода (ТЗ п. 9):
 * WorkoutRun(id, slotId, status, startedAt, startDate, zoneId, protocolSnapshot,
 * confirmedActiveMs, phaseState, checkpoint, interruptionReason, demo).
 */
@Entity(
    tableName = "workout_runs",
    indices = [
        Index(value = ["slotId"]),
        Index(value = ["status"]),
        Index(value = ["startDate", "isDemo"])
    ]
)
data class WorkoutRunEntity(
    @PrimaryKey val id: String,
    val slotId: String,
    val status: String,
    val startedAtEpochMs: Long,
    val startDate: String,
    val zoneId: String,
    val planVersion: Int,
    val titleRu: String,
    val groupCode: String,
    val contractionMs: Long,
    val relaxationMs: Long,
    val repetitions: Int,
    val isLastInGroup: Boolean,
    val soundPackIdSnapshot: String,
    val metronomeStyleIdSnapshot: String,
    val confirmedActiveMs: Long,
    val currentRepetition: Int,
    val currentPhase: String,
    val phaseRemainingMs: Long,
    val checkpointWallEpochMs: Long,
    val bootSessionId: String,
    val interruptionReason: String?,
    val isDemo: Boolean
)

/**
 * Завершение слота (ТЗ п. 9):
 * WorkoutCompletion(runId, slotId UNIQUE, completedAt, optionalDifficulty).
 * Уникальный индекс по `slotId` гарантирует отсутствие двойного зачёта одного и того же слота.
 */
@Entity(
    tableName = "workout_completions",
    indices = [Index(value = ["slotId"], unique = true)]
)
data class WorkoutCompletionEntity(
    @PrimaryKey val runId: String,
    val slotId: String,
    val localDate: String,
    val completedAtEpochMs: Long,
    val optionalDifficulty: String?,
    val isDemo: Boolean
)

/**
 * Представление слота дневного плана вместе со статусом его завершения.
 * Галочка выполнения в UI вычисляется строго из наличия записи в `workout_completions`.
 */
data class DailySlotWithCompletion(
    val slot: DailySlotEntity,
    val completion: WorkoutCompletionEntity?
) {
    val isCompleted: Boolean
        get() = completion != null
}
