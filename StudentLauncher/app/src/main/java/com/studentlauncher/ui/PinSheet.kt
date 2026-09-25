package com.studentlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.security.PinPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PinMode { Verify, Create }

/** [onSuccess] runs after the PIN was verified (Verify) or saved (Create). */
class PinRequest(val title: String, val mode: PinMode, val onSuccess: () -> Unit)

@Composable
fun PinSheet(vm: LauncherViewModel, req: PinRequest) {
    val c = LocalColors.current
    val pin = vm.pin
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var lockLeft by remember { mutableIntStateOf(pin.lockedForSec()) }
    var resetLeft by remember { mutableStateOf(pin.resetRemainingMs()) }
    val shake = remember { Animatable(0f) }

    // Keeps the lockout and the "forgot PIN" countdown live, and finishes a due reset.
    LaunchedEffect(Unit) {
        while (true) {
            lockLeft = pin.lockedForSec()
            resetLeft = pin.resetRemainingMs()
            if (req.mode == PinMode.Verify && pin.finishResetIfDue()) {
                vm.onPinChanged()
                vm.pinRequest = null
                req.onSuccess()
                break
            }
            delay(1000)
        }
    }

    fun fail(text: String) {
        message = text
        code = ""
        scope.launch {
            repeat(3) {
                shake.animateTo(14f, tween(45))
                shake.animateTo(-14f, tween(45))
            }
            shake.animateTo(0f, tween(60))
        }
    }

    val submit: () -> Unit = {
        when (req.mode) {
            PinMode.Create -> {
                val f = first
                if (f == null) {
                    if (PinPolicy.isValid(code)) {
                        first = code
                        code = ""
                        message = "Enter it again to confirm"
                    } else {
                        fail("Use ${PinPolicy.MIN_LEN} to ${PinPolicy.MAX_LEN} digits")
                    }
                } else if (f == code) {
                    pin.set(code)
                    vm.onPinChanged()
                    vm.pinRequest = null
                    req.onSuccess()
                } else {
                    first = null
                    fail("The PINs did not match. Start again.")
                }
            }
            PinMode.Verify -> {
                if (lockLeft == 0) {
                    if (pin.verify(code)) {
                        vm.pinRequest = null
                        req.onSuccess()
                    } else {
                        lockLeft = pin.lockedForSec()
                        fail(if (lockLeft > 0) "Too many tries" else "Wrong PIN")
                    }
                }
            }
        }
    }

    BottomSheet({ vm.pinRequest = null }, 0.9f) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(req.title, 18f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Txt(
                when {
                    lockLeft > 0 -> "Try again in ${lockLeft}s"
                    message != null -> message.orEmpty()
                    req.mode == PinMode.Create -> "Choose ${PinPolicy.MIN_LEN} to ${PinPolicy.MAX_LEN} digits"
                    else -> "Enter your PIN"
                },
                12f, if (lockLeft > 0 || message?.startsWith("Wrong") == true) c.accent else c.fg2,
                maxLines = 2, align = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.graphicsLayer { translationX = shake.value * density },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(PinPolicy.MAX_LEN) { i ->
                    val on = i < code.length
                    Box(
                        Modifier.size(14.dp).border(1.5.dp, if (on) c.fg else c.fg2, CircleShape)
                            .background(if (on) c.fg else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            NumberPad(
                onDigit = { if (lockLeft == 0 && code.length < PinPolicy.MAX_LEN) code += it },
                onBack = { code = code.dropLast(1) },
                onOk = { if (code.isNotEmpty()) submit() }
            )
            Spacer(Modifier.height(14.dp))
            if (req.mode == PinMode.Verify) {
                val left = resetLeft
                if (left == null) {
                    Txt("Forgot PIN?", 12f, c.fg2, modifier = Modifier.tap { pin.startReset(); resetLeft = pin.resetRemainingMs() }.padding(8.dp))
                } else {
                    val h = (left / 3_600_000L) + 1
                    Txt("PIN can be removed in about $h h", 12f, c.fg2)
                    Txt("Cancel reset", 12f, c.accent, modifier = Modifier.tap { pin.cancelReset(); resetLeft = null }.padding(8.dp))
                }
            }
            PillButton("Cancel", { vm.pinRequest = null })
        }
    }
}
