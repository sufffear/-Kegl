package com.example.domain

/** Stage-one contracts only. No persistence, rewards, network or Compose dependencies. */
enum class ChantalForm { FOX, HUMAN }
enum class ChantalAge { INFANT, CUB, ADULT_FOX, HUMAN }
data class ChantalCharacterState(val acceptedAdventures: Int, val age: ChantalAge) {
    val form: ChantalForm get() = if (age == ChantalAge.HUMAN) ChantalForm.HUMAN else ChantalForm.FOX
    companion object {
        /** XP, previews and repeated generations deliberately are not inputs. */
        fun fromAdventures(count: Int): ChantalCharacterState {
            require(count >= 0)
            return ChantalCharacterState(count, when {
                count < 10 -> ChantalAge.INFANT
                count < 50 -> ChantalAge.CUB
                count < 100 -> ChantalAge.ADULT_FOX
                else -> ChantalAge.HUMAN
            })
        }
    }
    init {
        require(acceptedAdventures >= 0)
        require(age == when {
            acceptedAdventures < 10 -> ChantalAge.INFANT
            acceptedAdventures < 50 -> ChantalAge.CUB
            acceptedAdventures < 100 -> ChantalAge.ADULT_FOX
            else -> ChantalAge.HUMAN
        })
    }
}

@JvmInline value class ChantalEventId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ChantalJourneyId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class ChantalDecisionId(val value: String) { init { require(value.isNotBlank()) } }
enum class ChantalMemoryKind { FACT, IMPRESSION, HYPOTHESIS, OPEN_QUESTION, PLAYER_DECISION, LEGACY_EPISODE }
enum class ChantalHypothesisVerdict { CONFIRMED, REFUTED }
enum class ChantalStoryEmotion { JOY, SADNESS, ANGER, FEAR, PRIDE, TENDERNESS, EMBARRASSMENT, CALM }
enum class ChantalSceneOrigin { JOURNEY, HOME, LEGACY_IMPORT }
enum class ChantalPlotStatus { OPEN, INVESTIGATING, SUSPENDED, RESOLVED }
enum class ChantalWishStatus { ACTIVE, DEFERRED, DECLINED, FULFILLED }
data class ChantalWish(val id: String, val text: String, val status: ChantalWishStatus) {
    init { require(id.isNotBlank() && text.isNotBlank()) }
}
data class ChantalPlot(val id: String, val status: ChantalPlotStatus)
data class ChantalMemoryRecord(val key: String, val kind: ChantalMemoryKind, val text: String) {
    init { require(key.isNotBlank() && text.isNotBlank()) }
}

sealed interface ChantalStoryEffect {
    data class Remember(val record: ChantalMemoryRecord) : ChantalStoryEffect
    /** A new event provides evidence; the original hypothesis is never overwritten. */
    data class JudgeHypothesis(val hypothesisEvent: ChantalEventId, val evidenceEvent: ChantalEventId,
        val verdict: ChantalHypothesisVerdict) : ChantalStoryEffect
    data class MovePlot(val id: String, val to: ChantalPlotStatus) : ChantalStoryEffect
}

/** Authored permissions issued by the engine, never by the model. */
data class ChantalEventSpec(val key: String, val origin: ChantalSceneOrigin,
    val locationId: String, val participantIds: Set<String>, val effect: ChantalStoryEffect,
    val referencedItemIds: Set<String> = emptySet())

data class ChantalStoryEvent(val id: ChantalEventId, val spec: ChantalEventSpec,
    val journeyId: ChantalJourneyId? = null, val decisionId: ChantalDecisionId? = null,
    val predecessorIds: Set<ChantalEventId> = emptySet())

/** These types intentionally have no money, XP, inventory or persistence operations. */
data class ChantalStoryText(val title: String, val paragraphs: List<String>)
data class ChantalAiStoryProposal(val text: ChantalStoryText, val requestedSpecKeys: List<String>)
data class ChantalPlayerDecision(val id: ChantalDecisionId, val episodeId: String,
    val label: String, val permittedFutureSpecKeys: Set<String>)
data class ChantalConfirmedEvent(val sequence: Long, val event: ChantalStoryEvent)
data class ChantalStoryState(val events: List<ChantalConfirmedEvent> = emptyList(),
    val plots: Map<String, ChantalPlotStatus> = emptyMap(),
    val wish: ChantalWish? = null, val storyEmotion: ChantalStoryEmotion? = null)

