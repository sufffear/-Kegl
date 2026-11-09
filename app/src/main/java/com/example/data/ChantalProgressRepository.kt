package com.example.data

import android.content.Context
import org.json.JSONObject

/**
 * One whole snapshot per edit: coins, inventory and accepted story cannot be
 * persisted independently. Room commits the aggregate before returning success.
 */
class ChantalProgressRepository(private val context: Context, private val database: VelarisDatabase = VelarisDatabase.getInstance(context)) {
    private val dao = database.chantalDao()
    private var expectedRevision: Long? = null
    private fun <T> io(block: () -> T): T = java.util.concurrent.FutureTask(block).also { Thread(it, "chantal-store").start() }.get()
    private fun stored(): String? = io {
        database.runInTransaction(java.util.concurrent.Callable {
            val row = dao.aggregate()
            if (row != null) { expectedRevision = row.revision; row.payload } else {
                val legacy = preferences.getString("snapshot", null)?.let { normalize(it) }
                if (legacy != null) validate(legacy)
                dao.putAggregate(ChantalAggregateEntity(revision=0, payload=legacy))
                expectedRevision = 0
                rebuildIndexes(legacy)
                legacy
            }
        })
    }
    private val preferences = context.applicationContext.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE)
    private var lastSaved: String? = null
    private var lastSubmitted: String? = null

    fun load(): JSONObject {
        lastSubmitted = null
        val raw = stored() ?: return JSONObject()
        val snapshot = validate(raw)
        snapshot.put("worldMemory", memoryFor(snapshot, snapshot.optJSONObject("worldMemory")))
        lastSaved = raw
        return snapshot
    }

    /** Synchronous replacement for backup restore; caller runs off the UI thread. */
    fun replace(source: String?) {
        val raw = source?.let { data ->
            val copy=JSONObject(normalize(data))
            copy.optJSONObject("narrativeBinding")?.let { it.remove("activeRequest");it.put("status","INTERRUPTED") }
            copy.toString()
        }
        if (raw != null) validate(raw)
        io { database.runInTransaction {
            val revision = (dao.aggregate()?.revision ?: 0) + 1
            dao.clearReceipts(); dao.clearEvents()
            dao.putAggregate(ChantalAggregateEntity(revision=revision,payload=raw))
            rebuildIndexes(raw)
            expectedRevision = revision
        } }
        lastSaved = raw
        lastSubmitted = null
        com.example.service.ChantalReturnScheduler.sync(context, raw?.let { JSONObject(it).optLong("activeJourneyStartMs", 0) } ?: 0)
    }

    suspend fun replaceWithinRestore(source: String?) {
        val raw = source?.let { data ->
            val copy=JSONObject(normalize(data))
            copy.optJSONObject("narrativeBinding")?.let { it.remove("activeRequest");it.put("status","INTERRUPTED") }
            copy.toString()
        }
        if (raw != null) validate(raw)
        val revision = (dao.aggregateAsync()?.revision ?: 0) + 1
        dao.clearReceiptsAsync(); dao.clearEventsAsync()
        dao.putAggregateAsync(ChantalAggregateEntity(revision=revision,payload=raw))
        raw?.let { data ->
            val snapshot = validate(data)
            val journal = snapshot.optJSONArray("journalList") ?: org.json.JSONArray()
            repeat(journal.length()) { index ->
                val episode = journal.getJSONObject(index)
                if (!episode.optString("chosenActionSummaryRu").isNullOrBlank()) {
                    val id = episode.getString("id")
                    dao.insertReceiptAsync(ChantalConfirmationEntity(id,"journey-confirmation:$id"))
                }
            }
            val events = snapshot.getJSONArray("storyEvents")
            repeat(events.length()) { val e = events.getJSONObject(it); dao.insertEventAsync(ChantalJournalEventEntity(e.getString("eventId"),e.toString())) }
        }
        lastSaved = null
        lastSubmitted = null
        expectedRevision = revision
    }

    /** Durable confirmation: receipt, accepted episode and rewards share one SQLite transaction. */
    fun confirmJourney(episodeId: String, choiceIndex: Int? = null, fillerText: String? = null): ChantalJourneyConfirmation.Result = io {
        database.runInTransaction(java.util.concurrent.Callable {
            val row = requireNotNull(dao.aggregate()) { "Progress must be loaded first" }
            require(expectedRevision == null || expectedRevision == row.revision) { "Progress changed; reload before confirming" }
            val current = row.payload?.let(::validate) ?: error("No pending progress")
            val result = ChantalJourneyConfirmation.apply(current, episodeId, choiceIndex, fillerText)
            if (dao.receipt(episodeId) != null || !result.applied) return@Callable result.copy(applied=false)
            current.optJSONObject("narrativeBinding")?.let { binding ->
                val description=ChantalNarrativeSession.describe(current,binding)
                result.snapshot.put("storyEmotion",(description.plan?.emotion ?: com.example.domain.ChantalStoryEmotion.CALM).name)
                if(description.tracked) {
                    val next=com.example.domain.ChantalStoryEngine.accept(requireNotNull(description.engine),requireNotNull(description.plan),requireNotNull(choiceIndex))
                    result.snapshot.put("storyEngineState",ChantalStoryEngineCodec.encode(next))
                }
            }
            // Legacy chapters and chests still count toward age after the new engine is active.
            if(current.optJSONObject("narrativeBinding")?.let { ChantalNarrativeSession.describe(current,it).tracked } != true
                && current.has("storyEngineState") && result.snapshot.optInt("totalJourneys")>current.optInt("totalJourneys")) {
                result.snapshot.put("storyEngineState",ChantalStoryEngineCodec.encode(com.example.domain.ChantalStoryEngine.advanceExternal(
                    ChantalStoryEngineCodec.decode(current.getJSONObject("storyEngineState")),episodeId)))
            }
            result.snapshot.remove("narrativeBinding")
            result.snapshot.put("worldMemory", memoryFor(result.snapshot,current.optJSONObject("worldMemory")))
            val events = result.snapshot.optJSONArray("storyEvents") ?: org.json.JSONArray()
            val ledgerEvent = JSONObject().put("kind","PLAYER_DECISION").put("eventId","decision:$episodeId")
                .put("episodeId",episodeId).put("decisionId",result.decisionId)
                .put("choiceIndex",choiceIndex ?: JSONObject.NULL).put("fillerText",fillerText ?: JSONObject.NULL)
            val source=current.optJSONObject("narrativeBinding")?.optString("source") ?: "legacy_local"
            ledgerEvent.put("narrativeSource",source)
            if(source=="openrouter") ledgerEvent.put("requestedModel",OpenRouterStoryTransport.DEFAULT_MODEL)
            events.put(ledgerEvent)
            result.snapshot.put("storyStorageVersion",1).put("storyEvents",events)
            val raw = result.snapshot.toString()
            validate(raw)
            dao.insertReceipt(ChantalConfirmationEntity(episodeId,result.decisionId))
            dao.insertEvent(ChantalJournalEventEntity("decision:$episodeId",ledgerEvent.toString()))
            dao.putAggregate(ChantalAggregateEntity(revision=row.revision+1,payload=raw))
            expectedRevision = row.revision+1
            lastSubmitted = null
            lastSaved = raw
            result
        })
    }

    data class NarrativeTicket(val snapshot: JSONObject, val episodeId: String, val hash: String, val token: String)

    /** Publish a deterministic fallback before any paid request; original rewards remain fixed. */
    fun prepareNarrative(resume: Boolean = false): JSONObject = io {
        database.runInTransaction(java.util.concurrent.Callable {
            val row=requireNotNull(dao.aggregate())
            require(expectedRevision==null || expectedRevision==row.revision)
            val snapshot=validate(requireNotNull(row.payload))
            val ep=snapshot.optJSONObject("pendingEpisode")?.let(ChantalProgressCodec::decodeChantalStoryEpisode)
            if(ep==null || ep.isFiller) return@Callable snapshot
            val existing=snapshot.optJSONObject("narrativeBinding")
            if(existing!=null && (!resume || existing.getBoolean("resume"))) return@Callable snapshot
            val base=JSONObject(snapshot.toString())
            if(existing!=null) base.put("pendingEpisode",existing.getJSONObject("original"))
            val binding=ChantalNarrativeSession.binding(base,existing?.getLong("seed") ?: java.security.SecureRandom().nextLong(),resume)
            val description=ChantalNarrativeSession.describe(snapshot,binding)
            snapshot.put("pendingEpisode",ChantalProgressCodec.encode(description.local))
            binding.put("currentHash",ChantalNarrativeSession.fingerprint(description.local)).put("source","local").put("status","READY")
            snapshot.put("narrativeBinding",binding)
            writeNarrative(row,snapshot)
            snapshot
        })
    }
    fun beginNarrativeRequest(episodeId: String): NarrativeTicket = io {
        database.runInTransaction(java.util.concurrent.Callable {
            val row=requireNotNull(dao.aggregate())
            require(expectedRevision == row.revision) { "Progress changed; reload before requesting a story" }
            val snapshot=validate(requireNotNull(row.payload))
            val ep=ChantalProgressCodec.decodeChantalStoryEpisode(snapshot.getJSONObject("pendingEpisode"))
            require(ep.id==episodeId)
            val binding=snapshot.getJSONObject("narrativeBinding")
            val token=java.util.UUID.randomUUID().toString()
            binding.put("activeRequest",token).put("status","IN_FLIGHT")
            writeNarrative(row,snapshot)
            NarrativeTicket(snapshot,episodeId,binding.getString("currentHash"),token)
        })
    }
    fun publishNarrative(ticket: NarrativeTicket, response: String, provider: String): JSONObject = io {
        require(provider in setOf("openrouter","gemini"))
        database.runInTransaction(java.util.concurrent.Callable {
            val row=requireNotNull(dao.aggregate());val snapshot=validate(requireNotNull(row.payload))
            val binding=snapshot.getJSONObject("narrativeBinding")
            require(binding.optString("activeRequest")==ticket.token && binding.getString("currentHash")==ticket.hash)
            val current=ChantalProgressCodec.decodeChantalStoryEpisode(snapshot.getJSONObject("pendingEpisode"))
            require(current.id==ticket.episodeId)
            val description=ChantalNarrativeSession.describe(snapshot,binding)
            val text=ChantalNarrativeSession.parse(response,snapshot,description)
            val episode=ChantalNarrativeSession.rendered(description,text)
            snapshot.put("pendingEpisode",ChantalProgressCodec.encode(episode))
            binding.remove("activeRequest")
            binding.put("status","READY").put("source",provider).put("currentHash",ChantalNarrativeSession.fingerprint(episode))
            writeNarrative(row,snapshot)
            snapshot
        })
    }
    fun finishNarrativeRequest(ticket: NarrativeTicket, cancelled: Boolean) = io {
        database.runInTransaction {
            val row=dao.aggregate() ?: return@runInTransaction
            val snapshot=row.payload?.let(::validate) ?: return@runInTransaction
            val binding=snapshot.optJSONObject("narrativeBinding") ?: return@runInTransaction
            if(binding.optString("activeRequest")!=ticket.token) return@runInTransaction
            binding.remove("activeRequest");binding.put("status",if(cancelled) "INTERRUPTED" else "FAILED")
            writeNarrative(row,snapshot)
        }
    }
    /** Refresh after cancellation/publication without rebasing a stale screen over an external restore. */
    fun ownNarrativeSnapshot(): JSONObject = io {
        val row=requireNotNull(dao.aggregate())
        require(expectedRevision==row.revision) { "Progress changed outside this screen" }
        validate(requireNotNull(row.payload))
    }

    private fun writeNarrative(row: ChantalAggregateEntity, snapshot: JSONObject) {
        val raw=snapshot.toString();validate(raw)
        dao.putAggregate(ChantalAggregateEntity(revision=row.revision+1,payload=raw))
        expectedRevision=row.revision+1;lastSubmitted=null;lastSaved=raw
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
            if (snapshot.has("worldInteractions")) ChantalWorldInteractions.validate(snapshot.getJSONObject("worldInteractions"))
            snapshot.optJSONObject("friendSceneAnswers")?.let { answers ->
                answers.keys().forEach { key ->
                    require(ChantalLivingWorld.profiles.any { it.key == key })
                    require(answers.get(key) is Int && answers.getInt(key) in 0..2)
                }
            }
            if (snapshot.has("shelfSelection")) {
                val shelf = snapshot.getJSONArray("shelfSelection")
                require(shelf.length() <= ChantalDenShelf.CAPACITY)
                val ids = (0 until shelf.length()).map { index ->
                    val id = shelf.get(index)
                    require(id is String && id.isNotBlank())
                    id
                }
                require(ids.distinct().size == ids.size)
            }
            if (snapshot.has("worldMemory")) ChantalWorldMemory.validate(snapshot.getJSONObject("worldMemory"))
            if(snapshot.has("homePresence")) ChantalHomePresence.decode(snapshot.getJSONObject("homePresence"))
            if(snapshot.has("storyEmotion")) com.example.domain.ChantalStoryEmotion.valueOf(snapshot.getString("storyEmotion"))
            if (snapshot.has("storyEngineState")) ChantalStoryEngineCodec.decode(snapshot.getJSONObject("storyEngineState"))
            if (snapshot.has("narrativeBinding")) ChantalNarrativeSession.validateBinding(snapshot)
            if (snapshot.has("storyStorageVersion")) require(snapshot.getInt("storyStorageVersion")==1)
            snapshot.optJSONArray("storyEvents")?.let { events ->
                val ids = mutableSetOf<String>()
                repeat(events.length()) { i ->
                    val e = events.getJSONObject(i)
                    require(e.getString("eventId").isNotBlank() && ids.add(e.getString("eventId")))
                    require(e.getString("kind") in setOf("LEGACY_EPISODE","PLAYER_DECISION"))
                    val episodeId = e.getString("episodeId")
                    val journal = snapshot.optJSONArray("journalList") ?: org.json.JSONArray()
                    require((0 until journal.length()).any { j -> journal.getJSONObject(j).getString("id")==episodeId && !journal.getJSONObject(j).optString("chosenActionSummaryRu").isNullOrBlank() })
                    if (e.getString("kind")=="PLAYER_DECISION") require(e.getString("decisionId")=="journey-confirmation:$episodeId")
                }
            }
            snapshot.optJSONArray("storyFeedback")?.let { feedback ->
                val ids = mutableSetOf<String>()
                repeat(feedback.length()) { index ->
                    val f = feedback.getJSONObject(index)
                    val episodeId = f.getString("episodeId")
                    require(ids.add(episodeId))
                    val journal = snapshot.optJSONArray("journalList") ?: org.json.JSONArray()
                    require((0 until journal.length()).any { j -> journal.getJSONObject(j).getString("id")==episodeId && !journal.getJSONObject(j).optString("chosenActionSummaryRu").isNullOrBlank() })
                    com.example.domain.ChantalStoryRating.valueOf(f.getString("rating"))
                    com.example.domain.ChantalFeedbackScope.valueOf(f.getString("scope"))
                    require(f.optString("comment").length <= 1000)
                    val reasons = f.getJSONArray("reasons")
                    repeat(reasons.length()) { com.example.domain.ChantalFeedbackReason.valueOf(reasons.getString(it)) }
                }
            }
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
            interactions.optJSONObject("silverThreadPractice")?.let { practice ->
                val source = practice.getString("sourceEpisodeId")
                if (episodes.any { it.id == source && !it.chosenActionSummaryRu.isNullOrBlank() }) confirmed = ChantalWorldMemory.withFact(confirmed, "relationships",
                    JSONObject().put("id", "silver_thread_practice").put("sourceEpisodeId", source).put("descriptionRu", practice.getString("textRu")))
            }
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
        snapshot.optJSONObject("friendSceneAnswers")?.let { answers ->
            ChantalLivingWorld.profiles.forEach { p ->
                val meetings = episodes.filter { !it.chosenActionSummaryRu.isNullOrBlank() && ChantalLivingWorld.profile(it.npcNameRu)?.key == p.key }.distinctBy { it.id }
                if (answers.has(p.key) && meetings.size >= 3) {
                    val reply = p.replies[answers.getInt(p.key)]
                    confirmed = ChantalWorldMemory.withFact(confirmed, "relationships", JSONObject()
                        .put("id", "friend-scene:${p.key}").put("sourceEpisodeId", meetings.first().id)
                        .put("descriptionRu", "Совместная встреча с ${p.name}. Ответ игрока: ${reply.first}. Шанталь рассказала: ${reply.second}"))
                }
            }
        }
        return ChantalGeneratedConsequences.apply(confirmed, episodes)
    }

    private fun normalize(raw: String): String {
        val snapshot = validate(raw)
        if (!snapshot.has("storyEvents")) {
            val events = org.json.JSONArray()
            val journal = snapshot.optJSONArray("journalList") ?: org.json.JSONArray()
            for (index in journal.length()-1 downTo 0) {
                val episode = journal.getJSONObject(index)
                if (!episode.optString("chosenActionSummaryRu").isNullOrBlank()) events.put(JSONObject()
                    .put("eventId","legacy:"+episode.getString("id")).put("episodeId",episode.getString("id"))
                    .put("kind","LEGACY_EPISODE"))
            }
            snapshot.put("storyStorageVersion",1).put("storyEvents",events)
        }
        val ledger = snapshot.getJSONArray("storyEvents")
        val represented = (0 until ledger.length()).map { ledger.getJSONObject(it).getString("episodeId") }.toSet()
        val accepted = snapshot.optJSONArray("journalList") ?: org.json.JSONArray()
        for (index in accepted.length()-1 downTo 0) {
            val ep = accepted.getJSONObject(index)
            if (!ep.optString("chosenActionSummaryRu").isNullOrBlank() && ep.getString("id") !in represented) {
                ledger.put(JSONObject().put("kind","LEGACY_EPISODE").put("eventId","legacy:"+ep.getString("id")).put("episodeId",ep.getString("id")))
            }
        }
        validate(snapshot.toString())
        return snapshot.toString()
    }

    private fun rebuildIndexes(raw: String?) {
        val snapshot = raw?.let(::validate) ?: return
        val journal = snapshot.optJSONArray("journalList") ?: org.json.JSONArray()
        repeat(journal.length()) { index ->
            val episode = journal.getJSONObject(index)
            if (!episode.optString("chosenActionSummaryRu").isNullOrBlank()) {
                val id = episode.getString("id")
                if (dao.receipt(id)==null) dao.insertReceipt(ChantalConfirmationEntity(id,"journey-confirmation:$id"))
            }
        }
        val events = snapshot.getJSONArray("storyEvents")
        repeat(events.length()) {
            val e = events.getJSONObject(it)
            val old = dao.event(e.getString("eventId"))
            require(old == null || old.payload == e.toString()) { "Event history is immutable" }
            if (old == null) dao.insertEvent(ChantalJournalEventEntity(e.getString("eventId"),e.toString()))
        }
    }

    fun saveFeedback(feedback: com.example.domain.ChantalStoryFeedback?) {
        val snapshot = load()
        val entries = snapshot.optJSONArray("storyFeedback") ?: org.json.JSONArray()
        val next = org.json.JSONArray()
        repeat(entries.length()) { if (feedback != null && entries.getJSONObject(it).getString("episodeId") != feedback.episodeId) next.put(entries.get(it)) }
        if (feedback != null) next.put(JSONObject().put("episodeId",feedback.episodeId).put("rating",feedback.rating.name)
            .put("scope",feedback.scope.name).put("reasons",org.json.JSONArray(feedback.reasons.map { it.name })).put("comment",feedback.comment))
        snapshot.put("storyFeedback",next)
        save(snapshot)
    }

    fun save(snapshot: JSONObject) {
        if (expectedRevision == null) stored()
        val incoming = JSONObject(snapshot.toString()).put("schemaVersion",1)
        val submitted = incoming.toString()
        io { database.runInTransaction {

            val row = dao.aggregate()
            require(expectedRevision == null || expectedRevision == row?.revision) { "Progress changed; reload before saving" }
            if (submitted == lastSubmitted) return@runInTransaction
            val previous = row?.payload?.let(::validate)
            if (previous?.has("storyEngineState")==true && incoming.has("storyEngineState")) require(
                previous.getJSONObject("storyEngineState").toString()==incoming.getJSONObject("storyEngineState").toString()
            ) { "Engine timeline may change only during confirmed journey transactions" }
            // UI does not own editorial feedback or future story fields.
            val merged = previous?.let { JSONObject(it.toString()) } ?: JSONObject()
            incoming.keys().forEach { key -> merged.put(key,incoming.get(key)) }
            merged.put("worldMemory",memoryFor(merged,merged.optJSONObject("worldMemory")))
            val priorEvents = previous?.optJSONArray("storyEvents")
            if (priorEvents != null) merged.put("storyEvents",priorEvents).put("storyStorageVersion",1)
            val priorJournal = previous?.optJSONArray("journalList")
            val newJournal = merged.optJSONArray("journalList") ?: org.json.JSONArray()
            if (priorJournal != null) repeat(priorJournal.length()) { i ->
                val ep = priorJournal.getJSONObject(i)
                if (!ep.optString("chosenActionSummaryRu").isNullOrBlank()) require((0 until newJournal.length()).any {
                    newJournal.getJSONObject(it).getString("id")==ep.getString("id") && ChantalProgressCodec.encode(ChantalProgressCodec.decodeChantalStoryEpisode(newJournal.getJSONObject(it))).toString()==ChantalProgressCodec.encode(ChantalProgressCodec.decodeChantalStoryEpisode(ep)).toString()
                }) { "Accepted history cannot be changed by a stale screen" }
            }
            val raw = normalize(merged.toString())
            validate(raw)
            if (raw != row?.payload) {
                val revision = (row?.revision ?: 0)+1
                dao.putAggregate(ChantalAggregateEntity(revision=revision,payload=raw))
                rebuildIndexes(raw)
                expectedRevision = revision
            } else expectedRevision = row?.revision
            lastSaved = raw
        } }
        lastSubmitted = submitted
        com.example.service.ChantalReturnScheduler.sync(context,incoming.optLong("activeJourneyStartMs",0))
    }
}
