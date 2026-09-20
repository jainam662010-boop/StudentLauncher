package com.studentlauncher.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WT
import com.studentlauncher.data.WidgetBg
import com.studentlauncher.data.WidgetItem
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------------------------
// Dot-matrix helpers (Nothing style, drawn on canvas so no font is needed)
// ---------------------------------------------------------------------------------------------

private val GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    ':' to listOf("0", "0", "1", "0", "1", "0", "0")
)

fun dotCols(text: String): Int {
    var n = 0
    text.forEachIndexed { i, ch -> n += (GLYPHS[ch]?.first()?.length ?: 0) + if (i < text.length - 1) 1 else 0 }
    return max(1, n)
}

@Composable
fun DotText(text: String, modifier: Modifier, color: Color, accent: Color) {
    Canvas(modifier) {
        val cols = dotCols(text)
        val cell = size.width / cols
        val r = cell * 0.36f
        var cx = 0
        for (ch in text) {
            val g = GLYPHS[ch] ?: continue
            for (row in g.indices) for (col in g[row].indices) {
                val lit = g[row][col] == '1'
                val base = if (ch == ':') accent else color
                drawCircle(
                    color = if (lit) base else color.copy(alpha = 0.10f),
                    radius = r,
                    center = Offset((cx + col + 0.5f) * cell, (row + 0.5f) * cell)
                )
            }
            cx += g[0].length + 1
        }
    }
}

@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }
    return now
}

private val MESSAGES = listOf(
    "Start with ten minutes.",
    "Small sessions add up.",
    "You have done harder things.",
    "Phone down. Page open.",
    "One chapter, then a break.",
    "Future you will be glad."
)

private val ONES = listOf("twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven")
private val TEENS = listOf("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
private val UNITS = listOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
private val TENS = listOf("", "", "twenty", "thirty", "forty", "fifty")

private fun hourWord(h: Int) = ONES[h % 12]
private fun minuteWords(m: Int): String = when {
    m == 0 -> "o'clock"
    m < 10 -> "oh ${UNITS[m]}"
    m < 20 -> TEENS[m - 10]
    else -> TENS[m / 10] + if (m % 10 == 0) "" else " ${UNITS[m % 10]}"
}

fun parseTasks(s: String): List<Pair<Boolean, String>> =
    s.split("\n").filter { it.length > 2 }.map { (it[0] == '1') to it.drop(2) }

fun encodeTasks(l: List<Pair<Boolean, String>>): String =
    l.joinToString("\n") { (if (it.first) "1" else "0") + "|" + it.second }

// ---------------------------------------------------------------------------------------------
// Widget area
// ---------------------------------------------------------------------------------------------

private fun buildRows(items: List<WidgetItem>): List<List<WidgetItem>> {
    val rows = mutableListOf<List<WidgetItem>>()
    var i = 0
    while (i < items.size) {
        val a = items[i]
        val b = items.getOrNull(i + 1)
        if (a.effectiveHalf && b != null && b.effectiveHalf) { rows.add(listOf(a, b)); i += 2 } else { rows.add(listOf(a)); i += 1 }
    }
    return rows
}

@Composable
private fun Appear(content: @Composable () -> Unit) {
    val motion = LocalMotionScale.current
    val s = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = s,
        enter = if (motion < .05f) fadeIn(tween(0)) else fadeIn(tween((180 * motion).toInt())) + scaleIn(Motion.Macro, initialScale = 0.96f) + expandVertically(tween((220 * motion).toInt())),
        exit = fadeOut() + scaleOut(Motion.Macro, targetScale = 0.96f) + shrinkVertically(tween((180 * motion).toInt()))
    ) { content() }
}

