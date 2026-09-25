package com.studentlauncher.mindful.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.mindful.MindfulEngine
import com.studentlauncher.mindful.Window
import com.studentlauncher.ui.BottomSheet
import com.studentlauncher.ui.GlassSwitch
import com.studentlauncher.ui.LocalColors
import com.studentlauncher.ui.PillButton
import com.studentlauncher.ui.Segmented
import com.studentlauncher.ui.Txt
import com.studentlauncher.ui.tap
import kotlinx.coroutines.delay

/** Keeps schedules accurate: refreshes the clock every 30 seconds while the launcher is open. */
@Composable
fun MindfulTicker(engine: MindfulEngine) {
    LaunchedEffect(engine) {
        while (true) {
            engine.tick()
            delay(30_000)
        }
    }
}

@Composable
fun MindfulSettingsSheet(engine: MindfulEngine) {
    val c = LocalColors.current
    val s = engine.settings

    BottomSheet({ engine.settingsOpen = false }, 0.92f) {
        Txt("Mindful Mode", 18f, c.fg, FontWeight.Medium)
        Txt("Gentle friction so distracting apps stay a choice.", 12f, c.fg2, maxLines = 2, modifier = Modifier.padding(top = 2.dp))

        Heading("Daily budget per app (minutes)")
        Segmented(
            listOf(0 to "Off", 15 to "15", 30 to "30", 45 to "45", 60 to "60", 90 to "90"),
            s.budgetMin, { v -> engine.update { it.copy(budgetMin = v) } }
        )
        ToggleRow("Exam mode", "Halves budgets and adds friction when an exam is within 14 days.", s.examMode) { v ->
            engine.update { it.copy(examMode = v) }
        }

        Heading("Free minutes you can earn per day")
        Segmented(
            listOf(0 to "Off", 20 to "20", 30 to "30", 60 to "60"),
            s.earnCapMin, { v -> engine.update { it.copy(earnCapMin = v) } }
        )

        WindowEditor("Study hours", "Hides distracting apps automatically.", s.studyHours) { w ->
            engine.update { it.copy(studyHours = w) }
        }
        WindowEditor("Bedtime", "Hides distracting apps overnight.", s.bedtime) { w ->
            engine.update { it.copy(bedtime = w) }
        }

        Spacer(Modifier.height(18.dp))
        PillButton("Done", { engine.settingsOpen = false }, filled = true)
    }
}

@Composable
private fun Heading(text: String) {
    Txt(text, 11f, LocalColors.current.fg2, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalColors.current
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(title, 14f, c.fg)
            Txt(sub, 11f, c.fg2, maxLines = 2)
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(on, onChange)
    }
}

@Composable
private fun WindowEditor(title: String, sub: String, w: Window, onChange: (Window) -> Unit) {
    Column(Modifier.padding(top = 6.dp)) {
        ToggleRow(title, sub, w.enabled) { onChange(w.copy(enabled = it)) }
        if (w.enabled) {
            TimeStepper("From", w.startMin) { onChange(w.copy(startMin = it)) }
            TimeStepper("Until", w.endMin) { onChange(w.copy(endMin = it)) }
        }
    }
}

@Composable
private fun TimeStepper(label: String, minutes: Int, onChange: (Int) -> Unit) {
    val c = LocalColors.current
    val step = 30
    fun wrap(m: Int) = ((m % 1440) + 1440) % 1440
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 13f, c.fg2, modifier = Modifier.weight(1f))
        Txt("-", 24f, c.fg, modifier = Modifier.tap { onChange(wrap(minutes - step)) }.padding(horizontal = 14.dp))
        Txt("%02d:%02d".format(minutes / 60, minutes % 60), 15f, c.fg)
        Txt("+", 24f, c.fg, modifier = Modifier.tap { onChange(wrap(minutes + step)) }.padding(horizontal = 14.dp))
    }
}
