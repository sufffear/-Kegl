package com.example.data

import com.example.domain.ChantalStoryEpisode
import org.json.*

/** Model proposes narrative facts only. Choice, IDs and application belong to the app. */
object ChantalGeneratedConsequences {
    fun validate(proposals: JSONArray, memory: JSONObject, checkAvailability: Boolean = true) {
        require(proposals.length() <= 3)
        val used = mutableSetOf<Int>()
        val promises = memory.getJSONArray("promises")
        val open = (0 until promises.length()).map { promises.getJSONObject(it) }.filter { it.getString("status") == "open" }
        repeat(proposals.length()) { i ->
            val p = proposals.getJSONObject(i)
            require(p.keys().asSequence().toSet() == setOf("choiceIndex", "kind", "descriptionRu", "targetId"))
            val index = p.get("choiceIndex")
            require(index is Int && index in 0..2 && used.add(index))
            val text = p.get("descriptionRu")
            require(text is String && text.isNotBlank() && text.length <= 350)
            val target = p.get("targetId")
            require(target is String)
            when(p.getString("kind")) {
                "relationship" -> require(target.isEmpty())
                "promise" -> require(target.isEmpty() && (!checkAvailability || open.size < 3))
                "resolve" -> require(target.isNotBlank() && (!checkAvailability || open.any { it.getString("id") == target }))
                else -> error("Unknown narrative consequence")
            }
        }
    }
    fun apply(initial: JSONObject, journal: List<ChantalStoryEpisode>): JSONObject {
        var memory = initial
        for (episode in journal.asReversed()) {
            val index = episode.chosenOptionIndex ?: continue
            if (episode.chosenActionSummaryRu.isNullOrBlank()) continue
            val raw = episode.narrativeConsequences ?: continue
            val proposals = JSONArray(raw)
            repeat(proposals.length()) { i ->
                val p = proposals.getJSONObject(i)
                if (p.getInt("choiceIndex") == index) {
                    val id = "generated:${episode.id}:$index"
                    val kind = p.getString("kind")
                    val category = if (kind == "relationship") "relationships" else "promises"
                    val records = memory.getJSONArray(category)
                    if (kind == "resolve") {
                        val old = (0 until records.length()).map { records.getJSONObject(it) }.firstOrNull { it.getString("id") == p.getString("targetId") }
                        if (old != null && old.getString("status") == "open") memory = ChantalWorldMemory.withFact(memory, category,
                            JSONObject(old.toString()).put("status", "resolved"))
                    } else if ((0 until records.length()).none { records.getJSONObject(it).getString("id") == id }) {
                        val count = (0 until records.length()).count { records.getJSONObject(it).optString("status") == "open" }
                        if (kind != "promise" || count < 3) {
                            val fact = JSONObject().put("id", id).put("sourceEpisodeId", episode.id).put("descriptionRu", p.getString("descriptionRu"))
                            if (kind == "promise") fact.put("status", "open")
                            memory = ChantalWorldMemory.withFact(memory, category, fact)
                        }
                    }
                }
            }
        }
        return memory
    }
}
