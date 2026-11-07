package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 5 воздушных светлых тем оформления Velaris в эстетике Apple iOS / Samsung One UI Light.
 * Полностью исключены тёмные тона: белоснежные карточки, мягкие пастельные фоны,
 * выразительные чистые акценты и высокий контраст тёмного текста и системного статус-бара.
 */
enum class VelarisThemeId(
    val id: String,
    val titleRu: String,
    val subtitleRu: String
) {
    DAWN(
        id = "theme_dawn",
        titleRu = "Лесной дневник",
        subtitleRu = "Тёмный лес, бронзовые рамки и тёплый янтарь"
    ),
    SKY(
        id = "theme_sky",
        titleRu = "Samsung Небесный",
        subtitleRu = "Светлый облачно-голубой стиль One UI, чистый белый и небесная лазурь"
    ),
    AURORA(
        id = "theme_aurora",
        titleRu = "Свежая Мята",
        subtitleRu = "Воздушный мятно-шалфейный оттенок, белые поверхности и живая зелень"
    ),
    SUNSET(
        id = "theme_sunset",
        titleRu = "Утренний Персик",
        subtitleRu = "Тёплый кремово-жемчужный фон, чистый белый и мягкий солнечный коралл"
    ),
    SAKURA(
        id = "theme_sakura",
        titleRu = "Воздушная Лаванда",
        subtitleRu = "Нежная утренняя сирень, белоснежные карточки и чистый ирисовый акцент"
    );

    companion object {
        fun fromId(id: String?): VelarisThemeId =
            entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) } ?: DAWN
    }
}

/**
 * Семантические цветовые токены светлой воздушной палитры Velaris.
 * Контраст основного текста >= 12:1, вторичного текста >= 5.5:1.
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

/**
 * 1. Лесной дневник — снежно-белые карточки, светлый серебристо-жемчужный фон и изумрудный акцент.
 */
val DawnPalette = VelarisPalette(
    themeId = VelarisThemeId.DAWN,
    backgroundTop = Color(0xFF111A16),
    backgroundMid = Color(0xFF0D1511),
    backgroundBottom = Color(0xFF090F0C),
    surface = Color(0xFF101A14),
    surfaceElevated = Color(0xFF241F17),
    surfaceVariant = Color(0xFF30271C),
    readerSurface = Color(0xFF172019),
    primaryAccent = Color(0xFFDBAE66),
    secondaryAccent = Color(0xFFD6B88B),
    onPrimaryAccent = Color(0xFF20170C),
    heroGradientStart = Color(0xFF14261C),
    heroGradientEnd = Color(0xFF0B130F),
    buttonGradientStart = Color(0xFF936126),
    buttonGradientEnd = Color(0xFFD6A54D),
    selectedContainer = Color(0xFF294630),
    selectedBorder = Color(0xFFAF854C),
    disabledContainer = Color(0xFF252B24),
    disabledContent = Color(0xFF919487),
    textPrimary = Color(0xFFF7F7F2),
    textSecondary = Color(0xFFD1D5CE),
    mountainFar = Color(0xFF283D2D),
    mountainMid = Color(0xFF1B2D20),
    mountainNear = Color(0xFF101C14),
    warningAccent = Color(0xFFE3AD60),
    successAccent = Color(0xFF9FC39A),
    errorAccent = Color(0xFFF19D85),
    phaseHoldAccent = Color(0xFFD9B16D),
    phaseRelaxAccent = Color(0xFF92BEAD),
    phasePrepAccent = Color(0xFFD5B78B),
    phasePauseAccent = Color(0xFFD5B78B),
    outline = Color(0xFF8C714D)
)

/**
 * 2. Samsung Небесный — воздушный голубой фон One UI, белоснежные карточки и чистая лазурь.
 */
val SkyPalette = VelarisPalette(
    themeId = VelarisThemeId.SKY,
    backgroundTop = Color(0xFFF2F7FF),
    backgroundMid = Color(0xFFEAF2FE),
    backgroundBottom = Color(0xFFE0ECFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEEF5FD),
    surfaceVariant = Color(0xFFDCEAFB),
    readerSurface = Color(0xFFF9FBFF),
    primaryAccent = Color(0xFF007AFF),
    secondaryAccent = Color(0xFF0284C7),
    onPrimaryAccent = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFFFFFFFF),
    heroGradientEnd = Color(0xFFEFF6FF),
    buttonGradientStart = Color(0xFF007AFF),
    buttonGradientEnd = Color(0xFF0284C7),
    selectedContainer = Color(0xFFDBEAFE),
    selectedBorder = Color(0xFF007AFF),
    disabledContainer = Color(0xFFE2E8F0),
    disabledContent = Color(0xFF94A3B8),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    mountainFar = Color(0xFFDBEAFE),
    mountainMid = Color(0xFFBFDBFE),
    mountainNear = Color(0xFF93C5FD),
    warningAccent = Color(0xFFD97706),
    successAccent = Color(0xFF059669),
    errorAccent = Color(0xFFDC2626),
    phaseHoldAccent = Color(0xFF007AFF),
    phaseRelaxAccent = Color(0xFF10B981),
    phasePrepAccent = Color(0xFF0284C7),
    phasePauseAccent = Color(0xFFD97706),
    outline = Color(0xFFCBD5E1)
)

/**
 * 3. Свежая Мята — мягкий мятно-шалфейный светлый фон, белые карточки и сочный зелёный акцент.
 */
