package com.example

import com.example.data.*
import com.example.domain.ChantalTraits
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalLongGameTest {
    @Test fun thousandAdventuresKeepBoundedContextAndFactSourcesWithoutErasingArchive() {
        val memory = ChantalWorldMemory.empty()
        repeat(1000) { i ->
            memory.getJSONArray("events").put(JSONObject().put("episodeId", "episode-$i").put("journeyNumber", i)
                .put("regionRu", "Лес").put("summaryRu", "Приключение $i " + "факт ".repeat(100)).put("choiceRu", "Помочь другу"))
            memory.getJSONArray("relationships").put(JSONObject().put("id", "gift-$i").put("sourceEpisodeId", "episode-$i")
                .put("descriptionRu", "Подарок $i " + "память ".repeat(70)))
        }
        memory.getJSONArray("promises").put(JSONObject().put("id", "old-promise").put("sourceEpisodeId", "episode-0")
            .put("descriptionRu", "Вернуться к старому другу").put("status", "open"))
        val original = memory.toString()
        val context = ChantalWorldMemory.context(memory)
        assertTrue(context.toString().length <= 30_000)
        ChantalWorldMemory.validate(context)
        val ids = context.getJSONArray("events").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("episodeId") } }
        assertTrue("episode-999" in ids)
        assertTrue("episode-0" in ids)
        assertEquals(1, context.getJSONArray("promises").length())
        assertEquals(original, memory.toString())
        assertTrue(ChantalStoryGeneration.prompt(ChantalOfflineAdventure.previewBase(), memory, ChantalTraits(), 1000, 10000).isNotBlank())
    }
    @Test fun corruptNewFieldsCannotReplaceHealthySave() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = ChantalProgressRepository(context)
        val healthy = JSONObject().put("schemaVersion", 1).put("coins", 42).toString()
        repository.replace(healthy)
        listOf(JSONObject(healthy).put("worldInteractions", "broken"), JSONObject(healthy).put("shelfSelection", JSONArray(listOf("a", "a"))),
            JSONObject(healthy).put("shelfSelection", JSONArray(listOf("a", "b", "c", "d")))).forEach {
            assertTrue(runCatching { repository.replace(it.toString()) }.isFailure)
            assertEquals(42, repository.load().getInt("coins"))
        }
        repository.replace(null)
    }
}
