package com.example
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalGenerationTest {
    private fun episode() = ChantalStoryEpisode(
        id = "episode-1", journeyNumber = 1, titleRu = "У моста", storyTextRu = "История\nс продолжением",
        mapNoteRu = "Обещала вернуться", regionTitleRu = "Лес", npcNameRu = "Брамбл",
        stayedInSameRegion = false, isFiller = false,
        lootItem = ChantalInventoryLootItem("loot-1", "Листок", category = ChantalLootCategory.ROYAL_CLUE,
            descriptionRu = "Улика", sellPriceCoins = 24, foundAtRegionRu = "Лес", foundDateIso = "2026-11-07"),
        coinsEarned = 20, causesExhaustion45 = true, debtAmountCoins = 0, badNewsRu = "Устала",
        choices = listOf(ChantalChoiceOption("Помочь", "Улыбнулась", ChantalTraits(1, 2, 0, 0, 0, 0)))
    )

    private fun response() = JSONObject().put("titleRu", "Фонарь в камышах")
        .put("sentencesRu", JSONArray((1..11).map { "Предложение $it о лесной тропе." }))
        .put("choices", JSONArray((0..2).map { JSONObject().put("labelRu", "Решение $it")
            .put("reactionRu", "Шанталь отвечает $it").put("trait", listOf("wit", "courage", "tenderness")[it]) }))
        .put("references", JSONArray()).put("proposals", JSONArray())

    @Test fun narrativeV3AcceptsLongerParagraphStoryAndPreservesFullEndingSummary() {
        val sample = ChantalNarrativeSample.episode()
        assertTrue(sample.storyTextRu.count { it == '.' || it == '?' || it == '!' } > 12)
        val summary = "Шанталь рассыпала шерсть, помогая воробью строить гнездо; они вместе собрали часть и устроили мягкую подстилку."
        val reply = response().apply {
            remove("sentencesRu")
            put("paragraphsRu", JSONArray(sample.storyTextRu.split("\n\n")))
            put("summaryRu", summary)
        }
        val generated = ChantalStoryGeneration.parse(reply.toString(), episode(), ChantalWorldMemory.empty()).episode
        assertEquals(sample.storyTextRu, generated.storyTextRu)
        assertEquals(summary, generated.memorySummaryRu)
        val accepted = generated.copy(chosenActionSummaryRu = "Ответ игрока")
        val restored = ChantalProgressCodec.decodeChantalStoryEpisode(ChantalProgressCodec.encode(accepted))
        assertEquals(accepted, restored)
        val memory = ChantalWorldMemory.reconcile(null, JSONArray().put(ChantalProgressCodec.encode(restored)))
        assertEquals(summary, memory.getJSONArray("events").getJSONObject(0).getString("summaryRu"))
    }
    @Test fun mixedFormatsAndOversizedParagraphsAreRejected() {
        val mixed = response().put("paragraphsRu", JSONArray().put("Я вернулась домой." )).put("summaryRu", "Факт")
        assertTrue(runCatching { ChantalStoryGeneration.parse(mixed.toString(), episode(), ChantalWorldMemory.empty()) }.isFailure)
        val oversized = response().apply {
            remove("sentencesRu")
            put("paragraphsRu", JSONArray().put("а".repeat(2401)))
            put("summaryRu", "Факт")
        }
        assertTrue(runCatching { ChantalStoryGeneration.parse(oversized.toString(), episode(), ChantalWorldMemory.empty()) }.isFailure)
    }
    @Test fun generationPromptCarriesApprovedVoiceAndConversationalChoices() {
        val prompt = ChantalStoryGeneration.prompt(episode(), ChantalWorldMemory.empty(), ChantalTraits())
        assertTrue(prompt.contains("ЛИТЕРАТУРНАЯ ИНСТРУКЦИЯ 3.3"))
        assertTrue(prompt.contains("от первого лица"))
        assertTrue(prompt.contains("350–600 слов"))
        assertFalse(prompt.contains("100–180 слов"))
        assertFalse(prompt.contains("300–650 слов"))
        assertTrue(prompt.contains("реплики игрока"))
        assertFalse(prompt.contains("из 10–12 полных предложений"))
    }

    @Test fun requestsUseActualCompanionStageInBothRulesAndContext() {
        val baby = ChantalStoryGeneration.prompt(episode(), ChantalWorldMemory.empty(), ChantalTraits(), 1, 10)
        assertTrue(baby.contains("совсем маленький лисёнок"))
        assertTrue(baby.contains("\"stageNumber\":1"))
        assertTrue(baby.contains("350–600 слов"))
        val older = ChantalStoryGeneration.prompt(episode(), ChantalWorldMemory.empty(), ChantalTraits(), 12, 120)
        assertTrue(older.contains("\"stageNumber\":2"))
        assertTrue(older.contains("\"narrativeAgeMode\":\"very_young\""))
        assertTrue(older.contains("350–600 слов"))
        val growing = ChantalStoryGeneration.prompt(episode(), ChantalWorldMemory.empty(), ChantalTraits(), 30, 300)
        assertTrue(growing.contains("\"narrativeAgeMode\":\"growing\""))
        assertFalse(older.contains("не более 220 слов"))
        assertEquals(1, ChantalNarrativeGuide.stage(10, 100))
        assertEquals(2, ChantalNarrativeGuide.stage(11, 110))
        assertEquals(3, ChantalNarrativeGuide.stage(51, 510))
        assertEquals(4, ChantalNarrativeGuide.stage(100, 1000))
        // Petting XP can advance the same stage used by the portrait, even with few journeys.
        assertEquals(2, ChantalNarrativeGuide.stage(0, 120))
    }

    @Test fun completeUserStyleIsPreservedVerbatimAndIncludedInRequests() {
        val expectedSha = "8a1bd6fecf448fd134b81048508509ffab1cfef91ce1ef50cacc681e3792fd34"
        val actualSha = java.security.MessageDigest.getInstance("SHA-256")
            .digest(ChantalNarrativeGuide.userStyle.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        assertEquals(expectedSha, actualSha)
        val prompt = ChantalStoryGeneration.prompt(episode(), ChantalWorldMemory.empty(), ChantalTraits())
        assertTrue(prompt.contains(ChantalNarrativeGuide.userStyle))
        assertTrue(prompt.indexOf(ChantalNarrativeGuide.userStyle) < prompt.indexOf("ДОПОЛНЕНИЯ ДЛЯ ПРИЛОЖЕНИЯ"))
    }

    @Test fun geminiEnvelopeRejectsTruncatedAndBlockedResponses() {
        val candidate = JSONObject().put("finishReason", "STOP").put("content", JSONObject()
            .put("parts", JSONArray().put(JSONObject().put("thought", true).put("text", "internal"))
                .put(JSONObject().put("text", response().toString()))))
        val envelope = JSONObject().put("candidates", JSONArray().put(candidate))
        assertEquals(response().toString(), GeminiStoryTransport.decodeResponse(envelope.toString()))
        candidate.put("finishReason", "MAX_TOKENS")
        assertTrue(runCatching { GeminiStoryTransport.decodeResponse(envelope.toString()) }.isFailure)
        assertTrue(runCatching { GeminiStoryTransport.decodeResponse("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}") }.isFailure)
    }

    @Test fun validResponseKeepsAppOwnedRewardsAndIds() {
        val base = episode()
        val generated = ChantalStoryGeneration.parse(response().toString(), base, ChantalWorldMemory.empty()).episode
        assertEquals(base.id, generated.id)
        assertEquals(base.coinsEarned, generated.coinsEarned)
        assertEquals(base.lootItem, generated.lootItem)
        assertEquals(3, generated.choices.size)
        assertEquals(2, generated.choices[0].delta.wit)
    }
    @Test fun invalidReferencesRewardsAndWorldProposalsAreRejected() {
        val memory = ChantalWorldMemory.empty()
        for (bad in listOf(response().put("coins", 9999), response().put("references", JSONArray().put("invented")),
            response().put("proposals", JSONArray().put(JSONObject().put("coins", 9999))),
            response().put("choices", JSONArray()))) {
            assertTrue(runCatching { ChantalStoryGeneration.parse(bad.toString(), episode(), memory) }.isFailure)
        }
    }
    @Test fun transportFailurePreservesMemoryAndCancellationPropagates() = runBlocking {
        val memory = ChantalWorldMemory.empty()
        val before = memory.toString()
        val result = ChantalStoryGeneration.generate(ChantalStoryTransport { error("quota") }, episode(), memory, ChantalTraits())
        assertEquals(ChantalGenerationResult.Unavailable, result)
        assertEquals(before, memory.toString())
        val failure = runCatching { ChantalStoryGeneration.generate(ChantalStoryTransport { throw CancellationException() }, episode(), memory, ChantalTraits()) }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }
}
