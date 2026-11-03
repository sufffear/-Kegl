package com.example.domain

/**
 * Доменная модель сведений о сборке и среде выполнения для этапа 0A («Техническая проверка»).
 * Не зависит от Android UI и может проверяться в чистых модульных тестах.
 */
data class AppBuildInfo(
    val appName: String,
    val stageCode: String,
    val stageTitle: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidRelease: String,
    val androidSdkInt: Int,
    val hasInternetPermission: Boolean,
    val isOfflineVerified: Boolean,
    val isDemoOnlyStage: Boolean
)
