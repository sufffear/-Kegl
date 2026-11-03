package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.VelarisDatabase
import com.example.data.WorkoutPersistenceRepository
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.PlanGroup
import com.example.domain.RunStatus
import com.example.domain.SoundPack
import com.example.domain.TimeProvider
import kotlinx.coroutines.runBlocking
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
    fun `verify stage 2 versionCode 3, applicationId and offline manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Velaris", context.getString(R.string.app_name))
        assertEquals("com.velaris.app", BuildConfig.APPLICATION_ID)
        assertEquals("0.3.0-s2", BuildConfig.VERSION_NAME)
        assertEquals(3, BuildConfig.VERSION_CODE)

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
    fun `verify Stage 2 screen displays demo safety warning, version 3, Room daily plan, and 3 sound packs`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("demo_safety_banner").assertExists()
        composeTestRule.onNodeWithText("Демо — не выполняйте упражнения по этим сигналам").assertExists()
        composeTestRule.onNodeWithTag("screen_title").assertExists()
        composeTestRule.onNodeWithText("Версия: 0.3.0-s2 (versionCode 3)").assertExists()
        composeTestRule.onNodeWithTag("daily_plan_persistence_card").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_soft").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_chimes").assertExists()
        composeTestRule.onNodeWithTag("sound_pack_crisp").assertExists()
        composeTestRule.onNodeWithTag("metronome_style_soft_tick").assertExists()
        composeTestRule.onNodeWithTag("metronome_style_crisp_tick").assertExists()
        composeTestRule.onNodeWithTag("btn_start_demo").assertExists()
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

            // 1. Стандартный демо-план на 3 подхода (N=3)
            val slotsDay1 = repo.getOrCreateDailySlots(
                localDate = "2026-11-03",
                planId = WorkoutPersistenceRepository.DEMO_PLAN_STANDARD_3,
                isDemo = true
            )
            assertEquals(3, slotsDay1.size)
            assertTrue(slotsDay1.none { it.isCompleted })

            // 2. План выходного дня без тренировок (N=0)
            val zeroSlots = repo.getOrCreateDailySlots(
                localDate = "2026-11-04",
                planId = WorkoutPersistenceRepository.DEMO_PLAN_ZERO_SLOTS,
                isDemo = true
            )
            assertEquals(0, zeroSlots.size)

            // 3. Атомарное завершение первого слота и защита от повторного зачёта (double tap / дубль сигнала)
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
            // Повторная попытка зачесть тот же слот (даже с новым runId) должна вернуть false
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

            // Попытка стартует в 23:59:50 2026-11-03
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

            // Наступает полночь (2026-11-04) и меняется часовой пояс, попытка завершается в 00:00:15
            clock.localDate = "2026-11-04"
            clock.zoneId = "Europe/Kaliningrad"
            clock.wallMs += 25_000L

            val finishSnap = startSnap.copy(
                status = RunStatus.COMPLETED,
                phaseRemainingMs = 0L,
                confirmedActiveMs = proto.totalActiveDurationMs
            )
            assertTrue(repo.completeRunAtomically(finishSnap))

            // Завершение обязано относиться к дате старта (2026-11-03), а на новой дате (2026-11-04) все слоты свободны
            val slotsNextDay = repo.getOrCreateDailySlots(localDate = "2026-11-04")
            assertEquals(0, slotsNextDay.count { it.isCompleted })

            // Возврат часов/даты к уже существующей дате 2026-11-03 использует прежний снимок дня и зачёт
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

            // Сохраняем контрольную точку посреди подхода (confirmedActiveMs = 6_000L)
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

            // Проходит 15 минут простоя и происходит перезапуск приложения (или перезагрузка устройства)
            clock.wallMs += 15 * 60 * 1000L
            clock.bootSession = "boot_after_reboot"

            val recovered = repo.recoverInterruptedRunAfterProcessDeath()
            assertNotNull(recovered)
            assertEquals(RunStatus.INTERRUPTED.name, recovered!!.status)
            // Время простоя НЕ начислено: confirmedActiveMs остался ровно 6_000L
            assertEquals(6_000L, recovered.confirmedActiveMs)
            assertEquals(1_500L, recovered.phaseRemainingMs)
            assertEquals("crisp", recovered.soundPackIdSnapshot)
        } finally {
            db.close()
        }
    }
}