val AuroraPalette = VelarisPalette(
    themeId = VelarisThemeId.AURORA,
    backgroundTop = Color(0xFFF0FDF4),
    backgroundMid = Color(0xFFE6FAEE),
    backgroundBottom = Color(0xFFDCFCE7),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFECFDF3),
    surfaceVariant = Color(0xFFD8F8E5),
    readerSurface = Color(0xFFF8FEFA),
    primaryAccent = Color(0xFF16A34A),
    secondaryAccent = Color(0xFF0D9488),
    onPrimaryAccent = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFFFFFFFF),
    heroGradientEnd = Color(0xFFDCFCE7),
    buttonGradientStart = Color(0xFF16A34A),
    buttonGradientEnd = Color(0xFF0D9488),
    selectedContainer = Color(0xFFDCFCE7),
    selectedBorder = Color(0xFF16A34A),
    disabledContainer = Color(0xFFE2E8F0),
    disabledContent = Color(0xFF94A3B8),
    textPrimary = Color(0xFF0F291E),
    textSecondary = Color(0xFF3F6252),
    mountainFar = Color(0xFFDCFCE7),
    mountainMid = Color(0xFFBBF7D0),
    mountainNear = Color(0xFF86EFAC),
    warningAccent = Color(0xFFD97706),
    successAccent = Color(0xFF15803D),
    errorAccent = Color(0xFFDC2626),
    phaseHoldAccent = Color(0xFF16A34A),
    phaseRelaxAccent = Color(0xFF0D9488),
    phasePrepAccent = Color(0xFF0284C7),
    phasePauseAccent = Color(0xFFD97706),
    outline = Color(0xFFBBF7D0)
)

/**
 * 4. Утренний Персик — тёплый жемчужно-кремовый светлый фон, белые карточки и мягкий коралл.
 */
val SunsetPalette = VelarisPalette(
    themeId = VelarisThemeId.SUNSET,
    backgroundTop = Color(0xFFFFFAF5),
    backgroundMid = Color(0xFFFEF4EA),
    backgroundBottom = Color(0xFFFCECE0),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFF5EB),
    surfaceVariant = Color(0xFFFDEBD9),
    readerSurface = Color(0xFFFFFDF9),
    primaryAccent = Color(0xFFEA580C),
    secondaryAccent = Color(0xFFD97706),
    onPrimaryAccent = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFFFFFFFF),
    heroGradientEnd = Color(0xFFFFF1E6),
    buttonGradientStart = Color(0xFFF97316),
    buttonGradientEnd = Color(0xFFEA580C),
    selectedContainer = Color(0xFFFFEDD5),
    selectedBorder = Color(0xFFF97316),
    disabledContainer = Color(0xFFE7E5E4),
    disabledContent = Color(0xFFA8A29E),
    textPrimary = Color(0xFF1C1917),
    textSecondary = Color(0xFF57534E),
    mountainFar = Color(0xFFFFEDD5),
    mountainMid = Color(0xFFFED7AA),
    mountainNear = Color(0xFFFDBA74),
    warningAccent = Color(0xFFD97706),
    successAccent = Color(0xFF059669),
    errorAccent = Color(0xFFDC2626),
    phaseHoldAccent = Color(0xFFEA580C),
    phaseRelaxAccent = Color(0xFF059669),
    phasePrepAccent = Color(0xFFD97706),
    phasePauseAccent = Color(0xFFD97706),
    outline = Color(0xFFE7D5C5)
)

/**
 * 5. Воздушная Лаванда — светлая сиреневая дымка, белоснежные карточки и чистый ирисовый акцент.
 */
val SakuraPalette = VelarisPalette(
    themeId = VelarisThemeId.SAKURA,
    backgroundTop = Color(0xFFFAF7FF),
    backgroundMid = Color(0xFFF4EEFF),
    backgroundBottom = Color(0xFFEDE4FF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF6F1FF),
    surfaceVariant = Color(0xFFECE3FF),
    readerSurface = Color(0xFFFCFAFF),
    primaryAccent = Color(0xFF7C3AED),
    secondaryAccent = Color(0xFF9333EA),
    onPrimaryAccent = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFFFFFFFF),
    heroGradientEnd = Color(0xFFF3E8FF),
    buttonGradientStart = Color(0xFF8B5CF6),
    buttonGradientEnd = Color(0xFF7C3AED),
    selectedContainer = Color(0xFFEDE9FE),
    selectedBorder = Color(0xFF7C3AED),
    disabledContainer = Color(0xFFE2E8F0),
    disabledContent = Color(0xFF94A3B8),
    textPrimary = Color(0xFF1E1B4B),
    textSecondary = Color(0xFF4C4975),
    mountainFar = Color(0xFFEDE9FE),
    mountainMid = Color(0xFFDDD6FE),
    mountainNear = Color(0xFFC4B5FD),
    warningAccent = Color(0xFFD97706),
    successAccent = Color(0xFF059669),
    errorAccent = Color(0xFFDC2626),
    phaseHoldAccent = Color(0xFF7C3AED),
    phaseRelaxAccent = Color(0xFF059669),
    phasePrepAccent = Color(0xFF9333EA),
    phasePauseAccent = Color(0xFFD97706),
    outline = Color(0xFFD8CFF0)
)

fun paletteForTheme(themeId: VelarisThemeId): VelarisPalette = when (themeId) {
    VelarisThemeId.DAWN -> DawnPalette
    VelarisThemeId.SKY -> SkyPalette
    VelarisThemeId.AURORA -> AuroraPalette
    VelarisThemeId.SUNSET -> SunsetPalette
    VelarisThemeId.SAKURA -> SakuraPalette
}

