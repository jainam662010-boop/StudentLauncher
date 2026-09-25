package com.studentlauncher.mindful.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.ui.LocalColors
import com.studentlauncher.ui.PillButton
import com.studentlauncher.ui.Txt
import kotlinx.coroutines.delay

/** Thirty seconds of guided breathing (4 s in, 6 s out). Often enough to let the urge pass. */
@Composable
internal fun BreatheStep(onBack: () -> Unit, onNotNow: () -> Unit) {
    val c = LocalColors.current
    var left by remember { mutableIntStateOf(30) }
    var inhale by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            inhale = true
            delay(4000)
            inhale = false
            delay(6000)
        }
    }
    val scale by animateFloatAsState(
        if (inhale) 1f else 0.55f,
        tween(if (inhale) 4000 else 6000, easing = FastOutSlowInEasing),
        label = "breathe"
    )

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(130.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape).background(c.accent.copy(alpha = 0.85f))
            )
        }
        Spacer(Modifier.height(10.dp))
        Txt(
            if (left > 0) (if (inhale) "Breathe in" else "Breathe out") else "Feeling better?",
            18f, c.fg, FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Txt(if (left > 0) "$left s" else "Well done.", 12f, c.fg2)
        Spacer(Modifier.height(18.dp))
        if (left == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Not now", onNotNow, filled = true)
                PillButton("Go back", onBack)
            }
        } else {
            PillButton("Skip", onBack)
        }
    }
}
