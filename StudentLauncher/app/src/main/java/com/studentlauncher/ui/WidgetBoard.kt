package com.studentlauncher.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WT
import com.studentlauncher.data.WidgetItem
import kotlin.math.roundToInt

@Composable
fun WidgetBoard(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Txt("WIDGET BOARD", 11f, c.fg2, FontWeight.Bold); Txt("Arrange your home", 20f, c.fg, FontWeight.SemiBold) }
                PillButton("Add", { vm.widgetTargetHome = false; vm.widgetTab = 0; vm.widgetCenter = true }, Modifier.semantics { contentDescription = "Add widget" })
            }
        }
        if (vm.boardWidgets.isEmpty()) item {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(c.tile), contentAlignment = Alignment.Center) {
                PillButton("Add your first widget", { vm.widgetTargetHome = false; vm.widgetCenter = true }, filled = true)
            }
        }
        items(vm.boardWidgets.toList(), key = { it.id }) { item ->
            val idx = vm.boardWidgets.indexOfFirst { it.id == item.id }
            val isDragging = idx == dragIndex
            BoardWidget(
                item = item,
                vm = vm,
                isDragging = isDragging,
                dragOffset = if (isDragging) dragOffset else 0f,
                onDragStart = { dragIndex = idx; dragOffset = 0f },
                onDrag = { delta ->
                    dragOffset += delta
                    val itemH = 80f
                    val swap = (dragOffset / itemH).roundToInt()
                    if (swap != 0) {
                        val target = (idx + swap).coerceIn(0, vm.boardWidgets.lastIndex)
                        if (target != idx) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.reorderBoardWidget(idx, target)
                            dragOffset -= swap * itemH
                            dragIndex = target
                        }
                    }
                },
                onDragEnd = { dragIndex = -1; dragOffset = 0f }
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun BoardWidget(
    item: WidgetItem,
    vm: LauncherViewModel,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val c = LocalColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize(Motion.Size)
            .graphicsLayer {
                if (isDragging) {
                    scaleY = 1.03f
                    translationY = dragOffset
                    shadowElevation = 8.dp.toPx()
                    alpha = 0.9f
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDragging) c.tile.copy(alpha = 0.85f) else c.tile)
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { _, drag -> onDrag(drag.y) }
                )
            }
            .padding(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Txt(WT.title(item.type), 13f, c.fg, FontWeight.Medium); Txt("Drag to reorder", 11f, c.fg2) }
            PillButton("↑", { vm.moveWidget(item.id, -1) }, Modifier.size(48.dp).semantics { contentDescription = "Move ${WT.title(item.type)} up" })
            PillButton("↓", { vm.moveWidget(item.id, 1) }, Modifier.size(48.dp).semantics { contentDescription = "Move ${WT.title(item.type)} down" })
            PillButton("Edit", { vm.editWidgetId = item.id }, Modifier.height(48.dp).semantics { contentDescription = "Edit ${WT.title(item.type)}" })
        }
        WidgetBody(item, vm, compact = vm.compactDensity)
    }
}
