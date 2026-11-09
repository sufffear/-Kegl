package com.example
import com.example.ui.VelarisNavDestination as D
import com.example.ui.VelarisNavigation as Nav
import org.junit.Assert.*
import org.junit.Test
class VelarisNavigationTest {
    @Test fun fourMainSectionsHaveStableIdsAndSettingsAreAChild() {
        assertEquals(listOf("home", "plan", "chantal", "statistics"), D.entries.filter { it.isMainSection }.map { it.id })
        assertFalse(D.SETTINGS.isMainSection)
        assertFalse(D.WORKOUT.isMainSection)
    }
    @Test fun backReturnsToPreviousSectionThenItsParentWithoutChangingAnEngine() {
        val settings = Nav().open(D.PLAN).open(D.STATISTICS).open(D.SETTINGS)
        val diagnostics = settings.open(D.DIAGNOSTICS)
        assertEquals(D.STATISTICS, diagnostics.mainSection)
        assertEquals(settings, diagnostics.back())
        assertEquals(D.STATISTICS, settings.back().current)
        assertEquals(D.PLAN, settings.back().back().current)
        assertEquals(Nav(), Nav().back())
    }
    @Test fun repeatedClicksAndCyclesDoNotAccumulateDuplicateRoutes() {
        var nav = Nav().open(D.PLAN).open(D.SETTINGS)
        assertEquals(nav, nav.open(D.SETTINGS))
        nav = nav.open(D.PLAN)
        assertEquals(listOf(D.HOME, D.PLAN), nav.routes)
        assertEquals(Nav(), nav.open(D.HOME))
    }
    @Test fun savedStateRestoresStackAndUnknownOrInvalidHistoryFallsBackSafely() {
        val nav = Nav().open(D.CHANTAL).open(D.SETTINGS).open(D.DIAGNOSTICS)
        assertEquals(nav, Nav.restore(nav.routes.map { it.id }))
        for (ids in listOf(null, emptyList(), listOf("unknown"), listOf("settings"), listOf("home", "home"))) assertEquals(Nav(), Nav.restore(ids))
    }
    @Test fun workoutRouteReturnsToOriginAndRemainsAChildOfTraining() {
        val nav = Nav().open(D.CHANTAL).open(D.PLAN)
        val workout = nav.open(D.WORKOUT)
        assertEquals(D.PLAN, workout.mainSection)
        assertEquals(nav, workout.back())
        assertEquals(workout, workout.open(D.SETTINGS).back())
    }
}
