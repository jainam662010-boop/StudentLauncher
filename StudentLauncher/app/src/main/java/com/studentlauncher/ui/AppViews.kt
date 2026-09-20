package com.studentlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AlphaSide
import com.studentlauncher.data.AppInfo
import com.studentlauncher.data.Chip
import com.studentlauncher.data.IconShape
import com.studentlauncher.data.IconStyle
import kotlin.math.abs
import kotlin.math.max

enum class EntryMode { List, Grid, Panel, Dock }

private val GRAY = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun AppIconView(app: AppInfo, vm: LauncherViewModel, size: Dp, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val ic = vm.icons[app.pkg]
    val shape: Shape = when (vm.iconShape) {
        IconShape.Circle -> CircleShape
        IconShape.Squircle -> RoundedCornerShape(size * 0.32f)
        IconShape.None -> RectangleShape
    }
    val container = if (vm.iconShape == IconShape.None) Modifier else Modifier
        .clip(shape)
        .background(c.tile)
        .border(
            0.8.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = if (c.dark) 0.35f else 0.9f), Color.White.copy(alpha = 0.08f))),
            shape
        )
    Box(modifier.size(size).then(container), contentAlignment = Alignment.Center) {
        if (ic != null) {
            val mono = vm.iconStyle == IconStyle.Mono
            val pad = if (vm.iconShape == IconShape.None) 0.dp else size * 0.2f
            when {
                mono && ic.mono != null ->
                    Image(ic.mono, null, Modifier.fillMaxSize().padding(pad), colorFilter = ColorFilter.tint(c.fg))
                mono ->
                    Image(ic.original, null, Modifier.fillMaxSize().padding(pad).clip(RoundedCornerShape(size * 0.24f)), colorFilter = GRAY)
                else ->
                    Image(ic.original, null, Modifier.fillMaxSize().padding(pad))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppEntry(
    app: AppInfo,
    vm: LauncherViewModel,
    mode: EntryMode,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "press")
    var rect by remember { mutableStateOf(Rect.Zero) }

    val gestures = Modifier.combinedClickable(
        interactionSource = src,
        indication = null,
        onClick = { onLaunch(app, rect) },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onMenu(app, rect)
        }
    )
    val press = Modifier.graphicsLayer { scaleX = s; scaleY = s }
    val iconPos = Modifier.onGloballyPositioned { rect = it.boundsInRoot() }
    val flagged = app.pkg in vm.flagged

    when (mode) {
        EntryMode.List, EntryMode.Panel -> Row(
            modifier.fillMaxWidth().then(press).then(gestures).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(app, vm, 42.dp, iconPos)
            if (vm.showNames || mode == EntryMode.Panel) {
                Spacer(Modifier.width(14.dp))
                Txt(app.label, 20f, c.fg, modifier = Modifier.weight(1f, fill = false))
                if (flagged) Box(Modifier.padding(start = 8.dp).size(6.dp).background(c.accent, CircleShape))
                if (mode == EntryMode.Panel && app.pkg in vm.studyApps) {
                    Txt("study", 11f, c.fg2, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        EntryMode.Grid -> Column(
            modifier.then(press).then(gestures).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIconView(app, vm, 56.dp, iconPos)
            if (vm.showNames) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(app.label, 11.5f, c.fg, modifier = Modifier.weight(1f, fill = false))
                    if (flagged) Box(Modifier.padding(start = 4.dp).size(5.dp).background(c.accent, CircleShape))
                }
            }
        }
        EntryMode.Dock -> Box(modifier.then(press).then(gestures)) { AppIconView(app, vm, 48.dp, iconPos) }
    }
}

// ---------------------------------------------------------------------------------------------
// Home favourites
// ---------------------------------------------------------------------------------------------

@Composable
fun HomeApps(
    vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vm.layout == com.studentlauncher.data.HomeLayout.Grid) {
        val list = vm.pinned.mapNotNull { vm.appFor(it) }.filter { vm.isVisible(it) }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.chunked(4).forEach { rowApps ->
                Row(Modifier.fillMaxWidth()) {
                    rowApps.forEach { a ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AppEntry(a, vm, EntryMode.Grid, onLaunch, onMenu)
                        }
                    }
                    repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        Column(modifier) {
            vm.pinned.toList().forEach { pkg ->
                val a = vm.appFor(pkg)
                if (a != null) {
                    androidx.compose.runtime.key(pkg) {
                        AnimatedVisibility(
                            visible = vm.isVisible(a),
                            enter = fadeIn(spring(stiffness = 300f)) + expandVertically(spring(dampingRatio = 0.8f, stiffness = 320f)),
                            exit = fadeOut() + shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 400f))
                        ) { AppEntry(a, vm, EntryMode.List, onLaunch, onMenu) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Alphabet scrubber (star = home, A-Z = jump, circle = settings)
// ---------------------------------------------------------------------------------------------

private val LETTERS: List<String> = listOf("*") + ('A'..'Z').map { it.toString() } + listOf("o")

@Composable
fun AlphaScrubber(vm: LauncherViewModel, letterH: Dp, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    var active by remember { mutableIntStateOf(-1) }
    var heightPx by remember { mutableIntStateOf(1) }
    val available = vm.availableLetters()
    val right = vm.alphaSide == AlphaSide.Right
    val fontSize = (letterH.value * 0.78f).coerceIn(9f, 12f)

    fun indexAt(y: Float) = (y / max(1, heightPx) * 28f).toInt().coerceIn(0, 27)

    Column(
        modifier
            .width(30.dp)
            .onSizeChanged { heightPx = it.height }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var i = indexAt(down.position.y)
                    active = i
                    vm.onScrub(i)
                    do {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.first()
                        val ni = indexAt(ch.position.y)
                        if (ni != i) {
                            i = ni
                            active = i
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.onScrub(i)
                        }
                        ch.consume()
                    } while (ev.changes.any { it.pressed })
                    vm.onScrubRelease(i)
                    active = -1
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LETTERS.forEachIndexed { idx, label ->
            val d = if (active < 0) 99 else abs(idx - active)
            val off = animateFloatAsState(
                if (active < 0) 0f else max(0f, 26f - d * 7f),
                spring(dampingRatio = 0.6f, stiffness = 500f), label = "wave"
            )
            val sc = animateFloatAsState(
                if (active < 0) 1f else 1f + max(0f, 0.55f - d * 0.16f),
                spring(dampingRatio = 0.6f, stiffness = 500f), label = "waveScale"
            )
            val dim = idx in 1..26 && ('A' + (idx - 1)) !in available
            Box(
                Modifier.height(letterH).width(30.dp).graphicsLayer {
                    translationX = (if (right) -off.value else off.value) * density
                    scaleX = sc.value; scaleY = sc.value
                },
                contentAlignment = Alignment.Center
            ) {
                when (idx) {
                    0 -> Txt("\u2605", fontSize, if (d == 0) c.fg else c.fg2)
                    27 -> Box(Modifier.size(7.dp).border(1.2.dp, if (d == 0) c.fg else c.fg2, CircleShape))
                    else -> Txt(label, fontSize, (if (d == 0) c.fg else c.fg2).copy(alpha = if (dim) 0.3f else 1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// All-apps panel (apps that are not on home)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AllAppsPanel(
    vm: LauncherViewModel,
    modifier: Modifier,
    startPad: Dp,
    endPad: Dp,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit
) {
    val c = LocalColors.current
    val searching = vm.query.isNotBlank()
    val key = if (searching) "q" else vm.letter.toString()

    Column(
        modifier.fillMaxWidth().imePadding().padding(start = startPad, end = endPad, bottom = 8.dp)
    ) {
        Segmented(listOf(Chip.All to "All", Chip.Study to "Study"), vm.chip, { vm.chip = it }, Modifier.width(170.dp))
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = key,
            transitionSpec = {
                val d = vm.letterDir
                (slideInVertically(spring(0.8f, 380f)) { it * d / 4 } + fadeIn(spring(stiffness = 500f))) togetherWith
                    (slideOutVertically(spring(0.8f, 380f)) { -it * d / 4 } + fadeOut(spring(stiffness = 700f)))
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "panel"
        ) { k ->
            val results = vm.appsForKey(k)
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(64.dp)) {
                    if (k != "q") Txt(k, 56f, c.fg, FontWeight.Light)
                }
                if (results.isEmpty()) {
                    Txt(
                        if (k == "q") "No matches" else "No apps under $k",
                        14f, c.fg2, modifier = Modifier.padding(top = 18.dp)
                    )
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(results, key = { it.pkg }) { a ->
                            AppEntry(a, vm, EntryMode.Panel, onLaunch, onMenu)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SearchField(vm) {
            vm.appsForKey("q").firstOrNull()?.let { onLaunch(it, Rect(Offset.Zero, Offset.Zero)) }
        }
    }
}

@Composable
private fun SearchField(vm: LauncherViewModel, onGo: () -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier.fillMaxWidth().height(46.dp).glass(23.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(16.dp)) {
            val s = Stroke(1.6.dp.toPx())
            drawCircle(c.fg2, radius = size.width * 0.34f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = s)
            drawLine(c.fg2, Offset(size.width * 0.68f, size.height * 0.68f), Offset(size.width * 0.95f, size.height * 0.95f), strokeWidth = 1.6.dp.toPx())
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (vm.query.isEmpty()) Txt("Search apps", 15f, c.fg2)
            BasicTextField(
                value = vm.query,
                onValueChange = { vm.query = it; if (it.isNotEmpty()) vm.view = com.studentlauncher.data.ViewMode.All },
                singleLine = true,
                textStyle = TextStyle(color = c.fg, fontSize = 15.sp),
                cursorBrush = SolidColor(c.fg),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onGo() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dock
// ---------------------------------------------------------------------------------------------

@Composable
fun Dock(
    vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val list = vm.dock.mapNotNull { vm.appFor(it) }.filter { vm.isVisible(it) }
    Row(
        modifier.fillMaxWidth().height(68.dp).glass(34.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        list.forEach { AppEntry(it, vm, EntryMode.Dock, onLaunch, onMenu) }
    }
}

@Composable
fun DockRail(
    vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val list = vm.dock.mapNotNull { vm.appFor(it) }.filter { vm.isVisible(it) }
    if (list.isEmpty()) return
    Column(
        modifier.width(58.dp).glass(29.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(5.dp).background(if (i == 0) c.accent else c.fg2.copy(alpha = 0.5f), CircleShape))
            }
        }
        list.forEach { AppEntry(it, vm, EntryMode.Dock, onLaunch, onMenu) }
    }
}
