package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Пять атмосферных тёмных тем Velaris (ТЗ п. 4 и Update v1.1 п. 2).
 * Для каждой темы заранее проверены тёмный фон, градиент гор и семантические цвета
 * с гарантированным контрастом WCAG AA:
 * - Основной текст на фоне и поверхности: контраст > 11.5:1 (норма ≥ 4.5:1)
 * - Вторичный текст на поверхности: контраст > 7.2:1 (норма ≥ 4.5:1)
 * - Интерактивные элементы, кольцо таймера и статусы: контраст > 6.5:1 (норма ≥ 3:1)
 */
enum class VelarisThemeId(
    val id: String,
    val titleRu: String,
    val subtitleRu: String
) {
    DAWN(
        id = "theme_dawn",
        titleRu = "Рассвет",
        subtitleRu = "Фиолетовые сумерки и мягкий розовый свет (по умолчанию)"
    ),
    SKY(
        id = "theme_sky",
        titleRu = "Небо",
        subtitleRu = "Глубокий ночной сапфир и тёплый персиковый горизонт"
    ),
    AURORA(
        id = "theme_aurora",
        titleRu = "Северное сияние",
        subtitleRu = "Северная хвоя, изумрудная дымка и бирюзовое свечение"
    ),
    SUNSET(
        id = "theme_sunset",
        titleRu = "Закат",
        subtitleRu = "Тёплый базальт, янтарный отсвет и мягкое золото"
    ),
    SAKURA(
        id = "theme_sakura",
        titleRu = "Сакура",
        subtitleRu = "Тёмно-сливовая тень и нежный лепестковый розовый"
    );

    companion object {
        fun fromId(id: String?): VelarisThemeId {
            return entries.firstOrNull { it.id == id } ?: DAWN
        }
    }
}

data class VelarisPalette(
    val themeId: VelarisThemeId,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val readerSurface: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val secondaryAccent: Color,
    val onSecondaryAccent: Color,
    val successAccent: Color,
    val warningAccent: Color,
    val errorAccent: Color,
    val mountainBackLayer: Color,
    val mountainMidLayer: Color,
    val mountainFrontLayer: Color
)

// Совместимые константы темы по умолчанию («Рассвет»)
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

val VelarisDawnPalette = VelarisPalette(
    themeId = VelarisThemeId.DAWN,
    backgroundTop = DawnBackgroundTop,
    backgroundMid = DawnBackgroundMid,
    backgroundBottom = DawnBackgroundBottom,
    surface = DawnSurface,
    surfaceVariant = DawnSurfaceVariant,
    readerSurface = Color(0xFF161024),
    outline = DawnOutline,
    textPrimary = DawnTextPrimary,
    textSecondary = DawnTextSecondary,
    primaryAccent = DawnPrimaryAccent,
    onPrimaryAccent = DawnOnPrimaryAccent,
    secondaryAccent = DawnSecondaryAccent,
    onSecondaryAccent = DawnOnSecondaryAccent,
    successAccent = DawnSuccessAccent,
    warningAccent = DawnWarningAccent,
    errorAccent = DawnErrorAccent,
    mountainBackLayer = Color(0xFF2D1E4A),
    mountainMidLayer = Color(0xFF221638),
    mountainFrontLayer = Color(0xFF170F29)
)

val VelarisSkyPalette = VelarisPalette(
    themeId = VelarisThemeId.SKY,
    backgroundTop = Color(0xFF0B1324),
    backgroundMid = Color(0xFF13223D),
    backgroundBottom = Color(0xFF1E3150),
    surface = Color(0xFF152036),
    surfaceVariant = Color(0xFF1F2F4D),
    readerSurface = Color(0xFF10192B),
    outline = Color(0xFF425B87),
    textPrimary = Color(0xFFF0F4FC),
    textSecondary = Color(0xFFC4D2EB),
    primaryAccent = Color(0xFFF4B998),
    onPrimaryAccent = Color(0xFF29160C),
    secondaryAccent = Color(0xFF9AC5F8),
    onSecondaryAccent = Color(0xFF0D213A),
    successAccent = Color(0xFF8CE0B8),
    warningAccent = Color(0xFFF6C98E),
    errorAccent = Color(0xFFFFB4AB),
    mountainBackLayer = Color(0xFF243B61),
    mountainMidLayer = Color(0xFF1A2C4A),
    mountainFrontLayer = Color(0xFF101D33)
)

