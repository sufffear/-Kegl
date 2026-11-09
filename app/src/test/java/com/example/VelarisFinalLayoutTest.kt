package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.ChantalProgressRepository
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w360dp-h800dp-port")
class VelarisFinalLayoutTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val compose = createAndroidComposeRule<MainActivity>()
    private val seed = object : ExternalResource() {
        override fun before() {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            RuntimeEnvironment.setFontScale(1.5f)
            ChantalProgressRepository(context).replace(JSONObject().put("schemaVersion",1).put("hasCompletedPrologue",true)
                .put("coins",42).put("maturityXp",29).put("totalJourneys",2).put("hasUsedPetThisCycle",true).toString())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(compose)
    private fun click(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); compose.waitForIdle() }
    private fun checkSystemIcons(dark: Boolean) = compose.runOnIdle {
        val controller = WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView)
        assertEquals(dark,controller.isAppearanceLightStatusBars)
        assertEquals(dark,controller.isAppearanceLightNavigationBars)
    }
    private fun screenshot(name: String) = compose.runOnIdle {
        val root = compose.activity.window.decorView
        val image = Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
        root.draw(Canvas(image))
        val file = File("build/reports/ui-final/$name.png"); file.parentFile.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
    }
    @Test fun largeFontThemeAndLandscapeKeepControlsAndSavedChildAccessible() {
        compose.waitForIdle()
        assertEquals(1.5f,compose.activity.resources.configuration.fontScale,0.01f)
        // Explicit theme selection makes this test independent of preceding preference fixtures.
        compose.onNodeWithTag("open_settings").performClick(); click("settings_category_appearance")
        click("theme_option_theme_dawn"); checkSystemIcons(false)
        compose.onNodeWithTag("toolbar_back").performClick(); compose.waitForIdle()
        compose.onNodeWithTag("toolbar_back").performClick(); compose.waitForIdle()
        compose.onNodeWithTag("nav_chantal").performClick(); compose.waitForIdle()
        for (tag in listOf("nav_home","nav_plan","nav_chantal","nav_statistics")) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            val title = com.example.ui.VelarisNavDestination.entries.first { "nav_${it.id}" == tag }.titleRu
            compose.onNodeWithTag(tag).assertContentDescriptionEquals(title)
        }
        val headerBottom = compose.onNodeWithTag("open_settings").fetchSemanticsNode().boundsInRoot.bottom
        val navTop = compose.onNodeWithTag("velaris_bottom_nav").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("chantal_context_action").performScrollTo().assertIsDisplayed()
        val action = compose.onNodeWithTag("chantal_context_action").fetchSemanticsNode().boundsInRoot
        assertTrue(action.top >= headerBottom); assertTrue(action.bottom <= navTop)
        screenshot("chantal-portrait-150")
        click("btn_quick_bag")
        RuntimeEnvironment.setQualifiers("w640dp-h360dp-land")
        RuntimeEnvironment.setFontScale(1.5f)
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        assertEquals(1.5f,compose.activity.resources.configuration.fontScale,0.01f)
        assertEquals(android.content.res.Configuration.ORIENTATION_LANDSCAPE,compose.activity.resources.configuration.orientation)
        compose.onNodeWithTag("chantal_section_bag").assertExists()
        compose.onNodeWithTag("toolbar_back").assertIsDisplayed().assertWidthIsAtLeast(48.dp).performClick(); compose.waitForIdle()
        compose.onNodeWithTag("chantal_context_action").performScrollTo().assertIsDisplayed()
        screenshot("chantal-landscape-150")
        compose.onNodeWithTag("open_settings").performClick(); click("settings_category_appearance")
        click("theme_option_theme_sky"); checkSystemIcons(true)
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithTag("theme_option_theme_sky").assertExists(); checkSystemIcons(true)
        screenshot("appearance-landscape-150")
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-port")
        RuntimeEnvironment.setFontScale(1.0f)
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithTag("nav_plan").assertTextContains("Тренировки")
        screenshot("appearance-portrait-100")
        val saved = ChantalProgressRepository(context).load()
        assertEquals(42,saved.getInt("coins")); assertEquals(29,saved.getInt("maturityXp")); assertEquals(2,saved.getInt("totalJourneys"))
    }
}
