package com.example.domain

/**
 * Доменная модель системы персонажа «Шанталь»:
 * - Двухактная эволюция (Акт I: лисёнок в стиле «Хоббита», стадии 1..3 -> Акт II: скрытая трансформация в расколдованную принцессу на 100+ походах).
 * - 6 параметров характера: Любопытство, Нежность, Игривость, Романтичность, Отвага, Остроумие.
 * - Разделение сюжетных кнопок (двигают канон и меняют характер) и свободного текста (одноразовые филлеры без изменения характера).
 * - Экономика Энергии (+10 за подход Кегеля, 30 за часовой поход) и Монет (с бонусом «Попутного ветра» от серии дней).
 * - Инвентарь находок с лором, Лавка из 3 категорий и «Плохие новости» (долг в монетах и усталость на 45 Энергии).
 */

enum class ChantalStoryDifficulty(val code: String, val titleRu: String) {
    EASY("EASY", "Лёгкий"),
    MEDIUM("MEDIUM", "Средний"),
    HARD("HARD", "Тяжёлый"),
    SUPER_HARD("SUPER_HARD", "Супер тяжёлый")
}

enum class ChantalAiProvider(val code: String, val titleRu: String) {
    GROK("grok", "Grok (xAI API)"),
    GEMINI("gemini", "Gemini Flash")
}

data class ChantalTraits(
    val curiosity: Int = 14,    // Любопытство
    val tenderness: Int = 16,   // Нежность
    val playfulness: Int = 12,  // Игривость
    val romanticism: Int = 10,  // Романтичность
    val courage: Int = 11,      // Отвага
    val wit: Int = 13           // Остроумие
) {
    fun applyCanonicalDelta(delta: ChantalTraits): ChantalTraits {
        return copy(
            curiosity = (curiosity + delta.curiosity).coerceAtLeast(1),
            tenderness = (tenderness + delta.tenderness).coerceAtLeast(1),
            playfulness = (playfulness + delta.playfulness).coerceAtLeast(1),
            romanticism = (romanticism + delta.romanticism).coerceAtLeast(1),
            courage = (courage + delta.courage).coerceAtLeast(1),
            wit = (wit + delta.wit).coerceAtLeast(1)
        )
    }

    fun formatArrowIndicators(): List<String> {
        val result = mutableListOf<String>()
        fun addIfNonZero(nameRu: String, v: Int) {
            if (v > 0) result.add("$nameRu ${if (v >= 2) "↑↑" else "↑"}")
            else if (v < 0) result.add("$nameRu ${if (v <= -2) "↓↓" else "↓"}")
        }
        addIfNonZero("Любопытство", curiosity)
        addIfNonZero("Нежность", tenderness)
        addIfNonZero("Игривость", playfulness)
        addIfNonZero("Романтичность", romanticism)
        addIfNonZero("Отвага", courage)
        addIfNonZero("Остроумие", wit)
        return result
    }
}

enum class ChantalLootCategory {
    TRINKET,
    HERB,
    ROYAL_CLUE
}

data class ChantalInventoryLootItem(
    val id: String,
    val titleRu: String,
    val category: ChantalLootCategory,
    val descriptionRu: String,
    val sellPriceCoins: Int,
    val foundAtRegionRu: String,
    val foundDateIso: String
)

enum class ChantalShopCategory {
    APPAREL,
    DEN_COMFORT,
    CONSUMABLE
}

data class ChantalShopItem(
    val id: String,
    val titleRu: String,
    val category: ChantalShopCategory,
    val priceCoins: Int,
    val visualSlotLabelRu: String,
    val descriptionRu: String,
    val gameplayEffectRu: String,
    val reducesExhaustionCostBy: Int = 0
)

data class ChantalStageStatus(
    val stageNumber: Int,
    val isTransformed: Boolean,
    val titleRu: String,
    val subtitleRu: String,
    val nextMilestoneHintRu: String
)

fun resolveChantalStageStatus(totalJourneys: Int, forceTransformPreview: Boolean = false): ChantalStageStatus {
    val isTransformed = forceTransformPreview || totalJourneys >= 100
    if (isTransformed) {
        val masteryRank = ((totalJourneys - 100).coerceAtLeast(0) / 50) + 1
        val nextTarget = 100 + masteryRank * 50
        return ChantalStageStatus(
            stageNumber = 4,
            isTransformed = true,
            titleRu = "Шанталь · Расколдованная принцесса (Ранг $masteryRank)",
            subtitleRu = "Проклятие пало — впереди большая магическая сага в мире королевств и древних чар",
            nextMilestoneHintRu = "До следующей главы мастерства: ${(nextTarget - totalJourneys).coerceAtLeast(1)} поход."
        )
    }
    return when {
        totalJourneys <= 10 -> ChantalStageStatus(
            stageNumber = 1,
            isTransformed = false,
            titleRu = "Стадия I · Спасённый лисёнок",
            subtitleRu = "Кроха в тёплом пледе: только осваивается в доме и делает первые шаги за порог",
            nextMilestoneHintRu = "До следующей вехи взросления: ${(10 - totalJourneys).coerceAtLeast(1)} из 10 походов"
        )
        totalJourneys <= 50 -> ChantalStageStatus(
            stageNumber = 2,
            isTransformed = false,
            titleRu = "Стадия II · Любопытный лисёнок",
            subtitleRu = "Окрепла, носит маленькую кожаную сумку через плечо и смело исследует тропы",
            nextMilestoneHintRu = "Прогресс стадии: ${totalJourneys - 10} из 40 походов (всего $totalJourneys/50)"
        )
        else -> ChantalStageStatus(
            stageNumber = 3,
            isTransformed = false,
            titleRu = "Стадия III · Юная путешественница",
            subtitleRu = "В дорожном плаще с медным фонарём: уходит на дальние перевалы навстречу тайнам",
            nextMilestoneHintRu = "Прогресс стадии: ${totalJourneys - 50} из 50 походов (всего $totalJourneys/100)"
        )
    }
}
