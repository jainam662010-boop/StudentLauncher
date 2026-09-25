package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardDragSwapTest {

    @Test fun noSwapWithinHalfNeighbor() {
        assertNull(boardSwap(offset = 40f, heightUp = 100f, heightDown = 100f))
        assertNull(boardSwap(offset = -40f, heightUp = 100f, heightDown = 100f))
        assertNull(boardSwap(offset = 50f, heightUp = 100f, heightDown = 100f))
        assertNull(boardSwap(offset = -50f, heightUp = 100f, heightDown = 100f))
    }

    @Test fun swapsWhenPastHalfUp() {
        assertEquals(-1 to 40f, boardSwap(-60f, 100f, 100f))
        assertEquals(-1 to 10f, boardSwap(-90f, 100f, 100f))
    }

    @Test fun swapsWhenPastHalfDown() {
        assertEquals(1 to -40f, boardSwap(60f, 100f, 100f))
        assertEquals(1 to -10f, boardSwap(90f, 100f, 100f))
    }

    @Test fun edgesDoNotSwap() {
        assertNull(boardSwap(-200f, null, 100f))
        assertNull(boardSwap(200f, 100f, null))
        assertNull(boardSwap(0f, null, null))
    }

    @Test fun measuredNeighborHeightsDriveThreshold() {
        // Tall neighbor: need more travel; short neighbor: swap sooner
        assertNull(boardSwap(-70f, 200f, null))
        assertEquals(-1 to -10f, boardSwap(-70f, 60f, null))
    }

    @Test fun defaultsRemainSingleColumnStep() {
        assertEquals(-1 to 40f, boardSwap(-60f, 100f, 100f))
        assertEquals(1 to -40f, boardSwap(60f, 100f, 100f))
    }
}
