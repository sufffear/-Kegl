package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.*
import com.example.ui.ChantalWorldDiary
import com.example.ui.theme.VelarisTheme
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONArray
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class ChantalWorldDiaryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun emptyWorldDoesNotInventMeetingsOrClues() {
        val memory = ChantalWorldMemory.empty()
        val original = memory.toString()
        compose.setContent { VelarisTheme { ChantalWorldDiary(memory, emptyList(), emptyList()) } }
        compose.onNodeWithText("Незавершённые дела · 0/3").assertExists()
        compose.onNodeWithText("Знакомые", substring = true).performClick()
        compose.onNodeWithText("Здесь появятся знакомые из завершённых приключений.").assertExists()
        compose.onNodeWithText("Шкатулка", substring = true).performClick()
        compose.onNodeWithText("Пока нет королевских улик. Найденные улики появятся здесь.").assertExists()
        assertEquals(original, memory.toString())
    }
    @Test fun confirmedTaskExposesItsSourceWithoutChangingMemory() {
        val episode = ChantalOfflineAdventure.previewBase().copy(chosenActionSummaryRu = "Помочь другу")
        var memory = ChantalWorldMemory.reconcile(null, JSONArray().put(ChantalProgressCodec.encode(episode)))
        memory = ChantalWorldMemory.withFact(memory, "promises", JSONObject().put("id", "test")
            .put("sourceEpisodeId", episode.id).put("descriptionRu", "Принести нитку").put("status", "resolved"))
        val original = memory.toString()
        compose.setContent { VelarisTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            ChantalWorldDiary(memory, listOf(episode), emptyList())
        } } }
        compose.onNodeWithText("✓ Принести нитку", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Твой ответ: Помочь другу", substring = true).assertExists()
        assertEquals(original, memory.toString())
    }
}
