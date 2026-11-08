package com.example
import com.example.data.*
import com.example.domain.ChantalTraits
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalTrailLifeTest {
    @Test fun lettersRevealAtThresholdsAndNeverBeforeStart() {
        val start=10_000L
        assertEquals(0, ChantalTrailLife.visibleCount(0, Long.MAX_VALUE))
        assertEquals(0, ChantalTrailLife.visibleCount(start, start-100))
        assertEquals(0, ChantalTrailLife.visibleCount(start, start+15*60_000-1))
        assertEquals(1, ChantalTrailLife.visibleCount(start, start+15*60_000))
        assertEquals(2, ChantalTrailLife.visibleCount(start, start+40*60_000))
        assertEquals(2, ChantalTrailLife.visibleCount(start, start+5*60*60_000))
        assertEquals(12, (0..11).map { ChantalTrailLife.notes(it) }.toSet().size)
    }
    @Test fun notesSurviveCodecAndBecomeConfirmedContextOnlyWithChoice() {
        val ep=ChantalOfflineAdventure.previewBase().copy(trailNotesRu=ChantalTrailLife.notes(0), personalDetailsRu="Любимое угощение: яблоки")
        val restored=ChantalProgressCodec.decodeChantalStoryEpisode(ChantalProgressCodec.encode(ep))
        assertEquals(ep, restored)
        val pending=ChantalWorldMemory.reconcile(null, JSONArray().put(ChantalProgressCodec.encode(ep)))
        assertEquals(0,pending.getJSONArray("events").length())
        val accepted=ChantalWorldMemory.reconcile(null,JSONArray().put(ChantalProgressCodec.encode(ep.copy(chosenActionSummaryRu="Помочь"))))
        assertEquals(ep.trailNotesRu[0],accepted.getJSONArray("events").getJSONObject(0).getJSONArray("trailNotesRu").getString(0))
        val prompt=ChantalStoryGeneration.prompt(ep,ChantalWorldMemory.empty(),ChantalTraits())
        assertTrue(prompt.contains(ep.trailNotesRu[0]))
        assertTrue(prompt.contains(ep.personalDetailsRu))
    }
}
