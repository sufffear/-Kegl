package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.domain.ChantalVisualState
import com.example.ui.ChantalAnimatedCompanion
import com.example.ui.theme.VelarisTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChantalAnimatedCompanionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun staticStatesRemainReadableWithoutMotion() {
        var view: android.view.View? = null
        compose.setContent {
            view = androidx.compose.ui.platform.LocalView.current
            VelarisTheme { Column {
                listOf(ChantalVisualState.REST, ChantalVisualState.TRAVELLING, ChantalVisualState.RETURNED, ChantalVisualState.TIRED).forEach {
                    ChantalAnimatedCompanion(it, false, false)
                }
            } }
        }
        compose.onNodeWithContentDescription("Шанталь: На лесной тропе").assertIsDisplayed()
        compose.onNodeWithText("Вернулась с историей").assertIsDisplayed()
        compose.runOnIdle {
            val root = view!!
            val bitmap = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            val directory = java.io.File("build/reports/chantal-screenshots").apply { mkdirs() }
            java.io.FileOutputStream(java.io.File(directory, "chantal-visual-states.png")).use {
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, it))
            }
            bitmap.recycle()
        }
    }
    @Test fun transformedCharacterDoesNotShowCubSprite() {
        compose.setContent { VelarisTheme { ChantalAnimatedCompanion(ChantalVisualState.REST, true, false) } }
        compose.onNodeWithText("Отдыхает дома").assertIsDisplayed()
        compose.onNodeWithContentDescription("Шанталь: Отдыхает дома").assertDoesNotExist()
    }
}
