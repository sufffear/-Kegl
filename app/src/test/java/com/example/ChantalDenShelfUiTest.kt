package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.*
import com.example.ui.ChantalDenShelfView
import com.example.ui.theme.VelarisTheme
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class ChantalDenShelfUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun arrangeInspectAndCloseWithLargeFont() {
        val items = listOf(ChantalShelfItem("light", "Деревянный огонёк", "✨", "Настоящее воспоминание", "Помню Брамбла"))
        var selection by mutableStateOf(emptyList<String>())
        compose.setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                androidx.compose.ui.platform.LocalDensity.current.density, 1.5f)) {
                VelarisTheme { ChantalDenShelfView(items, selection) { selection = ChantalDenShelf.select(selection, it, items) } }
            }
        }
        compose.onNodeWithTag("shelf_arrange").performClick()
        compose.onNodeWithTag("shelf_select_light").performScrollTo().performClick()
        compose.onNodeWithTag("shelf_close").assertIsDisplayed().performClick()
        compose.onNodeWithTag("shelf_item_light").performClick()
        compose.onNodeWithText("Настоящее воспоминание").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("shelf_close").assertIsDisplayed().performClick()
        assertEquals(listOf("light"), selection)
    }
}
