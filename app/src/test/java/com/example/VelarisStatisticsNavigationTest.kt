package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.*
import com.example.service.BenchSessionCoordinator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w360dp-h800dp")
class VelarisStatisticsNavigationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val compose = createAndroidComposeRule<MainActivity>()
    private val seed = object : ExternalResource() {
        override fun before() = runBlocking {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            val dao = VelarisDatabase.getInstance(context).velarisDao()
            // This test owns its fixture; never inherit sessions from a different UI test.
            dao.clearFinalStartCompletions(); dao.clearFinalStartRuns()
            dao.clearFinalStartSlots(); dao.clearFinalStartDays()
            repeat(8) { i ->
                val date=if(i<5) "2026-11-07" else "2026-11-08"
                dao.insertDailySlots(listOf(DailySlotEntity("slot-$i","day-$date",date,"assignment-$i","MORNING",i,"Занятие $i",
                    if(i<5) 1000L else 2000L,1000,100,false,false)))
                dao.insertCompletionIgnore(WorkoutCompletionEntity("saved-$i","slot-$i",date,i.toLong(),null,false))
            }
            dao.insertWorkoutRuns(listOf(journalRun("stopped")))
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(compose)
    @Test fun baselineCountsCalendarDayJournalAndDetailsAgreeWithoutMutatingTraining() {
        compose.waitForIdle()
        BenchSessionCoordinator.refreshDailySlotsAsync()
        compose.waitUntil(10000) {
            compose.waitForIdle() // drain Main-dispatcher publications from Room initialization
            BenchSessionCoordinator.weeklyOverview.value.totalRealCompletionsAllTime==8
        }
        compose.onNodeWithTag("nav_statistics").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("18 мин 20 с").assertExists()
        compose.onNodeWithTag("calendar_count_2026-11-07",useUnmergedTree=true).assertTextEquals("5")
        compose.onNodeWithTag("calendar_count_2026-11-08",useUnmergedTree=true).assertTextEquals("3")
        compose.onNodeWithTag("calendar_day_2026-11-07").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("statistics_day_screen").assertExists()
        compose.onNodeWithText("Завершено: 5").assertExists()
        compose.onNodeWithTag("btn_day_journal").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("journal_entry_saved-0").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("journal_entry_saved-7").assertDoesNotExist()
        compose.onNodeWithTag("journal_all_dates").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("journal_entry_stopped").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("session_status").assertTextEquals("Остановлено")
        compose.onNodeWithTag("toolbar_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("journal_entry_saved-0").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("session_status").assertTextEquals("Завершено")
        compose.runOnIdle {
            assertEquals(8,BenchSessionCoordinator.weeklyOverview.value.totalRealCompletionsAllTime)
            assertEquals(1100,BenchSessionCoordinator.weeklyOverview.value.totalHoldSecondsAllTime)
            assertEquals(80,BenchSessionCoordinator.workoutEnergy.value)
        }
    }
}
