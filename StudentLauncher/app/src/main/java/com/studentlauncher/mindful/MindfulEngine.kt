package com.studentlauncher.mindful

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.studentlauncher.FocusReminders
import com.studentlauncher.data.AppInfo
import java.time.LocalDate
import java.time.LocalTime

/** What Mindful Mode needs from the rest of the launcher. Implemented by the ViewModel. */
interface MindfulHost {
    /** Days until the nearest exam countdown widget, or null when there is none. */
    fun examDaysLeft(): Int?
    /** The wait before a distracting app opens (0 = no wait). */
    fun basePauseSec(): Int
    /** Minutes between reminders while you are in the app (0 = reminders off). */
    fun reminderRepeatMin(): Int
    fun openTasks(): List<TaskRef>
    fun completeTask(ref: TaskRef)
}

data class TaskRef(val widgetId: String, val index: Int, val text: String)

/** The door that is currently shown for a distracting app. */
data class DoorRequest(val app: AppInfo, val rect: Rect, val plan: DoorPlan)

/**
 * Owns Mindful Mode state: today's numbers, the open door, the running session and the schedules.
 * Rules come from [MindfulPolicy], storage from [MindfulStore]. Call every method on the main thread.
 */
class MindfulEngine(
    private val ctx: Context,
    private val store: MindfulStore,
    private val host: MindfulHost
) {
    var settings by mutableStateOf(store.loadSettings()); private set
    var door by mutableStateOf<DoorRequest?>(null); private set
    var settingsOpen by mutableStateOf(false)
    private var days by mutableStateOf(store.loadDays())
    var appLimitsOpen by mutableStateOf(false)
    private var appLimits by mutableStateOf(store.loadAppLimits())
    private var nowMin by mutableIntStateOf(minutesOfDay())

    // ---- read models ----
    val today: DayStats get() = days[dateKey()] ?: DayStats(dateKey())

    /** Oldest to newest, always seven entries. */
    fun week(): List<DayStats> {
        val end = LocalDate.now()
        return (6 downTo 0).map { back ->
            val k = end.minusDays(back.toLong()).toString()
            days[k] ?: DayStats(k)
        }
    }

    fun averageSessionMin(): Int = MindfulPolicy.averageSession(days.values)
    fun budgetMin(): Int = MindfulPolicy.effectiveBudget(settings.budgetMin, host.examDaysLeft(), settings.examMode)

    /** null shown as "Global" in the editor; present (including 0) overrides the global budget for this app. */
    fun appLimitFor(pkg: String): Int? = appLimits[pkg]

    fun setAppLimit(pkg: String, minutes: Int?) {
        appLimits = if (minutes == null) appLimits - pkg else appLimits + (pkg to minutes)
        store.saveAppLimits(appLimits)
    }

    /** The budget that actually applies to this app: its own override, or the global one, exam-adjusted either way. */
    fun budgetFor(pkg: String): Int =
        MindfulPolicy.effectiveBudget(appLimits[pkg] ?: settings.budgetMin, host.examDaysLeft(), settings.examMode)
    val studyHoursActive: Boolean get() = MindfulPolicy.inWindow(settings.studyHours, nowMin)
    val bedtimeActive: Boolean get() = MindfulPolicy.inWindow(settings.bedtime, nowMin)
    /** Distracting apps are hidden while a schedule is active. */
    val hidesFlagged: Boolean get() = studyHoursActive || bedtimeActive
    val canEarn: Boolean get() = settings.earnCapMin > 0 && today.earned < settings.earnCapMin

    /** Called every 30 s so schedules switch on time without a restart. */
    fun tick() { nowMin = minutesOfDay() }

    // ---- door ----
    fun beginDoor(app: AppInfo, rect: Rect) {
        val t = today
        val exam = host.examDaysLeft()
        val c = DoorContext(
            opensToday = t.opens[app.pkg] ?: 0,
            usedMin = t.minutes[app.pkg] ?: 0,
            budgetMin = budgetFor(app.pkg),
            balanceMin = t.balance,
            basePauseSec = host.basePauseSec(),
            strict = MindfulPolicy.isStrict(exam, settings.examMode)
        )
        door = DoorRequest(app, rect, MindfulPolicy.plan(c))
    }

    /** Closes the door without opening the app. [refused] counts as a "Not now". */
    fun cancelDoor(refused: Boolean) {
        if (door == null) return
        door = null
        if (refused) mutateToday { it.copy(refusals = it.refusals + 1) }
    }

    /** You chose to go in: starts the session and the reminders. */
    fun proceed(plannedMin: Int?) {
        val d = door ?: return
        door = null
        startSession(d.app, plannedMin)
    }

    // ---- sessions ----
    private fun startSession(app: AppInfo, plannedMin: Int?) {
        mutateToday { it.copy(opens = it.opens + (app.pkg to ((it.opens[app.pkg] ?: 0) + 1))) }
        store.saveSession(ActiveSession(app.pkg, app.label, System.currentTimeMillis(), plannedMin ?: 0))
        val repeat = host.reminderRepeatMin()
        if (repeat > 0) {
            val first = plannedMin ?: today.balance.takeIf { it > 0 } ?: repeat
            FocusReminders.start(ctx, app.pkg, app.label, first, repeat)
        }
    }

    /** You are back on the launcher: close the session and add the minutes. */
    fun endSession() {
        val s = store.loadSession() ?: return
        store.saveSession(null)
        val mins = MindfulPolicy.sessionMinutes(System.currentTimeMillis() - s.startMs)
        if (mins <= 0) return
        mutateToday { d ->
            d.copy(
                minutes = d.minutes + (s.pkg to ((d.minutes[s.pkg] ?: 0) + mins)),
                spent = d.spent + mins.coerceAtMost(d.balance)
            )
        }
    }

    // ---- rewards ----
    /** Adds free-pass minutes within the daily cap. Returns what was actually granted. */
    fun credit(minutes: Int): Int {
        val granted = MindfulPolicy.grantable(settings.earnCapMin, today.earned, minutes)
        if (granted > 0) mutateToday { it.copy(earned = it.earned + granted) }
        return granted
    }

    fun openTasks(): List<TaskRef> = host.openTasks()
    fun completeTask(ref: TaskRef) = host.completeTask(ref)

    // ---- settings ----
    fun update(change: (MindfulSettings) -> MindfulSettings) {
        settings = change(settings)
        store.saveSettings(settings)
    }

    // ---- internals ----
    private fun mutateToday(change: (DayStats) -> DayStats) {
        val key = dateKey()
        days = days + (key to change(days[key] ?: DayStats(key)))
        store.saveDays(days)
    }

    private fun dateKey(): String = LocalDate.now().toString()

    private fun minutesOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
}
