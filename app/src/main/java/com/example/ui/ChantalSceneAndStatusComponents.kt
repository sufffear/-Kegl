package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.ChantalMaturityProgress
import com.example.domain.ChantalShopItem
import com.example.domain.ChantalStageStatus
import com.example.domain.ChantalStoryEpisode

@Composable
internal fun ChantalPrologueCard(
    onCompletePrologue: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                color = HobbitGold
            )
            Text(
                text = "Неожиданная находка в лесу",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream
            )
            Text(
                text = "Возвращаясь вечером мимо старой водяной мельницы, ты услышал тихий шорох под корнями дуба. В мокром папоротнике, дрожа от ливня и поджав лапку, сидел крошечный огненно-рыжий лисёнок с удивительно осмысленными янтарными глазами.",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                color = HobbitCream
            )
            Text(
                text = "Каждая тренировка Кегеля даёт +10 Энергии. За 30 Энергии Шанталь сама выбирает маршрут на карте, отправляется в часовой поход и приносит истории, монеты, опыт взросления и находки!",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                color = HobbitEmerald
            )
            Button(
                onClick = onCompletePrologue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HobbitElevatedBg,
                    contentColor = HobbitCream
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("btn_complete_chantal_prologue")
            ) {
                Text(
                    text = "Согреть лисёнка у камина и назвать Шанталь",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun ChantalCompactHeader(
    stageTitleRu: String,
    coins: Int,
    currentEnergy: Int,
    currentStreakDays: Int,
    isMenuOpen: Boolean,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chantal_compact_header"),
        shape = RoundedCornerShape(18.dp),
        color = HobbitCardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = HobbitElevatedBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🦊", fontSize = 18.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Шанталь",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$stageTitleRu · $currentStreakDays дн.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = HobbitMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Монеты
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HobbitElevatedBg
                ) {
                    Text(
                        text = "🪙 $coins",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Энергия
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HobbitElevatedBg
                ) {
                    Text(
                        text = "⚡ $currentEnergy",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Кнопка открытия модального меню
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HobbitElevatedBg,
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, HobbitGold.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        .clickable { onToggleMenu() }
                        .testTag("btn_chantal_menu")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isMenuOpen) "✕" else "☰",
                            color = HobbitGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalCentralDenScene(
    stageInfo: ChantalStageStatus,
    portraitResId: Int,
    denInteriorResId: Int,
    denTierNumber: Int,
    itemsInCurrentStep: Int,
    totalDenItemsCount: Int,
    isHeartPulseActive: Boolean,
    tendernessHealBonusPct: Int,
    activeGearList: List<ChantalShopItem>,
    hasActiveRoadAmulet: Boolean,
    isJourneyActive: Boolean,
    activeRegionTitleRu: String,
    modifier: Modifier = Modifier
) {
    var preferInteriorBackdrop by remember(totalDenItemsCount, stageInfo.stageNumber) {
        mutableStateOf(!stageInfo.isTransformed && totalDenItemsCount >= 10)
    }
    val primaryResId = if (preferInteriorBackdrop) denInteriorResId else portraitResId
    val cameoResId = if (preferInteriorBackdrop) portraitResId else denInteriorResId

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fantasyFrame(HobbitGold)
            .testTag("chantal_interactive_portrait"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HobbitCardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(288.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Crossfade(
                targetState = primaryResId,
                animationSpec = tween(durationMillis = 320),
                label = "chantal_scene_crossfade"
            ) { targetRes ->
                Image(
                    painter = painterResource(id = targetRes),
                    contentDescription = stageInfo.titleRu,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Мягкая градиентная виньетка сверху и снизу для высокой контрастности текста
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                        )
                    )
            )

            // Верхний левый бейдж: Уровень Домика
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HobbitCardBg.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "🏠 Домик Ур. $denTierNumber ($itemsInCurrentStep/10)",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    color = HobbitCream,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Верхний правый переключатель: Портрет / Интерьер
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HobbitCardBg.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clickable { preferInteriorBackdrop = !preferInteriorBackdrop }
                    .testTag("btn_toggle_scene_view")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = cameoResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = if (preferInteriorBackdrop) "Портрет" else "Интерьер",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitGold
                    )
                }
            }

            // Всплывающий отклик на ласку
            androidx.compose.animation.AnimatedVisibility(
                visible = isHeartPulseActive,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = HobbitCardBg.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, HobbitPink, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "♥ +$tendernessHealBonusPct% к настроению · +3 XP!",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitPink,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }

            // Нижний бар сцены: статус и надетые предметы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HobbitCardBg.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = if (isJourneyActive) {
                            "🧭 В походе: «$activeRegionTitleRu»"
                        } else if (stageInfo.isTransformed) {
                            "✨ Девушка-лисица у камина"
                        } else {
                            "🔥 У тёплого камина"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isJourneyActive) HobbitGold else HobbitCream,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (activeGearList.isNotEmpty() || hasActiveRoadAmulet) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HobbitCardBg.copy(alpha = 0.92f)
                    ) {
                        val badges = buildList {
                            if (hasActiveRoadAmulet) add("🧿")
                            activeGearList.forEach { add(it.emoji) }
                        }.joinToString(" ")
                        Text(
                            text = badges,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalLevelXpMoodPanel(
    maturityProgress: ChantalMaturityProgress,
    moodPct: Int,
    totalJourneys: Int,
    recentXpGainText: String?,
    dominantTraitNameRu: String,
    denAccordionTab: String,
    itemsInCurrentStep: Int,
    onToggleTraits: () -> Unit,
    onToggleInterior: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedXpProgress by animateFloatAsState(
        targetValue = (maturityProgress.progressPct / 100f).coerceIn(0.03f, 1f),
        animationSpec = tween(durationMillis = 360),
        label = "chantal_xp_bar_anim"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chantal_status_bar"),
        shape = RoundedCornerShape(18.dp),
        color = HobbitCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HobbitElevatedBg
                    ) {
                        Text(
                            text = "Ур. ${maturityProgress.maturityLevel}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitGold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "✨ ${maturityProgress.xpInStage} / ${maturityProgress.stageTargetXp} XP (${maturityProgress.progressPct}%)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitCream
                    )
                }

                Text(
                    text = "💛 $moodPct%",
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitEmerald
                )
            }

            LinearProgressIndicator(
                progress = { animatedXpProgress },
                color = HobbitGold,
                trackColor = HobbitBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recentXpGainText ?: "До след. стадии: ${maturityProgress.xpToNextStage} XP · Походов: $totalJourneys",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (recentXpGainText != null) FontWeight.Bold else FontWeight.Medium,
                    color = if (recentXpGainText != null) HobbitPink else HobbitMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Компактные чипы раскрытия Характера и Интерьера
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (denAccordionTab == "traits") HobbitElevatedBg else HobbitBg,
                        modifier = Modifier
                            .clickable { onToggleTraits() }
                            .testTag("btn_accordion_traits")
                    ) {
                        Text(
                            text = "✨ $dominantTraitNameRu ${if (denAccordionTab == "traits") "▲" else "▼"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            color = if (denAccordionTab == "traits") HobbitGold else HobbitCream,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (denAccordionTab == "housing") HobbitElevatedBg else HobbitBg,
                        modifier = Modifier
                            .clickable { onToggleInterior() }
                            .testTag("btn_accordion_housing")
                    ) {
                        Text(
                            text = "🏠 $itemsInCurrentStep/10 ${if (denAccordionTab == "housing") "▲" else "▼"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            color = if (denAccordionTab == "housing") HobbitEmerald else HobbitCream,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChantalQuickActionsRow(
    canPet: Boolean,
    isJourneyActive: Boolean,
    isExpeditionPanelOpen: Boolean,
    bagCount: Int,
    hasPendingStory: Boolean,
    onPetClick: () -> Unit,
    onExpeditionClick: () -> Unit,
    onBagClick: () -> Unit,
    onStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chantal_quick_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Погладить
        QuickActionButton(
            emoji = "♥",
            label = "Погладить",
            badge = if (canPet) "+3 XP" else "Уютно",
            isHighlighted = canPet,
            accentColor = HobbitPink,
            onClick = { if (canPet) onPetClick() },
            testTag = "btn_pet_chantal",
            modifier = Modifier.weight(1f)
        )

        // 2. Поход (открывает и закрывает отдельную игровую панель подготовки и 4 слотов экипировки)
        QuickActionButton(
            emoji = "🧭",
            label = "Поход",
            badge = when {
                isJourneyActive -> "В пути"
                isExpeditionPanelOpen -> "Скрыть ▲"
                else -> "Сборы ▼"
            },
            isHighlighted = isExpeditionPanelOpen || isJourneyActive,
            accentColor = HobbitGold,
            onClick = onExpeditionClick,
            testTag = "btn_quick_expedition",
            modifier = Modifier.weight(1f)
        )

        // 3. Сумка
        QuickActionButton(
            emoji = "🎒",
            label = "Сумка",
            badge = "$bagCount предм.",
            isHighlighted = false,
            accentColor = HobbitEmerald,
            onClick = onBagClick,
            testTag = "btn_quick_bag",
            modifier = Modifier.weight(1f)
        )

        // 4. История
        QuickActionButton(
            emoji = "📖",
            label = "История",
            badge = if (hasPendingStory) "Новая!" else "Летопись",
            isHighlighted = hasPendingStory,
            accentColor = HobbitGold,
            onClick = onStoryClick,
            testTag = "btn_quick_story",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    emoji: String,
    label: String,
    badge: String,
    isHighlighted: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.01f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "quick_action_scale"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) HobbitElevatedBg else HobbitCardBg,
        modifier = modifier
            .scale(scale)
            .heightIn(min = 68.dp)
            .then(
                if (isHighlighted) {
                    Modifier.border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = HobbitCream,
                maxLines = 1
            )
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.SansSerif,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) accentColor else HobbitMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun ChantalCompactStoryEventCard(
    pendingEpisode: ChantalStoryEpisode?,
    fillerTextInput: String,
    onFillerTextChange: (String) -> Unit,
    onSelectEpisodeChoice: (ChantalStoryEpisode, Int) -> Unit,
    onSubmitFillerResponse: (ChantalStoryEpisode) -> Unit,
    petDialogueBubble: String?,
    lastReactionBanner: String?,
    latestJournalEpisode: ChantalStoryEpisode?,
    dominantTraitNameRu: String,
    onOpenFullJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (pendingEpisode != null) {
                    Modifier.border(1.5.dp, HobbitGold, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .testTag("chantal_compact_story_card"),
        shape = RoundedCornerShape(20.dp),
        color = if (pendingEpisode != null) HobbitElevatedBg else HobbitCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize(animationSpec = tween(220)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pendingEpisode != null) {
                Text(
                    text = if (pendingEpisode.isFiller) "Побочный эпизод (Филлер)" else "Основной сюжет · ${pendingEpisode.regionTitleRu}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    color = HobbitGold
                )
                Text(
                    text = pendingEpisode.titleRu,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitCream
                )
                Text(
                    text = "Маршрут: ${pendingEpisode.mapNoteRu}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                    color = HobbitEmerald
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HobbitCardBg
                ) {
                    Text(
                        text = pendingEpisode.storyTextRu,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                        color = HobbitCream,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    text = "Добыча: ${pendingEpisode.lootItem.emoji} «${pendingEpisode.lootItem.titleRu}» · +${pendingEpisode.coinsEarned} М",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = HobbitGold
                )
                Text(
                    text = "Как ты отреагируешь на рассказ Шанталь?",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = HobbitMuted
                )
                pendingEpisode.choices.forEachIndexed { idx, opt ->
                    Button(
                        onClick = { onSelectEpisodeChoice(pendingEpisode, idx) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HobbitCardBg,
                            contentColor = HobbitCream
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_story_choice_$idx")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${idx + 1}. ${opt.labelRu}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = fillerTextInput,
                    onValueChange = onFillerTextChange,
                    label = { Text("Или свой ответ / поручение (режим Филлера)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (fillerTextInput.isNotBlank()) {
                    Button(
                        onClick = { onSubmitFillerResponse(pendingEpisode) },
                        colors = ButtonDefaults.buttonColors(containerColor = HobbitCardBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Отправить свой текст (Филлер)",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = HobbitCream
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (petDialogueBubble != null) "🦊 Живой отклик у камина" else "📜 Текущее событие",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitGold
                    )
                    Text(
                        text = "Летопись →",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitEmerald,
                        modifier = Modifier.clickable { onOpenFullJournal() }
                    )
                }

                if (petDialogueBubble != null) {
                    Text(
                        text = "Ведущая черта: $dominantTraitNameRu · +3 XP взросления",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitPink
                    )
                    Text(
                        text = petDialogueBubble,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                        color = HobbitCream
                    )
                } else if (latestJournalEpisode != null) {
                    Text(
                        text = "Глава #${latestJournalEpisode.journeyNumber}: ${latestJournalEpisode.titleRu}",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = HobbitCream
                    )
                    Text(
                        text = latestJournalEpisode.storyTextRu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = HobbitMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Шанталь отдыхает у тёплого очага. Погладь её перед дорогой или нажми «Поход», чтобы снарядить 4 слота экипировки и отправить в лес!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        fontStyle = FontStyle.Italic,
                        color = HobbitMuted
                    )
                }

                if (lastReactionBanner != null) {
                    Text(
                        text = lastReactionBanner,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = HobbitEmerald
                    )
                }
            }
        }
    }
}
