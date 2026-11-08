package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.*
import org.json.JSONObject

private fun records(memory: JSONObject, key: String): List<JSONObject> = memory.getJSONArray(key).let { a ->
    (0 until a.length()).map { a.getJSONObject(it) }
}

@Composable
internal fun ChantalOpenTasks(memory: JSONObject) {
    val tasks = records(memory, "promises").filter { it.getString("status") == "open" }
    Text("Незавершённые дела · ${tasks.size}/3", color = HobbitGold)
    if (tasks.isEmpty()) Text("Обещаний пока нет. Можно просто гулять и знакомиться с миром.", color = HobbitMuted)
    tasks.forEach { task ->
        val source = records(memory, "events").firstOrNull { it.getString("episodeId") == task.getString("sourceEpisodeId") }
        Text("${task.getString("descriptionRu")}\n${source?.optString("regionRu").orEmpty()} · без срока", color = HobbitCream)
    }
}

@Composable
internal fun DiaryEntry(title: String, detail: String) {
    var expanded by remember(title, detail) { mutableStateOf(false) }
    Surface(color = HobbitElevatedBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text("$title ${if (expanded) "▴" else "▾"}", color = HobbitGold) }
            if (expanded) Text(detail, color = HobbitCream, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Read-only presentation of accepted facts: opening the diary never grants rewards. */
@Composable
internal fun ChantalWorldDiary(memory: JSONObject, journal: List<ChantalStoryEpisode>, inventory: List<ChantalInventoryLootItem>, interactions: JSONObject = com.example.data.ChantalWorldInteractions.empty(), onCompare: (String, String) -> Unit = { _, _ -> }) {
    var selectedClue by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("Дела") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Дневник нашего мира", color = HobbitCream, style = MaterialTheme.typography.titleLarge)
        // Two rows remain readable with large system fonts on narrow phones.
        listOf(listOf("Дела", "Знакомые"), listOf("Шкатулка", "Магия")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    OutlinedButton(onClick = { tab = label }, modifier = Modifier.weight(1f)) {
                        Text(if (tab == label) "• $label" else label, color = HobbitCream)
                    }
                }
            }
        }
        when (tab) {
            "Дела" -> {
                ChantalOpenTasks(memory)
                records(memory, "promises").filter { it.getString("status") == "resolved" }.forEach {
                    DiaryEntry("✓ ${it.getString("descriptionRu")}", "Дело завершено.\n" + sourceText(memory, it))
                }
            }
            "Знакомые" -> {
                val visits = journal.filter { !it.chosenActionSummaryRu.isNullOrBlank() && it.npcNameRu.isNotBlank() }.groupBy { it.npcNameRu }
                if (visits.isEmpty()) Text("Здесь появятся знакомые из завершённых приключений.", color = HobbitMuted)
                visits.forEach { (name, stories) ->
                    DiaryEntry("$name · встреч: ${stories.size}", (com.example.data.ChantalLivingWorld.profile(name)?.let { "${it.habit}\n${it.concern}\n\n" } ?: "") + stories.joinToString("\n\n") {
                        "${it.titleRu}\n${it.memorySummaryRu ?: it.mapNoteRu}\nТвой ответ: ${it.chosenActionSummaryRu}"
                    })
                }
                val gifts = interactions.getJSONArray("gifts")
                repeat(gifts.length()) { index ->
                    val gift = gifts.getJSONObject(index)
                    DiaryEntry("🎁 ${gift.getString("npcRu")} · ${gift.getString("itemTitleRu")}", gift.getString("textRu"))
                }
                records(memory, "relationships").forEach { DiaryEntry(it.getString("descriptionRu"), sourceText(memory, it)) }
            }
            "Шкатулка" -> {
                val clues = inventory.filter { it.category == ChantalLootCategory.ROYAL_CLUE }
                if (clues.isEmpty()) Text("Пока нет королевских улик. Найденные улики появятся здесь.", color = HobbitMuted)
                clues.forEach { DiaryEntry("${it.emoji} ${it.titleRu}", "${it.descriptionRu}\nНайдена: ${it.foundAtRegionRu}\n${if (it.inspectedBonusClaimed) "Уже исследована" else "Исследовать можно в Сумке находок"}") }
                val studied = clues.filter { it.inspectedBonusClaimed }
                Text("Сопоставление улик", color = HobbitGold)
                Text("Изучи две находки в сумке, затем выбери их здесь. Сравнение не расходует вещи.", color = HobbitMuted)
                studied.forEach { clue ->
                    OutlinedButton(onClick = {
                        val first = selectedClue
                        if (first == null) selectedClue = clue.id
                        else if (first == clue.id) selectedClue = null
                        else { onCompare(first, clue.id); selectedClue = null }
                    }) { Text("${if (selectedClue == clue.id) "✓ " else ""}${clue.titleRu}", color = HobbitCream) }
                }
                val comparisons = interactions.getJSONArray("comparisons")
                repeat(comparisons.length()) { index ->
                    DiaryEntry("Сопоставление ${index + 1}", comparisons.getJSONObject(index).getString("textRu"))
                }
                records(memory, "keepsakes").forEach { DiaryEntry(it.getString("descriptionRu"), sourceText(memory, it)) }
            }
            "Магия" -> {
                val items = records(memory, "magicItems")
                if (items.isEmpty()) Text("Пока нет находок с известным волшебным свойством.", color = HobbitMuted)
                items.forEach { DiaryEntry(it.getString("descriptionRu"), "${it.getString("propertyRu")}\n${sourceText(memory, it)}") }
            }
        }
    }
}

private fun sourceText(memory: JSONObject, fact: JSONObject): String {
    val event = records(memory, "events").firstOrNull { it.getString("episodeId") == fact.getString("sourceEpisodeId") }
    return event?.let { "${it.getString("regionRu")} · поход ${it.getInt("journeyNumber")}\n${it.getString("summaryRu")}\nТвой ответ: ${it.getString("choiceRu")}" }.orEmpty()
}
