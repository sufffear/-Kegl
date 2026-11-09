package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalAtomicProgressTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: VelarisDatabase
    @Before fun setup() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        context.getSharedPreferences("chantal_progress",0).edit().clear().commit()
        db=Room.inMemoryDatabaseBuilder(context,VelarisDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun close() { db.close() }
    private fun repo() = ChantalProgressRepository(context,db)
    private fun pending(): String {
        val r=repo()
        val ep=ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())
        r.save(JSONObject().put("coins",17).put("pendingEpisode",ChantalProgressCodec.encode(ep)))
        return ep.id
    }
    @Test fun migrationIsOneWayAndUnknownVersionDoesNotEraseLegacy() {
        val prefs=context.getSharedPreferences("chantal_progress",0)
        prefs.edit().putString("snapshot","{\"schemaVersion\":999}").commit()
        assertTrue(runCatching { repo().load() }.isFailure)
        assertNull(db.chantalDao().aggregate())
        prefs.edit().putString("snapshot","{\"schemaVersion\":1,\"coins\":42}").commit()
        assertEquals(42,repo().load().getInt("coins"))
        prefs.edit().putString("snapshot","broken").commit()
        assertEquals(42,repo().load().getInt("coins"))
        repo().replace(null)
        assertFalse(repo().load().has("coins")) // empty tombstone does not reimport old prefs
    }
    @Test fun restartAndRepeatCannotReapplyRewardAndArchiveRestoresReceipts() {
        val id=pending(); val r=repo();r.load()
        val first=r.confirmJourney(id,choiceIndex=0)
        val after=repo().load()
        assertTrue(first.applied)
        assertFalse(repo().confirmJourney(id,choiceIndex=1).applied)
        assertEquals(after.getInt("coins"),repo().load().getInt("coins"))
        assertEquals(1,db.chantalDao().events().size)
        val archive=repo().export()
        repo().replace(null);repo().replace(archive)
        assertNotNull(db.chantalDao().receipt(id))
        assertEquals("PLAYER_DECISION",JSONObject(db.chantalDao().events().single().payload).getString("kind"))
        assertFalse(repo().confirmJourney(id,choiceIndex=0).applied)
    }
    @Test fun failureBetweenReceiptAndAggregateRollsBackEverything() {
        val id=pending();val before=repo().load().getInt("coins")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_aggregate BEFORE INSERT ON chantal_aggregate BEGIN SELECT RAISE(ABORT, 'injected'); END")
        assertTrue(runCatching { repo().confirmJourney(id,choiceIndex=0) }.isFailure)
        assertNull(db.chantalDao().receipt(id));assertTrue(db.chantalDao().events().isEmpty())
        assertEquals(before,repo().load().getInt("coins"))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_aggregate")
        assertTrue(repo().confirmJourney(id,choiceIndex=0).applied)
    }
    @Test fun staleWriterCannotUndoConfirmedEpisode() {
        val id=pending();val stale=repo();val draft=stale.load()
        repo().confirmJourney(id,choiceIndex=0)
        assertTrue(runCatching { stale.save(draft.put("coins",0)) }.isFailure)
        assertNotNull(db.chantalDao().receipt(id))
    }
    @Test fun editorialFeedbackSurvivesNormalUiSaveAndExport() {
        val id=pending();repo().confirmJourney(id,choiceIndex=0)
        repo().saveFeedback(ChantalStoryFeedback(id,ChantalStoryRating.LIKED,ChantalFeedbackScope.GENERAL,setOf(ChantalFeedbackReason.FUNNY)))
        val r=repo();r.load();r.save(JSONObject().put("coins",100))
        val archive=r.export();r.replace(null);r.replace(archive)
        assertEquals("FUNNY",r.load().getJSONArray("storyFeedback").getJSONObject(0).getJSONArray("reasons").getString(0))
    }
}
