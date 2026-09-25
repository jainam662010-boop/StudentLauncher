package com.studentlauncher.mindful.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.ui.BottomSheet
import com.studentlauncher.ui.LocalColors
import com.studentlauncher.ui.PillButton
import com.studentlauncher.ui.Segmented
import com.studentlauncher.ui.Txt

private val STEPS = listOf<Int?>(null, 0, 15, 30, 45, 60, 90)
private fun label(v: Int?) = when (v) { null -> "Global"; 0 -> "Off"; else -> "$v" }

/**
 * A daily minute budget per distracting app, on top of the global default in Mindful Mode
 * settings. Reached only through the Focus menu, so it needs the PIN the same way every other
 * distraction-blocking setting does.
 */
@Composable
fun AppLimitsSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val engine = vm.mindful
    val apps = vm.flagged.mapNotNull { vm.appFor(it) }.sortedBy { it.label.lowercase() }

    BottomSheet({ engine.appLimitsOpen = false }, 0.85f) {
        Txt("App limits", 18f, c.fg, FontWeight.Medium)
        Txt(
            "Minutes per day before the door treats an app as over budget. Global uses the default from Mindful Mode settings.",
            12f, c.fg2, maxLines = 3, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        if (apps.isEmpty()) {
            Txt("Mark an app as distracting to set a limit for it.", 13f, c.fg2, maxLines = 2)
        }
        apps.forEach { app ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Txt(app.label, 14f, c.fg, modifier = Modifier.padding(bottom = 6.dp))
                Segmented(
                    STEPS.map { it to label(it) },
                    engine.appLimitFor(app.pkg),
                    { engine.setAppLimit(app.pkg, it) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PillButton("Done", { engine.appLimitsOpen = false }, filled = true)
    }
}
