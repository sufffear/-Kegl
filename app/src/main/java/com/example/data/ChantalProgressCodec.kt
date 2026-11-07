package com.example.data

import com.example.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Versioned explicit JSON encoding; no Java serialization or reflection. */
object ChantalProgressCodec {
    fun encode(value: ChantalTraits): JSONObject = JSONObject().apply {
        put("curiosity", value.curiosity)
        put("tenderness", value.tenderness)
        put("playfulness", value.playfulness)
        put("romanticism", value.romanticism)
        put("courage", value.courage)
        put("wit", value.wit)
    }

    fun decodeChantalTraits(value: JSONObject): ChantalTraits = ChantalTraits(
        curiosity = value.getInt("curiosity"),
        tenderness = value.getInt("tenderness"),
        playfulness = value.getInt("playfulness"),
        romanticism = value.getInt("romanticism"),
        courage = value.getInt("courage"),
        wit = value.getInt("wit")
    )

    fun encode(value: ChantalInventoryLootItem): JSONObject = JSONObject().apply {
        put("id", value.id)
        put("titleRu", value.titleRu)
        put("shortTitleRu", value.shortTitleRu)
        put("emoji", value.emoji)
        put("category", value.category.name)
        put("descriptionRu", value.descriptionRu)
        put("sellPriceCoins", value.sellPriceCoins)
        put("foundAtRegionRu", value.foundAtRegionRu)
        put("foundDateIso", value.foundDateIso)
        put("inspectedBonusClaimed", value.inspectedBonusClaimed)
    }

    fun decodeChantalInventoryLootItem(value: JSONObject): ChantalInventoryLootItem = ChantalInventoryLootItem(
        id = value.getString("id"),
        titleRu = value.getString("titleRu"),
        shortTitleRu = value.getString("shortTitleRu"),
        emoji = value.getString("emoji"),
        category = ChantalLootCategory.valueOf(value.getString("category")),
        descriptionRu = value.getString("descriptionRu"),
        sellPriceCoins = value.getInt("sellPriceCoins"),
        foundAtRegionRu = value.getString("foundAtRegionRu"),
        foundDateIso = value.getString("foundDateIso"),
        inspectedBonusClaimed = value.getBoolean("inspectedBonusClaimed")
    )

    fun encode(value: ChantalChoiceOption): JSONObject = JSONObject().apply {
        put("labelRu", value.labelRu)
        put("reactionRu", value.reactionRu)
        put("delta", encode(value.delta))
    }

    fun decodeChantalChoiceOption(value: JSONObject): ChantalChoiceOption = ChantalChoiceOption(
        labelRu = value.getString("labelRu"),
        reactionRu = value.getString("reactionRu"),
        delta = decodeChantalTraits(value.getJSONObject("delta"))
    )

    fun encode(value: ChantalStoryEpisode): JSONObject = JSONObject().apply {
        put("id", value.id)
        put("journeyNumber", value.journeyNumber)
        put("titleRu", value.titleRu)
        put("storyTextRu", value.storyTextRu)
        put("mapNoteRu", value.mapNoteRu)
        put("regionTitleRu", value.regionTitleRu)
        put("npcNameRu", value.npcNameRu)
        put("stayedInSameRegion", value.stayedInSameRegion)
        put("isFiller", value.isFiller)
        put("isMilestoneChest", value.isMilestoneChest)
        put("lootItem", encode(value.lootItem))
        put("coinsEarned", value.coinsEarned)
        put("causesExhaustion45", value.causesExhaustion45)
        put("debtAmountCoins", value.debtAmountCoins)
        put("badNewsRu", value.badNewsRu)
        put("choices", JSONArray(value.choices.map { encode(it) }))
        put("chosenActionSummaryRu", value.chosenActionSummaryRu ?: JSONObject.NULL)
        put("memorySummaryRu", value.memorySummaryRu ?: JSONObject.NULL)
    }

    fun decodeChantalStoryEpisode(value: JSONObject): ChantalStoryEpisode = ChantalStoryEpisode(
        id = value.getString("id"),
        journeyNumber = value.getInt("journeyNumber"),
        titleRu = value.getString("titleRu"),
        storyTextRu = value.getString("storyTextRu"),
        mapNoteRu = value.getString("mapNoteRu"),
        regionTitleRu = value.getString("regionTitleRu"),
        npcNameRu = value.getString("npcNameRu"),
        stayedInSameRegion = value.getBoolean("stayedInSameRegion"),
        isFiller = value.getBoolean("isFiller"),
        isMilestoneChest = value.getBoolean("isMilestoneChest"),
        lootItem = decodeChantalInventoryLootItem(value.getJSONObject("lootItem")),
        coinsEarned = value.getInt("coinsEarned"),
        causesExhaustion45 = value.getBoolean("causesExhaustion45"),
        debtAmountCoins = value.getInt("debtAmountCoins"),
        badNewsRu = value.getString("badNewsRu"),
        choices = value.getJSONArray("choices").let { a -> List(a.length()) { decodeChantalChoiceOption(a.getJSONObject(it)) } },
        chosenActionSummaryRu = if (value.isNull("chosenActionSummaryRu")) null else value.getString("chosenActionSummaryRu"),
        memorySummaryRu = if (value.isNull("memorySummaryRu")) null else value.getString("memorySummaryRu")
    )

    fun encode(value: ChantalRewardPopupData): JSONObject = JSONObject().apply {
        put("episodeTitleRu", value.episodeTitleRu)
        put("journeyNumber", value.journeyNumber)
        put("regionTitleRu", value.regionTitleRu)
        put("playerChoiceRu", value.playerChoiceRu)
        put("chantalReactionRu", value.chantalReactionRu)
        put("coinsEarned", value.coinsEarned)
        put("xpEarned", value.xpEarned)
        put("lootItem", encode(value.lootItem))
        put("traitArrowsText", value.traitArrowsText)
        put("isMilestoneChest", value.isMilestoneChest)
    }

    fun decodeChantalRewardPopupData(value: JSONObject): ChantalRewardPopupData = ChantalRewardPopupData(
        episodeTitleRu = value.getString("episodeTitleRu"),
        journeyNumber = value.getInt("journeyNumber"),
        regionTitleRu = value.getString("regionTitleRu"),
        playerChoiceRu = value.getString("playerChoiceRu"),
        chantalReactionRu = value.getString("chantalReactionRu"),
        coinsEarned = value.getInt("coinsEarned"),
        xpEarned = value.getInt("xpEarned"),
        lootItem = decodeChantalInventoryLootItem(value.getJSONObject("lootItem")),
        traitArrowsText = value.getString("traitArrowsText"),
        isMilestoneChest = value.getBoolean("isMilestoneChest")
    )

    fun strings(value: JSONArray?): List<String> = value?.let { a ->
        List(a.length()) { a.getString(it) }
    } ?: emptyList()

    fun counts(value: JSONObject?): Map<String, Int> = value?.let { obj ->
        obj.keys().asSequence().associateWith { obj.getInt(it) }
    } ?: emptyMap()
}

