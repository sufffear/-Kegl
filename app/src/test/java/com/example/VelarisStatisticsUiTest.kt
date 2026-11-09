package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.VelarisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w360dp-h800dp")
class VelarisStatisticsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun day(date: String, count: Int, frozen: Boolean = false) = WeeklyDaySummary(date,"Чт","29",isToday=true,completedCount=count,assignedCount=0,
        plannedCompletedCount=count,freeCompletedCount=0,completedTitlesRu=emptyList(),isFrozen=frozen)
    @Test fun canonicalMetricsLeapMonthCountsFreezeAndZeroDaysWorkAtLargeFont() {
        val today = day("2024-02-29",12)
        val overview = WeeklyOverviewState(days=listOf(today),calendarDaysByDate=mapOf(today.localDate to today,"2024-02-28" to day("2024-02-28",0,true)),
            selectedDate=today.localDate,currentStreakDays=2,totalHoldSecondsAllTime=1100)
        var selected = ""
        compose.setContent { VelarisTheme { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.5f)) {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) { VelarisStatisticsScreen(overview,{ selected=it },{},{}) }
        } } }
        compose.onNodeWithText("18 мин 20 с").assertExists()
        compose.onNodeWithTag("streak_motivation_banner").assertDoesNotExist()
        compose.onNodeWithTag("stability_heatmap_grid").assertDoesNotExist()
        compose.onNodeWithTag("calendar_count_2024-02-29",useUnmergedTree=true).assertTextEquals("12")
        compose.onNodeWithTag("calendar_count_2024-02-01",useUnmergedTree=true).assertTextEquals("0")
        compose.onNodeWithTag("calendar_day_2024-02-28").assertContentDescriptionEquals("2024-02-28. Завершено 0. Заморозка")
        compose.onNodeWithTag("calendar_day_2024-02-29").assertIsSelected().assertWidthIsAtLeast(48.dp)
        compose.onNodeWithTag("calendar_day_2024-02-01").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("2024-02-01",selected) }
        compose.onNodeWithTag("btn_calendar_next_month").assertIsNotEnabled()
        compose.onNodeWithTag("btn_calendar_prev_month").performScrollTo().performClick()
        compose.onNodeWithTag("calendar_day_2024-01-31").assertExists()
        compose.onNodeWithTag("btn_calendar_next_month").performScrollTo().performClick()
        compose.onNodeWithTag("calendar_day_2024-02-29").assertExists()
        compose.onNodeWithTag("calendar_day_2024-02-30").assertDoesNotExist()
    }
    @Test fun journalIncludesStoppedAndLegacyRecordsAndOpensTheSelectedEntry() {
        val legacy = WorkoutJournalEntry(null,WorkoutCompletionEntity("legacy","gone","2024-02-29",1,null,false),null)
        val stopped = WorkoutJournalEntry(journalRun("stopped"),null,null)
        var chosen = ""
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            VelarisWorkoutJournal(WorkoutJournalState(listOf(legacy,stopped)),null,{}, { chosen=it },{})
        } } }
        compose.onNodeWithTag("journal_entry_stopped").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("stopped",chosen) }
        compose.onNodeWithTag("journal_entry_legacy").assertExists()
        compose.onNodeWithTag("journal_empty").assertDoesNotExist()
    }
    @Test fun longJournalHasExplicitPagingWithoutDiscardingTheLastRecord() {
        val entries = List(51) { i -> WorkoutJournalEntry(null, WorkoutCompletionEntity("saved-$i","slot-$i","2026-11-09",i.toLong(),null,false),null) }
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            VelarisWorkoutJournal(WorkoutJournalState(entries),null,{}, {},{})
        } } }
        compose.onNodeWithTag("journal_entry_saved-50").assertDoesNotExist()
        compose.onNodeWithTag("journal_more").performScrollTo().performClick()
        compose.onNodeWithTag("journal_entry_saved-50").assertExists()
        compose.onNodeWithTag("journal_more").assertDoesNotExist()
        assertEquals(51,entries.size)
    }
    @Test fun failedJournalHasRetryInsteadOfFalseEmptyState() {
        var retry = 0
        compose.setContent { VelarisTheme { VelarisWorkoutJournal(WorkoutJournalState(error=true),null,{}, {},{retry++}) } }
        compose.onNodeWithTag("journal_empty").assertDoesNotExist()
        compose.onNodeWithTag("journal_retry").performClick()
        compose.runOnIdle { assertEquals(1,retry) }
    }
}
