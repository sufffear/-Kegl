package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.SystemTimeProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRestoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: VelarisDatabase
    private lateinit var prefs: PreferencesRepository
    private lateinit var store: DataStore<Preferences>
    private lateinit var scope: CoroutineScope
    private lateinit var prefsFile: File
    private val clock = SystemTimeProvider()
    private val chantal get() = ChantalProgressRepository(context, db)

    @Before fun setup() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        prefsFile = File(context.cacheDir, "test-${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { prefsFile })
        prefs = PreferencesRepository(store)
        chantal.replace(null)
        File(context.filesDir, "imported_books").deleteRecursively()
    }

    @After fun close() {
        db.close()
        scope.cancel()
        prefsFile.delete()
    }

    private fun manager(repository: PreferencesRepository = prefs) = BackupRestoreManager(
        context, db.velarisDao(), repository, StoryRepository(context, db.velarisDao(), clock), clock, database = db
    )

    private suspend fun archive(): ByteArray {
        val bytes = ByteArrayOutputStream()
        manager().createBackup(bytes).getOrThrow()
        return bytes.toByteArray()
    }

    private fun rewrite(bytes: ByteArray, edit: (MutableMap<String, ByteArray>) -> Unit): ByteArray {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        edit(entries)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, data) -> zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry() }
        }
        return output.toByteArray()
    }

    @Test fun roundTripRestoresCompanionSettingsAndTraining() = runBlocking {
        val plan = TrainingPlanEntity("saved", 1, "План", "Личный", "2026-11-07", false)
        db.velarisDao().insertTrainingPlans(listOf(plan))
        chantal.save(JSONObject().put("coins", 42).put("maturityXp", 13).put("activeJourneyStartMs", 1234L))
        val settings = BenchSignalSettings(soundEnabled = false, duckOtherApps = true, frozenDates = setOf("2026-11-06"))
        prefs.updateSettings(settings)
        val custom = CustomProgramEntity("custom", "Личная программа")
        db.velarisDao().upsertCustomProgram(custom)
        val bytes = archive()
        db.velarisDao().deleteAllCustomPrograms()
        chantal.save(JSONObject().put("coins", 999))
        prefs.updateSettings(BenchSignalSettings())
        db.velarisDao().deleteRealTrainingPlans()
        manager().restoreBackup(bytes.inputStream(), false).getOrThrow()
        assertEquals(listOf(plan), db.velarisDao().getRealTrainingPlans())
        assertEquals(42, chantal.load().getInt("coins"))
        assertEquals(1234L, chantal.load().getLong("activeJourneyStartMs"))
        assertEquals(settings, prefs.signalSettingsFlow.first())
        assertEquals(listOf(custom), db.velarisDao().getAllCustomPrograms())
    }

    @Test fun writeFailureRollsBackRoomAndSettings() = runBlocking {
        prefs.updateSettings(BenchSignalSettings(soundEnabled = false))
        val bytes = archive()
        val existing = TrainingPlanEntity("keep", 1, "Оставить", "Личный", "2026-11-07", false)
        db.velarisDao().insertTrainingPlans(listOf(existing))
        chantal.save(JSONObject().put("coins", 71))
        val original = BenchSignalSettings(soundEnabled = true, signalVolume = 0.25f)
        prefs.updateSettings(original)
        var fail = true
        val failingStore = object : DataStore<Preferences> {
            override val data = store.data
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                val updated = store.updateData(transform)
                if (fail) { fail = false; throw IOException("Injected failure after settings write") }
                return updated
            }
        }
        val result = manager(PreferencesRepository(failingStore)).restoreBackup(bytes.inputStream(), false)
        assertTrue(result.isFailure)
        assertEquals(listOf(existing), db.velarisDao().getRealTrainingPlans())
        assertEquals(original, prefs.signalSettingsFlow.first())
        assertEquals(71, chantal.load().getInt("coins"))
    }

    @Test fun bookStorageFailureRollsBackCompanionAndDatabase() = runBlocking {
        chantal.save(JSONObject().put("coins", 12))
        val bytes = archive()
        val existing = TrainingPlanEntity("keep", 1, "План", "Личный", "2026-11-07", false)
        db.velarisDao().insertTrainingPlans(listOf(existing))
        chantal.save(JSONObject().put("coins", 91))
        // A regular file blocks the book directory, failing AFTER preferences and Chantal writes.
        val blocker = File(context.filesDir, "imported_books")
        blocker.writeText("blocked")
        try {
            assertTrue(manager().restoreBackup(bytes.inputStream(), false).isFailure)
            assertEquals(91, chantal.load().getInt("coins"))
            assertEquals(listOf(existing), db.velarisDao().getRealTrainingPlans())
        } finally { blocker.delete() }
    }

    @Test fun legacyArchivePreservesExistingCompanion() = runBlocking {
        val legacy = rewrite(archive()) { files ->
            files.remove(BackupManifest.CHANTAL_FILE_NAME)
            val manifest = JSONObject(String(files.getValue(BackupManifest.MANIFEST_FILE_NAME)))
            manifest.put("formatVersion", 1)
            manifest.getJSONObject("files").remove(BackupManifest.CHANTAL_FILE_NAME)
            files[BackupManifest.MANIFEST_FILE_NAME] = manifest.toString().toByteArray()
        }
        chantal.save(JSONObject().put("coins", 88))
        manager().restoreBackup(legacy.inputStream(), false).getOrThrow()
        assertEquals(88, chantal.load().getInt("coins"))
    }

    @Test fun alteredCompanionIsRejectedBeforeAnyWrites() = runBlocking {
        val corrupt = rewrite(archive()) { it[BackupManifest.CHANTAL_FILE_NAME] = "{}".toByteArray() }
        chantal.save(JSONObject().put("coins", 65))
        assertTrue(manager().restoreBackup(corrupt.inputStream(), false).isFailure)
        assertEquals(65, chantal.load().getInt("coins"))
    }

    @Test fun omittedChecksumCannotBypassIntegrityCheck() = runBlocking {
        val corrupt = rewrite(archive()) { files ->
            val manifest = JSONObject(String(files.getValue(BackupManifest.MANIFEST_FILE_NAME)))
            manifest.getJSONObject("files").remove(BackupManifest.PREFERENCES_FILE_NAME)
            files[BackupManifest.MANIFEST_FILE_NAME] = manifest.toString().toByteArray()
        }
        assertTrue(manager().restoreBackup(corrupt.inputStream(), false).isFailure)
    }

    @Test fun activeWorkoutBlocksRestore() = runBlocking {
        val bytes = archive()
        chantal.save(JSONObject().put("coins", 19))
        assertTrue(manager().restoreBackup(bytes.inputStream(), true).isFailure)
        assertEquals(19, chantal.load().getInt("coins"))
    }
    private fun preferencesFromArchive(bytes: ByteArray): String {
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: error("Preferences missing")
                if (entry.name == BackupManifest.PREFERENCES_FILE_NAME) return zip.readBytes().toString(Charsets.UTF_8)
            }
        }
    }
    @Test fun interruptedRestoreWithoutCommitMarkerRecoversExternalPreimages() = runBlocking {
        val oldPreferences = preferencesFromArchive(archive())
        prefs.updateSettings(prefs.signalSettingsFlow.first().copy(soundEnabled=false))
        val directory=File(context.filesDir,"imported_books").apply { mkdirs() }
        File(directory,StoryRepository.ACTIVE_SOURCE_FILE_NAME).writeText("new partial book")
        RestoreRecoveryJournal(context.filesDir).write(JSONObject().put("operationId","interrupted")
            .put("preferences",oldPreferences).put("bookExisted",false).put("bookBase64",""))
        manager().recoverPendingRestore().getOrThrow()
        assertTrue(prefs.signalSettingsFlow.first().soundEnabled)
        assertFalse(File(directory,StoryRepository.ACTIVE_SOURCE_FILE_NAME).exists())
        manager().recoverPendingRestore().getOrThrow()
    }
    @Test fun committedRestoreKeepsNewExternalStateWhenJournalCleanupWasInterrupted() = runBlocking {
        val oldPreferences=preferencesFromArchive(archive())
        prefs.updateSettings(prefs.signalSettingsFlow.first().copy(soundEnabled=false))
        db.chantalDao().insertMarker(ChantalRestoreMarkerEntity("committed"))
        RestoreRecoveryJournal(context.filesDir).write(JSONObject().put("operationId","committed")
            .put("preferences",oldPreferences).put("bookExisted",false).put("bookBase64",""))
        manager().recoverPendingRestore().getOrThrow()
        assertFalse(prefs.signalSettingsFlow.first().soundEnabled)
        assertNull(RestoreRecoveryJournal(context.filesDir).read())
    }

    @Test fun finalStartClearsProgressOnceAndPreservesCredentialsSettingsAndPrograms() = runBlocking {
        val settings = BenchSignalSettings(soundEnabled=false, duckOtherApps=true, frozenDates=setOf("2026-11-06"))
        prefs.updateSettings(settings)
        val custom = CustomProgramEntity("retained", "Мой план")
        db.velarisDao().upsertCustomProgram(custom)
        val credentialStores = listOf("chantal_openrouter_access", "chantal_gemini_access", "chantal_ai_selection")
        credentialStores.forEach { context.getSharedPreferences(it,0).edit().putString("sentinel", "dummy-encrypted-test-value").commit() }
        repeat(3) { i -> db.velarisDao().insertCompletionIgnore(WorkoutCompletionEntity("old-$i","old-slot-$i","2026-11-06",1,null,false)) }
        chantal.save(JSONObject().put("coins",42).put("totalJourneys",101).put("maturityXp",2000))
        context.getSharedPreferences("chantal_progress",0).edit().putString("snapshot","{\"schemaVersion\":1,\"coins\":999}").commit()
        assertEquals(30,db.velarisDao().observeWorkoutEnergy().first())
        val oldArchive = archive()
        assertTrue(manager().startFinalGameIfNeeded().getOrThrow())
        assertEquals(0,db.velarisDao().observeWorkoutEnergy().first())
        assertFalse(chantal.load().has("coins"))
        assertNotNull(db.chantalDao().aggregate())
        assertNull(db.chantalDao().aggregate()!!.payload)
        assertEquals(settings.copy(frozenDates=emptySet()),prefs.signalSettingsFlow.first())
        assertEquals(listOf(custom),db.velarisDao().getAllCustomPrograms())
        credentialStores.forEach { assertEquals("dummy-encrypted-test-value",context.getSharedPreferences(it,0).getString("sentinel",null)) }
        chantal.save(JSONObject().put("coins",7).put("totalJourneys",1))
        assertFalse(manager().startFinalGameIfNeeded().getOrThrow())
        assertEquals(7,chantal.load().getInt("coins"))
        // An explicit old-archive restore is allowed and must not re-arm the one-time wipe.
        manager().restoreBackup(oldArchive.inputStream(),false).getOrThrow()
        assertFalse(manager().startFinalGameIfNeeded().getOrThrow())
        assertEquals(42,chantal.load().getInt("coins"))
        assertEquals(30,db.velarisDao().observeWorkoutEnergy().first())
    }

    @Test fun finalStartMarkerFailureRollsBackGameTrainingAndExternalSettings() = runBlocking {
        val before = BenchSignalSettings(soundEnabled=false,frozenDates=setOf("2026-11-06"))
        prefs.updateSettings(before)
        chantal.save(JSONObject().put("coins",17))
        db.velarisDao().insertCompletionIgnore(WorkoutCompletionEntity("before","before-slot","2026-11-06",1,null,false))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_final_start BEFORE INSERT ON chantal_restore_markers BEGIN SELECT RAISE(ABORT,'injected'); END")
        assertTrue(manager().startFinalGameIfNeeded().isFailure)
        assertEquals(17,chantal.load().getInt("coins"))
        assertEquals(10,db.velarisDao().observeWorkoutEnergy().first())
        assertEquals(before,prefs.signalSettingsFlow.first())
        assertFalse(db.chantalDao().markerPresent(BackupRestoreManager.FINAL_START_ID))
        assertNull(RestoreRecoveryJournal(context.filesDir).read())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_final_start")
        assertTrue(manager().startFinalGameIfNeeded().getOrThrow())
        assertEquals(0,db.velarisDao().observeWorkoutEnergy().first())
    }

    @Test fun interruptedFinalStartCleanupKeepsCommittedResetAndLaterProgress() = runBlocking {
        prefs.updateSettings(BenchSignalSettings(frozenDates=setOf("2026-11-06")))
        val oldPreferences = preferencesFromArchive(archive())
        assertTrue(manager().startFinalGameIfNeeded().getOrThrow())
        chantal.save(JSONObject().put("coins",9))
        RestoreRecoveryJournal(context.filesDir).write(JSONObject().put("operationId",BackupRestoreManager.FINAL_START_ID)
            .put("preferences",oldPreferences).put("bookExisted",false).put("bookBase64",""))
        manager().recoverPendingRestore().getOrThrow()
        assertEquals(9,chantal.load().getInt("coins"))
        assertTrue(prefs.signalSettingsFlow.first().frozenDates.isEmpty())
        assertFalse(manager().startFinalGameIfNeeded().getOrThrow())
        assertNull(RestoreRecoveryJournal(context.filesDir).read())
    }

    @Test fun emptyInstallAndRepeatedFinalStartAreIdempotentAndBackupVersionIsCurrent() = runBlocking {
        assertTrue(manager().startFinalGameIfNeeded().getOrThrow())
        assertFalse(manager().startFinalGameIfNeeded().getOrThrow())
        assertEquals(0,db.velarisDao().observeWorkoutEnergy().first())
        assertFalse(chantal.load().has("coins"))
        val manifest = manager().createBackup(ByteArrayOutputStream()).getOrThrow()
        assertEquals(com.example.BuildConfig.VERSION_NAME,manifest.appVersion)
        assertEquals(com.example.BuildConfig.VERSION_CODE,manifest.appVersionCode)
    }

}
