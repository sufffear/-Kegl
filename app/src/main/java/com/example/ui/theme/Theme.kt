package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VelarisDawnColorScheme = darkColorScheme(
    primary = DawnPrimaryAccent,
    onPrimary = DawnOnPrimaryAccent,
    secondary = DawnSecondaryAccent,
    onSecondary = DawnOnSecondaryAccent,
    tertiary = DawnSuccessAccent,
    background = DawnBackgroundTop,
    onBackground = DawnTextPrimary,
    surface = DawnSurface,
    onSurface = DawnTextPrimary,
    surfaceVariant = DawnSurfaceVariant,
    onSurfaceVariant = DawnTextSecondary,
    outline = DawnOutline,
    error = DawnErrorAccent
)

/**
 * Тема Velaris (по умолчанию — «Рассвет», тёмный атмосферный минимализм).
 * По ТЗ п. 4 используются заранее проверенные семантические цвета с гарантированным контрастом,
 * без случайной динамической генерации цветов.
 */
@Composable
fun VelarisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VelarisDawnColorScheme,
        typography = Typography,
        content = content
    )
}
