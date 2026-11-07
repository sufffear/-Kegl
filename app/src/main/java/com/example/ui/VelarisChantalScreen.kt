package com.example.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ChantalAiProvider
import com.example.domain.ChantalChoiceOption
import com.example.domain.ChantalInventoryLootItem
import com.example.domain.ChantalLootCategory
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
import com.example.domain.resolveChantalStageStatus
import kotlinx.coroutines.delay
import kotlin.random.Random

private val HobbitCardBg = Color(0xFF1E1712)
private val HobbitElevatedBg = Color(0xFF281F18)
private val HobbitBorder = Color(0xFF4D3D2C)
private val HobbitGold = Color(0xFFF59E0B)
private val HobbitEmerald = Color(0xFF10B981)
private val HobbitOrange = Color(0xFFEA580C)
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
    var hasCompletedPrologue by rememberSaveable { mutableStateOf(false) }
    var totalJourneys by rememberSaveable { mutableIntStateOf(3) }
    var forceTransformPreview by rememberSaveable { mutableStateOf(false) }
    var showDenInteriorPanorama by rememberSaveable { mutableStateOf(false) }
    var hasActiveRoadAmulet by rememberSaveable { mutableStateOf(false) }
    var claimedWorkoutDisciplinePraise by rememberSaveable { mutableStateOf(false) }

    // Энергия и Монеты (каждая тренировка Кегеля даёт +10 Энергии)
    var bonusEnergy by rememberSaveable { mutableIntStateOf(60) }
    var spentEnergy by rememberSaveable { mutableIntStateOf(0) }
    val currentEnergy = ((completedWorkoutsCount * 10) + bonusEnergy - spentEnergy).coerceAtLeast(0)
    var coins by rememberSaveable { mutableIntStateOf(95) }

    // 6 параметров характера
    var curiosity by rememberSaveable { mutableIntStateOf(14) }
    var tenderness by rememberSaveable { mutableIntStateOf(16) }
    var playfulness by rememberSaveable { mutableIntStateOf(12) }
    var romanticism by rememberSaveable { mutableIntStateOf(10) }
    var courage by rememberSaveable { mutableIntStateOf(11) }
    var wit by rememberSaveable { mutableIntStateOf(13) }

    val traits = ChantalTraits(
        curiosity = curiosity,
        tenderness = tenderness,
        playfulness = playfulness,
        romanticism = romanticism,
        courage = courage,
        wit = wit
    )
    val perks = remember(traits) { traits.computePerks() }
    val stageInfo = remember(totalJourneys, forceTransformPreview) {
        resolveChantalStageStatus(totalJourneys, forceTransformPreview)
    }

    var moodPct by rememberSaveable { mutableIntStateOf(88) }
    var isExhausted45 by rememberSaveable { mutableStateOf(false) }
    var debtCoins by rememberSaveable { mutableIntStateOf(0) }
    var debtReasonRu by rememberSaveable { mutableStateOf("") }

    var showTraitArrows by rememberSaveable { mutableStateOf(true) }
    var activeArrowToast by remember { mutableStateOf<String?>(null) }
    var lastReactionBanner by remember { mutableStateOf<String?>(null) }
    var petTapCount by rememberSaveable { mutableIntStateOf(0) }
    var petDialogueBubble by remember { mutableStateOf<String?>(null) }

    // Вкладки внутри мира Шанталь
    var subTab by rememberSaveable { mutableStateOf("den") }

    // Активный часовой поход
    var activeJourneyStartMs by rememberSaveable { mutableLongStateOf(0L) }
    var activeRegionId by rememberSaveable { mutableStateOf("") }
    var activeRegionTitleRu by rememberSaveable { mutableStateOf("") }
    var activeNpcRu by rememberSaveable { mutableStateOf("") }
    var activeStayedInSameRegion by rememberSaveable { mutableStateOf(false) }
    var activeAutonomousReasonRu by rememberSaveable { mutableStateOf("") }
    var activeIsFiller by rememberSaveable { mutableStateOf(false) }
    var activeFillerPrompt by rememberSaveable { mutableStateOf("") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeJourneyStartMs) {
        while (activeJourneyStartMs > 0L) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Непрочитанная история после возвращения
    var pendingEpisode by remember { mutableStateOf<ChantalStoryEpisode?>(null) }
    var fillerTextInput by rememberSaveable { mutableStateOf("") }
    var customLoreNotes by rememberSaveable { mutableStateOf("") }
    var selectedAiProvider by rememberSaveable { mutableStateOf(ChantalAiProvider.GEMINI.code) }
    var selectedOpenRouterModel by rememberSaveable { mutableStateOf("google/gemini-2.5-flash") }
    var customOpenRouterModelInput by rememberSaveable { mutableStateOf("") }
    var selectedDifficulty by rememberSaveable { mutableStateOf(ChantalStoryDifficulty.MEDIUM.code) }

    val ownedShopItemIds = remember {
        mutableStateListOf("den_heather_blanket", "gear_knitted_scarf")
    }

    val inventoryList = remember {
        mutableStateListOf(
            ChantalInventoryLootItem(
                id = "clue_1",
                titleRu = "Медальон с полустёртым гербом лилии",
                category = ChantalLootCategory.ROYAL_CLUE,
                descriptionRu = "Старинный серебряный жетон, который был у лисёнка в первый вечер. Хранится в Шкатулке королевских улик.",
                sellPriceCoins = 40,
                foundAtRegionRu = "Старая водяная мельница",
                foundDateIso = "2026-11-05"
            ),
            ChantalInventoryLootItem(
                id = "herb_1",
                titleRu = "Пучок душистого солнечного чабреца",
                category = ChantalLootCategory.HERB,
                descriptionRu = "Согревающая лесная трава от барсука Брамбла. Снимает усталость (45 Энергии) или идёт на крафт оберега!",
                sellPriceCoins = 18,
                foundAtRegionRu = "Старая водяная мельница",
                foundDateIso = "2026-11-06"
            ),
            ChantalInventoryLootItem(
                id = "trinket_1",
                titleRu = "Светящаяся шишка из Янтарной дубравы",
                category = ChantalLootCategory.TRINKET,
                descriptionRu = "Тёплая кедровая шишка. Можно продать, подарить другу-NPC или соединить с травой в оберег!",
                sellPriceCoins = 22,
                foundAtRegionRu = "Янтарная дубрава",
                foundDateIso = "2026-11-06"
            )
        )
    }

    val journalList = remember { mutableStateListOf<ChantalStoryEpisode>() }

    val ownedDenCount = DEFAULT_CHANTAL_SHOP_ITEMS.count {
        it.category == ChantalShopCategory.DEN_COMFORT && it.id in ownedShopItemIds
    }
    val denTierNumber = when {
        ownedDenCount >= 4 -> 3
        ownedDenCount >= 2 -> 2
        else -> 1
    }
    val denInteriorResId = when (denTierNumber) {
        3 -> R.drawable.chantal_den_tier3
        2 -> R.drawable.chantal_den_tier2
        else -> R.drawable.chantal_stage1
    }

    val comfortDiscount = DEFAULT_CHANTAL_SHOP_ITEMS
        .filter { it.id in ownedShopItemIds }
        .sumOf { it.reducesExhaustionCostBy }
    val requiredJourneyEnergy = if (isExhausted45) (45 - comfortDiscount).coerceAtLeast(30) else 30

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
                    text = "Каждая тренировка Кегеля даёт +10 Энергии. За 30 Энергии Шанталь сама выбирает маршрут на карте, отправляется в часовой поход и приносит истории, монеты и находки!",
                    style = MaterialTheme.typography.bodySmall,
                    color = HobbitEmerald,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = {
                        onPlaySampleTone(SignalType.SESSION_COMPLETE)
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

        // Верхняя карточка валют и вкладок
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stageInfo.titleRu,
                            style = MaterialTheme.typography.titleMedium,
                            color = HobbitCream,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Попутный ветер серии: +$streakBonusPct% монет · Походов: $totalJourneys",
                            style = MaterialTheme.typography.labelSmall,
                            color = HobbitMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HobbitElevatedBg
                        ) {
                            Text(
                                text = "⚡ $currentEnergy Эн.",
                                style = MaterialTheme.typography.labelLarge,
                                color = HobbitEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HobbitElevatedBg
                        ) {
                            Text(
                                text = "🪙 $coins М",
                                style = MaterialTheme.typography.labelLarge,
                                color = HobbitGold,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Навигация внутри мира Шанталь
                val tabs = listOf(
                    "den" to "Домик",
                    "map" to "Карта",
                    "bag" to "Сумка (${inventoryList.size})",
                    "shop" to "Лавка",
                    "journal" to "Дневник (${journalList.size})",
                    "ai" to "ИИ и Лор"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.take(3).forEach { (id, label) ->
                            val selected = subTab == id
                            OutlinedButton(
                                onClick = { subTab = id },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) HobbitElevatedBg else Color.Transparent,
                                    contentColor = if (selected) HobbitGold else HobbitMuted
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.drop(3).forEach { (id, label) ->
                            val selected = subTab == id
                            OutlinedButton(
                                onClick = { subTab = id },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) HobbitElevatedBg else Color.Transparent,
                                    contentColor = if (selected) HobbitGold else HobbitMuted
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                        text = if (ep.isFiller) "Побочный эпизод (Филлер — без изменения характера)" else "Основной сюжет · ${ep.regionTitleRu}",
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
                        text = "Принесена находка: «${ep.lootItem.titleRu}» · Монеты: +${ep.coinsEarned}",
                        style = MaterialTheme.typography.labelMedium,
                        color = HobbitGold,
                        fontWeight = FontWeight.Bold
                    )
                    if (ep.badNewsRu.isNotBlank()) {
                        Text(
                            text = "Плохие новости: ${ep.badNewsRu}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Выбери сюжетный ответ (меняет характер и запоминается в Дневнике):",
                        style = MaterialTheme.typography.labelSmall,
                        color = HobbitMuted
                    )
                    ep.choices.forEachIndexed { idx, opt ->
                        val arrows = opt.delta.formatArrowIndicators().joinToString(" · ")
                        Button(
                            onClick = {
                                onPlaySampleTone(SignalType.SESSION_COMPLETE)
                                inventoryList.add(0, ep.lootItem)
                                coins += ep.coinsEarned
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
                                journalList.add(0, ep.copy(chosenActionSummaryRu = opt.labelRu))
                                lastReactionBanner = opt.reactionRu
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
                                if (showTraitArrows && arrows.isNotBlank()) {
                                    Text(
                                        text = arrows,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HobbitGold
                                    )
                                }
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
                                onPlaySampleTone(SignalType.SESSION_COMPLETE)
                                inventoryList.add(0, ep.lootItem)
                                coins += ep.coinsEarned
                                if (debtCoins > 0) {
                                    debtCoins = 0
                                    debtReasonRu = ""
                                }
                                journalList.add(
                                    0,
                                    ep.copy(
                                        isFiller = true,
                                        chosenActionSummaryRu = "Свой текст: $fillerTextInput"
                                    )
                                )
                                lastReactionBanner = "Филлер-ответ («$fillerTextInput») принят без изменения характера!"
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

        // ВКЛАДКА 1: ДОМИК, ИНТЕРЬЕР УРОВНЯ I-III, 6 ЧЕРТ С БОНУСАМИ И ОТПРАВКА В ПОХОД
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Переключатель: Портрет Шанталь / Панорама Интерьера Домика (Ур. 1-3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDenInteriorPanorama = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (!showDenInteriorPanorama) "✓ Портрет Шанталь" else "Портрет Шанталь",
                                color = if (!showDenInteriorPanorama) HobbitGold else HobbitMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { showDenInteriorPanorama = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (showDenInteriorPanorama) "✓ Домик (Ур. $denTierNumber/3)" else "Домик (Ур. $denTierNumber/3)",
                                color = if (showDenInteriorPanorama) HobbitEmerald else HobbitMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Интерактивный портрет Шанталь ИЛИ иллюстрация обустроенного Домика (Ур. 1 / 2 / 3)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onPlaySampleTone(SignalType.RELAXATION)
                                petTapCount += 1
                                moodPct = (moodPct + perks.tendernessMoodHealBonusPct).coerceAtMost(100)
                                petDialogueBubble = traits.buildPettingReaction(stageInfo.isTransformed, petTapCount)
                            }
                            .testTag("chantal_interactive_portrait")
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (showDenInteriorPanorama) denInteriorResId else portraitResId
                            ),
                            contentDescription = stageInfo.titleRu,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (showDenInteriorPanorama) {
                                    "Интерьер Домика Ур. $denTierNumber/3 (предметов уюта: $ownedDenCount)"
                                } else {
                                    "Настроение $moodPct% · Нажми, чтобы погладить ♥"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = HobbitCream,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onPlaySampleTone(SignalType.RELAXATION)
                            petTapCount += 1
                            moodPct = (moodPct + perks.tendernessMoodHealBonusPct).coerceAtMost(100)
                            petDialogueBubble = traits.buildPettingReaction(stageInfo.isTransformed, petTapCount)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HobbitElevatedBg,
                            contentColor = HobbitCream
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_pet_chantal")
                    ) {
                        Text(
                            text = if (stageInfo.isTransformed) {
                                "♥ Поговорить с Шанталь у камина (+${perks.tendernessMoodHealBonusPct}% настр.)"
                            } else {
                                "♥ Погладить Шанталь за рыжим ушком (+${perks.tendernessMoodHealBonusPct}% настр.)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Живая реакция на реальные тренировки Кегеля
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Живая связь с тренировками Кегеля (выполнено: $completedWorkoutsCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = HobbitEmerald,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Button(
                                onClick = {
                                    if (!claimedWorkoutDisciplinePraise) {
                                        onPlaySampleTone(SignalType.SESSION_COMPLETE)
                                        claimedWorkoutDisciplinePraise = true
                                        coins += 15
                                        bonusEnergy += 5
                                        courage += 1
                                        activeArrowToast = "Отвага ↑"
                                        lastReactionBanner = "Шанталь гордится твоей дисциплиной! +15 Монет, +5 Энергии и Отвага ↑"
                                    }
                                },
                                enabled = !claimedWorkoutDisciplinePraise,
                                colors = ButtonDefaults.buttonColors(containerColor = HobbitEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (claimedWorkoutDisciplinePraise) {
                                        "✓ Бонус дисциплины за тренировки получен"
                                    } else {
                                        "Забрать похвалу от Шанталь (+15 М · +5 Эн. · Отвага ↑)"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    petDialogueBubble?.let { bubble ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Ведущая черта: ${perks.dominantTraitNameRu}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitGold,
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

                    Text(
                        text = stageInfo.nextMilestoneHintRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = HobbitGold,
                        fontWeight = FontWeight.Bold
                    )

                    // Отправка в поход (таймер 60 минут) или завершение похода (с поддержкой Юбилейных Сундуков Глав!)
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
                                        "Шанталь снова в локации «$activeRegionTitleRu» (незаконченное дело · $activeNpcRu)"
                                    } else {
                                        "Шанталь в походе: «$activeRegionTitleRu» ($activeNpcRu)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HobbitGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Таймер похода (60 мин): %02d:%02d".format(mm, ss),
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
                                        val memoryNote = if (journalList.isNotEmpty()) {
                                            "Помня события прошлого похода («${journalList.first().titleRu}»), "
                                        } else ""
                                        val baseReward = if (isMilestone) 60 else 28
                                        val amuletBonus = if (hasActiveRoadAmulet) 150 else 100
                                        val coinsFound = (((baseReward + perks.curiosityBonusCoins) * (100 + streakBonusPct) * amuletBonus) / 10000)
                                        hasActiveRoadAmulet = false
                                        val epChoices = mutableListOf(
                                            ChantalChoiceOption(
                                                labelRu = "Ласково встретить у камина и расспросить о деталях",
                                                reactionRu = "Шанталь с теплом откликается на твою заботу!",
                                                delta = ChantalTraits(1, 2, 0, 1, 0, 0)
                                            ),
                                            ChantalChoiceOption(
                                                labelRu = "Подшутить и устроить весёлую игру с находкой",
                                                reactionRu = "Она радостно фыркает и включается в игру!",
                                                delta = ChantalTraits(0, 0, 2, 0, 1, 2)
                                            ),
                                            ChantalChoiceOption(
                                                labelRu = "Внимательно изучить зацепки на карте",
                                                reactionRu = "Шанталь сосредоточенно садится рядом над картой.",
                                                delta = ChantalTraits(2, 0, 0, 0, 2, 1)
                                            )
                                        )
                                        if (isMilestone) {
                                            epChoices.add(
                                                ChantalChoiceOption(
                                                    labelRu = "★ Открыть потайное дно Юбилейного Сундука Главы #$nextNum",
                                                    reactionRu = "Вы открыли кованый Сундук Главы с королевской реликвией и двойной наградой!",
                                                    delta = ChantalTraits(2, 1, 1, 1, 2, 1)
                                                )
                                            )
                                        }
                                        pendingEpisode = ChantalStoryEpisode(
                                            id = "ep_${System.currentTimeMillis()}",
                                            titleRu = if (isMilestone) {
                                                "★ Юбилейный Сундук Главы #$nextNum в «$activeRegionTitleRu»"
                                            } else if (activeStayedInSameRegion) {
                                                "Снова в «$activeRegionTitleRu»: продолжение дела"
                                            } else {
                                                "Приключение в «$activeRegionTitleRu»"
                                            },
                                            storyTextRu = "${memoryNote}Шанталь самостоятельно выбрала маршрут в локацию «$activeRegionTitleRu», где её уже ждал $activeNpcRu. По дороге она обследовала старый мост и нашла спрятанный тайник под корнями векового дерева. Собеседник поделился с ней важными слухами о старом королевстве и угостил тёплым дорожным хлебом. Взамен Шанталь помогла ему разобраться с запутанной загадкой, проявив свою ведущую черту — ${perks.dominantTraitNameRu}. Домой она вернулась с полной сумкой новостей, звонкими монетами и удивительной находкой!",
                                            mapNoteRu = activeAutonomousReasonRu,
                                            regionTitleRu = activeRegionTitleRu,
                                            npcNameRu = activeNpcRu,
                                            stayedInSameRegion = activeStayedInSameRegion,
                                            isFiller = activeIsFiller,
                                            lootItem = ChantalInventoryLootItem(
                                                id = "loot_${System.currentTimeMillis()}",
                                                titleRu = if (isMilestone) "Королевская печать Главы #$nextNum" else "Резной жетон из «$activeRegionTitleRu»",
                                                category = if (isMilestone || totalJourneys % 3 == 0) ChantalLootCategory.ROYAL_CLUE else ChantalLootCategory.TRINKET,
                                                descriptionRu = "Памятная находка, которую Шанталь принесла со встречи с персонажем $activeNpcRu.",
                                                sellPriceCoins = 24,
                                                foundAtRegionRu = activeRegionTitleRu,
                                                foundDateIso = "Сегодня"
                                            ),
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
                                        activeIsFiller = false
                                        activeJourneyStartMs = System.currentTimeMillis()
                                    }
                                },
                                enabled = currentEnergy >= requiredJourneyEnergy,
                                colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_start_chantal_journey")
                            ) {
                                Text(
                                    text = "В поход ($requiredJourneyEnergy Эн.)",
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
                                    text = "К тренировкам (+10 Эн.)",
                                    color = HobbitEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 6 параметров характера с реальными геймплейными бонусами
                    Text(
                        text = "Характер Шанталь и активные геймплейные бонусы:",
                        style = MaterialTheme.typography.titleSmall,
                        color = HobbitCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val traitRows = listOf(
                        Triple("Любопытство", curiosity, "+${perks.curiosityBonusCoins} монет из тайников · ${perks.curiosityClueChancePct}% шанс улики"),
                        Triple("Нежность", tenderness, "+${perks.tendernessMoodHealBonusPct}% настроения при ласке дома"),
                        Triple("Игривость", playfulness, "${perks.playfulnessCharmOutOfDebtChancePct}% шанс обратить долг в шутку"),
                        Triple("Романтичность", romanticism, "+${perks.romanticismEveningCoinBonusPct}% монет в вечерних походах"),
                        Triple("Отвага", courage, "${perks.courageAvoidExhaustionChancePct}% защита от усталости 45 Эн."),
                        Triple("Остроумие", wit, "+${perks.witMerchantSellBonusPct}% к цене продажи диковинок")
                    )
                    traitRows.forEach { (nameRu, score, perkText) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = nameRu, color = HobbitCream, fontWeight = FontWeight.Bold)
                                    Text(text = "$score", color = HobbitGold, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (score * 0.03f).coerceIn(0.1f, 1f) },
                                    color = HobbitGold,
                                    trackColor = HobbitCardBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = perkText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitMuted
                                )
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
                    Text(
                        text = "Шанталь сама выбирает маршрут и может возвращаться в одну и ту же локацию подряд, если у неё там остались незаконченные дела или несколько собеседников.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HobbitMuted
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
                                    text = if (unlocked) "✓ ${reg.titleRu}" else "🔒 ${reg.titleRu} (с ${reg.minJourneysRequired} пох.)",
                                    color = if (unlocked) HobbitGold else HobbitMuted,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(text = reg.loreRu, style = MaterialTheme.typography.bodySmall, color = HobbitCream)
                                Text(
                                    text = "Персонажи: ${reg.npcsRu.joinToString(" · ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HobbitEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 3: УМНЫЙ ИНВЕНТАРЬ + ДОМАШНЯЯ АЛХИМИЯ И ПОДАРКИ ДРУЗЬЯМ-NPC
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val royalClues = inventoryList.filter { it.category == ChantalLootCategory.ROYAL_CLUE }
                    val herbs = inventoryList.filter { it.category == ChantalLootCategory.HERB }
                    val trinkets = inventoryList.filter { it.category == ChantalLootCategory.TRINKET }

                    // Домашняя Алхимия у камина
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Алхимический стол у камина (Крафт оберегов)",
                                color = HobbitEmerald,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Соедини 1 Целебную траву + 1 Диковинку, чтобы создать Оберег Попутного Ветра (+50% монет и защита в следующем походе)!",
                                style = MaterialTheme.typography.bodySmall,
                                color = HobbitMuted
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
                                        curiosity += 1
                                        activeArrowToast = "Любопытство ↑"
                                        lastReactionBanner = "Создан Оберег Попутного Ветра (+50% монет и защита в следующем походе)!"
                                    }
                                },
                                enabled = herbs.isNotEmpty() && (trinkets.isNotEmpty() || herbs.size >= 2),
                                colors = ButtonDefaults.buttonColors(containerColor = HobbitEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Смастерить Оберег Попутного Ветра", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Шкатулка королевских улик (${royalClues.size}/7 — Не продаются)",
                        style = MaterialTheme.typography.titleSmall,
                        color = HobbitGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                    royalClues.forEach { clue ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = clue.titleRu, color = HobbitCream, fontWeight = FontWeight.Bold)
                                Text(text = clue.descriptionRu, style = MaterialTheme.typography.bodySmall, color = HobbitMuted)
                                Button(
                                    onClick = {
                                        if (!clue.inspectedBonusClaimed) {
                                            curiosity += 1
                                            val idx = inventoryList.indexOfFirst { it.id == clue.id }
                                            if (idx >= 0) inventoryList[idx] = clue.copy(inspectedBonusClaimed = true)
                                            activeArrowToast = "Любопытство ↑"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg)
                                ) {
                                    Text(
                                        text = if (clue.inspectedBonusClaimed) "✓ Изучено в коллекции" else "Изучить у камина (+1 Любопытство ↑)",
                                        color = HobbitGold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Целебные травы (${herbs.size}) и Диковинки на продажу или в подарок NPC (${trinkets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = HobbitEmerald,
                        fontWeight = FontWeight.ExtraBold
                    )
                    (herbs + trinkets).forEach { item ->
                        val witPrice = (item.sellPriceCoins * (100 + perks.witMerchantSellBonusPct)) / 100
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = item.titleRu, color = HobbitCream, fontWeight = FontWeight.Bold)
                                Text(text = item.descriptionRu, style = MaterialTheme.typography.bodySmall, color = HobbitMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.category == ChantalLootCategory.HERB) {
                                        Button(
                                            onClick = {
                                                isExhausted45 = false
                                                moodPct = (moodPct + 15).coerceAtMost(100)
                                                inventoryList.removeAll { it.id == item.id }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HobbitEmerald)
                                        ) {
                                            Text("Дать Шанталь", color = Color.White)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                coins += 18
                                                tenderness += 1
                                                activeArrowToast = "Нежность ↑"
                                                inventoryList.removeAll { it.id == item.id }
                                                lastReactionBanner = "Подарок отправлен другу-NPC! В ответ получено +18 Монет и Нежность ↑"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange)
                                        ) {
                                            Text("Подарить NPC (+18 М)", color = HobbitCream)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            coins += witPrice
                                            inventoryList.removeAll { it.id == item.id }
                                        }
                                    ) {
                                        Text("Продать (+$witPrice М)", color = HobbitGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 4: ЛЕСНАЯ ЛАВКА + ФУРГОН СТРАНСТВУЮЩЕГО ТОРГОВЦА И СУНДУКИ ГЛАВ
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
                    Text(
                        text = "Лесная лавка (Тир 1→3) и Странствующий торговец (В кошельке: $coins М)",
                        style = MaterialTheme.typography.titleMedium,
                        color = HobbitCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Image(
                        painter = painterResource(id = R.drawable.chantal_merchant_chest),
                        contentDescription = "Фургон странствующего торговца редкостями и Сундук Главы",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    DEFAULT_CHANTAL_MERCHANT_OFFERS.forEach { offer ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "★ Купец редкостей: ${offer.titleRu} · ${offer.discountPriceCoins} М", color = HobbitGold, fontWeight = FontWeight.ExtraBold)
                                Text(text = offer.effectRu, style = MaterialTheme.typography.bodySmall, color = HobbitCream)
                                Button(
                                    onClick = {
                                        if (coins >= offer.discountPriceCoins) {
                                            coins -= offer.discountPriceCoins
                                            curiosity += 2
                                            hasActiveRoadAmulet = true
                                            activeArrowToast = "Любопытство ↑↑"
                                        }
                                    },
                                    enabled = coins >= offer.discountPriceCoins,
                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange)
                                ) {
                                    Text("Купить у купца за ${offer.discountPriceCoins} М", color = HobbitCream)
                                }
                            }
                        }
                    }

                    DEFAULT_CHANTAL_SHOP_ITEMS.forEach { shopItem ->
                        val owned = shopItem.id in ownedShopItemIds
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "${shopItem.titleRu} · ${shopItem.priceCoins} М", color = HobbitGold, fontWeight = FontWeight.ExtraBold)
                                Text(text = shopItem.descriptionRu, style = MaterialTheme.typography.bodySmall, color = HobbitCream)
                                Text(text = shopItem.gameplayEffectRu, style = MaterialTheme.typography.labelSmall, color = HobbitEmerald)
                                Button(
                                    onClick = {
                                        if (coins >= shopItem.priceCoins) {
                                            coins -= shopItem.priceCoins
                                            if (shopItem.category == ChantalShopCategory.CONSUMABLE) {
                                                isExhausted45 = false
                                                moodPct = 100
                                            } else if (!owned) {
                                                ownedShopItemIds.add(shopItem.id)
                                                if (shopItem.category == ChantalShopCategory.DEN_COMFORT) {
                                                    showDenInteriorPanorama = true
                                                }
                                            }
                                        }
                                    },
                                    enabled = coins >= shopItem.priceCoins && (!owned || shopItem.category == ChantalShopCategory.CONSUMABLE),
                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitOrange)
                                ) {
                                    Text(
                                        text = if (owned && shopItem.category != ChantalShopCategory.CONSUMABLE) "✓ Куплено (меняет уют Домика)" else "Купить за ${shopItem.priceCoins} М",
                                        color = HobbitCream
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 5: ДНЕВНИК С ПАМЯТЬЮ ПОСЛЕДНИХ 3 ПОХОДОВ
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
                        text = "Трёхслойная Память Хроник (Слой 1: все паспорта с #1 · Слой 2: досье NPC · Слой 3: 3 последние главы)",
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
                                text = "• [Поход #1] Старая мельница | Барсук Брамбл -> выбор: «Спрятать медальон в шкатулку тайн»\n• [Поход #2] Янтарная дубрава | Ворон Корвус -> выбор: «Попросить приносить лесные письма»\n• [Поход #3] Старая мельница | Перевозчик Хьюго -> выбор: «Пообещать найти ключ от ларца»",
                                style = MaterialTheme.typography.bodySmall,
                                color = HobbitMuted
                            )
                        }
                    }
                    if (journalList.isEmpty()) {
                        Text(
                            text = "Первые 3 похода уже в вечной летописи ИИ! Отправь Шанталь в новый поход, чтобы увидеть, как они откликнутся.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HobbitMuted
                        )
                    } else {
                        journalList.forEachIndexed { idx, entry ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = HobbitElevatedBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${if (idx < 3) "★ Слой 3 (Полный текст в ИИ) · " else "✓ Слой 1 (Паспорт в ИИ) · "}${entry.titleRu}",
                                        color = HobbitGold,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(text = entry.storyTextRu, style = MaterialTheme.typography.bodySmall, color = HobbitCream)
                                    entry.chosenActionSummaryRu?.let { chosen ->
                                        Text(
                                            text = "Твой выбор: «$chosen»",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HobbitEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ВКЛАДКА 6: НАСТРОЙКИ ИИ (GEMINI / OPENROUTER / GROK), ЛОРА И ТЕСТ СТАДИЙ
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
                        text = "Шлюз нейросети: Прямой Gemini (по умолч.), OpenRouter или Grok",
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
                        Text(
                            text = "Выбор модели внутри OpenRouter:",
                            style = MaterialTheme.typography.labelMedium,
                            color = HobbitGold,
                            fontWeight = FontWeight.Bold
                        )
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
                                    text = "${if (isChosen) "✓ " else ""}$modelLabel ($modelId)",
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
                        label = { Text("Дополнительные заметки лора и персонализации для ИИ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(onClick = { forceTransformPreview = false; totalJourneys = 3 }, modifier = Modifier.weight(1f)) {
                            Text("Ст. 1", color = HobbitCream)
                        }
                        OutlinedButton(onClick = { forceTransformPreview = false; totalJourneys = 22 }, modifier = Modifier.weight(1f)) {
                            Text("Ст. 2", color = HobbitCream)
                        }
                        OutlinedButton(onClick = { forceTransformPreview = false; totalJourneys = 65 }, modifier = Modifier.weight(1f)) {
                            Text("Ст. 3", color = HobbitCream)
                        }
                        OutlinedButton(onClick = { forceTransformPreview = true; totalJourneys = 105 }, modifier = Modifier.weight(1f)) {
                            Text("Акт II", color = HobbitGold)
                        }
                    }
                }
            }
        }
    }
}
