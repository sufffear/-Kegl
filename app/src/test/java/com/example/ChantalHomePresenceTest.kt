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
class ChantalHomePresenceTest {
    @Test fun emotionSurvivesReloadAndReactionDoesNotOverwriteIt() {
        val s = JSONObject().put("storyEmotion", "SADNESS")
        val state = ChantalHomePresence.fromSnapshot(s)
        assertEquals(ChantalStoryEmotion.SADNESS, state.storyEmotion)
        assertEquals("Мне приятно 💛", ChantalHomePresence.expression(state, ChantalHomePresence.Reaction.PETTED))
        assertEquals("Мне немного грустно", ChantalHomePresence.expression(state, null))
        assertEquals(state, ChantalHomePresence.decode(ChantalHomePresence.encode(state)))
        assertEquals(state, ChantalHomePresence.fromSnapshot(JSONObject(s.toString())))
    }
    @Test fun deferredAndDeclinedWishesDoNotAutomaticallyReappearOrReward() {
        val snapshot = JSONObject().put("coins", 9).put("maturityXp", 40).put("worldMemory", ChantalWorldMemory.empty())
        val before = snapshot.toString()
        val state = ChantalHomePresence.fromSnapshot(snapshot)
        val deferred = ChantalHomePresence.respond(state, ChantalWishStatus.DEFERRED)
        val reloaded = ChantalHomePresence.fromSnapshot(JSONObject().put("homePresence", ChantalHomePresence.encode(deferred)))
        assertEquals(ChantalWishStatus.DEFERRED, reloaded.wish.status)
        assertEquals(ChantalWishStatus.ACTIVE, ChantalHomePresence.respond(reloaded, ChantalWishStatus.ACTIVE).wish.status)
        for(status in listOf(ChantalWishStatus.DECLINED, ChantalWishStatus.FULFILLED)) {
            val terminal = ChantalHomePresence.respond(state, status)
            assertTrue(runCatching { ChantalHomePresence.respond(terminal, ChantalWishStatus.ACTIVE) }.isFailure)
            assertEquals(terminal, ChantalHomePresence.respond(terminal, status))
        }
        assertEquals(before, snapshot.toString())
    }
    @Test fun onlyAcceptedHistoryCanBeRememberedAndHumanTextHasNoFoxBody() {
        val pending = ChantalOfflineAdventure.previewBase().copy(titleRu="Не принято")
        val accepted = pending.copy(id="accepted", titleRu="Наша мельница", chosenActionSummaryRu="Выбрано")
        val snapshot = JSONObject().put("journalList", JSONArray().put(ChantalProgressCodec.encode(pending)).put(ChantalProgressCodec.encode(accepted)))
        for(count in listOf(0, 10, 50, 100)) {
            val text = ChantalHomePresence.rememberedLine(snapshot, ChantalCharacterState.fromAdventures(count))
            assertTrue(text.contains("Наша мельница"))
            assertFalse(text.contains("Не принято"))
            assertFalse(text.contains("хвост"))
            assertFalse(text.contains("лап"))
        }
        assertFalse(ChantalHomePresence.rememberedLine(JSONObject(), ChantalCharacterState.fromAdventures(0)).contains("вспомнила"))
    }
    @Test fun invalidPersistedStateIsRejected() {
        val valid = ChantalHomePresence.encode(ChantalHomePresence.State())
        assertTrue(runCatching { ChantalHomePresence.decode(JSONObject(valid.toString()).put("version", 2)) }.isFailure)
        val forged = JSONObject(valid.toString())
        forged.getJSONObject("wish").put("text", "Дай мне монеты")
        assertTrue(runCatching { ChantalHomePresence.decode(forged) }.isFailure)
        assertTrue(runCatching { ChantalHomePresence.fromSnapshot(JSONObject().put("storyEmotion", "unknown")) }.isFailure)
    }
}
