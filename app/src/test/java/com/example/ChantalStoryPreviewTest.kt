package com.example

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.ChantalStoryPreview
import com.example.ui.theme.VelarisTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class ChantalStoryPreviewTest {
    @get:Rule val compose = createComposeRule()
    @Test fun previewChoicesNeverMutateGameAndActionsRemainReachableWithLargeFont() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("chantal_progress", 0)
        val original = "{\"schemaVersion\":1,\"coins\":19,\"spentEnergy\":30}"
        prefs.edit().putString("snapshot", original).commit()
        var visible by mutableStateOf(true)
        var requests = 0
        val ep = ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())
        compose.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                androidx.compose.ui.platform.LocalDensity.current.density, 1.5f)) {
                VelarisTheme {
                    if (visible) ChantalStoryPreview({ visible = false }) {
                        requests++
                        ChantalGenerationResult.Ready(ChantalGeneratedStory(ep, emptyList()))
                    }
                }
            }
        }
        assertEquals(0, requests)
        compose.onNodeWithTag("preview_generate").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("preview_story_text").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("preview_choice_1").performScrollTo().performClick()
        compose.onNodeWithTag("preview_reaction").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("preview_close").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, requests)
            assertEquals(original, prefs.getString("snapshot", null))
            assertFalse(visible)
        }
    }
    @Test fun offlineAlternativesHaveConcreteEndingsAndThreeRelevantResponses() {
        val base = ChantalOfflineAdventure.previewBase()
        val stories = (1..3).map { ChantalOfflineAdventure.decorate(base.copy(journeyNumber = it)) }
        assertEquals(3, stories.map { it.storyTextRu }.toSet().size)
        for (ep in stories) {
            assertEquals(11, ep.storyTextRu.count { it == '.' || it == '!' || it == '?' })
            assertEquals(3, ep.choices.size)
            assertFalse(ep.storyTextRu.contains("важными слухами"))
            assertFalse(ep.storyTextRu.contains("проявив свою ведущую черту"))
            assertEquals(base.coinsEarned, ep.coinsEarned)
            assertEquals(base.id, ep.id)
        }
    }
}
