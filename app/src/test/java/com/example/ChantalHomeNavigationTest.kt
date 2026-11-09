package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.*
import com.example.domain.*
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
@Config(sdk=[28],qualifiers="w360dp-h800dp")
class ChantalHomeNavigationTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val compose=createAndroidComposeRule<MainActivity>()
    private val seed=object: ExternalResource() {
        override fun before() {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            val home=ChantalHomePresence.encode(ChantalHomePresence.respond(ChantalHomePresence.State(ChantalStoryEmotion.SADNESS),ChantalWishStatus.DEFERRED))
            ChantalProgressRepository(context).replace(JSONObject().put("schemaVersion",1).put("hasCompletedPrologue",true)
                .put("coins",42).put("maturityXp",29).put("totalJourneys",2).put("favoriteSnack",2).put("sharedGreeting",1)
                .put("storyEmotion","SADNESS").put("homePresence",home).put("hasUsedPetThisCycle",true).toString())
        }
    }
    @get:Rule val rules:RuleChain=RuleChain.outerRule(seed).around(compose)
    @Test fun childPagesKeepHomeCleanAndPreserveLegacyDataAndScroll() {
        compose.onNodeWithTag("nav_chantal").performClick();compose.waitForIdle()
        val repo=ChantalProgressRepository(context)
        val before=repo.load()
        val calls=context.getSharedPreferences("chantal_ai_selection",0).getLong("lastRequest",0)
        compose.onAllNodesWithTag("chantal_context_action").assertCountEquals(1)
        compose.onNodeWithText("Побыть с Шанталь").assertDoesNotExist()
        compose.onNodeWithText("Наши маленькие привычки",substring=true).assertDoesNotExist()
        compose.onNodeWithTag("chantal_status_bar").assertDoesNotExist()
        val top=compose.onNodeWithTag("chantal_interactive_portrait").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("btn_chantal_menu").performScrollTo().performClick();compose.waitForIdle()
        compose.onNodeWithTag("chantal_modal_menu").assertDoesNotExist()
        compose.onNodeWithTag("chantal_world_development").performScrollTo().performClick();compose.waitForIdle()
        compose.onNodeWithTag("chantal_status_bar").assertExists()
        compose.onNodeWithTag("toolbar_back").assertIsDisplayed().performClick();compose.waitForIdle()
        compose.onNodeWithTag("chantal_world_ai").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("toolbar_back").assertIsDisplayed().performClick();compose.waitForIdle()
        assertEquals(top,compose.onNodeWithTag("chantal_interactive_portrait").fetchSemanticsNode().boundsInRoot.top,1f)
        compose.onNodeWithTag("btn_quick_interior").performScrollTo().performClick();compose.waitForIdle()
        compose.onNodeWithTag("toolbar_back").assertIsDisplayed().performClick();compose.waitForIdle()
        compose.onNodeWithTag("btn_quick_bag").performScrollTo().performClick();compose.waitForIdle()
        compose.onNodeWithTag("chantal_section_bag").assertExists()
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        compose.onNodeWithTag("chantal_section_bag").assertExists()
        compose.onNodeWithTag("toolbar_back").assertIsDisplayed().performClick();compose.waitForIdle()
        val after=repo.load()
        for(key in listOf("coins","maturityXp","totalJourneys","favoriteSnack","sharedGreeting","storyEmotion","homePresence"))
            assertEquals(key,before.get(key).toString(),after.get(key).toString())
        assertEquals(calls,context.getSharedPreferences("chantal_ai_selection",0).getLong("lastRequest",0))
    }
}
