package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*

@Composable
internal fun ChantalDenShelfView(items: List<ChantalShelfItem>, selected: List<String>, onSelect: (String) -> Unit) {
    var arranging by remember { mutableStateOf(false) }
    var inspected by remember { mutableStateOf<String?>(null) }
    val displayed = selected.mapNotNull { id -> items.firstOrNull { it.id == id } }.take(ChantalDenShelf.CAPACITY)
    Surface(color = HobbitElevatedBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Полка воспоминаний · ${displayed.size}/3", color = HobbitGold, style = MaterialTheme.typography.titleMedium)
            if (displayed.isEmpty()) Text("Здесь можно поставить памятную находку, трофей или покупку для домика.", color = HobbitCream)
            displayed.forEach { item ->
                OutlinedButton(onClick = { inspected = item.id }, modifier = Modifier.fillMaxWidth().testTag("shelf_item_${item.id}")) {
                    Text("${item.emoji} ${item.title}", color = HobbitCream)
                }
            }
            TextButton(onClick = { arranging = true }, modifier = Modifier.testTag("shelf_arrange")) { Text("Расставить вещи", color = HobbitGold) }
        }
    }
    if (arranging || inspected != null) Dialog(
        onDismissRequest = { arranging = false; inspected = null }, properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = HobbitCardBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (arranging) "Что поставим на полку?" else "Память о находке", color = HobbitGold, style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (arranging) {
                        Text("Три места. Снятая с полки вещь остаётся у тебя. Перестановка бесплатная.", color = HobbitCream)
                        if (items.isEmpty()) Text("Пока нечего выставить. Находки из походов и покупки для домика появятся здесь.", color = HobbitMuted)
                        items.forEach { item ->
                            OutlinedButton(onClick = { onSelect(item.id) }, enabled = item.id in selected || displayed.size < 3,
                                modifier = Modifier.fillMaxWidth().testTag("shelf_select_${item.id}")) {
                                Text("${if (item.id in selected) "✓ Снять" else "Поставить"} · ${item.emoji} ${item.title}", color = HobbitCream)
                            }
                        }
                    } else items.firstOrNull { it.id == inspected }?.let {
                        Text("${it.emoji} ${it.title}", color = HobbitCream, style = MaterialTheme.typography.titleMedium)
                        Text("Шанталь: «${it.reaction}»", color = HobbitGold)
                        Text(it.memory, color = HobbitCream, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                TextButton(onClick = { arranging = false; inspected = null }, modifier = Modifier.testTag("shelf_close")) { Text("Закрыть", color = HobbitGold) }
            }
        }
    }
}
