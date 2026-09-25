package com.studentlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private val DECK = listOf(
    "12 x 7" to "84", "sqrt(144)" to "12", "15% of 200" to "30",
    "48 / 6" to "8", "9 x 9" to "81", "2^10" to "1024",
    "Capital of France?" to "Paris", "Largest ocean?" to "Pacific",
    "H2O is?" to "Water", "Speed of light?" to "3x10^8 m/s",
    "Fe is?" to "Iron", "Au is?" to "Gold",
    "Spelling: neccessary" to "necessary", "Spelling: accomodate" to "accommodate",
    "Spelling: seperate" to "separate", "Spelling: recieve" to "receive",
    "1 km = ? m" to "1000", "Pi to 2 decimals?" to "3.14",
    "Boiling point of water?" to "100 C", "7 x 8" to "56"
)

@Composable
fun FlashcardSheet(onComplete: () -> Unit, onDismiss: () -> Unit) {
    val c = LocalColors.current
    var idx by remember { mutableIntStateOf(Random.nextInt(DECK.size)) }
    var flipped by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }

    BottomSheet(onDismiss, 0.55f) {
        Txt("Flashcards", 16f, c.fg, FontWeight.Medium)
        Txt("Tap to flip  2 cards to unlock", 11f, c.fg2)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp))
                .background(c.fg.copy(alpha = 0.08f))
                .clickable { flipped = !flipped },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = flipped,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "flip"
            ) { isBack ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isBack) {
                        Txt(DECK[idx].first, 22f, c.fg, FontWeight.Medium, align = TextAlign.Center)
                        Txt("tap to reveal", 11f, c.fg2)
                    } else {
                        Txt(DECK[idx].second, 22f, c.accent, FontWeight.Bold, align = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Skip", {
                idx = Random.nextInt(DECK.size); flipped = false
            })
            PillButton("Got it", {
                done++
                flipped = false
                if (done >= 2) onComplete() else idx = Random.nextInt(DECK.size)
            }, filled = true)
        }
        Txt("$done / 2", 11f, c.fg2, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun MentalMathSheet(onComplete: () -> Unit, onDismiss: () -> Unit) {
    val c = LocalColors.current
    var total by remember { mutableIntStateOf(0) }
    var a by remember { mutableIntStateOf(Random.nextInt(5, 30)) }
    var b by remember { mutableIntStateOf(Random.nextInt(5, 30)) }
    var op by remember { mutableStateOf(listOf("+", "-", "x").random()) }
    var choices by remember { mutableStateOf(generateChoices(a, b, op)) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun next() {
        a = Random.nextInt(5, 30)
        b = Random.nextInt(5, 30)
        op = listOf("+", "-", "x").random()
        choices = generateChoices(a, b, op)
        feedback = null
    }

    BottomSheet(onDismiss, 0.55f) {
        Txt("Mental Math", 16f, c.fg, FontWeight.Medium)
        Txt("5 problems to unlock", 11f, c.fg2)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp))
                .background(c.fg.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Txt("$a $op $b = ?", 32f, c.fg, FontWeight.Bold, align = TextAlign.Center)
        }

        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { choice ->
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(c.fg.copy(alpha = 0.08f))
                                .clickable {
                                    val correct = computeAnswer(a, b, op)
                                    if (choice == correct) {
                                        total++
                                        feedback = null
                                        if (total >= 5) onComplete() else next()
                                    } else {
                                        feedback = "Try again"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Txt("$choice", 18f, c.fg, FontWeight.Medium) }
                    }
                }
            }
        }
        if (feedback != null) Txt(feedback!!, 12f, Color(0xFFFF6B6B), modifier = Modifier.padding(top = 8.dp))
        Txt("$total / 5", 11f, c.fg2, modifier = Modifier.padding(top = 8.dp))
    }
}

private fun computeAnswer(a: Int, b: Int, op: String): Int = when (op) {
    "+" -> a + b; "-" -> a - b; "x" -> a * b; else -> a + b
}

private fun generateChoices(a: Int, b: Int, op: String): List<Int> {
    val correct = computeAnswer(a, b, op)
    val opts = mutableSetOf(correct)
    while (opts.size < 4) opts.add(correct + Random.nextInt(-10, 11))
    return opts.shuffled()
}

@Composable
fun BreatheSheet(onComplete: () -> Unit, onDismiss: () -> Unit) {
    val c = LocalColors.current
    val transition = rememberInfiniteTransition(label = "breathe")
    val cycle by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "cycle"
    )
    var cycles by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf("Breathe in...") }

    LaunchedEffect(cycle) {
        phase = when {
            cycle < 0.33f -> "Breathe in..."
            cycle < 0.66f -> "Hold..."
            else -> "Breathe out..."
        }
    }

    LaunchedEffect(cycle) {
        if (cycle < 0.05f && cycles > 0) {
            cycles++
            if (cycles >= 5) onComplete()
        }
    }

    val scale = when {
        cycle < 0.33f -> 0.5f + 0.5f * (cycle / 0.33f)
        cycle < 0.66f -> 1f
        else -> 1f - 0.5f * ((cycle - 0.66f) / 0.34f)
    }

    BottomSheet(onDismiss, 0.55f) {
        Txt("Breathe", 16f, c.fg, FontWeight.Medium)
        Txt("5 cycles to unlock", 11f, c.fg2)
        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size((120 * scale).dp).clip(CircleShape)
                    .background(c.accent.copy(alpha = 0.3f + 0.7f * scale)),
                contentAlignment = Alignment.Center
            ) {
                Txt(phase, 14f, c.fg, FontWeight.Medium, align = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(16.dp))
        Txt("$cycles / 5 cycles", 12f, c.fg2, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}
