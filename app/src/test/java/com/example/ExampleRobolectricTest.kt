package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `verify app name, applicationId and version in BuildConfig`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Velaris", appName)
        assertEquals("com.velaris.app", BuildConfig.APPLICATION_ID)
        assertEquals("0.1.0-0a", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
    }

    @Test
    fun `verify merged manifest does not request INTERNET permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        assertFalse(
            "Готовое приложение Velaris не должно запрашивать разрешение INTERNET",
            permissions.contains(Manifest.permission.INTERNET)
        )
    }

    @Test
    fun `verify TechCheckScreen displays title and version`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Velaris · Техническая проверка").assertIsDisplayed()
        composeTestRule.onNodeWithTag("version_info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Версия: 0.1.0-0a (versionCode 1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("com.velaris.app").assertIsDisplayed()
    }
}
