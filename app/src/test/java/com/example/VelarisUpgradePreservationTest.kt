package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class VelarisUpgradePreservationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: VelarisDatabase
    private lateinit var repo: ChantalProgressRepository
    private val compose = createAndroidComposeRule<MainActivity>()
    private val seed = object : ExternalResource() {
        override fun before() = runBlocking {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            db = VelarisDatabase.getInstance(context)
            // Explicit synthetic fixture isolation; the application must NOT clean user data.
            db.velarisDao().clearFinalStartCompletions(); db.velarisDao().clearFinalStartRuns()
            db.velarisDao().clearFinalStartSlots(); db.velarisDao().clearFinalStartDays()
            repo = ChantalProgressRepository(context, db)
            repo.replace(null)
            repo.save(JSONObject().put("coins",42).put("totalJourneys",2).put("maturityXp",29))
            db.velarisDao().insertCompletionIgnore(WorkoutCompletionEntity("preserve","preserve-slot","2026-11-09",1,null,false))
            context.getSharedPreferences("chantal_openrouter_access",0).edit().putString("sentinel","dummy-test-value").commit()
            assertFalse(withContext(Dispatchers.IO) { db.chantalDao().markerPresent(BackupRestoreManager.FINAL_START_ID) })
        }
    }
    // Seed BEFORE launch; let Compose's rule own Activity/recomposer cleanup after the test.
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(compose)

    @Test fun launchWithoutLegacyFinalStartMarkerNeverErasesExistingGameOrTraining() = runBlocking {
        compose.waitForIdle()
        assertEquals(42,repo.load().getInt("coins"))
        assertEquals(29,repo.load().getInt("maturityXp"))
        assertEquals(2,repo.load().getInt("totalJourneys"))
        assertEquals(10,db.velarisDao().observeWorkoutEnergy().first())
        assertFalse(withContext(Dispatchers.IO) { db.chantalDao().markerPresent(BackupRestoreManager.FINAL_START_ID) })
        assertEquals("dummy-test-value",context.getSharedPreferences("chantal_openrouter_access",0).getString("sentinel",null))
    }
}
