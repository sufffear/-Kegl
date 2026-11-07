package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.ChantalAiProvider
import com.example.domain.ChantalInventoryLootItem
import com.example.domain.ChantalLootCategory
import com.example.domain.ChantalRewardPopupData
import com.example.domain.ChantalShopCategory
import com.example.domain.ChantalShopItem
import com.example.domain.ChantalStoryDifficulty
import com.example.domain.ChantalStoryEpisode
import com.example.domain.ChantalTraitPerks
import com.example.domain.ChantalTraits
import com.example.domain.DEFAULT_CHANTAL_MAP_REGIONS
import com.example.domain.DEFAULT_CHANTAL_MERCHANT_OFFERS
import com.example.domain.DEFAULT_CHANTAL_SHOP_ITEMS
import com.example.domain.OPENROUTER_ANDROID_MODELS
import com.example.ui.theme.LocalVelarisPalette
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun ChantalRewardPopupDialog(
    reward: ChantalRewardPopupData,
    onDismiss: () -> Unit,
    onOpenInBag: (String) -> Unit
) {
    val popScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "reward_pop_scale"
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(popScale)
                .border(1.5.dp, HobbitGold, RoundedCornerShape(26.dp))
                .testTag("chantal_reward_popup"),
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
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            color = HobbitGold
                        )
                        Text(
                            text = reward.episodeTitleRu,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitCream
                        )
                    }
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitMuted,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "🪙 Монеты",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                color = HobbitGold
                            )
                            Text(
                                text = "+${reward.coinsEarned} М",
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = HobbitCream
                            )
                            Text(
                                text = "Опыт: +${reward.xpEarned} XP",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                color = HobbitPink
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val catBadge = when (reward.lootItem.category) {
                                ChantalLootCategory.DEN_TROPHY -> "🏠 В Интерьер (+1/10)"
                                ChantalLootCategory.ROYAL_CLUE -> "👑 Улика"
                                ChantalLootCategory.HERB -> "🌿 Трава"
                                ChantalLootCategory.TRINKET -> "✦ Диковинка"
                            }
                            Text(
                                text = catBadge,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                color = HobbitEmerald
                            )
                            Text(
                                text = "${reward.lootItem.emoji} ${reward.lootItem.shortTitleRu}",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = HobbitCream,
                                maxLines = 2
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HobbitBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${reward.lootItem.emoji} ${reward.lootItem.titleRu}",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                        Text(
                            text = reward.lootItem.descriptionRu,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp,
                            color = HobbitCream
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HobbitElevatedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🦊 Реакция Шанталь:",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                        Text(
                            text = "«${reward.chantalReactionRu}»",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp,
                            fontStyle = FontStyle.Italic,
                            color = HobbitCream
                        )
                        if (reward.traitArrowsText.isNotBlank()) {
                            Text(
                                text = "Характер: ${reward.traitArrowsText}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = HobbitEmerald
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenInBag(reward.lootItem.id) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "🎒 В Сумку",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = HobbitElevatedBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_claim_reward_popup")
                    ) {
                        Text(
                            "Забрать ✨",
                            style = MaterialTheme.typography.titleSmall,
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

@Composable
internal fun ChantalModalMenuDialog(
    subTab: String,
    onSelectSubTab: (String) -> Unit,
    onDismiss: () -> Unit,
    denTierNumber: Int,
    itemsInCurrentStep: Int,
    ownedDenCount: Int,
    foundDenTrophyCount: Int,
    totalJourneys: Int,
    preferredRegionId: String,
    routeLocked: Boolean,
    onChooseRoute: (String) -> Unit,
    coins: Int,
    selectedAiProvider: String,
    traits: ChantalTraits,
    perks: ChantalTraitPerks,
    inventoryList: List<ChantalInventoryLootItem>,
    newlyAddedBagItemIds: List<String>,
    selectedBagItemId: String?,
    onSelectBagItem: (String?) -> Unit,
    onInspectClue: (ChantalInventoryLootItem) -> Unit,
    onBrewHerb: (ChantalInventoryLootItem) -> Unit,
    onGiftTrinket: (ChantalInventoryLootItem) -> Unit,
    onSellLootItem: (ChantalInventoryLootItem, Int) -> Unit,
    onCraftRoadAmulet: () -> Unit,
    selectedShopEntryId: String?,
    onSelectShopEntryId: (String?) -> Unit,
    shopCategoryTab: String,
    onSelectShopCategoryTab: (String) -> Unit,
    shopDenStepFilter: Int,
    onSelectShopDenStepFilter: (Int) -> Unit,
    ownedShopItemIds: List<String>,
    claimedMerchantOfferIds: List<String>,
    onBuyMerchantOffer: (String) -> Unit,
    onBuyShopItem: (ChantalShopItem) -> Unit,
    journalList: List<ChantalStoryEpisode>,
    onSelectAiProvider: (String) -> Unit,
    selectedOpenRouterModel: String,
    onSelectOpenRouterModel: (String) -> Unit,
    customOpenRouterModelInput: String,
    onCustomOpenRouterModelChange: (String) -> Unit,
    selectedDifficulty: String,
    onSelectDifficulty: (String) -> Unit,
    customLoreNotes: String,
    onCustomLoreNotesChange: (String) -> Unit,
    showTraitArrows: Boolean,
    onToggleTraitArrows: () -> Unit,
    forceTransformPreview: Boolean,
    onToggleTransformPreview: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .border(1.5.dp, HobbitGold, RoundedCornerShape(26.dp))
                .testTag("chantal_modal_menu"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .animateContentSize(animationSpec = tween(220)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Шапка модального меню
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🦊 Книга Странствий Шанталь",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitCream
                        )
                        Text(
                            text = "Шесть игровых разделов",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = HobbitGold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .testTag("btn_close_chantal_modal")
                    ) {
                        Text(
                            text = "✕ Закрыть",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitCream,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Сетка 6 разделов (2 колонки x 3 ряда)
                val unlockedRegionsCount = DEFAULT_CHANTAL_MAP_REGIONS.count { totalJourneys >= it.minJourneysRequired }
                val menuItems = listOf(
                    Triple("den", "🏠 Домик Шанталь", "Ур. $denTierNumber ($itemsInCurrentStep/10)"),
                    Triple("map", "🗺️ Карта троп", "$unlockedRegionsCount/${DEFAULT_CHANTAL_MAP_REGIONS.size} лок."),
                    Triple("bag", "🎒 Сумка находок", "${inventoryList.size} предм."),
                    Triple("shop", "🛒 Лесная лавка", "$coins 🪙"),
                    Triple("journal", "📖 Летопись странствий", "${journalList.size} гл."),
                    Triple("ai", "⚙️ Настройки ИИ и Лора", selectedAiProvider)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    menuItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (id, title, badge) ->
                                val isCurrent = subTab == id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isCurrent) HobbitElevatedBg else HobbitBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (isCurrent) {
                                                Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(14.dp))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onSelectSubTab(id) }
                                        .testTag("modal_nav_$id")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) HobbitGold else HobbitCream,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            color = HobbitMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Содержимое выбранного раздела внутри модального окна
                when (subTab) {
                    "den" -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🏠 Сводка Домика и Характера",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = HobbitGold
                                )
                                Text(
                                    text = "Уют жилья: Уровень $denTierNumber ($itemsInCurrentStep из 10 предметов) · Куплено: $ownedDenCount · Трофеев: $foundDenTrophyCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp,
                                    color = HobbitCream
                                )
                                Text(
                                    text = "Ведущая черта: ${perks.dominantTraitNameRu} · Любопытство ${traits.curiosity}, Нежность ${traits.tenderness}, Игривость ${traits.playfulness}, Романтичность ${traits.romanticism}, Отвага ${traits.courage}, Остроумие ${traits.wit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp,
                                    color = HobbitMuted
                                )
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = HobbitElevatedBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Вернуться к центральной сцене Домика",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = HobbitCream
                                    )
                                }
                            }
                        }
                    }

                    "map" -> {
                        Column {
                            ChantalWorldMap(totalJourneys, preferredRegionId, routeLocked, onChooseRoute)
                            journalList.filter { it.regionTitleRu.isNotBlank() }.groupBy { it.regionTitleRu }.forEach { (place, visits) ->
                                Text("$place · посещений: ${visits.size}", color = HobbitCream)
                                visits.take(3).forEach { entry ->
                                    Text("${entry.titleRu} — ${entry.chosenActionSummaryRu.orEmpty()}", color = HobbitMuted)
                                }
                            }
                        }
                    }

                    "bag" -> {
                        ChantalBagSectionCard(
                            inventoryList = inventoryList,
                            newlyAddedBagItemIds = newlyAddedBagItemIds,
                            selectedBagItemId = selectedBagItemId,
                            perks = perks,
                            onSelectBagItem = onSelectBagItem,
                            onInspectClue = onInspectClue,
                            onBrewHerb = onBrewHerb,
                            onGiftTrinket = onGiftTrinket,
                            onSellLootItem = onSellLootItem,
                            onCraftRoadAmulet = onCraftRoadAmulet
                        )
                    }

                    "shop" -> {
                        ChantalShopSectionCard(
                            coins = coins,
                            itemsInCurrentStep = itemsInCurrentStep,
                            selectedShopEntryId = selectedShopEntryId,
                            onSelectShopEntryId = onSelectShopEntryId,
                            shopCategoryTab = shopCategoryTab,
                            onSelectShopCategoryTab = onSelectShopCategoryTab,
                            shopDenStepFilter = shopDenStepFilter,
                            onSelectShopDenStepFilter = onSelectShopDenStepFilter,
                            ownedShopItemIds = ownedShopItemIds,
                            claimedMerchantOfferIds = claimedMerchantOfferIds,
                            onBuyMerchantOffer = onBuyMerchantOffer,
                            onBuyShopItem = onBuyShopItem
                        )
                    }

                    "journal" -> {
                        ChantalJournalSectionCard(journalList = journalList)
                    }

                    "ai" -> {
                        ChantalAiLoreSectionCard(
                            selectedAiProvider = selectedAiProvider,
                            onSelectAiProvider = onSelectAiProvider,
                            selectedOpenRouterModel = selectedOpenRouterModel,
                            onSelectOpenRouterModel = onSelectOpenRouterModel,
                            customOpenRouterModelInput = customOpenRouterModelInput,
                            onCustomOpenRouterModelChange = onCustomOpenRouterModelChange,
                            selectedDifficulty = selectedDifficulty,
                            onSelectDifficulty = onSelectDifficulty,
                            customLoreNotes = customLoreNotes,
                            onCustomLoreNotesChange = onCustomLoreNotesChange,
                            showTraitArrows = showTraitArrows,
                            onToggleTraitArrows = onToggleTraitArrows,
                            forceTransformPreview = forceTransformPreview,
                            onToggleTransformPreview = onToggleTransformPreview
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalMapSectionCard(
    totalJourneys: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HobbitBg,
        modifier = modifier.fillMaxWidth()
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
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream
            )
            Image(
                painter = painterResource(id = R.drawable.chantal_map),
                contentDescription = "Пергаментная карта мира Шанталь",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            DEFAULT_CHANTAL_MAP_REGIONS.forEach { reg ->
                val unlocked = totalJourneys >= reg.minJourneysRequired
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HobbitElevatedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (unlocked) "${reg.emoji} ${reg.titleRu}" else "🔒 ${reg.titleRu} (с ${reg.minJourneysRequired} пох.)",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = if (unlocked) HobbitGold else HobbitMuted
                        )
                        Text(
                            text = reg.loreRu,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp,
                            color = HobbitCream
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalBagSectionCard(
    inventoryList: List<ChantalInventoryLootItem>,
    newlyAddedBagItemIds: List<String>,
    selectedBagItemId: String?,
    perks: ChantalTraitPerks,
    onSelectBagItem: (String?) -> Unit,
    onInspectClue: (ChantalInventoryLootItem) -> Unit,
    onBrewHerb: (ChantalInventoryLootItem) -> Unit,
    onGiftTrinket: (ChantalInventoryLootItem) -> Unit,
    onSellLootItem: (ChantalInventoryLootItem, Int) -> Unit,
    onCraftRoadAmulet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val herbs = inventoryList.filter { it.category == ChantalLootCategory.HERB }
    val trinkets = inventoryList.filter { it.category == ChantalLootCategory.TRINKET }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HobbitBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize(animationSpec = tween(220)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎒 Сумка находок",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitCream
                )
                Text(
                    text = "Предметов: ${inventoryList.size}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitGold
                )
            }

            if (inventoryList.isEmpty()) {
                Text(
                    text = "Сумка пока пуста. Отправь Шанталь в часовой поход, чтобы она принесла лесные диковинки, травы, улики и трофеи для Домика!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    color = HobbitMuted
                )
            } else {
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
                                        onSelectBagItem(if (isSelected) null else item.id)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedBagItemId != null && inventoryList.any { it.id == selectedBagItemId },
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
            ) {
                inventoryList.find { it.id == selectedBagItemId }?.let { item ->
                    val witPrice = (item.sellPriceCoins * (100 + perks.witMerchantSellBonusPct)) / 100
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HobbitElevatedBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, HobbitGold, RoundedCornerShape(16.dp))
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
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = HobbitCream,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = HobbitMuted,
                                    modifier = Modifier.clickable { onSelectBagItem(null) }
                                )
                            }
                            Text(
                                text = "«${item.descriptionRu}»",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp,
                                color = HobbitCream
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                when (item.category) {
                                    ChantalLootCategory.ROYAL_CLUE -> {
                                        Button(
                                            onClick = { onInspectClue(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg)
                                        ) {
                                            Text(
                                                text = if (item.inspectedBonusClaimed) "✓ Изучено" else "Изучить (+1 Любопытство)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                color = HobbitCream
                                            )
                                        }
                                    }
                                    ChantalLootCategory.DEN_TROPHY -> {
                                        Text(
                                            text = "✓ Стоит в Домике (+1 из 10 к интерьеру)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            color = HobbitEmerald
                                        )
                                    }
                                    ChantalLootCategory.HERB -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onBrewHerb(item) },
                                                colors = ButtonDefaults.buttonColors(containerColor = LocalVelarisPalette.current.selectedContainer)
                                            ) {
                                                Text(
                                                    "🌿 Заварить",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            OutlinedButton(onClick = { onSellLootItem(item, witPrice) }) {
                                                Text(
                                                    "Продать · +$witPrice М",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = HobbitGold
                                                )
                                            }
                                        }
                                    }
                                    ChantalLootCategory.TRINKET -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onGiftTrinket(item) },
                                                colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg)
                                            ) {
                                                Text(
                                                    "🎁 Подарить (+18 М)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HobbitCream
                                                )
                                            }
                                            OutlinedButton(onClick = { onSellLootItem(item, witPrice) }) {
                                                Text(
                                                    "Продать · +$witPrice М",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = HobbitGold
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

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = HobbitElevatedBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⚗️ Алхимия у камина (1 Трава + 1 Находка = Оберег Попутного Ветра)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitEmerald
                    )
                    Button(
                        onClick = onCraftRoadAmulet,
                        enabled = herbs.isNotEmpty() && (trinkets.isNotEmpty() || herbs.size >= 2),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalVelarisPalette.current.selectedContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Смастерить Оберег",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalShopSectionCard(
    coins: Int,
    itemsInCurrentStep: Int,
    selectedShopEntryId: String?,
    onSelectShopEntryId: (String?) -> Unit,
    shopCategoryTab: String,
    onSelectShopCategoryTab: (String) -> Unit,
    shopDenStepFilter: Int,
    onSelectShopDenStepFilter: (Int) -> Unit,
    ownedShopItemIds: List<String>,
    claimedMerchantOfferIds: List<String>,
    onBuyMerchantOffer: (String) -> Unit,
    onBuyShopItem: (ChantalShopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HobbitBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize(animationSpec = tween(220)),
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
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitCream
                )
                Text(
                    text = "🪙 $coins Монет",
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitGold
                )
            }

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
                    color = HobbitCardBg.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Лавка на колёсах",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitGold,
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
                        color = if (isSelected) HobbitElevatedBg else HobbitCardBg,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onSelectShopEntryId(if (isSelected) null else offer.id)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = offer.emoji, fontSize = 18.sp)
                            Text(
                                text = offer.shortTitleRu,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = HobbitCream,
                                maxLines = 1
                            )
                            Text(
                                text = if (isClaimed) "Куплено ✓" else "${offer.discountPriceCoins} М",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isClaimed) HobbitEmerald else HobbitGold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSelectShopCategoryTab("den_comfort")
                        onSelectShopEntryId(null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (shopCategoryTab == "den_comfort") HobbitEmerald else HobbitElevatedBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "🏠 Интерьер ($itemsInCurrentStep из 10)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        onSelectShopCategoryTab("expedition")
                        onSelectShopEntryId(null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (shopCategoryTab == "expedition") HobbitOrange else HobbitElevatedBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "🎒 Снаряжение",
                        color = HobbitCream,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
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
                                onSelectShopDenStepFilter(stepNum)
                                onSelectShopEntryId(null)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (shopDenStepFilter == stepNum) FontWeight.Bold else FontWeight.Medium,
                                color = if (shopDenStepFilter == stepNum) HobbitEmerald else HobbitMuted
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
                                color = if (isSel) HobbitElevatedBg else HobbitCardBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isSel) {
                                            Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(14.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        onSelectShopEntryId(if (isSel) null else item.id)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(text = item.emoji, fontSize = 18.sp)
                                    Text(
                                        text = item.shortTitleRu,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = HobbitCream,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isOwned && item.category != ChantalShopCategory.CONSUMABLE) "Куплено ✓" else "${item.priceCoins} М",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOwned && item.category != ChantalShopCategory.CONSUMABLE) HobbitEmerald else HobbitGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedShopEntryId != null,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DEFAULT_CHANTAL_MERCHANT_OFFERS.find { it.id == selectedShopEntryId }?.let { offer ->
                        val isClaimed = offer.id in claimedMerchantOfferIds
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, HobbitGold, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${offer.emoji} ${offer.titleRu}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = HobbitCream
                                )
                                Text(
                                    text = offer.effectRu,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    color = HobbitEmerald
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onBuyMerchantOffer(offer.id) },
                                        enabled = !isClaimed && coins >= offer.discountPriceCoins,
                                        colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg)
                                    ) {
                                        Text(
                                            text = if (isClaimed) "Куплено ✓" else "Купить · ${offer.discountPriceCoins} 🪙",
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

                    DEFAULT_CHANTAL_SHOP_ITEMS.find { it.id == selectedShopEntryId }?.let { item ->
                        val owned = item.id in ownedShopItemIds
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = HobbitElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, HobbitGold, RoundedCornerShape(16.dp))
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
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = HobbitCream,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "✕",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = HobbitMuted,
                                        modifier = Modifier.clickable { onSelectShopEntryId(null) }
                                    )
                                }
                                Text(
                                    text = item.gameplayEffectRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HobbitEmerald
                                )
                                Text(
                                    text = "«${item.descriptionRu}»",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp,
                                    color = HobbitCream
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onBuyShopItem(item) },
                                        enabled = coins >= item.priceCoins && (!owned || item.category == ChantalShopCategory.CONSUMABLE),
                                        colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg)
                                    ) {
                                        Text(
                                            text = if (owned && item.category != ChantalShopCategory.CONSUMABLE) "Куплено ✓" else "Купить · ${item.priceCoins} 🪙",
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
    }
}

