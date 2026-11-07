package com.example.ui.theme

import android.animation.ValueAnimator
import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

val LocalVelarisPalette = staticCompositionLocalOf { DawnPalette }

@Composable
private fun animatePaletteColor(target: Color, animatorsEnabled: Boolean): Color {
    val spec = if (animatorsEnabled) tween<Color>(durationMillis = 160) else SnapSpec()
    val animated by animateColorAsState(targetValue = target, animationSpec = spec, label = "velaris_color")
    return animated
}

@Composable
fun VelarisTheme(
    themeId: VelarisThemeId = VelarisThemeId.DAWN,
    content: @Composable () -> Unit
) {
    val targetPalette = remember(themeId) { paletteForTheme(themeId) }
    val animatorsEnabled = remember {
        runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
    }

    val backgroundTop = animatePaletteColor(targetPalette.backgroundTop, animatorsEnabled)
    val backgroundMid = animatePaletteColor(targetPalette.backgroundMid, animatorsEnabled)
    val backgroundBottom = animatePaletteColor(targetPalette.backgroundBottom, animatorsEnabled)
    val surface = animatePaletteColor(targetPalette.surface, animatorsEnabled)
    val surfaceElevated = animatePaletteColor(targetPalette.surfaceElevated, animatorsEnabled)
    val surfaceVariant = animatePaletteColor(targetPalette.surfaceVariant, animatorsEnabled)
    val readerSurface = animatePaletteColor(targetPalette.readerSurface, animatorsEnabled)
    val primaryAccent = animatePaletteColor(targetPalette.primaryAccent, animatorsEnabled)
    val secondaryAccent = animatePaletteColor(targetPalette.secondaryAccent, animatorsEnabled)
    val onPrimaryAccent = animatePaletteColor(targetPalette.onPrimaryAccent, animatorsEnabled)
    val heroGradientStart = animatePaletteColor(targetPalette.heroGradientStart, animatorsEnabled)
    val heroGradientEnd = animatePaletteColor(targetPalette.heroGradientEnd, animatorsEnabled)
    val buttonGradientStart = animatePaletteColor(targetPalette.buttonGradientStart, animatorsEnabled)
    val buttonGradientEnd = animatePaletteColor(targetPalette.buttonGradientEnd, animatorsEnabled)
    val selectedContainer = animatePaletteColor(targetPalette.selectedContainer, animatorsEnabled)
    val selectedBorder = animatePaletteColor(targetPalette.selectedBorder, animatorsEnabled)

    val palette = remember(
        targetPalette,
        backgroundTop,
        backgroundMid,
        backgroundBottom,
        surface,
        surfaceElevated,
        surfaceVariant,
        readerSurface,
        primaryAccent,
        secondaryAccent,
        onPrimaryAccent,
        heroGradientStart,
        heroGradientEnd,
        buttonGradientStart,
        buttonGradientEnd,
        selectedContainer,
        selectedBorder
    ) {
        targetPalette.copy(
            backgroundTop = backgroundTop,
            backgroundMid = backgroundMid,
            backgroundBottom = backgroundBottom,
            surface = surface,
            surfaceElevated = surfaceElevated,
            surfaceVariant = surfaceVariant,
            readerSurface = readerSurface,
            primaryAccent = primaryAccent,
            secondaryAccent = secondaryAccent,
            onPrimaryAccent = onPrimaryAccent,
            heroGradientStart = heroGradientStart,
            heroGradientEnd = heroGradientEnd,
            buttonGradientStart = buttonGradientStart,
            buttonGradientEnd = buttonGradientEnd,
            selectedContainer = selectedContainer,
            selectedBorder = selectedBorder
        )
    }

    val colorScheme = remember(palette) {
        lightColorScheme(
            primary = palette.primaryAccent,
            onPrimary = palette.onPrimaryAccent,
            primaryContainer = palette.selectedContainer,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.secondaryAccent,
            onSecondary = palette.onPrimaryAccent,
            secondaryContainer = palette.surfaceElevated,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.warningAccent,
            background = palette.backgroundTop,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            error = palette.errorAccent
        )
    }

    // Принудительно включаем тёмные иконки/текст в системном верхнем статус-баре (время, заряд, уведомления)
    // и в нижней системной панели навигации во всех 5 воздушных светлых темах, независимо от системной темы телефона.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    // Нормализуем плотность экрана (если системный Масштаб экрана Samsung сжал ширину < 412dp)
    // и фиксируем аккуратный масштаб шрифта, чтобы интерфейс всегда оставался воздушным и компактным.
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentDensity = LocalDensity.current
    val balancedDensity = currentDensity

    CompositionLocalProvider(
        LocalVelarisPalette provides palette,
        LocalDensity provides balancedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