@Composable
fun Widgets(vm: LauncherViewModel, padStart: Dp, padEnd: Dp, compact: Boolean, maxH: Dp, modifier: Modifier) {
    val c = LocalColors.current
    val rows = buildRows(vm.homeWidgets.toList())
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize(Motion.Size)
            .heightIn(max = maxH)
            .verticalScroll(rememberScrollState())
            .padding(start = padStart, end = padEnd),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            key(row.joinToString { it.id }) {
                Appear {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        row.forEach { w -> Box(Modifier.weight(1f)) { WidgetBody(w, vm, compact) } }
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxWidth().glass(24.dp).tap { vm.widgetCenter = true }.padding(18.dp), contentAlignment = Alignment.Center) {
                Txt("No widgets yet. Tap to add one.", 13f, c.fg2)
            }
        }
    }
}

@Composable
private fun WidgetFrame(w: WidgetItem, vm: LauncherViewModel, content: @Composable ColumnScope.(Color, Color) -> Unit) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val fg = toneColor(w.tone, c)
    val fg2 = fg.copy(alpha = 0.6f)
    var showMenu by remember { mutableStateOf(false) }
    var resizeAccum by remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxWidth()) {
        // Widget content with long-press for context menu
        Box(
            Modifier
                .fillMaxWidth()
                .then(
                    when (w.bg) {
                        WidgetBg.Glass -> Modifier.glass(12.dp).border(1.dp, c.fg2.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        WidgetBg.Solid -> Modifier.clip(RoundedCornerShape(12.dp)).background(if (c.dark) Color(0xFF1C1C1E) else Color.White).border(1.dp, c.fg2.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        WidgetBg.None -> Modifier
                    }
                )
                .pointerInput(w.id) {
                    detectTapGestures(onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    })
                }
                .padding(if (w.bg == WidgetBg.None) 6.dp else 16.dp)
        ) {
            Column { content(fg, fg2) }
        }

        // Resize handle — separate touch target, NOT inside widget gesture scope
        Box(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(w.id, w.height) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        resizeAccum = 0f
                        do {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.first()
                            resizeAccum += ch.position.y - ch.previousPosition.y
                            ch.consume()
                        } while (ev.changes.any { it.pressed })
                        val steps = (resizeAccum / 36f).toInt()
                        if (steps != 0) {
                            val newH = (w.height + steps).coerceIn(1, 3)
                            if (newH != w.height) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.setWidgetHeight(w.id, newH)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.fg2.copy(alpha = 0.4f))
            )
        }

        if (showMenu) {
            WidgetContextMenu(
                w = w,
                onEdit = { vm.editWidgetId = w.id; showMenu = false },
                onRemove = { vm.removeWidget(w.id); showMenu = false },
                onSize = { h -> vm.setWidgetHeight(w.id, h); showMenu = false },
                onDismiss = { showMenu = false }
            )
        }
    }
}

@Composable
private fun WidgetContextMenu(w: WidgetItem, onEdit: () -> Unit, onRemove: () -> Unit, onSize: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val labels = listOf(1 to "Small", 2 to "Medium", 3 to "Tall")
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        Column(
            Modifier
                .align(Alignment.Center)
                .width(180.dp)
                .glass(20.dp)
                .padding(8.dp)
                .pointerInput(Unit) { detectTapGestures {} }
        ) {
            CtxMenuRow("Edit") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onEdit() }
            CtxMenuRow("Remove", danger = true) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRemove() }
            Txt("Size", 11f, c.fg2, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                labels.forEach { (h, label) ->
                    val selected = w.height == h
                    Box(
                        Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) c.fg else c.track)
                            .tap { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSize(h) },
                        contentAlignment = Alignment.Center
                    ) { Txt(label, 11f, if (selected) c.knobOn else c.fg) }
                }
            }
        }
    }
}

@Composable
private fun CtxMenuRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).tap(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) { Txt(label, 14f, if (danger) LocalColors.current.accent else LocalColors.current.fg) }
}

