package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 5 тем оформления приложения Velaris (Итерация v1.4, раздел 2).
 * Каждая тема объединяет глубокие тёмные поверхности, насыщенные выразительные акценты,
 * согласованные градиенты карточек/кнопок и спокойную однотонную поверхность чтения.
 */
enum class VelarisThemeId(
    val id: String,
    val titleRu: String,
    val subtitleRu: String
) {
    DAWN(
        id = "theme_dawn",
        titleRu = "Рассвет",
        subtitleRu = "Глубокий фиолетовый, насыщенный розовый и тёплый персик"
    ),
    SKY(
        id = "theme_sky",
        titleRu = "Небо",
        subtitleRu = "Глубокий тёмно-синий, электрический голубой и чистая лазурь"
    ),
    AURORA(
        id = "theme_aurora",
        titleRu = "Северное сияние",
        subtitleRu = "Глубокий изумрудный, бирюзовый и яркая мята"
    ),
    SUNSET(
        id = "theme_sunset",
        titleRu = "Закат",
        subtitleRu = "Глубокий сливовый, яркий коралл и насыщенный оранжевый"
    ),
    SAKURA(
        id = "theme_sakura",
        titleRu = "Сакура",
        subtitleRu = "Глубокий пурпур, сочный розовый и сиреневый"
    );

    companion object {
        fun fromId(id: String?): VelarisThemeId =
            entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) } ?: DAWN
    }
}

/**
 * Семантические цветовые токены палитры Velaris (Итерация v1.4, раздел 2).
 * Контраст основного текста >= 4.5:1, крупного текста и элементов управления >= 3:1.
 */
data class VelarisPalette(
    val themeId: VelarisThemeId,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val readerSurface: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val onPrimaryAccent: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val buttonGradientStart: Color,
    val buttonGradientEnd: Color,
    val selectedContainer: Color,
    val selectedBorder: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val mountainFar: Color,
    val mountainMid: Color,
    val mountainNear: Color,
    val warningAccent: Color,
    val successAccent: Color,
    val errorAccent: Color,
    val phaseHoldAccent: Color,
    val phaseRelaxAccent: Color,
    val phasePrepAccent: Color,
    val phasePauseAccent: Color,
    val outline: Color
)

val DawnPalette = VelarisPalette(
    themeId = VelarisThemeId.DAWN,
    backgroundTop = Color(0xFF0D071A),
    backgroundMid = Color(0xFF1A0F30),
    backgroundBottom = Color(0xFF281542),
    surface = Color(0xFF18102B),
    surfaceElevated = Color(0xFF23173D),
    surfaceVariant = Color(0xFF2E1F4F),
    readerSurface = Color(0xFF100A1D),
    primaryAccent = Color(0xFFFF4D94),
    secondaryAccent = Color(0xFFFF8E6E),
    onPrimaryAccent = Color(0xFF160410),
    heroGradientStart = Color(0xFF2E164D),
    heroGradientEnd = Color(0xFF1B0E30),
    buttonGradientStart = Color(0xFFFF4D94),
    buttonGradientEnd = Color(0xFFFF8066),
    selectedContainer = Color(0xFF391C54),
    selectedBorder = Color(0xFFFF4D94),
    disabledContainer = Color(0xFF241B35),
    disabledContent = Color(0xFF83749E),
    textPrimary = Color(0xFFF8F4FF),
    textSecondary = Color(0xFFD8CBEB),
    mountainFar = Color(0xFF3D225E),
    mountainMid = Color(0xFF291544),
    mountainNear = Color(0xFF180C29),
    warningAccent = Color(0xFFFFB74D),
    successAccent = Color(0xFF3CE6A5),
    errorAccent = Color(0xFFFF6B81),
    phaseHoldAccent = Color(0xFFFF4D94),
    phaseRelaxAccent = Color(0xFF3CE6A5),
    phasePrepAccent = Color(0xFFFF9E6D),
    phasePauseAccent = Color(0xFFFFB74D),
    outline = Color(0xFF563E7A)
)

val SkyPalette = VelarisPalette(
    themeId = VelarisThemeId.SKY,
    backgroundTop = Color(0xFF060D1F),
    backgroundMid = Color(0xFF0D1B38),
    backgroundBottom = Color(0xFF142952),
    surface = Color(0xFF0E1A33),
    surfaceElevated = Color(0xFF152647),
    surfaceVariant = Color(0xFF1E345E),
    readerSurface = Color(0xFF091122),
    primaryAccent = Color(0xFF24B0FF),
    secondaryAccent = Color(0xFF5CE6FF),
    onPrimaryAccent = Color(0xFF031322),
    heroGradientStart = Color(0xFF16325C),
    heroGradientEnd = Color(0xFF0D1D38),
    buttonGradientStart = Color(0xFF1DA4FF),
    buttonGradientEnd = Color(0xFF4DE1FF),
    selectedContainer = Color(0xFF183661),
    selectedBorder = Color(0xFF24B0FF),
    disabledContainer = Color(0xFF15223B),
    disabledContent = Color(0xFF6E85A8),
    textPrimary = Color(0xFFF2F8FF),
    textSecondary = Color(0xFFC4D8F2),
    mountainFar = Color(0xFF1E3E6E),
    mountainMid = Color(0xFF152C52),
    mountainNear = Color(0xFF0B1830),
    warningAccent = Color(0xFFFFC04D),
    successAccent = Color(0xFF3CE6B0),
    errorAccent = Color(0xFFFF6E7A),
    phaseHoldAccent = Color(0xFF24B0FF),
    phaseRelaxAccent = Color(0xFF3CE6B0),
    phasePrepAccent = Color(0xFF5CE6FF),
    phasePauseAccent = Color(0xFFFFC04D),
    outline = Color(0xFF345687)
)

