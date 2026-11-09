package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.domain.ChantalJourneyClock

internal enum class ChantalPage(val title: String) {
    HOME("Шанталь"), WORLD("Мир Шанталь"), MAP("Карта троп"), BAG("Сумка находок"),
    SHOP("Лесная лавка"), JOURNAL("История"), AI("Настройки Шанталь"),
    DEVELOPMENT("Характер и развитие"), INTERIOR("Интерьер"), PREPARE("Подготовка к походу"),
    TRAIL("Поход"), STORY("Рассказ Шанталь")
}
/** Finished preparation/travel pages are not return destinations after a state transition. */
internal fun chantalPagesAfterOpen(current: List<String>, next: ChantalPage): List<String> {
    if (next == ChantalPage.TRAIL || next == ChantalPage.STORY) return listOf(ChantalPage.HOME.name, next.name)
    val index = current.indexOf(next.name)
    return if (index >= 0) current.take(index + 1) else current + next.name
}
internal enum class ChantalHomeAction(val label: String, val line: String) {
    REWARD("Посмотреть награду", "Находки уже дома."),
    STORY("Читать историю", "Я вернулась с рассказом."),
    RETURN("Встретить Шанталь", "Поход завершён."),
    TRAVELLING("Посмотреть поход", "Я ещё в пути."),
    PREPARE("Собраться в поход", "Можно готовиться к прогулке.")
}
internal fun chantalHomeAction(reward: Boolean, pending: Boolean, start: Long, now: Long): ChantalHomeAction = when {
    reward -> ChantalHomeAction.REWARD
    pending -> ChantalHomeAction.STORY
    start > 0 && ChantalJourneyClock.ready(start, now) -> ChantalHomeAction.RETURN
    start > 0 -> ChantalHomeAction.TRAVELLING
    else -> ChantalHomeAction.PREPARE
}

@Composable
internal fun ChantalHero(portrait: Int, interior: Int, upgraded: Boolean, transformed: Boolean,
    canPet: Boolean, onPet: () -> Unit) {
    val hero = if (upgraded && !transformed) interior else portrait
    val height = ((LocalConfiguration.current.screenHeightDp - 180) * 0.4f).coerceIn(160f, 300f).dp
    Surface(shape = RoundedCornerShape(20.dp), color = HobbitCardBg) {
        Box(Modifier.fillMaxWidth().height(height).testTag("chantal_interactive_portrait")) {
            Image(painterResource(hero), "Портрет Шанталь и её домик", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (hero != portrait) Image(painterResource(portrait), "Шанталь", Modifier
                .align(Alignment.BottomStart).padding(8.dp).size(64.dp), contentScale = ContentScale.Fit)
            IconButton(onClick = onPet, enabled = canPet, modifier = Modifier.align(Alignment.BottomEnd)
                .padding(8.dp).size(48.dp).testTag("btn_pet_chantal")) {
                Icon(Icons.Default.Favorite, "Погладить Шанталь", tint = if (canPet) HobbitGold else HobbitMuted)
            }
        }
    }
}

@Composable
internal fun ChantalContextAction(action: ChantalHomeAction, onAction: () -> Unit) {
    VelarisPrimaryAction(action.label, onAction, Modifier.testTag("chantal_context_action"))
}

@Composable
internal fun ChantalQuickLinks(onBag: () -> Unit, onHistory: () -> Unit, onInterior: () -> Unit) {
    val links = listOf(Triple("Сумка", "btn_quick_bag", onBag), Triple("История", "btn_quick_story", onHistory),
        Triple("Интерьер", "btn_quick_interior", onInterior))
    if (LocalDensity.current.fontScale > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        links.forEach { (text, tag, action) -> OutlinedButton(action, Modifier.fillMaxWidth().heightIn(min=48.dp).testTag(tag)) { Text(text) } }
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        links.forEach { (text, tag, action) -> OutlinedButton(action, Modifier.weight(1f).heightIn(min=48.dp).testTag(tag),
            contentPadding = PaddingValues(horizontal=4.dp,vertical=8.dp)) { Text(text, style=MaterialTheme.typography.labelLarge) } }
    }
}

@Composable
internal fun ChantalWorldMenu(coins: Int, energy: Int, onOpen: (ChantalPage) -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.testTag("chantal_world_menu")) {
        Text("Монеты: $coins · Энергия: $energy", color=HobbitCream)
        listOf(ChantalPage.MAP, ChantalPage.SHOP, ChantalPage.DEVELOPMENT, ChantalPage.AI).forEach { page ->
            VelarisSectionLink(page.title, when(page) {
                ChantalPage.MAP -> "Регионы и маршруты"
                ChantalPage.SHOP -> "Снаряжение и вещи для домика"
                ChantalPage.DEVELOPMENT -> "Возраст, опыт и черты характера"
                else -> "Модель, ключ и параметры историй"
            }, { onOpen(page) }, Modifier.testTag("chantal_world_${page.name.lowercase()}"))
        }
    }
}
