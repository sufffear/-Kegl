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
    val isDemo: Boolean,
    val isFreeMode: Boolean = false
)

/**
 * Завершение слота (ТЗ п. 9, Итерация v1.3 п. 4):
 * WorkoutCompletion(runId, slotId UNIQUE, completedAt, optionalDifficulty, isDemo, isFreeMode).
 * Уникальный индекс по `slotId` гарантирует отсутствие двойного зачёта одного и того же слота.
 */
@Entity(
    tableName = "workout_completions",
    indices = [
        Index(value = ["slotId"], unique = true),
        Index(value = ["localDate", "isDemo", "isFreeMode"])
    ]
)
data class WorkoutCompletionEntity(
    @PrimaryKey val runId: String,
    val slotId: String,
    val localDate: String,
    val completedAtEpochMs: Long,
    val optionalDifficulty: String?,
    val isDemo: Boolean,
    val isFreeMode: Boolean = false
)

/**
 * Шаблон свободного интервального режима (Итерация v1.3 п. 2).
 * 5 слотов настроек (1..5): «Быстрый», «Медленный», «Длительное удержание», «Мой вариант 4», «Мой вариант 5».
 */
@Entity(tableName = "free_interval_templates")
data class FreeIntervalTemplateEntity(
    @PrimaryKey val slotIndex: Int,
    val name: String,
    val targetDurationMinutes: Int = 3,
    val targetDurationSeconds: Int = 0,
    val contractionSec: Int = 0,
    val relaxationSec: Int = 0,
    val isConfigured: Boolean = false,
    val updatedAtEpochMs: Long = 0L
) {
    val targetMinutes: Int get() = targetDurationMinutes
    val targetSeconds: Int get() = targetDurationSeconds
    val targetDurationSec: Int get() = totalDesiredSec
    val totalDesiredSec: Int get() = (targetDurationMinutes * 60 + targetDurationSeconds).coerceAtLeast(0)
    val cycleSec: Int get() = contractionSec + relaxationSec
    val calculatedCycles: Int get() = if (cycleSec > 0) totalDesiredSec / cycleSec else 0
    val actualDurationSec: Int get() = calculatedCycles * cycleSec
    val isValid: Boolean
        get() = isConfigured &&
            name.isNotBlank() &&
            contractionSec in 1..120 &&
            relaxationSec in 1..120 &&
            totalDesiredSec in 1..5400 &&
            calculatedCycles in 1..300

    companion object {
        val DEFAULT_INITIAL_NAMES = listOf(
            "Быстрый",
            "Медленный",
            "Длительное удержание",
            "Мой вариант 4",
            "Мой вариант 5"
        )

        fun createInitialDefaults(): List<FreeIntervalTemplateEntity> {
            return DEFAULT_INITIAL_NAMES.mapIndexed { idx, title ->
                val (cSec, rSec) = when (idx) {
                    0 -> 3 to 3
                    1 -> 10 to 10
                    2 -> 20 to 10
                    else -> 5 to 5
                }
                FreeIntervalTemplateEntity(
                    slotIndex = idx,
                    name = title,
                    targetDurationMinutes = 3,
                    targetDurationSeconds = 0,
                    contractionSec = cSec,
                    relaxationSec = rSec,
                    isConfigured = true,
                    updatedAtEpochMs = 0L
                )
            }
        }
    }
}

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

/**
 * Метаданные импортированного локального линейного романа в Markdown (Update v1.1 п. 5–7).
 * Исходные байты хранятся без изменений во внутреннем хранилище по пути `internalFilePath`
 * и проверяются по контрольной сумме `sha256Hex`; производные блоки для отображения хранятся отдельно.
 */
@Entity(tableName = "imported_novels")
data class ImportedNovelEntity(
    @PrimaryKey val slotKey: String = ACTIVE_SLOT_KEY,
    val bookId: String,
    val fileName: String,
    val explicitTitle: String?,
    val displayTitle: String,
    val sha256Hex: String,
    val byteSize: Int,
    val internalFilePath: String,
    val actHeadingLevel: Int,
    val chapterHeadingLevel: Int,
    val actCount: Int,
    val chapterCount: Int,
    val totalBlockCount: Int,
    val importedAtEpochMs: Long
) {
    companion object {
        const val ACTIVE_SLOT_KEY = "ACTIVE_NOVEL"
    }
}

/**
 * Производная запись главы импортированного романа для быстрой навигации акт → глава.
 */
@Entity(
    tableName = "novel_chapters",
    indices = [Index(value = ["bookId", "chapterNumber"])]
)
data class NovelChapterEntity(
    @PrimaryKey val chapterId: String,
    val bookId: String,
    val chapterNumber: Int,
    val actId: String,
    val actNumber: Int,
    val actTitle: String,
    val title: String,
    val blockCount: Int,
    val stepCount: Int
)

/**
 * Производный блок отображения внутри главы импортированного романа.
 * Хранится отдельно от неизменного файла исходных байтов.
 */
@Entity(
    tableName = "novel_blocks",
    indices = [
        Index(value = ["bookId", "chapterId", "orderInChapter"]),
        Index(value = ["bookId", "globalOrder"])
    ]
)
data class NovelBlockEntity(
    @PrimaryKey val blockId: String,
    val bookId: String,
    val chapterId: String,
    val orderInChapter: Int,
    val globalOrder: Int,
    val stepIndex: Int,
    val stepId: String,
    val blockType: String,
    val subheadingLevel: Int,
    val plainText: String,
    val inlineSpansEncoded: String
)

/**
 * Сохранённая позиция чтения для импортированного линейного романа (Update v1.1 п. 7).
 * Хранит стабильные ID главы и блока (`currentChapterId`, `currentBlockId`) и индекс шага (`currentStepIndex`),
 * поэтому переключение между режимами «Книга» и «Пошаговый» возвращает читателя к тому же самому фрагменту.
 */
@Entity(tableName = "novel_reading_progress")
data class NovelReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val currentActId: String,
    val currentChapterId: String,
    val currentBlockId: String,
    val currentStepIndex: Int,
    val readerModeId: String,
    val isCompleted: Boolean,
    val updatedAtEpochMs: Long
)

/**
 * Устаревшая таблица схемы v2 (сохранена для обратной совместимости миграций `MIGRATION_1_2` -> `MIGRATION_2_3`).
 */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val storyId: String,
    val currentChapterId: String,
    val currentNodeId: String,
    val readerModeId: String,
    val isCompleted: Boolean,
    val updatedAtEpochMs: Long
)
