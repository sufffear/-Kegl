package com.example.data

import com.example.domain.*
import org.json.JSONObject

data class ChantalShelfItem(val id: String, val title: String, val emoji: String, val memory: String, val reaction: String)

/** Display selection never owns or consumes the underlying inventory item. */
object ChantalDenShelf {
    const val CAPACITY = 3
    fun items(memory: JSONObject, journal: List<ChantalStoryEpisode>, inventory: List<ChantalInventoryLootItem>, owned: List<String>): List<ChantalShelfItem> {
        val keepsakes = memory.getJSONArray("keepsakes")
        val result = mutableListOf<ChantalShelfItem>()
        repeat(keepsakes.length()) { i ->
            val fact = keepsakes.getJSONObject(i)
            val source = journal.firstOrNull { it.id == fact.getString("sourceEpisodeId") && !it.chosenActionSummaryRu.isNullOrBlank() }
            val event = memory.getJSONArray("events").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.getString("episodeId") == fact.getString("sourceEpisodeId") } }
            if (source != null || event != null) result += ChantalShelfItem("memory:" + fact.getString("id"), fact.getString("descriptionRu"), "✨",
                source?.let { "${it.titleRu}\n\n${it.storyTextRu}\n\nТвой ответ: ${it.chosenActionSummaryRu}" }
                    ?: "${event!!.getString("summaryRu")}\nТвой ответ: ${event.getString("choiceRu")}",
                if (fact.getString("id") == "wooden_light") "Я оставлю этот огонёк здесь. Смотришь на него — и сразу вспоминаешь Брамбла."
                else "Пусть стоит на виду. Я хочу помнить, откуда эта штука у меня.")
        }
        inventory.filter { it.category == ChantalLootCategory.DEN_TROPHY }.forEach { item ->
            val story = journal.firstOrNull { it.lootItem.id == item.id && !it.chosenActionSummaryRu.isNullOrBlank() }
            result += ChantalShelfItem("trophy:" + item.id, item.titleRu, item.emoji,
                "${item.descriptionRu}\nНайдено: ${item.foundAtRegionRu}" + (story?.let { "\n\n${it.storyTextRu}\nТвой ответ: ${it.chosenActionSummaryRu}" } ?: ""),
                "Вот эту находку я хочу оставить дома. На полке она точно не потеряется!")
        }
        DEFAULT_CHANTAL_SHOP_ITEMS.filter { it.id in owned && it.category == ChantalShopCategory.DEN_COMFORT }.forEach {
            result += ChantalShelfItem("decor:" + it.id, it.titleRu, it.emoji, "${it.descriptionRu}\n${it.gameplayEffectRu}\nКуплено в лесной лавке.", "Так. Это теперь наше. Мне нравится, как дома становится уютнее.")
        }
        return result.distinctBy { it.id }
    }
    fun select(selected: List<String>, id: String, available: List<ChantalShelfItem>): List<String> {
        require(available.any { it.id == id })
        val current = selected.distinct().filter { key -> available.any { it.id == key } }
        if (id in current) return current - id
        require(current.size < CAPACITY)
        return current + id
    }
}
