package com.studentlauncher.data

import androidx.compose.ui.unit.dp
import com.studentlauncher.ui.homeAppsWindowMax
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure pin-limit policy: mirrors LauncherViewModel.togglePin gate. */
class HomePinPolicyTest {

    private fun canPin(pinned: Int, alreadyPinned: Boolean, limit: Int): Boolean =
        if (alreadyPinned) true else pinned < limit

    @Test fun allowsUpToLimit() {
        assertTrue(canPin(0, false, 4))
        assertTrue(canPin(3, false, 4))
        assertFalse(canPin(4, false, 4))
        assertFalse(canPin(8, false, 8))
    }

    @Test fun loweringLimitKeepsExistingButBlocksNew() {
        assertTrue(canPin(8, alreadyPinned = true, limit = 4))
        assertFalse(canPin(8, alreadyPinned = false, limit = 4))
    }

    @Test fun unpinAlwaysAllowed() {
        assertTrue(canPin(12, alreadyPinned = true, limit = 4))
    }

    @Test fun windowIsTwoGridRowsRegardlessOfPinCount() {
        assertEquals(178.dp, homeAppsWindowMax(showNames = true, isGrid = true))
        assertEquals(138.dp, homeAppsWindowMax(showNames = false, isGrid = true))
        assertEquals(400.dp, homeAppsWindowMax(showNames = true, isGrid = false))
    }
}