/** Finite, engine-issued catalogue (may be scoped to the current event references). Unknown entities and model-invented event specs cannot enter canon. */
class ChantalStoryRules(private val locations: Set<String>, private val npcs: Set<String>,
    private val memoryKeys: Set<String>, private val plotIds: Set<String>, specs: List<ChantalEventSpec>,
    private val itemIds: Set<String> = emptySet()) {
    private val catalogue = specs.associateBy { it.key }
    init {
        require(catalogue.size == specs.size)
        require(specs.all { it.key.isNotBlank() })
        specs.forEach { s ->
            require(s.locationId in locations && s.participantIds.all { it in npcs })
            require(s.referencedItemIds.all { it in itemIds })
            when (val e = s.effect) {
                is ChantalStoryEffect.Remember -> require(e.record.key in memoryKeys)
                is ChantalStoryEffect.MovePlot -> require(e.id in plotIds)
                is ChantalStoryEffect.JudgeHypothesis -> Unit
            }
        }
    }

    /** Pure validation/reduction. Disk atomicity and economic confirmation belong to stage two. */
    fun validateDecision(decision: ChantalPlayerDecision) {
        require(decision.episodeId.isNotBlank() && decision.label.isNotBlank())
        require(decision.permittedFutureSpecKeys.all { it in catalogue })
    }

    fun validateState(state: ChantalStoryState) {
        var rebuilt = ChantalStoryState()
        state.events.forEach { confirmed ->
            require(confirmed.sequence == rebuilt.events.size.toLong() + 1L)
            val next = reduce(rebuilt, confirmed.event)
            require(next.events.size == rebuilt.events.size + 1)
            rebuilt = next
        }
        require(rebuilt.plots == state.plots) { "Plot projection differs from event history" }
    }

    fun accept(state: ChantalStoryState, event: ChantalStoryEvent): ChantalStoryState {
        validateState(state)
        return reduce(state,event)
    }

    /** A journey outcome and its decision are validated/applied as one pure batch. */
    fun acceptBatch(state: ChantalStoryState, events: List<ChantalStoryEvent>): ChantalStoryState {
        require(events.map { it.id }.distinct().size == events.size)
        validateState(state)
        return events.fold(state) { current, event -> reduce(current,event) }
    }

    /** Only for engine-owned state. Untrusted archives must be rebuilt through the engine, not passed here. */
    internal fun acceptOwnedBatch(state: ChantalStoryState, events: List<ChantalStoryEvent>): ChantalStoryState =
        events.fold(state) { current, event -> reduce(current,event) }

    private fun reduce(state: ChantalStoryState, event: ChantalStoryEvent): ChantalStoryState {
        val previous = state.events.firstOrNull { it.event.id == event.id }
        if (previous != null) {
            require(previous.event == event) { "Conflicting payload for an existing event ID" }
            return state
        }
        require(catalogue[event.spec.key] == event.spec) { "Unknown or altered permission" }
        val known = state.events.map { it.event.id }.toSet()
        require(event.predecessorIds.all { it in known })
        require(event.spec.origin != ChantalSceneOrigin.JOURNEY || event.journeyId != null)
        require(event.spec.origin == ChantalSceneOrigin.JOURNEY || event.journeyId == null)
        if (event.decisionId != null) require(state.events.none { it.event.decisionId == event.decisionId })
        // A journey may supply multiple facts; one decision receipt owns its confirmation batch.
        if (event.journeyId != null && event.decisionId != null) require(state.events.none {
            it.event.journeyId == event.journeyId && it.event.decisionId != null && it.event.decisionId != event.decisionId
        })
        var plots = state.plots
        when (val e = event.spec.effect) {
            is ChantalStoryEffect.Remember -> {
                if (event.spec.origin == ChantalSceneOrigin.HOME) require(e.record.kind in setOf(
                    ChantalMemoryKind.IMPRESSION, ChantalMemoryKind.PLAYER_DECISION))
                if (event.spec.origin == ChantalSceneOrigin.LEGACY_IMPORT) require(e.record.kind == ChantalMemoryKind.LEGACY_EPISODE)
            }
            is ChantalStoryEffect.JudgeHypothesis -> {
                require(event.spec.origin == ChantalSceneOrigin.JOURNEY)
                val hypothesis = state.events.firstOrNull { it.event.id == e.hypothesisEvent }
                val evidence = state.events.firstOrNull { it.event.id == e.evidenceEvent }
                require((hypothesis?.event?.spec?.effect as? ChantalStoryEffect.Remember)?.record?.kind == ChantalMemoryKind.HYPOTHESIS)
                require((evidence?.event?.spec?.effect as? ChantalStoryEffect.Remember)?.record?.kind == ChantalMemoryKind.FACT)
                require(e.hypothesisEvent in event.predecessorIds && e.evidenceEvent in event.predecessorIds)
                require(state.events.none { (it.event.spec.effect as? ChantalStoryEffect.JudgeHypothesis)?.hypothesisEvent == e.hypothesisEvent })
            }
            is ChantalStoryEffect.MovePlot -> {
                require(event.spec.origin == ChantalSceneOrigin.JOURNEY)
                val old = plots[e.id]
                val allowed = when(old) {
                    null -> setOf(ChantalPlotStatus.OPEN)
                    ChantalPlotStatus.OPEN -> setOf(ChantalPlotStatus.INVESTIGATING, ChantalPlotStatus.SUSPENDED)
                    ChantalPlotStatus.INVESTIGATING -> setOf(ChantalPlotStatus.SUSPENDED, ChantalPlotStatus.RESOLVED)
                    ChantalPlotStatus.SUSPENDED -> setOf(ChantalPlotStatus.INVESTIGATING)
                    ChantalPlotStatus.RESOLVED -> emptySet()
                }
                require(e.to in allowed)
                require(e.to == ChantalPlotStatus.RESOLVED || plots.none { it.key != e.id && it.value != ChantalPlotStatus.RESOLVED })
                plots = plots + (e.id to e.to)
            }
        }
        return state.copy(events = state.events + ChantalConfirmedEvent((state.events.lastOrNull()?.sequence ?: 0L) + 1L,event), plots = plots)
    }
}

/** Editorial feedback is neither an in-world decision nor a reward-bearing action. */
enum class ChantalStoryRating { LIKED, OKAY, DISLIKED }
enum class ChantalFeedbackScope { THIS_STORY, GENERAL }
enum class ChantalFeedbackReason { FUNNY, TOUCHING, WANT_CONTINUATION, TOO_LONG, TOO_VAGUE, UNCLEAR, REPETITIVE }
data class ChantalStoryFeedback(val episodeId: String, val rating: ChantalStoryRating,
    val scope: ChantalFeedbackScope, val reasons: Set<ChantalFeedbackReason> = emptySet(), val comment: String = "") {
    init { require(episodeId.isNotBlank() && comment.length <= 1000) }
}
