package com.studentlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel

// ponytail: Phase 1 swipe shell placeholder; real widgets move here in Phase 3.
@Composable
fun WidgetBoard(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(4) }) {
            Box(
                Modifier.fillMaxWidth().height(64.dp).glass(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Txt("Widget Board", size = 15f, color = c.fg, weight = FontWeight.SemiBold)
            }
        }
        items(6) { i ->
            Box(
                Modifier.fillMaxWidth().height(if (i % 3 == 0) 140.dp else 100.dp).glass(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Txt("W${i + 1}", size = 13f, color = c.fg2, weight = FontWeight.Medium)
            }
        }
    }
}
