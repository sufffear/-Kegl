package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.VelarisMountainHeader
import com.example.ui.theme.VelarisTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class VelarisHeaderLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun narrowLargeFontHeaderKeepsBothActionsReachableAt48dp() {
        var back = false
        var settings = false
        compose.setContent { VelarisTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                Box(Modifier.width(320.dp)) {
                    VelarisMountainHeader("Текущая тренировка", false, { back = true }, { settings = true })
                }
            }
        } }
        for (tag in listOf("toolbar_back", "open_settings")) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp).performClick()
        }
        assertTrue(back)
        assertTrue(settings)
    }
}
