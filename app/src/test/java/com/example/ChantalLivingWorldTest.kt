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
class ChantalLivingWorldTest {
    @Test fun recentThreeThemesDoNotRepeatAndFullHistoryIsUnchanged() {
        val memory = ChantalWorldMemory.empty()
        repeat(20) { i ->
            val theme = ChantalLivingWorld.theme(memory, i)
            val events = memory.getJSONArray("events")
            val recent = (maxOf(0, events.length()-3) until events.length()).map { events.getJSONObject(it).getString("themeRu") }
            assertFalse(theme in recent)
            events.put(JSONObject().put("episodeId", "$i").put("journeyNumber", i).put("regionRu", "Лес").put("summaryRu", "Факт").put("choiceRu", "Ответ").put("themeRu", theme))
        }
        assertEquals(20, memory.getJSONArray("events").length())
        assertEquals("Корвус", ChantalLivingWorld.profile("Ворон-почтальон Корвус")!!.key)
    }
    @Test fun invitationRequiresThreeDistinctAcceptedMeetingsAndSurvivesRestore() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = ChantalProgressRepository(context)
        repo.replace(null)
        val base = ChantalOfflineAdventure.previewBase().copy(npcNameRu="Ворон-почтальон Корвус", chosenActionSummaryRu="Помочь")
        val snapshot = JSONObject().put("friendSceneAnswers", JSONObject().put("Корвус", 1))
        snapshot.put("journalList", JSONArray().put(ChantalProgressCodec.encode(base)))
        repo.save(snapshot)
        assertFalse(repo.load().getJSONObject("worldMemory").getJSONArray("relationships").toString().contains("friend-scene:"))
        snapshot.put("journalList", JSONArray((0..2).map { ChantalProgressCodec.encode(base.copy(id="meeting-$it")) }))
        repo.save(snapshot)
        val archive = repo.export()
        repo.replace(null); repo.replace(archive)
        assertTrue(repo.load().getJSONObject("worldMemory").getJSONArray("relationships").toString().contains("friend-scene:"))
        val first = repo.export()
        repo.save(repo.load())
        assertEquals(first, repo.export())
        repo.replace(null)
    }
}
