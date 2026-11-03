package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.BuildConfig
import com.example.domain.AppBuildInfo

/**
 * Репозиторий сведений о среде и параметрах сборки (пакет data).
 * Считывает фактический applicationId, версию из BuildConfig, реальную модель устройства
 * и проверяет итоговый манифест пакета на отсутствие сетевых разрешений.
 */
class AppBuildInfoRepository(private val context: Context) {

    fun loadBuildInfo(): AppBuildInfo {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val requestedPermissions: Array<String> = try {
            @Suppress("DEPRECATION")
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            packageInfo.requestedPermissions ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        val hasInternetPermission = requestedPermissions.contains(Manifest.permission.INTERNET)

        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        val model = Build.MODEL ?: "Unknown"
        val release = Build.VERSION.RELEASE ?: "Unknown"
        val sdkInt = Build.VERSION.SDK_INT

        return AppBuildInfo(
            appName = "Velaris",
            stageCode = "0B",
            stageTitle = "Технический стенд фонового таймера, сигналов и прерываний",
            applicationId = BuildConfig.APPLICATION_ID,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            minSdk = 24,
            targetSdk = 36,
            compileSdk = 36,
            deviceManufacturer = manufacturer,
            deviceModel = model,
            androidRelease = release,
            androidSdkInt = sdkInt,
            hasInternetPermission = hasInternetPermission,
            isOfflineVerified = !hasInternetPermission,
            isDemoOnlyStage = true
        )
    }
}
