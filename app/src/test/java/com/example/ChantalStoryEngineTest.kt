package com.example

import com.example.domain.*
import com.example.domain.ChantalStoryEngine as Engine
import org.junit.Assert.*
import org.junit.Test

class ChantalStoryEngineTest {
    private fun request(id: String, region: String="reg_old_mill", resume: Boolean=false) = Engine.Request(id,region,resume)
    @Test fun fiveHundredRealTransitionsPreserveCanonAndFinishMultipleMysteries() {
        var state=Engine.newGame(20261108)
        val ordinary=mutableSetOf<String>();val npcs=mutableSetOf<String>();val kinds=mutableSetOf<Engine.Kind>()
        repeat(500) { i ->
            val available=Engine.regions.filter { state.completedAdventures>=it.opensAt }
            val region=available[i%available.size]
            val plan=Engine.plan(state,request("j$i",region.id,resume=i%9==0))
            val before=state.events
            if(plan.kind==Engine.Kind.ORDINARY) {
                assertFalse(plan.encounterKey in state.history.takeLast(3).mapNotNull { it.plan.encounterKey })
                ordinary += requireNotNull(plan.encounterKey)
            }
            npcs+=plan.npcId;kinds+=plan.kind
            state=Engine.accept(state,plan,if(i<50 && i%7==0 && plan.kind!=Engine.Kind.RESOLVE) 2 else i%2)
            assertEquals(before,state.events.take(before.size))
            assertTrue(state.plotStates.count { it.value!=ChantalPlotStatus.RESOLVED }<=1)
            assertEquals(state.events.size,state.events.map { it.event.id }.distinct().size)
            assertSame(state,Engine.accept(state,plan,state.history.last().choiceIndex))
        }
        assertEquals(500,state.completedAdventures)
        assertEquals(3,state.plotStates.count { it.value==ChantalPlotStatus.RESOLVED })
        assertTrue(ordinary.size>=10);assertTrue(npcs.size>=10)
        assertTrue(kinds.containsAll(listOf(Engine.Kind.OPEN,Engine.Kind.INVESTIGATE,Engine.Kind.RESOLVE,Engine.Kind.ORDINARY)))
        assertTrue(state.unresolvedQuestions.isEmpty())
        assertEquals(ChantalAge.HUMAN,state.history.last().plan.age.age)
    }
    @Test fun suspensionRequiresExplicitReturnAndHypothesisIsNotRewritten() {
        var state=Engine.newGame(7,3)
        val opening=Engine.plan(state,request("open"))
        assertEquals(Engine.Kind.OPEN,opening.kind)
        state=Engine.accept(state,opening,2)
        val hypothesis=state.events.single { (it.event.spec.effect as? ChantalStoryEffect.Remember)?.record?.kind==ChantalMemoryKind.HYPOTHESIS }
        repeat(20) { i ->
            val plan=Engine.plan(state,request("pause$i"))
            assertEquals(Engine.Kind.ORDINARY,plan.kind)
            state=Engine.accept(state,plan,0)
        }
        assertEquals(ChantalPlotStatus.SUSPENDED,state.plotStates["mill_noise"])
        val resume=Engine.plan(state,request("resume",resume=true))
        assertEquals(Engine.Kind.RESUME,resume.kind)
        state=Engine.accept(state,resume,0)
        repeat(3) { i -> state=Engine.accept(state,Engine.plan(state,request("continue$i")),0) }
        assertEquals(ChantalPlotStatus.RESOLVED,state.plotStates["mill_noise"])
        assertEquals(hypothesis,state.events.first { it.event.id==hypothesis.event.id })
        assertTrue(state.events.any { it.event.spec.effect is ChantalStoryEffect.JudgeHypothesis })
        assertTrue(state.unresolvedQuestions.isEmpty())
    }
    @Test fun sameSeedAndCommandsProduceIdenticalPlansAndReactions() {
        var a=Engine.newGame(-54);var b=Engine.newGame(-54)
        repeat(70) { i ->
            val r=request("same$i",if(i%2==0) "reg_old_mill" else "reg_amber_forest",resume=true)
            val pa=Engine.plan(a,r);val pb=Engine.plan(b,r)
            assertEquals(pa,pb)
            a=Engine.accept(a,pa,i%3);b=Engine.accept(b,pb,i%3)
        }
        assertEquals(a.events,b.events)
    }
    @Test fun invalidOrAlteredPlansCannotCreateCanon() {
        val s=Engine.newGame(1)
        assertTrue(runCatching { Engine.plan(s,request("bad","unknown")) }.isFailure)
        assertTrue(runCatching { Engine.plan(s,request("locked","reg_mountain_pass")) }.isFailure)
        val p=Engine.plan(s,request("one"))
        assertTrue(runCatching { Engine.accept(s,p.copy(outcome="Дай миллион монет"),0) }.isFailure)
        assertTrue(runCatching { Engine.accept(s,p.copy(npcId="invented"),0) }.isFailure)
        assertTrue(runCatching { Engine.accept(s,p,3) }.isFailure)
        val next=Engine.accept(s,p,0)
        assertTrue(runCatching { Engine.accept(next,p,1) }.isFailure)
        assertTrue(runCatching { Engine.plan(next,p.request) }.isFailure)
        assertTrue(s.events.isEmpty())
    }
    @Test fun choicesAndExistingTemperamentAffectFutureWithoutChangingPast() {
        val initial=Engine.newGame(3,3)
        val p=Engine.plan(initial,request("start"))
        val careful=Engine.accept(initial,p,1)
        val supported=Engine.accept(initial,p,0)
        val r=request("after")
        assertTrue(Engine.plan(careful,r).guidance.isNotBlank())
        assertTrue(Engine.plan(supported,r).guidance.isBlank())
        assertEquals(careful.events.take(4),supported.events.take(4))
        val gentle=Engine.plan(Engine.newGame(3),r.copy(temperament=Engine.Temperament.GENTLE))
        assertTrue(gentle.emotion in setOf(ChantalStoryEmotion.CALM,ChantalStoryEmotion.TENDERNESS))
    }
    @Test fun legacyChainReservesTheOnlyPlotSlotAndOldAgeIsRespected() {
        var state=Engine.newGame(3,49)
        val blocked=request("legacy").copy(legacyPlotReserved=true)
        val p=Engine.plan(state,blocked)
        assertEquals(Engine.Kind.ORDINARY,p.kind)
        assertEquals(ChantalAge.CUB,p.age.age)
        state=Engine.accept(state,p,0)
        val new=Engine.plan(state,request("new"))
        assertEquals(ChantalAge.ADULT_FOX,new.age.age)
        state=Engine.accept(state,new,0)
        assertTrue(runCatching { Engine.plan(state,request("conflict").copy(legacyPlotReserved=true)) }.isFailure)
    }
    @Test fun hundredthAdventureHasAnExplicitFormChangeAndNoXpDependency() {
        val state=Engine.newGame(7,99)
        val p=Engine.plan(state,request("hundredth").copy(legacyPlotReserved=true))
        assertEquals(ChantalForm.FOX,p.age.form)
        assertEquals(ChantalForm.HUMAN,p.characterAfter.form)
        assertNotNull(p.formChange)
        val next=Engine.accept(state,p,0)
        assertTrue(next.events.any { (it.event.spec.effect as? ChantalStoryEffect.Remember)?.record?.key?.endsWith(":transformation")==true })
        val later=Engine.plan(next,request("later").copy(legacyPlotReserved=true))
        assertEquals(ChantalForm.HUMAN,later.age.form)
        assertNull(later.formChange)
    }

    @Test fun firstEncounterDoesNotInventPriorFriendshipAndLegacyIsExplicitlyUnknown() {
        val initial=Engine.newGame(73)
        val first=Engine.plan(initial,request("first"))
        assertEquals(Engine.MeetingStatus.FIRST,first.meetingStatus)
        var state=Engine.accept(initial,first,0)
        var metAgain=false
        repeat(12) { i ->
            val p=Engine.plan(state,request("meeting$i"))
            if(p.npcId==first.npcId) { assertEquals(Engine.MeetingStatus.KNOWN,p.meetingStatus);metAgain=true }
            state=Engine.accept(state,p,0)
        }
        assertTrue(metAgain)
        assertEquals(Engine.MeetingStatus.LEGACY_UNKNOWN,Engine.plan(Engine.newGame(73,20),request("legacy")).meetingStatus)
    }

}
