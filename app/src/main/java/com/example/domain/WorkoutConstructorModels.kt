package com.example.domain

import java.util.UUID

/**
 * 4 типа блоков согласно требованиям продвинутого конструктора:
 * 1. Обычные сокращения — удержание, расслабление, количество повторений.
 * 2. Короткие сокращения — короткие настраиваемые интервалы.
 * 3. Длительные удержания — продолжительные интервалы с отдыхом.
 * 4. Произвольная последовательность — пользователь самостоятельно задаёт порядок и длительность фаз.
 */
enum class WorkoutBlockType(val titleRu: String, val descriptionRu: String) {
    REGULAR(
        titleRu = "Обычные сокращения",
        descriptionRu = "Классический ритм: удержание, расслабление, повторы"
    ),
    SHORT_PULSE(
        titleRu = "Короткие сокращения",
        descriptionRu = "Быстрые частые импульсы для реактивности мышц"
    ),
    LONG_HOLD(
        titleRu = "Длительные удержания",
        descriptionRu = "Продолжительные фазы напряжения с глубоким отдыхом"
    ),
    CUSTOM_SEQUENCE(
        titleRu = "Произвольная последовательность",
        descriptionRu = "Свободный порядок и произвольная длина каждой фазы"
    )
}

/**
 * Отдельный шаг произвольной последовательности.
 */
data class CustomSequenceStep(
    val id: String = UUID.randomUUID().toString(),
    val phaseType: PhaseType = PhaseType.CONTRACTION,
    val durationSec: Int = 5
) {
    val durationMs: Long get() = durationSec * 1000L
}

/**
 * Блок тренировочной программы.
 */
data class WorkoutBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: WorkoutBlockType = WorkoutBlockType.REGULAR,
    val name: String = type.titleRu,
    // Для REGULAR, SHORT_PULSE, LONG_HOLD:
    val contractionSec: Int = when (type) {
        WorkoutBlockType.REGULAR -> 5
        WorkoutBlockType.SHORT_PULSE -> 2
        WorkoutBlockType.LONG_HOLD -> 20
        WorkoutBlockType.CUSTOM_SEQUENCE -> 5
    },
    val relaxationSec: Int = when (type) {
        WorkoutBlockType.REGULAR -> 5
        WorkoutBlockType.SHORT_PULSE -> 2
        WorkoutBlockType.LONG_HOLD -> 15
        WorkoutBlockType.CUSTOM_SEQUENCE -> 5
    },
    val repetitions: Int = when (type) {
        WorkoutBlockType.REGULAR -> 10
        WorkoutBlockType.SHORT_PULSE -> 15
        WorkoutBlockType.LONG_HOLD -> 3
        WorkoutBlockType.CUSTOM_SEQUENCE -> 1
    },
    // Для CUSTOM_SEQUENCE:
    val customSteps: List<CustomSequenceStep> = emptyList(),
    // Количество повторов всей последовательности для CUSTOM_SEQUENCE:
    val sequenceCycles: Int = 1
) {
    /**
     * Расчёт суммарной длительности блока в секундах.
     */
    val totalDurationSec: Int
        get() = when (type) {
            WorkoutBlockType.REGULAR,
            WorkoutBlockType.SHORT_PULSE,
            WorkoutBlockType.LONG_HOLD -> (contractionSec + relaxationSec) * repetitions

            WorkoutBlockType.CUSTOM_SEQUENCE -> {
                val singlePassSec = customSteps.sumOf { it.durationSec }
                singlePassSec * sequenceCycles
            }
        }

    val totalDurationMs: Long get() = totalDurationSec * 1000L

    /**
     * Разворачивание блока в плоский список атомарных шагов для PhaseEngine.
     */
    fun expandToExecutionSteps(): List<ExpandedPhaseStep> {
        val result = mutableListOf<ExpandedPhaseStep>()
        when (type) {
            WorkoutBlockType.REGULAR,
            WorkoutBlockType.SHORT_PULSE,
            WorkoutBlockType.LONG_HOLD -> {
                for (rep in 1..repetitions) {
                    result.add(
                        ExpandedPhaseStep(
                            blockId = id,
                            blockName = name,
                            phaseType = PhaseType.CONTRACTION,
                            durationMs = contractionSec * 1000L,
                            cycleIndex = rep,
                            totalCyclesInBlock = repetitions
                        )
                    )
                    result.add(
                        ExpandedPhaseStep(
                            blockId = id,
                            blockName = name,
                            phaseType = PhaseType.RELAXATION,
                            durationMs = relaxationSec * 1000L,
                            cycleIndex = rep,
                            totalCyclesInBlock = repetitions
                        )
                    )
                }
            }

            WorkoutBlockType.CUSTOM_SEQUENCE -> {
                val stepsToUse = if (customSteps.isEmpty()) {
                    listOf(
                        CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = contractionSec),
                        CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = relaxationSec)
                    )
                } else customSteps

                for (cycle in 1..sequenceCycles) {
                    stepsToUse.forEachIndexed { stepIdx, step ->
                        result.add(
                            ExpandedPhaseStep(
                                blockId = id,
                                blockName = name,
                                phaseType = step.phaseType,
                                durationMs = step.durationMs,
                                cycleIndex = cycle,
                                totalCyclesInBlock = sequenceCycles,
                                stepInSequenceIndex = stepIdx + 1,
                                totalStepsInSequence = stepsToUse.size
                            )
                        )
                    }
                }
            }
        }
        return result
    }
}

/**
 * Атомарный шаг исполнения, передаваемый в PhaseEngine.
 */
data class ExpandedPhaseStep(
    val blockId: String,
    val blockName: String,
    val phaseType: PhaseType,
    val durationMs: Long,
    val cycleIndex: Int,
    val totalCyclesInBlock: Int,
    val stepInSequenceIndex: Int = 1,
    val totalStepsInSequence: Int = 1
)

/**
 * Программа тренировки, состоящая из набора блоков.
 */
data class CustomWorkoutProgram(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val blocks: List<WorkoutBlock> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val totalDurationSec: Int get() = blocks.sumOf { it.totalDurationSec }
    val totalDurationMs: Long get() = totalDurationSec * 1000L
    val totalCycles: Int get() = blocks.sumOf {
        if (it.type == WorkoutBlockType.CUSTOM_SEQUENCE) it.sequenceCycles else it.repetitions
    }

    fun formattedTotalDuration(): String {
        val min = totalDurationSec / 60
        val sec = totalDurationSec % 60
        return if (min > 0) "$min мин $sec с" else "$sec с"
    }

    fun expandAllSteps(): List<ExpandedPhaseStep> = blocks.flatMap { it.expandToExecutionSteps() }
}
