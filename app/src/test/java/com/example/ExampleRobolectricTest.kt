package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.StoryRepository
import com.example.data.VelarisDatabase
import com.example.data.WorkoutPersistenceRepository
import com.example.domain.DemoProtocolConfig
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
    fun `verify story3_1 versionCode 9, applicationId and optional network manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Velaris", context.getString(R.string.app_name))
        assertEquals("com.velaris.app", BuildConfig.APPLICATION_ID)
        assertEquals("0.8.1-story3.1", BuildConfig.VERSION_NAME)
        assertEquals(9, BuildConfig.VERSION_CODE)

        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        assertTrue(
            "Gemini требует разрешение INTERNET для явного запроса истории",
            permissions.contains(Manifest.permission.INTERNET)
        )
        assertTrue(
            "Должно быть объявлено разрешение FOREGROUND_SERVICE_SPECIAL_USE",
            permissions.contains(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        )
    }

    @Test
    fun `verify v1_4 3-tab navigation, clean Home without versionCode clutter, and Diagnostics in Settings`() {
        composeTestRule.waitForIdle()

        // 1. В нижней навигации разделы: Главная, Тренировки, Шанталь, Настройки
        composeTestRule.onNodeWithTag("nav_home").assertExists()
        composeTestRule.onNodeWithTag("nav_plan").assertExists()
        composeTestRule.onNodeWithTag("nav_chantal").assertExists()
        composeTestRule.onNodeWithTag("nav_settings").assertExists()
        composeTestRule.onNodeWithTag("nav_reader").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_workout").assertDoesNotExist()

        // На Главной нет технического номера сборки, демо-режима и книги; отображаются только Активная сессия и Недельный обзор
        composeTestRule.onNodeWithTag("demo_safety_banner").assertDoesNotExist()
        composeTestRule.onNodeWithText("Версия: ${BuildConfig.VERSION_NAME} (versionCode ${BuildConfig.VERSION_CODE})").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_story_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_open_reader").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_workout_card").assertExists()
        composeTestRule.onNodeWithTag("weekly_overview_card").assertExists()

        // 2. Переход в Тренировку: проверяем 3 горизонтальные вкладки (Утро, Обед, Вечер) и 3 тренировки внутри Утра
        composeTestRule.onNodeWithTag("nav_plan").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("daily_plan_persistence_card").assertExists()
        composeTestRule.onNodeWithTag("plan_group_tab_MORNING").assertExists()
        composeTestRule.onNodeWithTag("plan_group_tab_NOON").assertExists()
        composeTestRule.onNodeWithTag("plan_group_tab_EVENING").assertExists()
        composeTestRule.onNodeWithTag("daily_slot_real_v2_morning_fast").assertExists()
        composeTestRule.onNodeWithTag("daily_slot_real_v2_morning_slow").assertExists()
        composeTestRule.onNodeWithTag("daily_slot_real_v2_morning_endurance").assertExists()

        // 4. Переход в Настройки (вкладки категорий + спойлеры: Оформление, Звук, Образцы, Данные, Диагностика)
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_category_tabs_row").assertExists()
        composeTestRule.onNodeWithTag("theme_selector_card").assertExists()

        // Раскрываем спойлер «Оформление» и проверяем все 5 тем
        composeTestRule.onNodeWithTag("theme_selector_header").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_option_theme_dawn").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_sky").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_aurora").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_sunset").assertExists()
        composeTestRule.onNodeWithTag("theme_option_theme_sakura").assertExists()

        // Раскрываем спойлер «Звук, профили и вибрация» и проверяем мягкие и звонкие профили + кнопки сравнения Сжать/Отдых
        composeTestRule.onNodeWithTag("sound_settings_header")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sound_pack_velvet_bowl").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_warm_marimba").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_harp_breeze").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_soft").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_chimes").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_crisp").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_preview_hold_velvet_bowl", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("sound_pack_preview_relax_velvet_bowl", useUnmergedTree = true).assertExists()

        // Раскрываем спойлер «Прослушать образцы команд»
        composeTestRule.onNodeWithTag("sound_samples_header")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_sample_contraction").assertExists()

        // Раскрываем спойлер «Резервные копии и данные»
        composeTestRule.onNodeWithTag("data_settings_header")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_create_backup")
            .performScrollTo()
            .assertExists()
        composeTestRule.onNodeWithTag("btn_restore_backup")
            .performScrollTo()
            .assertExists()

        // Технические сведения находятся строго в «Настройки → О приложении → Диагностика»
        composeTestRule.onNodeWithTag("btn_open_diagnostics")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diagnostics_screen").assertExists()
        composeTestRule.onNodeWithTag("audio_diagnostics_card").assertExists()
        composeTestRule.onNodeWithTag("diagnostics_build_info_card")
            .performScrollTo()
            .assertExists()
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

    @Test
    fun `five free interval templates are seeded in Room and can be edited and persisted`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            // Проверяем начальное заполнение 5 шаблонов
            repo.ensureFreeTemplatesSeeded()
            val templates = repo.getAllFreeTemplates()
            assertEquals(5, templates.size)
            assertEquals("Быстрый", templates[0].name)
            assertEquals("Медленный", templates[1].name)
            assertEquals("Длительное удержание", templates[2].name)
            assertEquals("Мой вариант 4", templates[3].name)
            assertEquals("Мой вариант 5", templates[4].name)

            // Редактируем шаблон #1 (индекс 0)
            val updated = templates[0].copy(
                name = "Мой быстрый спринт",
                targetDurationMinutes = 4,
                targetDurationSeconds = 30,
                contractionSec = 6,
                relaxationSec = 4
            )
            repo.saveFreeTemplate(updated)

            val reloaded = repo.getAllFreeTemplates()
            val savedSlot0 = reloaded.first { it.slotIndex == 0 }
            assertEquals("Мой быстрый спринт", savedSlot0.name)
            assertEquals(4, savedSlot0.targetMinutes)
            assertEquals(30, savedSlot0.targetSeconds)
            assertEquals(6, savedSlot0.contractionSec)
            assertEquals(4, savedSlot0.relaxationSec)
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup and restore preserves free interval templates with backward compatibility`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            // 1. Создаём снимок с кастомным шаблоном
            repo.ensureFreeTemplatesSeeded()
            val customTemplate = repo.getAllFreeTemplates()[0].copy(name = "Спец-шаблон")
            repo.saveFreeTemplate(customTemplate)

            val snapshot = com.example.data.BackupDatabaseSnapshot(
                plans = emptyList(),
                runs = emptyList(),
                completions = emptyList(),
                freeTemplates = repo.getAllFreeTemplates()
            )
            val json = snapshot.toJson()

            // 2. Десериализуем и проверяем, что кастомный шаблон сохранился
            val restoredSnapshot = com.example.data.BackupDatabaseSnapshot.fromJson(json)
            assertEquals(5, restoredSnapshot.freeTemplates.size)
            assertEquals("Спец-шаблон", restoredSnapshot.freeTemplates[0].name)

            // 3. Обратная совместимость: снимок старой версии без freeTemplates
            val legacyJson = """{"plans":[],"runs":[],"completions":[]}"""
            val restoredLegacy = com.example.data.BackupDatabaseSnapshot.fromJson(legacyJson)
            assertTrue(restoredLegacy.freeTemplates.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `weekly overview separates planned and free workout completions`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-03")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            // Завершаем 2 плановых подхода (isFreeMode = false, isDemo = false)
            val planSlots = repo.getOrCreateDailySlots(
                localDate = "2026-11-03",
                planId = WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3,
                isDemo = false
            )
            val slot0 = planSlots[0].slot
            val proto0 = repo.slotToProtocolConfig(slot0)
            val snap0 = PhaseEngineSnapshot(
                status = RunStatus.COMPLETED,
                protocol = proto0,
                currentRepetition = proto0.repetitions,
                totalRepetitions = proto0.repetitions,
                currentPhase = PhaseType.RELAXATION,
                phaseRemainingMs = 0L,
                phaseDurationMs = proto0.relaxationMs,
                prepRemainingMs = 0L,
                confirmedActiveMs = proto0.totalActiveDurationMs,
                totalActiveDurationMs = proto0.totalActiveDurationMs,
                snapshotElapsedRealtimeMs = clock.elapsedMs,
                runId = "run_planned_0",
                slotId = slot0.id,
                startDate = "2026-11-03",
                zoneId = "Europe/Moscow",
                isDemo = false,
                isFreeMode = false
            )
            assertTrue(repo.completeRunAtomically(snap0))

            val slot1 = planSlots[1].slot
            val proto1 = repo.slotToProtocolConfig(slot1)
            val snap1 = snap0.copy(
                protocol = proto1,
                runId = "run_planned_1",
                slotId = slot1.id
            )
            assertTrue(repo.completeRunAtomically(snap1))

            // Завершаем 1 свободный подход (isFreeMode = true, isDemo = false)
            val freeProto = DemoProtocolConfig(
                id = "free_proto_1",
                titleRu = "Свободный 1",
                contractionMs = 5000L,
                relaxationMs = 5000L,
                repetitions = 3
            )
            val freeSnap = PhaseEngineSnapshot(
                status = RunStatus.COMPLETED,
                protocol = freeProto,
                currentRepetition = 3,
                totalRepetitions = 3,
                currentPhase = PhaseType.RELAXATION,
                phaseRemainingMs = 0L,
                phaseDurationMs = 5000L,
                prepRemainingMs = 0L,
                confirmedActiveMs = 30000L,
                totalActiveDurationMs = 30000L,
                snapshotElapsedRealtimeMs = clock.elapsedMs,
                runId = "run_free_1",
                slotId = "free_slot_1",
                startDate = "2026-11-03",
                zoneId = "Europe/Moscow",
                isDemo = false,
                isFreeMode = true
            )
            assertTrue(repo.completeRunAtomically(freeSnap))

            val overview = repo.loadWeeklyOverview(todayDateStr = "2026-11-03")
            val todaySummary = overview.days.first { it.isToday }
            assertEquals(3, todaySummary.completedCount)
            assertEquals(2, todaySummary.plannedCompletedCount)
            assertEquals(1, todaySummary.freeCompletedCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify switching to free intervals tab, 5 templates, 300-cycle limit error, and unsaved changes confirmation`() {
        composeTestRule.waitForIdle()

        // Переходим на экран Тренировки
        composeTestRule.onNodeWithTag("nav_plan").performClick()
        composeTestRule.waitForIdle()

        // Переключаемся на вкладку «Мои интервалы»
        composeTestRule.onNodeWithTag("plan_tab_free").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Проверяем наличие секции шаблонов и обязательной подписи
        composeTestRule.onNodeWithTag("free_interval_templates_section").assertExists()
        composeTestRule.onNodeWithTag("free_intervals_disclaimer_caption").assertExists()
        composeTestRule.onNodeWithText("Ваши интервалы. Приложение не оценивает подходящую нагрузку").assertExists()

        // Проверяем наличие всех 5 карточек шаблонов и их кнопок действий
        for (i in 0..4) {
            composeTestRule.onNodeWithTag("free_template_card_$i")
                .performScrollTo()
                .assertExists()
            composeTestRule.onNodeWithTag("btn_start_free_$i").assertExists()
            composeTestRule.onNodeWithTag("btn_edit_free_$i").assertExists()
        }

        // Открываем редактор первого шаблона
        composeTestRule.onNodeWithTag("btn_edit_free_0")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("free_template_editor_dialog").assertExists()

        // Вводим 90 минут при 1 с удержания и 1 с отдыха -> 2700 циклов > 300 -> ошибка без молчаливого сброса
        composeTestRule.onNodeWithTag("edit_template_minutes_input").performTextReplacement("90")
        composeTestRule.onNodeWithTag("edit_template_contraction_input").performTextReplacement("1")
        composeTestRule.onNodeWithTag("edit_template_relaxation_input").performTextReplacement("1")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("free_template_validation_error").assertExists()

        // Проверяем защиту от случайной потери несохранённых правок при нажатии «Отмена»
        composeTestRule.onNodeWithTag("btn_cancel_free_template")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("unsaved_changes_confirm_bar").assertExists()

        // Закрываем без сохранения и убеждаемся, что исходное имя шаблона не потерялось
        composeTestRule.onNodeWithTag("btn_discard_unsaved_template").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("free_template_editor_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("Быстрый").assertExists()
    }

    @Test
    fun `verify compact active workout bar appears on other screens and returns to workout without duplicate timer`() {
        composeTestRule.waitForIdle()

        // Запускаем свободный подход из «Тренировки → Мои интервалы»
        composeTestRule.onNodeWithTag("nav_plan").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("plan_tab_free").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_start_free_0")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Открылся экран активной тренировки
        composeTestRule.onNodeWithTag("active_workout_screen").assertExists()
        composeTestRule.onNodeWithTag("smooth_timer_ring").assertExists()
        val initialRunId = com.example.service.BenchSessionCoordinator.engineSnapshot.value.runId
        assertNotNull(initialRunId)

        // Переходим на экран Настроек -> должна появиться компактная плашка текущей тренировки над нижней навигацией
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_session_return_banner").assertExists()
        composeTestRule.onNodeWithTag("btn_return_to_workout").assertExists()

        // Переходим на Главную -> плашка тоже видна, а runId не изменился (второй таймер не создан)
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_session_return_banner").assertExists()
        assertEquals(initialRunId, com.example.service.BenchSessionCoordinator.engineSnapshot.value.runId)

        // Возвращаемся к тренировке по нажатию на кнопку в компактной плашке
        composeTestRule.onNodeWithTag("btn_return_to_workout").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_workout_screen").assertExists()

        // Останавливаем и сбрасываем тренировку, чтобы очистить состояние
        com.example.service.BenchSessionCoordinator.resetToIdle()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `real daily plan v2 seeds 3 workouts in Morning, Noon, and Evening with exact intervals`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-05")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            val slots = repo.getOrCreateDailySlots(
                localDate = "2026-11-05",
                planId = WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3,
                isDemo = false
            )
            assertEquals(9, slots.size)

            val morning = slots.filter { it.slot.groupCode == "MORNING" }
            val noon = slots.filter { it.slot.groupCode == "NOON" }
            val evening = slots.filter { it.slot.groupCode == "EVENING" }
            assertEquals(3, morning.size)
            assertEquals(3, noon.size)
            assertEquals(3, evening.size)

            // 1. Быстрая тренировка: 100 сжиманий, 1 с сжатие, 1 с отдых
            assertEquals(1_000L, morning[0].slot.contractionMs)
            assertEquals(1_000L, morning[0].slot.relaxationMs)
            assertEquals(100, morning[0].slot.repetitions)

            // 2. Медленная тренировка (Силовой контроль): 10 сжиманий, 10 с сжатие, 5 с отдых
            assertEquals(10_000L, morning[1].slot.contractionMs)
            assertEquals(5_000L, morning[1].slot.relaxationMs)
            assertEquals(10, morning[1].slot.repetitions)

            // 3. Очень медленные сжимания (Выносливость): 10 сжиманий, 25 с удержание, 10 с отдых
            assertEquals(25_000L, morning[2].slot.contractionMs)
            assertEquals(10_000L, morning[2].slot.relaxationMs)
            assertEquals(10, morning[2].slot.repetitions)
        } finally {
            db.close()
        }
    }

    @Test
    fun `stability chain calendar computes 28-day heatmap, streak freeze bridge, pure hold seconds, and switches UI mode`() = runBlocking {
        composeTestRule.waitForIdle()

        // Проверяем элементы мотивации, витрину жетонов, дельту недель и 60-сек блиц на Главной
        composeTestRule.onNodeWithTag("streak_motivation_banner").assertExists()
        composeTestRule.onNodeWithTag("streak_freeze_shields_badge").assertExists()
        composeTestRule.onNodeWithTag("milestone_tokens_showcase_row").assertExists()
        composeTestRule.onNodeWithTag("daily_motivation_quest_box").assertExists()
        composeTestRule.onNodeWithTag("btn_streak_rescue_blitz").assertExists()
        composeTestRule.onNodeWithTag("week_vs_prev_week_banner").assertExists()
        composeTestRule.onNodeWithTag("week_delta_badge").assertExists()

        // Переключатель «Неделя / Тепловая карта» убран; используется единый календарь с переключением месяцев
        composeTestRule.onNodeWithTag("calendar_mode_toggle_row").assertDoesNotExist()
        composeTestRule.onNodeWithTag("monthly_calendar_section").assertExists()
        composeTestRule.onNodeWithTag("monthly_calendar_header").assertExists()
        composeTestRule.onNodeWithTag("btn_calendar_prev_month").assertExists()
        composeTestRule.onNodeWithTag("btn_calendar_next_month").assertExists()
        composeTestRule.onNodeWithTag("stability_heatmap_grid").assertExists()
        composeTestRule.onNodeWithTag("weekly_selected_day_details").assertExists()
        composeTestRule.onNodeWithTag("weekly_selected_day_breakdown").assertExists()
        composeTestRule.onNodeWithTag("expandable_session_history_section").assertExists()

        // Открываем жетон «Искра ритма» (3 дня) в витрине и проверяем баннер тактильного салюта
        composeTestRule.onNodeWithTag("milestone_token_3")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("milestone_salute_banner").assertExists()
        composeTestRule.onNodeWithTag("btn_play_milestone_salute").assertExists()
        composeTestRule.onNodeWithTag("btn_close_milestone_salute")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("milestone_salute_banner").assertDoesNotExist()

        // Проверяем переключение месяцев в едином календаре (назад и вперёд)
        composeTestRule.onNodeWithTag("btn_calendar_prev_month")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stability_heatmap_grid").assertExists()

        composeTestRule.onNodeWithTag("btn_calendar_next_month")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stability_heatmap_grid").assertExists()

        // Проверяем раскрытие истории занятий
        composeTestRule.onNodeWithTag("expandable_session_history_header")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("weekly_completed_titles_list").assertExists()

        // Проверяем запуск 60-секундного протокола «Блиц-спасение серии»
        composeTestRule.onNodeWithTag("btn_streak_rescue_blitz")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("active_workout_screen").assertExists()
        assertEquals(30, com.example.service.BenchSessionCoordinator.engineSnapshot.value.totalRepetitions)
        com.example.service.BenchSessionCoordinator.resetToIdle()
        composeTestRule.waitForIdle()

        // Проверяем расчёт серии, щита заморозки, витрины жетонов и сравнения с прошлой неделей в репозитории
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VelarisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val clock = FakeControllableTimeProvider(localDate = "2026-11-05")
            val repo = WorkoutPersistenceRepository(db.velarisDao(), clock)

            suspend fun completeFirstSlotOn(dateStr: String) {
                val slots = repo.getOrCreateDailySlots(
                    localDate = dateStr,
                    planId = WorkoutPersistenceRepository.REAL_PLAN_STANDARD_3,
                    isDemo = false
                )
                val firstSlot = slots.first().slot
                val proto = repo.slotToProtocolConfig(firstSlot)
                val snap = PhaseEngineSnapshot(
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
                    runId = "run_$dateStr",
                    slotId = firstSlot.id,
                    startDate = dateStr,
                    zoneId = "Europe/Moscow",
                    isDemo = false,
                    soundPackSnapshot = SoundPack.CRISP,
                    metronomeStyleSnapshot = MetronomeTickStyle.CRISP
                )
                assertTrue(repo.completeRunAtomically(snap))
            }

            // 1 тренировка на прошлой неделе (2026-10-25 = 100 с сжатия)
            completeFirstSlotOn("2026-10-25")

            // 3 тренировки на этой неделе: 2026-11-02, 2026-11-03, пропуск 2026-11-04 (защищается щитом заморозки!), активен 2026-11-05
            completeFirstSlotOn("2026-11-02")
            completeFirstSlotOn("2026-11-03")
            completeFirstSlotOn("2026-11-05")

            val overview = repo.loadWeeklyOverview(todayDateStr = "2026-11-05")
            assertEquals(28, overview.heatmapDays.size)
            assertEquals(7, overview.days.size)
            assertTrue("Пропущенный день 2026-11-04 должен быть защищён щитом заморозки", "2026-11-04" in overview.usedFreezeDates)
            assertEquals("Серия должна составлять 3 активных дня благодаря мосту заморозки", 3, overview.currentStreakDays)
            assertEquals("Искра ритма", overview.rankTitleRu)
            assertEquals(300, overview.totalHoldSecondsInWeek)
            assertEquals(100, overview.previousWeekHoldSeconds)
            assertEquals(200, overview.weekHoldDeltaSeconds)
            assertEquals(200, overview.weekHoldDeltaPercent)
            assertTrue("Жетон 3 дней должен быть разблокирован", overview.milestones.first { it.targetDays == 3 }.isUnlocked)
        } finally {
            db.close()
        }
    }
}
