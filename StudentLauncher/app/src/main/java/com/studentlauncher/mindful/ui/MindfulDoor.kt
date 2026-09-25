package com.studentlauncher.mindful.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.mindful.DoorRequest
import com.studentlauncher.mindful.IntentChoice
import com.studentlauncher.mindful.MindfulEngine
import com.studentlauncher.mindful.MindfulPolicy
import com.studentlauncher.ui.BottomSheet
import com.studentlauncher.ui.LocalColors
import com.studentlauncher.ui.PillButton
import com.studentlauncher.ui.Txt
import com.studentlauncher.ui.tap
import kotlinx.coroutines.delay

private enum class Step { Door, Math, Tasks, Breathe }

/**
 * The door shown when you tap a distracting app: how long you have been in it today, a wait that
 * grows with every open, a "What for?" choice, and kinder alternatives (earn time, breathe).
 * [onProceed] receives the minutes you planned, or null when you use a free pass.
 */
@Composable
fun MindfulDoor(
    vm: LauncherViewModel,
    door: DoorRequest,
    onProceed: (Int?) -> Unit,
    onCancel: () -> Unit
) {
    val engine = vm.mindful
    var step by remember { mutableStateOf(Step.Door) }
    var left by remember { mutableIntStateOf(door.plan.pauseSec) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }

    BottomSheet(onCancel, 0.92f) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(spring(0.86f, 320f)) { it / 5 * dir } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(spring(0.86f, 320f)) { -it / 5 * dir } + fadeOut(tween(120)))
            },
            label = "doorStep"
        ) { s ->
            Column(Modifier.fillMaxWidth()) {
                when (s) {
                    Step.Door -> DoorStep(door, engine, left, { step = it }, onProceed, onCancel)
                    Step.Math -> MathStep(engine) { step = Step.Door }
                    Step.Tasks -> TasksStep(engine) { step = Step.Door }
                    Step.Breathe -> BreatheStep(onBack = { step = Step.Door }, onNotNow = onCancel)
                }
            }
        }
    }
}

@Composable
private fun DoorStep(
    door: DoorRequest,
    engine: MindfulEngine,
    left: Int,
    go: (Step) -> Unit,
    onProceed: (Int?) -> Unit,
    onCancel: () -> Unit
) {
    val c = LocalColors.current
    val plan = door.plan
    val free = engine.today.balance > 0
    val waiting = left > 0 && !free

    val breath = rememberInfiniteTransition(label = "dot")
    val scale by breath.animateFloat(
        0.62f, 1f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale"
    )

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(56.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(c.accent))
        }
        Spacer(Modifier.height(14.dp))
        Txt(if (free) "Free pass: ${engine.today.balance} min left" else plan.headline, 18f, c.fg, FontWeight.Medium, align = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Txt("${door.app.label}  -  ${plan.detail}", 12f, c.fg2, maxLines = 2, align = TextAlign.Center)
        Spacer(Modifier.height(18.dp))

        when {
            free -> PillButton("Open ${door.app.label}", { onProceed(null) }, filled = true)
            plan.overBudget -> {
                if (engine.canEarn) {
                    PillButton("Earn ${MindfulPolicy.MATH_REWARD_MIN} min first", { go(Step.Math) }, filled = true)
                    Spacer(Modifier.height(10.dp))
                }
                DoorChip(if (waiting) "Open anyway in ${left}s" else "Open anyway", !waiting) {
                    onProceed(IntentChoice.Short.minutes)
                }
            }
            else -> {
                Txt(if (waiting) "What for?  (wait ${left}s)" else "What for?", 12f, c.fg2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntentChoice.values().forEach { choice ->
                        DoorChip(choice.label, !waiting) { onProceed(choice.minutes) }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Txt("Instead", 12f, c.fg2)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (engine.canEarn) {
                DoorChip("Earn time", true) { go(Step.Math) }
                DoorChip("Tick a task", true) { go(Step.Tasks) }
            }
            DoorChip("Breathe", true) { go(Step.Breathe) }
        }
        Spacer(Modifier.height(18.dp))
        PillButton("Not now", onCancel, filled = !free)
    }
}

@Composable
internal fun DoorChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(c.track)
            .then(if (enabled) Modifier.tap(onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Txt(label, 13f, c.fg) }
}
