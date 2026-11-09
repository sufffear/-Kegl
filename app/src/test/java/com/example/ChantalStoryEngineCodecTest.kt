package com.example

import com.example.data.*
import com.example.domain.*
import com.example.domain.ChantalStoryEngine as Engine
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalStoryEngineCodecTest {
    private fun played(): Engine.State {
        var s=Engine.newGame(73)
        repeat(120) { i ->
            val region=Engine.regions.filter { s.completedAdventures>=it.opensAt }[i%Engine.regions.count { s.completedAdventures>=it.opensAt }]
            val p=Engine.plan(s,Engine.Request("run$i",region.id,true))
            s=Engine.accept(s,p,i%2)
        }
        return s
    }
    @Test fun archiveReplayAndNextPlanMatchWithoutSerializingInventedFacts() {
        val state=played()
        val encoded=ChantalStoryEngineCodec.encode(state)
        val restored=ChantalStoryEngineCodec.decode(JSONObject(encoded.toString()))
        assertEquals(state.events,restored.events)
        assertEquals(state.plotStates,restored.plotStates)
        assertEquals(state.meetings,restored.meetings)
        val request=Engine.Request("next","reg_amber_forest")
        assertEquals(Engine.plan(state,request),Engine.plan(restored,request))
        val snapshot=JSONObject().put("schemaVersion",1).put("coins",37).put("maturityXp",48).put("storyEngineState",encoded)
        val roundTrip=ChantalProgressRepository.validate(snapshot.toString())
        assertEquals(37,roundTrip.getInt("coins"));assertEquals(48,roundTrip.getInt("maturityXp"))
    }
    @Test fun malformedUnknownAndDuplicateCommandsCannotBypassReplay() {
        val base=ChantalStoryEngineCodec.encode(played()).toString()
        fun bad(change: (JSONObject)->Unit) {
            val raw=JSONObject(base);change(raw)
            assertTrue(runCatching { ChantalStoryEngineCodec.decode(raw) }.isFailure)
        }
        bad { it.put("version",999) }
        bad { it.put("catalogueSignature","changed") }
        bad { it.put("plots",JSONObject().put("invented","OPEN")) }
        bad { it.getJSONArray("runs").getJSONObject(0).put("locationId","unknown") }
        bad { it.getJSONArray("runs").getJSONObject(0).put("locationId","reg_mountain_pass") }
        bad { it.getJSONArray("runs").getJSONObject(0).put("choiceIndex",8) }
        bad { it.getJSONArray("runs").getJSONObject(0).put("temperament","NEW_TRAIT") }
        bad { it.getJSONArray("runs").getJSONObject(1).put("journeyId","run0") }
    }
    @Test fun compactContextContainsSourcesAndSeparatesIntentionsFromFacts() {
        val state=played()
        val c=Engine.context(state,"reg_old_mill")
        assertTrue(c.memories.size<=12)
        val known=state.events.map { it.event.id }.toSet()
        assertTrue(c.memories.all { it.eventId in known })
        assertTrue(c.memories.none { it.record.kind==ChantalMemoryKind.PLAYER_DECISION || it.record.kind==ChantalMemoryKind.LEGACY_EPISODE })
        assertNotNull(c.lastIntention)
        assertEquals(ChantalAge.HUMAN,c.character.age)
        assertEquals(ChantalForm.HUMAN,c.character.form)
    }
}
