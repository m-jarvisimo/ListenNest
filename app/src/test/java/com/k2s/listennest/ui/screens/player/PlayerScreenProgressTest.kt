package com.k2s.listennest.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerScreenProgressTest {
    @Test
    fun progressSummary_returnsPercentCompleteWhenDurationIsKnown() {
        assertEquals("42% complete", progressSummaryLabel(positionMs = 8 * 60 * 1000L + 24 * 1000L, durationMs = 20 * 60 * 1000L))
    }

    @Test
    fun progressSummary_returnsListeningProgressWhenDurationIsUnknown() {
        assertEquals("Listening progress", progressSummaryLabel(positionMs = 10_000L, durationMs = 0L))
    }
}