@Composable
internal fun ChantalJournalSectionCard(
    journalList: List<ChantalStoryEpisode>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HobbitBg,
        modifier = modifier.fillMaxWidth()
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
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = HobbitElevatedBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Слой 1 (Вечная летопись с Похода #1):",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitGold
                    )
                    Text(
                        text = "• [Поход #1] Старая мельница | Барсук Брамбл -> «Спрятать медальон в шкатулку тайн»\n• [Поход #2] Янтарная дубрава | Ворон Корвус -> «Попросить приносить лесные письма»\n• [Поход #3] Старая мельница | Перевозчик Хьюго -> «Пообещать найти ключ от ларца»",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = HobbitCream
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
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold
                        )
                        Text(
                            text = entry.storyTextRu,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp,
                            color = HobbitCream
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalAiLoreSectionCard(
    selectedAiProvider: String,
    onSelectAiProvider: (String) -> Unit,
    selectedOpenRouterModel: String,
    onSelectOpenRouterModel: (String) -> Unit,
    customOpenRouterModelInput: String,
    onCustomOpenRouterModelChange: (String) -> Unit,
    selectedDifficulty: String,
    onSelectDifficulty: (String) -> Unit,
    customLoreNotes: String,
    onCustomLoreNotesChange: (String) -> Unit,
    showTraitArrows: Boolean,
    onToggleTraitArrows: () -> Unit,
    forceTransformPreview: Boolean,
    onToggleTransformPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HobbitBg,
        modifier = modifier.fillMaxWidth()
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
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChantalAiProvider.entries.forEach { prov ->
                    Button(
                        onClick = { onSelectAiProvider(prov.code) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAiProvider == prov.code) HobbitOrange else HobbitElevatedBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            prov.titleRu,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitCream
                        )
                    }
                }
            }
            if (selectedAiProvider == ChantalAiProvider.OPENROUTER.code) {
                OPENROUTER_ANDROID_MODELS.forEach { (modelId, modelLabel) ->
                    val isChosen = customOpenRouterModelInput.isBlank() && selectedOpenRouterModel == modelId
                    OutlinedButton(
                        onClick = {
                            onSelectOpenRouterModel(modelId)
                            onCustomOpenRouterModelChange("")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${if (isChosen) "✓ " else ""}$modelLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChosen) HobbitGold else HobbitCream
                        )
                    }
                }
                OutlinedTextField(
                    value = customOpenRouterModelInput,
                    onValueChange = onCustomOpenRouterModelChange,
                    label = { Text("Или свой ID модели с OpenRouter") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChantalStoryDifficulty.entries.forEach { diff ->
                    OutlinedButton(
                        onClick = { onSelectDifficulty(diff.code) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Text(
                            text = diff.titleRu,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = if (selectedDifficulty == diff.code) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedDifficulty == diff.code) HobbitGold else HobbitMuted
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleTraitArrows,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (showTraitArrows) "Стрелки черт: Вкл" else "Стрелки черт: Выкл",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitCream
                    )
                }
                OutlinedButton(
                    onClick = onToggleTransformPreview,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (forceTransformPreview) "Стадия IV: Вкл" else "Осмотр Стадии IV",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitGold
                    )
                }
            }
            OutlinedTextField(
                value = customLoreNotes,
                onValueChange = onCustomLoreNotesChange,
                label = { Text("Заметки лора и персонализации для ИИ") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun BagDropSlotTile(
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

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) HobbitElevatedBg else HobbitCardBg,
        modifier = modifier
            .offset { IntOffset(0, dropOffsetY.value.roundToInt()) }
            .then(
                if (isSelected || isNew) {
                    Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
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
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitGold
                )
            }
            Text(
                text = item.emoji,
                fontSize = 20.sp,
                modifier = Modifier.scale(iconScale.value)
            )
            Text(
                text = item.shortTitleRu,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
