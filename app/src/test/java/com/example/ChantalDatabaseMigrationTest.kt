package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChantalDatabaseMigrationTest {
    @Test fun versionFiveUpgradePreservesExistingTablesAndValidatesRoomSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "chantal-migration-test.db"
        context.deleteDatabase(name)
        // Existing v5 tables are unchanged by v6. Remove only the additive v6 tables
        // to construct a v5 fixture, then let Room run and validate the real migration.
        Room.databaseBuilder(context, VelarisDatabase::class.java, name).allowMainThreadQueries().build().let { db ->
            db.openHelper.writableDatabase.execSQL("INSERT INTO custom_programs VALUES ('retained', 'Программа', 'Описание', 0, '[]', 1, 1)")
            db.close()
        }
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
            old.execSQL("DROP TABLE chantal_restore_markers")
            old.execSQL("DROP TABLE chantal_aggregate")
            old.execSQL("DROP TABLE chantal_confirmations")
            old.execSQL("DROP TABLE chantal_journal_events")
            old.version = 5
        }
        val db = Room.databaseBuilder(context, VelarisDatabase::class.java, name)
            .addMigrations(VelarisDatabase.MIGRATION_5_6).allowMainThreadQueries().build()
        try {
            assertNull(db.chantalDao().aggregate())
            db.openHelper.readableDatabase.query("SELECT title FROM custom_programs WHERE id = 'retained'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Программа", cursor.getString(0))
            }
            db.chantalDao().putAggregate(ChantalAggregateEntity(revision = 0, payload = null))
            assertNotNull(db.chantalDao().aggregate())
            assertNull(db.chantalDao().aggregate()!!.payload)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun duplicateDecisionRollsBackAggregateAndJournalTogether() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.chantalDao()
            dao.putAggregate(ChantalAggregateEntity(revision = 1, payload = "before"))
            dao.insertReceipt(ChantalConfirmationEntity("episode1", "decision1"))
            assertTrue(runCatching {
                db.runInTransaction {
                    dao.putAggregate(ChantalAggregateEntity(revision = 2, payload = "after"))
                    dao.insertEvent(ChantalJournalEventEntity("event2", "{}"))
                    dao.insertReceipt(ChantalConfirmationEntity("episode2", "decision1"))
                }
            }.isFailure)
            assertEquals("before", dao.aggregate()!!.payload)
            assertEquals(1L, dao.aggregate()!!.revision)
            assertNull(dao.event("event2"))
            assertNull(dao.receipt("episode2"))
        } finally { db.close() }
    }
}
