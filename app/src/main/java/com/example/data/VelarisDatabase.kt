package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Локальная база данных Room проекта Velaris (ТЗ п. 9, Update v1.1 п. 6–8).
 * Запрещён destructive fallback (`fallbackToDestructiveMigration` не используется);
 * поддерживаются явные миграции `MIGRATION_1_2` и `MIGRATION_2_3` без потери пользовательских данных.
 */
@Database(
    entities = [
        TrainingPlanEntity::class,
        PlanAssignmentEntity::class,
        DailyPlanEntity::class,
        DailySlotEntity::class,
        WorkoutRunEntity::class,
        WorkoutCompletionEntity::class,
        ImportedNovelEntity::class,
        NovelChapterEntity::class,
        NovelBlockEntity::class,
        NovelReadingProgressEntity::class,
        ReadingProgressEntity::class,
        FreeIntervalTemplateEntity::class,
        CustomProgramEntity::class,
        ChantalAggregateEntity::class,
        ChantalConfirmationEntity::class,
        ChantalJournalEventEntity::class,
        ChantalRestoreMarkerEntity::class,
        TrainingConfiguration::class,
        ExtraTrainingCycle::class,
        WorkoutExecutionParameters::class
    ],
    version = 7,
    exportSchema = true
)
abstract class VelarisDatabase : RoomDatabase() {

    abstract fun velarisDao(): VelarisDao
    abstract fun chantalDao(): ChantalDao

    companion object {
        @Volatile
        private var INSTANCE: VelarisDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_progress` (
                        `storyId` TEXT NOT NULL,
                        `currentChapterId` TEXT NOT NULL,
                        `currentNodeId` TEXT NOT NULL,
                        `readerModeId` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`storyId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `story_choice_selections`")
                db.execSQL("DROP TABLE IF EXISTS `story_flags`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `imported_novels` (
                        `slotKey` TEXT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `explicitTitle` TEXT,
                        `displayTitle` TEXT NOT NULL,
                        `sha256Hex` TEXT NOT NULL,
                        `byteSize` INTEGER NOT NULL,
                        `internalFilePath` TEXT NOT NULL,
                        `actHeadingLevel` INTEGER NOT NULL,
                        `chapterHeadingLevel` INTEGER NOT NULL,
                        `actCount` INTEGER NOT NULL,
                        `chapterCount` INTEGER NOT NULL,
                        `totalBlockCount` INTEGER NOT NULL,
                        `importedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`slotKey`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `novel_chapters` (
                        `chapterId` TEXT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `chapterNumber` INTEGER NOT NULL,
                        `actId` TEXT NOT NULL,
                        `actNumber` INTEGER NOT NULL,
                        `actTitle` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `blockCount` INTEGER NOT NULL,
                        `stepCount` INTEGER NOT NULL,
                        PRIMARY KEY(`chapterId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_novel_chapters_bookId_chapterNumber` ON `novel_chapters` (`bookId`, `chapterNumber`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `novel_blocks` (
                        `blockId` TEXT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `chapterId` TEXT NOT NULL,
                        `orderInChapter` INTEGER NOT NULL,
                        `globalOrder` INTEGER NOT NULL,
                        `stepIndex` INTEGER NOT NULL,
                        `stepId` TEXT NOT NULL,
                        `blockType` TEXT NOT NULL,
                        `subheadingLevel` INTEGER NOT NULL,
                        `plainText` TEXT NOT NULL,
                        `inlineSpansEncoded` TEXT NOT NULL,
                        PRIMARY KEY(`blockId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_novel_blocks_bookId_chapterId_orderInChapter` ON `novel_blocks` (`bookId`, `chapterId`, `orderInChapter`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_novel_blocks_bookId_globalOrder` ON `novel_blocks` (`bookId`, `globalOrder`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `novel_reading_progress` (
                        `bookId` TEXT NOT NULL,
                        `currentActId` TEXT NOT NULL,
                        `currentChapterId` TEXT NOT NULL,
                        `currentBlockId` TEXT NOT NULL,
                        `currentStepIndex` INTEGER NOT NULL,
                        `readerModeId` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`bookId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `free_interval_templates` (
                        `slotIndex` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetDurationMinutes` INTEGER NOT NULL,
                        `targetDurationSeconds` INTEGER NOT NULL,
                        `contractionSec` INTEGER NOT NULL,
                        `relaxationSec` INTEGER NOT NULL,
                        `isConfigured` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`slotIndex`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `workout_runs` ADD COLUMN `isFreeMode` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `workout_completions` ADD COLUMN `isFreeMode` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_completions_localDate_isDemo_isFreeMode` ON `workout_completions` (`localDate`, `isDemo`, `isFreeMode`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_programs` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `blocksJson` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `chantal_restore_markers` (`operationId` TEXT NOT NULL, PRIMARY KEY(`operationId`))")
                // Do not import preferences here: the repository validates and imports them atomically.
                db.execSQL("CREATE TABLE IF NOT EXISTS `chantal_aggregate` (`id` INTEGER NOT NULL, `revision` INTEGER NOT NULL, `payload` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `chantal_confirmations` (`episodeId` TEXT NOT NULL, `decisionId` TEXT NOT NULL, PRIMARY KEY(`episodeId`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chantal_confirmations_decisionId` ON `chantal_confirmations` (`decisionId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `chantal_journal_events` (`eventId` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`eventId`))")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_execution_parameters` (`runId` TEXT NOT NULL, `parametersJson` TEXT NOT NULL, `totalMs` INTEGER NOT NULL, `holdMs` INTEGER NOT NULL, PRIMARY KEY(`runId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `training_configurations` (`key` TEXT NOT NULL, `contractionSec` INTEGER NOT NULL, `relaxationSec` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL, PRIMARY KEY(`key`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `extra_training_cycle` (`id` INTEGER NOT NULL, `round` INTEGER NOT NULL, `mask` INTEGER NOT NULL, `celebrationUntilEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getInstance(context: Context): VelarisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VelarisDatabase::class.java,
                    "velaris_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
