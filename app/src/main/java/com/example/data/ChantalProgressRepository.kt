package com.example.data

import android.content.Context
import org.json.JSONObject

/**
 * One whole snapshot per edit: coins, inventory and accepted story cannot be
 * persisted independently. apply updates in-process memory immediately and lets
 * Android finish pending disk writes when activities stop.
 */
class ChantalProgressRepository(private val context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE)
    private var lastSaved: String? = null

    fun load(): JSONObject {
        val raw = preferences.getString("snapshot", null) ?: return JSONObject()
        val snapshot = validate(raw)
        snapshot.put("worldMemory", memoryFor(snapshot, snapshot.optJSONObject("worldMemory")))
        lastSaved = raw
        return snapshot
    }

    /** Synchronous replacement for backup restore; caller runs off the UI thread. */
    fun replace(raw: String?) {
        if (raw != null) validate(raw)
        val edit = preferences.edit()
        if (raw == null) edit.remove("snapshot") else edit.putString("snapshot", raw)
        check(edit.commit()) { "Не удалось сохранить прогресс Шанталь" }
        lastSaved = raw
        com.example.service.ChantalReturnScheduler.sync(context, raw?.let { JSONObject(it).optLong("activeJourneyStartMs", 0) } ?: 0)
    }

    fun export(): String = load().put("schemaVersion", 1).toString()

    companion object {
        fun validate(raw: String): JSONObject {
            val snapshot = JSONObject(raw)
            require(snapshot.getInt("schemaVersion") == 1) { "Unsupported Chantal save version" }
            // Validate complex values before the screen can overwrite an unreadable save.
            snapshot.optJSONArray("inventoryList")?.let { a ->
                repeat(a.length()) { ChantalProgressCodec.decodeChantalInventoryLootItem(a.getJSONObject(it)) }
            }
            snapshot.optJSONArray("journalList")?.let { a ->
                repeat(a.length()) { ChantalProgressCodec.decodeChantalStoryEpisode(a.getJSONObject(it)) }
            }
            snapshot.optJSONObject("pendingEpisode")?.let { ChantalProgressCodec.decodeChantalStoryEpisode(it) }
            snapshot.optJSONObject("rewardPopupData")?.let { ChantalProgressCodec.decodeChantalRewardPopupData(it) }
            snapshot.optJSONObject("worldInteractions")?.let { ChantalWorldInteractions.validate(it) }
            if (snapshot.has("worldMemory")) ChantalWorldMemory.validate(snapshot.getJSONObject("worldMemory"))
            return snapshot
        }
    }

    private fun memoryFor(snapshot: JSONObject, existing: JSONObject?): JSONObject {
        val journal = snapshot.optJSONArray("journalList")
        val memory = ChantalWorldMemory.reconcile(existing, journal)
        val episodes = if (journal == null) emptyList() else (0 until journal.length()).map {
            ChantalProgressCodec.decodeChantalStoryEpisode(journal.getJSONObject(it))
        }
        var confirmed = ChantalLanternAdventure.consequences(memory, episodes)
        snapshot.optJSONObject("worldInteractions")?.let { interactions ->
            val gifts = ChantalWorldInteractions.validate(interactions).getJSONArray("gifts")
            repeat(gifts.length()) { index ->
                val gift = gifts.getJSONObject(index)
                val source = gift.getString("sourceEpisodeId")
                if (episodes.any { it.id == source && !it.chosenActionSummaryRu.isNullOrBlank() }) {
                    confirmed = ChantalWorldMemory.withFact(confirmed, "relationships", JSONObject()
                        .put("id", "gift:" + gift.getString("id"))
                        .put("sourceEpisodeId", source).put("descriptionRu", gift.getString("textRu")))
                }
            }
        }
        return confirmed
    }

    fun save(snapshot: JSONObject) {
        snapshot.put("schemaVersion", 1)
        val existing = snapshot.optJSONObject("worldMemory")
            ?: preferences.getString("snapshot", null)?.let { validate(it).optJSONObject("worldMemory") }
        snapshot.put("worldMemory", memoryFor(snapshot, existing))
        val raw = snapshot.toString()
        if (raw == lastSaved) return
        preferences.edit().putString("snapshot", raw).apply()
        com.example.service.ChantalReturnScheduler.sync(context, snapshot.optLong("activeJourneyStartMs", 0))
        lastSaved = raw
    }
}
