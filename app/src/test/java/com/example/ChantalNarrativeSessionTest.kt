package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.*
import com.example.domain.ChantalStoryEngine as Engine
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalNarrativeSessionTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: VelarisDatabase
    private lateinit var repo: ChantalProgressRepository
    @Before fun setup() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        db=Room.inMemoryDatabaseBuilder(context,VelarisDatabase::class.java).allowMainThreadQueries().build()
        repo=ChantalProgressRepository(context,db);repo.replace(null)
    }
    @After fun close() { db.close() }
    private fun seed(id: String="pending",count: Int=4,complete: Boolean=true,milestone: Boolean=false) {
        val original=ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(id=id,regionTitleRu="Старая водяная мельница",isMilestoneChest=milestone,coinsEarned=21)
        val journal=JSONArray()
        if(complete) repeat(4) { journal.put(ChantalProgressCodec.encode(original.copy(id="lantern_chain_$it",chosenActionSummaryRu="Помочь"))) }
        else journal.put(ChantalProgressCodec.encode(original.copy(id="lantern_chain_0",chosenActionSummaryRu="Помочь")))
        val snapshot=JSONObject().put("totalJourneys",count).put("coins",100).put("maturityXp",40)
            .put("journalList",journal).put("pendingEpisode",ChantalProgressCodec.encode(original))
        repo.save(snapshot)
    }
    private fun response(snapshot: JSONObject): String {
        val d=ChantalNarrativeSession.describe(snapshot,snapshot.getJSONObject("narrativeBinding"))
        return JSONObject().put("titleRu","Моя прогулка").put("paragraphsRu",JSONArray(d.local.storyTextRu.split("\n\n")))
            .put("reactions",JSONArray(d.local.choices.indices.map { JSONObject().put("choiceIndex",it).put("textRu","Я услышала тебя. Спасибо! $it") }))
            .put("references",JSONArray()).toString()
    }
    @Test fun generationAndConfirmationAreSeparateAndEngineMovesWithRewardExactlyOnce() {
        seed()
        val local=repo.prepareNarrative()
        assertEquals("local",local.getJSONObject("narrativeBinding").getString("source"))
        assertEquals(100,local.getInt("coins"));assertEquals(4,local.getInt("totalJourneys"));assertFalse(local.has("storyEngineState"))
        val ticket=repo.beginNarrativeRequest("pending")
        val published=repo.publishNarrative(ticket,response(ticket.snapshot),"openrouter")
        assertEquals(100,published.getInt("coins"));assertEquals(4,published.getInt("totalJourneys"))
        val accepted=repo.confirmJourney("pending",choiceIndex=2)
        assertTrue(accepted.applied);assertEquals(121,accepted.snapshot.getInt("coins"))
        val engine=ChantalStoryEngineCodec.decode(accepted.snapshot.getJSONObject("storyEngineState"))
        assertEquals(5,engine.completedAdventures);assertEquals(ChantalPlotStatus.SUSPENDED,engine.plotStates["mill_noise"])
        assertFalse(accepted.snapshot.has("narrativeBinding"))
        assertFalse(repo.confirmJourney("pending",choiceIndex=0).applied)
    }
    @Test fun invalidResponsePreservesLocalEpisodeAndCannotGrantAnything() {
        seed();repo.prepareNarrative()
        val ticket=repo.beginNarrativeRequest("pending")
        val before=repo.load().getJSONObject("pendingEpisode").toString()
        val bad=JSONObject(response(ticket.snapshot)).put("coins",999999)
        assertTrue(runCatching { repo.publishNarrative(ticket,bad.toString(),"openrouter") }.isFailure)
        val unknown=JSONObject(response(ticket.snapshot)).put("references",JSONArray().put("invented_npc"))
        assertTrue(runCatching { repo.publishNarrative(ticket,unknown.toString(),"openrouter") }.isFailure)
        repo.finishNarrativeRequest(ticket,false)
        assertEquals(before,repo.load().getJSONObject("pendingEpisode").toString())
        assertEquals(100,repo.load().getInt("coins"));assertFalse(repo.load().has("storyEngineState"))
    }
    @Test fun cancelledSupersededAndRestoredRequestsCannotPublishOldResponses() {
        seed();repo.prepareNarrative()
        val old=repo.beginNarrativeRequest("pending")
        repo.finishNarrativeRequest(old,true)
        val next=repo.beginNarrativeRequest("pending")
        assertTrue(runCatching { repo.publishNarrative(old,response(old.snapshot),"openrouter") }.isFailure)
        repo.replace(repo.export())
        assertTrue(runCatching { repo.publishNarrative(next,response(next.snapshot),"openrouter") }.isFailure)
        assertEquals("INTERRUPTED",repo.load().getJSONObject("narrativeBinding").getString("status"))
    }
    @Test fun engineAndRewardRollbackTogetherOnDiskFailure() {
        seed();repo.prepareNarrative()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_confirmation BEFORE INSERT ON chantal_aggregate BEGIN SELECT RAISE(ABORT,'injected'); END")
        assertTrue(runCatching { repo.confirmJourney("pending",choiceIndex=0) }.isFailure)
        assertNull(db.chantalDao().receipt("pending"))
        assertFalse(repo.load().has("storyEngineState"));assertEquals(100,repo.load().getInt("coins"))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_confirmation")
        assertTrue(repo.confirmJourney("pending",choiceIndex=0).applied)
    }
    @Test fun legacyReservationDoesNotStartACompetingPlot() {
        seed(count=3,complete=false)
        val prepared=repo.prepareNarrative()
        val d=ChantalNarrativeSession.describe(prepared,prepared.getJSONObject("narrativeBinding"))
        assertFalse(d.tracked);assertEquals(Engine.Kind.ORDINARY,d.plan!!.kind)
        val accepted=repo.confirmJourney("pending",choiceIndex=0)
        assertFalse(accepted.snapshot.has("storyEngineState"))
    }
    @Test fun externalChestAdvancesAgeWithoutInventingEngineFactsAndVersionOneStillReads() {
        seed(count=9,milestone=true)
        val s=repo.load();s.put("storyEngineState",ChantalStoryEngineCodec.encode(Engine.newGame(13,9)))
        repo.save(s);repo.prepareNarrative()
        val accepted=repo.confirmJourney("pending",choiceIndex=0)
        val state=ChantalStoryEngineCodec.decode(accepted.snapshot.getJSONObject("storyEngineState"))
        assertEquals(10,state.completedAdventures);assertTrue(state.events.isEmpty());assertEquals(1,state.externalHistory.size)
        val v1=ChantalStoryEngineCodec.encode(Engine.newGame(13,9)).put("version",1).also { it.remove("externalAdvancements") }
        assertEquals(9,ChantalStoryEngineCodec.decode(v1).completedAdventures)
    }
    @Test fun promptIsBoundedUsesActualAgeAndSeparatesFeedbackFromCanon() {
        seed(count=99)
        repo.saveFeedback(ChantalStoryFeedback("lantern_chain_0",ChantalStoryRating.DISLIKED,ChantalFeedbackScope.GENERAL,
            setOf(ChantalFeedbackReason.TOO_VAGUE),"Меньше воды, больше живых диалогов"))
        val prepared=repo.prepareNarrative()
        val d=ChantalNarrativeSession.describe(prepared,prepared.getJSONObject("narrativeBinding"))
        val prompt=ChantalNarrativeSession.prompt(prepared,d)
        assertTrue(prompt.length<=48000)
        assertTrue(prompt.contains("ADULT_FOX"));assertTrue(prompt.contains("HUMAN"))
        assertTrue(prompt.contains(ChantalNarrativeGuide.userStyle))
        assertTrue(prompt.contains("Меньше воды, больше живых диалогов"))
        assertFalse(d.outcome.contains("Меньше воды"))
    }
    @Test fun explicitResumeReplansForFreeAndChangesCanonOnlyAfterTheChoice() {
        seed();repo.prepareNarrative();repo.confirmJourney("pending",choiceIndex=2)
        val next=ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(id="return",regionTitleRu="Старая водяная мельница")
        repo.save(repo.load().put("pendingEpisode",ChantalProgressCodec.encode(next)))
        val ordinary=repo.prepareNarrative()
        assertEquals(Engine.Kind.ORDINARY,ChantalNarrativeSession.describe(ordinary,ordinary.getJSONObject("narrativeBinding")).plan!!.kind)
        val resumed=repo.prepareNarrative(resume=true)
        assertEquals(Engine.Kind.RESUME,ChantalNarrativeSession.describe(resumed,resumed.getJSONObject("narrativeBinding")).plan!!.kind)
        assertEquals(ordinary.getInt("coins"),resumed.getInt("coins"))
        assertEquals(ChantalPlotStatus.SUSPENDED,ChantalStoryEngineCodec.decode(resumed.getJSONObject("storyEngineState")).plotStates["mill_noise"])
        val committed=repo.confirmJourney("return",choiceIndex=0)
        assertEquals(ChantalPlotStatus.INVESTIGATING,ChantalStoryEngineCodec.decode(committed.snapshot.getJSONObject("storyEngineState")).plotStates["mill_noise"])
    }
    @Test fun archiveCannotSilentlyAlterAnAlreadyBoundReward() {
        seed();repo.prepareNarrative()
        val before=repo.export()
        val corrupt=JSONObject(before)
        corrupt.getJSONObject("narrativeBinding").getJSONObject("original").put("coinsEarned",999999)
        assertTrue(runCatching { repo.replace(corrupt.toString()) }.isFailure)
        assertEquals(before,repo.export())
    }

    @Test fun oldMagicPropertiesSurviveTheShortRecentWindowWithoutSendingTheWholeArchive() {
        seed(count=10)
        val snapshot=repo.load()
        val old=snapshot.getJSONArray("journalList")
        val newer=JSONArray()
        val ep=ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())
        repeat(6) { newer.put(ChantalProgressCodec.encode(ep.copy(id="extra$it",chosenActionSummaryRu="Помочь"))) }
        repeat(old.length()) { newer.put(old.get(it)) }
        snapshot.put("journalList",newer)
        repo.save(snapshot)
        val withMemory=repo.load()
        withMemory.put("worldMemory",ChantalWorldMemory.withFact(withMemory.getJSONObject("worldMemory"),"magicItems",
            JSONObject().put("id","silver_thread").put("sourceEpisodeId","lantern_chain_0")
                .put("descriptionRu","Серебряная нить").put("propertyRu","Хранит один напев и возвращает его у воды")))
        repo.save(withMemory)
        val prepared=repo.prepareNarrative()
        val description=ChantalNarrativeSession.describe(prepared,prepared.getJSONObject("narrativeBinding"))
        val prompt=ChantalNarrativeSession.prompt(prepared,description)
        assertTrue(prompt.contains("Хранит один напев и возвращает его у воды"))
        assertTrue(prompt.length<=48000)
        val reply=JSONObject(response(prepared)).put("references",JSONArray().put("lantern_chain_0"))
        assertTrue(ChantalNarrativeSession.parse(reply.toString(),prepared,description).paragraphs.isNotEmpty())
    }

}
