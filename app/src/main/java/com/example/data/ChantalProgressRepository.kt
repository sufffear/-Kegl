package com.example.data

import android.content.Context
import org.json.JSONObject

/**
 * One whole snapshot per edit: coins, inventory and accepted story cannot be
 * persisted independently. apply updates in-process memory immediately and lets
 * Android finish pending disk writes when activities stop.
 */
class ChantalProgressRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE)
    private var lastSaved: String? = null

    fun load(): JSONObject {
        val raw = preferences.getString("snapshot", null) ?: return JSONObject()
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
        lastSaved = raw
        return snapshot
    }

    fun save(snapshot: JSONObject) {
        snapshot.put("schemaVersion", 1)
        val raw = snapshot.toString()
        if (raw == lastSaved) return
        preferences.edit().putString("snapshot", raw).apply()
        lastSaved = raw
    }
}

