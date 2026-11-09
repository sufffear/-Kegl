package com.example.data

import com.example.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Pure whole-snapshot reduction. The repository must durably commit this result before UI effects. */
object ChantalJourneyConfirmation {
    data class Result(val snapshot: JSONObject, val applied: Boolean, val decisionId: String,
        val episode: ChantalStoryEpisode?, val xpEarned: Int)

    fun apply(snapshot: JSONObject, episodeId: String, choiceIndex: Int? = null, fillerText: String? = null): Result {
        require(episodeId.isNotBlank())
        require((choiceIndex != null) xor (fillerText != null)) { "Exactly one decision is required" }
        val next = JSONObject(snapshot.toString())
        val journal = next.optJSONArray("journalList") ?: JSONArray()
        val decisionId = "journey-confirmation:$episodeId"
        if ((0 until journal.length()).any { journal.getJSONObject(it).getString("id") == episodeId })
            return Result(next, false, decisionId, null, 0)
        val episode = next.optJSONObject("pendingEpisode")?.let(ChantalProgressCodec::decodeChantalStoryEpisode)
        require(episode != null && episode.id == episodeId) { "Pending episode changed" }
        val option = if (choiceIndex != null) episode.choices.getOrNull(choiceIndex) else null
        if (choiceIndex != null) require(option != null) { "Unknown choice" }
        else require(episode.isFiller) { "Free text requires a personal errand" }
        val xp = if (fillerText != null) 8 else if (episode.isMilestoneChest) 15 else if (episode.isFiller) 8 else 10
        val arrows = option?.delta?.formatArrowIndicators()?.joinToString(" · ").orEmpty()
        val accepted = if (option != null) episode.copy(
            chosenActionSummaryRu = "${option.labelRu} — Реакция Шанталь: ${option.reactionRu}", chosenOptionIndex = choiceIndex)
        else episode.copy(isFiller = true, chosenActionSummaryRu = "Свой текст: $fillerText")
        fun prepend(value: JSONObject, rest: JSONArray) = JSONArray().put(value).also { a ->
            repeat(rest.length()) { a.put(rest.get(it)) }
        }
        next.put("inventoryList", prepend(ChantalProgressCodec.encode(episode.lootItem), next.optJSONArray("inventoryList") ?: JSONArray()))
        val newIds = next.optJSONArray("newlyAddedBagItemIds") ?: JSONArray()
        if ((0 until newIds.length()).none { newIds.getString(it) == episode.lootItem.id }) newIds.put(episode.lootItem.id)
        next.put("newlyAddedBagItemIds", newIds).put("selectedBagItemId", episode.lootItem.id)
        next.put("coins", Math.addExact(next.optInt("coins"), episode.coinsEarned))
        next.put("maturityXp", Math.addExact(next.optInt("maturityXp"), xp))
        if (option != null) {
            if (episode.causesExhaustion45) next.put("isExhausted45", true)
            if (episode.debtAmountCoins > 0) next.put("debtCoins", Math.addExact(next.optInt("debtCoins"), episode.debtAmountCoins)).put("debtReasonRu", episode.badNewsRu)
            if (!episode.isFiller) {
                val defaults = ChantalProgressCodec.encode(ChantalTraits())
                defaults.keys().forEach { key -> if (next.has(key)) defaults.put(key, next.getInt(key)) }
                val updated = ChantalProgressCodec.encode(ChantalProgressCodec.decodeChantalTraits(defaults).applyCanonicalDelta(option.delta))
                updated.keys().forEach { key -> next.put(key, updated.getInt(key)) }
                next.put("totalJourneys", Math.addExact(next.optInt("totalJourneys"), 1))
            }
        } else {
            if (next.optInt("debtCoins") > 0) next.put("debtCoins", 0).put("debtReasonRu", "")
            next.put("fillerTextInput", "")
        }
        val popup = ChantalRewardPopupData(episodeTitleRu = episode.titleRu, journeyNumber = episode.journeyNumber,
            regionTitleRu = episode.regionTitleRu, playerChoiceRu = option?.labelRu ?: fillerText.orEmpty(),
            chantalReactionRu = option?.reactionRu ?: "Шанталь внимательно выслушала твоё поручение, радостно тявкнула и выложила на стол принесённые монеты и находку!",
            coinsEarned = episode.coinsEarned, xpEarned = xp, lootItem = episode.lootItem,
            traitArrowsText = arrows, isMilestoneChest = episode.isMilestoneChest)
        next.put("journalList", prepend(ChantalProgressCodec.encode(accepted), journal))
            .put("rewardPopupData", ChantalProgressCodec.encode(popup)).put("pendingEpisode", JSONObject.NULL)
        return Result(next, true, decisionId, accepted, xp)
    }
}
