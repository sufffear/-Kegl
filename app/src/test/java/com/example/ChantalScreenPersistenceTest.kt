package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChantalProgressRepository
import com.example.ui.VelarisChantalScreen
import com.example.ui.theme.VelarisTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w411dp-h891dp")
class ChantalScreenPersistenceTest {
    @get:Rule val compose = createComposeRule()

    private fun saveCurrentWindowsScreenshot(rootView: View, fileName: String) {
        runCatching {
            val width = rootView.width.coerceAtLeast(411)
            val height = rootView.height.coerceAtLeast(891)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)

            runCatching {
                val wmGlobalClass = Class.forName("android.view.WindowManagerGlobal")
                val instance = wmGlobalClass.getMethod("getInstance").invoke(null)
                val viewsField = wmGlobalClass.getDeclaredField("mViews").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val views = viewsField.get(instance) as? List<View> ?: emptyList()
                views.forEach { decor ->
                    if (decor !== rootView.rootView && decor.visibility == View.VISIBLE && decor.width > 0 && decor.height > 0) {
                        canvas.drawARGB(120, 0, 0, 0)
                        val dx = ((width - decor.width) / 2f).coerceAtLeast(0f)
                        val dy = ((height - decor.height) / 2f).coerceAtLeast(0f)
                        canvas.save()
                        canvas.translate(dx, dy)
                        decor.draw(canvas)
                        canvas.restore()
                    }
                }
            }

            val outDir = File("/.aistudio/artifacts/brain/966c255f-81a5-45cf-b325-e14f2420e652")
            outDir.mkdirs()
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
        }
    }

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
        // Verify single petting button (no duplicate)
        compose.onAllNodesWithTag("btn_pet_chantal").assertCountEquals(1)
        compose.onNodeWithTag("btn_pet_chantal").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("chantal_prologue_card").assertDoesNotExist()
        // This cycle's pet was already claimed before leaving the screen.
        compose.onNodeWithTag("btn_pet_chantal").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            val saved = ChantalProgressRepository(context).load()
            assertTrue(saved.getBoolean("hasCompletedPrologue"))
            assertEquals(3, saved.getInt("maturityXp"))
            assertEquals(0, saved.getInt("totalJourneys"))
            assertEquals(0, saved.getJSONArray("inventoryList").length())
            assertEquals(0, saved.getInt("coins"))
        }
    }

    @Test fun modalMenuDoesNotShiftPortraitAndCoversAllFourJourneyStates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE).edit().clear().commit()

        var capturedRootView: View? = null
        compose.setContent {
            capturedRootView = LocalView.current
            VelarisTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VelarisChantalScreen(
                        completedWorkoutsCount = 3,
                        currentStreakDays = 3,
                        onOpenWorkoutPlan = {},
                        onPlaySampleTone = {}
                    )
                }
            }
        }

        // Complete prologue in isolated test profile
        compose.onNodeWithTag("btn_complete_chantal_prologue").performScrollTo().performClick()
        compose.waitForIdle()

        // State 1: Compact Den with closed menu — verify scene, header, status bar, 4 quick actions, story card, and hidden expedition panel
        compose.onNodeWithTag("chantal_compact_header").assertIsDisplayed()
        compose.onNodeWithTag("chantal_interactive_portrait").assertIsDisplayed()
        compose.onNodeWithTag("chantal_status_bar").assertIsDisplayed()
        compose.onNodeWithTag("chantal_quick_actions_row").assertIsDisplayed()
        compose.onNodeWithTag("chantal_compact_story_card").assertIsDisplayed()
        compose.onNodeWithTag("chantal_expedition_prep_panel").assertDoesNotExist()
        compose.runOnIdle {
            capturedRootView?.let { saveCurrentWindowsScreenshot(it, "1_chantal_den_closed_menu.png") }
        }

        val portraitTopBeforeMenu = compose.onNodeWithTag("chantal_interactive_portrait")
            .fetchSemanticsNode().boundsInRoot.top

        // State 2: Open main modal menu — verify all 6 sections exist and portrait Y did not shift down
        compose.onNodeWithTag("btn_chantal_menu").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("chantal_modal_menu").assertIsDisplayed()
        compose.onNodeWithTag("modal_nav_den").assertIsDisplayed()
        compose.onNodeWithTag("modal_nav_map").assertIsDisplayed()
        compose.onNodeWithTag("modal_nav_bag").assertIsDisplayed()
        compose.onNodeWithTag("modal_nav_shop").assertIsDisplayed()
        compose.onNodeWithTag("modal_nav_journal").assertIsDisplayed()
        compose.onNodeWithTag("modal_nav_ai").assertIsDisplayed()
        compose.runOnIdle {
            capturedRootView?.let { saveCurrentWindowsScreenshot(it, "2_chantal_open_main_menu.png") }
        }

        val portraitTopDuringMenu = compose.onNodeWithTag("chantal_interactive_portrait")
            .fetchSemanticsNode().boundsInRoot.top
        assertEquals(portraitTopBeforeMenu, portraitTopDuringMenu, 0.5f)

        // Close modal menu
        compose.onNodeWithTag("btn_close_chantal_modal").performClick()
        compose.waitForIdle()

        // State 3: Open Expedition preparation panel via the «Поход» button (btn_quick_expedition)
        compose.onNodeWithTag("btn_quick_expedition").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("chantal_expedition_prep_panel").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("slot_card_OUTFIT").assertIsDisplayed()
        compose.onNodeWithTag("slot_card_TOOL").assertIsDisplayed()
        compose.onNodeWithTag("slot_card_WEAPON").assertIsDisplayed()
        compose.onNodeWithTag("slot_card_BUNDLE").assertIsDisplayed()
        compose.onNodeWithTag("btn_start_chantal_journey").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            capturedRootView?.let { saveCurrentWindowsScreenshot(it, "3_chantal_expedition_prep.png") }
        }

        // Launch expedition (30 energy available from 3 completed workouts)
        compose.onNodeWithTag("btn_start_chantal_journey").performScrollTo().performClick()
        compose.waitForIdle()

        // State 4: Active expedition & return with reward popup
        compose.onNodeWithTag("chantal_active_journey_card").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("btn_complete_active_journey").performScrollTo().performClick()
        compose.waitForIdle()

        // Pending story episode choice -> opens reward popup and saves progress
        compose.onNodeWithTag("btn_story_choice_0").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("chantal_reward_popup").assertIsDisplayed()
        compose.runOnIdle {
            capturedRootView?.let { saveCurrentWindowsScreenshot(it, "4_chantal_active_journey_and_reward.png") }
        }
        compose.onNodeWithTag("btn_claim_reward_popup").performClick()
        compose.waitForIdle()

        compose.runOnIdle {
            val saved = ChantalProgressRepository(context).load()
            assertEquals(1, saved.getInt("totalJourneys"))
            assertTrue(saved.getInt("coins") > 0)
            assertEquals(1, saved.getJSONArray("inventoryList").length())
            assertEquals(1, saved.getJSONArray("journalList").length())
        }
    }
}
