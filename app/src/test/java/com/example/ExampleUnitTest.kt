package com.example

import com.example.domain.AppBuildInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `domain model holds stage 0A parameters accurately`() {
        val info = AppBuildInfo(
            appName = "Velaris",
            stageCode = "0A",
            stageTitle = "Проверка среды, минимальной сборки и способа установки",
            applicationId = "com.velaris.app",
            versionName = "0.1.0-0a",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 36,
            compileSdk = 36,
            deviceManufacturer = "Samsung",
            deviceModel = "SM-S921B",
            androidRelease = "15",
            androidSdkInt = 35,
            hasInternetPermission = false,
            isOfflineVerified = true,
            isDemoOnlyStage = true
        )

        assertEquals("0A", info.stageCode)
        assertEquals("com.velaris.app", info.applicationId)
        assertFalse(info.hasInternetPermission)
        assertTrue(info.isOfflineVerified)
        assertTrue(info.isDemoOnlyStage)
    }
}
