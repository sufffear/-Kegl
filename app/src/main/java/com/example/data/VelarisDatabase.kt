package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Локальная база данных Room проекта Velaris (ТЗ п. 9).
 * Запрещён destructive fallback (`fallbackToDestructiveMigration` не используется);
 * поддерживаются явные миграции.
 */
@Database(
    entities = [
        TrainingPlanEntity::class,
        PlanAssignmentEntity::class,
        DailyPlanEntity::class,
        DailySlotEntity::class,
        WorkoutRunEntity::class,
        WorkoutCompletionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VelarisDatabase : RoomDatabase() {

    abstract fun velarisDao(): VelarisDao

    companion object {
        @Volatile
        private var INSTANCE: VelarisDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Задел под будущие миграции этапа 3/4 без потери пользовательских данных
            }
        }

        fun getInstance(context: Context): VelarisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VelarisDatabase::class.java,
                    "velaris_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
