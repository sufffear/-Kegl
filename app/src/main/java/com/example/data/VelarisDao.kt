package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.domain.RunStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VelarisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrainingPlan(plan: TrainingPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlanAssignments(assignments: List<PlanAssignmentEntity>)

    @Query("SELECT * FROM training_plans WHERE id = :planId LIMIT 1")
    suspend fun getTrainingPlanById(planId: String): TrainingPlanEntity?

    @Query("SELECT * FROM plan_assignments WHERE planId = :planId AND planVersion = :version ORDER BY sortOrder ASC")
    suspend fun getAssignmentsForPlan(planId: String, version: Int): List<PlanAssignmentEntity>

    @Query("SELECT * FROM daily_plans WHERE localDate = :localDate AND isDemo = :isDemo LIMIT 1")
    suspend fun getDailyPlan(localDate: String, isDemo: Boolean): DailyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyPlanIgnore(dailyPlan: DailyPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailySlotsIgnore(slots: List<DailySlotEntity>)

    @Query("SELECT * FROM daily_slots WHERE dailyPlanId = :dailyPlanId ORDER BY sortOrder ASC")
    suspend fun getSlotsForDailyPlan(dailyPlanId: String): List<DailySlotEntity>

    @Query("SELECT * FROM daily_slots WHERE id = :slotId LIMIT 1")
    suspend fun getSlotById(slotId: String): DailySlotEntity?

    @Query("SELECT * FROM workout_completions WHERE localDate = :localDate AND isDemo = :isDemo")
    suspend fun getCompletionsForDate(localDate: String, isDemo: Boolean): List<WorkoutCompletionEntity>

    @Query("SELECT * FROM workout_completions WHERE localDate = :localDate AND isDemo = :isDemo")
    fun observeCompletionsForDate(localDate: String, isDemo: Boolean): Flow<List<WorkoutCompletionEntity>>

    @Query("SELECT * FROM workout_completions WHERE slotId = :slotId LIMIT 1")
    suspend fun getCompletionForSlot(slotId: String): WorkoutCompletionEntity?

    @Query("SELECT COUNT(*) FROM workout_completions WHERE isDemo = :isDemo")
    suspend fun countCompletionsByDemoFlag(isDemo: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: WorkoutRunEntity)

    @Update
    suspend fun updateRun(run: WorkoutRunEntity)

    @Query("SELECT * FROM workout_runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: String): WorkoutRunEntity?

    @Query(
        "SELECT * FROM workout_runs WHERE status IN ('PREPARING', 'RUNNING', 'PAUSED', 'INTERRUPTED') " +
            "ORDER BY startedAtEpochMs DESC"
    )
    suspend fun getActiveOrInterruptedRuns(): List<WorkoutRunEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletionIgnore(completion: WorkoutCompletionEntity): Long

    @Query("UPDATE workout_completions SET optionalDifficulty = :difficulty WHERE slotId = :slotId")
    suspend fun updateCompletionDifficulty(slotId: String, difficulty: String?)

    /**
     * Завершение попытки и запись результата — одна атомарная Room-транзакция (ТЗ п. 9).
     * Повторный callback, быстрые нажатия или восстановление не создают второе завершение
     * благодаря уникальному индексу по `slotId` и проверке внутри транзакции.
     */
    @Transaction
    suspend fun completeRunAtomically(
        runId: String,
        completedAtEpochMs: Long,
        optionalDifficulty: String? = null
    ): Boolean {
        val run = getRunById(runId) ?: return false
        val totalMs = (run.contractionMs + run.relaxationMs) * run.repetitions

        val updatedRun = run.copy(
            status = RunStatus.COMPLETED.name,
            confirmedActiveMs = totalMs,
            phaseRemainingMs = 0L,
            checkpointWallEpochMs = completedAtEpochMs,
            interruptionReason = null
        )
        updateRun(updatedRun)

        val existingCompletion = getCompletionForSlot(run.slotId)
        if (existingCompletion != null) {
            return false
        }

        val slot = getSlotById(run.slotId)
        val completionDate = slot?.localDate ?: run.startDate

        val rowId = insertCompletionIgnore(
            WorkoutCompletionEntity(
                runId = run.id,
                slotId = run.slotId,
                localDate = completionDate,
                completedAtEpochMs = completedAtEpochMs,
                optionalDifficulty = optionalDifficulty,
                isDemo = run.isDemo
            )
        )
        return rowId != -1L
    }
}
