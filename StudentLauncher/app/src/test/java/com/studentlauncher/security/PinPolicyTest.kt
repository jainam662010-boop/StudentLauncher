package com.studentlauncher.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {

    @Test fun acceptsFourToEightDigitsOnly() {
        assertTrue(PinPolicy.isValid("1234"))
        assertTrue(PinPolicy.isValid("12345678"))
        assertFalse(PinPolicy.isValid("123"))
        assertFalse(PinPolicy.isValid("123456789"))
        assertFalse(PinPolicy.isValid("12a4"))
        assertFalse(PinPolicy.isValid(""))
    }

    @Test fun lockoutStartsAfterFiveWrongTriesAndGrowsToACap() {
        assertEquals(0, PinPolicy.lockoutSeconds(0))
        assertEquals(0, PinPolicy.lockoutSeconds(4))
        assertEquals(30, PinPolicy.lockoutSeconds(5))
        assertEquals(60, PinPolicy.lockoutSeconds(6))
        assertEquals(120, PinPolicy.lockoutSeconds(7))
        assertEquals(240, PinPolicy.lockoutSeconds(8))
        assertEquals(PinPolicy.MAX_LOCKOUT_SEC, PinPolicy.lockoutSeconds(9))
        assertEquals(PinPolicy.MAX_LOCKOUT_SEC, PinPolicy.lockoutSeconds(50))
    }

    @Test fun resetCoolingOffCountsDown() {
        val start = 1_000L
        assertEquals(PinPolicy.RESET_DELAY_MS, PinPolicy.resetRemainingMs(start, start))
        assertEquals(0L, PinPolicy.resetRemainingMs(start, start + PinPolicy.RESET_DELAY_MS))
        assertEquals(0L, PinPolicy.resetRemainingMs(start, start + PinPolicy.RESET_DELAY_MS + 5_000L))
    }
}
