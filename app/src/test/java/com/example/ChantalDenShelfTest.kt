package com.example

import com.example.data.*
import com.example.domain.*
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalDenShelfTest {
    @Test fun shelfUsesActualMemoryAndOwnedDecorationsOnly() {
        val episode = ChantalOfflineAdventure.previewBase().copy(chosenActionSummaryRu = "Помочь")
        val memory = ChantalWorldMemory.withFact(ChantalWorldMemory.reconcile(null, JSONArray().put(ChantalProgressCodec.encode(episode))), "keepsakes",
            JSONObject().put("id", "keepsake").put("sourceEpisodeId", episode.id).put("descriptionRu", "Памятная пуговица"))
        val owned = DEFAULT_CHANTAL_SHOP_ITEMS.first { it.category == ChantalShopCategory.DEN_COMFORT }
        val items = ChantalDenShelf.items(memory, listOf(episode), emptyList(), listOf(owned.id))
        assertEquals(2, items.size)
        assertTrue(items.first().memory.contains(episode.storyTextRu))
        assertTrue(items.first().memory.contains("Твой ответ: Помочь"))
        assertEquals(1, ChantalDenShelf.items(memory, emptyList(), emptyList(), emptyList()).size)
        assertTrue(ChantalDenShelf.items(ChantalWorldMemory.empty(), emptyList(), emptyList(), emptyList()).isEmpty())
    }
    @Test fun selectionHasThreeSlotsAndDoesNotDestroyItems() {
        val items = (1..4).map { ChantalShelfItem("$it", "Вещь $it", "✨", "Событие", "Реплика") }
        var selected = emptyList<String>()
        (1..3).forEach { selected = ChantalDenShelf.select(selected, "$it", items) }
        assertTrue(runCatching { ChantalDenShelf.select(selected, "4", items) }.isFailure)
        selected = ChantalDenShelf.select(selected, "2", items)
        assertEquals(listOf("1", "3"), selected)
        assertEquals(4, items.size)
        assertTrue(runCatching { ChantalDenShelf.select(selected, "unknown", items) }.isFailure)
    }
    @Test fun arrangementSurvivesBackupWithoutRewardsOrInventoryChanges() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = ChantalProgressRepository(context)
        repo.replace(null)
        repo.save(JSONObject().put("coins", 27).put("shelfSelection", JSONArray(listOf("decor:chair"))))
        val backup = repo.export()
        repo.replace(null)
        repo.replace(backup)
        assertEquals("decor:chair", repo.load().getJSONArray("shelfSelection").getString(0))
        assertEquals(27, repo.load().getInt("coins"))
        repo.replace(null)
    }
}
