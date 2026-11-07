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
    private val chantal get() = ChantalProgressRepository(context)

    @Before fun setup() {
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
        context, db.velarisDao(), repository, StoryRepository(context, db.velarisDao(), clock), clock
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
        val bytes = archive()
        chantal.save(JSONObject().put("coins", 999))
        prefs.updateSettings(BenchSignalSettings())
        db.velarisDao().deleteRealTrainingPlans()
        manager().restoreBackup(bytes.inputStream(), false).getOrThrow()
        assertEquals(listOf(plan), db.velarisDao().getRealTrainingPlans())
        assertEquals(42, chantal.load().getInt("coins"))
        assertEquals(1234L, chantal.load().getLong("activeJourneyStartMs"))
        assertEquals(settings, prefs.signalSettingsFlow.first())
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
}
