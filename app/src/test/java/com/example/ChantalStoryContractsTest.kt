package com.example

import com.example.domain.*
import org.junit.Assert.*
import org.junit.Test

class ChantalStoryContractsTest {
    private fun record(key: String, kind: ChantalMemoryKind, origin: ChantalSceneOrigin = ChantalSceneOrigin.JOURNEY) =
        ChantalEventSpec(key,origin,"mill",setOf("chantal","corvus"),ChantalStoryEffect.Remember(ChantalMemoryRecord(key,kind,"Авторская запись $key")))
    private val fact = record("sound",ChantalMemoryKind.FACT)
    private val guess = record("ghost",ChantalMemoryKind.HYPOTHESIS)
    private val intention = record("return",ChantalMemoryKind.PLAYER_DECISION,ChantalSceneOrigin.HOME)
    private fun plot(id: String, status: ChantalPlotStatus) = ChantalEventSpec("$id:$status",ChantalSceneOrigin.JOURNEY,"mill",setOf("chantal"),ChantalStoryEffect.MovePlot(id,status))
    private fun rules(vararg specs: ChantalEventSpec) = ChantalStoryRules(setOf("mill"),setOf("chantal","corvus"),setOf("sound","ghost","return","old"),setOf("noise","letter"),specs.toList())
    private fun event(spec: ChantalEventSpec, id: String = spec.key, decision: String? = null, journey: String = id) =
        ChantalStoryEvent(ChantalEventId(id),spec,if(spec.origin==ChantalSceneOrigin.JOURNEY) ChantalJourneyId(journey) else null,decision?.let(::ChantalDecisionId))
    private fun rejected(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java,block) }

    @Test fun ageBoundariesDoNotDependOnXp() {
        val cases = mapOf(0 to ChantalAge.INFANT,9 to ChantalAge.INFANT,10 to ChantalAge.CUB,49 to ChantalAge.CUB,50 to ChantalAge.ADULT_FOX,99 to ChantalAge.ADULT_FOX,100 to ChantalAge.HUMAN,500 to ChantalAge.HUMAN)
        cases.forEach { (n,a) -> assertEquals(a,ChantalCharacterState.fromAdventures(n).age) }
        assertEquals(ChantalForm.HUMAN,ChantalCharacterState.fromAdventures(100).form)
        rejected { ChantalCharacterState.fromAdventures(-1) }
        rejected { ChantalCharacterState(1,ChantalAge.HUMAN) }
    }
    @Test fun exactReplayIsNoOpButConflictingIdIsRejected() {
        val r=rules(fact,guess); val e=event(fact,decision="d")
        val once=r.accept(ChantalStoryState(),e)
        assertSame(once,r.accept(once,e))
        rejected { r.accept(once,event(guess,id=e.id.value)) }
        assertEquals(1,once.events.size)
    }
    @Test fun duplicateDecisionAndSecondConfirmationOfJourneyAreRejected() {
        val r=rules(fact,guess);val once=r.accept(ChantalStoryState(),event(fact,decision="d",journey="j"))
        rejected { r.accept(once,event(guess,decision="d",journey="other")) }
        rejected { r.accept(once,event(guess,decision="d2",journey="j")) }
    }
    @Test fun oneActivePlotIncludingSuspendedAndResolvedCannotReopen() {
        val steps=listOf(plot("noise",ChantalPlotStatus.OPEN),plot("noise",ChantalPlotStatus.INVESTIGATING),plot("noise",ChantalPlotStatus.SUSPENDED),plot("noise",ChantalPlotStatus.RESOLVED),plot("letter",ChantalPlotStatus.OPEN))
        val r=rules(*steps.toTypedArray()); var s=r.accept(ChantalStoryState(),event(steps[0]))
        rejected { r.accept(s,event(steps[4])) }
        s=r.accept(s,event(steps[1]));s=r.accept(s,event(steps[2]))
        rejected { r.accept(s,event(steps[4])) }
        s=r.accept(s,event(steps[1],id="resume"));s=r.accept(s,event(steps[3]))
        rejected { r.accept(s,event(steps[0],id="reopen")) }
        assertEquals(ChantalPlotStatus.OPEN,r.accept(s,event(steps[4])).plots["letter"])
    }
    @Test fun hypothesisNeedsConfirmedEvidenceAndKeepsOriginalRecord() {
        val effect=ChantalStoryEffect.JudgeHypothesis(ChantalEventId("g"),ChantalEventId("f"),ChantalHypothesisVerdict.REFUTED)
        val verdict=ChantalEventSpec("verdict",ChantalSceneOrigin.JOURNEY,"mill",setOf("chantal"),effect)
        val r=rules(fact,guess,verdict); val g=r.accept(ChantalStoryState(),event(guess,"g"))
        val judgment=event(verdict).copy(predecessorIds=setOf(ChantalEventId("g"),ChantalEventId("f")))
        rejected { r.accept(g,judgment) }
        val f=r.accept(g,event(fact,"f"));val done=r.accept(f,judgment)
        assertEquals(g.events.first(),done.events.first())
        rejected { r.accept(done,judgment.copy(id=ChantalEventId("again"))) }
        assertEquals(ChantalMemoryKind.HYPOTHESIS,(done.events.first().event.spec.effect as ChantalStoryEffect.Remember).record.kind)
    }
    @Test fun unknownNpcPlaceItemAndAlteredSpecAreRejected() {
        rejected { rules(fact.copy(participantIds=setOf("invented"))) }
        rejected { rules(fact.copy(locationId="moon")) }
        rejected { rules(fact.copy(referencedItemIds=setOf("invented_item"))) }
        rejected { rules(fact).accept(ChantalStoryState(),event(fact.copy(locationId="moon"))) }
    }
    @Test fun homeDecisionAddsIntentionWithoutRewritingPastOrCreatingFact() {
        val homeFact=fact.copy(key="home",origin=ChantalSceneOrigin.HOME)
        val r=rules(fact,intention,homeFact)
        val past=r.accept(ChantalStoryState(),event(fact))
        val next=r.accept(past,event(intention))
        assertEquals(past.events,next.events.take(1))
        rejected { r.accept(next,event(homeFact)) }
        assertEquals(2,next.events.size)
    }
    @Test fun legacyDoesNotBecomeConfirmedFact() {
        val old=record("old",ChantalMemoryKind.LEGACY_EPISODE,ChantalSceneOrigin.LEGACY_IMPORT)
        val fake=old.copy(key="fake",effect=ChantalStoryEffect.Remember(ChantalMemoryRecord("old",ChantalMemoryKind.FACT,"старый текст")))
        val r=rules(old,fake)
        assertEquals(1,r.accept(ChantalStoryState(),event(old)).events.size)
        rejected { r.accept(ChantalStoryState(),event(fake)) }
    }
    @Test fun corruptedStateAndUnknownDecisionPermissionAreRejected() {
        val r=rules(fact); val once=r.accept(ChantalStoryState(),event(fact))
        rejected { r.accept(once.copy(plots=mapOf("noise" to ChantalPlotStatus.OPEN)),event(fact)) }
        rejected { r.validateState(once.copy(events=once.events+once.events)) }
        rejected { r.validateState(once.copy(events=listOf(once.events[0].copy(sequence=9)))) }
        rejected { r.validateDecision(ChantalPlayerDecision(ChantalDecisionId("d"),"e","Позже",setOf("unknown"))) }
        r.validateDecision(ChantalPlayerDecision(ChantalDecisionId("d"),"e","Позже",setOf(fact.key)))
    }
    @Test fun feedbackHasIndependentScopeAndBoundedComment() {
        val feedback=ChantalStoryFeedback("episode",ChantalStoryRating.DISLIKED,ChantalFeedbackScope.THIS_STORY,setOf(ChantalFeedbackReason.TOO_LONG))
        assertEquals(ChantalFeedbackScope.THIS_STORY,feedback.scope)
        rejected { feedback.copy(episodeId="") }
        rejected { feedback.copy(comment="x".repeat(1001)) }
    }
}
