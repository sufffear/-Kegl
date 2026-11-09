package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.service.BenchSessionCoordinator as Coordinator
import com.example.domain.RunStatus
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], qualifiers="w320dp-h640dp")
class VelarisTrainingNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun dispatchServiceCommand(action: String) {
        compose.runOnIdle {
            val intent = org.robolectric.Shadows.shadowOf(compose.activity.application).nextStartedService
            assertNotNull(intent)
            assertEquals(action, intent.action)
            assertEquals(com.example.service.BenchTimerService::class.java.name, intent.component?.className)
            // Robolectric records service intents; Android delivers these branches automatically.
            when (action) {
                com.example.service.BenchTimerService.ACTION_START_DEMO -> assertTrue(Coordinator.startDemoSession())
                com.example.service.BenchTimerService.ACTION_PAUSE_MANUAL -> Coordinator.pauseSession()
                com.example.service.BenchTimerService.ACTION_REQUEST_STOP -> Coordinator.requestStopWithConfirmation()
                com.example.service.BenchTimerService.ACTION_CONFIRM_STOP -> Coordinator.confirmStopWithoutCredit()
                else -> error("Unexpected service command")
            }
        }
        compose.waitForIdle()
    }
    @After fun cleanup() { Coordinator.resetToIdle() }
    @Test fun referenceTrainingKeepsPlanPresetsIntervalsAndConstructorReachable() {
        compose.onNodeWithTag("nav_plan").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("training_hero").assertExists()
        compose.waitUntil(10000) { Coordinator.dailySlots.value.isNotEmpty() }
        compose.onNodeWithTag("daily_slot_real_v2_morning_fast").assertExists()
        compose.waitForIdle()
        compose.onNodeWithTag("daily_slot_real_v2_morning_fast").assertExists()
        compose.onNodeWithTag("btn_start_slot_real_v2_morning_fast").assertIsEnabled()
        compose.onNodeWithTag("daily_slot_real_v2_morning_fast").performScrollTo().performClick()
        compose.onNodeWithTag("btn_details_start").assertExists()
        compose.onNodeWithTag("toolbar_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("btn_other_workouts").performScrollTo().performClick()
        compose.waitForIdle()
        for (tag in listOf("plan_tab_extras", "plan_tab_free", "plan_tab_constructor")) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithTag("plan_tab_extras").performScrollTo().performClick()
        compose.onNodeWithTag("extra_workout_103").assertExists()
        compose.onNodeWithTag("plan_tab_free").performScrollTo().performClick()
        compose.onNodeWithTag("free_interval_templates_section").assertExists()
        compose.onNodeWithTag("plan_tab_constructor").performScrollTo().performClick()
        compose.onNodeWithTag("btn_create_custom_program").assertExists()
        compose.onNodeWithTag("toolbar_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("btn_other_workouts").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("btn_create_custom_program").assertExists()
    }
    @Test fun homeStartPauseMinimizeAndStopKeepOneRunAndConfirmation() {
        compose.waitUntil(10000) { Coordinator.dailySlots.value.isNotEmpty() }
        compose.onNodeWithTag("btn_home_primary_workout").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("active_workout_screen").assertExists()
        dispatchServiceCommand(com.example.service.BenchTimerService.ACTION_START_DEMO)
        val run = Coordinator.engineSnapshot.value.runId
        assertNotNull(run)
        compose.onNodeWithTag("btn_pause_demo").performScrollTo().performClick()
        dispatchServiceCommand(com.example.service.BenchTimerService.ACTION_PAUSE_MANUAL)
        compose.onNodeWithTag("btn_minimize_workout").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("training_hero").assertExists()
        assertEquals(run,Coordinator.engineSnapshot.value.runId)
        assertEquals(RunStatus.PAUSED,Coordinator.engineSnapshot.value.status)
        compose.onNodeWithTag("btn_return_to_workout").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("btn_stop_demo").performScrollTo().performClick()
        dispatchServiceCommand(com.example.service.BenchTimerService.ACTION_REQUEST_STOP)
        compose.onNodeWithTag("stop_confirm_dialog").assertExists()
        assertEquals(run,Coordinator.engineSnapshot.value.runId)
        compose.onNodeWithTag("btn_confirm_stop").performClick()
        dispatchServiceCommand(com.example.service.BenchTimerService.ACTION_CONFIRM_STOP)
        compose.waitForIdle()
        assertEquals(RunStatus.STOPPED,Coordinator.engineSnapshot.value.status)
    }
}
