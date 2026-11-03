package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AppBuildInfo
import com.example.ui.theme.DawnBackgroundBottom
import com.example.ui.theme.DawnBackgroundMid
import com.example.ui.theme.DawnBackgroundTop
import com.example.ui.theme.DawnOutline
import com.example.ui.theme.DawnPrimaryAccent
import com.example.ui.theme.DawnSecondaryAccent
import com.example.ui.theme.DawnSuccessAccent
import com.example.ui.theme.DawnSurface
import com.example.ui.theme.DawnSurfaceVariant
import com.example.ui.theme.DawnTextPrimary
import com.example.ui.theme.DawnTextSecondary
import com.example.ui.theme.DawnWarningAccent

@Composable
fun TechCheckScreen(
    uiState: TechCheckUiState,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(DawnBackgroundTop, DawnBackgroundMid, DawnBackgroundBottom)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            // Тонкий векторный силуэт гор в нижней части фона (ТЗ п. 4: статичный градиент + тонкие векторные силуэты гор)
            MountainSilhouetteBackground(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .align(Alignment.BottomCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (uiState) {
                        is TechCheckUiState.Loading -> {
                            Text(
                                text = stringResource(R.string.tech_check_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = DawnTextPrimary,
                                modifier = Modifier.testTag("screen_title")
                            )
                        }

                        is TechCheckUiState.Ready -> {
                            val info = uiState.buildInfo

                            HeaderCard(info = info)
                            BuildIdentityCard(info = info)
                            DeviceRuntimeCard(info = info)
                            OfflineSafetyCard(info = info)
                            StageBoundaryNoticeCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(info: AppBuildInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DawnOutline.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = DawnSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Бейдж этапа 0A
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DawnSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("stage_badge")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DawnPrimaryAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tech_check_stage_badge),
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnPrimaryAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Главный заголовок: «Velaris · Техническая проверка»
            Text(
                text = stringResource(R.string.tech_check_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DawnTextPrimary,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag("screen_title")
            )

            // Явный вывод номера версии и versionCode на главном экране
            Text(
                text = stringResource(
                    R.string.tech_check_version_format,
                    info.versionName,
                    info.versionCode
                ),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = DawnSecondaryAccent,
                modifier = Modifier.testTag("version_info")
            )

            // Статус завершения сборки (галочка + текст «Завершено» + спокойный зелёный акцент по ТЗ п. 4)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("build_ready_status")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Статус: Завершено",
                    tint = DawnSuccessAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Завершено · Сборка готова к проверке установки",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BuildIdentityCard(info: AppBuildInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("build_identity_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Параметры пакета",
                    tint = DawnPrimaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Параметры сборки и пакета",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DawnTextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
            }

            InfoRow(
                label = stringResource(R.string.tech_check_app_id_label),
                value = info.applicationId,
                valueTag = "application_id_value",
                isMonospace = true
            )
            InfoRow(
                label = "Версия (versionName / versionCode)",
                value = "${info.versionName} / ${info.versionCode}",
                valueTag = "version_code_value",
                isMonospace = true
            )
            InfoRow(
                label = stringResource(R.string.tech_check_sdk_label),
                value = "minSdk ${info.minSdk} · targetSdk ${info.targetSdk} · compileSdk ${info.compileSdk}",
                valueTag = "sdk_levels_value",
                isMonospace = true
            )
            InfoRow(
                label = "Архитектура пакетов",
                value = "ui · domain · data (Single Activity, Compose M3)",
                valueTag = "architecture_value",
                isMonospace = false
            )
        }
    }
}

@Composable
private fun DeviceRuntimeCard(info: AppBuildInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_runtime_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = "Устройство",
                    tint = DawnSecondaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tech_check_device_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DawnTextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
            }

            InfoRow(
                label = "Производитель и модель",
                value = "${info.deviceManufacturer} · ${info.deviceModel}",
                valueTag = "device_model_value",
                isMonospace = false
            )
            InfoRow(
                label = "Фактическая ОС Android",
                value = "Android ${info.androidRelease} (API ${info.androidSdkInt})",
                valueTag = "device_os_value",
                isMonospace = true
            )
        }
    }
}

@Composable
private fun OfflineSafetyCard(info: AppBuildInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_safety_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Автономность и приватность",
                    tint = DawnSuccessAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tech_check_offline_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DawnTextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
            }

            Text(
                text = stringResource(R.string.tech_check_offline_status),
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSecondary
            )

            val internetPermissionStatus = if (!info.hasInternetPermission) {
                "Завершено · Разрешение INTERNET отсутствует в манифесте"
            } else {
                "Внимание · Обнаружено разрешение INTERNET"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("manifest_internet_status")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (!info.hasInternetPermission) DawnSuccessAccent else DawnWarningAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = internetPermissionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = DawnTextPrimary
                )
            }
        }
    }
}

@Composable
private fun StageBoundaryNoticeCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DawnWarningAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("stage_boundary_notice"),
        shape = RoundedCornerShape(16.dp),
        color = DawnSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Демо — не выполняйте упражнения по этим сигналам",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DawnWarningAccent
            )
            Text(
                text = stringResource(R.string.tech_check_stage_notice),
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSecondary
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueTag: String,
    isMonospace: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium,
            color = DawnTextPrimary,
            modifier = Modifier.testTag(valueTag)
        )
    }
}

@Composable
private fun MountainSilhouetteBackground(modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.mountain_illustration_desc)
    Canvas(
        modifier = modifier.semantics { contentDescription = desc }
    ) {
        val w = size.width
        val h = size.height

        // Дальний хребет (тонкий контур)
        val farRidge = Path().apply {
            moveTo(0f, h * 0.75f)
            lineTo(w * 0.22f, h * 0.35f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.74f, h * 0.25f)
            lineTo(w, h * 0.60f)
        }
        drawPath(
            path = farRidge,
            color = DawnPrimaryAccent.copy(alpha = 0.16f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Ближний хребет (тонкий контур)
        val nearRidge = Path().apply {
            moveTo(0f, h * 0.88f)
            lineTo(w * 0.34f, h * 0.48f)
            lineTo(w * 0.61f, h * 0.72f)
            lineTo(w * 0.86f, h * 0.44f)
            lineTo(w, h * 0.78f)
        }
        drawPath(
            path = nearRidge,
            color = DawnSecondaryAccent.copy(alpha = 0.20f),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Спокойная горизонтальная линия у основания
        drawLine(
            color = DawnOutline.copy(alpha = 0.25f),
            start = Offset(0f, h * 0.92f),
            end = Offset(w, h * 0.92f),
            strokeWidth = 1.dp.toPx()
        )
    }
}