val AuroraPalette = VelarisPalette(
    themeId = VelarisThemeId.AURORA,
    backgroundTop = Color(0xFF051614),
    backgroundMid = Color(0xFF0B2825),
    backgroundBottom = Color(0xFF113B36),
    surface = Color(0xFF0B2220),
    surfaceElevated = Color(0xFF123330),
    surfaceVariant = Color(0xFF194540),
    readerSurface = Color(0xFF071615),
    primaryAccent = Color(0xFF00E5B8),
    secondaryAccent = Color(0xFF64FFDA),
    onPrimaryAccent = Color(0xFF021A15),
    heroGradientStart = Color(0xFF13423C),
    heroGradientEnd = Color(0xFF0B2421),
    buttonGradientStart = Color(0xFF00DCA8),
    buttonGradientEnd = Color(0xFF5CFFD6),
    selectedContainer = Color(0xFF154741),
    selectedBorder = Color(0xFF00E5B8),
    disabledContainer = Color(0xFF122B28),
    disabledContent = Color(0xFF68948D),
    textPrimary = Color(0xFFF2FFFB),
    textSecondary = Color(0xFFBCE6DD),
    mountainFar = Color(0xFF1B544B),
    mountainMid = Color(0xFF123B35),
    mountainNear = Color(0xFF09211E),
    warningAccent = Color(0xFFFFC457),
    successAccent = Color(0xFF47F09E),
    errorAccent = Color(0xFFFF6E7E),
    phaseHoldAccent = Color(0xFF00E5B8),
    phaseRelaxAccent = Color(0xFF64FFDA),
    phasePrepAccent = Color(0xFF5CE1FF),
    phasePauseAccent = Color(0xFFFFC457),
    outline = Color(0xFF2D6B62)
)

val SunsetPalette = VelarisPalette(
    themeId = VelarisThemeId.SUNSET,
    backgroundTop = Color(0xFF180813),
    backgroundMid = Color(0xFF2B1022),
    backgroundBottom = Color(0xFF3E172E),
    surface = Color(0xFF240E1D),
    surfaceElevated = Color(0xFF331529),
    surfaceVariant = Color(0xFF421C35),
    readerSurface = Color(0xFF140811),
    primaryAccent = Color(0xFFFF5E62),
    secondaryAccent = Color(0xFFFF9933),
    onPrimaryAccent = Color(0xFF1A0507),
    heroGradientStart = Color(0xFF471932),
    heroGradientEnd = Color(0xFF260E1E),
    buttonGradientStart = Color(0xFFFF5E62),
    buttonGradientEnd = Color(0xFFFF9933),
    selectedContainer = Color(0xFF4D1E36),
    selectedBorder = Color(0xFFFF5E62),
    disabledContainer = Color(0xFF2E1826),
    disabledContent = Color(0xFF947085),
    textPrimary = Color(0xFFFFF4F6),
    textSecondary = Color(0xFFEBC5D1),
    mountainFar = Color(0xFF59233D),
    mountainMid = Color(0xFF3D162B),
    mountainNear = Color(0xFF220B18),
    warningAccent = Color(0xFFFFB74D),
    successAccent = Color(0xFF42E6A4),
    errorAccent = Color(0xFFFF5268),
    phaseHoldAccent = Color(0xFFFF5E62),
    phaseRelaxAccent = Color(0xFF42E6A4),
    phasePrepAccent = Color(0xFFFF9933),
    phasePauseAccent = Color(0xFFFFB74D),
    outline = Color(0xFF703754)
)

val SakuraPalette = VelarisPalette(
    themeId = VelarisThemeId.SAKURA,
    backgroundTop = Color(0xFF15081C),
    backgroundMid = Color(0xFF261032),
    backgroundBottom = Color(0xFF361747),
    surface = Color(0xFF200E2B),
    surfaceElevated = Color(0xFF2E153D),
    surfaceVariant = Color(0xFF3C1D50),
    readerSurface = Color(0xFF120818),
    primaryAccent = Color(0xFFFF52A8),
    secondaryAccent = Color(0xFFD47AFF),
    onPrimaryAccent = Color(0xFF1A0412),
    heroGradientStart = Color(0xFF421957),
    heroGradientEnd = Color(0xFF230E30),
    buttonGradientStart = Color(0xFFFF52A8),
    buttonGradientEnd = Color(0xFFD47AFF),
    selectedContainer = Color(0xFF461D5C),
    selectedBorder = Color(0xFFFF52A8),
    disabledContainer = Color(0xFF2B1936),
    disabledContent = Color(0xFF8E729E),
    textPrimary = Color(0xFFFCF4FF),
    textSecondary = Color(0xFFE2C7F0),
    mountainFar = Color(0xFF52236B),
    mountainMid = Color(0xFF38164A),
    mountainNear = Color(0xFF1F0B2B),
    warningAccent = Color(0xFFFFBE55),
    successAccent = Color(0xFF42E6AD),
    errorAccent = Color(0xFFFF6680),
    phaseHoldAccent = Color(0xFFFF52A8),
    phaseRelaxAccent = Color(0xFF42E6AD),
    phasePrepAccent = Color(0xFFD47AFF),
    phasePauseAccent = Color(0xFFFFBE55),
    outline = Color(0xFF663A82)
)

fun paletteForTheme(themeId: VelarisThemeId): VelarisPalette = when (themeId) {
    VelarisThemeId.DAWN -> DawnPalette
    VelarisThemeId.SKY -> SkyPalette
    VelarisThemeId.AURORA -> AuroraPalette
    VelarisThemeId.SUNSET -> SunsetPalette
    VelarisThemeId.SAKURA -> SakuraPalette
}
