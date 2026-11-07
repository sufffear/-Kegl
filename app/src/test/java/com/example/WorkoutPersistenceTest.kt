package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WorkoutPersistenceTest {
    private lateinit var db: VelarisDatabase
    private lateinit var repo: WorkoutPersistenceRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), VelarisDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = WorkoutPersistenceRepository(db.velarisDao(), SystemTimeProvider())
    }
    @After fun close() { db.close() }
    private fun snapshot() = PhaseEngineSnapshot(runId = "run", slotId = "slot", startDate = "2026-11-07",
        status = RunStatus.RUNNING, confirmedActiveMs = 1000, isDemo = false)

    @Test fun lateCheckpointCannotUndoCompletionOrDuplicateReward() = runBlocking {
        val running = snapshot()
        repo.saveRunCheckpoint(running)
        val done = running.copy(status = RunStatus.COMPLETED, confirmedActiveMs = 12345, phaseRemainingMs = 0)
        assertTrue(repo.completeRunAtomically(done))
        repo.saveRunCheckpoint(running)
        assertFalse(repo.completeRunAtomically(done))
        assertEquals(RunStatus.COMPLETED.name, db.velarisDao().getRunById("run")!!.status)
        assertEquals(12345L, db.velarisDao().getRunById("run")!!.confirmedActiveMs)
        assertEquals(1, repo.countRealCompletions())
    }
    @Test fun olderActiveTimeCannotOverwriteNewerCheckpoint() = runBlocking {
        repo.saveRunCheckpoint(snapshot().copy(confirmedActiveMs = 2000))
        repo.saveRunCheckpoint(snapshot())
        assertEquals(2000L, db.velarisDao().getRunById("run")!!.confirmedActiveMs)
    }
    @Test fun recoveryNeverAwardsOfflineTimeOrStartsAutomatically() = runBlocking {
        repo.saveRunCheckpoint(snapshot())
        val restored = repo.recoverInterruptedRunAfterProcessDeath()!!
        assertEquals(RunStatus.INTERRUPTED.name, restored.status)
        assertEquals(1000L, restored.confirmedActiveMs)
        assertEquals(0, repo.countRealCompletions())
    }
    @Test fun legacyConstructorCheckpointCannotResumeAsRegularWorkout() = runBlocking {
        repo.saveRunCheckpoint(snapshot().copy(slotId = "prog_custom_run"))
        assertNull(repo.recoverInterruptedRunAfterProcessDeath())
        assertEquals(RunStatus.STOPPED.name, db.velarisDao().getRunById("run")!!.status)
        assertEquals(0, repo.countRealCompletions())
    }
}
