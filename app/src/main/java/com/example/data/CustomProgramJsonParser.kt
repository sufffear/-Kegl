package com.example.data

import com.example.domain.CustomSequenceStep
import com.example.domain.CustomWorkoutProgram
import com.example.domain.PhaseType
import com.example.domain.WorkoutBlock
import com.example.domain.WorkoutBlockType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object CustomProgramJsonParser {

    fun serializeBlocks(blocks: List<WorkoutBlock>): String {
        val rootArray = JSONArray()
        for (b in blocks) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("type", b.type.name)
            obj.put("name", b.name)
            obj.put("contractionSec", b.contractionSec)
            obj.put("relaxationSec", b.relaxationSec)
            obj.put("repetitions", b.repetitions)
            obj.put("sequenceCycles", b.sequenceCycles)

            val stepsArray = JSONArray()
            for (s in b.customSteps) {
                val stepObj = JSONObject()
                stepObj.put("id", s.id)
                stepObj.put("phaseType", s.phaseType.name)
                stepObj.put("durationSec", s.durationSec)
                stepsArray.put(stepObj)
            }
            obj.put("customSteps", stepsArray)
            rootArray.put(obj)
        }
        return rootArray.toString()
    }

    fun deserializeBlocks(jsonString: String): List<WorkoutBlock> {
        if (jsonString.isBlank() || jsonString == "[]") return emptyList()
        val result = mutableListOf<WorkoutBlock>()
        try {
            val rootArray = JSONArray(jsonString)
            for (i in 0 until rootArray.length()) {
                val obj = rootArray.getJSONObject(i)
                val type = runCatching { WorkoutBlockType.valueOf(obj.getString("type")) }
                    .getOrDefault(WorkoutBlockType.REGULAR)

                val customSteps = mutableListOf<CustomSequenceStep>()
                if (obj.has("customSteps")) {
                    val stepsArray = obj.getJSONArray("customSteps")
                    for (j in 0 until stepsArray.length()) {
                        val stepObj = stepsArray.getJSONObject(j)
                        val phase = runCatching { PhaseType.valueOf(stepObj.getString("phaseType")) }
                            .getOrDefault(PhaseType.CONTRACTION)
                        customSteps.add(
                            CustomSequenceStep(
                                id = stepObj.optString("id", UUID.randomUUID().toString()),
                                phaseType = phase,
                                durationSec = stepObj.optInt("durationSec", 5).coerceAtLeast(1)
                            )
                        )
                    }
                }

                result.add(
                    WorkoutBlock(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        type = type,
                        name = obj.optString("name", type.titleRu),
                        contractionSec = obj.optInt("contractionSec", 5).coerceIn(1, 120),
                        relaxationSec = obj.optInt("relaxationSec", 5).coerceIn(1, 120),
                        repetitions = obj.optInt("repetitions", 10).coerceIn(1, 100),
                        customSteps = customSteps,
                        sequenceCycles = obj.optInt("sequenceCycles", 1).coerceIn(1, 100)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun toDomain(entity: CustomProgramEntity): CustomWorkoutProgram {
        return CustomWorkoutProgram(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            blocks = deserializeBlocks(entity.blocksJson),
            createdAtEpochMs = entity.createdAtEpochMs,
            updatedAtEpochMs = entity.updatedAtEpochMs
        )
    }

    fun toEntity(program: CustomWorkoutProgram, sortOrder: Int = 0): CustomProgramEntity {
        return CustomProgramEntity(
            id = program.id,
            title = program.title,
            description = program.description,
            sortOrder = sortOrder,
            blocksJson = serializeBlocks(program.blocks),
            createdAtEpochMs = program.createdAtEpochMs,
            updatedAtEpochMs = program.updatedAtEpochMs
        )
    }

    /**
     * Предустановленные примеры программ для первого знакомства с конструктором.
     */
    fun createDefaultPrograms(): List<CustomWorkoutProgram> {
        val now = System.currentTimeMillis()
        return listOf(
            CustomWorkoutProgram(
                id = "default_prog_pyramid",
                title = "Пирамида выносливости",
                description = "Разминка обычными сокращениями, взрывные быстрые импульсы и мощное длительное удержание.",
                blocks = listOf(
                    WorkoutBlock(
                        id = "block_pyr_1",
                        type = WorkoutBlockType.REGULAR,
                        name = "Разминка: Обычные сокращения",
                        contractionSec = 5,
                        relaxationSec = 5,
                        repetitions = 6
                    ),
                    WorkoutBlock(
                        id = "block_pyr_2",
                        type = WorkoutBlockType.SHORT_PULSE,
                        name = "Импульсы: Быстрый темп",
                        contractionSec = 2,
                        relaxationSec = 2,
                        repetitions = 12
                    ),
                    WorkoutBlock(
                        id = "block_pyr_3",
                        type = WorkoutBlockType.LONG_HOLD,
                        name = "Пик: Длительное удержание",
                        contractionSec = 20,
                        relaxationSec = 15,
                        repetitions = 3
                    )
                ),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            ),
            CustomWorkoutProgram(
                id = "default_prog_ladder",
                title = "Лесенка контрастов",
                description = "Свободная последовательность с постепенным увеличением времени удержания: 4с -> 8с -> 12с -> 16с.",
                blocks = listOf(
                    WorkoutBlock(
                        id = "block_lad_1",
                        type = WorkoutBlockType.CUSTOM_SEQUENCE,
                        name = "Лесенка фаз",
                        customSteps = listOf(
                            CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 4),
                            CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 4),
                            CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 8),
                            CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 6),
                            CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 12),
                            CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 8),
                            CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 16),
                            CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 10)
                        ),
                        sequenceCycles = 2
                    )
                ),
                createdAtEpochMs = now + 1,
                updatedAtEpochMs = now + 1
            )
        )
    }
}
