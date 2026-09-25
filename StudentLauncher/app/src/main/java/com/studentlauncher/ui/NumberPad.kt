package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** 3 x 4 number pad: digits, backspace ("<") and confirm ("OK"). */
@Composable
fun NumberPad(onDigit: (String) -> Unit, onBack: () -> Unit, onOk: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("<", "0", "OK"))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    NumberKey(key, Modifier.weight(1f)) {
                        when (key) {
                            "<" -> onBack()
                            "OK" -> onOk()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (label == "OK") c.fg else c.track).tap(onClick),
        contentAlignment = Alignment.Center
    ) { Txt(label, 20f, if (label == "OK") c.knobOn else c.fg) }
}
