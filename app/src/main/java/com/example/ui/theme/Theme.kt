package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVelarisPalette = staticCompositionLocalOf { VelarisDawnPalette }

private fun VelarisPalette.toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = primaryAccent,
    onPrimary = onPrimaryAccent,
    secondary = secondaryAccent,
    onSecondary = onSecondaryAccent,
    tertiary = successAccent,
    background = backgroundTop,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = textSecondary,
    outline = outline,
    error = errorAccent
)

/**
 * Тема Velaris с поддержкой 5 заранее проверенных контрастных палитр:
 * «Рассвет» (по умолчанию), «Небо», «Северное сияние», «Закат», «Сакура».
 */
@Composable
fun VelarisTheme(
    themeId: VelarisThemeId = VelarisThemeId.DAWN,
    content: @Composable () -> Unit
) {
    val palette = paletteForTheme(themeId)
    val colorScheme = palette.toMaterialColorScheme()

    CompositionLocalProvider(LocalVelarisPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