@Composable
fun WidgetBody(w: WidgetItem, vm: LauncherViewModel, compact: Boolean) {
    if (w.type == WT.ANDROID) {
        HostedWidget(vm, w, Modifier.fillMaxWidth())
        return
    }
    val accent = LocalColors.current.accent
    // Height 1=80dp, 2=120dp, 3=180dp — controls max vertical space
    val maxContentH = when (w.height) {
        1 -> if (compact) 60.dp else 80.dp
        3 -> if (compact) 140.dp else 180.dp
        else -> if (compact) 90.dp else 120.dp
    }
    WidgetFrame(w, vm) { fg, fg2 ->
        Column(Modifier.heightIn(max = maxContentH)) {
            when (w.type) {
                WT.CLOCK -> ClockContent(w, fg, fg2, accent)
                WT.DATE -> DateContent(fg, fg2)
                WT.WEEK -> WeekContent(w, fg, fg2, accent)
                WT.BATTERY -> BatteryContent(fg, accent)
                WT.NEXT -> NextContent(w, fg, fg2)
                WT.COUNTDOWN -> CountdownContent(w, fg, fg2, accent)
                WT.TIMER -> TimerContent(w, vm, fg, fg2, accent)
                WT.TASKS -> TasksContent(w, vm, fg, fg2, accent)
                WT.QUICK -> QuickContent(fg)
                WT.SPHERE -> Box(Modifier.fillMaxWidth().height(if (compact) 60.dp else 80.dp)) {
                    DotSphere(Modifier.fillMaxSize(), fg, accent)
                }
                WT.GLYPH -> GlyphContent(w, vm, fg, accent, compact)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Widgets
// ---------------------------------------------------------------------------------------------

@Composable
private fun ClockContent(w: WidgetItem, fg: Color, fg2: Color, accent: Color) {
    val now = rememberNow()
    val ctx = LocalContext.current
    val sys24 = remember { DateFormat.is24HourFormat(ctx) }
    val is24 = when (w.get("fmt", "sys")) { "24" -> true; "12" -> false; else -> sys24 }
    val time = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "hh:mm"))
    var q by remember { mutableIntStateOf(0) }
    when (w.get("style", "Dot")) {
        "Digital" -> Txt(time, 54f, fg, FontWeight.Light)
        "Words" -> {
            Txt(hourWord(now.hour), 40f, fg, FontWeight.Bold)
            Txt(minuteWords(now.minute), 40f, fg, FontWeight.Light, maxLines = 2)
        }
        "Analog" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            AnalogClock(now, Modifier.size(112.dp), fg, accent)
        }
        else -> DotText(time, Modifier.fillMaxWidth().aspectRatio(dotCols(time) / 7f), fg, accent)
    }
    if (w.get("date", "1") == "1") {
        Txt(now.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())), 12f, fg2, modifier = Modifier.padding(top = 8.dp))
    }
    if (w.get("quote", "1") == "1") {
        AnimatedContent(
            targetState = q,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
            label = "quote"
        ) { i ->
            Txt(MESSAGES[i], 13f, fg, maxLines = 2, modifier = Modifier.tap { q = (q + 1) % MESSAGES.size }.padding(top = 8.dp))
        }
    }
}

private fun polar(c: Offset, r: Float, deg: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(c.x + r * sin(a).toFloat(), c.y - r * cos(a).toFloat())
}

@Composable
private fun AnalogClock(now: LocalDateTime, modifier: Modifier, fg: Color, accent: Color) {
    Canvas(modifier) {
        val r = min(size.width, size.height) / 2f
        val c = center
        for (i in 0 until 60) {
            val big = i % 5 == 0
            drawCircle(
                color = fg.copy(alpha = if (big) 0.9f else 0.35f),
                radius = if (big) r * 0.035f else r * 0.018f,
                center = polar(c, r * 0.92f, i * 6f)
            )
        }
        val hDeg = (now.hour % 12 + now.minute / 60f) * 30f
        val mDeg = now.minute * 6f
        drawLine(fg, c, polar(c, r * 0.5f, hDeg), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(accent, c, polar(c, r * 0.78f, mDeg), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(accent, 4.dp.toPx(), c)
    }
}

@Composable
private fun DateContent(fg: Color, fg2: Color) {
    val now = rememberNow()
    Txt(now.dayOfMonth.toString(), 60f, fg, FontWeight.Light)
    Txt(now.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault()), 13f, fg)
    Txt(now.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()), 12f, fg2)
}

