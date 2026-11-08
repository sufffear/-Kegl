package com.example

import com.example.domain.ChantalVisualState as S
import org.junit.Test
import org.junit.Assert.*

class ChantalVisualStateTest {
    @Test fun statesFollowRealEventsWithExplicitPriority() {
        assertEquals(S.TRAVELLING, S.resolve(true, true, true, true, true))
        assertEquals(S.HAPPY, S.resolve(false, true, true, true, true))
        assertEquals(S.RETURNED, S.resolve(false, true, false, true, true))
        assertEquals(S.TIRED, S.resolve(false, false, false, true, true))
        assertEquals(S.PACKING, S.resolve(false, false, false, false, true))
        assertEquals(S.REST, S.resolve(false, false, false, false, false))
    }
}
