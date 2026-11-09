package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.data.*
import com.example.domain.*
import com.example.ui.VelarisHomeScreen
import com.example.ui.VelarisActiveWorkoutScreen
import com.example.ui.theme.VelarisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], qualifiers="w320dp-h640dp")
class VelarisTrainingPresentationTest {
    @get:Rule val compose = createComposeRule()
    private fun slot(done: Boolean = false) = DailySlotWithCompletion(
        DailySlotEntity("slot","day","2026-11-09","assignment","MORNING",0,"Быстрая тренировка",1000,1000,100,false,false),
        if(done) WorkoutCompletionEntity("run","slot","2026-11-09",1,null,false) else null)

    @Test fun homeStatesDoNotInventRewardsOrMixStatisticsIntoTheFirstScreen() {
        var slots by mutableStateOf(listOf(slot()))
        var snapshot by mutableStateOf(PhaseEngineSnapshot())
        var error by mutableStateOf<String?>(null)
        var starts = 0; var plans = 0; var returns = 0; var pauses = 0; var resumes = 0; var retries = 0
        compose.setContent { VelarisTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.5f)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VelarisHomeScreen(snapshot,slots,WeeklyOverviewState(currentStreakDays=2),
                        { returns++ }, { assertEquals("slot",it.slot.id); starts++ }, { plans++ },
                        { pauses++ }, { resumes++ },error,{ retries++ })
                }
            }
        } }
        compose.onNodeWithTag("monthly_calendar_section").assertDoesNotExist()
        compose.onNodeWithTag("weekly_overview_card").assertDoesNotExist()
        compose.onNodeWithTag("home_streak_summary").assertExists()
        compose.onNodeWithTag("btn_home_primary_workout").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,starts); slots=listOf(slot(true)) }
        compose.onNodeWithTag("home_day_complete").assertExists()
        compose.onNodeWithTag("btn_home_primary_workout").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,starts); assertEquals(1,plans); slots=emptyList() }
        compose.onNodeWithTag("zero_slots_calm_message").assertExists()
        compose.onAllNodes(hasText("0/0",substring=true)).assertCountEquals(0)
        compose.runOnIdle { error="read failed" }
        compose.onNodeWithTag("home_data_error").assertExists()
        compose.onNodeWithTag("btn_home_primary_workout").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,retries); error=null; snapshot=snapshot.copy(status=RunStatus.RUNNING,runId="same-run") }
        compose.onNodeWithTag("btn_home_primary_workout").performScrollTo().performClick()
        compose.onNodeWithTag("btn_home_pause_resume").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,returns); assertEquals(1,pauses); snapshot=snapshot.copy(status=RunStatus.PAUSED) }
        compose.onNodeWithTag("btn_home_pause_resume").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,resumes); assertEquals(1,starts) }
    }

    @Test fun completionShowsOnePrimaryReturnAndKeepsOptionalDifficultyAccessible() {
        var returned = 0
        var difficulty: WorkoutDifficulty? = null
        compose.setContent { VelarisTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                VelarisActiveWorkoutScreen(PhaseEngineSnapshot(status=RunStatus.COMPLETED,runId="done"),
                    AudioHardwareStatus(),true,null,{}, {}, {}, {}, {}, { difficulty=it },{ returned++ },{})
            }
        } }
        compose.onNodeWithTag("workout_completion_card").assertExists()
        compose.onNodeWithTag("timer_state_card").assertDoesNotExist()
        compose.onNodeWithTag("btn_start_demo").assertDoesNotExist()
        val first = WorkoutDifficulty.entries.first()
        val tag = "difficulty_btn_${first.code.lowercase(Locale.ROOT)}"
        compose.onNodeWithTag(tag).assertDoesNotExist()
        compose.onNodeWithTag("btn_rate_workout").performScrollTo().performClick()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(first,difficulty) }
        compose.onNodeWithTag("btn_completion_to_plan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,returned) }
    }
}
