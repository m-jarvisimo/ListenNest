package com.k2s.listennest.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerScreenFallbackLabelTest {
    @Test
    fun fallbackCoverBadge_usesInitialsFromMultipleWords() {
        assertEquals("TS", fallbackCoverBadge("The Stormlight Archive"))
    }

    @Test
    fun fallbackCoverBadge_usesFirstTwoLettersForSingleWordTitles() {
        assertEquals("DU", fallbackCoverBadge("Dune"))
    }

    @Test
    fun fallbackCoverBadge_returnsLNForBlankTitles() {
        assertEquals("LN", fallbackCoverBadge("   "))
    }
}
