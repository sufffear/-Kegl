package com.example.data

import com.example.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Detached, validated records. No currency or friendship points are minted here. */
object ChantalWorldInteractions {
    fun empty() = JSONObject().put("gifts", JSONArray()).put("comparisons", JSONArray())
    fun validate(value: JSONObject): JSONObject {
        for (category in listOf("gifts", "comparisons")) {
            val seen = mutableSetOf<String>()
            val entries = value.getJSONArray(category)
            repeat(entries.length()) {
                val record = entries.getJSONObject(it)
                require(seen.add(record.getString("id")))
                require(record.getString("textRu").isNotBlank())
                if (category == "gifts") {
                    require(record.getString("npcRu").isNotBlank())
                    require(record.getString("itemTitleRu").isNotBlank())
                    require(record.getString("sourceEpisodeId").isNotBlank())
                }
            }
        }
        value.optJSONObject("silverThreadPractice")?.let {
            require(it.get("choice") is Int && it.getInt("choice") in 0..2)
            require(it.getString("sourceEpisodeId").isNotBlank())
            require(it.getString("textRu") == ChantalMagicPractice.reactions[it.getInt("choice")])
        }
        return value
    }
    fun gift(state: JSONObject, itemId: String, npc: String, inventory: List<ChantalInventoryLootItem>, journal: List<ChantalStoryEpisode>): JSONObject {
        val item = inventory.single { it.id == itemId && it.category == ChantalLootCategory.TRINKET }
        val meeting = journal.first { it.npcNameRu == npc && !it.chosenActionSummaryRu.isNullOrBlank() }
        val copy = JSONObject(validate(state).toString())
        val gifts = copy.getJSONArray("gifts")
        require((0 until gifts.length()).none {
            val old = gifts.getJSONObject(it)
            old.getString("npcRu") == npc && old.getString("itemTitleRu").equals(item.titleRu, ignoreCase = true)
        }) { "Этот знакомый уже получил такую вещь" }
        gifts.put(JSONObject().put("id", item.id).put("npcRu", npc).put("itemTitleRu", item.titleRu)
            .put("sourceEpisodeId", meeting.id)
            .put("textRu", "$npc получил подарок «${item.titleRu}» после знакомства в «${meeting.regionTitleRu}»."))
        return validate(copy)
    }
    fun compare(state: JSONObject, firstId: String, secondId: String, inventory: List<ChantalInventoryLootItem>): JSONObject {
        require(firstId != secondId)
        val items = listOf(firstId, secondId).map { id -> inventory.single { it.id == id && it.category == ChantalLootCategory.ROYAL_CLUE && it.inspectedBonusClaimed } }
        val id = JSONArray(listOf(firstId, secondId).sorted()).toString()
        val copy = JSONObject(validate(state).toString())
        val records = copy.getJSONArray("comparisons")
        if ((0 until records.length()).any { records.getJSONObject(it).getString("id") == id }) return copy
        val samePlace = items[0].foundAtRegionRu.isNotBlank() && items[0].foundAtRegionRu == items[1].foundAtRegionRu
        val result = if (samePlace) "Обе находки из «${items[0].foundAtRegionRu}». Это общая зацепка, но ещё не доказательство связи. Можно вернуться туда и расспросить местных." else "Находки из разных мест. По имеющимся сведениям связь пока не установлена. Сохраним обе версии и дождёмся новых сведений."
        records.put(JSONObject().put("id", id).put("textRu", "${items[0].titleRu} + ${items[1].titleRu}\n$result\n\n${items[0].descriptionRu}\n\n${items[1].descriptionRu}"))
        return validate(copy)
    }
}
