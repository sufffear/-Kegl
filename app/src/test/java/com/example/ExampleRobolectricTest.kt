package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.StoryRepository
import com.example.data.VelarisDatabase
import com.example.data.WorkoutPersistenceRepository
import com.example.domain.MarkdownNovelParser
import com.example.domain.MetronomeTickStyle
import com.example.domain.NeutralTestMarkdownFactory
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.RunStatus
import com.example.domain.SoundPack
import com.example.domain.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private class FakeControllableTimeProvider(
        var elapsedMs: Long = 10_000L,
        var wallMs: Long = 1_762_128_000_000L,
        var localDate: String = "2026-11-03",
        var zoneId: String = "Europe/Moscow",
        var bootSession: String = "boot_session_1"
    ) : TimeProvider {
        override fun elapsedRealtimeMs(): Long = elapsedMs
        override fun currentWallTimeMs(): Long = wallMs
        override fun currentLocalDate(): String = localDate
        override fun currentZoneId(): String = zoneId
        override fun bootSessionId(): String = bootSession
    }

    @Test
    fun `verify v1_2 versionCode 5, applicationId and offline manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Velaris", context.getString(R.string.app_name))
        assertEquals("com.velaris.app", BuildConfig.APPLICATION_ID)
        assertEquals("0.5.0-v1.2", BuildConfig.VERSION_NAME)
        assertEquals(5, BuildConfig.VERSION_CODE)

        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        assertFalse(
            "Приложение не должно запрашивать разрешение INTERNET",
            permissions.contains(Manifest.permission.INTERNET)
        )
        assertTrue(
            "Должно быть объявлено разрешение FOREGROUND_SERVICE_SPECIAL_USE",
            permissions.contains(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        )
    }

    @Test
    fun `verify v1_2 full interface navigation across Home, Plan, Workout, Reader, Settings and Diagnostics`() {
        composeTestRule.waitForIdle()

        // 1. Главная: карточка Тренировка, карточка История («Добавить книгу» когда книга не импортирована), 7-дневный обзор, предупреждение Демо и версия 0.5.0-v1.2
        composeTestRule.onNodeWithTag("demo_safety_banner").assertExists()
        composeTestRule.onNodeWithText("Версия: 0.5.0-v1.2 (versionCode 5)").assertExists()
        composeTestRule.onNodeWithTag("home_workout_card").assertExists()
        composeTestRule.onNodeWithTag("home_story_card").assertExists()
        composeTestRule.onNodeWithTag("weekly_overview_card").assertExists()

        // 2. Переход в План
        composeTestRule.onNodeWithTag("nav_plan").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("daily_plan_persistence_card").assertExists()

        // 3. Переход в Активный подход
        composeTestRule.onNodeWithTag("nav_workout").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("smooth_timer_ring").assertExists()
        composeTestRule.onNodeWithTag("btn_start_demo").assertExists()

        // 4. Переход в Чтение (пустое состояние «Открыть книгу из файла» до импорта)
        composeTestRule.onNodeWithTag("nav_reader").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("story_reader_screen").assertExists()
        composeTestRule.onNodeWithTag("reader_empty_import_card").assertExists()
        composeTestRule.onNodeWithTag("btn_open_book_from_file").assertExists()

        // 5. Переход в Настройки (5 тем, данные, резервные копии и 3 набора звуков) -> Диагностика
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_selector_card").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_dawn").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_sky").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_aurora").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_sunset").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_sakura").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_soft").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_chimes").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_crisp").assertExists()

        // Кнопки резервного копирования и восстановления в «Настройки → Данные» (v1.2 п. 1)
        composeTestRule.onNodeWithTag("btn_create_backup")
            .performScrollTo()
            .assertExists()
        composeTestRule.onNodeWithTag("btn_restore_backup")
            .performScrollTo()
            .assertExists()

        composeTestRule.onNodeWithTag("btn_open_diagnostics")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diagnostics_screen").assertExists()
        composeTestRule.onNodeWithTag("audio_diagnostics_card").assertExists()
    }

    @Test
    fun `weekly 7-day overview includes only real completions and excludes demo completions`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            // Изначально реальных записей нет -> пустое честное состояние
            val initialOverview = repo.loadWeeklyOverview(todayDateStr = "2026-11-03")
            assertEquals(7, initialOverview.days.size)
            assertFalse(initialOverview.hasAnyRealData)
            assertEquals(0, initialOverview.totalRealCompletionsInWeek)

            // Завершаем демо-подход (isDemo = true) -> он НЕ должен попасть в реальный недельный обзор
            val demoSlots = repo.getOrCreateDailySlots(
                localDate = "2026-11-03",
                planId = WorkoutPersistenceRepository.DEMO_PLAN_STANDARD_3,
                isDemo = true
            )
            val demoSlot = demoSlots.first().slot
            val demoProto = repo.slotToProtocolConfig(demoSlot)
            val demoSnap = PhaseEngineSnapshot(
                status = RunStatus.COMPLETED,
                protocol = demoProto,
                currentRepetition = demoProto.repetitions,
                totalRepetitions = demoProto.repetitions,
                currentPhase = PhaseType.RELAXATION,
                phaseRemainingMs = 0L,
                phaseDurationMs = demoProto.relaxationMs,
                prepRemainingMs = 0L,
                confirmedActiveMs = demoProto.totalActiveDurationMs,
                totalActiveDurationMs = demoProto.totalActiveDurationMs,
                interruptionReason = null,
                isStopDialogVisible = false,
                lastSchedulerDriftMs = 0L,
                maxSchedulerDriftMs = 0L,
                snapshotElapsedRealtimeMs = clock.elapsedMs,
                runId = "run_demo_excluded",
                slotId = demoSlot.id,
                startDate = "2026-11-03",
                zoneId = "Europe/Moscow",
                isDemo = true,
                soundPackSnapshot = SoundPack.SOFT,
                metronomeStyleSnapshot = MetronomeTickStyle.SOFT
            )
            assertTrue(repo.completeRunAtomically(demoSnap))

            val overviewAfterDemo = repo.loadWeeklyOverview(todayDateStr = "2026-11-03")
            assertFalse("Демо-подход не должен попадать в реальную статистику", overviewAfterDemo.hasAnyRealData)
            assertEquals(0, overviewAfterDemo.totalRealCompletionsInWeek)

            // Теперь завершаем реальный подход (isDemo = false) за 2026-11-03
            val realSlotsToday = repo.getOrCreateDailySlots(
                localDate = "2026-11-03",
                planId = WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3,
                isDemo = false
            )
            val realSlot = realSlotsToday.first().slot
            val realProto = repo.slotToProtocolConfig(realSlot)
            val realSnap = demoSnap.copy(
                protocol = realProto,
                runId = "run_real_included",
                slotId = realSlot.id,
                isDemo = false
            )
            assertTrue(repo.completeRunAtomically(realSnap))

            val overviewAfterReal = repo.loadWeeklyOverview(todayDateStr = "2026-11-03")
            assertTrue(overviewAfterReal.hasAnyRealData)
            assertEquals(1, overviewAfterReal.totalRealCompletionsInWeek)
            val todaySummary = overviewAfterReal.days.last()
            assertTrue(todaySummary.isToday)
            assertEquals(1, todaySummary.completedCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun `markdown novel repository imports 3 acts 12 chapters, verifies SHA-256 original copy, saves position across restart and mode switch, handles idempotent re-import, and preserves book on corrupted import`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider()
            val storyRepo = StoryRepository(context, db.velarisDao(), clock)
            storyRepo.removeImportedBook()

            // Изначально книги нет
            val emptyState = storyRepo.loadReaderState()
            assertFalse(emptyState.hasImportedBook)

            // 1. Импорт нейтрального тестового Markdown (3 акта, 12 глав)
            val rawMarkdown = NeutralTestMarkdownFactory.buildThreeActsTwelveChaptersMarkdown()
            val rawBytes = rawMarkdown.toByteArray(Charsets.UTF_8)
            val expectedSha256 = MarkdownNovelParser.computeSha256Hex(rawBytes)

            var state = storyRepo.prepareImportPreview(rawBytes, "roman_v1.md")
            assertNotNull(state.pendingImportPreview)
            assertEquals(3, state.pendingImportPreview?.actCount)
            assertEquals(12, state.pendingImportPreview?.chapterCount)
            assertEquals("Нейтральный технический макет (3 акта, 12 глав)", state.pendingImportPreview?.explicitTitle)

            state = storyRepo.confirmPendingImport()
            assertTrue(state.hasImportedBook)
            assertEquals(3, state.actCount)
            assertEquals(12, state.chapterCount)
            assertEquals(expectedSha256, state.sha256Hex)
            assertTrue("Копия исходного файла во внутреннем хранилище должна проходить проверку SHA-256", state.originalBytesChecksumVerified)

            val savedSourceFile = java.io.File(
                java.io.File(context.applicationContext.filesDir, "imported_books"),
                StoryRepository.ACTIVE_SOURCE_FILE_NAME
            )
            assertTrue(savedSourceFile.exists())
            assertArrayEquals(
                "Исходный файл во внутреннем хранилище должен быть побайтово идентичен оригиналу без модификации",
                rawBytes,
                savedSourceFile.readBytes()
            )

            // 4. Одинаковый авторский текст в обоих режимах отображения («Книга» vs «Пошаговый»)
            for (chapter in state.chapters) {
                val bookTexts = chapter.blocks.map { it.plainText }
                val stepTexts = chapter.steps.flatMap { it.blocks }.map { it.plainText }
                assertEquals(bookTexts, stepTexts)
            }

            // 5. Сохранение позиции чтения при переходе между шагами/главами, переключении режима и перезапуске
            state = storyRepo.selectChapter("ch_05")
            assertEquals("ch_05", state.currentChapter?.chapterId)
            state = storyRepo.nextStep()
            val savedChapterId = state.currentChapter?.chapterId.orEmpty()
            val savedBlockId = state.currentBlockId
            val savedStepIndex = state.currentStepIndex
            assertEquals("ch_05", savedChapterId)
            assertTrue(savedStepIndex > 0)

            // Создаём новый экземпляр StoryRepository поверх той же БД Room (имитация перезапуска приложения)
            val restartedRepo = StoryRepository(context, db.velarisDao(), clock)
            val restoredState = restartedRepo.loadReaderState()
            assertTrue(restoredState.hasImportedBook)
            assertEquals("Позиция главы должна сохраняться после перезапуска", savedChapterId, restoredState.currentChapter?.chapterId)
            assertEquals("Позиция блока должна сохраняться после перезапуска и смены режима", savedBlockId, restoredState.currentBlockId)
            assertEquals("Позиция шага должна сохраняться после перезапуска", savedStepIndex, restoredState.currentStepIndex)

            // 6. Повторный импорт того же файла (одинаковый SHA-256) не создаёт дубликат и не сбрасывает позицию чтения
            val previewSame = restartedRepo.prepareImportPreview(rawBytes, "roman_v1_copy.md")
            assertTrue("Предпросмотр должен определять совпадение SHA-256", previewSame.pendingImportPreview?.isIdenticalToCurrentBook == true)
            val afterReimport = restartedRepo.confirmPendingImport()
            assertNotNull(db.velarisDao().getActiveNovel())
            assertEquals("Повторный импорт того же файла НЕ должен сбрасывать главу", savedChapterId, afterReimport.currentChapter?.chapterId)
            assertEquals("Повторный импорт того же файла НЕ должен сбрасывать блок", savedBlockId, afterReimport.currentBlockId)
            assertEquals("Повторный импорт того же файла НЕ должен сбрасывать шаг", savedStepIndex, afterReimport.currentStepIndex)

            // 7. Импорт повреждённого файла не затирает уже установленную книгу и её прогресс
            val corruptedBytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x81.toByte())
            val stateAfterCorruptedPreview = restartedRepo.prepareImportPreview(corruptedBytes, "corrupted.md")
            assertNotNull("Должна отображаться понятная ошибка импорта", stateAfterCorruptedPreview.errorMessageRu)
            assertTrue("Установленная книга должна оставаться доступной", stateAfterCorruptedPreview.hasImportedBook)
            assertEquals(savedChapterId, stateAfterCorruptedPreview.currentChapter?.chapterId)
            assertEquals(savedBlockId, stateAfterCorruptedPreview.currentBlockId)
            assertArrayEquals(rawBytes, savedSourceFile.readBytes())

            // Проверяем прямое отклонение пустого файла через importMarkdownDirect
            val stateAfterEmptyDirect = restartedRepo.importMarkdownDirect("   \n ".toByteArray(Charsets.UTF_8), "empty.md")
            assertNotNull(stateAfterEmptyDirect.errorMessageRu)
            assertTrue(stateAfterEmptyDirect.hasImportedBook)
            assertEquals(savedChapterId, stateAfterEmptyDirect.currentChapter?.chapterId)
            assertEquals(savedBlockId, stateAfterEmptyDirect.currentBlockId)

            // Переход к концу книги -> состояние «История прочитана» без автоматического сброса позиции
            var endState = restartedRepo.selectChapter("ch_12")
            while (!endState.isCompleted) {
                endState = restartedRepo.nextStep()
            }
            assertTrue(endState.isCompleted)
            assertEquals("ch_12", endState.currentChapter?.chapterId)
        } finally {
            db.close()
        }
    }


    @Test
    fun `room persistence supports variable slots N=3 and N=0, atomic completion, and prevents duplicate credit`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            val slotsDay1 = repo.getOrCreateDailySlots(
                localDate = "2026-11-03",
                planId = WorkoutPersistenceRepository.DEMO_PLAN_STANDARD_3,
                isDemo = true
            )
            assertEquals(3, slotsDay1.size)
            assertTrue(slotsDay1.none { it.isCompleted })

            val zeroSlots = repo.getOrCreateDailySlots(
                localDate = "2026-11-04",
                planId = WorkoutPersistenceRepository.DEMO_PLAN_ZERO_SLOTS,
                isDemo = true
            )
            assertEquals(0, zeroSlots.size)

            val targetSlot = slotsDay1.first().slot
            val proto = repo.slotToProtocolConfig(targetSlot)
            val completedSnap = PhaseEngineSnapshot(
                status = RunStatus.COMPLETED,
                protocol = proto,
                currentRepetition = proto.repetitions,
                totalRepetitions = proto.repetitions,
                currentPhase = PhaseType.RELAXATION,
                phaseRemainingMs = 0L,
                phaseDurationMs = proto.relaxationMs,
                prepRemainingMs = 0L,
                confirmedActiveMs = proto.totalActiveDurationMs,
                totalActiveDurationMs = proto.totalActiveDurationMs,
                interruptionReason = null,
                isStopDialogVisible = false,
                lastSchedulerDriftMs = 0L,
                maxSchedulerDriftMs = 0L,
                snapshotElapsedRealtimeMs = clock.elapsedMs,
                runId = "run_atomic_1",
                slotId = targetSlot.id,
                startDate = "2026-11-03",
                zoneId = "Europe/Moscow",
                isDemo = true,
                soundPackSnapshot = SoundPack.SOFT,
                metronomeStyleSnapshot = MetronomeTickStyle.SOFT
            )

            assertTrue(repo.completeRunAtomically(completedSnap))
            val duplicateSnap = completedSnap.copy(runId = "run_atomic_duplicate")
            assertFalse(repo.completeRunAtomically(duplicateSnap))

            assertEquals(1, repo.countDemoCompletions())
            assertEquals(0, repo.countRealCompletions())
        } finally {
            db.close()
        }
    }

    @Test
    fun `room persistence handles midnight and timezone shift and reuses existing date snapshot`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(
                localDate = "2026-11-03",
                zoneId = "Europe/Moscow"
            )
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            val slotsBeforeMidnight = repo.getOrCreateDailySlots(localDate = "2026-11-03")
            val slot1 = slotsBeforeMidnight.first().slot
            val proto = repo.slotToProtocolConfig(slot1)

            val startSnap = PhaseEngineSnapshot(
                status = RunStatus.RUNNING,
                protocol = proto,
                currentRepetition = 1,
                totalRepetitions = proto.repetitions,
                currentPhase = PhaseType.CONTRACTION,
                phaseRemainingMs = 2_000L,
                phaseDurationMs = proto.contractionMs,
                prepRemainingMs = 0L,
                confirmedActiveMs = 1_000L,
                totalActiveDurationMs = proto.totalActiveDurationMs,
                interruptionReason = null,
                isStopDialogVisible = false,
                lastSchedulerDriftMs = 0L,
                maxSchedulerDriftMs = 0L,
                snapshotElapsedRealtimeMs = clock.elapsedMs,
                runId = "run_midnight_1",
                slotId = slot1.id,
                startDate = "2026-11-03",
                zoneId = "Europe/Moscow",
                isDemo = true,
                soundPackSnapshot = SoundPack.CHIMES,
                metronomeStyleSnapshot = MetronomeTickStyle.SOFT
            )
            repo.saveRunCheckpoint(startSnap)

            clock.localDate = "2026-11-04"
            clock.zoneId = "Europe/Kaliningrad"
            clock.wallMs += 25_000L

            val finishSnap = startSnap.copy(
                status = RunStatus.COMPLETED,
                phaseRemainingMs = 0L,
                confirmedActiveMs = proto.totalActiveDurationMs
            )
            assertTrue(repo.completeRunAtomically(finishSnap))

            val slotsNextDay = repo.getOrCreateDailySlots(localDate = "2026-11-04")
            assertEquals(0, slotsNextDay.count { it.isCompleted })

            val slotsReturned = repo.getOrCreateDailySlots(localDate = "2026-11-03")
            assertEquals(1, slotsReturned.count { it.isCompleted })
            assertEquals(slot1.id, slotsReturned.first { it.isCompleted }.slot.id)
        } finally {
            db.close()
        }
    }

    @Test
    fun `room persistence recovers interrupted run after process death without adding idle time`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(
                elapsedMs = 20_000L,
                wallMs = 1_762_128_000_000L,
                localDate = "2026-11-03",
                bootSession = "boot_before_death"
            )
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)
            val slots = repo.getOrCreateDailySlots(localDate = "2026-11-03")
            val slot = slots.first().slot
            val proto = repo.slotToProtocolConfig(slot)

            val checkpointSnap = PhaseEngineSnapshot(
                status = RunStatus.RUNNING,
                protocol = proto,
                currentRepetition = 2,
                totalRepetitions = proto.repetitions,
                currentPhase = PhaseType.CONTRACTION,
                phaseRemainingMs = 1_500L,
                phaseDurationMs = proto.contractionMs,
                prepRemainingMs = 0L,
                confirmedActiveMs = 6_000L,
                totalActiveDurationMs = proto.totalActiveDurationMs,
                interruptionReason = null,
                isStopDialogVisible = false,
                lastSchedulerDriftMs = 0L,
                maxSchedulerDriftMs = 0L,
                snapshotElapsedRealtimeMs = clock.elapsedMs,
                runId = "run_process_death_1",
                slotId = slot.id,
                startDate = "2026-11-03",
                zoneId = "Europe/Moscow",
                isDemo = true,
                soundPackSnapshot = SoundPack.CRISP,
                metronomeStyleSnapshot = MetronomeTickStyle.CRISP
            )
            repo.saveRunCheckpoint(checkpointSnap)

            clock.wallMs += 15 * 60 * 1000L
            clock.bootSession = "boot_after_reboot"

            val recovered = repo.recoverInterruptedRunAfterProcessDeath()
            assertNotNull(recovered)
            assertEquals(RunStatus.INTERRUPTED.name, recovered!!.status)
            assertEquals(6_000L, recovered.confirmedActiveMs)
            assertEquals(1_500L, recovered.phaseRemainingMs)
            assertEquals("crisp", recovered.soundPackIdSnapshot)
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup export creates valid zip containing real completions, preferences, and novel without demo records`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val dao = db.velarisDao()
            val repo = WorkoutPersistenceRepository(dao, clock)
            val preferencesRepo = com.example.data.PreferencesRepository(context)
            preferencesRepo.setSoundPack(SoundPack.CRISP)
            preferencesRepo.setThemeId(com.example.ui.theme.VelarisThemeId.SKY)

            val storyRepo = StoryRepository(context, dao, clock)
            storyRepo.importNeutralTestSample()

            // 1 реальный завершённый слот + 1 демонстрационный завершённый слот
            val realSlots = repo.getOrCreateDailySlots(localDate = "2026-11-03", isDemo = false)
            val realSlot = realSlots.first().slot
            dao.insertCompletionIgnore(
                com.example.data.WorkoutCompletionEntity(
                    runId = "real_run_1",
                    slotId = realSlot.id,
                    localDate = "2026-11-03",
                    completedAtEpochMs = 1762129000000L,
                    optionalDifficulty = "EASY",
                    isDemo = false
                )
            )

            val demoSlots = repo.getOrCreateDailySlots(localDate = "2026-11-03", isDemo = true)
            val demoSlot = demoSlots.first().slot
            dao.insertCompletionIgnore(
                com.example.data.WorkoutCompletionEntity(
                    runId = "demo_run_1",
                    slotId = demoSlot.id,
                    localDate = "2026-11-03",
                    completedAtEpochMs = 1762128000000L,
                    optionalDifficulty = null,
                    isDemo = true
                )
            )

            val backupManager = com.example.data.BackupRestoreManager(
                context = context,
                dao = dao,
                preferencesRepository = preferencesRepo,
                storyRepository = storyRepo,
                timeProvider = clock
            )

            val bos = java.io.ByteArrayOutputStream()
            val exportResult = backupManager.createBackup(bos)
            assertTrue(exportResult.isSuccess)

            val manifest = exportResult.getOrThrow()
            assertEquals("Резервная копия должна содержать только реальные тренировки", 1, manifest.workoutCount)
            assertTrue(manifest.hasBook)
            assertEquals(12, manifest.chapterCount)
            assertEquals(3, manifest.actCount)

            // Проверяем предварительный осмотр архива перед подтверждением (v1.2 п. 2)
            val backupBytes = bos.toByteArray()
            val inspectResult = backupManager.inspectBackup(
                java.io.ByteArrayInputStream(backupBytes),
                backupBytes.size.toLong()
            )
            assertTrue(inspectResult.isSuccess)
            val preview = inspectResult.getOrThrow()
            assertEquals(1, preview.workoutCount)
            assertTrue(preview.hasBook)
            assertEquals(12, preview.chapterCount)

            // Проверяем атомарное восстановление из архива
            // Очищаем базу данных и проверяем, что restore восстановит реальные тренировки, роман и настройки
            dao.clearImportedNovelAtomically()
            dao.deleteRealWorkoutCompletions()
            preferencesRepo.setSoundPack(SoundPack.SOFT)

            val restoreResult = backupManager.restoreBackup(
                java.io.ByteArrayInputStream(backupBytes),
                isWorkoutActiveOrPaused = false
            )
            assertTrue(restoreResult.isSuccess)

            val restoredCompletions = dao.getRealWorkoutCompletions()
            assertEquals(1, restoredCompletions.size)
            assertEquals("real_run_1", restoredCompletions.first().runId)

            val restoredNovel = dao.getActiveNovel()
            assertNotNull(restoredNovel)
            assertEquals(12, dao.getNovelChapters(restoredNovel!!.bookId).size)

            val restoredBytes = storyRepo.readStoredOriginalBytes()
            assertNotNull(restoredBytes)
            assertEquals(manifest.files[com.example.data.BackupManifest.BOOK_SOURCE_FILE_NAME], MarkdownNovelParser.computeSha256Hex(restoredBytes!!))
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup restore with invalid checksum or active workout is rejected preserving existing data intact`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val dao = db.velarisDao()
            val preferencesRepo = com.example.data.PreferencesRepository(context)
            val storyRepo = StoryRepository(context, dao, clock)

            val backupManager = com.example.data.BackupRestoreManager(
                context = context,
                dao = dao,
                preferencesRepository = preferencesRepo,
                storyRepository = storyRepo,
                timeProvider = clock
            )

            // 1. Попытка восстановления во время активной тренировки должна быть отклонена
            val blockedResult = backupManager.restoreBackup(
                java.io.ByteArrayInputStream(byteArrayOf()),
                isWorkoutActiveOrPaused = true
            )
            assertTrue(blockedResult.isFailure)
            assertTrue(blockedResult.exceptionOrNull()?.message?.contains("Нельзя восстанавливать данные во время активной тренировки") == true)

            // 2. Попытка восстановления повреждённого файла
            val corruptedZip = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00)
            val corruptedResult = backupManager.restoreBackup(
                java.io.ByteArrayInputStream(corruptedZip),
                isWorkoutActiveOrPaused = false
            )
            assertTrue(corruptedResult.isFailure)
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup manifest serialization and parsing preserves format version, checksums, and counts`() {
        val manifest = com.example.data.BackupManifest(
            formatVersion = 1,
            appVersion = "0.5.0-v1.2",
            appVersionCode = 5,
            schemaVersion = 3,
            createdAtEpochMs = 1762128000000L,
            createdAtRu = "03.11.2026 12:00",
            workoutCount = 7,
            hasBook = true,
            bookTitle = "Роман Испытание",
            bookFileName = "test_novel.md",
            chapterCount = 12,
            actCount = 3,
            files = mapOf(
                "database_export.json" to "abc123hash",
                "preferences.json" to "def456hash",
                "book_source.md" to "789xyzhash"
            )
        )

        val json = manifest.toJson()
        val restored = com.example.data.BackupManifest.fromJson(json)

        assertEquals(1, restored.formatVersion)
        assertEquals("0.5.0-v1.2", restored.appVersion)
        assertEquals(5, restored.appVersionCode)
        assertEquals(3, restored.schemaVersion)
        assertEquals(7, restored.workoutCount)
        assertTrue(restored.hasBook)
        assertEquals("Роман Испытание", restored.bookTitle)
        assertEquals("test_novel.md", restored.bookFileName)
        assertEquals(12, restored.chapterCount)
        assertEquals(3, restored.actCount)
        assertEquals(3, restored.files.size)
        assertEquals("abc123hash", restored.files["database_export.json"])
        assertEquals("789xyzhash", restored.files["book_source.md"])
    }

    @Test
    fun `backup database and preferences snapshots serialize correctly and roundtrip without loss`() {
        val prefsSnapshot = com.example.data.BackupPreferencesSnapshot(
            soundEnabled = true,
            vibrationEnabled = false,
            metronomeEnabled = true,
            signalVolume = 0.95f,
            metronomeVolume = 0.25f,
            soundPackId = "CRISP",
            metronomeTickStyleId = "WOOD",
            themeId = "SKY",
            readerModeId = "BOOK",
            readerFontSizeId = "LARGE",
            readerLineSpacingId = "SPACIOUS",
            isRealProgramEnabled = true,
            realProgramSource = "Мой протокол"
        )
        val prefsJson = prefsSnapshot.toJson()
        val restoredPrefs = com.example.data.BackupPreferencesSnapshot.fromJson(prefsJson)

        assertEquals("CRISP", restoredPrefs.soundPackId)
        assertEquals("SKY", restoredPrefs.themeId)
        assertEquals("BOOK", restoredPrefs.readerModeId)
        assertFalse(restoredPrefs.vibrationEnabled)
        assertEquals(0.95f, restoredPrefs.signalVolume, 0.001f)
        assertEquals(0.25f, restoredPrefs.metronomeVolume, 0.001f)
        assertTrue(restoredPrefs.isRealProgramEnabled)
        assertEquals("Мой протокол", restoredPrefs.realProgramSource)

        val dbSnapshot = com.example.data.BackupDatabaseSnapshot(
            plans = listOf(
                com.example.data.TrainingPlanEntity(
                    id = "real_p1",
                    version = 1,
                    titleRu = "Реальный план",
                    source = "Пользователь",
                    effectiveFromDate = "2026-11-03",
                    isDemo = false
                )
            ),
            completions = listOf(
                com.example.data.WorkoutCompletionEntity(
                    runId = "run_101",
                    slotId = "slot_morning",
                    localDate = "2026-11-03",
                    completedAtEpochMs = 1762129000000L,
                    optionalDifficulty = "MEDIUM",
                    isDemo = false
                )
            )
        )
        val dbJson = dbSnapshot.toJson()
        val restoredDb = com.example.data.BackupDatabaseSnapshot.fromJson(dbJson)

        assertEquals(1, restoredDb.plans.size)
        assertEquals("real_p1", restoredDb.plans.first().id)
        assertFalse(restoredDb.plans.first().isDemo)
        assertEquals(1, restoredDb.completions.size)
        assertEquals("run_101", restoredDb.completions.first().runId)
        assertFalse(restoredDb.completions.first().isDemo)
    }
}
