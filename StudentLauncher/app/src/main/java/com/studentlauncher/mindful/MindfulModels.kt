package com.studentlauncher.mindful

/**
 * Plain data for Mindful Mode. No Android or Compose types in this file, so everything here
 * (and MindfulPolicy) can be unit tested on the JVM.
 */

/** One calendar day of behaviour, keyed by ISO date (yyyy-MM-dd). */
data class DayStats(
    val date: String,
    /** package -> times it was opened through the launcher */
    val opens: Map<String, Int> = emptyMap(),
    /** package -> approximate minutes (launch until you came back home) */
    val minutes: Map<String, Int> = emptyMap(),
    /** times you chose "Not now" */
    val refusals: Int = 0,
    /** free-pass minutes earned today */
    val earned: Int = 0,
    /** free-pass minutes used today */
    val spent: Int = 0
) {
    val totalMinutes: Int get() = minutes.values.sum()
    val totalOpens: Int get() = opens.values.sum()
    val balance: Int get() = (earned - spent).coerceAtLeast(0)
}

/** A daily time window in minutes since midnight. May cross midnight (start > end). */
data class Window(val enabled: Boolean, val startMin: Int, val endMin: Int)

data class MindfulSettings(
    /** Daily minutes allowed per distracting app. 0 = no budget. */
    val budgetMin: Int = 30,
    /** Halve budgets and add friction when an exam is close. */
    val examMode: Boolean = true,
    /** Free minutes that can be earned per day. 0 = earning switched off. */
    val earnCapMin: Int = 30,
    val studyHours: Window = Window(false, 18 * 60, 21 * 60),
    val bedtime: Window = Window(false, 23 * 60, 6 * 60)
)

/** A session that is running right now (survives the launcher process being killed). */
data class ActiveSession(val pkg: String, val label: String, val startMs: Long, val plannedMin: Int)

/** The answers to "What for?". */
enum class IntentChoice(val minutes: Int, val label: String) {
    Check(3, "Just checking"),
    Short(5, "5 min"),
    Medium(15, "15 min")
}

/** Everything the door needs to decide how hard to make it. */
data class DoorContext(
    val opensToday: Int,
    val usedMin: Int,
    /** already exam adjusted, 0 = no budget */
    val budgetMin: Int,
    val balanceMin: Int,
    val basePauseSec: Int,
    val strict: Boolean
)

data class DoorPlan(
    val pauseSec: Int,
    val overBudget: Boolean,
    val freePass: Boolean,
    val headline: String,
    val detail: String
)

data class MathQuestion(val text: String, val answer: Int)
