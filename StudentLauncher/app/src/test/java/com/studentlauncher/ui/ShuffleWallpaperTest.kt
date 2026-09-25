package com.studentlauncher.ui

import com.studentlauncher.ui.Wallpapers
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ShuffleWallpaperTest {

    @Test fun slotForDayRoundRobinsAcrossCount() {
        val base = LocalDate.of(2026, 9, 24).toEpochDay()
        assertEquals(0, Wallpapers.slotForDay(0, base))
        assertEquals(0, Wallpapers.slotForDay(1, base))
        val start = Wallpapers.slotForDay(3, base)
        val s3 = (0L until 6L).map { Wallpapers.slotForDay(3, base + it) }
        assertEquals(listOf(start, (start + 1) % 3, (start + 2) % 3, start, (start + 1) % 3, (start + 2) % 3), s3)
        val start5 = Wallpapers.slotForDay(5, base)
        val s5 = (0L until 10L).map { Wallpapers.slotForDay(5, base + it) }
        assertEquals(
            listOf(start5, (start5 + 1) % 5, (start5 + 2) % 5, (start5 + 3) % 5, (start5 + 4) % 5,
                start5, (start5 + 1) % 5, (start5 + 2) % 5, (start5 + 3) % 5, (start5 + 4) % 5),
            s5
        )
    }

    @Test fun slotForDayNeverIndexesEmptyOrPastEnd() {
        val base = 2_000_000L
        for (count in 1..5) {
            for (d in -500L..500L) {
                val s = Wallpapers.slotForDay(count, base + d)
                assert(s in 0 until count) { "count=$count day=$base+d slot=$s" }
            }
        }
    }

    @Test fun slotForDayIsStableWithinSameDay() {
        val day = LocalDate.of(2026, 1, 1).toEpochDay()
        val a = Wallpapers.slotForDay(5, day)
        val b = Wallpapers.slotForDay(5, day)
        assertEquals(a, b)
    }
}
