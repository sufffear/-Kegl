package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w320dp-h720dp")
class VelarisSettingsNavigationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val compose = createAndroidComposeRule<MainActivity>()
    private val setup = object : ExternalResource() {
        override fun before() {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            ChantalProgressRepository(context).replace(JSONObject().put("schemaVersion",1).put("coins",83).put("totalJourneys",8).toString())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(setup).around(compose)
    private fun click(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); compose.waitForIdle() }
    @Test fun categoriesKeepPositionAndBackupRestoresAsAChildAfterRecreation() {
        compose.onNodeWithTag("open_settings").performClick(); compose.waitForIdle()
        compose.onNodeWithTag("settings_category_tabs_row").assertDoesNotExist()
        compose.onNodeWithTag("theme_selector_card").assertDoesNotExist()
        compose.onNodeWithTag("settings_category_about").performScrollTo()
        val top = compose.onNodeWithTag("settings_category_about").fetchSemanticsNode().boundsInRoot.top
        click("settings_category_about")
        compose.onNodeWithTag("btn_open_diagnostics").assertExists()
        compose.onNodeWithTag("toolbar_back").performClick(); compose.waitForIdle()
        assertEquals(top,compose.onNodeWithTag("settings_category_about").fetchSemanticsNode().boundsInRoot.top,1f)
        click("settings_category_backup")
        compose.onNodeWithTag("btn_create_backup").assertExists()
        compose.onNodeWithTag("btn_restore_backup").assertExists()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithTag("data_settings_card").assertExists()
        click("btn_create_backup")
        compose.onNodeWithTag("btn_confirm_export_dialog").assertExists()
        compose.onNodeWithText("Отмена").performClick(); compose.waitForIdle()
        val saved = ChantalProgressRepository(context).load()
        assertEquals(83,saved.getInt("coins")); assertEquals(8,saved.getInt("totalJourneys"))
    }
    @Test fun appearanceSoundAndSamplesOpenDirectlyWithoutAccordions() {
        compose.onNodeWithTag("open_settings").performClick(); compose.waitForIdle()
        click("settings_category_appearance")
        compose.onNodeWithTag("theme_option_theme_dawn").assertExists()
        compose.onNodeWithTag("sound_settings_card").assertDoesNotExist()
        compose.onNodeWithTag("toolbar_back").performClick(); compose.waitForIdle()
        click("settings_category_sound")
        compose.onNodeWithTag("switch_sound_enabled").assertExists()
        compose.onNodeWithTag("sound_pack_velvet_bowl").assertExists()
        compose.onNodeWithTag("sound_samples_card").assertDoesNotExist()
        compose.onNodeWithTag("toolbar_back").performClick(); compose.waitForIdle()
        click("settings_category_samples")
        compose.onNodeWithTag("btn_sample_contraction").assertExists()
    }
}
