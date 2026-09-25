package com.studentlauncher.mindful

import java.util.Random
import kotlin.math.roundToInt

/**
 * All Mindful Mode rules live here as pure functions: how long the door makes you wait, when a
 * budget is exceeded, which schedule is active, what a session counts as, and how rewards work.
 * Keeping the rules in one place makes them easy to test and to tune.
 */
object MindfulPolicy {
    const val MAX_PAUSE_SEC = 45
    const val OVER_BUDGET_EXTRA_SEC = 15
    const val EXAM_WINDOW_DAYS = 14
    const val MATH_REWARD_MIN = 10
    const val TASK_REWARD_MIN = 5
    const val DEFAULT_SESSION_MIN = 8
    const val MIN_COUNTED_SESSION_MS = 20_000L

    /** The wait grows each time you open the app in a day: x1, x2, x4, x6. */
    private val ESCALATION = intArrayOf(1, 2, 4, 6)

    fun pauseSeconds(basePauseSec: Int, opensToday: Int, overBudget: Boolean, strict: Boolean): Int {
        if (basePauseSec <= 0) return 0
        var s = basePauseSec * ESCALATION[opensToday.coerceIn(0, ESCALATION.lastIndex)]
        if (overBudget) s += OVER_BUDGET_EXTRA_SEC
        if (strict) s = s * 3 / 2
        return s.coerceAtMost(MAX_PAUSE_SEC)
    }

    fun isStrict(examDaysLeft: Int?, examMode: Boolean): Boolean =
        examMode && examDaysLeft != null && examDaysLeft in 0..EXAM_WINDOW_DAYS

    /** Exam mode halves the budget (never below 10 minutes). */
    fun effectiveBudget(baseMin: Int, examDaysLeft: Int?, examMode: Boolean): Int {
        if (baseMin <= 0) return 0
        return if (isStrict(examDaysLeft, examMode)) (baseMin / 2).coerceAtLeast(10) else baseMin
    }

    /** True when [nowMin] (minutes since midnight) is inside [w]. Handles windows that cross midnight. */
    fun inWindow(w: Window, nowMin: Int): Boolean {
        if (!w.enabled || w.startMin == w.endMin) return false
        return if (w.startMin < w.endMin) nowMin in w.startMin until w.endMin
        else nowMin >= w.startMin || nowMin < w.endMin
    }

    fun plan(c: DoorContext): DoorPlan {
        val over = c.budgetMin > 0 && c.usedMin >= c.budgetMin
        val free = c.balanceMin > 0
        val pause = if (free) 0 else pauseSeconds(c.basePauseSec, c.opensToday, over, c.strict)
        val headline = when {
            free -> "Free pass: ${c.balanceMin} min left"
            over -> "Over budget for today"
            c.opensToday >= 2 -> "Opened ${c.opensToday} times today"
            else -> "Take a breath"
        }
        val detail = buildString {
            append(if (c.budgetMin > 0) "${c.usedMin} of ${c.budgetMin} min used today" else "${c.usedMin} min today")
            if (c.strict) append("  -  Exam mode")
        }
        return DoorPlan(pause, over, free, headline, detail)
    }

    /** Sessions shorter than 20 seconds count as an open but not as minutes. */
    fun sessionMinutes(durationMs: Long): Int =
        if (durationMs < MIN_COUNTED_SESSION_MS) 0 else ((durationMs + 30_000L) / 60_000L).toInt().coerceAtLeast(1)

    fun averageSession(days: Collection<DayStats>): Int {
        val opens = days.sumOf { it.totalOpens }
        val mins = days.sumOf { it.totalMinutes }
        return if (opens == 0 || mins == 0) DEFAULT_SESSION_MIN
        else (mins.toFloat() / opens).roundToInt().coerceIn(3, 30)
    }

    /** Rough minutes not spent because you said "Not now". Always shown with a "~". */
    fun savedMinutes(refusals: Int, averageSessionMin: Int): Int = refusals * averageSessionMin

    fun underBudgetDays(days: List<DayStats>, budgetMin: Int): Int =
        if (budgetMin <= 0) 0 else days.count { it.totalMinutes <= budgetMin }

    /** 80% or better on a brain task earns the reward. */
    fun earnedFor(correct: Int, total: Int): Int =
        if (total > 0 && correct * 5 >= total * 4) MATH_REWARD_MIN else 0

    /** How much of [requestMin] can still be credited today under the daily [capMin]. */
    fun grantable(capMin: Int, earnedToday: Int, requestMin: Int): Int =
        minOf(requestMin, (capMin - earnedToday).coerceAtLeast(0))

    /** Deterministic for a given seed, so it can be tested. [level] 1..3 widens the number range. */
    fun mathQuestions(seed: Long, count: Int = 5, level: Int = 1): List<MathQuestion> {
        val r = Random(seed)
        val lv = level.coerceIn(1, 3)
        return List(count) { i ->
            when ((i + r.nextInt(3)) % 3) {
                0 -> {
                    val a = 12 + r.nextInt(38 + lv * 20)
                    val b = 11 + r.nextInt(38 + lv * 20)
                    MathQuestion("$a + $b", a + b)
                }
                1 -> {
                    val a = 40 + r.nextInt(60 + lv * 30)
                    val b = 10 + r.nextInt(30)
                    MathQuestion("$a - $b", a - b)
                }
                else -> {
                    val a = 3 + r.nextInt(7 + lv)
                    val b = 3 + r.nextInt(7 + lv)
                    MathQuestion("$a x $b", a * b)
                }
            }
        }
    }
}
