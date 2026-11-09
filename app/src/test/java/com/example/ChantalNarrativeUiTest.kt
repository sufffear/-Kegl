package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.VelarisTheme
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w411dp-h891dp")
class ChantalNarrativeUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun openRouterIsDefaultButNoRequestOrRewardHappensUntilUserActs() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("chantal_ai_selection",0).edit().clear().commit()
        context.getSharedPreferences("chantal_openrouter_access",0).edit().clear().commit()
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) { ChantalAiSettings() } } }
        compose.onNodeWithText("x-ai/grok-4.20").assertExists()
        compose.onNodeWithTag("ai_save_key").assertIsNotEnabled()
        compose.onNodeWithTag("ai_check_connection").assertDoesNotExist()
        assertEquals(0,context.getSharedPreferences("chantal_ai_selection",0).getLong("lastRequest",0))
    }
    @Test fun pendingStoryGetsFreeFallbackAndChoiceCommitsTheEngineWithReward() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val repo=ChantalProgressRepository(context)
        val ep=ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase()).copy(id="ui-engine",regionTitleRu="Старая водяная мельница",coinsEarned=21)
        val journal=JSONArray((0..3).map { ChantalProgressCodec.encode(ep.copy(id="lantern_chain_$it",chosenActionSummaryRu="Помочь")) })
        repo.replace(JSONObject().put("schemaVersion",1).put("hasCompletedPrologue",true).put("totalJourneys",4)
            .put("coins",100).put("journalList",journal).put("pendingEpisode",ChantalProgressCodec.encode(ep)).toString())
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) { VelarisChantalScreen(0,0,{},{}) } } }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("btn_story_choice_0").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Локальный рассказ · без запроса к модели").assertExists()
        assertEquals(100,ChantalProgressRepository(context).load().getInt("coins"))
        compose.onNodeWithTag("btn_story_choice_0").performScrollTo().performClick()
        compose.waitForIdle()
        val saved=ChantalProgressRepository(context).load()
        assertEquals(121,saved.getInt("coins"));assertEquals(5,saved.getInt("totalJourneys"))
        assertTrue(saved.has("storyEngineState"));assertTrue(saved.isNull("pendingEpisode"))
    }
}
