package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import com.example.data.ChantalProgressRepository
import com.example.data.ChantalProgressCodec
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.domain.ChantalAiProvider
import com.example.domain.ChantalChoiceOption
import com.example.domain.ChantalInventoryLootItem
import com.example.domain.ChantalLoadoutSlotType
import com.example.domain.ChantalLootCategory
import com.example.domain.ChantalRewardPopupData
import com.example.domain.ChantalShopCategory
import com.example.domain.ChantalShopItem
import com.example.domain.ChantalStoryDifficulty
import com.example.domain.ChantalStoryEpisode
import com.example.domain.ChantalTraits
import com.example.domain.DEFAULT_CHANTAL_MAP_REGIONS
import com.example.domain.DEFAULT_CHANTAL_MERCHANT_OFFERS
import com.example.domain.DEFAULT_CHANTAL_SHOP_ITEMS
import com.example.domain.OPENROUTER_ANDROID_MODELS
import com.example.domain.SignalType
import com.example.domain.computeChantalMaturityProgress
import com.example.domain.resolveChantalStageStatus
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private val HobbitBg = Color(0xFF15100C)
private val HobbitCardBg = Color(0xFF1E1712)
private val HobbitElevatedBg = Color(0xFF281F18)
private val HobbitBorder = Color(0xFF4D3D2C)
private val HobbitGold = Color(0xFFF59E0B)
private val HobbitEmerald = Color(0xFF10B981)
private val HobbitOrange = Color(0xFFEA580C)
private val HobbitPink = Color(0xFFEC4899)
private val HobbitSky = Color(0xFF38BDF8)
private val HobbitCream = Color(0xFFFFF8EB)
private val HobbitMuted = Color(0xFFD5C3AA)

