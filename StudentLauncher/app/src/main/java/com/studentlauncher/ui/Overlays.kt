package com.studentlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AppInfo
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class LaunchAnim(val app: AppInfo, val rect: Rect)

/** Transparent tap-catcher (menus, context menu). */
@Composable
fun Scrim(onTap: () -> Unit) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onTap() } })
}

/** Dimming tap-catcher whose alpha follows an enter animation without recomposing. */
@Composable
fun FadeScrim(onTap: () -> Unit, progress: Float, max: Float = 0.22f) {
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = progress.coerceIn(0f, 1f) * max }
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onTap() } }
    )
}

/** Bottom sheet used by widgets, about and pause sheets. Springs up, dims the home screen. */
@Composable
fun BottomSheet(onDismiss: () -> Unit, maxHeightFrac: Float = 0.82f, content: @Composable ColumnScope.() -> Unit) {
    val visible = remember { MutableTransitionState(true) }
    LaunchedEffect(visible.targetState) { if (!visible.targetState) onDismiss() }
    val e by animateFloatAsState(if (visible.targetState) 1f else 0f, Motion.Macro, label = "bs")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxH = maxHeight * maxHeightFrac
        FadeScrim({ visible.targetState = false }, e)
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(14.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .graphicsLayer { translationY = (1f - e) * size.height * 1.15f }
                    .glass(32.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun ContextMenu(vm: LauncherViewModel, app: AppInfo, anchor: Rect, screenW: Dp, topLimit: Float) {
    val visible = remember { MutableTransitionState(true) }
    LaunchedEffect(visible.targetState) { if (!visible.targetState) vm.ctxApp = null }
    val e by animateFloatAsState(if (visible.targetState) 1f else 0f, Motion.Macro, label = "ctx")
    val density = LocalDensity.current
    var h by remember { mutableIntStateOf(0) }
    val wPx = with(density) { 216.dp.toPx() }
    val margin = with(density) { 12.dp.toPx() }
    val gap = with(density) { 8.dp.toPx() }
    val x = anchor.left.coerceIn(margin, with(density) { screenW.toPx() } - wPx - margin)
    val above = anchor.top - h - gap
    val y = if (above >= topLimit) above else anchor.bottom + gap
    val originY = if (above >= topLimit) 1f else 0f

    Column(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(216.dp)
            .graphicsLayer {
                val s = 0.82f + 0.18f * e
                scaleX = s; scaleY = s
                alpha = e.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.2f, originY)
            }
            .onSizeChanged { h = it.height }
            .glass(22.dp)
            .padding(6.dp)
    ) {
        val pinned = app.pkg in vm.pinned
        val docked = app.pkg in vm.dock
        CtxItem(if (pinned) "Remove from home" else "Pin to home") { visible.targetState = false; vm.togglePin(app.pkg) }
        CtxItem(if (docked) "Remove from dock" else "Add to dock") { visible.targetState = false; vm.toggleDock(app.pkg) }
        CtxItem(if (app.pkg in vm.flagged) "Not distracting" else "Mark as distracting") { visible.targetState = false; vm.toggleFlag(app.pkg) }
        CtxItem(if (app.pkg in vm.studyApps) "Not a study app" else "Mark as study app") { visible.targetState = false; vm.toggleStudyApp(app.pkg) }
        CtxItem("App info") { visible.targetState = false; vm.openAppInfo(app.pkg) }
    }
}

@Composable
private fun CtxItem(label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp)).tap {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) { Txt(label, 14f) }
}

@Composable
fun PauseSheet(vm: LauncherViewModel, app: AppInfo, onCancel: () -> Unit, onOpen: () -> Unit) {
    val c = LocalColors.current
    var left by remember { mutableIntStateOf(vm.pauseSec) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
    val breath = rememberInfiniteTransition(label = "breath")
    val motion = LocalMotionScale.current
    val bs by breath.animateFloat(
        0.62f, 1f,
        infiniteRepeatable(tween((4000 * motion).coerceAtLeast(1f).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bs"
    )
    BottomSheet(onCancel, 0.6f) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).graphicsLayer { scaleX = bs; scaleY = bs }.clip(CircleShape).background(c.accent))
            }
            Spacer(Modifier.height(14.dp))
            Txt("Opening ${app.label}", 17f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Txt("Is this what you want to do right now?", 13f, c.fg2)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Not now", onCancel)
                val ready = left == 0
                PillButton(if (ready) "Open" else "Open in $left", { if (ready) onOpen() }, filled = ready)
            }
        }
    }
}

@Composable
fun ToastPill(text: String) {
    val c = LocalColors.current
    Box(Modifier.glass(20.dp).padding(horizontal = 18.dp, vertical = 10.dp)) { Txt(text, 13f, c.fg) }
}

/** The tapped icon grows into a full-screen sheet (iPhone-style zoom), then collapses on return. */
@Composable
fun LaunchOverlay(anim: LaunchAnim?, dark: Boolean) {
    val p = animateFloatAsState(if (anim != null) 1f else 0f, Motion.Macro, label = "launch")
    var last by remember { mutableStateOf<LaunchAnim?>(null) }
    LaunchedEffect(anim) { if (anim != null) last = anim }
    val cur = anim ?: last
    if (cur != null) {
        val tint = if (dark) Color(0xFF0B0B0C) else Color(0xFFF5F5F7)
        Canvas(Modifier.fillMaxSize()) {
            val f = p.value
            if (f > 0.002f) {
                val r = cur.rect
                val l = lerp(r.left, 0f, f)
                val t = lerp(r.top, 0f, f)
                val rr = lerp(r.right, size.width, f)
                val b = lerp(r.bottom, size.height, f)
                val cr = lerp(r.width * 0.3f, 44.dp.toPx(), f)
                drawRoundRect(
                    color = tint.copy(alpha = 0.55f + 0.45f * f),
                    topLeft = Offset(l, t),
                    size = Size(rr - l, b - t),
                    cornerRadius = CornerRadius(cr)
                )
            }
        }
    }
}
