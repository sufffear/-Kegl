package com.example

import com.example.domain.ChantalCharacterState
import com.example.domain.ChantalForm
import com.example.domain.resolveChantalStageStatus
import org.junit.Assert.*
import org.junit.Test

class ChantalAgeProgressTest {
    @Test fun adventureBoundariesMatchCanonicalForm() {
        listOf(0 to 1, 9 to 1, 10 to 2, 49 to 2, 50 to 3, 99 to 3, 100 to 4, 120 to 4).forEach { (count, expected) ->
            val stage = resolveChantalStageStatus(count, maturityXp = 0)
            assertEquals("Adventures: $count", expected, stage.stageNumber)
            assertEquals(ChantalCharacterState.fromAdventures(count).form == ChantalForm.HUMAN, stage.isTransformed)
            if (!stage.isTransformed) assertTrue(stage.nextStageTargetJourneys > count)
        }
    }

    @Test fun experienceNeverAcceleratesAgeOrTransformation() {
        listOf(0, 9, 10, 49, 50, 99, 100).forEach { count ->
            val baseline = resolveChantalStageStatus(count, maturityXp = 0)
            listOf(3, 999, 1000, 100000, Int.MAX_VALUE).forEach { xp ->
                assertEquals(baseline, resolveChantalStageStatus(count, maturityXp = xp))
            }
        }
    }

    @Test fun inspectionPreviewDoesNotChangeNormalResolution() {
        assertEquals(4, resolveChantalStageStatus(0, forceTransformPreview = true).stageNumber)
        assertEquals(1, resolveChantalStageStatus(0).stageNumber)
    }
}
