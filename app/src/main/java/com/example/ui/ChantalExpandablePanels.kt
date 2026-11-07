package com.example.ui

import com.example.domain.TimeProvider
import com.example.domain.ChantalJourneyClock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ChantalInventoryLootItem
import com.example.domain.ChantalLoadoutSlotType
import com.example.domain.ChantalShopCategory
import com.example.domain.ChantalShopItem
import com.example.domain.ChantalTraitPerks
import com.example.domain.ChantalTraits
import com.example.domain.DEFAULT_CHANTAL_SHOP_ITEMS
import com.example.ui.theme.LocalVelarisPalette
import kotlinx.coroutines.delay

@Composable
internal fun ChantalExpeditionAndSubPanelsSection(
    denAccordionTab: String,
    onClosePanel: () -> Unit,
    traits: ChantalTraits,
    perks: ChantalTraitPerks,
    selectedTraitPopupName: String?,
    onSelectTraitPopupName: (String?) -> Unit,
    denTierNumber: Int,
    itemsInCurrentStep: Int,
    ownedDenCount: Int,
    foundDenTrophyCount: Int,
    onOpenShopComfort: () -> Unit,
    isJourneyActive: Boolean,
    activeJourneyStartMs: Long,
    activeRegionTitleRu: String,
    activeNpcRu: String,
    activeStayedInSameRegion: Boolean,
    activeAutonomousReasonRu: String,
    onCompleteActiveJourney: () -> Unit,
    timeProvider: TimeProvider,
    hasActiveRoadAmulet: Boolean,
    loadoutProtectPct: Int,
    loadoutCoinsBonusPct: Int,
    onOpenBagForAmulet: () -> Unit,
    equippedOutfitId: String?,
    equippedToolId: String?,
    equippedWeaponId: String?,
    activeBundleShopItem: ChantalShopItem?,
    activeBundleLootItem: ChantalInventoryLootItem?,
    ownedShopItemIds: List<String>,
    pantryProvisionCounts: Map<String, Int>,
    activeLoadoutSlotPicker: String,
    onSelectLoadoutSlotPicker: (String) -> Unit,
    activeSlotInspectItemId: String?,
    onSelectSlotInspectItemId: (String?) -> Unit,
    coins: Int,
    onToggleEquipGear: (String, ChantalShopItem) -> Unit,
    onBuyAndEquipGear: (ChantalShopItem) -> Unit,
    fillerTextInput: String,
    onFillerTextChange: (String) -> Unit,
    currentEnergy: Int,
    requiredJourneyEnergy: Int,
    onStartJourney: () -> Unit,
    hasPendingStory: Boolean,
    preferredRegionTitle: String?,
    onOpenWorkoutPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Маршрут: ${preferredRegionTitle ?: "выбирает Шанталь"}", color = HobbitCream)
        if (hasPendingStory) Text("Сначала прочитай историю предыдущего похода", color = HobbitMuted)
        // 1. ОТДЕЛЬНАЯ ИГРОВАЯ ПАНЕЛЬ ПОХОДА И 4 СЛОТОВ ЭКИПИРОВКИ (открывается кнопкой «Поход» или при активном походе)
        AnimatedVisibility(
            visible = denAccordionTab == "loadout" || isJourneyActive,
            enter = fadeIn(tween(220)) + expandVertically(tween(260)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(220))
        ) {
            if (isJourneyActive) {
                ChantalActiveExpeditionCard(
                    activeJourneyStartMs = activeJourneyStartMs,
                    activeRegionTitleRu = activeRegionTitleRu,
                    activeNpcRu = activeNpcRu,
                    activeStayedInSameRegion = activeStayedInSameRegion,
                    activeAutonomousReasonRu = activeAutonomousReasonRu,
                    onCompleteActiveJourney = onCompleteActiveJourney,
                    timeProvider = timeProvider
                )
            } else {
                ChantalExpeditionPrepContent(
                    hasActiveRoadAmulet = hasActiveRoadAmulet,
                    loadoutProtectPct = loadoutProtectPct,
                    loadoutCoinsBonusPct = loadoutCoinsBonusPct,
                    onClosePrepPanel = onClosePanel,
                    onOpenBagForAmulet = onOpenBagForAmulet,
                    equippedOutfitId = equippedOutfitId,
                    equippedToolId = equippedToolId,
                    equippedWeaponId = equippedWeaponId,
                    activeBundleShopItem = activeBundleShopItem,
                    activeBundleLootItem = activeBundleLootItem,
                    ownedShopItemIds = ownedShopItemIds,
                    pantryProvisionCounts = pantryProvisionCounts,
                    activeLoadoutSlotPicker = activeLoadoutSlotPicker,
                    onSelectLoadoutSlotPicker = onSelectLoadoutSlotPicker,
                    activeSlotInspectItemId = activeSlotInspectItemId,
                    onSelectSlotInspectItemId = onSelectSlotInspectItemId,
                    coins = coins,
                    onToggleEquipGear = onToggleEquipGear,
                    onBuyAndEquipGear = onBuyAndEquipGear,
                    fillerTextInput = fillerTextInput,
                    onFillerTextChange = onFillerTextChange,
                    currentEnergy = if (hasPendingStory) 0 else currentEnergy,
                    requiredJourneyEnergy = requiredJourneyEnergy,
                    onStartJourney = onStartJourney,
                    onOpenWorkoutPlan = onOpenWorkoutPlan
                )
            }
        }

        // 2. РАСКРЫВАЮЩАЯСЯ ПАНЕЛЬ ХАРАКТЕРА (6 ЧЕРТ)
        AnimatedVisibility(
            visible = denAccordionTab == "traits",
            enter = fadeIn(tween(220)) + expandVertically(tween(250)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
        ) {
            val traitRows = listOf(
                Triple("Любопытство", traits.curiosity, "+${perks.curiosityBonusCoins} монет из тайников · ${perks.curiosityClueChancePct}% шанс улики"),
                Triple("Нежность", traits.tenderness, "+${perks.tendernessMoodHealBonusPct}% настроения при ласке дома"),
                Triple("Игривость", traits.playfulness, "${perks.playfulnessCharmOutOfDebtChancePct}% шанс обратить долг в шутку"),
                Triple("Романтичность", traits.romanticism, "+${perks.romanticismEveningCoinBonusPct}% монет в вечерних походах"),
                Triple("Отвага", traits.courage, "${perks.courageAvoidExhaustionChancePct}% защита от усталости 45 Эн."),
                Triple("Остроумие", traits.wit, "+${perks.witMerchantSellBonusPct}% к цене продажи диковинок")
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = HobbitCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("panel_traits_content")
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .animateContentSize(animationSpec = tween(200)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ Шесть черт характера Шанталь",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitMuted,
                            modifier = Modifier
                                .clickable { onClosePanel() }
                                .padding(4.dp)
                        )
                    }

                    traitRows.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (nameRu, score, perkText) ->
                                val isPopupOpen = selectedTraitPopupName == nameRu
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPopupOpen) HobbitElevatedBg else HobbitBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSelectTraitPopupName(if (isPopupOpen) null else nameRu)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = nameRu,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.SemiBold,
                                                color = HobbitCream
                                            )
                                            Text(
                                                text = "$score/100",
                                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                color = HobbitGold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { (score / 100f).coerceIn(0.02f, 1f) },
                                            color = HobbitGold,
                                            trackColor = HobbitCardBg,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                        AnimatedVisibility(visible = isPopupOpen) {
                                            Text(
                                                text = perkText,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 18.sp,
                                                color = HobbitEmerald,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. РАСКРЫВАЮЩАЯСЯ ПАНЕЛЬ ИНТЕРЬЕРА ДОМИКА
        AnimatedVisibility(
            visible = denAccordionTab == "housing",
            enter = fadeIn(tween(220)) + expandVertically(tween(250)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = HobbitCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("panel_housing_content")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏠 Интерьер Домика · Уровень $denTierNumber ($itemsInCurrentStep из 10)",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitEmerald
                        )
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitMuted,
                            modifier = Modifier
                                .clickable { onClosePanel() }
                                .padding(4.dp)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (itemsInCurrentStep / 10f).coerceIn(0.05f, 1f) },
                        color = HobbitEmerald,
                        trackColor = HobbitBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Куплено в лавке: $ownedDenCount · Принесено из походов: $foundDenTrophyCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        color = HobbitMuted
                    )
                    Button(
                        onClick = onOpenShopComfort,
                        colors = ButtonDefaults.buttonColors(containerColor = HobbitElevatedBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛒 Выбрать мебель в Лесной лавке",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalActiveExpeditionCard(
    activeJourneyStartMs: Long,
    activeRegionTitleRu: String,
    activeNpcRu: String,
    activeStayedInSameRegion: Boolean,
    activeAutonomousReasonRu: String,
    onCompleteActiveJourney: () -> Unit,
    timeProvider: TimeProvider,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(timeProvider.currentWallTimeMs()) }
    LaunchedEffect(activeJourneyStartMs) {
        while (activeJourneyStartMs > 0L) {
            nowMs = timeProvider.currentWallTimeMs()
            delay(1000L)
        }
    }
    val elapsedSec = ((nowMs - activeJourneyStartMs) / 1000L).coerceAtLeast(0L)
    val remainSec = (ChantalJourneyClock.remainingMs(activeJourneyStartMs, maxOf(nowMs, timeProvider.currentWallTimeMs())) + 999L) / 1000L
    val mm = remainSec / 60L
    val ss = remainSec % 60L

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, HobbitGold, RoundedCornerShape(20.dp))
            .testTag("chantal_active_journey_card"),
        shape = RoundedCornerShape(20.dp),
        color = HobbitElevatedBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (activeStayedInSameRegion) {
                    "Шанталь снова в «$activeRegionTitleRu» ($activeNpcRu)"
                } else {
                    "Шанталь в походе: «$activeRegionTitleRu» ($activeNpcRu)"
                },
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitGold
            )
            Text(
                text = "В пути (60 мин): %02d:%02d".format(mm, ss),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream
            )
            Text(
                text = activeAutonomousReasonRu,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = HobbitMuted
            )
            Button(
                onClick = onCompleteActiveJourney,
                enabled = ChantalJourneyClock.ready(activeJourneyStartMs, maxOf(nowMs, timeProvider.currentWallTimeMs())),
                colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("btn_complete_active_journey")
            ) {
                Text(
                    text = "Встретить Шанталь и прочитать историю",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitCream
                )
            }
        }
    }
}

