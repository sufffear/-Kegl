package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class TrainingConfigurationCycleTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: VelarisDatabase
    private lateinit var repo: WorkoutPersistenceRepository
    private val date="2026-11-09"
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(context,VelarisDatabase::class.java).allowMainThreadQueries().build()
        repo=WorkoutPersistenceRepository(db.velarisDao(),SystemTimeProvider())
    }
    @After fun close() {db.close()}
    private suspend fun slots()=repo.getOrCreateDailySlots(date,WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3,false)
    private fun done(id: Int,receipt: String="r$id",status: RunStatus=RunStatus.COMPLETED)=PhaseEngineSnapshot(
        runId=receipt,slotId="free_${id}_$receipt",startDate=date,status=status,isDemo=false,isFreeMode=true,
        protocol=DemoProtocolConfig("free_slot_$id","Extra",1000,1000,2),confirmedActiveMs=4000)

    @Test fun nineIndependentConfigurationsDefaultsDurationsAndSingleReset()=runBlocking {
        val factory=slots()
        assertEquals(9,factory.size)
        factory.forEachIndexed {i,item->db.velarisDao().saveTrainingConfiguration(TrainingConfiguration.from(item.slot).copy(contractionSec=i+2,relaxationSec=3,repetitions=7))}
        val configured=slots()
        assertEquals((2..10).toList(),configured.map{(it.slot.contractionMs/1000).toInt()})
        assertEquals((2..10).map{(it+3)*7*1000L},configured.map{(it.slot.contractionMs+it.slot.relaxationMs)*it.slot.repetitions})
        db.velarisDao().resetTrainingConfiguration(TrainingConfiguration.from(factory[0].slot).key)
        val reset=slots()
        assertEquals(factory[0].slot,reset[0].slot)
        assertEquals(configured.drop(1),reset.drop(1))
        assertEquals(listOf(200000L,150000L,350000L),factory.take(3).map{TrainingConfiguration.from(it.slot).durationMs})
    }
    @Test fun unsavedDraftNeverChangesPersistedValuesOrPastRunAndCompletion()=runBlocking {
        val factory=slots();val item=factory.first()
        val saved=TrainingConfiguration.from(item.slot).copy(contractionSec=2,repetitions=8)
        db.velarisDao().saveTrainingConfiguration(saved)
        val draft=saved.copy(contractionSec=9)
        assertNotEquals(draft,TrainingConfiguration.from(slots().first().slot))
        val completed=PhaseEngineSnapshot(runId="mandatory",slotId=item.slot.id,startDate=date,status=RunStatus.COMPLETED,
            isDemo=false,protocol=repo.slotToProtocolConfig(saved.applyTo(item.slot)),confirmedActiveMs=saved.durationMs)
        assertTrue(repo.completeRunAtomically(completed))
        db.velarisDao().saveTrainingConfiguration(draft)
        assertTrue(slots().first().isCompleted)
        assertEquals(2000L,db.velarisDao().getRunById("mandatory")!!.contractionMs)
        val summary=repo.loadWeeklyOverview(date,date)
        val session=summary.days.first{it.localDate==date}.completedSessions.single()
        assertEquals(24,session.durationSeconds)
        assertEquals(16,session.holdSeconds)
    }
    @Test fun extrasAreSharedAcrossAllPeriodsAndUseExistingDefaults()=runBlocking {
        val factory=ExtraWorkoutPresets.all
        val saved=TrainingConfiguration.from(factory[1]).copy(contractionSec=8,repetitions=12)
        db.velarisDao().saveTrainingConfiguration(saved)
        PlanGroup.entries.forEach { _ ->
            val settings=db.velarisDao().getTrainingConfigurations().single()
            val template=settings.applyTo(factory[1])
            assertEquals(12,template.calculatedCycles);assertEquals(156,template.actualDurationSec)
        }
        assertEquals(listOf(90,20,10),factory.map{it.calculatedCycles})
        assertEquals(listOf(180000L,300000L,350000L),factory.map{TrainingConfiguration.from(it).durationMs})
    }
    @Test fun cycleAnyOrderDedupeLocksAndCustomAndInterruptedRunsExcluded()=runBlocking {
        repo.saveRunCheckpoint(done(101,"cancel",RunStatus.STOPPED))
        assertFalse(repo.completeRunAtomically(done(101,"another_cancel",RunStatus.STOPPED)))
        assertFalse(repo.completeRunAtomically(done(101,"interrupted",RunStatus.INTERRUPTED)))
        assertNull(db.velarisDao().getExtraTrainingCycle())
        assertTrue(repo.completeRunAtomically(done(103)))
        var cycle=db.velarisDao().getExtraTrainingCycle()!!
        assertTrue(cycle.isLocked(103));assertFalse(cycle.isLocked(101));assertFalse(cycle.isLocked(102))
        assertFalse(repo.completeRunAtomically(done(103)))
        repo.getOrCreateDailySlots("2026-11-10",WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3,false)
        assertEquals(4,db.velarisDao().getExtraTrainingCycle()!!.mask)
        repo.completeRunAtomically(done(104).copy(slotId="prog_custom"))
        assertEquals(4,db.velarisDao().getExtraTrainingCycle()!!.mask)
        repo.completeRunAtomically(done(101))
        assertEquals(5,db.velarisDao().getExtraTrainingCycle()!!.mask)
        db.velarisDao().saveTrainingConfiguration(TrainingConfiguration("extra:101",2,2,7))
        assertEquals(5,db.velarisDao().getExtraTrainingCycle()!!.mask)
        repo.completeRunAtomically(done(102))
        cycle=db.velarisDao().getExtraTrainingCycle()!!
        assertEquals(7,cycle.mask);assertTrue(cycle.celebrationUntilEpochMs>0)
        db.velarisDao().finishExtraTrainingCelebration(cycle.round)
        cycle=db.velarisDao().getExtraTrainingCycle()!!
        assertEquals(0,cycle.mask);assertEquals(1L,cycle.round)
        assertFalse(repo.completeRunAtomically(done(102)))
        assertEquals(0,db.velarisDao().getExtraTrainingCycle()!!.mask)
        assertEquals(20,db.velarisDao().observeWorkoutEnergy().first()) // 3 extras + custom, unchanged economy
    }
    @Test fun cycleAndCompletionRollbackTogetherOnStorageFailure()=runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_cycle BEFORE INSERT ON extra_training_cycle BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END")
        assertTrue(runCatching {repo.completeRunAtomically(done(101))}.isFailure)
        assertNull(db.velarisDao().getRunById("r101"))
        assertEquals(0,repo.countRealCompletions())
    }
    @Test fun restartRetainsSettingsProgramsAndPartialRoundAndPendingRoundCanRecover()=runBlocking {
        val name="training-restart.db";context.deleteDatabase(name)
        fun open()=Room.databaseBuilder(context,VelarisDatabase::class.java,name).allowMainThreadQueries().build()
        var disk=open();var repository=WorkoutPersistenceRepository(disk.velarisDao(),SystemTimeProvider())
        try {
            val settings=TrainingConfiguration("extra:101",2,1,80)
            disk.velarisDao().saveTrainingConfiguration(settings)
            val program=CustomWorkoutProgram(id="legacy-program",title="Моя программа",blocks=listOf(WorkoutBlock()))
            repository.saveCustomProgram(program)
            repository.completeRunAtomically(done(102))
            disk.close();disk=open();repository=WorkoutPersistenceRepository(disk.velarisDao(),SystemTimeProvider())
            assertEquals(settings,disk.velarisDao().getTrainingConfigurations().single())
            assertEquals(program.id,repository.getAllCustomPrograms().single().id)
            assertEquals(2,disk.velarisDao().getExtraTrainingCycle()!!.mask)
            repository.completeRunAtomically(done(101));repository.completeRunAtomically(done(103))
            disk.close();disk=open()
            val round=disk.velarisDao().getExtraTrainingCycle()!!
            assertEquals(7,round.mask)
            disk.velarisDao().finishExtraTrainingCelebration(round.round)
            assertEquals(0,disk.velarisDao().getExtraTrainingCycle()!!.mask)
        } finally {disk.close();context.deleteDatabase(name)}
    }
    @Test fun backupsRoundTripNewSettingsCycleAndProgramsWithoutBreakingOldArchives()=runBlocking {
        val settings=TrainingConfiguration("mandatory:real_v2_morning_fast",2,1,80)
        val snapshot=BackupDatabaseSnapshot(trainingConfigurations=listOf(settings),extraTrainingCycle=ExtraTrainingCycle(mask=5))
        val restored=BackupDatabaseSnapshot.fromJson(snapshot.toJson())
        assertEquals(listOf(settings),restored.trainingConfigurations)
        assertEquals(5,restored.extraTrainingCycle!!.mask)
        val old=BackupDatabaseSnapshot.fromJson("{}")
        assertNull(old.trainingConfigurations);assertNull(old.extraTrainingCycle)
    }
    @Test fun deletingLastProgramDoesNotResurrectStarterPrograms()=runBlocking {
        val program=CustomWorkoutProgram(id="last",title="Последняя",blocks=listOf(WorkoutBlock()))
        repo.saveCustomProgram(program)
        repo.deleteCustomProgram(program.id)
        assertTrue(repo.getAllCustomPrograms().isEmpty())
        assertTrue(WorkoutPersistenceRepository(db.velarisDao(),SystemTimeProvider()).getAllCustomPrograms().isEmpty())
    }
    @Test fun concurrentCompletionCallbacksHaveOneReceiptAndOneCycleCredit()=runBlocking {
        val results=kotlinx.coroutines.coroutineScope {
            (1..12).map {async(kotlinx.coroutines.Dispatchers.IO) {repo.completeRunAtomically(done(101))}}.let { jobs ->
                jobs.map {it.await()}
            }
        }
        assertEquals(1,results.count {it})
        assertEquals(1,db.velarisDao().getExtraTrainingCycle()!!.mask)
        assertEquals(1,repo.countRealCompletions())
        assertEquals(5,db.velarisDao().observeWorkoutEnergy().first())
    }
    @Test fun constructorHistoryRetainsFullExecutedSequenceAfterProgramEdits()=runBlocking {
        val program=CustomWorkoutProgram(id="history",title="История",blocks=listOf(
            WorkoutBlock(contractionSec=7,relaxationSec=3,repetitions=2),
            WorkoutBlock(type=WorkoutBlockType.CUSTOM_SEQUENCE,sequenceCycles=2,customSteps=listOf(
                CustomSequenceStep(durationSec=4),CustomSequenceStep(phaseType=PhaseType.RELAXATION,durationSec=6)))))
        val snapshot=done(104,"history").copy(slotId="prog_history",isCustomProgram=true,
            confirmedActiveMs=program.totalDurationMs,protocol=DemoProtocolConfig(program.id,program.title,1000,5000,4))
        repo.saveRunCheckpoint(snapshot.copy(status=RunStatus.RUNNING),program)
        repo.saveCustomProgram(program.copy(blocks=listOf(WorkoutBlock())))
        assertTrue(repo.completeRunAtomically(snapshot,program=program.copy(blocks=listOf(WorkoutBlock()))))
        val execution=db.velarisDao().getExecutionParameters().single()
        assertEquals(40000L,execution.totalMs);assertEquals(22000L,execution.holdMs)
        assertEquals(2,org.json.JSONArray(execution.parametersJson).length())
        val report=repo.loadWeeklyOverview(date,date).days.first{it.localDate==date}.completedSessions.single()
        assertEquals(40,report.durationSeconds);assertEquals(22,report.holdSeconds)
        val backup=BackupDatabaseSnapshot(executionParameters=listOf(execution))
        assertEquals(listOf(execution),BackupDatabaseSnapshot.fromJson(backup.toJson()).executionParameters)
    }
    @Test fun versionSixMigrationPreservesExistingDataAndCreatesValidatedTables() {
        val name="training-migration.db";context.deleteDatabase(name)
        Room.databaseBuilder(context,VelarisDatabase::class.java,name).allowMainThreadQueries().build().let { database ->
            database.openHelper.writableDatabase.execSQL("INSERT INTO custom_programs VALUES ('retained', 'Программа', '', 0, '[]', 1, 1)")
            database.close()
        }
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,SQLiteDatabase.OPEN_READWRITE).use {old ->
            old.execSQL("DROP TABLE workout_execution_parameters");old.execSQL("DROP TABLE training_configurations");old.execSQL("DROP TABLE extra_training_cycle");old.version=6
        }
        val upgraded=Room.databaseBuilder(context,VelarisDatabase::class.java,name).addMigrations(VelarisDatabase.MIGRATION_6_7).allowMainThreadQueries().build()
        try {
            upgraded.openHelper.readableDatabase.query("SELECT id FROM custom_programs").use{assertTrue(it.moveToFirst());assertEquals("retained",it.getString(0))}
            upgraded.openHelper.readableDatabase.query("SELECT * FROM training_configurations").use{assertEquals(0,it.count)}
            upgraded.openHelper.readableDatabase.query("SELECT * FROM extra_training_cycle").use{assertEquals(0,it.count)}
        } finally {upgraded.close();context.deleteDatabase(name)}
    }
}