@Composable
private fun WeekContent(w: WidgetItem, fg: Color, fg2: Color, accent: Color) {
    val today = rememberNow().toLocalDate()
    val mon = w.get("mon", "1") == "1"
    val offset = if (mon) today.dayOfWeek.value - 1 else today.dayOfWeek.value % 7
    val start = today.minusDays(offset.toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            val isToday = d == today
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(d.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), 11f, fg2)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (isToday) accent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) { Txt(d.dayOfMonth.toString(), 13f, if (isToday) Color.White else fg) }
            }
        }
    }
}

@Composable
private fun BatteryContent(fg: Color, accent: Color) {
    val ctx = LocalContext.current
    var pct by remember { mutableIntStateOf(100) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (i != null) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0 && s > 0) pct = l * 100 / s
                val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            }
            delay(30_000)
        }
    }
    Box(Modifier.fillMaxWidth(0.8f).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.dp.toPx()
            val tl = Offset(sw / 2f, sw / 2f)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(fg.copy(alpha = 0.12f), -90f, 360f, false, tl, sz, style = Stroke(sw))
            drawArc(if (charging) accent else fg, -90f, 360f * pct / 100f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Txt("$pct%", 20f, fg, FontWeight.Medium)
    }
}

@Composable
private fun NextContent(w: WidgetItem, fg: Color, fg2: Color) {
    Txt("Next", 11f, fg2)
    Spacer(Modifier.height(18.dp))
    Txt(w.get("title").ifBlank { "Nothing yet" }, 18f, fg, FontWeight.Medium, maxLines = 2)
    Txt(w.get("time").ifBlank { "Long-press to set" }, 12f, fg2, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun CountdownContent(w: WidgetItem, fg: Color, fg2: Color, accent: Color) {
    val d = runCatching { LocalDate.parse(w.get("date")) }.getOrNull()
    val days = d?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt().coerceAtLeast(0) }
    Txt(w.get("title").ifBlank { "Exam" }, 13f, fg2)
    Spacer(Modifier.height(12.dp))
    if (days != null) {
        val s = days.toString()
        DotText(s, Modifier.height(30.dp).aspectRatio(dotCols(s) / 7f), fg, accent)
        Txt(if (days == 1) "day left" else "days left", 12f, fg2, modifier = Modifier.padding(top = 8.dp))
    } else {
        Txt("Long-press to set a date", 12f, fg2, maxLines = 2)
    }
}

@Composable
private fun TimerContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val minutes = w.get("min", "25").toIntOrNull() ?: 25
    val start = w.get("start", "0").toLongOrNull() ?: 0L
    val total = minutes * 60_000L
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val haptic = LocalHapticFeedback.current
    val left = if (start > 0L) (total - (now - start)).coerceAtLeast(0L) else total
    val done = start > 0L && left == 0L

    LaunchedEffect(start) {
        while (start > 0L) {
            now = System.currentTimeMillis()
            if (now - start >= total) break
            delay(1000)
        }
    }
    LaunchedEffect(done) { if (done) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    val secs = (left / 1000).toInt()
    val text = "%02d:%02d".format(secs / 60, secs % 60)
    Txt("Study timer", 11f, fg2)
    Spacer(Modifier.height(10.dp))
    DotText(text, Modifier.fillMaxWidth().aspectRatio(dotCols(text) / 7f), if (done) accent else fg, accent)
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        WPill(if (start > 0L) "Reset" else "Start", fg) {
            vm.setCfg(w.id, "start", if (start > 0L) "0" else System.currentTimeMillis().toString())
        }
        if (done) Txt("Done. Take a break.", 12f, accent, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun WPill(label: String, fg: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.clip(CircleShape).background(fg.copy(alpha = 0.14f)).tap {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }.padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Txt(label, 12f, fg) }
}

@Composable
private fun TasksContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val tasks = parseTasks(w.get("items"))
    var draft by remember { mutableStateOf("") }
    val add: () -> Unit = {
        val t = draft.trim()
        if (t.isNotEmpty()) {
            vm.setCfg(w.id, "items", encodeTasks(tasks + (false to t)))
            draft = ""
        }
    }
    Txt("Tasks", 11f, fg2)
    tasks.forEachIndexed { idx, t ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(20.dp).clip(CircleShape)
                    .background(if (t.first) accent else Color.Transparent)
                    .border(1.5.dp, if (t.first) accent else fg2, CircleShape)
                    .tap {
                        vm.setCfg(w.id, "items", encodeTasks(tasks.mapIndexed { i, x -> if (i == idx) (!x.first) to x.second else x }))
                    }
            )
            Spacer(Modifier.width(10.dp))
            Txt(t.second, 14f, if (t.first) fg2 else fg, modifier = Modifier.weight(1f), strike = t.first)
            Txt(
                "x", 14f, fg2,
                modifier = Modifier.tap { vm.setCfg(w.id, "items", encodeTasks(tasks.filterIndexed { i, _ -> i != idx })) }.padding(6.dp)
            )
        }
    }
    if (tasks.size < 6) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (draft.isEmpty()) Txt("Add a task", 14f, fg2)
                BasicTextField(
                    draft, { draft = it }, singleLine = true,
                    textStyle = TextStyle(color = fg, fontSize = 14.sp),
                    cursorBrush = SolidColor(fg),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Txt("Add", 13f, accent, modifier = Modifier.tap { add() }.padding(6.dp))
        }
    }
}

