package com.example

import com.example.data.*
import com.example.domain.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalJourneyConfirmationTest {
    private fun snapshot(ep: ChantalStoryEpisode) = JSONObject().put("pendingEpisode", ChantalProgressCodec.encode(ep))
        .put("coins", 100).put("maturityXp", 70).put("totalJourneys", 7).put("debtCoins", 9)
        .put("debtReasonRu", "Старый долг").put("bonusEnergy", 23).put("spentEnergy", 30)
        .put("curiosity", 99).put("tenderness", 6).put("playfulness", 4)
        .put("romanticism", 3).put("courage", 5).put("wit", 5)

    @Test fun choiceAppliesExistingRewardsOnceAndPreservesInput() {
        val ep = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(coinsEarned = 21, causesExhaustion45 = true, debtAmountCoins = 12, badNewsRu = "Новый долг")
        val original = snapshot(ep)
        val before = original.toString()
        val r = ChantalJourneyConfirmation.apply(original, ep.id, choiceIndex = 0)
        assertTrue(r.applied)
        assertEquals(before, original.toString())
        assertEquals(121, r.snapshot.getInt("coins"))
        assertEquals(80, r.snapshot.getInt("maturityXp"))
        assertEquals(8, r.snapshot.getInt("totalJourneys"))
        assertEquals(21, r.snapshot.getInt("debtCoins"))
        assertTrue(r.snapshot.getBoolean("isExhausted45"))
        assertEquals(23, r.snapshot.getInt("bonusEnergy"))
        assertEquals(30, r.snapshot.getInt("spentEnergy"))
        assertEquals(1, r.snapshot.getJSONArray("inventoryList").length())
        val expected = ChantalTraits(99,6,4,3,5,5).applyCanonicalDelta(ep.choices[0].delta)
        assertEquals(expected.curiosity, r.snapshot.getInt("curiosity"))
        val repeat = ChantalJourneyConfirmation.apply(r.snapshot, ep.id, choiceIndex = 1)
        assertFalse(repeat.applied)
        assertEquals(r.snapshot.toString(), repeat.snapshot.toString())
    }
    @Test fun chestAndFillerRetainTheirDistinctXpAndDebtRules() {
        val chest = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(isMilestoneChest = true)
        assertEquals(15, ChantalJourneyConfirmation.apply(snapshot(chest), chest.id, choiceIndex = 0).xpEarned)
        val filler = chest.copy(isFiller = true, causesExhaustion45 = true, debtAmountCoins = 11)
        val r = ChantalJourneyConfirmation.apply(snapshot(filler), filler.id, fillerText = "Вернись к мельнице")
        assertEquals(8, r.xpEarned)
        assertEquals(7, r.snapshot.getInt("totalJourneys"))
        assertEquals(0, r.snapshot.getInt("debtCoins"))
        assertEquals("", r.snapshot.getString("debtReasonRu"))
        assertEquals(99, r.snapshot.getInt("curiosity"))
        assertFalse(r.snapshot.optBoolean("isExhausted45"))
        assertEquals("Свой текст: Вернись к мельнице", r.episode!!.chosenActionSummaryRu)
    }
    @Test fun invalidOrStaleDecisionCannotMutateSnapshot() {
        val ep = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())
        val s = snapshot(ep)
        val before = s.toString()
        assertTrue(runCatching { ChantalJourneyConfirmation.apply(s, "different", choiceIndex = 0) }.isFailure)
        assertTrue(runCatching { ChantalJourneyConfirmation.apply(s, ep.id, choiceIndex = 99) }.isFailure)
        assertTrue(runCatching { ChantalJourneyConfirmation.apply(s, ep.id, fillerText = "x") }.isFailure)
        assertTrue(runCatching { ChantalJourneyConfirmation.apply(s, ep.id, choiceIndex = 0, fillerText = "x") }.isFailure)
        assertEquals(before, s.toString())
    }
    @Test fun numericOverflowRejectsWholeDecision() {
        val ep = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(coinsEarned = 1)
        val s = snapshot(ep).put("coins", Int.MAX_VALUE)
        val before = s.toString()
        assertTrue(runCatching { ChantalJourneyConfirmation.apply(s, ep.id, choiceIndex = 0) }.isFailure)
        assertEquals(before, s.toString())
    }
}
