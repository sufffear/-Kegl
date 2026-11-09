package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.service.BenchSessionCoordinator as Coordinator
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
@Config(sdk=[28],qualifiers="w360dp-h800dp")
class VelarisPlumActivityTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val setup=object:ExternalResource() {
        override fun before() { WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext<Context>()) }
    }
    @get:Rule val rules:RuleChain=RuleChain.outerRule(setup).around(compose)
    private fun click(tag:String){compose.onNodeWithTag(tag).performScrollTo().performClick();compose.waitForIdle()}
    private fun screenshot(name:String)=compose.runOnIdle {
        val view=compose.activity.window.decorView;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap));val file=File("build/reports/training-reference/$name.png");file.parentFile?.mkdirs()
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    @Test fun referenceRootPreviewRestorationAndExistingCatalogDoNotStartOrRewardAWorkout() {
        compose.waitUntil(10000){Coordinator.dailySlots.value.isNotEmpty()}
        // Fixed visual fixture: the prior tests may leave a different global theme.
        compose.onNodeWithTag("open_settings").performClick();click("settings_category_appearance")
        click("theme_option_theme_dawn")
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.onNodeWithTag("nav_plan").performClick();compose.waitForIdle()
        val energy=Coordinator.workoutEnergy.value
        val runId=Coordinator.engineSnapshot.value.runId
        compose.onNodeWithTag("training_hero").assertIsDisplayed()
        compose.onNodeWithTag("daily_campaign_card").assertDoesNotExist()
        compose.onNodeWithText("3:20").assertExists()
        screenshot("phone-portrait-100")
        click("btn_other_workouts")
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.onNodeWithTag("btn_other_workouts").performScrollTo().assertIsDisplayed()
        screenshot("phone-bottom-100")
        click("plan_group_tab_EVENING")
        click("daily_slot_real_v2_evening_slow")
        compose.onNodeWithTag("training_exercise_details").assertExists()
        assertEquals(runId,Coordinator.engineSnapshot.value.runId)
        screenshot("phone-settings-100")
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        compose.onNodeWithTag("training_settings_duration").assertTextContains("Длительность: 2:30",substring=true)
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.onNodeWithTag("plan_group_tab_EVENING").assertIsSelected()
        click("plan_group_tab_MORNING")
        RuntimeEnvironment.setFontScale(1.5f);compose.activityRule.scenario.recreate();compose.waitForIdle()
        click("btn_other_workouts")
        compose.onNodeWithTag("btn_create_custom_program").assertExists()
        click("plan_tab_extras");compose.onNodeWithTag("extra_workout_103").assertExists()
        click("plan_tab_free");compose.onNodeWithTag("free_interval_templates_section").assertExists()
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.onNodeWithTag("btn_start_slot_real_v2_morning_fast").performScrollTo().assertIsDisplayed()
        screenshot("phone-portrait-150")
        assertEquals(energy,Coordinator.workoutEnergy.value)
        assertEquals(runId,Coordinator.engineSnapshot.value.runId)
        compose.onNodeWithTag("open_settings").performScrollTo().performClick();click("settings_category_appearance")
        click("theme_option_theme_sky")
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.onNodeWithTag("toolbar_back").performClick();compose.waitForIdle()
        compose.runOnIdle {assertFalse(WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightStatusBars)}
        compose.onNodeWithTag("nav_home").performClick();compose.waitForIdle()
        compose.runOnIdle {assertTrue(WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightStatusBars)}
    }
}
