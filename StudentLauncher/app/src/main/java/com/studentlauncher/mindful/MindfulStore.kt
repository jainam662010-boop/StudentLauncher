package com.studentlauncher.mindful

import android.content.SharedPreferences
import org.json.JSONObject

/** Persistence for Mindful Mode. Everything stays on the device in the launcher's private prefs. */
class MindfulStore(private val prefs: SharedPreferences) {

    // ---- settings ----
    fun loadSettings(): MindfulSettings {
        val raw = prefs.getString(K_SETTINGS, null) ?: return MindfulSettings()
        return runCatching {
            val o = JSONObject(raw)
            val d = MindfulSettings()
            MindfulSettings(
                budgetMin = o.optInt("budget", d.budgetMin),
                examMode = o.optBoolean("exam", d.examMode),
                earnCapMin = o.optInt("earnCap", d.earnCapMin),
                studyHours = window(o.optJSONObject("study"), d.studyHours),
                bedtime = window(o.optJSONObject("bed"), d.bedtime)
            )
        }.getOrDefault(MindfulSettings())
    }

    fun saveSettings(s: MindfulSettings) {
        val o = JSONObject()
            .put("budget", s.budgetMin)
            .put("exam", s.examMode)
            .put("earnCap", s.earnCapMin)
            .put("study", windowJson(s.studyHours))
            .put("bed", windowJson(s.bedtime))
        prefs.edit().putString(K_SETTINGS, o.toString()).apply()
    }

    // ---- per-app limit overrides (absence = use the global budget) ----
    fun loadAppLimits(): Map<String, Int> {
        val raw = prefs.getString(K_LIMITS, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { k -> o.getInt(k) }
        }.getOrDefault(emptyMap())
    }

    fun saveAppLimits(limits: Map<String, Int>) {
        val o = JSONObject()
        limits.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(K_LIMITS, o.toString()).apply()
    }

    // ---- daily stats ----
    fun loadDays(): Map<String, DayStats> {
        val raw = prefs.getString(K_DAYS, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { k -> dayFromJson(k, o.getJSONObject(k)) }
        }.getOrDefault(emptyMap())
    }

    /** Keeps the newest [KEEP_DAYS] days so storage never grows. */
    fun saveDays(days: Map<String, DayStats>) {
        val o = JSONObject()
        days.keys.sortedDescending().take(KEEP_DAYS).forEach { k -> o.put(k, dayToJson(days.getValue(k))) }
        prefs.edit().putString(K_DAYS, o.toString()).apply()
    }

    // ---- active session ----
    fun loadSession(): ActiveSession? {
        val raw = prefs.getString(K_SESSION, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            ActiveSession(o.getString("pkg"), o.getString("label"), o.getLong("start"), o.optInt("planned", 0))
        }.getOrNull()
    }

    fun saveSession(s: ActiveSession?) {
        val e = prefs.edit()
        if (s == null) e.remove(K_SESSION)
        else e.putString(K_SESSION, JSONObject().put("pkg", s.pkg).put("label", s.label).put("start", s.startMs).put("planned", s.plannedMin).toString())
        e.apply()
    }

    // ---- json helpers ----
    private fun window(o: JSONObject?, def: Window) =
        if (o == null) def else Window(o.optBoolean("on", def.enabled), o.optInt("from", def.startMin), o.optInt("to", def.endMin))

    private fun windowJson(w: Window) = JSONObject().put("on", w.enabled).put("from", w.startMin).put("to", w.endMin)

    private fun intMap(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        return o.keys().asSequence().associateWith { k -> o.optInt(k, 0) }
    }

    private fun mapJson(m: Map<String, Int>): JSONObject {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, v) }
        return o
    }

    private fun dayFromJson(date: String, o: JSONObject) = DayStats(
        date = date,
        opens = intMap(o.optJSONObject("opens")),
        minutes = intMap(o.optJSONObject("minutes")),
        refusals = o.optInt("refusals", 0),
        earned = o.optInt("earned", 0),
        spent = o.optInt("spent", 0)
    )

    private fun dayToJson(d: DayStats) = JSONObject()
        .put("opens", mapJson(d.opens))
        .put("minutes", mapJson(d.minutes))
        .put("refusals", d.refusals)
        .put("earned", d.earned)
        .put("spent", d.spent)

    private companion object {
        const val K_SETTINGS = "mindful_settings"
        const val K_DAYS = "mindful_days"
        const val K_SESSION = "mindful_session"
        const val K_LIMITS = "mindful_app_limits"
        const val KEEP_DAYS = 30
    }
}
