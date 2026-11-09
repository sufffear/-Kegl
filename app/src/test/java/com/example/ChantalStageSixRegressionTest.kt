package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Disk-backed lifecycle stress; does not claim to kill an Android process or contact an API. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalStageSixRegressionTest {
    @Test fun longGameSurvivesDatabaseReopenRestoreFaultAndRepeatedChoices() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val name = "stage-six-stress.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, VelarisDatabase::class.java, name)
            .allowMainThreadQueries().build()
        var db = open()
        var repo = ChantalProgressRepository(context, db)
        val original = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())
            .copy(regionTitleRu = "Старая водяная мельница", coinsEarned = 21)
        val legacy = JSONArray()
        repeat(4) { legacy.put(ChantalProgressCodec.encode(original.copy(id = "lantern_chain_$it", chosenActionSummaryRu = "Помочь"))) }
        val home = ChantalHomePresence.respond(ChantalHomePresence.State(), ChantalWishStatus.DECLINED)
        val started = System.nanoTime()
        var longestMs = 0L
        try {
            repo.replace(null)
            repo.save(JSONObject().put("totalJourneys", 4).put("coins", 100).put("maturityXp", 5000)
                .put("journalList", legacy).put("homePresence", ChantalHomePresence.encode(home)))
            repeat(120) { index ->
                val stepStart = System.nanoTime()
                val id = "stress-$index"
                repo.save(repo.load().put("pendingEpisode", ChantalProgressCodec.encode(original.copy(id = id))))
                val prepared = repo.prepareNarrative(resume = index % 7 == 0)
                val description = ChantalNarrativeSession.describe(prepared, prepared.getJSONObject("narrativeBinding"))
                assertTrue(ChantalNarrativeSession.prompt(prepared, description).length <= 48000)
                if (index == 60) {
                    val before = repo.export()
                    db.openHelper.writableDatabase.execSQL("CREATE TRIGGER stage6_fault BEFORE INSERT ON chantal_aggregate BEGIN SELECT RAISE(ABORT, 'stage6'); END")
                    assertTrue(runCatching { repo.confirmJourney(id, choiceIndex = 0) }.isFailure)
                    assertEquals(before, repo.export())
                    assertNull(db.chantalDao().receipt(id))
                    db.openHelper.writableDatabase.execSQL("DROP TRIGGER stage6_fault")
                }
                val result = repo.confirmJourney(id, choiceIndex = 0)
                assertTrue(result.applied)
                assertEquals(5 + index, result.snapshot.getInt("totalJourneys"))
                assertEquals(121 + index * 21, result.snapshot.getInt("coins"))
                assertEquals(5010 + index * 10, result.snapshot.getInt("maturityXp"))
                assertEquals(ChantalWishStatus.DECLINED, ChantalHomePresence.fromSnapshot(result.snapshot).wish.status)
                val engine = ChantalStoryEngineCodec.decode(result.snapshot.getJSONObject("storyEngineState"))
                assertEquals(5 + index, engine.completedAdventures)
                assertTrue(engine.plotStates.count { it.value != ChantalPlotStatus.RESOLVED } <= 1)
                if (index % 10 == 0) {
                    assertFalse(repo.confirmJourney(id, choiceIndex = 1).applied)
                    assertEquals(result.snapshot.getInt("coins"), repo.load().getInt("coins"))
                }
                if (index in listOf(5, 45, 95, 119)) {
                    val archive = repo.export()
                    repo.replace(archive)
                    repo.replace(archive)
                    assertEquals(archive, repo.export())
                    db.close()
                    db = open()
                    repo = ChantalProgressRepository(context, db)
                    assertEquals(5 + index, repo.load().getInt("totalJourneys"))
                    assertNotNull(db.chantalDao().receipt(id))
                    assertFalse(repo.confirmJourney(id, choiceIndex = 2).applied)
                }
                longestMs = maxOf(longestMs, (System.nanoTime() - stepStart) / 1_000_000)
            }
            val final = repo.load()
            assertEquals(124, final.getJSONArray("journalList").length())
            assertEquals(124, db.chantalDao().events().size)
            assertEquals(ChantalAge.HUMAN, ChantalCharacterState.fromAdventures(final.getInt("totalJourneys")).age)
            println("Stage6 disk stress: 120 adventures, snapshot=${repo.export().toByteArray().size} bytes, longest cycle=${longestMs}ms, elapsed=${(System.nanoTime()-started)/1_000_000}ms")
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun interruptedGenerationSurvivesDiskReopenWithoutRewardAndRequiresManualRetry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val name = "stage-six-interrupted.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, VelarisDatabase::class.java, name).allowMainThreadQueries().build()
        var db = open()
        try {
            var repo = ChantalProgressRepository(context, db)
            repo.replace(null)
            val episode = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(id = "interrupted")
            repo.save(JSONObject().put("coins", 100).put("totalJourneys", 0)
                .put("pendingEpisode", ChantalProgressCodec.encode(episode)))
            repo.prepareNarrative()
            val old = repo.beginNarrativeRequest(episode.id)
            db.close(); db = open()
            repo = ChantalProgressRepository(context, db)
            val recovered = repo.load()
            assertEquals(100, recovered.getInt("coins"))
            assertEquals(0, recovered.getInt("totalJourneys"))
            assertEquals("IN_FLIGHT", recovered.getJSONObject("narrativeBinding").getString("status"))
            val retry = repo.beginNarrativeRequest(episode.id)
            assertNotEquals(old.token, retry.token)
            repo.finishNarrativeRequest(old, true)
            assertEquals(retry.token, repo.load().getJSONObject("narrativeBinding").getString("activeRequest"))
            repo.finishNarrativeRequest(retry, true)
            assertEquals("INTERRUPTED", repo.load().getJSONObject("narrativeBinding").getString("status"))
            assertTrue(repo.confirmJourney(episode.id, choiceIndex = 0).applied)
            assertFalse(repo.confirmJourney(episode.id, choiceIndex = 0).applied)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
