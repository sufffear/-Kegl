package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Reference-derived semantic palette; opt-in per screen until the remaining UI is approved. */
object ReferencePlum {
    val Background = Color(0xFF100D13)
    val BackgroundBottom = Color(0xFF0C0A0F)
    val Surface = Color(0xFF231823)
    val SurfaceTop = Color(0xFF33212F)
    val Gold = Color(0xFFE7BC78)
    val GoldBright = Color(0xFFFFD990)
    val GoldShadow = Color(0xFF70452D)
    val Lilac = Color(0xFFE09BD9)
    val Purple = Color(0xFFA95BB0)
    val PurpleDeep = Color(0xFF44213F)
    val Outline = Color(0xFF70505F)
    val Text = Color(0xFFF6F0F0)
    val Muted = Color(0xFFC5B7C6)
    val Success = Color(0xFF91C3A0)
    val SuccessSurface = Color(0xFF1B3125)
    val Disabled = Color(0xFF796B7C)
    val FastGradient = listOf(Color(0xFF382327),Color(0xFF221A1B),Color(0xFF1C1419))
    val StrengthGradient = listOf(Color(0xFF332237),Color(0xFF211720),Color(0xFF1B131D))
    val EnduranceGradient = listOf(Color(0xFF39223F),Color(0xFF221823),Color(0xFF1C1420))
    val ActivePeriodGradient = listOf(Color(0xFF78522D),Color(0xFF3A281E))
    val CompletedPeriodGradient = listOf(SuccessSurface,Color(0xFF15221B))
    val InfoGradient = listOf(Color(0xFF25221F),Color(0xFF18151B))
    val ExtraGradient = listOf(Color(0xFF1D141D),Background)
    val PlayGradient = listOf(Color(0xFF814554),Color(0xFF48213F))
    val Error = Color(0xFFF196A7)
    val palette = DawnPalette.copy(
        backgroundTop = Background, backgroundMid = Background, backgroundBottom = BackgroundBottom,
        surface = Surface, surfaceElevated = SurfaceTop, surfaceVariant = PurpleDeep, readerSurface = Background,
        primaryAccent = Gold, secondaryAccent = Lilac, onPrimaryAccent = Background,
        heroGradientStart = SurfaceTop, heroGradientEnd = Background,
        buttonGradientStart = GoldShadow, buttonGradientEnd = Gold,
        selectedContainer = PurpleDeep, selectedBorder = Lilac, disabledContainer = Surface,
        disabledContent = Disabled, textPrimary = Text, textSecondary = Muted,
        warningAccent = Gold, successAccent = Success, errorAccent = Error,
        phaseHoldAccent = Purple, phaseRelaxAccent = Lilac, phasePrepAccent = Gold,
        phasePauseAccent = Muted, outline = Outline,
        mountainFar = SurfaceTop, mountainMid = Surface, mountainNear = Background
    )
}

@Composable
internal fun ReferencePlumTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVelarisPalette provides ReferencePlum.palette) {
        MaterialTheme(colorScheme = darkColorScheme(
            primary = ReferencePlum.Gold, onPrimary = ReferencePlum.Background,
            secondary = ReferencePlum.Lilac, onSecondary = ReferencePlum.Background,
            background = ReferencePlum.Background, onBackground = ReferencePlum.Text,
            surface = ReferencePlum.Surface, onSurface = ReferencePlum.Text,
            surfaceVariant = ReferencePlum.PurpleDeep, onSurfaceVariant = ReferencePlum.Muted,
            outline = ReferencePlum.Outline, error = ReferencePlum.Error
        ), typography = MaterialTheme.typography, content = content)
    }
}
