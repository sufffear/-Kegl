package com.example

import com.example.data.*
import com.example.domain.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalWorldInteractionsTest {
    private val episode = ChantalOfflineAdventure.previewBase().copy(chosenActionSummaryRu = "Выслушать")
    private val gift = episode.lootItem.copy(id = "gift", titleRu = "Пуговица", category = ChantalLootCategory.TRINKET)
    @Test fun giftRequiresMeetingAndOwnedItemAndCannotRepeatByTitle() {
        val original = ChantalWorldInteractions.empty()
        assertTrue(runCatching { ChantalWorldInteractions.gift(original, gift.id, episode.npcNameRu, listOf(gift), emptyList()) }.isFailure)
        val updated = ChantalWorldInteractions.gift(original, gift.id, episode.npcNameRu, listOf(gift), listOf(episode))
        assertEquals(0, original.getJSONArray("gifts").length())
        assertEquals(1, updated.getJSONArray("gifts").length())
        assertTrue(runCatching { ChantalWorldInteractions.gift(updated, "other", episode.npcNameRu, listOf(gift.copy(id = "other")), listOf(episode)) }.isFailure)
        assertTrue(runCatching { ChantalWorldInteractions.gift(updated, gift.id, episode.npcNameRu, emptyList(), listOf(episode)) }.isFailure)
        assertEquals(updated.toString(), ChantalWorldInteractions.validate(org.json.JSONObject(updated.toString())).toString())
    }
    @Test fun comparisonRequiresTwoStudiedCluesAndIsOrderIndependent() {
        val a = gift.copy(id = "a", category = ChantalLootCategory.ROYAL_CLUE, inspectedBonusClaimed = true)
        val b = a.copy(id = "b")
        val empty = ChantalWorldInteractions.empty()
        assertTrue(runCatching { ChantalWorldInteractions.compare(empty, "a", "a", listOf(a)) }.isFailure)
        assertTrue(runCatching { ChantalWorldInteractions.compare(empty, "a", "b", listOf(a, b.copy(inspectedBonusClaimed = false))) }.isFailure)
        val compared = ChantalWorldInteractions.compare(empty, "a", "b", listOf(a, b))
        val again = ChantalWorldInteractions.compare(compared, "b", "a", listOf(a, b))
        assertEquals(1, again.getJSONArray("comparisons").length())
        assertEquals(0, empty.getJSONArray("comparisons").length())
    }
}
