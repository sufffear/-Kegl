package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.DemoProtocolConfig

/** Stable assignment/type identifiers, never localized titles or list positions. */
@Entity(tableName = "training_configurations")
data class TrainingConfiguration(
    @PrimaryKey val key: String,
    val contractionSec: Int,
    val relaxationSec: Int,
    val repetitions: Int
) {
    val isValid: Boolean get() = contractionSec in 1..120 && relaxationSec in 1..120 &&
        repetitions in 1..300 && durationMs <= 5_400_000L
    val durationMs: Long get() = DemoProtocolConfig(key, "", contractionSec * 1000L,
        relaxationSec * 1000L, repetitions).totalActiveDurationMs
    val holdMs: Long get() = contractionSec * 1000L * repetitions
    fun applyTo(slot: DailySlotEntity) = slot.copy(contractionMs=contractionSec*1000L,
        relaxationMs=relaxationSec*1000L,repetitions=repetitions)
    fun applyTo(template: FreeIntervalTemplateEntity): FreeIntervalTemplateEntity {
        val seconds = (durationMs / 1000).toInt()
        return template.copy(contractionSec=contractionSec,relaxationSec=relaxationSec,
            targetDurationMinutes=seconds/60,targetDurationSeconds=seconds%60)
    }
    companion object {
        fun from(slot: DailySlotEntity) = TrainingConfiguration("mandatory:${slot.assignmentId}",
            (slot.contractionMs/1000).toInt(),(slot.relaxationMs/1000).toInt(),slot.repetitions)
        fun from(template: FreeIntervalTemplateEntity) = TrainingConfiguration("extra:${template.slotIndex}",
            template.contractionSec,template.relaxationSec,template.calculatedCycles)
    }
}

/** Completion and this aggregate are written by the same Room transaction. */
@Entity(tableName = "extra_training_cycle")
data class ExtraTrainingCycle(
    @PrimaryKey val id: Int = 1,
    val round: Long = 0,
    val mask: Int = 0,
    val celebrationUntilEpochMs: Long = 0
) {
    fun isLocked(templateId: Int): Boolean = templateId in 101..103 && mask and (1 shl (templateId-101)) != 0
    fun complete(templateId: Int, now: Long): ExtraTrainingCycle {
        if (templateId !in 101..103 || isLocked(templateId)) return this
        val nextMask = mask or (1 shl (templateId-101))
        return copy(mask=nextMask,celebrationUntilEpochMs=if(nextMask==7) now+3000 else 0)
    }
}

/** Immutable executed parameters, including the complete constructor block sequence. */
@Entity(tableName = "workout_execution_parameters")
data class WorkoutExecutionParameters(
    @PrimaryKey val runId: String,
    val parametersJson: String,
    val totalMs: Long,
    val holdMs: Long
)
