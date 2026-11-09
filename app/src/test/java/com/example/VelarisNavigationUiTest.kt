package com.example
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], qualifiers="w360dp-h800dp")
class VelarisNavigationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun back() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
    private fun scrollOffset(tag: String): Float = compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
    @Test fun fourSectionsSettingsBackAndScrollPositionArePreserved() {
        compose.onAllNodes(hasTestTag("nav_home") or hasTestTag("nav_plan") or hasTestTag("nav_chantal") or hasTestTag("nav_statistics")).assertCountEquals(4)
        compose.onNodeWithTag("nav_settings").assertDoesNotExist()
        compose.onNodeWithTag("monthly_calendar_section").assertDoesNotExist()
        compose.onNodeWithTag("nav_statistics").performClick()
        compose.waitForIdle()
        compose.waitUntil(10000) { com.example.service.BenchSessionCoordinator.weeklyOverview.value.calendarDaysByDate.isNotEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("btn_workout_journal").performScrollTo()
        val statisticsOffset = scrollOffset("route_statistics")
        assertTrue(statisticsOffset > 0)
        compose.onNodeWithTag("open_settings").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("route_settings").assertExists()
        compose.onNodeWithTag("toolbar_back").performClick()
        compose.waitForIdle()
        assertEquals(statisticsOffset, scrollOffset("route_statistics"), 1f)
        back()
        compose.onNodeWithTag("route_home").assertExists()
        compose.onNodeWithTag("nav_statistics").performClick()
        compose.waitForIdle()
        assertEquals(statisticsOffset, scrollOffset("route_statistics"), 1f)
    }
    @Test fun activityRecreationRestoresChildRouteAndParent() {
        compose.onNodeWithTag("nav_statistics").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("open_settings").performClick()
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("route_settings").assertExists()
        compose.onNodeWithTag("toolbar_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("route_statistics").assertExists()
    }
    @Test fun backReturnsFromChantalChildBeforeNavigatingAway() {
        compose.onNodeWithTag("nav_chantal").performClick()
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("btn_complete_chantal_prologue").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("btn_complete_chantal_prologue").performScrollTo().performClick()
        compose.onNodeWithTag("btn_chantal_menu").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("chantal_world_menu").assertExists()
        back()
        compose.onNodeWithTag("chantal_world_menu").assertDoesNotExist()
        compose.onNodeWithTag("route_chantal").assertExists()
    }
}
