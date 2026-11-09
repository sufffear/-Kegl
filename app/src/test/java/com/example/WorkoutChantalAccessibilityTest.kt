package com.example
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.ChantalProgressRepository
import com.example.ui.VelarisChantalScreen
import com.example.ui.theme.VelarisTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class WorkoutChantalAccessibilityTest {
    @get:Rule val compose = createComposeRule()
    @Test fun quickActionsAndMapRemainReachableAtLargeFont() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ChantalProgressRepository(context).save(JSONObject().put("hasCompletedPrologue", true))
        compose.setContent { VelarisTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                Column(Modifier.verticalScroll(rememberScrollState())) { VelarisChantalScreen(0, 0, {}, {}) }
            }
        } }
        for (tag in listOf("btn_pet_chantal", "chantal_context_action", "btn_quick_bag", "btn_quick_story")) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("btn_chantal_menu").performScrollTo().performClick()
        compose.onNodeWithTag("chantal_world_map").performScrollTo().performClick()
        compose.onNodeWithTag("open_world_map").performScrollTo().assertIsDisplayed()
    }
}
