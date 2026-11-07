package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.*
import com.example.domain.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalAdventureTest {
    private fun episode() = ChantalStoryEpisode(
        id = "episode-1", journeyNumber = 1, titleRu = "У моста", storyTextRu = "История\nс продолжением",
        mapNoteRu = "Обещала вернуться", regionTitleRu = "Лес", npcNameRu = "Брамбл",
        stayedInSameRegion = false, isFiller = false,
        lootItem = ChantalInventoryLootItem("loot-1", "Листок", category = ChantalLootCategory.ROYAL_CLUE,
            descriptionRu = "Улика", sellPriceCoins = 24, foundAtRegionRu = "Лес", foundDateIso = "2026-11-07"),
        coinsEarned = 20, causesExhaustion45 = true, debtAmountCoins = 0, badNewsRu = "Устала",
        choices = listOf(ChantalChoiceOption("Помочь", "Улыбнулась", ChantalTraits(1, 2, 0, 0, 0, 0)))
    )

    @Test fun allBranchesFinishWithThreeChoicesAndPersistentConsequences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        for (first in 0..2) for (thread in 0..2) for (boat in 0..2) {
            val repo = ChantalProgressRepository(context)
            repo.replace(null)
            val journal = mutableListOf<ChantalStoryEpisode>()
            val choices = listOf(first, thread, boat, 0)
            repeat(4) { stage ->
                val memory = repo.load().optJSONObject("worldMemory") ?: ChantalWorldMemory.empty()
                val ep = ChantalLanternAdventure.next(episode().copy(regionTitleRu = "Янтарная дубрава"), journal, memory)!!
                assertEquals(3, ep.choices.size)
                assertEquals(11, ep.storyTextRu.count { it == '.' || it == '!' || it == '?' })
                if (stage == 2) {
                    if (thread == 0) assertTrue(ep.storyTextRu.contains("согрелась в узелке"))
                    else assertFalse(ep.storyTextRu.contains("согрелась в узелке"))
                }
                val opt = ep.choices[choices[stage]]
                assertTrue(opt.reactionRu.isNotBlank())
                journal.add(ep.copy(chosenActionSummaryRu = "${opt.labelRu} — Реакция Шанталь: ${opt.reactionRu}"))
                repo.save(JSONObject().put("journalList", JSONArray(journal.map { ChantalProgressCodec.encode(it) })))
                // Simulate closing the app and restoring its complete archive after each choice.
                val archive = repo.export()
                repo.replace(null)
                repo.replace(archive)
            }
            val memory = repo.load().getJSONObject("worldMemory")
            assertEquals(4, memory.getJSONArray("events").length())
            assertEquals(1, memory.getJSONArray("keepsakes").length())
            assertEquals(if (thread == 0) 1 else 0, memory.getJSONArray("magicItems").length())
            assertEquals(if (first == 0) 1 else 0, memory.getJSONArray("promises").length())
            if (first == 0) assertEquals("resolved", memory.getJSONArray("promises").getJSONObject(0).getString("status"))
            assertNull(ChantalLanternAdventure.next(episode().copy(regionTitleRu = "Янтарная дубрава"), journal, memory))
            assertEquals(memory.toString(), ChantalLanternAdventure.consequences(memory, journal).toString())
        }
    }

    @Test fun pendingChapterDoesNotAdvanceAndUnrelatedRoutesKeepTheirStories() {
        val base = episode().copy(regionTitleRu = "Янтарная дубрава")
        val pending = ChantalLanternAdventure.next(base, emptyList(), ChantalWorldMemory.empty())!!
        assertEquals(pending.id, ChantalLanternAdventure.next(base, listOf(pending), ChantalWorldMemory.empty())!!.id)
        assertNull(ChantalLanternAdventure.next(base.copy(regionTitleRu = "Далёкие горы"), emptyList(), ChantalWorldMemory.empty()))
        assertNull(ChantalLanternAdventure.next(base.copy(isFiller = true), emptyList(), ChantalWorldMemory.empty()))
    }
}
