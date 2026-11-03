package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `verify stage 0B versionCode 2, applicationId and offline manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Velaris", context.getString(R.string.app_name))
        assertEquals("com.velaris.app", BuildConfig.APPLICATION_ID)
        assertEquals("0.2.0-0b", BuildConfig.VERSION_NAME)
        assertEquals(2, BuildConfig.VERSION_CODE)

        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        assertFalse(
            "Приложение не должно запрашивать разрешение INTERNET",
            permissions.contains(Manifest.permission.INTERNET)
        )
        assertTrue(
            "Должно быть объявлено разрешение FOREGROUND_SERVICE_SPECIAL_USE",
            permissions.contains(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        )
    }

    @Test
    fun `verify Stage 0B screen displays demo safety warning, version 2, and controls`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("demo_safety_banner").assertExists()
        composeTestRule.onNodeWithText("Демо — не выполняйте упражнения по этим сигналам").assertExists()
        composeTestRule.onNodeWithTag("screen_title").assertExists()
        composeTestRule.onNodeWithText("Версия: 0.2.0-0b (versionCode 2)").assertExists()
        composeTestRule.onNodeWithTag("btn_start_demo").assertExists()
    }
}
