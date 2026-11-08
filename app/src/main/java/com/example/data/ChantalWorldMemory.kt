package com.example.data

import org.json.JSONArray
import org.json.JSONObject

/** Confirmed facts only. Generated prose must never be passed here before the player's choice. */
object ChantalWorldMemory {
    const val VERSION = 1
    fun empty() = JSONObject().put("version", VERSION).put("events", JSONArray())
        .put("promises", JSONArray()).put("relationships", JSONArray())
        .put("magicItems", JSONArray()).put("keepsakes", JSONArray())

    fun validate(memory: JSONObject): JSONObject {
        require(memory.getInt("version") == VERSION) { "Unsupported world memory version" }
        val events = memory.getJSONArray("events")
        val ids = mutableSetOf<String>()
        repeat(events.length()) {
            val event = events.getJSONObject(it)
            require(event.getString("episodeId").isNotBlank() && ids.add(event.getString("episodeId")))
            require(event.getString("summaryRu").isNotBlank())
            require(event.getString("choiceRu").isNotBlank())
            event.getInt("journeyNumber")
            event.getString("regionRu")
        }
        var open = 0
        for (key in listOf("promises", "relationships", "magicItems", "keepsakes")) {
            val records = memory.getJSONArray(key)
            val recordIds = mutableSetOf<String>()
            repeat(records.length()) {
                val record = records.getJSONObject(it)
                require(record.getString("id").isNotBlank() && recordIds.add(record.getString("id")))
                require(record.getString("sourceEpisodeId") in ids) { "Fact must refer to an accepted adventure" }
                require(record.getString("descriptionRu").isNotBlank())
                if (key == "promises") {
                    require(record.getString("status") in listOf("open", "resolved"))
                    if (record.getString("status") == "open") open++
                }
                if (key == "magicItems") require(record.getString("propertyRu").isNotBlank())
            }
        }
        require(open <= 3) { "At most three open promises" }
        return memory
    }

    /** Migrate old saves from accepted journal entries; no promises or relationships are invented. */
    fun reconcile(existing: JSONObject?, journal: JSONArray?): JSONObject {
        val memory = existing?.let { JSONObject(validate(it).toString()) } ?: empty()
        val events = memory.getJSONArray("events")
        val seen = (0 until events.length()).map { events.getJSONObject(it).getString("episodeId") }.toMutableSet()
        if (journal != null) for (i in journal.length() - 1 downTo 0) {
            val episode = ChantalProgressCodec.decodeChantalStoryEpisode(journal.getJSONObject(i))
            val choice = episode.chosenActionSummaryRu?.takeIf { it.isNotBlank() } ?: continue
            if (!seen.add(episode.id)) continue
            // A bounded factual extract, not an invented model summary. The full story remains in the journal.
            val excerpt = episode.storyTextRu.replace(Regex("\\s+"), " ").trim().take(600)
            events.put(JSONObject().put("episodeId", episode.id).put("journeyNumber", episode.journeyNumber)
                .put("regionRu", episode.regionTitleRu).put("summaryRu", episode.memorySummaryRu?.takeIf { it.isNotBlank() }?.take(700) ?: excerpt.ifBlank { episode.titleRu })
                .put("choiceRu", choice).put("themeRu", episode.narrativeThemeRu ?: ""))
        }
        return validate(memory)
    }

    /** Apply authored, confirmed consequences as one validated copy; invalid updates leave memory intact. */
    fun withFact(memory: JSONObject, category: String, fact: JSONObject): JSONObject {
        require(category in listOf("promises", "relationships", "magicItems", "keepsakes"))
        val copy = JSONObject(validate(memory).toString())
        val records = copy.getJSONArray(category)
        val index = (0 until records.length()).firstOrNull { records.getJSONObject(it).getString("id") == fact.getString("id") }
        val detached = JSONObject(fact.toString())
        if (index == null) records.put(detached) else records.put(index, detached)
        return validate(copy)
    }

    /** Bounded request view. Full history stays in the save; every selected fact retains its source. */
    fun context(memory: JSONObject, recentCount: Int = 6): JSONObject {
        validate(memory)
        val events = memory.getJSONArray("events")
        val byId = (0 until events.length()).associate { events.getJSONObject(it).getString("episodeId") to events.getJSONObject(it) }
        var result = empty()
        fun compactEvent(event: JSONObject) = JSONObject(event.toString())
            .put("summaryRu", event.getString("summaryRu").take(700))
            .put("choiceRu", event.getString("choiceRu").take(280))
            .put("regionRu", event.getString("regionRu").take(140))
        fun addEvent(target: JSONObject, event: JSONObject) {
            val selected = target.getJSONArray("events")
            if ((0 until selected.length()).none { selected.getJSONObject(it).getString("episodeId") == event.getString("episodeId") }) selected.put(compactEvent(event))
        }
        for (i in (events.length() - recentCount.coerceIn(0, 12)).coerceAtLeast(0) until events.length()) {
            val trial = JSONObject(result.toString())
            addEvent(trial, events.getJSONObject(i))
            if (trial.toString().length <= 30_000) result = trial
        }
        // Open commitments first; bounded recent facts keep long-running saves usable.
        for (category in listOf("promises", "magicItems", "keepsakes", "relationships")) {
            val all = memory.getJSONArray(category)
            val records = (0 until all.length()).map { all.getJSONObject(it) }.asReversed()
            val chosen = if (category == "promises") records.filter { it.getString("status") == "open" } + records.filter { it.getString("status") == "resolved" }.take(3)
                else records.take(12)
            for (record in chosen) {
                val trial = JSONObject(result.toString())
                val fact = JSONObject(record.toString()).put("descriptionRu", record.getString("descriptionRu").take(400))
                if (fact.has("propertyRu")) fact.put("propertyRu", fact.getString("propertyRu").take(400))
                addEvent(trial, byId.getValue(fact.getString("sourceEpisodeId")))
                trial.getJSONArray(category).put(fact)
                if (trial.toString().length <= 30_000) result = trial
            }
        }
        return validate(result)
    }
}
