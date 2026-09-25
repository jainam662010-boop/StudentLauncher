package com.studentlauncher.mindful

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindfulPolicyTest {

    @Test fun pauseEscalatesWithEachOpen() {
        assertEquals(5, MindfulPolicy.pauseSeconds(5, 0, overBudget = false, strict = false))
        assertEquals(10, MindfulPolicy.pauseSeconds(5, 1, false, false))
        assertEquals(20, MindfulPolicy.pauseSeconds(5, 2, false, false))
        assertEquals(30, MindfulPolicy.pauseSeconds(5, 3, false, false))
        assertEquals(30, MindfulPolicy.pauseSeconds(5, 9, false, false))
    }

    @Test fun pauseIsZeroWhenBaseIsOff() {
        assertEquals(0, MindfulPolicy.pauseSeconds(0, 5, overBudget = true, strict = true))
    }

    @Test fun overBudgetAndStrictAddFrictionButAreCapped() {
        assertEquals(20, MindfulPolicy.pauseSeconds(5, 0, overBudget = true, strict = false))
        assertEquals(15, MindfulPolicy.pauseSeconds(5, 1, overBudget = false, strict = true))
        assertEquals(MindfulPolicy.MAX_PAUSE_SEC, MindfulPolicy.pauseSeconds(10, 3, overBudget = true, strict = true))
    }

    @Test fun examModeHalvesTheBudgetOnlyWhenAnExamIsClose() {
        assertEquals(15, MindfulPolicy.effectiveBudget(30, 10, examMode = true))
        assertEquals(30, MindfulPolicy.effectiveBudget(30, 40, examMode = true))
        assertEquals(30, MindfulPolicy.effectiveBudget(30, 3, examMode = false))
        assertEquals(30, MindfulPolicy.effectiveBudget(30, null, examMode = true))
        assertEquals(10, MindfulPolicy.effectiveBudget(15, 5, examMode = true))
        assertEquals(0, MindfulPolicy.effectiveBudget(0, 5, examMode = true))
    }

    @Test fun perAppOverrideStillGetsExamAdjustment() {
        // MindfulEngine.budgetFor() feeds an app's own override through this same function,
        // so a custom 40-minute limit is still halved to 20 when an exam is close.
        assertEquals(20, MindfulPolicy.effectiveBudget(40, 5, examMode = true))
        assertEquals(0, MindfulPolicy.effectiveBudget(0, 5, examMode = true))
    }

    @Test fun windowsWorkForSameDayAndOvernightRanges() {
        val day = Window(true, 18 * 60, 21 * 60)
        assertTrue(MindfulPolicy.inWindow(day, 18 * 60))
        assertTrue(MindfulPolicy.inWindow(day, 20 * 60 + 59))
        assertFalse(MindfulPolicy.inWindow(day, 21 * 60))
        val night = Window(true, 23 * 60, 6 * 60)
        assertTrue(MindfulPolicy.inWindow(night, 23 * 60 + 30))
        assertTrue(MindfulPolicy.inWindow(night, 2 * 60))
        assertFalse(MindfulPolicy.inWindow(night, 12 * 60))
        assertFalse(MindfulPolicy.inWindow(night.copy(enabled = false), 2 * 60))
        assertFalse(MindfulPolicy.inWindow(Window(true, 600, 600), 600))
    }

    @Test fun planSkipsTheWaitWithAFreePass() {
        val ctx = DoorContext(opensToday = 4, usedMin = 50, budgetMin = 30, balanceMin = 10, basePauseSec = 5, strict = false)
        val plan = MindfulPolicy.plan(ctx)
        assertEquals(0, plan.pauseSec)
        assertTrue(plan.freePass)
    }

    @Test fun planFlagsOverBudget() {
        val ctx = DoorContext(opensToday = 0, usedMin = 30, budgetMin = 30, balanceMin = 0, basePauseSec = 5, strict = false)
        val plan = MindfulPolicy.plan(ctx)
        assertTrue(plan.overBudget)
        assertEquals(20, plan.pauseSec)
    }

    @Test fun planWithoutBudgetIsNeverOverBudget() {
        val ctx = DoorContext(0, 500, budgetMin = 0, balanceMin = 0, basePauseSec = 5, strict = false)
        assertFalse(MindfulPolicy.plan(ctx).overBudget)
    }

    @Test fun shortSessionsCountAsOpensNotMinutes() {
        assertEquals(0, MindfulPolicy.sessionMinutes(10_000))
        assertEquals(1, MindfulPolicy.sessionMinutes(20_000))
        assertEquals(1, MindfulPolicy.sessionMinutes(60_000))
        assertEquals(6, MindfulPolicy.sessionMinutes(5 * 60_000L + 40_000L))
    }

    @Test fun averageSessionFallsBackToDefault() {
        assertEquals(MindfulPolicy.DEFAULT_SESSION_MIN, MindfulPolicy.averageSession(emptyList()))
        val d = DayStats("2026-09-20", opens = mapOf("a" to 2, "b" to 2), minutes = mapOf("a" to 20, "b" to 20))
        assertEquals(10, MindfulPolicy.averageSession(listOf(d)))
    }

    @Test fun underBudgetDaysCountsQuietDays() {
        val days = listOf(
            DayStats("d1", minutes = mapOf("a" to 10)),
            DayStats("d2", minutes = mapOf("a" to 45)),
            DayStats("d3")
        )
        assertEquals(2, MindfulPolicy.underBudgetDays(days, 30))
        assertEquals(0, MindfulPolicy.underBudgetDays(days, 0))
    }

    @Test fun rewardsNeedEightyPercentAndRespectTheDailyCap() {
        assertEquals(MindfulPolicy.MATH_REWARD_MIN, MindfulPolicy.earnedFor(4, 5))
        assertEquals(0, MindfulPolicy.earnedFor(3, 5))
        assertEquals(0, MindfulPolicy.earnedFor(0, 0))
        assertEquals(10, MindfulPolicy.grantable(30, 0, 10))
        assertEquals(5, MindfulPolicy.grantable(30, 25, 10))
        assertEquals(0, MindfulPolicy.grantable(30, 30, 10))
        assertEquals(0, MindfulPolicy.grantable(0, 0, 10))
    }

    @Test fun mathQuestionsAreDeterministicAndCorrect() {
        val a = MindfulPolicy.mathQuestions(42L, 5, 2)
        val b = MindfulPolicy.mathQuestions(42L, 5, 2)
        assertEquals(a, b)
        assertEquals(5, a.size)
        a.forEach { q ->
            val parts = q.text.split(" ")
            val x = parts[0].toInt()
            val y = parts[2].toInt()
            val expected = when (parts[1]) {
                "+" -> x + y
                "-" -> x - y
                else -> x * y
            }
            assertEquals(expected, q.answer)
            assertTrue(q.answer >= 0)
        }
    }
}
