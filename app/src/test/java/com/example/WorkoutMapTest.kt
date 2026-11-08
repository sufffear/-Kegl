package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.ChantalWorldMap
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class WorkoutMapTest {
    @get:Rule val compose = createComposeRule()
    private fun screenshot(name: String) {
        compose.runOnIdle {
            val global=Class.forName("android.view.WindowManagerGlobal")
            val instance=global.getMethod("getInstance").invoke(null)
            @Suppress("UNCHECKED_CAST")
            val views=global.getDeclaredField("mViews").apply { isAccessible=true }.get(instance) as List<android.view.View>
            val view=views.last { it.visibility==android.view.View.VISIBLE && it.width>0 }
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val dir=java.io.File("build/reports/chantal-screenshots").apply { mkdirs() }
            java.io.FileOutputStream(java.io.File(dir,name)).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,95,it) }
            bitmap.recycle()
        }
    }
    @Test fun doubleTapZoomsAndReturnsToOverviewWithoutOpeningPlace() {
        compose.setContent { ChantalWorldMap(0, "", false) {} }
        compose.onNodeWithTag("open_world_map").performClick()
        compose.onNodeWithTag("map_gesture_surface").performTouchInput { doubleClick(center) }
        compose.onNodeWithTag("map_gesture_surface").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Масштаб 2.5"))
        compose.onNodeWithTag("map_place_details").assertDoesNotExist()
        compose.onNodeWithTag("map_gesture_surface").performTouchInput { doubleClick(center) }
        compose.onNodeWithTag("map_gesture_surface").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Масштаб 1.0"))
        screenshot("map_overview.png")
        compose.onNodeWithTag("map_label_1").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("choose_map_route").assertIsDisplayed()
        screenshot("map_details.png")
    }
    @Test fun lockedRegionCanBeInspectedButCannotBeSelected() {
        var selected = ""
        compose.setContent { Column(Modifier.verticalScroll(rememberScrollState())) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density, 1.5f)) {
                ChantalWorldMap(0, "", false) { selected = it }
            }
        } }
        compose.onNodeWithTag("region_reg_stone_halls").performScrollTo().performClick()
        compose.onNodeWithTag("choose_map_route").assertIsNotEnabled()
        compose.onNodeWithTag("close_map_details").performClick()
        compose.onNodeWithTag("region_reg_old_mill").performScrollTo().performClick()
        compose.onNodeWithTag("choose_map_route").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("reg_old_mill", selected) }
    }
    @Test fun transparentLabelsOpenDetailsWithoutMapControls() {
        compose.setContent { Column(Modifier.verticalScroll(rememberScrollState())) {
            ChantalWorldMap(0, "", false) { }
        } }
        compose.onNodeWithTag("open_world_map").performScrollTo().performClick()
        compose.onNodeWithText("Карта троп · приближай двумя пальцами").assertDoesNotExist()
        compose.onNodeWithText("Сброс").assertDoesNotExist()
        compose.onNodeWithTag("map_label_1").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("map_place_details").assertIsDisplayed()
        compose.onNode(hasText("Янтарная дубрава") and hasAnyAncestor(hasTestTag("map_place_details"))).assertIsDisplayed()
        compose.onNodeWithTag("choose_map_route").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("close_map_details").performClick()
        compose.onNodeWithTag("map_label_3").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("choose_map_route").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("close_map_details").performClick()
        compose.onNodeWithTag("close_world_map").performClick()
        compose.onNodeWithTag("map_label_1").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun placeActionsStayVisibleInLandscape() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent { Column(Modifier.verticalScroll(rememberScrollState())) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density, 1.5f)) {
                ChantalWorldMap(0, "", false) { }
            }
        } }
        compose.onNodeWithTag("open_world_map").performScrollTo().performClick()
        compose.onNodeWithTag("map_label_1").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("choose_map_route").assertIsDisplayed()
        compose.onNodeWithTag("close_map_details").assertIsDisplayed()
        screenshot("map_landscape.png")
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun detailsRemainUsableAtDoubleFontSize() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density, 2f)) {
                ChantalWorldMap(0, "", false) {}
            }
        }
        compose.onNodeWithTag("open_world_map").performClick()
        compose.onNodeWithTag("map_label_0").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("choose_map_route").assertIsDisplayed()
        compose.onNodeWithTag("close_map_details").assertIsDisplayed()
        screenshot("map_large_font.png")
    }

}
