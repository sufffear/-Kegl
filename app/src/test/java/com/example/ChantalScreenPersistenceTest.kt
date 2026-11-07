package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChantalProgressRepository
import com.example.ui.VelarisChantalScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp")
class ChantalScreenPersistenceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun prologueAndPettingSurviveLeavingTheScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE).edit().clear().commit()
        val visible = mutableStateOf(true)
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (visible.value) VelarisChantalScreen(0, 0, {}, {})
            }
        }
        compose.onNodeWithTag("btn_complete_chantal_prologue").performScrollTo().performClick()
        compose.onNodeWithTag("btn_pet_chantal").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("chantal_prologue_card").assertDoesNotExist()
        compose.runOnIdle {
            val saved = ChantalProgressRepository(context).load()
            assertTrue(saved.getBoolean("hasCompletedPrologue"))
            assertEquals(3, saved.getInt("maturityXp"))
            assertEquals(0, saved.getInt("totalJourneys"))
            assertEquals(0, saved.getJSONArray("inventoryList").length())
            assertEquals(0, saved.getInt("coins"))
        }
    }
}