private val QUICK = listOf(
    "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
    "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
    "Sound" to Settings.ACTION_SOUND_SETTINGS,
    "Display" to Settings.ACTION_DISPLAY_SETTINGS,
    "Battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
    "Airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS
)

@Composable
private fun QuickContent(fg: Color) {
    val ctx = LocalContext.current
    QUICK.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (label, action) ->
                Box(
                    Modifier.weight(1f).height(38.dp).clip(CircleShape).background(fg.copy(alpha = 0.12f))
                        .tap { runCatching { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                    contentAlignment = Alignment.Center
                ) { Txt(label, 12f, fg) }
            }
        }
    }
}

private val GLYPH_MODES = listOf("Pulse", "Spiral", "Wave", "Time")

@Composable
private fun GlyphContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, accent: Color, compact: Boolean) {
    val mode = w.get("mode", "Pulse")
    Box(
        Modifier.fillMaxWidth().tap {
            vm.setCfg(w.id, "mode", GLYPH_MODES[(GLYPH_MODES.indexOf(mode) + 1).mod(GLYPH_MODES.size)])
        },
        contentAlignment = Alignment.Center
    ) {
        GlyphMatrix(mode, fg, accent, Modifier.fillMaxWidth(if (compact) 0.55f else 0.62f).aspectRatio(1f))
    }
}

/** On-screen 25 x 25 dot display inspired by Nothing's Glyph Matrix. */
@Composable
fun GlyphMatrix(mode: String, color: Color, accent: Color, modifier: Modifier) {
    val now = rememberNow()
    var t by remember { mutableFloatStateOf(0f) }
    val motion = LocalMotionScale.current
    LaunchedEffect(motion) {
        var last = -1L
        while (motion > .05f) {
            val n = withFrameNanos { it }
            if (last >= 0) t += (n - last) / 1e9f * motion
            last = n
        }
    }
    val lit = remember(mode, now.hour, now.minute) { if (mode == "Time") timeCells(now.hour, now.minute) else emptySet() }
    Canvas(modifier) {
        val n = 25
        val cell = size.width / n
        val r = cell * 0.36f
        for (y in 0 until n) for (x in 0 until n) {
            val dx = x - 12f
            val dy = y - 12f
            val d = sqrt(dx * dx + dy * dy)
            if (d > 12.6f) continue
            val b = when (mode) {
                "Spiral" -> (sin(atan2(dy, dx) * 2f + d * 0.7f - t * 3f) + 1f) / 2f
                "Wave" -> (sin(x * 0.55f + t * 2.4f) + cos(y * 0.45f - t * 1.7f) + 2f) / 4f
                "Time" -> if ((y * n + x) in lit) 1f else 0.08f
                else -> { val v = (sin(d * 0.9f - t * 3.2f) + 1f) / 2f; v * v }
            }.coerceIn(0f, 1f)
            val centre = d < 1.2f
            drawCircle(
                color = if (centre) accent else color.copy(alpha = 0.10f + 0.9f * b),
                radius = r,
                center = Offset((x + 0.5f) * cell, (y + 0.5f) * cell)
            )
        }
    }
}

