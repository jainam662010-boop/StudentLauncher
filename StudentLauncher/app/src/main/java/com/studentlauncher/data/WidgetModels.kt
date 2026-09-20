package com.studentlauncher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class WidgetBg { Glass, Solid, None }

/** Widget type ids and the catalog shown in "Add a widget". */
object WT {
    const val CLOCK = "clock"
    const val DATE = "date"
    const val WEEK = "week"
    const val BATTERY = "battery"
    const val NEXT = "next"
    const val COUNTDOWN = "countdown"
    const val TIMER = "timer"
    const val TASKS = "tasks"
    const val QUICK = "quick"
    const val SPHERE = "sphere"
    const val GLYPH = "glyph"
    const val ANDROID = "android"

    val catalog: List<Triple<String, String, String>> = listOf(
        Triple(CLOCK, "Clock", "Dot, digital, words or analog"),
        Triple(DATE, "Date", "Big day number"),
        Triple(WEEK, "Week", "This week at a glance"),
        Triple(BATTERY, "Battery", "Ring with the charge level"),
        Triple(NEXT, "Next up", "Your next class or task"),
        Triple(COUNTDOWN, "Exam countdown", "Days left, in dots"),
        Triple(TIMER, "Study timer", "15 to 60 minute focus timer"),
        Triple(TASKS, "Tasks", "A tiny checklist"),
        Triple(QUICK, "Quick settings", "Shortcuts to Wi-Fi, Bluetooth and more"),
        Triple(SPHERE, "3D sphere", "Drag to spin it"),
        Triple(GLYPH, "Glyph matrix", "25 by 25 dot display, tap to change")
    )

    fun title(type: String): String =
        catalog.firstOrNull { it.first == type }?.second ?: if (type == ANDROID) "Android widget" else type
}

data class WidgetItem(
    val id: String,
    val type: String,
    val bg: WidgetBg = WidgetBg.Glass,
    val half: Boolean = false,
    val size: Int = 1,
    val tone: String = "auto",
    val cfg: Map<String, String> = emptyMap()
) {
    fun get(key: String, def: String = ""): String = cfg[key] ?: def
    fun put(key: String, value: String): WidgetItem = copy(cfg = cfg + (key to value))
}

fun newWidget(type: String): WidgetItem = WidgetItem(
    id = UUID.randomUUID().toString(),
    type = type,
    half = type in setOf(WT.DATE, WT.BATTERY, WT.NEXT, WT.COUNTDOWN, WT.TIMER),
    cfg = when (type) {
        WT.CLOCK -> mapOf("style" to "Dot", "fmt" to "sys", "date" to "1", "quote" to "1")
        WT.NEXT -> mapOf("title" to "Physics", "time" to "10:30")
        WT.COUNTDOWN -> mapOf("title" to "Exam", "date" to "")
        WT.TIMER -> mapOf("min" to "25", "start" to "0")
        WT.GLYPH -> mapOf("mode" to "Pulse")
        WT.WEEK -> mapOf("mon" to "1")
        else -> emptyMap()
    }
)

fun defaultWidgets(): List<WidgetItem> = listOf(newWidget(WT.CLOCK), newWidget(WT.NEXT), newWidget(WT.TIMER))

fun List<WidgetItem>.toJson(): String {
    val arr = JSONArray()
    forEach { w ->
        val cfg = JSONObject()
        w.cfg.forEach { (k, v) -> cfg.put(k, v) }
        arr.put(
            JSONObject()
                .put("id", w.id).put("type", w.type).put("bg", w.bg.name)
                .put("half", w.half).put("size", w.size).put("tone", w.tone).put("cfg", cfg)
        )
    }
    return arr.toString()
}

fun parseWidgets(s: String?): List<WidgetItem>? {
    if (s == null) return null
    return runCatching {
        val arr = JSONArray(s)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val c = o.optJSONObject("cfg") ?: JSONObject()
            val cfg = mutableMapOf<String, String>()
            c.keys().forEach { k -> cfg[k] = c.optString(k) }
            WidgetItem(
                id = o.getString("id"),
                type = o.getString("type"),
                bg = runCatching { WidgetBg.valueOf(o.optString("bg", "Glass")) }.getOrDefault(WidgetBg.Glass),
                half = o.optBoolean("half", false),
                size = o.optInt("size", 1),
                tone = o.optString("tone", "auto"),
                cfg = cfg
            )
        }
    }.getOrNull()
}
