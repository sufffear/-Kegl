package com.example.domain

/**
 * Доменная модель системы персонажа «Шанталь» для Android (Jetpack Compose):
 * - Двухактная эволюция (Акт I: лисёнок в стиле «Хоббита», стадии 1..3 -> Акт II: скрытая трансформация в расколдованную принцессу на 100+ походах).
 * - 6 параметров характера с активными геймплейными бонусами: Любопытство, Нежность, Игривость, Романтичность, Отвага, Остроумие.
 * - Интерактивное поглаживание («Погладить за ушком / Поговорить у камина») с живыми репликами от ведущей черты.
 * - Умный автономный выбор локации на карте (с возможностью возвращаться в одну и ту же локацию подряд ради незаконченных дел и разных NPC).
 * - Трёхсекционный инвентарь (Шкатулка королевских улик без риска случайной продажи, Целебные травы с кнопкой применения, Диковинки с бонусом Остроумия).
 * - Память последних 3 походов из Дневника для сериальной преемственности сюжета.
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
    ROYAL_CLUE
}

data class ChantalInventoryLootItem(
    val id: String,
    val titleRu: String,
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

data class ChantalShopItem(
    val id: String,
    val titleRu: String,
    val category: ChantalShopCategory,
    val priceCoins: Int,
    val upgradeCostCoins: Int = 35,
    val maxTier: Int = 3,
    val visualSlotLabelRu: String,
    val descriptionRu: String,
    val gameplayEffectRu: String,
    val reducesExhaustionCostBy: Int = 0
)

data class ChantalMerchantOffer(
    val id: String,
    val titleRu: String,
    val discountPriceCoins: Int,
    val effectRu: String
)

val DEFAULT_CHANTAL_MERCHANT_OFFERS = listOf(
    ChantalMerchantOffer(
        id = "merch_clue_map",
        titleRu = "Карта потайных звериных троп (-35%)",
        discountPriceCoins = 55,
        effectRu = "Открывает Королевскую улику в шкатулку и даёт +2 к Любопытству ↑↑"
    ),
    ChantalMerchantOffer(
        id = "merch_amulet",
        titleRu = "Янтарный оберег Попутного Ветра",
        discountPriceCoins = 38,
        effectRu = "100% защита от неудач и +50% монет в следующем походе"
    ),
    ChantalMerchantOffer(
        id = "merch_herbs",
        titleRu = "Связка редких трав со Звёздного перевала",
        discountPriceCoins = 28,
        effectRu = "Добавляет целебные травы в Сумку для лечения или крафта"
    )
)

val DEFAULT_CHANTAL_SHOP_ITEMS = listOf(
    ChantalShopItem(
        id = "gear_knitted_scarf",
        titleRu = "Тёплый вязаный шарф",
        category = ChantalShopCategory.APPAREL,
        priceCoins = 35,
        upgradeCostCoins = 30,
        visualSlotLabelRu = "На шее: Шерстяной шарф цвета вереска",
        descriptionRu = "Мягкий шарф ручной вязки, пахнущий сушёной лавандой и домашним теплом.",
        gameplayEffectRu = "Защищает от холодного ветра · снижает риск непогоды (+4 монеты за Тир)"
    ),
    ChantalShopItem(
        id = "gear_herb_satchel",
        titleRu = "Кожаная сумка следопыта",
        category = ChantalShopCategory.APPAREL,
        priceCoins = 60,
        upgradeCostCoins = 45,
        visualSlotLabelRu = "Через плечо: Тиснёная сумка с пряжкой",
        descriptionRu = "Лёгкая дорожная сумочка из мягкой кожи для трав и диковинок.",
        gameplayEffectRu = "+6 монет из каждого похода (+4 монеты за каждый Тир)"
    ),
    ChantalShopItem(
        id = "gear_brass_lantern",
        titleRu = "Медный путевой фонарь",
        category = ChantalShopCategory.APPAREL,
        priceCoins = 85,
        upgradeCostCoins = 55,
        visualSlotLabelRu = "В лапе: Негасимый медный фонарь",
        descriptionRu = "Старинный фонарь гномьей работы, освещающий туманные овраги.",
        gameplayEffectRu = "Освещает ночные тропы · снижает риск передряг на 25%"
    ),
    ChantalShopItem(
        id = "den_heather_blanket",
        titleRu = "Плед из овечьей шерсти",
        category = ChantalShopCategory.DEN_COMFORT,
        priceCoins = 40,
        upgradeCostCoins = 30,
        visualSlotLabelRu = "В уголке: Мягкий клетчатый плед",
        descriptionRu = "Тёплое гнёздышко у самого камина (Уровень Домика I).",
        gameplayEffectRu = "Снижает штраф восстановления после тяжёлого похода на 5 Энергии",
        reducesExhaustionCostBy = 5
    ),
    ChantalShopItem(
        id = "den_curiosity_shelf",
        titleRu = "Дубовая полка для диковинок",
        category = ChantalShopCategory.DEN_COMFORT,
        priceCoins = 65,
        upgradeCostCoins = 45,
        visualSlotLabelRu = "На стене: Резная полка с трофеями",
        descriptionRu = "Фигурная полка из дуба для светящихся шишек (открывает иллюстрацию Домика II).",
        gameplayEffectRu = "+5 бонусных монет за каждый поход (+4 за Тир)"
    ),
    ChantalShopItem(
        id = "den_stone_hearth",
        titleRu = "Каменный очаг с медным котелком",
        category = ChantalShopCategory.DEN_COMFORT,
        priceCoins = 95,
        upgradeCostCoins = 55,
        visualSlotLabelRu = "В центре дома: Живой тёплый камелёк",
        descriptionRu = "Потрескивающие дрова и связки трав над камином (интерьер Домика II).",
        gameplayEffectRu = "Снижает штраф усталости ещё на 5 Энергии",
        reducesExhaustionCostBy = 5
    ),
    ChantalShopItem(
        id = "den_alchemy_table",
        titleRu = "Дубовый алхимический стол",
        category = ChantalShopCategory.DEN_COMFORT,
        priceCoins = 115,
        upgradeCostCoins = 65,
        visualSlotLabelRu = "У окна: Резной стол алхимии и оберегов",
        descriptionRu = "Мастерская со склянками и оберегами (открывает иллюстрацию Домика III).",
        gameplayEffectRu = "Усиливает домашний крафт оберегов и снижает усталость на 5 Энергии",
        reducesExhaustionCostBy = 5
    ),
    ChantalShopItem(
        id = "den_star_astrolabe",
        titleRu = "Латунная звёздная астролябия",
        category = ChantalShopCategory.DEN_COMFORT,
        priceCoins = 135,
        upgradeCostCoins = 75,
        visualSlotLabelRu = "У сводчатого окна: Королевская астролябия",
        descriptionRu = "Астрономический прибор у стрельчатого окна (вершина интерьера Домика III).",
        gameplayEffectRu = "+8 монет за поход и повышенный шанс найти Королевскую улику"
    ),
    ChantalShopItem(
        id = "supply_herbal_balm",
        titleRu = "Тёплый медово-травяной бальзам",
        category = ChantalShopCategory.CONSUMABLE,
        priceCoins = 20,
        visualSlotLabelRu = "Сразу снимает усталость",
        descriptionRu = "Душистый отвар барсука Брамбла. Мгновенно возвращает бодрость.",
        gameplayEffectRu = "Мгновенно снимает штраф «45 Энергии» и возвращает цену 30 Энергии"
    )
)

data class ChantalMapRegion(
    val id: String,
    val titleRu: String,
    val minJourneysRequired: Int,
    val loreRu: String,
    val npcsRu: List<String>,
    val unfinishedThreadsRu: List<String>
)

val DEFAULT_CHANTAL_MAP_REGIONS = listOf(
    ChantalMapRegion(
        id = "reg_old_mill",
        titleRu = "Старая водяная мельница",
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
    val titleRu: String,
    val storyTextRu: String,
    val mapNoteRu: String,
    val regionTitleRu: String,
    val npcNameRu: String,
    val stayedInSameRegion: Boolean,
    val isFiller: Boolean,
    val lootItem: ChantalInventoryLootItem,
    val coinsEarned: Int,
    val causesExhaustion45: Boolean,
    val debtAmountCoins: Int,
    val badNewsRu: String,
    val choices: List<ChantalChoiceOption>,
    val chosenActionSummaryRu: String? = null
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
