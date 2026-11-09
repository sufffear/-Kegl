package com.example.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Recovery is exceptional startup work. No UI/worker may read partially restored stores. */
object RestoreStartupGate {
    /** Foreground launch only, before creating the workout coordinator or any game UI. */
    fun ensureFinalStart(context: Context) = runBlocking(Dispatchers.IO) {
        val db = VelarisDatabase.getInstance(context)
        val clock = com.example.domain.SystemTimeProvider()
        BackupRestoreManager(context, db.velarisDao(), PreferencesRepository(context),
            StoryRepository(context, db.velarisDao(), clock), clock, db).startFinalGameIfNeeded().getOrThrow()
    }

    fun ensureRecovered(context: Context) {
        val directory = context.applicationContext.filesDir
        if (!File(directory,"restore_recovery.json").exists() && !File(directory,"restore_recovery.json.bak").exists()) return
        runBlocking(Dispatchers.IO) {
            val db = VelarisDatabase.getInstance(context)
            val clock = com.example.domain.SystemTimeProvider()
            BackupRestoreManager(context,db.velarisDao(),PreferencesRepository(context),
                StoryRepository(context,db.velarisDao(),clock),clock,db).recoverPendingRestore().getOrThrow()
        }
    }
}
