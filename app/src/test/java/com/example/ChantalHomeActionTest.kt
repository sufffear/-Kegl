package com.example
import com.example.ui.ChantalHomeAction as A
import com.example.ui.chantalHomeAction
import com.example.domain.ChantalJourneyClock
import org.junit.Assert.*
import org.junit.Test
class ChantalHomeActionTest {
    @Test fun requiredRewardAndStoryTakePriorityOverTravelAndPreparation() {
        assertEquals(A.REWARD, chantalHomeAction(true,true,100,Long.MAX_VALUE))
        assertEquals(A.STORY, chantalHomeAction(false,true,100,Long.MAX_VALUE))
        assertEquals(A.PREPARE, chantalHomeAction(false,false,0,Long.MAX_VALUE))
    }
    @Test fun returnRequiresTheWholeExistingJourneyDurationAndDoesNotChangeCanon() {
        val start = 100L
        assertEquals(A.TRAVELLING, chantalHomeAction(false,false,start,start-1))
        assertEquals(A.TRAVELLING, chantalHomeAction(false,false,start,start+ChantalJourneyClock.DURATION_MS-1))
        assertEquals(A.RETURN, chantalHomeAction(false,false,start,start+ChantalJourneyClock.DURATION_MS))
    }
    @Test fun finishedTravelDoesNotLeaveStalePreparationBehindTheStory() {
        val home = com.example.ui.ChantalPage.HOME
        val prep = com.example.ui.ChantalPage.PREPARE
        val trail = com.example.ui.ChantalPage.TRAIL
        val story = com.example.ui.ChantalPage.STORY
        val inTravel = com.example.ui.chantalPagesAfterOpen(listOf(home.name,prep.name),trail)
        assertEquals(listOf(home.name,trail.name),inTravel)
        val reading = com.example.ui.chantalPagesAfterOpen(inTravel,story)
        assertEquals(listOf(home.name,story.name),reading)
        assertEquals(listOf(home.name),reading.dropLast(1))
        assertEquals(listOf(home.name,"WORLD","BAG"),com.example.ui.chantalPagesAfterOpen(listOf(home.name,"WORLD"),com.example.ui.ChantalPage.BAG))
    }

}
