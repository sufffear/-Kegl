package com.example.domain

/**
 * Полная доменная модель мира «Шанталь» для Android (Jetpack Compose),
 * синхронизированная 1-в-1 с веб-версией:
 * - Система накопления опыта взросления (XP): каждый поход (+10/+15 XP) и поглаживание (+3 XP)
 *   сохраняются отдельно; возраст определяется приключениями с границами 10/50/100.
 * - Система жилья «10 предметов = новый уровень Домика» (Уровни I → II → III → IV, 30 предметов в лавке + трофеи из походов).
 * - 4 слота снаряжения в поход: Одежда (OUTFIT), Инструмент (TOOL), Оружие (WEAPON) и Узелок (BUNDLE).
 * - 6 черт характера по 100-балльной шкале с активными перками.
 */

enum class ChantalStoryDifficulty(val code: String, val titleRu: String) {
    EASY("EASY", "Лёгкий"),
    MEDIUM("MEDIUM", "Средний"),
    HARD("HARD", "Тяжёлый"),
    SUPER_HARD("SUPER_HARD", "Супер тяжёлый")
}

enum class ChantalAiProvider(val code: String, val titleRu: String) {
    GEMINI("gemini", "Прямой Gemini Flash (По умолч.)"),
    OPENROUTER("openrouter", "OpenRouter (Любая модель)"),
    GROK("grok", "Прямой Grok (xAI API)")
}

val OPENROUTER_ANDROID_MODELS = listOf(
    "google/gemini-2.5-flash" to "Gemini 2.5 Flash (Быстрый)",
    "x-ai/grok-3-mini" to "Grok 3 Mini (Юмор и драма)",
    "anthropic/claude-3.7-sonnet" to "Claude 3.7 Sonnet (Проза)",
    "deepseek/deepseek-chat" to "DeepSeek V3 (Логика сюжета)",
    "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3 70B (:free)"
)

data class ChantalTraitPerks(
    val curiosityBonusCoins: Int,
    val curiosityClueChancePct: Int,
    val tendernessMoodHealBonusPct: Int,
    val playfulnessCharmOutOfDebtChancePct: Int,
    val romanticismEveningCoinBonusPct: Int,
    val courageAvoidExhaustionChancePct: Int,
    val witMerchantSellBonusPct: Int,
    val dominantTraitNameRu: String
)

