package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalVelarisPalette

internal val HobbitBg: Color
    @Composable get() = LocalVelarisPalette.current.backgroundMid
internal val HobbitCardBg: Color
    @Composable get() = LocalVelarisPalette.current.surface
internal val HobbitElevatedBg: Color
    @Composable get() = LocalVelarisPalette.current.surfaceElevated
internal val HobbitBorder: Color
    @Composable get() = LocalVelarisPalette.current.outline
internal val HobbitGold: Color
    @Composable get() = LocalVelarisPalette.current.secondaryAccent
internal val HobbitEmerald: Color
    @Composable get() = LocalVelarisPalette.current.successAccent
internal val HobbitOrange: Color
    @Composable get() = LocalVelarisPalette.current.warningAccent
internal val HobbitPink: Color
    @Composable get() = LocalVelarisPalette.current.secondaryAccent
internal val HobbitSky: Color
    @Composable get() = LocalVelarisPalette.current.primaryAccent
internal val HobbitCream: Color
    @Composable get() = LocalVelarisPalette.current.textPrimary
internal val HobbitMuted: Color
    @Composable get() = LocalVelarisPalette.current.textPrimary.copy(alpha = 0.80f)