@Composable
internal fun ChantalExpeditionPrepContent(
    hasActiveRoadAmulet: Boolean,
    loadoutProtectPct: Int,
    loadoutCoinsBonusPct: Int,
    onClosePrepPanel: () -> Unit,
    onOpenBagForAmulet: () -> Unit,
    equippedOutfitId: String?,
    equippedToolId: String?,
    equippedWeaponId: String?,
    activeBundleShopItem: ChantalShopItem?,
    activeBundleLootItem: ChantalInventoryLootItem?,
    ownedShopItemIds: List<String>,
    pantryProvisionCounts: Map<String, Int>,
    activeLoadoutSlotPicker: String,
    onSelectLoadoutSlotPicker: (String) -> Unit,
    activeSlotInspectItemId: String?,
    onSelectSlotInspectItemId: (String?) -> Unit,
    coins: Int,
    onToggleEquipGear: (String, ChantalShopItem) -> Unit,
    onBuyAndEquipGear: (ChantalShopItem) -> Unit,
    fillerTextInput: String,
    onFillerTextChange: (String) -> Unit,
    currentEnergy: Int,
    requiredJourneyEnergy: Int,
    onStartJourney: () -> Unit,
    onOpenWorkoutPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = HobbitCardBg,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HobbitGold.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .testTag("chantal_expedition_prep_panel")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize(animationSpec = tween(220)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🧭 Подготовка к походу",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitGold
                    )
                    Text(
                        text = "Бонусы экипировки: 🛡️ $loadoutProtectPct% · 🪙 +$loadoutCoinsBonusPct%",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitEmerald
                    )
                }
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitMuted,
                    modifier = Modifier
                        .clickable { onClosePrepPanel() }
                        .padding(4.dp)
                )
            }

            // Плашка Оберега
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HobbitElevatedBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBagForAmulet() }
            ) {
                Text(
                    text = if (hasActiveRoadAmulet) {
                        "🧿 Оберег активен: 🛡️ +100% защита · 🪙 +50% монет"
                    } else {
                        "🧿 Оберег в дорогу: Пусто (смастерить в Сумке →)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasActiveRoadAmulet) HobbitEmerald else HobbitCream,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // 4 слота снаряжения: OUTFIT, TOOL, WEAPON, BUNDLE
            val outfitObj = DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedOutfitId && it.id in ownedShopItemIds }
            val toolObj = DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedToolId && it.id in ownedShopItemIds }
            val weaponObj = DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == equippedWeaponId && it.id in ownedShopItemIds }
            val bundleTitle = activeBundleShopItem?.shortTitleRu ?: activeBundleLootItem?.shortTitleRu ?: "Пусто"
            val bundleEmoji = activeBundleShopItem?.emoji ?: activeBundleLootItem?.emoji ?: "🧺"

            val slotCards = listOf(
                Triple("OUTFIT", outfitObj?.emoji ?: "🧥", outfitObj?.shortTitleRu ?: "Одежда"),
                Triple("TOOL", toolObj?.emoji ?: "🧭", toolObj?.shortTitleRu ?: "Прибор"),
                Triple("WEAPON", weaponObj?.emoji ?: "🏹", weaponObj?.shortTitleRu ?: "Защита"),
                Triple("BUNDLE", bundleEmoji, if (bundleTitle == "Пусто") "Узелок" else bundleTitle)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slotCards.forEach { (slotKey, emoji, label) ->
                    val isPicked = activeLoadoutSlotPicker == slotKey
                    val isEquipped = when (slotKey) {
                        "OUTFIT" -> outfitObj != null
                        "TOOL" -> toolObj != null
                        "WEAPON" -> weaponObj != null
                        else -> activeBundleShopItem != null || activeBundleLootItem != null
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPicked || isEquipped) HobbitElevatedBg else HobbitBg,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isPicked) {
                                    Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(14.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onSelectLoadoutSlotPicker(if (isPicked) "NONE" else slotKey)
                                onSelectSlotInspectItemId(null)
                            }
                            .testTag("slot_card_$slotKey")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isEquipped || isPicked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isEquipped || isPicked) HobbitCream else HobbitMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Анимированное раскрытие выбора предмета для слота
            AnimatedVisibility(
                visible = activeLoadoutSlotPicker != "NONE",
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
            ) {
                val slotItems = when (activeLoadoutSlotPicker) {
                    "OUTFIT" -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.OUTFIT }
                    "TOOL" -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.TOOL }
                    "WEAPON" -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.WEAPON }
                    else -> DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.loadoutSlotType == ChantalLoadoutSlotType.BUNDLE }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HobbitBg, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val slotTitle = when (activeLoadoutSlotPicker) {
                            "OUTFIT" -> "🧥 Одежда в поход"
                            "TOOL" -> "🧭 Инструмент следопыта"
                            "WEAPON" -> "🏹 Оружие и защита"
                            else -> "🧺 Узелок в дорогу"
                        }
                        Text(
                            text = slotTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitMuted,
                            modifier = Modifier.clickable {
                                onSelectLoadoutSlotPicker("NONE")
                                onSelectSlotInspectItemId(null)
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
                                color = if (isSelectedInspect) HobbitElevatedBg else HobbitCardBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isSelectedInspect) {
                                            Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(12.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        onSelectSlotInspectItemId(if (isSelectedInspect) null else gear.id)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(text = gear.emoji, fontSize = 18.sp)
                                    Text(
                                        text = gear.shortTitleRu,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = HobbitCream,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isOwned) "В сумке ✓" else "${gear.priceCoins} М",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOwned) HobbitEmerald else HobbitGold
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
                            color = HobbitElevatedBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${inspectGear.emoji} ${inspectGear.titleRu}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = HobbitCream
                                )
                                Text(
                                    text = inspectGear.gameplayEffectRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    color = HobbitEmerald
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isOwned) {
                                        Button(
                                            onClick = { onToggleEquipGear(activeLoadoutSlotPicker, inspectGear) },
                                            colors = ButtonDefaults.buttonColors(containerColor = LocalVelarisPalette.current.selectedContainer),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                "Взять / Снять",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { onBuyAndEquipGear(inspectGear) },
                                            enabled = coins >= inspectGear.priceCoins,
                                            colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                "Купить · ${inspectGear.priceCoins} 🪙",
                                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                color = HobbitCream
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Поле пожелания / поручения
            OutlinedTextField(
                value = fillerTextInput,
                onValueChange = onFillerTextChange,
                label = { Text("🪶 Пожелание или поручение в дорогу (необязательно)") },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartJourney,
                    enabled = currentEnergy >= requiredJourneyEnergy,
                    colors = ButtonDefaults.buttonColors(containerColor = HobbitElevatedBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("btn_start_chantal_journey")
                ) {
                    Text(
                        text = "Отправить в поход · ⚡ $requiredJourneyEnergy",
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitCream
                    )
                }

                OutlinedButton(
                    onClick = onOpenWorkoutPlan,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                ) {
                    Text(
                        text = "+10 Эн. (Кегель)",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitEmerald
                    )
                }
            }
        }
    }
}