data class ChantalTraits(
    val curiosity: Int = 5,    // Любопытство (0..100)
    val tenderness: Int = 6,   // Нежность (0..100)
    val playfulness: Int = 4,  // Игривость (0..100)
    val romanticism: Int = 3,  // Романтичность (0..100)
    val courage: Int = 4,      // Отвага (0..100)
    val wit: Int = 5           // Остроумие (0..100)
) {
    fun applyCanonicalDelta(delta: ChantalTraits): ChantalTraits {
        return copy(
            curiosity = (curiosity + delta.curiosity).coerceIn(1, 100),
            tenderness = (tenderness + delta.tenderness).coerceIn(1, 100),
            playfulness = (playfulness + delta.playfulness).coerceIn(1, 100),
            romanticism = (romanticism + delta.romanticism).coerceIn(1, 100),
            courage = (courage + delta.courage).coerceIn(1, 100),
            wit = (wit + delta.wit).coerceIn(1, 100)
        )
    }

    fun computePerks(): ChantalTraitPerks {
        val pairs = listOf(
            "Любопытство" to curiosity,
            "Нежность" to tenderness,
            "Игривость" to playfulness,
            "Романтичность" to romanticism,
            "Отвага" to courage,
            "Остроумие" to wit
        )
        val dominant = pairs.maxByOrNull { it.second }?.first ?: "Нежность"
        return ChantalTraitPerks(
            curiosityBonusCoins = (curiosity / 3).coerceAtLeast(2),
            curiosityClueChancePct = (15 + curiosity * 2).coerceAtMost(65),
            tendernessMoodHealBonusPct = (8 + tenderness / 2).coerceAtMost(30),
            playfulnessCharmOutOfDebtChancePct = (12 + playfulness * 2).coerceAtMost(50),
            romanticismEveningCoinBonusPct = (romanticism * 2).coerceAtMost(45),
            courageAvoidExhaustionChancePct = (10 + courage * 2).coerceAtMost(55),
            witMerchantSellBonusPct = (wit * 2).coerceAtMost(60),
            dominantTraitNameRu = dominant
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

    fun buildPettingReaction(isTransformed: Boolean, tapCount: Int): String {
        val dom = computePerks().dominantTraitNameRu
        if (isTransformed) {
            return when (dom) {
                "Любопытство" -> "Шанталь поправляет огненно-рыжую прядь над картой: «Смотри, на полях этого свитка проступают королевские руны! Скоро мы разгадаем шифр»."
                "Игривость" -> "Шанталь лукаво смеётся у камина: «Знаешь, в лисьем облике таскать яблоки с ярмарки было проще, но и сейчас я любого купца обведу вокруг пальца!»"
                "Романтичность" -> "Глядя на искры в камине, Шанталь тихо произносит: «Звёзды над перевалом сегодня светят совсем как в ту ночь, когда ты спас меня в лесу...»"
                "Отвага" -> "Шанталь решительно проверяет ремешок сумки: «Пусть буря воет на перевале — с твоей поддержкой мы пройдём любую тропу»."
                "Остроумие" -> "Шанталь с улыбкой подбрасывает золотую монету: «Гном Торвин снова пытался сбить цену, но против нашей логики у него не было ни шанса!»"
                else -> if (tapCount % 2 == 0) {
                    "Шанталь мягко касается твоей ладони тёплыми пальцами: «Как же хорошо возвращаться домой, когда меня здесь ждут»."
                } else {
                    "Она уютно кутается в плед у камина и прислоняется к твоему плечу: «Побудь со мной немного в тишине перед новой дорогой»."
                }
            }
        }
        return when (dom) {
            "Любопытство" -> "Ты гладишь Шанталь за рыжим ушком — она смешно наклоняет голову набок и тут же начинает обнюхивать твои карманы в поисках диковинок!"
            "Игривость" -> "Едва ты протягиваешь руку, лисёнок делает озорной прыжок передними лапками и затевает весёлую возню на пледе!"
            "Романтичность" -> "Шанталь ласково трётся щекой о твою ладонь и долго смотрит в окно на первые вечерние звёзды удивительно осмысленным взглядом."
            "Отвага" -> "Лисёнок гордо выпрямляет спинку, когда ты треплешь её по холке — она явно считает себя главным защитником этого дома!"
            "Остроумие" -> "Шанталь хитро прищуривается от удовольствия и ловко подвигает лапкой мисочку ближе к тебе — намекая на медовый пирог!"
            else -> if (tapCount % 2 == 0) {
                "Шанталь блаженно зажмуривает янтарные глаза, утыкается тёплым носом в твою ладонь и тихонько урчит, обвив руку пушистым хвостом."
            } else {
                "От твоего прикосновения лисёнок доверчиво переворачивается на спинку у самого камина, подставляя пушистый животик."
            }
        }
    }
}

enum class ChantalLootCategory {
    TRINKET,
    HERB,
    ROYAL_CLUE,
    DEN_TROPHY
}

data class ChantalInventoryLootItem(
    val id: String,
    val titleRu: String,
    val shortTitleRu: String = titleRu.substringBefore(" "),
    val emoji: String = "🔮",
    val category: ChantalLootCategory,
    val descriptionRu: String,
    val sellPriceCoins: Int,
    val foundAtRegionRu: String,
    val foundDateIso: String,
    val inspectedBonusClaimed: Boolean = false
)

enum class ChantalShopCategory {
    APPAREL,
    DEN_COMFORT,
    CONSUMABLE
}

enum class ChantalLoadoutSlotType {
    OUTFIT,
    TOOL,
    WEAPON,
    BUNDLE
}

data class ChantalShopItem(
    val id: String,
    val titleRu: String,
    val shortTitleRu: String,
    val emoji: String,
    val category: ChantalShopCategory,
    val loadoutSlotType: ChantalLoadoutSlotType? = null,
    val denStep: Int = 1,
    val priceCoins: Int,
    val upgradeCostCoins: Int = 35,
    val maxTier: Int = 3,
    val maxDurability: Int = 5,
    val refillCostCoins: Int = 10,
    val preventsMishapBonusPct: Int = 0,
    val expeditionCoinMultiplierPct: Int = 0,
    val denTrophyChanceBonusPct: Int = 0,
    val visualSlotLabelRu: String,
    val descriptionRu: String,
    val gameplayEffectRu: String,
    val reducesExhaustionCostBy: Int = 0
)

data class ChantalMerchantOffer(
    val id: String,
    val titleRu: String,
    val shortTitleRu: String,
    val emoji: String,
    val originalPriceCoins: Int,
    val discountPriceCoins: Int,
    val rewardType: String,
    val effectRu: String
)

val DEFAULT_CHANTAL_MERCHANT_OFFERS = listOf(
    ChantalMerchantOffer(
        id = "merch_den_lantern_crate",
        titleRu = "Резной фонарик со светлячком в Домик",
        shortTitleRu = "Ночник в Дом",
        emoji = "🏮",
        originalPriceCoins = 75,
        discountPriceCoins = 48,
        rewardType = "DEN_TROPHY",
        effectRu = "Сразу ставится в Домик и прибавляет +1 к прогрессу уровня интерьера (X из 10)!"
    ),
    ChantalMerchantOffer(
        id = "merch_clue_map",
        titleRu = "Карта потайных звериных троп (-35%)",
        shortTitleRu = "Карта троп",
        emoji = "🗺️",
        originalPriceCoins = 80,
        discountPriceCoins = 52,
        rewardType = "ROYAL_CLUE",
        effectRu = "Открывает Королевскую улику в шкатулку и даёт +2 к Любопытству ↑↑"
    ),
    ChantalMerchantOffer(
        id = "merch_amulet",
        titleRu = "Янтарный оберег Попутного Ветра",
        shortTitleRu = "Оберег ветра",
        emoji = "🧿",
        originalPriceCoins = 55,
        discountPriceCoins = 34,
        rewardType = "ROAD_AMULET",
        effectRu = "100% защита от неудач и +50% монет в следующем походе"
    )
)

val DEFAULT_CHANTAL_SHOP_ITEMS = listOf(
    // --- СНАРЯЖЕНИЕ: ОДЕЖДА (OUTFIT) ---
    ChantalShopItem(
        id = "gear_knitted_scarf",
        titleRu = "Тёплый вязаный шарф",
        shortTitleRu = "Шарф",
        emoji = "🧣",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.OUTFIT,
        priceCoins = 38,
        upgradeCostCoins = 28,
        preventsMishapBonusPct = 35,
        visualSlotLabelRu = "Одежда: Шерстяной шарф",
        descriptionRu = "Мягкий шарф ручной вязки, пахнущий лавандой. Защищает от ветра и сырости.",
        gameplayEffectRu = "🛡️ +35% защита от непогоды и усталости"
    ),
    ChantalShopItem(
        id = "gear_leather_jacket",
        titleRu = "Походная курточка следопыта",
        shortTitleRu = "Курточка",
        emoji = "🧥",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.OUTFIT,
        priceCoins = 46,
        upgradeCostCoins = 32,
        preventsMishapBonusPct = 25,
        expeditionCoinMultiplierPct = 20,
        visualSlotLabelRu = "Одежда: Кожаная курточка",
        descriptionRu = "Удобная замшевая курточка с медными застёжками и потайными карманами.",
        gameplayEffectRu = "🪙 +20% монет · 🛡️ +25% защита"
    ),
    ChantalShopItem(
        id = "gear_emerald_cloak",
        titleRu = "Изумрудный плащ от непогоды",
        shortTitleRu = "Плащ",
        emoji = "🦸",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.OUTFIT,
        priceCoins = 85,
        upgradeCostCoins = 50,
        preventsMishapBonusPct = 55,
        expeditionCoinMultiplierPct = 25,
        visualSlotLabelRu = "Одежда: Изумрудный плащ",
        descriptionRu = "Плотный дорожный плащ с золотистой фибулой, не промокающий под ливнем.",
        gameplayEffectRu = "🛡️ +55% защита · 🪙 +25% монет"
    ),

    // --- СНАРЯЖЕНИЕ: ИНСТРУМЕНТЫ (TOOL) ---
    ChantalShopItem(
        id = "gear_brass_lantern",
        titleRu = "Медный путевой фонарь",
        shortTitleRu = "Фонарь",
        emoji = "🏮",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.TOOL,
        priceCoins = 54,
        upgradeCostCoins = 35,
        preventsMishapBonusPct = 30,
        expeditionCoinMultiplierPct = 15,
        denTrophyChanceBonusPct = 25,
        visualSlotLabelRu = "Инструмент: Медный фонарь",
        descriptionRu = "Старинный фонарь гномьей работы, высвечивающий тайники в дуплах и руинах.",
        gameplayEffectRu = "🏠 +25% шанс трофея в Дом · 🛡️ +30% защита"
    ),
    ChantalShopItem(
        id = "gear_star_compass",
        titleRu = "Латунный компас тайных троп",
        shortTitleRu = "Компас",
        emoji = "🧭",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.TOOL,
        priceCoins = 72,
        upgradeCostCoins = 45,
        preventsMishapBonusPct = 25,
        expeditionCoinMultiplierPct = 35,
        denTrophyChanceBonusPct = 30,
        visualSlotLabelRu = "Инструмент: Звёздный компас",
        descriptionRu = "Стрелка этого компаса указывает на забытые схроны и редкие диковинки.",
        gameplayEffectRu = "🪙 +35% монет · 🏠 +30% трофей в Дом"
    ),

    // --- СНАРЯЖЕНИЕ: ОРУЖИЕ И ЗАЩИТА (WEAPON) ---
    ChantalShopItem(
        id = "weapon_sleep_apple",
        titleRu = "Сонное яблоко алхимика",
        shortTitleRu = "Яблоко",
        emoji = "🍎",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.WEAPON,
        priceCoins = 42,
        upgradeCostCoins = 28,
        preventsMishapBonusPct = 40,
        expeditionCoinMultiplierPct = 15,
        visualSlotLabelRu = "Оружие: Сонное яблоко",
        descriptionRu = "Ароматное яблочко в сонном настое — усыпляет сердитых сторожевых гусей и барсуков.",
        gameplayEffectRu = "🛡️ +40% защита от штрафов · 🪙 +15% монет"
    ),
    ChantalShopItem(
        id = "weapon_thorn_dart",
        titleRu = "Терновый дротик следопыта",
        shortTitleRu = "Дротик",
        emoji = "🏹",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.WEAPON,
        priceCoins = 56,
        upgradeCostCoins = 36,
        preventsMishapBonusPct = 45,
        expeditionCoinMultiplierPct = 25,
        denTrophyChanceBonusPct = 15,
        visualSlotLabelRu = "Оружие: Терновый дротик",
        descriptionRu = "Лёгкий духовой дротик из шипа боярышника — сбивает замки и отпугивает ворон.",
        gameplayEffectRu = "🛡️ +45% защита · 🪙 +25% монет"
    ),
    ChantalShopItem(
        id = "weapon_silver_sling",
        titleRu = "Серебряная праща с желудями",
        shortTitleRu = "Праща",
        emoji = "🪃",
        category = ChantalShopCategory.APPAREL,
        loadoutSlotType = ChantalLoadoutSlotType.WEAPON,
        priceCoins = 64,
        upgradeCostCoins = 40,
        preventsMishapBonusPct = 50,
        expeditionCoinMultiplierPct = 20,
        denTrophyChanceBonusPct = 20,
        visualSlotLabelRu = "Оружие: Серебряная праща",
        descriptionRu = "Точная ременная праща: сбивает высоко висящие ларцы и звонкие шишки.",
        gameplayEffectRu = "🛡️ +50% защита · 🏠 +20% трофей"
    ),

    // --- СНАРЯЖЕНИЕ: ГОСТИНЦЫ В УЗЕЛОК (CONSUMABLE / BUNDLE) ---
    ChantalShopItem(
        id = "supply_honey_pie",
        titleRu = "Ежевичный пирожок в дорогу",
        shortTitleRu = "Пирожок",
        emoji = "🥧",
        category = ChantalShopCategory.CONSUMABLE,
        loadoutSlotType = ChantalLoadoutSlotType.BUNDLE,
        priceCoins = 16,
        expeditionCoinMultiplierPct = 15,
        visualSlotLabelRu = "В узелок: Тёплый пирожок",
        descriptionRu = "Душистая выпечка: даёт 100% защиту от усталости (45 Эн.) в пути.",
        gameplayEffectRu = "⚡ Защита от усталости · 🪙 +15% монет"
    ),
    ChantalShopItem(
        id = "supply_herbal_balm",
        titleRu = "Тёплый медово-травяной бальзам",
        shortTitleRu = "Бальзам",
        emoji = "🍯",
        category = ChantalShopCategory.CONSUMABLE,
        loadoutSlotType = ChantalLoadoutSlotType.BUNDLE,
        priceCoins = 18,
        visualSlotLabelRu = "Дома или в узелок: Бальзам",
        descriptionRu = "Мгновенно снимает дорожную усталость (возвращает цену похода к 30 Энергии).",
        gameplayEffectRu = "🌿 Снимает усталость (30 Эн.) и +15% настр."
    ),
    ChantalShopItem(
        id = "supply_shiny_buttons",
        titleRu = "Мешочек блестящих пуговиц для обмена",
        shortTitleRu = "Пуговицы",
        emoji = "🪙",
        category = ChantalShopCategory.CONSUMABLE,
        loadoutSlotType = ChantalLoadoutSlotType.BUNDLE,
        priceCoins = 22,
        expeditionCoinMultiplierPct = 25,
        denTrophyChanceBonusPct = 35,
        visualSlotLabelRu = "В узелок: Набор для обмена",
        descriptionRu = "Вороны и белки-менялы обожают эти пуговицы и отдают взамен вещи для Домика!",
        gameplayEffectRu = "🏠 +35% шанс трофея в Дом · 🪙 +25% монет"
    ),

    // --- ИНТЕРЬЕР ДОМИКА: СТУПЕНЬ 1 (10 предметов -> Уровень II) ---
    ChantalShopItem("den_moss_bedding", "Мягкая подстилка из лесного мха", "Подстилка", "🌿", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 32, visualSlotLabelRu = "У очага: Мшистая лежанка", descriptionRu = "Пушистая лежанка из сухого мха и папоротника.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -2 Эн. усталости", reducesExhaustionCostBy = 2),
    ChantalShopItem("den_clay_bowl", "Глиняная мисочка с росписью", "Мисочка", "🥣", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 35, visualSlotLabelRu = "У столика: Мисочка Торвина", descriptionRu = "Обливная мисочка для тёплого молока и ягод.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +1 М/поход"),
    ChantalShopItem("den_heather_blanket", "Плед из овечьей шерсти", "Плед", "🧶", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 42, visualSlotLabelRu = "В уголке: Клетчатый плед", descriptionRu = "Тёплое гнёздышко у самого камина.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -3 Эн. усталости", reducesExhaustionCostBy = 3),
    ChantalShopItem("den_pine_hooks", "Вешалка из сосновых сучков", "Вешалка", "🪝", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 45, visualSlotLabelRu = "У двери: Крючки для плаща", descriptionRu = "Удобная вешалка у порога для дорожной сумки и шарфа.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +1 М/поход"),
    ChantalShopItem("den_acorn_garland", "Гирлянда из сушёных жёлудей", "Гирлянда", "🌰", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 48, visualSlotLabelRu = "Под балкой: Лесная гирлянда", descriptionRu = "Уютно шуршит от тёплого воздуха камина.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +1 М/поход"),
    ChantalShopItem("den_wick_candle", "Восковая свеча в плошке", "Свеча", "🕯️", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 52, visualSlotLabelRu = "На окне: Медовая свеча", descriptionRu = "Пахнет гречишным мёдом и указывает путь домой.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +2 М/поход"),
    ChantalShopItem("den_woven_rug", "Плетёный коврик из камыша", "Коврик", "🪺", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 56, visualSlotLabelRu = "На полу: Тёплый коврик", descriptionRu = "Защищает лапки от утренней прохлады.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -2 Эн. усталости", reducesExhaustionCostBy = 2),
    ChantalShopItem("den_birch_box", "Берестяной туесок для ягод", "Туесок", "🧺", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 60, visualSlotLabelRu = "В кладовой: Резной туесок", descriptionRu = "Хранит лесную малину и орехи свежими.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +2 М/поход"),
    ChantalShopItem("den_wooden_stool", "Резная дубовая скамеечка", "Скамейка", "🪑", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 65, visualSlotLabelRu = "У огня: Низкая скамеечка", descriptionRu = "Удобно сидеть рядом с Шанталь и слушать её истории.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +2 М/поход"),
    ChantalShopItem("den_hearth_kettle", "Медный чайник со свистком", "Чайник", "🫖", ChantalShopCategory.DEN_COMFORT, denStep = 1, priceCoins = 72, visualSlotLabelRu = "Над огнём: Пузатый чайник", descriptionRu = "Всегда готов согреть чабрецовый чай после похода.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -3 Эн. усталости", reducesExhaustionCostBy = 3),

    // --- ИНТЕРЬЕР ДОМИКА: СТУПЕНЬ 2 (10 предметов -> Уровень III) ---
    ChantalShopItem("den_curiosity_shelf", "Дубовая полка для диковинок", "Полка", "📚", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 55, visualSlotLabelRu = "На стене: Полка трофеев", descriptionRu = "Фигурная полка для светящихся шишек и находок.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +3 М/поход"),
    ChantalShopItem("den_herb_rack", "Сушилка для душистых трав", "Сушилка", "🌿", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 58, visualSlotLabelRu = "Под потолком: Связки трав", descriptionRu = "Наполняет нору целебным ароматом чабреца и мяты.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -2 Эн. усталости", reducesExhaustionCostBy = 2),
    ChantalShopItem("den_patchwork_quilt", "Лоскутное одеяло мельничихи", "Одеяло", "🛏️", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 62, visualSlotLabelRu = "На лежанке: Яркое одеяло", descriptionRu = "Сшито из самых тёплых и мягких лоскутков.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +2 М/поход"),
    ChantalShopItem("den_wall_sconce", "Кованый настенный бра-фонарь", "Бра", "🏮", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 66, visualSlotLabelRu = "У входа: Янтарный светильник", descriptionRu = "Озаряет комнату мягким золотистым светом.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +2 М/поход"),
    ChantalShopItem("den_stone_hearth", "Каменный очаг с котелком", "Очаг", "🔥", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 74, visualSlotLabelRu = "В центре: Каменный камелёк", descriptionRu = "Настоящее сердце лесной норы с живым огнём.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -4 Эн. усталости", reducesExhaustionCostBy = 4),
    ChantalShopItem("den_map_stand", "Пюпитр для дорожных карт", "Пюпитр", "🗺️", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 78, visualSlotLabelRu = "У окна: Стойка с картами", descriptionRu = "Помогает Шанталь планировать новые маршруты.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +3 М/поход"),
    ChantalShopItem("den_feather_pillow", "Пуховая подушка с вышивкой", "Подушка", "🛋️", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 82, visualSlotLabelRu = "У камина: Мягкая подушка", descriptionRu = "Лисий сон на ней особенно сладкий и спокойный.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +3 М/поход"),
    ChantalShopItem("den_spice_cabinet", "Шкафчик со склянками и мёдом", "Шкафчик", "🍯", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 86, visualSlotLabelRu = "В углу: Запасы Брамбла", descriptionRu = "Баночки с вареньем, сушёной черникой и нектаром.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +3 М/поход"),
    ChantalShopItem("den_oak_table", "Круглый дубовый столик", "Столик", "🪵", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 92, visualSlotLabelRu = "В центре: Резной стол", descriptionRu = "На нём так удобно раскладывать принесённые находки.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +3 М/поход"),
    ChantalShopItem("den_velvet_curtains", "Тяжёлые бархатные портьеры", "Портьеры", "🪟", ChantalShopCategory.DEN_COMFORT, denStep = 2, priceCoins = 98, visualSlotLabelRu = "На окне: Изумрудные шторы", descriptionRu = "Берегут тепло даже в самую лютую горную бурю.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -3 Эн. усталости", reducesExhaustionCostBy = 3),

    // --- ИНТЕРЬЕР ДОМИКА: СТУПЕНЬ 3 (10 предметов -> Уровень IV) ---
    ChantalShopItem("den_alchemy_table", "Дубовый алхимический стол", "Алхимия", "⚗️", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 75, visualSlotLabelRu = "Мастерская: Стол алхимии", descriptionRu = "Позволяет варить усиленные обереги Попутного Ветра.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +4 М/поход"),
    ChantalShopItem("den_crystal_lantern", "Хрустальный фонарь чародеев", "Кристалл", "💎", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 80, visualSlotLabelRu = "Под сводом: Сияющий кристалл", descriptionRu = "Горит чистым звёздным светом со Звёздного перевала.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +4 М/поход"),
    ChantalShopItem("den_chronicle_bookcase", "Книжный шкаф древних хроник", "Библиотека", "📖", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 85, visualSlotLabelRu = "У стены: Собрание свитков", descriptionRu = "Хранит все легенды и разгаданные тайны королевства.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +4 М/поход"),
    ChantalShopItem("den_royal_tapestry", "Гобелен с королевскими лилиями", "Гобелен", "🖼️", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 90, visualSlotLabelRu = "На стене: Старинный гобелен", descriptionRu = "Пробуждает в душе Шанталь воспоминания о родном замке.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +4 М/поход"),
    ChantalShopItem("den_plush_armchair", "Бархатное кресло у камина", "Кресло", "🛋️", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 96, visualSlotLabelRu = "У очага: Глубокое кресло", descriptionRu = "Любимое место отдыха после дальних странствий.", gameplayEffectRu = "+1 из 10 к Уровню Домика · -4 Эн. усталости", reducesExhaustionCostBy = 4),
    ChantalShopItem("den_music_box", "Заводная шкатулка менестреля", "Шкатулка", "🎶", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 102, visualSlotLabelRu = "На столике: Музыкальная ларец", descriptionRu = "Играет нежную колыбельную Серебряного Шпиля.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +4 М/поход"),
    ChantalShopItem("den_stained_glass", "Витражное окно «Звёздная лилия»", "Витраж", "🌈", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 110, visualSlotLabelRu = "Свод: Цветной витраж", descriptionRu = "Преломляет лучи солнца в сказочную радугу.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +5 М/поход"),
    ChantalShopItem("den_treasure_coffer", "Кованый ларец для реликвий", "Ларец", "🧰", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 118, visualSlotLabelRu = "В нише: Сокровищница", descriptionRu = "Надёжно бережёт королевские печати и медальоны.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +5 М/поход"),
    ChantalShopItem("den_celestial_globe", "Позолоченный небесный глобус", "Глобус", "🌐", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 126, visualSlotLabelRu = "У окна: Сфера созвездий", descriptionRu = "Показывает движение светил над всем королевством.", gameplayEffectRu = "+1 из 10 к Уровню Домика · +5 М/поход"),
    ChantalShopItem("den_star_astrolabe", "Большой телескоп Звездочёта", "Телескоп", "🔭", ChantalShopCategory.DEN_COMFORT, denStep = 3, priceCoins = 135, visualSlotLabelRu = "Под куполом: Звёздный телескоп", descriptionRu = "Вершина обустройства Сказочного терема (Уровень IV).", gameplayEffectRu = "+1 из 10 к Уровню Домика · +6 М/поход")
)

data class ChantalMapRegion(
    val id: String,
    val titleRu: String,
    val shortTitleRu: String,
    val emoji: String,
    val minJourneysRequired: Int,
    val loreRu: String,
    val npcsRu: List<String>,
    val unfinishedThreadsRu: List<String>
)

val DEFAULT_CHANTAL_MAP_REGIONS = listOf(
    ChantalMapRegion(
        id = "reg_old_mill",
        titleRu = "Старая водяная мельница",
        shortTitleRu = "Мельница",
        emoji = "⚙️",
        minJourneysRequired = 0,
        loreRu = "Шумное водяное колесо, мучная пыль в солнечных лучах и ярмарочные телеги у моста.",
        npcsRu = listOf("Барсук-травник Брамбл", "Мельничиха Марта", "Старый перевозчик Хьюго"),
        unfinishedThreadsRu = listOf(
            "разобрать старый сундук на чердаке мельницы",
            "помочь травнику Брамблу собрать редкие коренья",
            "расспросить паромщика Хьюго о ночных путниках с гербом лилии"
        )
    ),
    ChantalMapRegion(
        id = "reg_amber_forest",
        titleRu = "Янтарная дубрава",
        shortTitleRu = "Дубрава",
        emoji = "🍂",
        minJourneysRequired = 0,
        loreRu = "Тёплый золотистый лес с вековыми дубами, грибными полянами и старыми указателями.",
        npcsRu = listOf("Ворон-почтальон Корвус", "Лесничий Олеандр", "Белка-меняла Тилли"),
        unfinishedThreadsRu = listOf(
            "дочитать оборванное письмо с сургучной печатью у ворона Корвуса",
            "проверить тайник под корнями Царь-Дуба в сумерках"
        )
    ),
    ChantalMapRegion(
        id = "reg_misty_hollow",
        titleRu = "Туманная лощина",
        shortTitleRu = "Лощина",
        emoji = "🌫️",
        minJourneysRequired = 5,
        loreRu = "Сумрачное ущелье со светящимся мхом, старым каменным мостом и эхом забытых песен.",
        npcsRu = listOf("Гном-гончар Торвин", "Фонарщица Эльза", "Старый рудокоп Балин"),
        unfinishedThreadsRu = listOf(
            "отыскать вторую половину глиняной таблички в мастерской Торвина",
            "пройти с фонарщицей Эльзой по дальней арке моста"
        )
    ),
    ChantalMapRegion(
        id = "reg_stone_halls",
        titleRu = "Древние Каменные Чертоги",
        shortTitleRu = "Чертоги",
        emoji = "🏛️",
        minJourneysRequired = 15,
        loreRu = "Заросшие плющом руины королевской заставы, где на камнях высечены лилии.",
        npcsRu = listOf("Менестрель Лютик-младший", "Архивариус Себастьян", "Капитан стражи Родерик"),
        unfinishedThreadsRu = listOf(
            "собрать недостающие куплеты баллады о пропавшей принцессе",
            "открыть закрытый читальный зал архивариуса Себастьяна"
        )
    ),
    ChantalMapRegion(
        id = "reg_mountain_pass",
        titleRu = "Звёздный перевал чародеев",
        shortTitleRu = "Перевал",
        emoji = "🏔️",
        minJourneysRequired = 40,
        loreRu = "Высокогорные тропы, где воздух звенит от древней магии и видна столица.",
        npcsRu = listOf("Хранительница обсерватории Ирэн", "Алхимик Калькештейн", "Гонец Серебряного Шпиля"),
        unfinishedThreadsRu = listOf(
            "дождаться схождения созвездий в телескопе Ирэн для чтения тайной печати",
            "проверить реакцию древнего кристалла на прикосновение Шанталь"
        )
    )
)

data class ChantalChoiceOption(
    val labelRu: String,
    val reactionRu: String,
    val delta: ChantalTraits
)

data class ChantalStoryEpisode(
    val id: String,
    val journeyNumber: Int = 4,
    val titleRu: String,
    val storyTextRu: String,
    val mapNoteRu: String,
    val regionTitleRu: String,
    val npcNameRu: String,
    val stayedInSameRegion: Boolean,
    val isFiller: Boolean,
    val isMilestoneChest: Boolean = false,
    val lootItem: ChantalInventoryLootItem,
    val coinsEarned: Int,
    val causesExhaustion45: Boolean,
    val debtAmountCoins: Int,
    val badNewsRu: String,
    val choices: List<ChantalChoiceOption>,
    val chosenActionSummaryRu: String? = null,
    val memorySummaryRu: String? = null,
    val narrativeConsequences: String? = null,
    val chosenOptionIndex: Int? = null,
    val narrativeThemeRu: String? = null,
    val trailNotesRu: List<String> = emptyList(),
    val personalDetailsRu: String = ""
)

data class ChantalRewardPopupData(
    val episodeTitleRu: String,
    val journeyNumber: Int,
    val regionTitleRu: String,
    val playerChoiceRu: String,
    val chantalReactionRu: String,
    val coinsEarned: Int,
    val xpEarned: Int,
    val lootItem: ChantalInventoryLootItem,
    val traitArrowsText: String,
    val isMilestoneChest: Boolean
)

data class ChantalStageStatus(
    val stageNumber: Int,
    val isTransformed: Boolean,
    val titleRu: String,
    val subtitleRu: String,
    val currentStageStartJourneys: Int,
    val nextStageTargetJourneys: Int,
    val nextMilestoneHintRu: String
)

data class ChantalMaturityProgress(
    val maturityXp: Int,
    val maturityLevel: Int,
    val xpInStage: Int,
    val stageTargetXp: Int,
    val progressPct: Int,
    val xpToNextStage: Int
)

@Suppress("UNUSED_PARAMETER") // Legacy XP parameter is retained for call-site compatibility, never for age.
fun resolveChantalStageStatus(
    totalJourneys: Int,
    forceTransformPreview: Boolean = false,
    maturityXp: Int = totalJourneys * 10
): ChantalStageStatus {
    val effectiveJourneys = totalJourneys.coerceAtLeast(0)
    val isTransformed = forceTransformPreview || effectiveJourneys >= 100
    if (isTransformed) {
        val masteryRank = ((effectiveJourneys - 100).coerceAtLeast(0) / 50) + 1
        val startJ = 100 + (masteryRank - 1) * 50
        val nextTarget = 100 + masteryRank * 50
        return ChantalStageStatus(
            stageNumber = 4,
            isTransformed = true,
            titleRu = "Шанталь · Расколдованная принцесса (Ранг $masteryRank)",
            subtitleRu = "Проклятие пало — впереди большая магическая сага в мире королевств и древних чар",
            currentStageStartJourneys = startJ,
            nextStageTargetJourneys = nextTarget,
            nextMilestoneHintRu = "До следующей главы мастерства: ${(nextTarget - effectiveJourneys).coerceAtLeast(1)} поход."
        )
    }
    return when {
        effectiveJourneys < 10 -> ChantalStageStatus(
            stageNumber = 1,
            isTransformed = false,
            titleRu = "Стадия I · Спасённый лисёнок",
            subtitleRu = "Кроха в тёплом пледе: только осваивается в доме и делает первые шаги за порог",
            currentStageStartJourneys = 0,
            nextStageTargetJourneys = 10,
            nextMilestoneHintRu = "До следующей вехи взросления: ${(10 - effectiveJourneys).coerceAtLeast(1)} из 10 походов"
        )
        effectiveJourneys < 50 -> ChantalStageStatus(
            stageNumber = 2,
            isTransformed = false,
            titleRu = "Стадия II · Любопытный лисёнок",
            subtitleRu = "Окрепла, носит маленькую кожаную сумку через плечо и смело исследует тропы",
            currentStageStartJourneys = 10,
            nextStageTargetJourneys = 50,
            nextMilestoneHintRu = "Прогресс стадии: ${effectiveJourneys - 10} из 40 походов (всего $effectiveJourneys/50)"
        )
        else -> ChantalStageStatus(
            stageNumber = 3,
            isTransformed = false,
            titleRu = "Стадия III · Юная путешественница",
            subtitleRu = "В дорожном плаще с медным фонарём: уходит на дальние перевалы навстречу тайнам",
            currentStageStartJourneys = 50,
            nextStageTargetJourneys = 100,
            nextMilestoneHintRu = "Прогресс стадии: ${effectiveJourneys - 50} из 50 походов (всего $effectiveJourneys/100)"
        )
    }
}

fun computeChantalMaturityProgress(
    maturityXp: Int,
    stageStatus: ChantalStageStatus
): ChantalMaturityProgress {
    val startXp = stageStatus.currentStageStartJourneys * 10
    val endXp = stageStatus.nextStageTargetJourneys * 10
    val targetXp = (endXp - startXp).coerceAtLeast(10)
    val inStage = (maturityXp - startXp).coerceIn(0, targetXp)
    val pct = ((inStage * 100) / targetXp).coerceIn(3, 100)
    val toNext = (endXp - maturityXp).coerceAtLeast(0)
    val level = (maturityXp / 20) + 1
    return ChantalMaturityProgress(
        maturityXp = maturityXp,
        maturityLevel = level,
        xpInStage = inStage,
        stageTargetXp = targetXp,
        progressPct = pct,
        xpToNextStage = toNext
    )
}
