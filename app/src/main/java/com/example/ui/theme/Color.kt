package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Тема 1 по умолчанию: «Рассвет» (фиолетовый/розовый, тёмный атмосферный минимализм).
// Семантические цвета проверены на контрастность WCAG AA:
// - Основной текст (#F5EFFB) на фоне (#120D1D) и поверхности (#1E1630): контраст > 13:1 (норма ≥ 4.5:1)
// - Вторичный текст (#D4C4E3) на поверхности (#1E1630): контраст > 8.5:1 (норма ≥ 4.5:1)
// - Акцентные элементы (#E8B4D4) и статус завершения (#8CE0B8) на поверхности (#1E1630): контраст > 7.5:1 (норма ≥ 3:1)

val DawnBackgroundTop = Color(0xFF120D1D)
val DawnBackgroundMid = Color(0xFF1D1430)
val DawnBackgroundBottom = Color(0xFF2B1B42)

val DawnSurface = Color(0xFF1E1630)
val DawnSurfaceVariant = Color(0xFF2A1F42)
val DawnOutline = Color(0xFF564275)

val DawnTextPrimary = Color(0xFFF5EFFB)
val DawnTextSecondary = Color(0xFFD4C4E3)

val DawnPrimaryAccent = Color(0xFFE8B4D4)
val DawnOnPrimaryAccent = Color(0xFF241023)

val DawnSecondaryAccent = Color(0xFFC5B3F0)
val DawnOnSecondaryAccent = Color(0xFF1D1338)

val DawnSuccessAccent = Color(0xFF8CE0B8)
val DawnWarningAccent = Color(0xFFF6C98E)
val DawnErrorAccent = Color(0xFFFFB4AB)
