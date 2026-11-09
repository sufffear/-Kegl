package com.example.data

import com.example.domain.ChantalStoryEngine as Engine
import org.json.JSONArray
import org.json.JSONObject

/** Persist commands, replay authored rules. Never trust imported plots, facts or model-written outcomes. */
object ChantalStoryEngineCodec {
    const val VERSION = 2
    private const val MAX_RUNS = 10000
    fun encode(state: Engine.State): JSONObject = JSONObject().put("version",VERSION)
        .put("catalogueSignature",Engine.catalogueSignature).put("seed",state.seed).put("initialAdventures",state.initialAdventures)
        .put("externalAdvancements",JSONArray(state.externalHistory.map { JSONObject().put("episodeId",it.episodeId).put("afterRuns",it.afterRuns) }))
        .put("runs",JSONArray().also { a -> state.history.forEach { run ->
            a.put(JSONObject().put("journeyId",run.plan.request.journeyId).put("locationId",run.plan.request.locationId)
                .put("resumeSuspended",run.plan.request.resumeSuspended).put("temperament",run.plan.request.temperament.name)
                .put("legacyPlotReserved",run.plan.request.legacyPlotReserved).put("choiceIndex",run.choiceIndex))
        } })
    fun decode(raw: JSONObject): Engine.State {
        val keys=setOf("version","catalogueSignature","seed","initialAdventures","runs")
        require(raw.keys().asSequence().toSet()==if(raw.getInt("version")==1) keys else keys+"externalAdvancements")
        require(raw.getString("catalogueSignature")==Engine.catalogueSignature) { "Authored catalogue changed; explicit migration required" }
        require(raw.get("version") is Int && raw.getInt("version") in setOf(1,VERSION))
        require(raw.get("seed") is Int || raw.get("seed") is Long)
        require(raw.get("initialAdventures") is Int)
        var state=Engine.newGame(raw.getLong("seed"),raw.getInt("initialAdventures"))
        val runs=raw.getJSONArray("runs")
        require(runs.length()<=MAX_RUNS) { "Archive exceeds supported engine history; do not truncate" }
        val external=raw.optJSONArray("externalAdvancements") ?: JSONArray()
        require(external.length()<=MAX_RUNS && runs.length()+external.length()<=MAX_RUNS)
        val receipts=(0 until external.length()).map {
            val record=external.getJSONObject(it)
            require(record.keys().asSequence().toSet()==setOf("episodeId","afterRuns"))
            require(record.get("afterRuns") is Int && record.getInt("afterRuns") in 0..runs.length())
            Engine.ExternalAdvancement(record.getString("episodeId"),record.getInt("afterRuns"))
        }
        require(receipts.map { it.episodeId }.distinct().size==receipts.size)
        require(receipts.zipWithNext().all { (a,b)->a.afterRuns<=b.afterRuns })
        fun advanceAt(index: Int) { receipts.filter { it.afterRuns==index }.forEach { state=Engine.advanceExternal(state,it.episodeId) } }
        repeat(runs.length()) { index ->
            advanceAt(index)
            val run=runs.getJSONObject(index)
            require(run.keys().asSequence().toSet()==setOf("journeyId","locationId","resumeSuspended","temperament","legacyPlotReserved","choiceIndex"))
            require(run.get("resumeSuspended") is Boolean && run.get("legacyPlotReserved") is Boolean && run.get("choiceIndex") is Int)
            val request=Engine.Request(run.getString("journeyId"),run.getString("locationId"),run.getBoolean("resumeSuspended"),Engine.Temperament.valueOf(run.getString("temperament")),run.getBoolean("legacyPlotReserved"))
            state=Engine.accept(state,Engine.plan(state,request),run.getInt("choiceIndex"))
        }
        advanceAt(runs.length())
        return state
    }
}