private fun timeCells(h: Int, m: Int): Set<Int> {
    val out = mutableSetOf<Int>()
    val digits = listOf(h / 10, h % 10, m / 10, m % 10)
    digits.forEachIndexed { i, dgt ->
        val g = GLYPHS[('0' + dgt)] ?: return@forEachIndexed
        val row0 = if (i < 2) 4 else 14
        val col0 = 7 + (i % 2) * 6
        for (r in g.indices) for (c in g[r].indices) if (g[r][c] == '1') out.add((row0 + r) * 25 + col0 + c)
    }
    return out
}

// ---------------------------------------------------------------------------------------------
// 3D dot sphere
// ---------------------------------------------------------------------------------------------

@Composable
fun DotSphere(modifier: Modifier, color: Color, accent: Color) {
    val n = 260
    val pts = remember {
        FloatArray(n * 3).also { a ->
            for (i in 0 until n) {
                val y = 1f - 2f * (i + 0.5f) / n
                val r = sqrt(1f - y * y)
                val th = i * 2.39996f
                a[i * 3] = cos(th) * r; a[i * 3 + 1] = y; a[i * 3 + 2] = sin(th) * r
            }
        }
    }
    val xs = remember { FloatArray(n) }
    val ys = remember { FloatArray(n) }
    val zs = remember { FloatArray(n) }
    var yaw by remember { mutableFloatStateOf(0f) }
    var tilt by remember { mutableFloatStateOf(0.35f) }
    var dragging by remember { mutableStateOf(false) }
    val motion = LocalMotionScale.current

    LaunchedEffect(motion) {
        var last = -1L
        while (motion > .05f) {
            val n = withFrameNanos { it }
            if (last >= 0 && !dragging) {
                yaw += (n - last) / 1e9f * 0.5f * motion
                tilt += (0.35f - tilt) * 0.05f
            }
            last = n
        }
    }

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false }
            ) { change, drag ->
                change.consume()
                yaw += drag.x * 0.012f
                tilt = (tilt + drag.y * 0.008f).coerceIn(-0.6f, 1.2f)
            }
        }
    ) {
        val radius = min(size.width, size.height) / 2f * 0.9f
        val cy = cos(yaw); val sy = sin(yaw); val ct = cos(tilt); val st = sin(tilt)
        for (i in 0 until n) {
            val px = pts[i * 3]; val py = pts[i * 3 + 1]; val pz = pts[i * 3 + 2]
            val x1 = px * cy + pz * sy
            val z1 = -px * sy + pz * cy
            xs[i] = x1; ys[i] = py * ct - z1 * st; zs[i] = py * st + z1 * ct
        }
        val order = (0 until n).sortedBy { zs[it] }
        val c = Offset(size.width / 2f, size.height / 2f)
        for (i in order) {
            val d = (zs[i] + 1f) / 2f
            val s = 1.9f / (1.9f - zs[i] * 0.5f)
            val ring = kotlin.math.abs(ys[i]) < 0.05f
            drawCircle(
                color = if (ring) accent.copy(alpha = 0.4f + 0.6f * d) else color.copy(alpha = 0.2f + 0.8f * d),
                radius = (0.9f + 1.5f * d) * s * (radius / 100f),
                center = Offset(c.x + xs[i] * radius * s, c.y - ys[i] * radius * s)
            )
        }
    }
}
