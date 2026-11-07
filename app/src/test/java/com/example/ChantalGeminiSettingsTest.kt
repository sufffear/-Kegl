package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ChantalGeminiSettings
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class ChantalGeminiSettingsTest {
    @get:Rule val compose = createComposeRule()
    @Before fun clear() { ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("chantal_gemini_access", 0).edit().clear().commit() }
    @Test fun keyAndModelAreSeparatedAndHelpStartsCollapsed() {
        compose.setContent { Column(Modifier.verticalScroll(rememberScrollState())) { ChantalGeminiSettings() } }
        compose.onNodeWithTag("gemini_key_input").assertIsDisplayed()
        compose.onNodeWithTag("gemini_save_key").assertIsNotEnabled()
        compose.onNodeWithTag("gemini_model_input").assertDoesNotExist()
        compose.onNodeWithText("Изменить модель").performScrollTo().performClick()
        compose.onNodeWithTag("gemini_model_input").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Свернуть").performScrollTo().performClick()
        compose.onNodeWithTag("gemini_model_input").assertDoesNotExist()
        compose.onNodeWithTag("gemini_key_input").performScrollTo().performTextReplacement("short")
        compose.onNodeWithTag("gemini_save_key").performScrollTo().performClick()
        compose.onNode(hasText("KEY_FORMAT", substring = true)).assertExists()
    }
}
