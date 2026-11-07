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
class WorkoutMapTest {
    @get:Rule val compose = createComposeRule()
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
}
