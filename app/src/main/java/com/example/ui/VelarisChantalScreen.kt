package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.TimeProvider
import com.example.domain.SystemTimeProvider
import com.example.domain.ChantalJourneyClock
import com.example.R
import com.example.data.ChantalProgressCodec
import com.example.data.ChantalProgressRepository
import com.example.domain.ChantalAiProvider
import com.example.domain.ChantalChoiceOption
import com.example.domain.ChantalInventoryLootItem
import com.example.domain.ChantalLoadoutSlotType
import com.example.domain.ChantalLootCategory
import com.example.domain.ChantalRewardPopupData
import com.example.domain.ChantalShopCategory
import com.example.domain.ChantalStoryDifficulty
import com.example.domain.ChantalStoryEpisode
import com.example.domain.ChantalTraits
import com.example.domain.DEFAULT_CHANTAL_MAP_REGIONS
import com.example.domain.DEFAULT_CHANTAL_MERCHANT_OFFERS
import com.example.domain.DEFAULT_CHANTAL_SHOP_ITEMS
import com.example.domain.SignalType
import com.example.domain.computeChantalMaturityProgress
import com.example.domain.resolveChantalStageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

@Composable
fun VelarisChantalScreen(
    completedWorkoutsCount: Int,
    currentStreakDays: Int,
    onOpenWorkoutPlan: () -> Unit,
    onPlaySampleTone: (SignalType) -> Unit,
    modifier: Modifier = Modifier,
    earnedWorkoutEnergy: Int = completedWorkoutsCount * 10,
    timeProvider: TimeProvider = SystemTimeProvider()
) {
    val context = LocalContext.current
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    val progressRepository = remember(context.applicationContext) { ChantalProgressRepository(context) }
    val loadedProgress = remember(progressRepository) { runCatching { progressRepository.load() } }
    if (loadedProgress.isFailure) {
        Text(
            "Не удалось прочитать сохранение Шанталь. Данные сохранены без изменений. Перезапусти приложение; если ошибка повторяется, сообщи нам.",
            modifier = modifier
        )
        return
    }
    val saved = loadedProgress.getOrThrow()

    var hasCompletedPrologue by remember { mutableStateOf(saved.optBoolean("hasCompletedPrologue", false)) }
    var totalJourneys by remember { mutableIntStateOf(saved.optInt("totalJourneys", 0)) }
    var maturityXp by remember { mutableIntStateOf(saved.optInt("maturityXp", 0)) }
    var recentXpGainText by remember { mutableStateOf<String?>(null) }
    var forceTransformPreview by remember { mutableStateOf(saved.optBoolean("forceTransformPreview", false)) }
    var hasUsedPetThisCycle by remember { mutableStateOf(saved.optBoolean("hasUsedPetThisCycle", false)) }
    var isBurgerMenuOpen by remember { mutableStateOf(false) }

    // Подразделы в Домике («traits» / «housing» / «loadout» / «none»)
    var denAccordionTab by remember { mutableStateOf(saved.optString("denAccordionTab", "none")) }
    var selectedTraitPopupName by remember { mutableStateOf<String?>(null) }

    // 4 слота снаряжения в поход (OUTFIT, TOOL, WEAPON, BUNDLE)
    var activeLoadoutSlotPicker by remember { mutableStateOf(saved.optString("activeLoadoutSlotPicker", "NONE")) }
    var activeSlotInspectItemId by remember { mutableStateOf<String?>(null) }
    var equippedOutfitId by remember { mutableStateOf<String?>(if (saved.isNull("equippedOutfitId")) null else saved.getString("equippedOutfitId")) }
    var equippedToolId by remember { mutableStateOf<String?>(if (saved.isNull("equippedToolId")) null else saved.getString("equippedToolId")) }
    var equippedWeaponId by remember { mutableStateOf<String?>(if (saved.isNull("equippedWeaponId")) null else saved.getString("equippedWeaponId")) }
    var equippedBundleItemId by remember { mutableStateOf<String?>(if (saved.isNull("equippedBundleItemId")) null else saved.getString("equippedBundleItemId")) }

    val gearDurabilityMap = remember {
        mutableStateMapOf<String, Int>().apply { putAll(ChantalProgressCodec.counts(saved.optJSONObject("gearDurabilityMap"))) }
    }
    val pantryProvisionCounts = remember {
        mutableStateMapOf<String, Int>().apply { putAll(ChantalProgressCodec.counts(saved.optJSONObject("pantryProvisionCounts"))) }
    }

    var selectedBagItemId by remember { mutableStateOf<String?>(if (saved.isNull("selectedBagItemId")) null else saved.getString("selectedBagItemId")) }
    val newlyAddedBagItemIds = remember { mutableStateListOf<String>().apply { addAll(ChantalProgressCodec.strings(saved.optJSONArray("newlyAddedBagItemIds"))) } }
    var selectedShopEntryId by remember { mutableStateOf<String?>(null) }
    var shopCategoryTab by remember { mutableStateOf(saved.optString("shopCategoryTab", "den_comfort")) }
    var shopDenStepFilter by remember { mutableIntStateOf(saved.optInt("shopDenStepFilter", 1)) }
    var hasActiveRoadAmulet by remember { mutableStateOf(saved.optBoolean("hasActiveRoadAmulet", false)) }
    var rewardPopupData by remember { mutableStateOf<ChantalRewardPopupData?>(saved.optJSONObject("rewardPopupData")?.let { ChantalProgressCodec.decodeChantalRewardPopupData(it) }) }

    // Энергия и Монеты (каждая тренировка Кегеля даёт +10 Энергии)
    var bonusEnergy by remember { mutableIntStateOf(saved.optInt("bonusEnergy", 0)) }
    var spentEnergy by remember { mutableIntStateOf(saved.optInt("spentEnergy", 0)) }
    val currentEnergy = (earnedWorkoutEnergy + bonusEnergy - spentEnergy).coerceAtLeast(0)
    var coins by remember { mutableIntStateOf(saved.optInt("coins", 0)) }

    // 6 параметров характера (шкала 0..100, шаг +1/+2)
    var curiosity by remember { mutableIntStateOf(saved.optInt("curiosity", 5)) }
    var tenderness by remember { mutableIntStateOf(saved.optInt("tenderness", 6)) }
    var playfulness by remember { mutableIntStateOf(saved.optInt("playfulness", 4)) }
    var romanticism by remember { mutableIntStateOf(saved.optInt("romanticism", 3)) }
    var courage by remember { mutableIntStateOf(saved.optInt("courage", 4)) }
    var wit by remember { mutableIntStateOf(saved.optInt("wit", 5)) }

    val traits = ChantalTraits(
        curiosity = curiosity,
        tenderness = tenderness,
        playfulness = playfulness,
        romanticism = romanticism,
        courage = courage,
        wit = wit
    )
    val perks = remember(traits) { traits.computePerks() }
    val stageInfo = remember(totalJourneys, forceTransformPreview, maturityXp) {
        resolveChantalStageStatus(totalJourneys, forceTransformPreview, maturityXp)
    }
    val maturityProgress = remember(maturityXp, stageInfo) {
        computeChantalMaturityProgress(maturityXp, stageInfo)
    }

    LaunchedEffect(recentXpGainText) {
        if (recentXpGainText != null) {
            delay(4000L)
            recentXpGainText = null
        }
    }

    var moodPct by remember { mutableIntStateOf(saved.optInt("moodPct", 88)) }
    var isExhausted45 by remember { mutableStateOf(saved.optBoolean("isExhausted45", false)) }
    var debtCoins by remember { mutableIntStateOf(saved.optInt("debtCoins", 0)) }
    var debtReasonRu by remember { mutableStateOf(saved.optString("debtReasonRu", "")) }

    var showTraitArrows by remember { mutableStateOf(saved.optBoolean("showTraitArrows", true)) }
    var activeArrowToast by remember { mutableStateOf<String?>(null) }
    var lastReactionBanner by remember { mutableStateOf<String?>(null) }
    var petTapCount by remember { mutableIntStateOf(saved.optInt("petTapCount", 0)) }
    var petDialogueBubble by remember { mutableStateOf<String?>(null) }
    var isHeartPulseActive by remember { mutableStateOf(false) }

    LaunchedEffect(isHeartPulseActive) {
        if (isHeartPulseActive) {
            delay(1400L)
            isHeartPulseActive = false
        }
    }

    LaunchedEffect(lastReactionBanner) {
        if (lastReactionBanner != null) {
            delay(4500L)
            lastReactionBanner = null
        }
    }

    // Текущий раздел в Модальном Меню
    var subTab by remember { mutableStateOf(saved.optString("subTab", "den")) }

    // Активный часовой поход
    var activeJourneyStartMs by remember { mutableLongStateOf(saved.optLong("activeJourneyStartMs", 0L)) }
    var preferredRegionId by remember { mutableStateOf(saved.optString("preferredRegionId", "")) }
    var activeRegionId by remember { mutableStateOf(saved.optString("activeRegionId", "")) }
    var activeRegionTitleRu by remember { mutableStateOf(saved.optString("activeRegionTitleRu", "")) }
    var activeNpcRu by remember { mutableStateOf(saved.optString("activeNpcRu", "")) }
    var activeStayedInSameRegion by remember { mutableStateOf(saved.optBoolean("activeStayedInSameRegion", false)) }
    var activeAutonomousReasonRu by remember { mutableStateOf(saved.optString("activeAutonomousReasonRu", "")) }
    var activeIsFiller by remember { mutableStateOf(saved.optBoolean("activeIsFiller", false)) }
    var activeFillerPrompt by remember { mutableStateOf(saved.optString("activeFillerPrompt", "")) }

    // Непрочитанная история после возвращения
    var pendingEpisode by remember { mutableStateOf<ChantalStoryEpisode?>(saved.optJSONObject("pendingEpisode")?.let { ChantalProgressCodec.decodeChantalStoryEpisode(it) }) }
    val generationScope = androidx.compose.runtime.rememberCoroutineScope()
    val geminiAccess = remember(context) { com.example.data.ChantalGeminiAccess(context) }
    var generationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationMessage by remember { mutableStateOf("") }
    var fillerTextInput by remember { mutableStateOf(saved.optString("fillerTextInput", "")) }
    var customLoreNotes by remember { mutableStateOf(saved.optString("customLoreNotes", "")) }
    var selectedAiProvider by remember { mutableStateOf(saved.optString("selectedAiProvider", ChantalAiProvider.GEMINI.code)) }
    var selectedOpenRouterModel by remember { mutableStateOf(saved.optString("selectedOpenRouterModel", "google/gemini-2.5-flash")) }
    var customOpenRouterModelInput by remember { mutableStateOf(saved.optString("customOpenRouterModelInput", "")) }
    var selectedDifficulty by remember { mutableStateOf(saved.optString("selectedDifficulty", ChantalStoryDifficulty.MEDIUM.code)) }

    val ownedShopItemIds = remember { mutableStateListOf<String>().apply { addAll(ChantalProgressCodec.strings(saved.optJSONArray("ownedShopItemIds"))) } }
    val claimedMerchantOfferIds = remember { mutableStateListOf<String>().apply { addAll(ChantalProgressCodec.strings(saved.optJSONArray("claimedMerchantOfferIds"))) } }

    val inventoryList = remember {
        mutableStateListOf<ChantalInventoryLootItem>().apply {
            saved.optJSONArray("inventoryList")?.let { a ->
                repeat(a.length()) { add(ChantalProgressCodec.decodeChantalInventoryLootItem(a.getJSONObject(it))) }
            }
        }
    }

    val journalList = remember {
        mutableStateListOf<ChantalStoryEpisode>().apply {
            saved.optJSONArray("journalList")?.let { a ->
                repeat(a.length()) { add(ChantalProgressCodec.decodeChantalStoryEpisode(a.getJSONObject(it))) }
            }
        }
    }

    // Подсчёт предметов интерьера (10 предметов = новый уровень Домика!)
    val ownedDenCount = DEFAULT_CHANTAL_SHOP_ITEMS.count {
        it.category == ChantalShopCategory.DEN_COMFORT && it.id in ownedShopItemIds
    }
    val foundDenTrophyCount = inventoryList.count {
        it.category == ChantalLootCategory.DEN_TROPHY
    }
    val totalDenItemsCount = ownedDenCount + foundDenTrophyCount
    val denTierNumber = when {
        totalDenItemsCount >= 30 -> 4
        totalDenItemsCount >= 20 -> 3
        totalDenItemsCount >= 10 -> 2
        else -> 1
    }
    val itemsInCurrentStep = if (denTierNumber >= 4) 10 else (totalDenItemsCount % 10)
    val denInteriorResId = when (denTierNumber) {
        4 -> R.drawable.chantal_den_tier4
        3 -> R.drawable.chantal_den_tier3
        2 -> R.drawable.chantal_den_tier2
        else -> R.drawable.chantal_stage1
    }

    val comfortDiscount = DEFAULT_CHANTAL_SHOP_ITEMS
        .filter { it.id in ownedShopItemIds }
        .sumOf { it.reducesExhaustionCostBy }
    val requiredJourneyEnergy = if (isExhausted45 && denTierNumber < 4) {
        (45 - comfortDiscount).coerceAtLeast(30)
    } else 30

    val streakBonusPct = when {
        currentStreakDays >= 7 -> 50
        currentStreakDays >= 3 -> 25
        else -> 0
    }

    val portraitResId = when (stageInfo.stageNumber) {
        1 -> R.drawable.chantal_stage1
        2 -> R.drawable.chantal_stage2
        3 -> R.drawable.chantal_stage3
        else -> R.drawable.chantal_stage4
    }

    // Бонусы выбранного снаряжения в 4 слотах
    val activeGearList = listOfNotNull(
        DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedOutfitId && it.id in ownedShopItemIds && (gearDurabilityMap[it.id] ?: 5) > 0 },
        DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedToolId && it.id in ownedShopItemIds && (gearDurabilityMap[it.id] ?: 5) > 0 },
        DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedWeaponId && it.id in ownedShopItemIds && (gearDurabilityMap[it.id] ?: 5) > 0 }
    )
    val activeBundleShopItem = DEFAULT_CHANTAL_SHOP_ITEMS.find {
        it.id == equippedBundleItemId && (pantryProvisionCounts[it.id] ?: 0) > 0
    }
    val activeBundleLootItem = if (activeBundleShopItem == null) {
        inventoryList.find { it.id == equippedBundleItemId }
    } else null

    val loadoutProtectPct = (activeGearList.sumOf { it.preventsMishapBonusPct } +
        (if (activeBundleLootItem?.category == ChantalLootCategory.HERB) 20 else 0)).coerceAtMost(95)
    val loadoutCoinsBonusPct = activeGearList.sumOf { it.expeditionCoinMultiplierPct } +
        (activeBundleShopItem?.expeditionCoinMultiplierPct ?: 0) +
        (if (activeBundleLootItem?.category == ChantalLootCategory.TRINKET) 20 else 0)
    val loadoutTrophyBonusPct = (15 + activeGearList.sumOf { it.denTrophyChanceBonusPct } +
        (activeBundleShopItem?.denTrophyChanceBonusPct ?: 0)).coerceAtMost(85)

    // Сохранение полного снимка прогресса (schemaVersion = 1 в ChantalProgressRepository)
    val progressSnapshot = JSONObject().apply {
        put("hasCompletedPrologue", hasCompletedPrologue)
        put("totalJourneys", totalJourneys)
        put("maturityXp", maturityXp)
        put("forceTransformPreview", forceTransformPreview)
        put("hasUsedPetThisCycle", hasUsedPetThisCycle)
        put("denAccordionTab", denAccordionTab)
        put("activeLoadoutSlotPicker", activeLoadoutSlotPicker)
        put("equippedOutfitId", equippedOutfitId ?: JSONObject.NULL)
        put("equippedToolId", equippedToolId ?: JSONObject.NULL)
        put("equippedWeaponId", equippedWeaponId ?: JSONObject.NULL)
        put("equippedBundleItemId", equippedBundleItemId ?: JSONObject.NULL)
        put("selectedBagItemId", selectedBagItemId ?: JSONObject.NULL)
        put("shopCategoryTab", shopCategoryTab)
        put("shopDenStepFilter", shopDenStepFilter)
        put("hasActiveRoadAmulet", hasActiveRoadAmulet)
        put("bonusEnergy", bonusEnergy)
        put("spentEnergy", spentEnergy)
        put("coins", coins)
        put("curiosity", curiosity)
        put("tenderness", tenderness)
        put("playfulness", playfulness)
        put("romanticism", romanticism)
        put("courage", courage)
        put("wit", wit)
        put("moodPct", moodPct)
        put("isExhausted45", isExhausted45)
        put("debtCoins", debtCoins)
        put("debtReasonRu", debtReasonRu)
        put("showTraitArrows", showTraitArrows)
        put("petTapCount", petTapCount)
        put("subTab", subTab)
        put("activeJourneyStartMs", activeJourneyStartMs)
        put("preferredRegionId", preferredRegionId)
        put("activeRegionId", activeRegionId)
        put("activeRegionTitleRu", activeRegionTitleRu)
        put("activeNpcRu", activeNpcRu)
        put("activeStayedInSameRegion", activeStayedInSameRegion)
        put("activeAutonomousReasonRu", activeAutonomousReasonRu)
        put("activeIsFiller", activeIsFiller)
        put("activeFillerPrompt", activeFillerPrompt)
        put("fillerTextInput", fillerTextInput)
        put("customLoreNotes", customLoreNotes)
        put("selectedAiProvider", selectedAiProvider)
        put("selectedOpenRouterModel", selectedOpenRouterModel)
        put("customOpenRouterModelInput", customOpenRouterModelInput)
        put("selectedDifficulty", selectedDifficulty)
        put("newlyAddedBagItemIds", JSONArray(newlyAddedBagItemIds.toList()))
        put("ownedShopItemIds", JSONArray(ownedShopItemIds.toList()))
        put("claimedMerchantOfferIds", JSONArray(claimedMerchantOfferIds.toList()))
        put("gearDurabilityMap", JSONObject(gearDurabilityMap.toMap()))
        put("pantryProvisionCounts", JSONObject(pantryProvisionCounts.toMap()))
        put("inventoryList", JSONArray(inventoryList.map { ChantalProgressCodec.encode(it) }))
        put("journalList", JSONArray(journalList.map { ChantalProgressCodec.encode(it) }))
        put("pendingEpisode", pendingEpisode?.let { ChantalProgressCodec.encode(it) } ?: JSONObject.NULL)
        put("rewardPopupData", rewardPopupData?.let { ChantalProgressCodec.encode(it) } ?: JSONObject.NULL)
    }
    SideEffect { progressRepository.save(progressSnapshot) }

    // Пролог (первая встреча со спасённым рыжим лисёнком)
    if (!hasCompletedPrologue) {
        ChantalPrologueCard(
            onCompletePrologue = {
                onPlaySampleTone(SignalType.SET_COMPLETED)
                hasCompletedPrologue = true
            },
            modifier = modifier
        )
        return
    }

    // Модальный Pop-up награды после похода
    rewardPopupData?.let { reward ->
        ChantalRewardPopupDialog(
            reward = reward,
            onDismiss = { rewardPopupData = null },
            onOpenInBag = { lootId ->
                selectedBagItemId = lootId
                subTab = "bag"
                rewardPopupData = null
                isBurgerMenuOpen = true
            }
        )
    }

    // Модальное меню с 6 игровыми разделами (открывается поверх экрана, НЕ сдвигая сцену с персонажем вниз)
    if (isBurgerMenuOpen) {
        ChantalModalMenuDialog(
            subTab = subTab,
            onSelectSubTab = { chosenTab ->
                subTab = chosenTab
            },
            onDismiss = { isBurgerMenuOpen = false },
            denTierNumber = denTierNumber,
            itemsInCurrentStep = itemsInCurrentStep,
            ownedDenCount = ownedDenCount,
            foundDenTrophyCount = foundDenTrophyCount,
            totalJourneys = totalJourneys,
            preferredRegionId = preferredRegionId,
            routeLocked = activeJourneyStartMs > 0L || pendingEpisode != null,
            onChooseRoute = { preferredRegionId = it; isBurgerMenuOpen = false; denAccordionTab = "loadout" },
            coins = coins,
            selectedAiProvider = selectedAiProvider,
            traits = traits,
            perks = perks,
            inventoryList = inventoryList,
            newlyAddedBagItemIds = newlyAddedBagItemIds,
            selectedBagItemId = selectedBagItemId,
            onSelectBagItem = { id ->
                selectedBagItemId = id
                if (id != null) newlyAddedBagItemIds.remove(id)
            },
            onInspectClue = { item ->
                if (!item.inspectedBonusClaimed) {
                    curiosity = (curiosity + 1).coerceAtMost(100)
                    val idx = inventoryList.indexOfFirst { it.id == item.id }
                    if (idx >= 0) inventoryList[idx] = item.copy(inspectedBonusClaimed = true)
                    activeArrowToast = "Любопытство ↑"
                }
                selectedBagItemId = null
            },
            onBrewHerb = { item ->
                isExhausted45 = false
                moodPct = (moodPct + 15).coerceAtMost(100)
                inventoryList.removeAll { it.id == item.id }
                selectedBagItemId = null
            },
            onGiftTrinket = { item ->
                coins += 18
                tenderness = (tenderness + 1).coerceAtMost(100)
                activeArrowToast = "Нежность ↑"
                inventoryList.removeAll { it.id == item.id }
                selectedBagItemId = null
            },
            onSellLootItem = { item, sellPrice ->
                coins += sellPrice
                inventoryList.removeAll { it.id == item.id }
                selectedBagItemId = null
            },
            onCraftRoadAmulet = {
                val herbs = inventoryList.filter { it.category == ChantalLootCategory.HERB }
                val trinkets = inventoryList.filter { it.category == ChantalLootCategory.TRINKET }
                val firstHerb = herbs.firstOrNull()
                val secondItem = trinkets.firstOrNull() ?: herbs.getOrNull(1)
                if (firstHerb != null && secondItem != null) {
                    inventoryList.remove(firstHerb)
                    inventoryList.remove(secondItem)
                    hasActiveRoadAmulet = true
                    isExhausted45 = false
                    curiosity = (curiosity + 1).coerceAtMost(100)
                    activeArrowToast = "Любопытство ↑"
                    lastReactionBanner = "Создан Оберег Попутного Ветра (+100% защита и +50% монет)!"
                }
            },
            selectedShopEntryId = selectedShopEntryId,
            onSelectShopEntryId = { selectedShopEntryId = it },
            shopCategoryTab = shopCategoryTab,
            onSelectShopCategoryTab = { shopCategoryTab = it },
            shopDenStepFilter = shopDenStepFilter,
            onSelectShopDenStepFilter = { shopDenStepFilter = it },
            ownedShopItemIds = ownedShopItemIds,
            claimedMerchantOfferIds = claimedMerchantOfferIds,
            onBuyMerchantOffer = { offerId ->
                DEFAULT_CHANTAL_MERCHANT_OFFERS.find { it.id == offerId }?.let { offer ->
                    val isClaimed = offer.id in claimedMerchantOfferIds
                    if (!isClaimed && coins >= offer.discountPriceCoins) {
                        coins -= offer.discountPriceCoins
                        claimedMerchantOfferIds.add(offer.id)
                        when (offer.rewardType) {
                            "DEN_TROPHY" -> {
                                val trophy = ChantalInventoryLootItem(
                                    id = "merch_trophy_${System.currentTimeMillis()}",
                                    titleRu = offer.titleRu,
                                    shortTitleRu = offer.shortTitleRu,
                                    emoji = offer.emoji,
                                    category = ChantalLootCategory.DEN_TROPHY,
                                    descriptionRu = offer.effectRu,
                                    sellPriceCoins = 28,
                                    foundAtRegionRu = "Лавка на колёсах",
                                    foundDateIso = "Сегодня"
                                )
                                inventoryList.add(0, trophy)
                                newlyAddedBagItemIds.add(trophy.id)
                            }
                            "ROYAL_CLUE" -> {
                                curiosity = (curiosity + 2).coerceAtMost(100)
                            }
                            else -> {
                                hasActiveRoadAmulet = true
                            }
                        }
                        selectedShopEntryId = null
                    }
                }
            },
            onBuyShopItem = { item ->
                val owned = item.id in ownedShopItemIds
                if (coins >= item.priceCoins && (!owned || item.category == ChantalShopCategory.CONSUMABLE)) {
                    coins -= item.priceCoins
                    if (item.category == ChantalShopCategory.CONSUMABLE) {
                        pantryProvisionCounts[item.id] = (pantryProvisionCounts[item.id] ?: 0) + 1
                        equippedBundleItemId = item.id
                        isExhausted45 = false
                    } else if (!owned) {
                        ownedShopItemIds.add(item.id)
                        if (item.category == ChantalShopCategory.APPAREL) {
                            gearDurabilityMap[item.id] = item.maxDurability
                        }
                    }
                    selectedShopEntryId = null
                }
            },
            journalList = journalList,
            onSelectAiProvider = { selectedAiProvider = it },
            selectedOpenRouterModel = selectedOpenRouterModel,
            onSelectOpenRouterModel = { selectedOpenRouterModel = it },
            customOpenRouterModelInput = customOpenRouterModelInput,
            onCustomOpenRouterModelChange = { customOpenRouterModelInput = it },
            selectedDifficulty = selectedDifficulty,
            onSelectDifficulty = { selectedDifficulty = it },
            customLoreNotes = customLoreNotes,
            onCustomLoreNotesChange = { customLoreNotes = it },
            showTraitArrows = showTraitArrows,
            onToggleTraitArrows = { showTraitArrows = !showTraitArrows },
            forceTransformPreview = forceTransformPreview,
            onToggleTransformPreview = { forceTransformPreview = !forceTransformPreview }
        )
    }

    val isJourneyActive = activeJourneyStartMs > 0L
    val canPet = !hasUsedPetThisCycle && !isJourneyActive

    // Компактный главный экран: Шапка -> Сцена персонажа -> Настроение и XP -> 4 кнопки -> Панель Похода (по клику) -> Текущее событие
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("velaris_chantal_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showTraitArrows && activeArrowToast != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HobbitElevatedBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, HobbitGold, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Изменение характера: $activeArrowToast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HobbitGold,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // 1. Компактная шапка
        ChantalCompactHeader(
            stageTitleRu = stageInfo.titleRu,
            coins = coins,
            currentEnergy = currentEnergy,
            currentStreakDays = currentStreakDays,
            isMenuOpen = isBurgerMenuOpen,
            onToggleMenu = { isBurgerMenuOpen = !isBurgerMenuOpen }
        )

        // 2. Крупная центральная сцена с изображением Шанталь в домике
        ChantalCentralDenScene(
            stageInfo = stageInfo,
            portraitResId = portraitResId,
            denInteriorResId = denInteriorResId,
            denTierNumber = denTierNumber,
            itemsInCurrentStep = itemsInCurrentStep,
            totalDenItemsCount = totalDenItemsCount,
            isHeartPulseActive = isHeartPulseActive,
            tendernessHealBonusPct = perks.tendernessMoodHealBonusPct,
            activeGearList = activeGearList,
            hasActiveRoadAmulet = hasActiveRoadAmulet,
            isJourneyActive = isJourneyActive,
            activeRegionTitleRu = activeRegionTitleRu
        )

        // 3. Панель уровня, XP и настроения (с компактными чипами Характера и Интерьера)
        ChantalLevelXpMoodPanel(
            maturityProgress = maturityProgress,
            moodPct = moodPct,
            totalJourneys = totalJourneys,
            recentXpGainText = recentXpGainText,
            dominantTraitNameRu = perks.dominantTraitNameRu,
            denAccordionTab = denAccordionTab,
            itemsInCurrentStep = itemsInCurrentStep,
            onToggleTraits = {
                denAccordionTab = if (denAccordionTab == "traits") "none" else "traits"
            },
            onToggleInterior = {
                denAccordionTab = if (denAccordionTab == "housing") "none" else "housing"
            }
        )

        // 4. Четыре основные кнопки: Погладить, Поход, Сумка, История
        ChantalQuickActionsRow(
            canPet = canPet,
            isJourneyActive = isJourneyActive,
            isExpeditionPanelOpen = denAccordionTab == "loadout",
            bagCount = inventoryList.size,
            hasPendingStory = pendingEpisode != null,
            onPetClick = {
                onPlaySampleTone(SignalType.RELAXATION)
                hasUsedPetThisCycle = true
                petTapCount += 1
                maturityXp += 3
                recentXpGainText = "+3 XP (Ласка ❤️)"
                moodPct = (moodPct + perks.tendernessMoodHealBonusPct).coerceAtMost(100)
                petDialogueBubble = traits.buildPettingReaction(stageInfo.isTransformed, petTapCount)
                isHeartPulseActive = true
            },
            onExpeditionClick = {
                denAccordionTab = if (denAccordionTab == "loadout") "none" else "loadout"
            },
            onBagClick = {
                subTab = "bag"
                isBurgerMenuOpen = true
            },
            onStoryClick = {
                if (pendingEpisode == null) {
                    subTab = "journal"
                    isBurgerMenuOpen = true
                }
            }
        )

        // 5. Отдельная игровая панель подготовки к походу и 4 слотов экипировки (открывается кнопкой «Поход»), а также раскрывающиеся панели Характера / Интерьера
        ChantalExpeditionAndSubPanelsSection(
            denAccordionTab = denAccordionTab,
            onClosePanel = { denAccordionTab = "none" },
            traits = traits,
            perks = perks,
            selectedTraitPopupName = selectedTraitPopupName,
            onSelectTraitPopupName = { selectedTraitPopupName = it },
            denTierNumber = denTierNumber,
            itemsInCurrentStep = itemsInCurrentStep,
            ownedDenCount = ownedDenCount,
            foundDenTrophyCount = foundDenTrophyCount,
            onOpenShopComfort = {
                shopCategoryTab = "den_comfort"
                subTab = "shop"
                isBurgerMenuOpen = true
            },
            isJourneyActive = isJourneyActive,
            activeJourneyStartMs = activeJourneyStartMs,
            activeRegionTitleRu = activeRegionTitleRu,
            activeNpcRu = activeNpcRu,
            activeStayedInSameRegion = activeStayedInSameRegion,
            activeAutonomousReasonRu = activeAutonomousReasonRu,
            timeProvider = timeProvider,
            hasPendingStory = pendingEpisode != null,
            preferredRegionTitle = DEFAULT_CHANTAL_MAP_REGIONS.firstOrNull { it.id == preferredRegionId }?.titleRu,
            onCompleteActiveJourney = complete@{
                if (!ChantalJourneyClock.ready(activeJourneyStartMs, timeProvider.currentWallTimeMs()) || pendingEpisode != null) return@complete
                val nextNum = totalJourneys + 1
                val isMilestone = !activeIsFiller && nextNum % 5 == 0
                val baseReward = if (isMilestone) 38 else 19
                val amuletBonus = if (hasActiveRoadAmulet) 150 else 100
                val coinsFound = (((baseReward + perks.curiosityBonusCoins) * (100 + streakBonusPct + loadoutCoinsBonusPct) * amuletBonus) / 10000)
                hasActiveRoadAmulet = false
                hasUsedPetThisCycle = false

                val bringsDenTrophy = !isMilestone && Random.nextInt(100) < loadoutTrophyBonusPct
                val foundLoot = if (isMilestone) {
                    ChantalInventoryLootItem(
                        id = "loot_${System.currentTimeMillis()}",
                        titleRu = "Королевская печать Главы #$nextNum",
                        shortTitleRu = "Печать #$nextNum",
                        emoji = "💮",
                        category = ChantalLootCategory.ROYAL_CLUE,
                        descriptionRu = "Древняя королевская реликвия из Юбилейного Сундука Главы #$nextNum.",
                        sellPriceCoins = 45,
                        foundAtRegionRu = activeRegionTitleRu,
                        foundDateIso = "Сегодня"
                    )
                } else if (bringsDenTrophy) {
                    ChantalInventoryLootItem(
                        id = "loot_${System.currentTimeMillis()}",
                        titleRu = "Резной фонарик со светлячком для Домика",
                        shortTitleRu = "Ночник в Дом",
                        emoji = "🏮",
                        category = ChantalLootCategory.DEN_TROPHY,
                        descriptionRu = "Уютная находка для интерьера Домика (+1 из 10 к уровню жилья)!",
                        sellPriceCoins = 26,
                        foundAtRegionRu = activeRegionTitleRu,
                        foundDateIso = "Сегодня"
                    )
                } else {
                    ChantalInventoryLootItem(
                        id = "loot_${System.currentTimeMillis()}",
                        titleRu = "Резной жетон из «$activeRegionTitleRu»",
                        shortTitleRu = "Жетон",
                        emoji = "🏅",
                        category = ChantalLootCategory.TRINKET,
                        descriptionRu = "Памятная находка со встречи с персонажем $activeNpcRu.",
                        sellPriceCoins = 22,
                        foundAtRegionRu = activeRegionTitleRu,
                        foundDateIso = "Сегодня"
                    )
                }

                val epChoices = mutableListOf(
                    ChantalChoiceOption(
                        labelRu = "Выбрать из её рыжей шёрстки репейник и тихо расспросить о встрече у старого моста",
                        reactionRu = "Шанталь доверчиво прижимается щекой к твоей ладони, задумчиво смотрит в огонь камина и радостно подталкивает носом принесённую находку.",
                        delta = ChantalTraits(1, 2, 0, 1, 0, 0)
                    ),
                    ChantalChoiceOption(
                        labelRu = "Поддразнить её, что собеседник отдал находку только ради того, чтобы лиса не утащила коврижку",
                        reactionRu = "Лисёнок возмущённо тявкает, делает озорной разворот вокруг своей оси и гордо садится у камина в ожидании угощения!",
                        delta = ChantalTraits(0, 0, 2, 0, 1, 2)
                    ),
                    ChantalChoiceOption(
                        labelRu = "Поднести медный фонарь к находке и поискать на ней следы старинного королевского клейма",
                        reactionRu = "Шанталь мгновенно забывает про усталость, вскакивает передними лапками на край столика и внимательно изучает линии на находке вместе с тобой.",
                        delta = ChantalTraits(2, 0, 0, 0, 2, 1)
                    )
                )
                if (isMilestone) {
                    epChoices.add(
                        ChantalChoiceOption(
                            labelRu = "★ Открыть потайное отделение Юбилейного Сундука Главы #$nextNum",
                            reactionRu = "Крышка кованого сундука со звоном откидывается: внутри сверкает королевская печать и полновесная горсть золотых монет!",
                            delta = ChantalTraits(2, 1, 1, 1, 2, 1)
                        )
                    )
                }
                pendingEpisode = ChantalStoryEpisode(
                    id = "ep_${System.currentTimeMillis()}",
                    journeyNumber = nextNum,
                    titleRu = if (isMilestone) {
                        "★ Юбилейный Сундук Главы #$nextNum в «$activeRegionTitleRu»"
                    } else {
                        "Приключение в «$activeRegionTitleRu»"
                    },
                    storyTextRu = "Шанталь самостоятельно выбрала маршрут в локацию «$activeRegionTitleRu», где её уже ждал $activeNpcRu. По дороге она обследовала старый мост и нашла спрятанный тайник под корнями векового дерева. Собеседник поделился с ней важными слухами о старом королевстве, а Шанталь помогла ему разобраться с загадкой, проявив свою ведущую черту — ${perks.dominantTraitNameRu}. Домой она вернулась с полной сумкой новостей, монетами и находкой!",
                    mapNoteRu = activeAutonomousReasonRu,
                    regionTitleRu = activeRegionTitleRu,
                    npcNameRu = activeNpcRu,
                    stayedInSameRegion = activeStayedInSameRegion,
                    isFiller = activeIsFiller,
                    isMilestoneChest = isMilestone,
                    lootItem = foundLoot,
                    coinsEarned = coinsFound,
                    causesExhaustion45 = false,
                    debtAmountCoins = 0,
                    badNewsRu = "",
                    choices = epChoices
                )
                pendingEpisode = pendingEpisode?.let { com.example.data.ChantalOfflineAdventure.decorate(it) }
                pendingEpisode?.let { base ->
                    com.example.data.ChantalLanternAdventure.next(base, journalList.toList(),
                        progressRepository.load().optJSONObject("worldMemory") ?: com.example.data.ChantalWorldMemory.empty())
                        ?.let { pendingEpisode = it }
                }
                activeJourneyStartMs = 0L
                denAccordionTab = "none"
            },
            hasActiveRoadAmulet = hasActiveRoadAmulet,
            loadoutProtectPct = loadoutProtectPct,
            loadoutCoinsBonusPct = loadoutCoinsBonusPct,
            onOpenBagForAmulet = {
                subTab = "bag"
                isBurgerMenuOpen = true
            },
            equippedOutfitId = equippedOutfitId,
            equippedToolId = equippedToolId,
            equippedWeaponId = equippedWeaponId,
            activeBundleShopItem = activeBundleShopItem,
            activeBundleLootItem = activeBundleLootItem,
            ownedShopItemIds = ownedShopItemIds,
            pantryProvisionCounts = pantryProvisionCounts,
            activeLoadoutSlotPicker = activeLoadoutSlotPicker,
            onSelectLoadoutSlotPicker = { activeLoadoutSlotPicker = it },
            activeSlotInspectItemId = activeSlotInspectItemId,
            onSelectSlotInspectItemId = { activeSlotInspectItemId = it },
            coins = coins,
            onToggleEquipGear = { slotKey, inspectGear ->
                when (slotKey) {
                    "OUTFIT" -> equippedOutfitId = if (equippedOutfitId == inspectGear.id) null else inspectGear.id
                    "TOOL" -> equippedToolId = if (equippedToolId == inspectGear.id) null else inspectGear.id
                    "WEAPON" -> equippedWeaponId = if (equippedWeaponId == inspectGear.id) null else inspectGear.id
                    else -> equippedBundleItemId = if (equippedBundleItemId == inspectGear.id) null else inspectGear.id
                }
                activeSlotInspectItemId = null
            },
            onBuyAndEquipGear = { inspectGear ->
                if (coins >= inspectGear.priceCoins) {
                    coins -= inspectGear.priceCoins
                    if (inspectGear.category == ChantalShopCategory.CONSUMABLE) {
                        pantryProvisionCounts[inspectGear.id] = (pantryProvisionCounts[inspectGear.id] ?: 0) + 1
                        equippedBundleItemId = inspectGear.id
                    } else {
                        ownedShopItemIds.add(inspectGear.id)
                        gearDurabilityMap[inspectGear.id] = inspectGear.maxDurability
                        when (inspectGear.loadoutSlotType) {
                            ChantalLoadoutSlotType.OUTFIT -> equippedOutfitId = inspectGear.id
                            ChantalLoadoutSlotType.TOOL -> equippedToolId = inspectGear.id
                            ChantalLoadoutSlotType.WEAPON -> equippedWeaponId = inspectGear.id
                            else -> {}
                        }
                    }
                    activeSlotInspectItemId = null
                }
            },
            fillerTextInput = fillerTextInput,
            onFillerTextChange = { fillerTextInput = it },
            currentEnergy = currentEnergy,
            requiredJourneyEnergy = requiredJourneyEnergy,
            onStartJourney = {
                if (activeJourneyStartMs == 0L && pendingEpisode == null && currentEnergy >= requiredJourneyEnergy) {
                    onPlaySampleTone(SignalType.CONTRACTION)
                    spentEnergy += requiredJourneyEnergy
                    isExhausted45 = false
                    activeLoadoutSlotPicker = "NONE"

                    val unlocked = DEFAULT_CHANTAL_MAP_REGIONS.filter { totalJourneys >= it.minJourneysRequired }
                    val lastRegTitle = journalList.firstOrNull()?.regionTitleRu
                    val lastReg = unlocked.find { it.titleRu == lastRegTitle }
                    val staySame = lastReg != null && Random.nextFloat() < 0.45f
                    val chosenReg = unlocked.firstOrNull { it.id == preferredRegionId } ?: if (staySame && lastReg != null) lastReg else unlocked.random()
                    val chosenNpc = chosenReg.npcsRu.random()
                    val chosenThread = chosenReg.unfinishedThreadsRu.random()

                    activeRegionId = chosenReg.id
                    activeRegionTitleRu = chosenReg.titleRu
                    activeNpcRu = chosenNpc
                    activeStayedInSameRegion = chosenReg.id == lastReg?.id
                    activeAutonomousReasonRu = if (activeStayedInSameRegion) {
                        "Шанталь решила снова вернуться в «${chosenReg.titleRu}», так как у неё там остались незаконченные дела ($chosenThread) и встреча с персонажем $chosenNpc."
                    } else {
                        "Шанталь выбрала на карте «${chosenReg.titleRu}», чтобы $chosenThread и встретиться с персонажем $chosenNpc."
                    }
                    activeIsFiller = fillerTextInput.isNotBlank()
                    activeFillerPrompt = fillerTextInput
                    fillerTextInput = ""
                    activeJourneyStartMs = timeProvider.currentWallTimeMs()
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        val prefs = context.getSharedPreferences("chantal_notifications", android.content.Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("permissionAsked", false) && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            prefs.edit().putBoolean("permissionAsked", true).apply()
                            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            },
            onOpenWorkoutPlan = onOpenWorkoutPlan
        )

        // Generate only on an explicit user request. The persisted original survives all failures.
        pendingEpisode?.takeIf { !it.isFiller && !it.isMilestoneChest && !it.id.startsWith("lantern_chain_") }?.let { base ->
            if (isGenerating) {
                Text("Шанталь вспоминает приключение…")
                androidx.compose.material3.TextButton(onClick = { generationJob?.cancel() }) { Text("Отменить и оставить историю") }
            } else {
                androidx.compose.material3.OutlinedButton(onClick = {
                    if (!geminiAccess.hasKey()) {
                        subTab = "ai"; isBurgerMenuOpen = true
                    } else if (!geminiAccess.claimRequest()) {
                        generationMessage = "Между запросами нужна минута. Текущую историю можно прочитать сейчас."
                    } else {
                        isGenerating = true
                        generationMessage = ""
                        generationJob = generationScope.launch {
                            try {
                                val result = try {
                                    val secret = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { geminiAccess.read() }
                                    com.example.data.ChantalStoryGeneration.generate(
                                        com.example.data.GeminiStoryTransport(secret, geminiAccess.model()), base,
                                        progressRepository.load().optJSONObject("worldMemory") ?: com.example.data.ChantalWorldMemory.empty(), traits)
                                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                  catch (_: Exception) { com.example.data.ChantalGenerationResult.Unavailable }
                                if (pendingEpisode?.id == base.id && journalList.none { it.id == base.id }) {
                                    if (result is com.example.data.ChantalGenerationResult.Ready) {
                                        pendingEpisode = result.story.episode
                                        generationMessage = "История Gemini готова. Выбери, как ответить Шанталь."
                                    } else generationMessage = "Gemini пока недоступен: проверь ключ, модель, связь и квоту. Исходная история сохранена."
                                }
                            } finally { isGenerating = false }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().testTag("generate_gemini_story")) {
                    Text("Новая версия истории · Gemini")
                }
            }
        }
        if (generationMessage.isNotBlank()) Text(generationMessage)
        // Hide decision actions while a request is in flight; cancellation restores the original.
        if (!isGenerating) ChantalCompactStoryEventCard(
            pendingEpisode = pendingEpisode,
            fillerTextInput = fillerTextInput,
            onFillerTextChange = { fillerTextInput = it },
            onSelectEpisodeChoice = { ep, choiceIdx ->
                val opt = ep.choices.getOrNull(choiceIdx) ?: return@ChantalCompactStoryEventCard
                if (pendingEpisode?.id != ep.id || journalList.any { it.id == ep.id }) return@ChantalCompactStoryEventCard
                onPlaySampleTone(SignalType.SET_COMPLETED)
                val arrows = opt.delta.formatArrowIndicators().joinToString(" · ")
                inventoryList.add(0, ep.lootItem)
                newlyAddedBagItemIds.add(ep.lootItem.id)
                selectedBagItemId = ep.lootItem.id
                coins += ep.coinsEarned
                val xpEarned = if (ep.isMilestoneChest) 15 else if (ep.isFiller) 8 else 10
                maturityXp += xpEarned
                recentXpGainText = "+$xpEarned XP (Поход 🧭)"

                if (ep.causesExhaustion45) isExhausted45 = true
                if (ep.debtAmountCoins > 0) {
                    debtCoins += ep.debtAmountCoins
                    debtReasonRu = ep.badNewsRu
                }
                if (!ep.isFiller) {
                    val updated = traits.applyCanonicalDelta(opt.delta)
                    curiosity = updated.curiosity
                    tenderness = updated.tenderness
                    playfulness = updated.playfulness
                    romanticism = updated.romanticism
                    courage = updated.courage
                    wit = updated.wit
                    totalJourneys += 1
                    if (showTraitArrows && arrows.isNotBlank()) {
                        activeArrowToast = arrows
                    }
                }
                journalList.add(
                    0,
                    ep.copy(chosenActionSummaryRu = "${opt.labelRu} — Реакция Шанталь: ${opt.reactionRu}")
                )
                rewardPopupData = ChantalRewardPopupData(
                    episodeTitleRu = ep.titleRu,
                    journeyNumber = ep.journeyNumber,
                    regionTitleRu = ep.regionTitleRu,
                    playerChoiceRu = opt.labelRu,
                    chantalReactionRu = opt.reactionRu,
                    coinsEarned = ep.coinsEarned,
                    xpEarned = xpEarned,
                    lootItem = ep.lootItem,
                    traitArrowsText = arrows,
                    isMilestoneChest = ep.isMilestoneChest
                )
                pendingEpisode = null
            },
            onSubmitFillerResponse = { ep ->
                if (pendingEpisode?.id != ep.id || journalList.any { it.id == ep.id }) return@ChantalCompactStoryEventCard
                onPlaySampleTone(SignalType.SET_COMPLETED)
                inventoryList.add(0, ep.lootItem)
                newlyAddedBagItemIds.add(ep.lootItem.id)
                selectedBagItemId = ep.lootItem.id
                coins += ep.coinsEarned
                val xpEarned = 8
                maturityXp += xpEarned
                recentXpGainText = "+$xpEarned XP (Поручение 🪶)"
                if (debtCoins > 0) {
                    debtCoins = 0
                    debtReasonRu = ""
                }
                val userText = fillerTextInput
                journalList.add(
                    0,
                    ep.copy(
                        isFiller = true,
                        chosenActionSummaryRu = "Свой текст: $userText"
                    )
                )
                rewardPopupData = ChantalRewardPopupData(
                    episodeTitleRu = ep.titleRu,
                    journeyNumber = ep.journeyNumber,
                    regionTitleRu = ep.regionTitleRu,
                    playerChoiceRu = userText,
                    chantalReactionRu = "Шанталь внимательно выслушала твоё поручение, радостно тявкнула и выложила на стол принесённые монеты и находку!",
                    coinsEarned = ep.coinsEarned,
                    xpEarned = xpEarned,
                    lootItem = ep.lootItem,
                    traitArrowsText = "",
                    isMilestoneChest = ep.isMilestoneChest
                )
                fillerTextInput = ""
                pendingEpisode = null
            },
            petDialogueBubble = petDialogueBubble,
            lastReactionBanner = lastReactionBanner,
            latestJournalEpisode = journalList.firstOrNull(),
            dominantTraitNameRu = perks.dominantTraitNameRu,
            onOpenFullJournal = {
                subTab = "journal"
                isBurgerMenuOpen = true
            }
        )
    }
}