val VelarisAuroraPalette = VelarisPalette(
    themeId = VelarisThemeId.AURORA,
    backgroundTop = Color(0xFF0A1919),
    backgroundMid = Color(0xFF102829),
    backgroundBottom = Color(0xFF173A3B),
    surface = Color(0xFF132627),
    surfaceVariant = Color(0xFF1C3638),
    readerSurface = Color(0xFF0E1E1F),
    outline = Color(0xFF3E6E70),
    textPrimary = Color(0xFFECF8F7),
    textSecondary = Color(0xFFBBDAD8),
    primaryAccent = Color(0xFF84E3C8),
    onPrimaryAccent = Color(0xFF08261E),
    secondaryAccent = Color(0xFF96D4EE),
    onSecondaryAccent = Color(0xFF0B2530),
    successAccent = Color(0xFF95E8B6),
    warningAccent = Color(0xFFF6C98E),
    errorAccent = Color(0xFFFFB4AB),
    mountainBackLayer = Color(0xFF1E4748),
    mountainMidLayer = Color(0xFF163536),
    mountainFrontLayer = Color(0xFF0E2425)
)

val VelarisSunsetPalette = VelarisPalette(
    themeId = VelarisThemeId.SUNSET,
    backgroundTop = Color(0xFF1C1014),
    backgroundMid = Color(0xFF2B181D),
    backgroundBottom = Color(0xFF3D2226),
    surface = Color(0xFF26161B),
    surfaceVariant = Color(0xFF362027),
    readerSurface = Color(0xFF1D1115),
    outline = Color(0xFF6E454E),
    textPrimary = Color(0xFFFFF4F0),
    textSecondary = Color(0xFFE6C8BE),
    primaryAccent = Color(0xFFF5B87A),
    onPrimaryAccent = Color(0xFF2B1704),
    secondaryAccent = Color(0xFFF2A499),
    onSecondaryAccent = Color(0xFF2E110D),
    successAccent = Color(0xFF8CE0B8),
    warningAccent = Color(0xFFF7D08A),
    errorAccent = Color(0xFFFFB4AB),
    mountainBackLayer = Color(0xFF4A292E),
    mountainMidLayer = Color(0xFF361D22),
    mountainFrontLayer = Color(0xFF241216)
)

val VelarisSakuraPalette = VelarisPalette(
    themeId = VelarisThemeId.SAKURA,
    backgroundTop = Color(0xFF1A0F1A),
    backgroundMid = Color(0xFF281628),
    backgroundBottom = Color(0xFF381F36),
    surface = Color(0xFF241424),
    surfaceVariant = Color(0xFF341E34),
    readerSurface = Color(0xFF1B0F1B),
    outline = Color(0xFF6B4269),
    textPrimary = Color(0xFFFCF0F7),
    textSecondary = Color(0xFFE3C3D9),
    primaryAccent = Color(0xFFF4B0CB),
    onPrimaryAccent = Color(0xFF2B0E1B),
    secondaryAccent = Color(0xFFDAB6F2),
    onSecondaryAccent = Color(0xFF231130),
    successAccent = Color(0xFF8CE0B8),
    warningAccent = Color(0xFFF6C98E),
    errorAccent = Color(0xFFFFB4AB),
    mountainBackLayer = Color(0xFF452442),
    mountainMidLayer = Color(0xFF321930),
    mountainFrontLayer = Color(0xFF211020)
)

fun paletteForTheme(themeId: VelarisThemeId): VelarisPalette = when (themeId) {
    VelarisThemeId.DAWN -> VelarisDawnPalette
    VelarisThemeId.SKY -> VelarisSkyPalette
    VelarisThemeId.AURORA -> VelarisAuroraPalette
    VelarisThemeId.SUNSET -> VelarisSunsetPalette
    VelarisThemeId.SAKURA -> VelarisSakuraPalette
}
