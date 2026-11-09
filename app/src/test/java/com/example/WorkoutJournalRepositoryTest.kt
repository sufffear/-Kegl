package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.SystemTimeProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal fun journalRun(id: String, status: String = "STOPPED", date: String = "2026-11-09", free: Boolean = false, demo: Boolean = false) =
    WorkoutRunEntity(id,"slot-$id",status,1000,date,"Europe/Moscow",1,"Занятие $id","MORNING",1000,1000,10,false,
        "velvet_bowl","soft",19000,10,"CONTRACTION",0,2000,"boot",if(status=="STOPPED") "По запросу пользователя" else null,demo,free)

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class WorkoutJournalRepositoryTest {
    @Test fun projectionKeepsUncreditedAttemptsLegacyAndSavedLocalDatesWithoutCountingDemo() {
        val crossing = journalRun("cross","COMPLETED","2026-11-08")
        val uncredited = journalRun("uncredited","COMPLETED")
        val stopped = journalRun("stopped",free=true)
        val receipts = listOf(WorkoutCompletionEntity("cross",crossing.slotId,"2026-11-09",3000,null,false),
            WorkoutCompletionEntity("legacy","gone","2024-02-29",1,null,false,true),
            WorkoutCompletionEntity("demo","slot-demo","2026-11-09",4000,null,true))
        val projected = WorkoutJournalRepository.project(listOf(crossing,uncredited,stopped,journalRun("demo",demo=true)),receipts,emptyList())
        assertEquals(setOf("cross","uncredited","stopped","legacy"),projected.map { it.id }.toSet())
        assertEquals("2026-11-09",projected.first { it.id=="cross" }.localDate)
        assertEquals("Europe/Moscow",projected.first { it.id=="cross" }.run!!.zoneId)
        assertFalse(projected.first { it.id=="uncredited" }.credited)
        assertEquals("Завершение не подтверждено",projected.first { it.id=="uncredited" }.statusRu)
        assertTrue(projected.first { it.id=="stopped" }.isFree)
        assertEquals("2024-02-29",projected.last().localDate)
        assertNull(projected.last().run)
        assertEquals(2,projected.count { it.credited })
    }

    @Test fun readOnlyTransactionPreservesCanonicalCountsHoldTimeFreezeAndEnergy() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context,VelarisDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.velarisDao()
            dao.insertWorkoutRuns(listOf(journalRun("planned","COMPLETED"),journalRun("free","COMPLETED",free=true),journalRun("stopped"),journalRun("demo",demo=true)))
            dao.insertWorkoutCompletions(listOf(WorkoutCompletionEntity("planned","slot-planned","2026-11-09",3000,null,false),
                WorkoutCompletionEntity("free","slot-free","2026-11-09",4000,null,false,true),WorkoutCompletionEntity("demo","slot-demo","2026-11-09",5000,null,true)))
            val repository = WorkoutPersistenceRepository(dao,SystemTimeProvider())
            val before = repository.loadWeeklyOverview("2026-11-09",manualFrozenDates=setOf("2026-11-07"))
            for (table in listOf("workout_runs","workout_completions","daily_slots")) for (event in listOf("INSERT","UPDATE","DELETE"))
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER forbid_${table}_$event BEFORE $event ON $table BEGIN SELECT RAISE(ABORT,'read only'); END")
            val rows = WorkoutJournalRepository(db).load()
            assertEquals(3,rows.size)
            assertEquals(2,rows.count { it.credited })
            assertEquals(15,dao.observeWorkoutEnergy().first())
            assertEquals(before,repository.loadWeeklyOverview("2026-11-09",manualFrozenDates=setOf("2026-11-07")))
        } finally { db.close() }
    }
}
