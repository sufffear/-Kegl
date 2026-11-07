package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChantalProgressCodec
import com.example.data.ChantalProgressRepository
import com.example.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clear() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        context.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun episode() = ChantalStoryEpisode(
        id = "episode-1", journeyNumber = 1, titleRu = "У моста", storyTextRu = "История\nс продолжением",
        mapNoteRu = "Обещала вернуться", regionTitleRu = "Лес", npcNameRu = "Брамбл",
        stayedInSameRegion = false, isFiller = false,
        lootItem = ChantalInventoryLootItem("loot-1", "Листок", category = ChantalLootCategory.ROYAL_CLUE,
            descriptionRu = "Улика", sellPriceCoins = 24, foundAtRegionRu = "Лес", foundDateIso = "2026-11-07"),
        coinsEarned = 20, causesExhaustion45 = true, debtAmountCoins = 0, badNewsRu = "Устала",
        choices = listOf(ChantalChoiceOption("Помочь", "Улыбнулась", ChantalTraits(1, 2, 0, 0, 0, 0)))
    )

    @Test fun newInstallationHasNoSeededProgress() {
        val snapshot = ChantalProgressRepository(context).load()
        assertEquals(0, snapshot.optInt("coins", 0))
        assertEquals(0, snapshot.optInt("maturityXp", 0))
        assertFalse(snapshot.optBoolean("hasCompletedPrologue", false))
    }

    @Test fun pendingStoryAndActiveJourneySurviveRepositoryRecreation() {
        val ep = episode()
        val snapshot = JSONObject().put("activeJourneyStartMs", 123456L)
            .put("spentEnergy", 30).put("pendingEpisode", ChantalProgressCodec.encode(ep))
        ChantalProgressRepository(context).save(snapshot)
        val restored = ChantalProgressRepository(context).load()
        assertEquals(123456L, restored.getLong("activeJourneyStartMs"))
        assertEquals(30, restored.getInt("spentEnergy"))
        assertEquals(ep, ChantalProgressCodec.decodeChantalStoryEpisode(restored.getJSONObject("pendingEpisode")))
    }

    @Test fun acceptedStoryInventoryAndRewardsAreSavedTogether() {
        val ep = episode().copy(chosenActionSummaryRu = "Помочь")
        val snapshot = JSONObject().put("coins", 20).put("maturityXp", 10)
            .put("pendingEpisode", JSONObject.NULL)
            .put("inventoryList", JSONArray().put(ChantalProgressCodec.encode(ep.lootItem)))
            .put("journalList", JSONArray().put(ChantalProgressCodec.encode(ep)))
            .put("gearDurabilityMap", JSONObject().put("scarf", 3))
            .put("pantryProvisionCounts", JSONObject().put("pie", 2))
        ChantalProgressRepository(context).save(snapshot)
        val restored = ChantalProgressRepository(context).load()
        assertTrue(restored.isNull("pendingEpisode"))
        assertEquals(20, restored.getInt("coins"))
        assertEquals(10, restored.getInt("maturityXp"))
        assertEquals(ep, ChantalProgressCodec.decodeChantalStoryEpisode(restored.getJSONArray("journalList").getJSONObject(0)))
        assertEquals(ep.lootItem, ChantalProgressCodec.decodeChantalInventoryLootItem(restored.getJSONArray("inventoryList").getJSONObject(0)))
        assertEquals(mapOf("scarf" to 3), ChantalProgressCodec.counts(restored.getJSONObject("gearDurabilityMap")))
    }

    @Test fun invalidSaveIsRejectedAndNotReplacedWithDefaults() {
        val prefs = context.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE)
        prefs.edit().putString("snapshot", "{broken").commit()
        assertTrue(runCatching { ChantalProgressRepository(context).load() }.isFailure)
        assertEquals("{broken", prefs.getString("snapshot", null))
        prefs.edit().putString("snapshot", "{\"schemaVersion\":999}").commit()
        assertTrue(runCatching { ChantalProgressRepository(context).load() }.isFailure)
    }
}