@Composable
fun VelarisChantalScreen(
    completedWorkoutsCount: Int,
    currentStreakDays: Int,
    onOpenWorkoutPlan: () -> Unit,
    onPlaySampleTone: (SignalType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progressRepository = remember(context.applicationContext) { ChantalProgressRepository(context) }
    val loadedProgress = remember(progressRepository) { runCatching { progressRepository.load() } }
    if (loadedProgress.isFailure) {
        Text("Не удалось прочитать сохранение Шанталь. Данные сохранены без изменений. Перезапусти приложение; если ошибка повторяется, сообщи нам.", modifier = modifier)
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

    // Подразделы в Домике («Характер» / «Интерьер»)
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
    val currentEnergy = ((completedWorkoutsCount * 10) + bonusEnergy - spentEnergy).coerceAtLeast(0)
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

    // Текущий раздел в Бургер-меню
    var subTab by remember { mutableStateOf(saved.optString("subTab", "den")) }

    // Активный часовой поход
    var activeJourneyStartMs by remember { mutableLongStateOf(saved.optLong("activeJourneyStartMs", 0L)) }
    var activeRegionId by remember { mutableStateOf(saved.optString("activeRegionId", "")) }
    var activeRegionTitleRu by remember { mutableStateOf(saved.optString("activeRegionTitleRu", "")) }
    var activeNpcRu by remember { mutableStateOf(saved.optString("activeNpcRu", "")) }
    var activeStayedInSameRegion by remember { mutableStateOf(saved.optBoolean("activeStayedInSameRegion", false)) }
    var activeAutonomousReasonRu by remember { mutableStateOf(saved.optString("activeAutonomousReasonRu", "")) }
    var activeIsFiller by remember { mutableStateOf(saved.optBoolean("activeIsFiller", false)) }
    var activeFillerPrompt by remember { mutableStateOf(saved.optString("activeFillerPrompt", "")) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeJourneyStartMs) {
        while (activeJourneyStartMs > 0L) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Непрочитанная история после возвращения
    var pendingEpisode by remember { mutableStateOf<ChantalStoryEpisode?>(saved.optJSONObject("pendingEpisode")?.let { ChantalProgressCodec.decodeChantalStoryEpisode(it) }) }
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

    // Capture all game fields from one successful composition, before the prologue return.
    val progressSnapshot = JSONObject().apply {
        put("hasCompletedPrologue", hasCompletedPrologue ?: JSONObject.NULL)
        put("totalJourneys", totalJourneys ?: JSONObject.NULL)
        put("maturityXp", maturityXp ?: JSONObject.NULL)
        put("forceTransformPreview", forceTransformPreview ?: JSONObject.NULL)
        put("hasUsedPetThisCycle", hasUsedPetThisCycle ?: JSONObject.NULL)
        put("denAccordionTab", denAccordionTab ?: JSONObject.NULL)
        put("activeLoadoutSlotPicker", activeLoadoutSlotPicker ?: JSONObject.NULL)
        put("equippedOutfitId", equippedOutfitId ?: JSONObject.NULL)
        put("equippedToolId", equippedToolId ?: JSONObject.NULL)
        put("equippedWeaponId", equippedWeaponId ?: JSONObject.NULL)
        put("equippedBundleItemId", equippedBundleItemId ?: JSONObject.NULL)
        put("selectedBagItemId", selectedBagItemId ?: JSONObject.NULL)
        put("shopCategoryTab", shopCategoryTab ?: JSONObject.NULL)
        put("shopDenStepFilter", shopDenStepFilter ?: JSONObject.NULL)
        put("hasActiveRoadAmulet", hasActiveRoadAmulet ?: JSONObject.NULL)
        put("bonusEnergy", bonusEnergy ?: JSONObject.NULL)
        put("spentEnergy", spentEnergy ?: JSONObject.NULL)
        put("coins", coins ?: JSONObject.NULL)
        put("curiosity", curiosity ?: JSONObject.NULL)
        put("tenderness", tenderness ?: JSONObject.NULL)
        put("playfulness", playfulness ?: JSONObject.NULL)
        put("romanticism", romanticism ?: JSONObject.NULL)
        put("courage", courage ?: JSONObject.NULL)
        put("wit", wit ?: JSONObject.NULL)
        put("moodPct", moodPct ?: JSONObject.NULL)
        put("isExhausted45", isExhausted45 ?: JSONObject.NULL)
        put("debtCoins", debtCoins ?: JSONObject.NULL)
        put("debtReasonRu", debtReasonRu ?: JSONObject.NULL)
        put("showTraitArrows", showTraitArrows ?: JSONObject.NULL)
        put("petTapCount", petTapCount ?: JSONObject.NULL)
        put("subTab", subTab ?: JSONObject.NULL)
        put("activeJourneyStartMs", activeJourneyStartMs ?: JSONObject.NULL)
        put("activeRegionId", activeRegionId ?: JSONObject.NULL)
        put("activeRegionTitleRu", activeRegionTitleRu ?: JSONObject.NULL)
        put("activeNpcRu", activeNpcRu ?: JSONObject.NULL)
        put("activeStayedInSameRegion", activeStayedInSameRegion ?: JSONObject.NULL)
        put("activeAutonomousReasonRu", activeAutonomousReasonRu ?: JSONObject.NULL)
        put("activeIsFiller", activeIsFiller ?: JSONObject.NULL)
        put("activeFillerPrompt", activeFillerPrompt ?: JSONObject.NULL)
        put("fillerTextInput", fillerTextInput ?: JSONObject.NULL)
        put("customLoreNotes", customLoreNotes ?: JSONObject.NULL)
        put("selectedAiProvider", selectedAiProvider ?: JSONObject.NULL)
        put("selectedOpenRouterModel", selectedOpenRouterModel ?: JSONObject.NULL)
        put("customOpenRouterModelInput", customOpenRouterModelInput ?: JSONObject.NULL)
        put("selectedDifficulty", selectedDifficulty ?: JSONObject.NULL)
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

    // ПРОЛОГ (Первая встреча со спасённым рыжим лисёнком)
    if (!hasCompletedPrologue) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, HobbitBorder, RoundedCornerShape(24.dp))
                .testTag("chantal_prologue_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chantal_stage1),
                    contentDescription = "Маленький рыжий лисёнок Шанталь",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Text(
                    text = "Пролог · Дождливые сумерки у Старого Корня",
                    style = MaterialTheme.typography.labelMedium,
                    color = HobbitGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Неожиданная находка в лесу",
                    style = MaterialTheme.typography.titleLarge,
                    color = HobbitCream,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Возвращаясь вечером мимо старой водяной мельницы, ты услышал тихий шорох под корнями дуба. В мокром папоротнике, дрожа от ливня и поджав лапку, сидел крошечный огненно-рыжий лисёнок с удивительно осмысленными янтарными глазами.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HobbitMuted
                )
                Text(
                    text = "Каждая тренировка Кегеля даёт +10 Энергии. За 30 Энергии Шанталь сама выбирает маршрут на карте, отправляется в часовой поход и приносит истории, монеты, опыт взросления и находки!",
                    style = MaterialTheme.typography.bodySmall,
                    color = HobbitEmerald,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = {
                        onPlaySampleTone(SignalType.SET_COMPLETED)
                        hasCompletedPrologue = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HobbitOrange,
                        contentColor = HobbitCream
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("btn_complete_chantal_prologue")
                ) {
                    Text(
                        text = "Согреть лисёнка у камина и назвать Шанталь",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        return
    }

    // АНИМИРОВАННЫЙ POP-UP НАГРАД ПОСЛЕ ПУТЕШЕСТВИЯ (ВМЕСТО ПРОСТОГО ТЕКСТОВОГО УВЕДОМЛЕНИЯ)
    rewardPopupData?.let { reward ->
        val popScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "reward_pop_scale"
        )
        Dialog(onDismissRequest = { rewardPopupData = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(popScale)
                    .border(2.dp, HobbitGold, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (reward.isMilestoneChest) {
                                    "★ Сокровища Сундука Главы #${reward.journeyNumber}"
                                } else {
                                    "✨ Добыча из Похода #${reward.journeyNumber} · ${reward.regionTitleRu}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = HobbitGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = reward.episodeTitleRu,
                                style = MaterialTheme.typography.titleMedium,
                                color = HobbitCream,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "✕",
                            color = HobbitMuted,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { rewardPopupData = null }
                                .padding(4.dp)
                        )
                    }

                    // Две анимированные карточки выпадения: Монеты (+XP) и Находка
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, HobbitGold, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = "🪙 Монеты", style = MaterialTheme.typography.labelSmall, color = HobbitGold, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "+${reward.coinsEarned} М",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HobbitCream,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Опыт: +${reward.xpEarned} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitPink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, HobbitEmerald, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val catBadge = when (reward.lootItem.category) {
                                    ChantalLootCategory.DEN_TROPHY -> "🏠 В Интерьер (+1/10)"
                                    ChantalLootCategory.ROYAL_CLUE -> "👑 Улика"
                                    ChantalLootCategory.HERB -> "🌿 Трава"
                                    ChantalLootCategory.TRINKET -> "✦ Диковинка"
                                }
                                Text(text = catBadge, style = MaterialTheme.typography.labelSmall, color = HobbitEmerald, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${reward.lootItem.emoji} ${reward.lootItem.shortTitleRu}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HobbitCream,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    // Описание находки + живая реакция Шанталь
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${reward.lootItem.emoji} ${reward.lootItem.titleRu}",
                                style = MaterialTheme.typography.labelMedium,
                                color = HobbitGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = reward.lootItem.descriptionRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = HobbitMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HobbitGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🦊 Реакция Шанталь:",
                                style = MaterialTheme.typography.labelSmall,
                                color = HobbitGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "«${reward.chantalReactionRu}»",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = HobbitCream
                            )
                            if (reward.traitArrowsText.isNotBlank()) {
                                Text(
                                    text = "Характер: ${reward.traitArrowsText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedBagItemId = reward.lootItem.id
                                subTab = "bag"
                                rewardPopupData = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🎒 В Сумку", color = HobbitGold, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { rewardPopupData = null },
                            colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Забрать ✨", color = HobbitCream, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("velaris_chantal_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Плашка со стрелками изменения характера (отключаемая)
        if (showTraitArrows && activeArrowToast != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HobbitElevatedBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, HobbitGold, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Изменение характера: $activeArrowToast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HobbitGold,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ЕДИНАЯ КОМПАКТНАЯ ШАПКА НАД СЦЕНОЙ С БУРГЕР-МЕНЮ (☰) СЛЕВА
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HobbitBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, HobbitGold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { isBurgerMenuOpen = !isBurgerMenuOpen }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBurgerMenuOpen) "✕" else "☰",
                                    color = HobbitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Column {
                            val sectionName = when (subTab) {
                                "den" -> "Домик"
                                "map" -> "Карта троп"
                                "bag" -> "Сумка находок"
                                "shop" -> "Лесная лавка"
                                "journal" -> "Летопись"
                                else -> "ИИ и Лор"
                            }
                            Text(
                                text = "🦊 Шанталь · $sectionName",
                                style = MaterialTheme.typography.titleMedium,
                                color = HobbitCream,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Серия $currentStreakDays дн. · ⚡ $currentEnergy Эн. · 🪙 $coins М",
                                style = MaterialTheme.typography.labelSmall,
                                color = HobbitMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (subTab != "den") {
                        OutlinedButton(
                            onClick = {
                                subTab = "den"
                                isBurgerMenuOpen = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🏠 В Домик", style = MaterialTheme.typography.labelSmall, color = HobbitGold, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Раскрывающееся Бургер-меню со всеми разделами (и дублирующие быстрые вкладки для удобства)
                AnimatedVisibility(visible = isBurgerMenuOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HobbitBg, RoundedCornerShape(14.dp))
                            .border(1.5.dp, HobbitGold, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val menuItems = listOf(
                            Triple("den", "🏠 Домик Шанталь", "Ур. $denTierNumber"),
                            Triple("map", "🗺️ Карта троп", "${DEFAULT_CHANTAL_MAP_REGIONS.count { totalJourneys >= it.minJourneysRequired }}/${DEFAULT_CHANTAL_MAP_REGIONS.size}"),
                            Triple("bag", "🎒 Сумка находок", "${inventoryList.size} предм."),
                            Triple("shop", "🛒 Лесная лавка", "$coins 🪙"),
                            Triple("journal", "📖 Летопись странствий", "${journalList.size} гл."),
                            Triple("ai", "⚙️ Настройки ИИ и Лора", selectedAiProvider)
                        )
                        menuItems.forEach { (id, title, badge) ->
                            val isCurrent = subTab == id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) HobbitElevatedBg else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        subTab = id
                                        isBurgerMenuOpen = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCurrent) HobbitGold else HobbitCream,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HobbitMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // АКТИВНЫЙ СВИТОК ИСТОРИИ ПОСЛЕ ПОХОДА (10 предложений + 3 кнопки + свободный текст)
        pendingEpisode?.let { ep ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, HobbitGold, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitElevatedBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (ep.isFiller) "Побочный эпизод (Филлер)" else "Основной сюжет · ${ep.regionTitleRu}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HobbitGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ep.titleRu,
                        style = MaterialTheme.typography.titleLarge,
                        color = HobbitCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Маршрут на карте: ${ep.mapNoteRu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HobbitEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitCardBg
                    ) {
                        Text(
                            text = ep.storyTextRu,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HobbitCream,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = "Добыча: ${ep.lootItem.emoji} «${ep.lootItem.titleRu}» · +${ep.coinsEarned} М",
                        style = MaterialTheme.typography.labelMedium,
                        color = HobbitGold,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Как ты отреагируешь на рассказ Шанталь?",
                        style = MaterialTheme.typography.labelSmall,
                        color = HobbitMuted
                    )
                    ep.choices.forEachIndexed { idx, opt ->
                        val arrows = opt.delta.formatArrowIndicators().joinToString(" · ")
                        Button(
                            onClick = {
                                onPlaySampleTone(SignalType.SET_COMPLETED)
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HobbitCardBg,
                                contentColor = HobbitCream
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${idx + 1}. ${opt.labelRu}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Свободный текст (Филлер)
                    OutlinedTextField(
                        value = fillerTextInput,
                        onValueChange = { fillerTextInput = it },
                        label = { Text("Или свой ответ / поручение (режим Филлера)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (fillerTextInput.isNotBlank()) {
                        Button(
                            onClick = {
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
                            colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отправить свой текст (Филлер)", color = HobbitCream, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 1: ДОМИК (ЧИСТАЯ ИЛЛЮСТРАЦИЯ + КРУГЛАЯ КНОПКА СЕРДЕЧКА + ШКАЛА ОПЫТА ПОД ПОРТРЕТОМ + 4 СЛОТА СНАРЯЖЕНИЯ)
        if (subTab == "den") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HobbitBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val mainCardResId = if (!stageInfo.isTransformed && totalDenItemsCount >= 10) {
                        denInteriorResId
                    } else {
                        portraitResId
                    }

                    // 1. ЧИСТАЯ КАРТИНКА БЕЗ ТЕКСТОВЫХ НАЛОЖЕНИЙ + МАЛЕНЬКАЯ КРУГЛАЯ КНОПКА СЕРДЕЧКА В УГЛУ
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("chantal_interactive_portrait")
                    ) {
                        Image(
                            painter = painterResource(id = mainCardResId),
                            contentDescription = stageInfo.titleRu,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isHeartPulseActive) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .border(1.5.dp, HobbitPink, RoundedCornerShape(14.dp))
                            ) {
                                Text(
                                    text = "♥ +${perks.tendernessMoodHealBonusPct}% к настроению · +3 XP взросления!",
                                    color = HobbitPink,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Маленькая круглая кнопка сердечка (активна 1 раз за цикл после возвращения из похода)
                        val canPet = !hasUsedPetThisCycle && activeJourneyStartMs == 0L
                        Surface(
                            shape = CircleShape,
                            color = if (canPet) Color(0xE62A161E) else Color(0xCC15100C),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(44.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (canPet) HobbitPink else HobbitBorder,
                                    shape = CircleShape
                                )
                                .clickable(enabled = canPet) {
                                    onPlaySampleTone(SignalType.RELAXATION)
                                    hasUsedPetThisCycle = true
                                    petTapCount += 1
                                    maturityXp += 3
                                    recentXpGainText = "+3 XP (Ласка ❤️)"
                                    moodPct = (moodPct + perks.tendernessMoodHealBonusPct).coerceAtMost(100)
                                    petDialogueBubble = traits.buildPettingReaction(stageInfo.isTransformed, petTapCount)
                                    isHeartPulseActive = true
                                }
                                .testTag("btn_pet_chantal")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "♥",
                                    color = if (canPet) HobbitPink else HobbitMuted.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // 2. ВИЗУАЛЬНЫЙ ПРОГРЕСС-БАР ОПЫТА И ВЗРОСЛЕНИЯ ПРЯМО ПОД ПОРТРЕТОМ
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HobbitBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🦊 ${stageInfo.titleRu}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = HobbitCream,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = HobbitCardBg,
                                        modifier = Modifier.border(1.dp, HobbitGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = "Ур. ${maturityProgress.maturityLevel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HobbitGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "✨ ${maturityProgress.xpInStage} / ${maturityProgress.stageTargetXp} XP (${maturityProgress.progressPct}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            recentXpGainText?.let { gainBadge ->
                                Text(
                                    text = "Начислено: $gainBadge",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitPink,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (maturityProgress.progressPct / 100f).coerceIn(0.03f, 1f) },
                                color = HobbitGold,
                                trackColor = HobbitBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🧭 Поход: +10 XP · ❤️ Ласка: +3 XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitMuted
                                )
                                Text(
                                    text = "До след. стадии: ${maturityProgress.xpToNextStage} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3. СТРУКТУРИРОВАННЫЕ ПУНКТЫ ВАЖНОЙ ИНФОРМАЦИИ (2x2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Стадия и Походы", style = MaterialTheme.typography.labelSmall, color = HobbitMuted)
                                Text(
                                    text = "🧭 Походов: $totalJourneys",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HobbitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    denAccordionTab = if (denAccordionTab == "housing") "none" else "housing"
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Настроение и Интерьер", style = MaterialTheme.typography.labelSmall, color = HobbitMuted)
                                Text(
                                    text = "💛 $moodPct% · 🏠 $itemsInCurrentStep из 10",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HobbitEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    petDialogueBubble?.let { bubble ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HobbitPink.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Ведущая черта: ${perks.dominantTraitNameRu} · +3 XP взросления",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitPink,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bubble,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HobbitCream
                                )
                            }
                        }
                    }

                    lastReactionBanner?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = HobbitEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Кнопки спойлеров: [ ✨ Характер ▼ ] и [ 🏠 Интерьер (X из 10) ▼ ] (Без статичной беседы у камина)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                denAccordionTab = if (denAccordionTab == "traits") "none" else "traits"
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (denAccordionTab == "traits") "✨ Характер ▲" else "✨ Характер ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (denAccordionTab == "traits") HobbitGold else HobbitCream,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                denAccordionTab = if (denAccordionTab == "housing") "none" else "housing"
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (denAccordionTab == "housing") "🏠 Интерьер ($itemsInCurrentStep из 10) ▲" else "🏠 Интерьер ($itemsInCurrentStep из 10) ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (denAccordionTab == "housing") HobbitEmerald else HobbitCream,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (denAccordionTab == "traits") {
                        val traitRows = listOf(
                            Triple("Любопытство", curiosity, "+${perks.curiosityBonusCoins} монет из тайников · ${perks.curiosityClueChancePct}% шанс улики"),
                            Triple("Нежность", tenderness, "+${perks.tendernessMoodHealBonusPct}% настроения при ласке дома"),
                            Triple("Игривость", playfulness, "${perks.playfulnessCharmOutOfDebtChancePct}% шанс обратить долг в шутку"),
                            Triple("Романтичность", romanticism, "+${perks.romanticismEveningCoinBonusPct}% монет в вечерних походах"),
                            Triple("Отвага", courage, "${perks.courageAvoidExhaustionChancePct}% защита от усталости 45 Эн."),
                            Triple("Остроумие", wit, "+${perks.witMerchantSellBonusPct}% к цене продажи диковинок")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            traitRows.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pair.forEach { (nameRu, score, perkText) ->
                                        val isPopupOpen = selectedTraitPopupName == nameRu
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isPopupOpen) HobbitElevatedBg else HobbitBg,
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    if (isPopupOpen) HobbitGold else HobbitBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    selectedTraitPopupName = if (isPopupOpen) null else nameRu
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = nameRu, style = MaterialTheme.typography.labelSmall, color = HobbitCream, fontWeight = FontWeight.Bold)
                                                    Text(text = "$score/100", style = MaterialTheme.typography.labelSmall, color = HobbitGold, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { (score / 100f).coerceIn(0.02f, 1f) },
                                                    color = HobbitGold,
                                                    trackColor = HobbitCardBg,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                )
                                                if (isPopupOpen) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = perkText, style = MaterialTheme.typography.labelSmall, color = HobbitEmerald)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (denAccordionTab == "housing") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HobbitEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏠 Интерьер · Уровень $denTierNumber",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HobbitEmerald,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF065F46),
                                        modifier = Modifier.border(1.dp, HobbitEmerald, RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = "$itemsInCurrentStep из 10",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HobbitCream,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { (itemsInCurrentStep / 10f).coerceIn(0.05f, 1f) },
                                    color = HobbitEmerald,
                                    trackColor = HobbitCardBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = "Куплено в лавке: $ownedDenCount · Принесено из походов: $foundDenTrophyCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitMuted
                                )
                            }
                        }
                    }

                    // 4. СБОР И ОТПРАВКА В ПОХОД (ОБЕРЕГ + 4 ЦВЕТНЫХ СЛОТА СНАРЯЖЕНИЯ + ЕДИНАЯ КНОПКА ЗАПУСКА)
                    val isJourneyActive = activeJourneyStartMs > 0L
                    if (isJourneyActive) {
                        val elapsedSec = ((nowMs - activeJourneyStartMs) / 1000L).coerceAtLeast(0L)
                        val remainSec = (3600L - elapsedSec).coerceAtLeast(0L)
                        val mm = remainSec / 60L
                        val ss = remainSec % 60L
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (activeStayedInSameRegion) {
                                        "Шанталь снова в «$activeRegionTitleRu» ($activeNpcRu)"
                                    } else {
                                        "Шанталь в походе: «$activeRegionTitleRu» ($activeNpcRu)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HobbitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "В пути (60 мин): %02d:%02d".format(mm, ss),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HobbitCream,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = activeAutonomousReasonRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HobbitMuted
                                )
                                Button(
                                    onClick = {
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
                                        activeJourneyStartMs = 0L
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Встретить Шанталь и прочитать историю", color = HobbitCream, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HobbitBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Компактная плашка Оберега (графически показывает +100% защиту и +50% монет)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (hasActiveRoadAmulet) Color(0xFF064E3B) else HobbitElevatedBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (hasActiveRoadAmulet) HobbitEmerald else HobbitBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            subTab = "bag"
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (hasActiveRoadAmulet) "🧿 Оберег активен: 🛡️ +100% защита · 🪙 +50% монет" else "🧿 Оберег: Пусто (смастерить в Сумке)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hasActiveRoadAmulet) HobbitCream else HobbitMuted,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "🛡️ $loadoutProtectPct% · 🪙 +$loadoutCoinsBonusPct%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HobbitGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // 4 разноцветных слота снаряжения без надписей «Слот 1 / Слот 2 / Налегке»
                                val outfitObj = DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedOutfitId && it.id in ownedShopItemIds }
                                val toolObj = DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedToolId && it.id in ownedShopItemIds }
                                val weaponObj = DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedWeaponId && it.id in ownedShopItemIds }
                                val bundleTitle = activeBundleShopItem?.shortTitleRu ?: activeBundleLootItem?.shortTitleRu ?: "Пусто"
                                val bundleEmoji = activeBundleShopItem?.emoji ?: activeBundleLootItem?.emoji ?: "🧺"

                                val slotCards = listOf(
                                    Triple("OUTFIT", outfitObj?.emoji ?: "🧥", outfitObj?.shortTitleRu ?: "Пусто"),
                                    Triple("TOOL", toolObj?.emoji ?: "🧭", toolObj?.shortTitleRu ?: "Пусто"),
                                    Triple("WEAPON", weaponObj?.emoji ?: "🏹", weaponObj?.shortTitleRu ?: "Пусто"),
                                    Triple("BUNDLE", bundleEmoji, bundleTitle)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    slotCards.forEach { (slotKey, emoji, label) ->
                                        val isPicked = activeLoadoutSlotPicker == slotKey
                                        val accentColor = when (slotKey) {
                                            "OUTFIT" -> HobbitSky
                                            "TOOL" -> HobbitGold
                                            "WEAPON" -> HobbitPink
                                            else -> HobbitEmerald
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isPicked) HobbitElevatedBg else HobbitCardBg,
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    width = if (isPicked) 2.dp else 1.dp,
                                                    color = if (isPicked || label != "Пусто") accentColor else HobbitBorder,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    activeLoadoutSlotPicker = if (isPicked) "NONE" else slotKey
                                                    activeSlotInspectItemId = null
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (label == "Пусто") HobbitMuted else HobbitCream,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // Всплывающая панель выбора предмета для нажатого слота (Рюкзачок + Лавка)
                                if (activeLoadoutSlotPicker != "NONE") {
                                    val slotItems = when (activeLoadoutSlotPicker) {
                                        "OUTFIT" -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.OUTFIT }
                                        "TOOL" -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.TOOL }
                                        "WEAPON" -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.WEAPON }
                                        else -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.BUNDLE }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(HobbitElevatedBg, RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            scoreSlotTitle(activeLoadoutSlotPicker)
                                            Text(
                                                text = "✕",
                                                color = HobbitMuted,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    activeLoadoutSlotPicker = "NONE"
                                                    activeSlotInspectItemId = null
                                                }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            slotItems.forEach { gear ->
                                                val isOwned = if (gear.category == ChantalShopCategory.CONSUMABLE) {
                                                    (pantryProvisionCounts[gear.id] ?: 0) > 0
                                                } else gear.id in ownedShopItemIds
                                                val isSelectedInspect = activeSlotInspectItemId == gear.id
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelectedInspect) HobbitCardBg else HobbitBg,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(
                                                            1.dp,
                                                            if (isSelectedInspect) HobbitGold else if (isOwned) HobbitEmerald else HobbitBorder,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            activeSlotInspectItemId = if (isSelectedInspect) null else gear.id
                                                        }
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(text = gear.emoji, style = MaterialTheme.typography.titleMedium)
                                                        Text(text = gear.shortTitleRu, style = MaterialTheme.typography.labelSmall, color = HobbitCream, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(
                                                            text = if (isOwned) "В сумке ✓" else "${gear.priceCoins} М",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isOwned) HobbitEmerald else HobbitGold,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        slotItems.find { it.id == activeSlotInspectItemId }?.let { inspectGear ->
                                            val isOwned = if (inspectGear.category == ChantalShopCategory.CONSUMABLE) {
                                                (pantryProvisionCounts[inspectGear.id] ?: 0) > 0
                                            } else inspectGear.id in ownedShopItemIds
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = HobbitCardBg,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.5.dp, HobbitGold, RoundedCornerShape(12.dp))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${inspectGear.emoji} ${inspectGear.titleRu}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = HobbitCream,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text(
                                                        text = inspectGear.gameplayEffectRu,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = HobbitEmerald,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        if (isOwned) {
                                                            Button(
                                                                onClick = {
                                                                    when (activeLoadoutSlotPicker) {
                                                                        "OUTFIT" -> equippedOutfitId = if (equippedOutfitId == inspectGear.id) null else inspectGear.id
                                                                        "TOOL" -> equippedToolId = if (equippedToolId == inspectGear.id) null else inspectGear.id
                                                                        "WEAPON" -> equippedWeaponId = if (equippedWeaponId == inspectGear.id) null else inspectGear.id
                                                                        else -> equippedBundleItemId = if (equippedBundleItemId == inspectGear.id) null else inspectGear.id
                                                                    }
                                                                    activeSlotInspectItemId = null
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = HobbitEmerald),
                                                                shape = RoundedCornerShape(10.dp)
                                                            ) {
                                                                Text("Взять / Снять", color = Color.White, fontWeight = FontWeight.Bold)
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = {
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
                                                                enabled = coins >= inspectGear.priceCoins,
                                                                colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange),
                                                                shape = RoundedCornerShape(10.dp)
                                                            ) {
                                                                Text("Купить · ${inspectGear.priceCoins} 🪙", color = HobbitCream, fontWeight = FontWeight.ExtraBold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Поле пожелания / поручения + Единая кнопка отправки в поход
                                OutlinedTextField(
                                    value = fillerTextInput,
                                    onValueChange = { fillerTextInput = it },
                                    label = { Text("🪶 Пожелание или поручение в дорогу (необязательно)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (currentEnergy >= requiredJourneyEnergy) {
                                                onPlaySampleTone(SignalType.CONTRACTION)
                                                spentEnergy += requiredJourneyEnergy
                                                isExhausted45 = false
                                                activeLoadoutSlotPicker = "NONE"

                                                val unlocked = DEFAULT_CHANTAL_MAP_REGIONS.filter { totalJourneys >= it.minJourneysRequired }
                                                val lastRegTitle = journalList.firstOrNull()?.regionTitleRu
                                                val lastReg = unlocked.find { it.titleRu == lastRegTitle }
                                                val staySame = lastReg != null && Random.nextFloat() < 0.45f
                                                val chosenReg = if (staySame && lastReg != null) lastReg else unlocked.random()
                                                val chosenNpc = chosenReg.npcsRu.random()
                                                val chosenThread = chosenReg.unfinishedThreadsRu.random()

                                                activeRegionId = chosenReg.id
                                                activeRegionTitleRu = chosenReg.titleRu
                                                activeNpcRu = chosenNpc
                                                activeStayedInSameRegion = staySame
                                                activeAutonomousReasonRu = if (staySame) {
                                                    "Шанталь решила снова вернуться в «${chosenReg.titleRu}», так как у неё там остались незаконченные дела ($chosenThread) и встреча с персонажем $chosenNpc."
                                                } else {
                                                    "Шанталь выбрала на карте «${chosenReg.titleRu}», чтобы $chosenThread и встретиться с персонажем $chosenNpc."
                                                }
                                                activeIsFiller = fillerTextInput.isNotBlank()
                                                activeFillerPrompt = fillerTextInput
                                                fillerTextInput = ""
                                                activeJourneyStartMs = System.currentTimeMillis()
                                            }
                                        },
                                        enabled = currentEnergy >= requiredJourneyEnergy,
                                        colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_start_chantal_journey")
                                    ) {
                                        Text(
                                            text = "Отправить в поход · ⚡ $requiredJourneyEnergy",
                                            color = HobbitCream,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onOpenWorkoutPlan,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            text = "+10 Эн. (Кегель)",
                                            color = HobbitEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 2: ПЕРГАМЕНТНАЯ КАРТА ТРОП
        if (subTab == "map") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HobbitBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Пергаментная карта странствий Шанталь",
                        style = MaterialTheme.typography.titleMedium,
                        color = HobbitCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Image(
                        painter = painterResource(id = R.drawable.chantal_map),
                        contentDescription = "Пергаментная карта мира Шанталь",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    DEFAULT_CHANTAL_MAP_REGIONS.forEach { reg ->
                        val unlocked = totalJourneys >= reg.minJourneysRequired
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (unlocked) "${reg.emoji} ${reg.titleRu}" else "🔒 ${reg.titleRu} (с ${reg.minJourneysRequired} пох.)",
                                    color = if (unlocked) HobbitGold else HobbitMuted,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(text = reg.loreRu, style = MaterialTheme.typography.bodySmall, color = HobbitCream)
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 3: СУМКА НАХОДОК (С ПЛАВНОЙ АНИМАЦИЕЙ ВЫПАДЕНИЯ ИКОНОК В СЕТКУ И КАРТОЧКОЙ Сбоку/Снизу)
        if (subTab == "bag") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HobbitBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val herbs = inventoryList.filter { it.category == ChantalLootCategory.HERB }
                    val trinkets = inventoryList.filter { it.category == ChantalLootCategory.TRINKET }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎒 Сумка находок",
                            style = MaterialTheme.typography.titleMedium,
                            color = HobbitCream,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Предметов: ${inventoryList.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = HobbitGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Сетка предметов с анимацией мягкого выпадения (BagDropSlotItem)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        inventoryList.chunked(3).forEachIndexed { rowIdx, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEachIndexed { colIdx, item ->
                                    val globalIdx = rowIdx * 3 + colIdx
                                    val isSelected = selectedBagItemId == item.id
                                    val isNew = item.id in newlyAddedBagItemIds
                                    BagDropSlotTile(
                                        item = item,
                                        index = globalIdx,
                                        isSelected = isSelected,
                                        isNew = isNew,
                                        onClick = {
                                            selectedBagItemId = if (isSelected) null else item.id
                                            newlyAddedBagItemIds.remove(item.id)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Всплывающая карточка выбранного предмета (при повторном нажатии закрывается)
                    inventoryList.find { it.id == selectedBagItemId }?.let { item ->
                        val witPrice = (item.sellPriceCoins * (100 + perks.witMerchantSellBonusPct)) / 100
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, HobbitGold, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.emoji} ${item.titleRu}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HobbitCream,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "✕",
                                        color = HobbitMuted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { selectedBagItemId = null }
                                    )
                                }
                                Text(
                                    text = "«${item.descriptionRu}»",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HobbitMuted
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    when (item.category) {
                                        ChantalLootCategory.ROYAL_CLUE -> {
                                            Button(
                                                onClick = {
                                                    if (!item.inspectedBonusClaimed) {
                                                        curiosity = (curiosity + 1).coerceAtMost(100)
                                                        val idx = inventoryList.indexOfFirst { it.id == item.id }
                                                        if (idx >= 0) inventoryList[idx] = item.copy(inspectedBonusClaimed = true)
                                                        activeArrowToast = "Любопытство ↑"
                                                    }
                                                    selectedBagItemId = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = HobbitGold)
                                            ) {
                                                Text(
                                                    text = if (item.inspectedBonusClaimed) "✓ Изучено" else "Изучить (+1 Любопытство)",
                                                    color = HobbitBg,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                        ChantalLootCategory.DEN_TROPHY -> {
                                            Text(
                                                text = "✓ Стоит в Домике (+1 из 10 к интерьеру)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = HobbitEmerald,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        ChantalLootCategory.HERB -> {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        isExhausted45 = false
                                                        moodPct = (moodPct + 15).coerceAtMost(100)
                                                        inventoryList.removeAll { it.id == item.id }
                                                        selectedBagItemId = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitEmerald)
                                                ) {
                                                    Text("🌿 Заварить", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        coins += witPrice
                                                        inventoryList.removeAll { it.id == item.id }
                                                        selectedBagItemId = null
                                                    }
                                                ) {
                                                    Text("Продать · +$witPrice М", color = HobbitGold)
                                                }
                                            }
                                        }
                                        ChantalLootCategory.TRINKET -> {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        coins += 18
                                                        tenderness = (tenderness + 1).coerceAtMost(100)
                                                        activeArrowToast = "Нежность ↑"
                                                        inventoryList.removeAll { it.id == item.id }
                                                        selectedBagItemId = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange)
                                                ) {
                                                    Text("🎁 Подарить (+18 М)", color = HobbitCream, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        coins += witPrice
                                                        inventoryList.removeAll { it.id == item.id }
                                                        selectedBagItemId = null
                                                    }
                                                ) {
                                                    Text("Продать · +$witPrice М", color = HobbitGold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Домашняя Алхимия у камина
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "⚗️ Алхимия у камина (1 Трава + 1 Находка = Оберег Попутного Ветра)",
                                style = MaterialTheme.typography.labelMedium,
                                color = HobbitEmerald,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Button(
                                onClick = {
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
                                enabled = herbs.isNotEmpty() && (trinkets.isNotEmpty() || herbs.size >= 2),
                                colors = ButtonDefaults.buttonColors(containerColor = HobbitEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Смастерить Оберег", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 4: ЛЕСНАЯ ЛАВКА (ИНТЕРЬЕР X ИЗ 10 + СНАРЯЖЕНИЕ + КАРТОЧКА КУПИТЬ СПРАВА/ВНИЗУ)
        if (subTab == "shop") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HobbitBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Лесная лавка",
                            style = MaterialTheme.typography.titleMedium,
                            color = HobbitCream,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "🪙 $coins Монет",
                            style = MaterialTheme.typography.labelLarge,
                            color = HobbitGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Лавка на колёсах (иллюстрация + 3 редких товара со всплывающей кнопкой Купить)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.chantal_merchant_chest),
                            contentDescription = "Лавка на колёсах",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Surface(
                            color = HobbitGold,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Лавка на колёсах",
                                style = MaterialTheme.typography.labelSmall,
                                color = HobbitBg,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DEFAULT_CHANTAL_MERCHANT_OFFERS.forEach { offer ->
                            val isSelected = selectedShopEntryId == offer.id
                            val isClaimed = offer.id in claimedMerchantOfferIds
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) HobbitElevatedBg else HobbitBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) HobbitGold else HobbitBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedShopEntryId = if (isSelected) null else offer.id
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = offer.emoji, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = offer.shortTitleRu,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HobbitCream,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isClaimed) "Куплено ✓" else "${offer.discountPriceCoins} М",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isClaimed) HobbitEmerald else HobbitGold,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Переключатель «🏠 Интерьер (X из 10)» и «🎒 Снаряжение»
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                shopCategoryTab = "den_comfort"
                                selectedShopEntryId = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (shopCategoryTab == "den_comfort") HobbitEmerald else HobbitElevatedBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🏠 Интерьер ($itemsInCurrentStep из 10)", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = {
                                shopCategoryTab = "expedition"
                                selectedShopEntryId = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (shopCategoryTab == "expedition") HobbitOrange else HobbitElevatedBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🎒 Снаряжение", color = HobbitCream, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (shopCategoryTab == "den_comfort") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "Ур. I→II", 2 to "Ур. II→III", 3 to "Ур. III→IV").forEach { (stepNum, label) ->
                                OutlinedButton(
                                    onClick = {
                                        shopDenStepFilter = stepNum
                                        selectedShopEntryId = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (shopDenStepFilter == stepNum) HobbitEmerald else HobbitMuted,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    val displayedShopItems = if (shopCategoryTab == "den_comfort") {
                        DEFAULT_CHANTAL_SHOP_ITEMS.filter {
                            it.category == ChantalShopCategory.DEN_COMFORT && it.denStep == shopDenStepFilter
                        }
                    } else {
                        DEFAULT_CHANTAL_SHOP_ITEMS.filter {
                            it.category != ChantalShopCategory.DEN_COMFORT
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        displayedShopItems.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val isSel = selectedShopEntryId == item.id
                                    val isOwned = item.id in ownedShopItemIds
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSel) HobbitElevatedBg else HobbitBg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (isSel) 2.dp else 1.dp,
                                                color = if (isSel) HobbitGold else if (isOwned) HobbitEmerald else HobbitBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                // Повторное нажатие на ту же иконку закрывает карточку Купить
                                                selectedShopEntryId = if (isSel) null else item.id
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = item.emoji, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = item.shortTitleRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HobbitCream,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (isOwned && item.category != ChantalShopCategory.CONSUMABLE) "Куплено ✓" else "${item.priceCoins} М",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isOwned && item.category != ChantalShopCategory.CONSUMABLE) HobbitEmerald else HobbitGold,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Всплывающая карточка с кнопкой «Купить» справа внизу (исчезает после покупки или повторного нажатия)
                    DEFAULT_CHANTAL_MERCHANT_OFFERS.find { it.id == selectedShopEntryId }?.let { offer ->
                        val isClaimed = offer.id in claimedMerchantOfferIds
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, HobbitGold, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${offer.emoji} ${offer.titleRu}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HobbitCream,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(text = offer.effectRu, style = MaterialTheme.typography.bodySmall, color = HobbitEmerald)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            if (!isClaimed && coins >= offer.discountPriceCoins) {
                                                coins -= offer.discountPriceCoins
                                                claimedMerchantOfferIds.add(offer.id)
                                                if (offer.rewardType == "DEN_TROPHY") {
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
                                                } else if (offer.rewardType == "ROYAL_CLUE") {
                                                    curiosity = (curiosity + 2).coerceAtMost(100)
                                                } else {
                                                    hasActiveRoadAmulet = true
                                                }
                                                selectedShopEntryId = null
                                            }
                                        },
                                        enabled = !isClaimed && coins >= offer.discountPriceCoins,
                                        colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange)
                                    ) {
                                        Text(
                                            text = if (isClaimed) "Куплено ✓" else "Купить · ${offer.discountPriceCoins} 🪙",
                                            color = HobbitCream,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == selectedShopEntryId }?.let { item ->
                        val owned = item.id in ownedShopItemIds
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, HobbitGold, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.emoji} ${item.titleRu}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HobbitCream,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "✕",
                                        color = HobbitMuted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { selectedShopEntryId = null }
                                    )
                                }
                                Text(
                                    text = item.gameplayEffectRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "«${item.descriptionRu}»",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HobbitMuted
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            if (coins >= item.priceCoins) {
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
                                        enabled = coins >= item.priceCoins && (!owned || item.category == ChantalShopCategory.CONSUMABLE),
                                        colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange)
                                    ) {
                                        Text(
                                            text = if (owned && item.category != ChantalShopCategory.CONSUMABLE) "Куплено ✓" else "Купить · ${item.priceCoins} 🪙",
                                            color = HobbitCream,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 5: ЛЕТОПИСЬ СТРАНСТВИЙ
        if (subTab == "journal") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HobbitBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📖 Летопись странствий (Трёхслойная Память Хроник)",
                        style = MaterialTheme.typography.titleMedium,
                        color = HobbitCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Слой 1 (Вечная летопись с Похода #1):",
                                style = MaterialTheme.typography.labelSmall,
                                color = HobbitGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• [Поход #1] Старая мельница | Барсук Брамбл -> «Спрятать медальон в шкатулку тайн»\n• [Поход #2] Янтарная дубрава | Ворон Корвус -> «Попросить приносить лесные письма»\n• [Поход #3] Старая мельница | Перевозчик Хьюго -> «Пообещать найти ключ от ларца»",
                                style = MaterialTheme.typography.bodySmall,
                                color = HobbitMuted
                            )
                        }
                    }
                    journalList.forEachIndexed { idx, entry ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${if (idx < 3) "★ " else "✓ "}${entry.titleRu}",
                                    color = HobbitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(text = entry.storyTextRu, style = MaterialTheme.typography.bodySmall, color = HobbitCream)
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 6: НАСТРОЙКИ ИИ И ЛОРА
        if (subTab == "ai") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HobbitBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Шлюз нейросети: Gemini, OpenRouter или Grok",
                        style = MaterialTheme.typography.titleMedium,
                        color = HobbitCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChantalAiProvider.entries.forEach { prov ->
                            Button(
                                onClick = { selectedAiProvider = prov.code },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedAiProvider == prov.code) HobbitOrange else HobbitElevatedBg
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(prov.titleRu, color = HobbitCream, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (selectedAiProvider == ChantalAiProvider.OPENROUTER.code) {
                        OPENROUTER_ANDROID_MODELS.forEach { (modelId, modelLabel) ->
                            val isChosen = customOpenRouterModelInput.isBlank() && selectedOpenRouterModel == modelId
                            OutlinedButton(
                                onClick = {
                                    selectedOpenRouterModel = modelId
                                    customOpenRouterModelInput = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${if (isChosen) "✓ " else ""}$modelLabel",
                                    color = if (isChosen) HobbitGold else HobbitCream
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customOpenRouterModelInput,
                            onValueChange = { customOpenRouterModelInput = it },
                            label = { Text("Или свой ID модели с OpenRouter") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChantalStoryDifficulty.entries.forEach { diff ->
                            OutlinedButton(
                                onClick = { selectedDifficulty = diff.code },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(
                                    text = diff.titleRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedDifficulty == diff.code) HobbitGold else HobbitMuted
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customLoreNotes,
                        onValueChange = { customLoreNotes = it },
                        label = { Text("Заметки лора и персонализации для ИИ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun scoreSlotTitle(slotKey: String) {
    val label = when (slotKey) {
        "OUTFIT" -> "🧥 Одежда в поход"
        "TOOL" -> "🧭 Инструмент следопыта"
        "WEAPON" -> "🏹 Оружие и защита"
        else -> "🧺 Узелок в дорогу"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = HobbitGold,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun BagDropSlotTile(
    item: ChantalInventoryLootItem,
    index: Int,
    isSelected: Boolean,
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dropOffsetY = remember(item.id) { Animatable(-24f) }
    val iconScale = remember(item.id) { Animatable(0.75f) }

    LaunchedEffect(item.id) {
        delay((index * 45L).coerceAtMost(320L))
        dropOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(item.id) {
        delay((index * 45L + 50L).coerceAtMost(370L))
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val borderColor = when {
        isSelected -> HobbitGold
        isNew -> HobbitGold
        item.category == ChantalLootCategory.DEN_TROPHY -> HobbitPink
        item.category == ChantalLootCategory.HERB -> HobbitEmerald
        item.category == ChantalLootCategory.ROYAL_CLUE -> HobbitGold
        else -> HobbitBorder
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) HobbitElevatedBg else HobbitBg,
        modifier = modifier
            .offset { IntOffset(0, dropOffsetY.value.roundToInt()) }
            .border(
                width = if (isSelected || isNew) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isNew) {
                Text(
                    text = "✦ Нов.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HobbitGold,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = item.emoji,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.scale(iconScale.value)
            )
            Text(
                text = item.shortTitleRu,
                style = MaterialTheme.typography.labelSmall,
                color = HobbitCream,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
