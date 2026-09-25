package com.studentlauncher.mindful.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentlauncher.mindful.MindfulEngine
import com.studentlauncher.mindful.MindfulPolicy
import com.studentlauncher.ui.LocalColors
import com.studentlauncher.ui.NumberPad
import com.studentlauncher.ui.PillButton
import com.studentlauncher.ui.Txt
import com.studentlauncher.ui.tap
import kotlinx.coroutines.delay

/** "Earn time" with five quick mental-math questions. 4 out of 5 earns free minutes. */
@Composable
internal fun MathStep(engine: MindfulEngine, onDone: () -> Unit) {
    val c = LocalColors.current
    val level = remember { 1 + (engine.today.earned / 20).coerceAtMost(2) }
    val questions = remember { MindfulPolicy.mathQuestions(System.nanoTime(), 5, level) }
    var idx by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var correct by remember { mutableIntStateOf(0) }
    var granted by remember { mutableStateOf<Int?>(null) }
    var flash by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(350)
            flash = null
        }
    }
    val box by animateColorAsState(
        when (flash) {
            true -> c.fg.copy(alpha = 0.25f)
            false -> c.accent.copy(alpha = 0.35f)
            null -> c.track
        },
        label = "flash"
    )

    val result = granted
    if (result != null) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(if (result > 0) "+$result min earned" else "Not quite. Try again later.", 20f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Txt("$correct of ${questions.size} correct", 13f, c.fg2)
            Spacer(Modifier.height(20.dp))
            PillButton("Continue", onDone, filled = true)
        }
        return
    }

    val submit: () -> Unit = {
        val ok = input.toIntOrNull() == questions[idx].answer
        if (ok) correct++
        flash = ok
        input = ""
        if (idx == questions.lastIndex) {
            granted = engine.credit(MindfulPolicy.earnedFor(correct, questions.size))
        } else {
            idx++
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Txt("Question ${idx + 1} of ${questions.size}", 12f, c.fg2)
        Spacer(Modifier.height(10.dp))
        Txt("${questions[idx].text} = ?", 32f, c.fg, FontWeight.Light)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(box).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) { Txt(input.ifEmpty { " " }, 24f, c.fg, align = TextAlign.Center) }
        Spacer(Modifier.height(12.dp))
        NumberPad(
            onDigit = { if (input.length < 4) input += it },
            onBack = { input = input.dropLast(1) },
            onOk = { if (input.isNotEmpty()) submit() }
        )
        Spacer(Modifier.height(12.dp))
        PillButton("Back", onDone)
    }
}

/** "Tick a task": finishing something real from your Tasks widget earns a few free minutes. */
@Composable
internal fun TasksStep(engine: MindfulEngine, onDone: () -> Unit) {
    val c = LocalColors.current
    val tasks = remember { engine.openTasks().toMutableStateList() }
    Column(Modifier.fillMaxWidth()) {
        Txt("Tick a task", 18f, c.fg, FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Txt(
            if (engine.canEarn) "Each finished task earns ${MindfulPolicy.TASK_REWARD_MIN} free minutes." else "You reached today's earning limit.",
            12f, c.fg2, maxLines = 2
        )
        Spacer(Modifier.height(12.dp))
        if (tasks.isEmpty()) {
            Txt("No open tasks. Add some with the Tasks widget.", 13f, c.fg2, maxLines = 2)
        }
        tasks.toList().forEach { ref ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(ref.text, 15f, c.fg, modifier = Modifier.weight(1f))
                PillButton("Done", {
                    engine.completeTask(ref)
                    engine.credit(MindfulPolicy.TASK_REWARD_MIN)
                    tasks.remove(ref)
                })
            }
        }
        Spacer(Modifier.height(16.dp))
        PillButton("Back", onDone, filled = true)
    }
}
