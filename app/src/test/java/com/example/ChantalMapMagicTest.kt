package com.example
import com.example.data.*
import com.example.ui.clampMapCenter
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ChantalMapMagicTest {
    @Test fun cameraBoundsKeepImageInViewAcrossViewportSizes() {
        assertEquals(.5f, clampMapCenter(0f, 360f,360f,1f),.0001f)
        assertEquals(.1f, clampMapCenter(-10f,360f,360f,5f),.0001f)
        assertEquals(.9f, clampMapCenter(10f,360f,360f,5f),.0001f)
        assertEquals(.5f, clampMapCenter(.2f,200f,800f,2f),.0001f)
    }
    @Test fun magicRequiresOwnedFactAndDoesNotRepeatOrConsumeIt() {
        val state=ChantalWorldInteractions.empty()
        assertTrue(runCatching { ChantalMagicPractice.apply(state,ChantalWorldMemory.empty(),0) }.isFailure)
        val ep=ChantalOfflineAdventure.previewBase().copy(chosenActionSummaryRu="Взять нитку")
        val memory=ChantalWorldMemory.withFact(ChantalWorldMemory.reconcile(null,JSONArray().put(ChantalProgressCodec.encode(ep))),"magicItems",
            JSONObject().put("id","silver_thread").put("sourceEpisodeId",ep.id).put("descriptionRu","Нить").put("propertyRu","Возвращает напев у воды"))
        val before=memory.toString()
        val done=ChantalMagicPractice.apply(state,memory,0)
        assertEquals(done.toString(),ChantalMagicPractice.apply(done,memory,1).toString())
        assertEquals(before,memory.toString())
        assertFalse(state.has("silverThreadPractice"))
        ChantalWorldInteractions.validate(done)
    }
}
