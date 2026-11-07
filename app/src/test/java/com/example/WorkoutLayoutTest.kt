package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.ExtraWorkoutsSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class WorkoutLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun largeTextPresetRequiresConfirmationAndRespectsSessionLock() {
        var started = 0
        val locked = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ExtraWorkoutsSection(locked.value) { started++; locked.value = true }
                }
            }
        }
        compose.onNodeWithTag("extra_workout_103").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, started) }
        compose.onNodeWithText("Начать").performClick()
        compose.runOnIdle { assertEquals(1, started) }
        compose.onNodeWithTag("extra_workout_101").performScrollTo().assertIsNotEnabled()
    }
}
