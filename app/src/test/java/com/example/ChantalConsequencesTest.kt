package com.example
import com.example.data.*
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalConsequencesTest {
    @Test fun chosenConsequencesSurviveRestartAndApplyOnlyOnce() {
        val proposals = JSONArray().put(JSONObject().put("choiceIndex", 1).put("kind", "promise").put("descriptionRu", "Отнести нитку воробью").put("targetId", ""))
        val memory = ChantalWorldMemory.empty()
        ChantalGeneratedConsequences.validate(proposals, memory)
        val pending = ChantalOfflineAdventure.previewBase().copy(narrativeConsequences=proposals.toString())
        assertEquals(0, ChantalGeneratedConsequences.apply(memory, listOf(pending)).getJSONArray("promises").length())
        val accepted = ChantalProgressCodec.decodeChantalStoryEpisode(ChantalProgressCodec.encode(pending.copy(chosenOptionIndex=1, chosenActionSummaryRu="Помочь воробью")))
        val confirmed = ChantalWorldMemory.reconcile(memory, JSONArray().put(ChantalProgressCodec.encode(accepted)))
        val applied = ChantalGeneratedConsequences.apply(confirmed, listOf(accepted))
        assertEquals(1, applied.getJSONArray("promises").length())
        assertEquals(applied.toString(), ChantalGeneratedConsequences.apply(applied, listOf(accepted)).toString())
        assertEquals(0, ChantalGeneratedConsequences.apply(confirmed, listOf(accepted.copy(chosenOptionIndex=0))).getJSONArray("promises").length())
    }
    @Test fun commandsAndUnknownTargetsAreRejected() {
        for (kind in listOf("coins", "resolve")) {
            val proposals = JSONArray().put(JSONObject().put("choiceIndex", 0).put("kind", kind).put("descriptionRu", "Факт").put("targetId", "unknown"))
            assertTrue(runCatching { ChantalGeneratedConsequences.validate(proposals, ChantalWorldMemory.empty()) }.isFailure)
        }
    }
}
