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

    @Query("SELECT * FROM daily_slots WHERE localDate IN (:localDates) AND isDemo = :isDemo ORDER BY localDate ASC, sortOrder ASC")
    suspend fun getSlotsForDates(localDates: List<String>, isDemo: Boolean): List<DailySlotEntity>

    @Query("SELECT * FROM daily_slots WHERE id = :slotId LIMIT 1")
    suspend fun getSlotById(slotId: String): DailySlotEntity?

    @Query("SELECT * FROM workout_completions WHERE localDate = :localDate AND isDemo = :isDemo")
    suspend fun getCompletionsForDate(localDate: String, isDemo: Boolean): List<WorkoutCompletionEntity>

    @Query("SELECT * FROM workout_completions WHERE localDate IN (:localDates) AND isDemo = :isDemo ORDER BY completedAtEpochMs ASC")
    suspend fun getCompletionsForDates(localDates: List<String>, isDemo: Boolean): List<WorkoutCompletionEntity>

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

    // --- Универсальный офлайн-читатель линейного романа из Markdown (Update v1.1 п. 5–7) ---

    @Query("SELECT * FROM imported_novels WHERE slotKey = :slotKey LIMIT 1")
    suspend fun getActiveNovel(slotKey: String = ImportedNovelEntity.ACTIVE_SLOT_KEY): ImportedNovelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedNovel(novel: ImportedNovelEntity)

    @Query("SELECT * FROM novel_chapters WHERE bookId = :bookId ORDER BY chapterNumber ASC")
    suspend fun getNovelChapters(bookId: String): List<NovelChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovelChapters(chapters: List<NovelChapterEntity>)

    @Query("SELECT * FROM novel_blocks WHERE bookId = :bookId ORDER BY globalOrder ASC")
    suspend fun getNovelBlocksForBook(bookId: String): List<NovelBlockEntity>

    @Query("SELECT * FROM novel_blocks WHERE bookId = :bookId AND chapterId = :chapterId ORDER BY orderInChapter ASC")
    suspend fun getNovelBlocksForChapter(bookId: String, chapterId: String): List<NovelBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovelBlocks(blocks: List<NovelBlockEntity>)

    @Query("SELECT * FROM novel_reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getNovelReadingProgress(bookId: String): NovelReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNovelReadingProgress(progress: NovelReadingProgressEntity)

    @Query("DELETE FROM imported_novels")
    suspend fun deleteAllImportedNovels()

    @Query("DELETE FROM novel_chapters")
    suspend fun deleteAllNovelChapters()

    @Query("DELETE FROM novel_blocks")
    suspend fun deleteAllNovelBlocks()

    @Query("DELETE FROM novel_reading_progress WHERE bookId = :bookId")
    suspend fun deleteNovelReadingProgress(bookId: String)

    @Query("DELETE FROM novel_reading_progress")
    suspend fun deleteAllNovelReadingProgress()

    /**
     * Атомарная установка или замена импортированного романа вместе с производными главами,
     * блоками и начальной позицией чтения.
     */
    @Transaction
    suspend fun replaceActiveNovelAtomically(
        novel: ImportedNovelEntity,
        chapters: List<NovelChapterEntity>,
        blocks: List<NovelBlockEntity>,
        initialProgress: NovelReadingProgressEntity
    ) {
        deleteAllNovelBlocks()
        deleteAllNovelChapters()
        deleteAllImportedNovels()
        deleteAllNovelReadingProgress()
        upsertImportedNovel(novel)
        insertNovelChapters(chapters)
        insertNovelBlocks(blocks)
        upsertNovelReadingProgress(initialProgress)
    }

    @Transaction
    suspend fun clearImportedNovelAtomically() {
        deleteAllNovelBlocks()
        deleteAllNovelChapters()
        deleteAllImportedNovels()
        deleteAllNovelReadingProgress()
    }

    // --- Резервное копирование и атомарное восстановление данных (v1.2 п. 1–2) ---

    @Query("SELECT * FROM training_plans WHERE isDemo = 0")
    suspend fun getRealTrainingPlans(): List<TrainingPlanEntity>

    @Query("SELECT * FROM plan_assignments WHERE isDemo = 0")
    suspend fun getRealPlanAssignments(): List<PlanAssignmentEntity>

    @Query("SELECT * FROM daily_plans WHERE isDemo = 0")
    suspend fun getRealDailyPlans(): List<DailyPlanEntity>

    @Query("SELECT * FROM daily_slots WHERE isDemo = 0")
    suspend fun getRealDailySlots(): List<DailySlotEntity>

    @Query("SELECT * FROM workout_runs WHERE isDemo = 0")
    suspend fun getRealWorkoutRuns(): List<WorkoutRunEntity>

    @Query("SELECT * FROM workout_completions WHERE isDemo = 0")
    suspend fun getRealWorkoutCompletions(): List<WorkoutCompletionEntity>

    @Query("DELETE FROM training_plans WHERE isDemo = 0")
    suspend fun deleteRealTrainingPlans()

    @Query("DELETE FROM plan_assignments WHERE isDemo = 0")
    suspend fun deleteRealPlanAssignments()

    @Query("DELETE FROM daily_plans WHERE isDemo = 0")
    suspend fun deleteRealDailyPlans()

    @Query("DELETE FROM daily_slots WHERE isDemo = 0")
    suspend fun deleteRealDailySlots()

    @Query("DELETE FROM workout_runs WHERE isDemo = 0")
    suspend fun deleteRealWorkoutRuns()

    @Query("DELETE FROM workout_completions WHERE isDemo = 0")
    suspend fun deleteRealWorkoutCompletions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingPlans(plans: List<TrainingPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlans(plans: List<DailyPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySlots(slots: List<DailySlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutRuns(runs: List<WorkoutRunEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutCompletions(completions: List<WorkoutCompletionEntity>)

    /**
     * Атомарная замена реальных тренировочных данных и романа (v1.2 п. 2).
     * Демонстрационные данные не затрагиваются; все реальные данные перезаписываются
     * в рамках одной Room-транзакции без частичных состояний.
     */
    @Transaction
    suspend fun restoreUserDataAtomically(
        plans: List<TrainingPlanEntity>,
        assignments: List<PlanAssignmentEntity>,
        dailyPlans: List<DailyPlanEntity>,
        slots: List<DailySlotEntity>,
        runs: List<WorkoutRunEntity>,
        completions: List<WorkoutCompletionEntity>,
        novel: ImportedNovelEntity?,
        chapters: List<NovelChapterEntity>,
        blocks: List<NovelBlockEntity>,
        progress: NovelReadingProgressEntity?
    ) {
        deleteRealWorkoutCompletions()
        deleteRealWorkoutRuns()
        deleteRealDailySlots()
        deleteRealDailyPlans()
        deleteRealPlanAssignments()
        deleteRealTrainingPlans()

        if (plans.isNotEmpty()) insertTrainingPlans(plans)
        if (assignments.isNotEmpty()) upsertPlanAssignments(assignments)
        if (dailyPlans.isNotEmpty()) insertDailyPlans(dailyPlans)
        if (slots.isNotEmpty()) insertDailySlots(slots)
        if (runs.isNotEmpty()) insertWorkoutRuns(runs)
        if (completions.isNotEmpty()) insertWorkoutCompletions(completions)

        clearImportedNovelAtomically()
        if (novel != null) {
            upsertImportedNovel(novel)
            if (chapters.isNotEmpty()) insertNovelChapters(chapters)
            if (blocks.isNotEmpty()) insertNovelBlocks(blocks)
            if (progress != null) upsertNovelReadingProgress(progress)
        }
    }
}
