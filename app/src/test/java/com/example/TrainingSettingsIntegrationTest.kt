package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.*
import com.example.domain.*
import com.example.service.BenchSessionCoordinator as Coordinator
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w360dp-h800dp")
class TrainingSettingsIntegrationTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val init=object:ExternalResource() {
        override fun before() {WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext<Context>())}
    }
    @get:Rule val rules:RuleChain=RuleChain.outerRule(init).around(compose)
    @After fun cleanup() {Coordinator.resetToIdle()}
    private fun click(tag:String) {
        val node=compose.onNodeWithTag(tag)
        if(!tag.startsWith("nav_") && tag!="toolbar_back") node.performScrollTo()
        node.performClick();compose.waitForIdle()
    }
    private fun ready() {compose.waitUntil(10000){Coordinator.trainingStateReady.value && Coordinator.dailySlots.value.size==9}}
    @Test fun savedMorningSettingsSurviveRecreationAndPlayUsesThemWithoutChangingNoon() {
        ready();val energy=Coordinator.workoutEnergy.value
        click("nav_plan");click("daily_slot_real_v2_morning_fast");click("training_hold_plus")
        // Leave without saving: direct start elsewhere must still use the committed default.
        click("toolbar_back");click("daily_slot_real_v2_morning_fast")
        compose.onNodeWithTag("training_parameter_hold").assertTextEquals("1")
        click("training_hold_plus");click("training_save_parameters")
        compose.waitUntil(10000){Coordinator.trainingConfigurations.value.any{it.key=="mandatory:real_v2_morning_fast" && it.contractionSec==2}}
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        compose.onNodeWithTag("training_parameter_hold").assertTextEquals("2")
        click("toolbar_back");click("plan_group_tab_NOON")
        compose.onNodeWithTag("training_period_summary").assertTextEquals("3 упражнения · 450 с сжатия")
        click("plan_group_tab_MORNING")
        compose.onNodeWithTag("training_period_summary").assertTextEquals("3 упражнения · 550 с сжатия")
        click("btn_start_slot_real_v2_morning_fast")
        compose.onNodeWithTag("active_workout_screen").assertExists()
        compose.runOnIdle {assertTrue(Coordinator.startDemoSession())} // deliver recorded service start as in navigation regression
        assertEquals(2000L,Coordinator.engineSnapshot.value.protocol.contractionMs)
        assertEquals(300000L,Coordinator.engineSnapshot.value.totalActiveDurationMs)
        assertEquals(energy,Coordinator.workoutEnergy.value)
    }
    @Test fun completedRoundTriggersOverlayAndAutomaticallyUnlocksWithoutNavigationOrText(): Unit =runBlocking {
        ready();click("nav_plan")
        val dao=VelarisDatabase.getInstance(ApplicationProvider.getApplicationContext<Context>()).velarisDao()
        val repo=WorkoutPersistenceRepository(dao,SystemTimeProvider())
        for(id in listOf(103,101,102)) {
            val receipt="integration_$id"
            repo.completeRunAtomically(PhaseEngineSnapshot(runId=receipt,slotId="free_${id}_$receipt",startDate="2026-11-09",
                status=RunStatus.COMPLETED,isDemo=false,isFreeMode=true,confirmedActiveMs=4000,
                protocol=DemoProtocolConfig("free_slot_$id","Extra",1000,1000,2)))
            compose.waitUntil(3000){Coordinator.extraTrainingCycle.value.isLocked(id)}
            compose.onNodeWithTag("btn_start_slot_extra_$id").assertIsNotEnabled()
        }
        compose.onNodeWithTag("extra_cycle_fireworks").assertExists()
        compose.waitUntil(6000){Coordinator.extraTrainingCycle.value.mask==0}
        compose.waitForIdle()
        compose.onNodeWithTag("extra_cycle_fireworks").assertDoesNotExist()
        for(id in 101..103) compose.onNodeWithTag("btn_start_slot_extra_$id").assertIsEnabled()
        compose.onNodeWithTag("training_hero").assertExists()
    }
    @Test fun constructorSaveReturnsToUnifiedListAndProgramSurvivesRecreationAndStarts() {
        ready();click("nav_plan");click("btn_other_workouts");click("btn_create_custom_program")
        compose.onNodeWithText("Название программы").performTextReplacement("Мой новый ритм")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10000){Coordinator.customPrograms.value.any{it.title=="Мой новый ритм"}}
        compose.waitForIdle()
        compose.onNodeWithTag("training_hero").assertExists()
        val program=Coordinator.customPrograms.value.first{it.title=="Мой новый ритм"}
        click("daily_slot_program_${program.id}")
        compose.onNodeWithTag("training_edit_program").assertExists()
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        compose.onNodeWithText("Мой новый ритм").assertExists()
        click("toolbar_back");click("btn_start_slot_program_${program.id}")
        compose.onNodeWithTag("active_workout_screen").assertExists()
        assertTrue(Coordinator.engineSnapshot.value.isCustomProgram)
        assertEquals(program.totalDurationMs,Coordinator.engineSnapshot.value.totalActiveDurationMs)
        assertEquals(0,Coordinator.extraTrainingCycle.value.mask)
    }
}
