package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.domain.*
import com.example.ui.ChantalStoryFeedbackView
import com.example.ui.theme.VelarisTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class ChantalStoryFeedbackUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun optionalEditorSavesOnlyOnExplicitActionAndCanBeRevised() {
        var feedback by mutableStateOf<ChantalStoryFeedback?>(null)
        var saves = 0
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            ChantalStoryFeedbackView("episode-1", feedback) { feedback = it; saves++ }
        } } }
        compose.onNodeWithTag("feedback_save").assertDoesNotExist()
        compose.onNodeWithTag("feedback_expand").performClick()
        compose.onNodeWithTag("feedback_save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("feedback_rating_LIKED").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_reason_FUNNY").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_scope_GENERAL").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_comment").performScrollTo().performTextInput("Больше смешных диалогов")
        compose.runOnIdle { assertEquals(0, saves) }
        compose.onNodeWithTag("feedback_save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(ChantalStoryFeedback("episode-1", ChantalStoryRating.LIKED, ChantalFeedbackScope.GENERAL,
                setOf(ChantalFeedbackReason.FUNNY), "Больше смешных диалогов"), feedback)
        }
        compose.onNodeWithTag("feedback_expand").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_rating_DISLIKED").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_scope_THIS_STORY").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, saves); assertEquals(ChantalFeedbackScope.THIS_STORY, feedback!!.scope) }
    }

    @Test fun commentIsBoundedAndDraftDoesNotLeakToAnotherEpisode() {
        var episode by mutableStateOf("first")
        var saved: ChantalStoryFeedback? = null
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            ChantalStoryFeedbackView(episode, null) { saved = it }
        } } }
        compose.onNodeWithTag("feedback_expand").performClick()
        compose.onNodeWithTag("feedback_rating_OKAY").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_comment").performScrollTo().performTextInput("а".repeat(1100))
        compose.onNodeWithTag("feedback_save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1000, saved!!.comment.length); episode = "second" }
        compose.onNodeWithTag("feedback_expand").performScrollTo().performClick()
        compose.onNodeWithTag("feedback_save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("feedback_comment").performScrollTo().assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
    }
}
