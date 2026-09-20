package com.studentlauncher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WT
import com.studentlauncher.data.WidgetBg
import java.time.LocalDate

@Composable
private fun SLabel(text: String) {
    Txt(text, 11f, LocalColors.current.fg2, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
private fun SwitchLine(title: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(title, 14f, modifier = Modifier.weight(1f))
        GlassSwitch(on, onChange)
    }
}

/** Add, arrange and remove widgets. */
@Composable
fun WidgetCenter(vm: LauncherViewModel, onAddAndroid: (AppWidgetProviderInfo) -> Unit) {
    val c = LocalColors.current
    val pm = LocalContext.current.packageManager
    val tab = vm.widgetTab
    var showAndroid by remember { mutableStateOf(false) }
    val targetList = if (vm.widgetTargetHome) vm.homeWidgets else vm.boardWidgets
    val targetLabel = if (vm.widgetTargetHome) "Home" else "Board"

    BottomSheet({ vm.widgetCenter = false }, 0.88f) {
        Txt("Widgets", 18f, c.fg, FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Segmented(listOf(true to "Home", false to "Board"), vm.widgetTargetHome, { vm.widgetTargetHome = it })
        Spacer(Modifier.height(10.dp))
        Segmented(listOf(0 to "Add", 1 to "Yours (${targetList.size})"), tab, { vm.widgetTab = it })
        Spacer(Modifier.height(10.dp))

        if (tab == 0) {
            WT.catalog.forEach { (type, name, desc) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Txt(name, 15f, c.fg)
                        Txt(desc, 11f, c.fg2, maxLines = 2)
                    }
                    Spacer(Modifier.width(10.dp))
                    PillButton("Add", { vm.addWidget(type) })
                }
            }
            Spacer(Modifier.height(10.dp))
            PillButton(if (showAndroid) "Hide Android widgets" else "Browse Android widgets", { showAndroid = !showAndroid })
            if (showAndroid) {
                val providers = remember { vm.hostMgr.providers() }
                providers.forEach { p ->
                    val app = remember(p) {
                        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(p.provider.packageName, 0)).toString() }
                            .getOrDefault(p.provider.packageName)
                    }
                    Column(Modifier.fillMaxWidth().tap { onAddAndroid(p) }.padding(vertical = 10.dp)) {
                        Txt(p.loadLabel(pm), 15f, c.fg)
                        Txt(app, 11f, c.fg2)
                    }
                }
            }
        } else {
            if (targetList.isEmpty()) Txt("Nothing here yet in $targetLabel. Use the Add tab.", 13f, c.fg2, modifier = Modifier.padding(vertical = 12.dp))
            targetList.toList().forEach { w ->
                val name = if (w.type == WT.ANDROID) {
                    vm.hostMgr.info(w.get("appWidgetId").toIntOrNull() ?: -1)?.loadLabel(pm) ?: "Android widget"
                } else WT.title(w.type)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(name, 15f, c.fg, modifier = Modifier.weight(1f))
                    Txt("Up", 13f, c.fg2, modifier = Modifier.tap { vm.moveWidget(w.id, -1) }.padding(8.dp))
                    Txt("Down", 13f, c.fg2, modifier = Modifier.tap { vm.moveWidget(w.id, 1) }.padding(8.dp))
                    Txt("Edit", 13f, c.fg, modifier = Modifier.tap { vm.editWidgetId = w.id }.padding(8.dp))
                    Txt("Remove", 13f, c.accent, modifier = Modifier.tap { vm.removeWidget(w.id) }.padding(8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PillButton("Done", { vm.widgetCenter = false }, filled = true)
    }
}

/** Per-widget settings: background, width, colour and type specific options. */
@Composable
fun WidgetSettings(vm: LauncherViewModel, id: String) {
    val w = vm.findWidget(id) ?: return
    val c = LocalColors.current

    BottomSheet({ vm.editWidgetId = null }, 0.88f) {
        Txt(WT.title(w.type), 18f, c.fg, FontWeight.Medium)

        if (w.type == WT.ANDROID) {
            SLabel("Height")
            Segmented(listOf(0 to "Small", 1 to "Medium", 2 to "Large"), w.size, { v -> vm.updateWidget(id) { it.copy(size = v) } })
        } else {
            SLabel("Background")
            Segmented(
                listOf(WidgetBg.Glass to "Glass", WidgetBg.Solid to "Solid", WidgetBg.None to "None"),
                w.bg, { v -> vm.updateWidget(id) { it.copy(bg = v) } }
            )
            SLabel("Width")
            Segmented(listOf(false to "Full", true to "Half"), w.half, { v -> vm.updateWidget(id) { it.copy(half = v) } })
            SLabel("Text color")
            Segmented(
                listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark", "red" to "Red"),
                w.tone, { v -> vm.updateWidget(id) { it.copy(tone = v) } }
            )
        }

        when (w.type) {
            WT.CLOCK -> {
                SLabel("Style")
                Segmented(
                    listOf("Dot" to "Dot", "Digital" to "Digital", "Words" to "Words", "Analog" to "Analog"),
                    w.get("style", "Dot"), { vm.setCfg(id, "style", it) }
                )
                SLabel("Time format")
                Segmented(listOf("sys" to "System", "12" to "12 h", "24" to "24 h"), w.get("fmt", "sys"), { vm.setCfg(id, "fmt", it) })
                SwitchLine("Show date", w.get("date", "1") == "1") { vm.setCfg(id, "date", if (it) "1" else "0") }
                SwitchLine("Quote line", w.get("quote", "1") == "1") { vm.setCfg(id, "quote", if (it) "1" else "0") }
            }
            WT.NEXT -> {
                SLabel("Title")
                GlassField(w.get("title"), "What is next?", { vm.setCfg(id, "title", it) })
                SLabel("When")
                GlassField(w.get("time"), "10:30", { vm.setCfg(id, "time", it) })
            }
            WT.COUNTDOWN -> {
                SLabel("Title")
                GlassField(w.get("title"), "Exam name", { vm.setCfg(id, "title", it) })
                SLabel("Date (YYYY-MM-DD)")
                GlassField(w.get("date"), "2027-01-22", { vm.setCfg(id, "date", it) })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { d ->
                        PillButton("+$d days", { vm.setCfg(id, "date", LocalDate.now().plusDays(d.toLong()).toString()) })
                    }
                }
            }
            WT.TIMER -> {
                SLabel("Minutes")
                Segmented(
                    listOf("15" to "15", "25" to "25", "45" to "45", "60" to "60"),
                    w.get("min", "25"), { v -> vm.updateWidget(id) { it.put("min", v).put("start", "0") } }
                )
            }
            WT.TASKS -> {
                Spacer(Modifier.height(12.dp))
                PillButton("Clear completed", {
                    vm.setCfg(id, "items", encodeTasks(parseTasks(w.get("items")).filter { !it.first }))
                })
            }
            WT.WEEK -> {
                SLabel("Week starts on")
                Segmented(listOf("1" to "Monday", "0" to "Sunday"), w.get("mon", "1"), { vm.setCfg(id, "mon", it) })
            }
            WT.GLYPH -> {
                SLabel("Pattern")
                Segmented(
                    listOf("Pulse" to "Pulse", "Spiral" to "Spiral", "Wave" to "Wave", "Time" to "Time"),
                    w.get("mode", "Pulse"), { vm.setCfg(id, "mode", it) }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Up", { vm.moveWidget(id, -1) })
            PillButton("Down", { vm.moveWidget(id, 1) })
            PillButton("Remove", { vm.removeWidget(id) }, danger = true)
        }
        Spacer(Modifier.height(10.dp))
        PillButton("Done", { vm.editWidgetId = null }, filled = true)
    }
}

@Composable
fun AboutSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "2.0"
    }
    BottomSheet({ vm.aboutOpen = false }, 0.6f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(c.accent))
            Spacer(Modifier.width(10.dp))
            Txt("Student Launcher", 18f, c.fg, FontWeight.Medium)
        }
        Txt("Version $version", 12f, c.fg2, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        Txt("Developed by thats.jainam", 15f, c.fg)
        Spacer(Modifier.height(8.dp))
        Txt(
            "A calm launcher for students. Everything stays on your phone: no accounts, no ads, no tracking.",
            13f, c.fg2, maxLines = 4
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Privacy Policy", {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://jainam662010-boop.github.io/StudentLauncher/privacy.html")))
            })
            PillButton("Close", { vm.aboutOpen = false }, filled = true)
        }
    }
}
