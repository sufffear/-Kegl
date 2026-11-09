package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.ChantalAiSettings
import com.example.ui.theme.VelarisTheme
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w320dp-h720dp")
class ChantalAiSettingsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cryptoKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private fun credential(provider: String) = ChantalGeminiAccess(context, provider) { cryptoKey }
    private val fakeSecret = "test.only.synthetic.123456789012345"
    private var visible by mutableStateOf(true)
    @Before fun seed() {
        for (name in listOf("chantal_ai_selection","chantal_gemini_access","chantal_openrouter_access")) context.getSharedPreferences(name,0).edit().clear().commit()
        credential("openrouter").save(fakeSecret)
        ChantalProgressRepository(context).replace(JSONObject().put("schemaVersion",1).put("coins",71).put("totalJourneys",3).toString())
    }
    private fun render(check: suspend (String) -> String = { error("Unexpected request") }) {
        compose.setContent { VelarisTheme {
            if (visible) Column(Modifier.verticalScroll(rememberScrollState())) {
                ChantalAiSettings(credentialFactory = ::credential, checkConnection = check)
            }
        } }
    }
    private fun click(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); compose.waitForIdle() }
    private fun unchangedGame() {
        val saved = ChantalProgressRepository(context).load()
        assertEquals(71,saved.getInt("coins")); assertEquals(3,saved.getInt("totalJourneys"))
    }
    @Test fun overviewIsCompactAndKeyEditingNeverSendsARequest() {
        var requests = 0
        render { requests++; "{}" }
        compose.onNodeWithTag("ai_key_input").assertDoesNotExist()
        click("ai_manage_key")
        compose.onNodeWithTag("ai_key_input").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithTag("ai_key_input").performTextReplacement(fakeSecret + "new")
        click("ai_save_key")
        compose.onNodeWithText("Ключ сохранён. Запрос не отправлялся.").assertExists()
        assertEquals(fakeSecret + "new",credential("openrouter").read())
        assertEquals(0,requests); unchangedGame()
    }
    @Test fun providerModelAndBothEncryptedKeysSurviveLeavingAndReopening() {
        credential("gemini").save(fakeSecret + "gemini")
        val originalCipher = context.getSharedPreferences("chantal_openrouter_access",0).getString("encrypted",null)
        render()
        click("ai_provider_gemini"); click("ai_manage_model")
        compose.onNodeWithTag("ai_model_input").performTextReplacement("gemini-custom-1")
        click("ai_save_model"); click("ai_detail_close")
        compose.runOnIdle { visible = false }; compose.waitForIdle()
        compose.runOnIdle { visible = true }; compose.waitForIdle()
        compose.onNodeWithTag("ai_current_model").assertTextEquals("gemini-custom-1")
        assertEquals("gemini",ChantalAiAccess(context).provider())
        assertEquals(fakeSecret + "gemini",credential("gemini").read())
        assertEquals(originalCipher,context.getSharedPreferences("chantal_openrouter_access",0).getString("encrypted",null))
        assertEquals(fakeSecret,credential("openrouter").read()); unchangedGame()
    }
    @Test fun connectionIsExplicitSingleFlightAndShowsLoadingAndSuccess() {
        val response = CompletableDeferred<String>()
        var requests = 0
        render { requests++; response.await() }
        assertEquals(0,requests)
        val action = compose.onNodeWithTag("ai_check_connection").performScrollTo().fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle { action(); action() }; compose.waitForIdle()
        compose.onNodeWithTag("ai_check_loading").assertExists()
        compose.onNodeWithTag("ai_check_connection").assertIsNotEnabled()
        assertEquals(1,requests)
        compose.runOnIdle { response.complete("{\"ok\":true}") }; compose.waitForIdle()
        compose.onNodeWithText("Модель отвечает. Можно создавать истории.").assertExists()
        compose.onNodeWithTag("ai_check_loading").assertDoesNotExist()
        click("ai_check_connection")
        compose.onNodeWithText("Между запросами нужна минута.").assertExists()
        assertEquals(1,requests); unchangedGame()
    }
    @Test fun leavingCancelsTheUiRequestAndDoesNotLeaveALoadingFlagOrAutoRetry() {
        val response = CompletableDeferred<String>()
        var requests = 0
        var cancelled = false
        render { requests++; try { response.await() } finally { cancelled = true } }
        click("ai_check_connection")
        compose.runOnIdle { visible = false }; compose.waitForIdle()
        assertTrue(cancelled)
        compose.runOnIdle { visible = true }; compose.waitForIdle()
        compose.onNodeWithTag("ai_check_loading").assertDoesNotExist()
        compose.onNodeWithTag("ai_check_connection").assertIsEnabled()
        assertEquals(1,requests); unchangedGame()
    }
    @Test fun connectionFailureIsSanitizedAndLoadingEnds() {
        render { throw OpenRouterRequestException(401) }
        click("ai_check_connection")
        compose.onNodeWithTag("ai_status_message").assertTextContains("HTTP 401", substring = true)
        compose.onNodeWithTag("ai_check_loading").assertDoesNotExist(); unchangedGame()
    }
    @Test fun keyDeletionRequiresConfirmationAndPreviewOpeningIsFree() {
        render()
        click("ai_open_preview"); compose.onNodeWithTag("preview_close").performClick(); compose.waitForIdle()
        assertEquals(0,context.getSharedPreferences("chantal_ai_selection",0).getLong("lastRequest",0))
        click("ai_manage_key"); click("ai_delete_key"); click("ai_cancel_delete")
        assertTrue(credential("openrouter").hasKey())
        click("ai_delete_key"); click("ai_confirm_delete"); click("ai_detail_close")
        assertFalse(credential("openrouter").hasKey())
        compose.onNodeWithTag("ai_check_connection").assertIsNotEnabled(); unchangedGame()
    }
}
