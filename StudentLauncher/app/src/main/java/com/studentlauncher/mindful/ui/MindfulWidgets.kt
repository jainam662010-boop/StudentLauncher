package com.studentlauncher.mindful.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.mindful.MindfulPolicy
import com.studentlauncher.ui.Txt
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

/** Widget: minutes on distracting apps today against the budget, top apps, and schedule state. */
@Composable
fun MindfulTodayContent(vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val e = vm.mindful
    val t = e.today
    val budget = e.budgetMin()
    val over = budget > 0 && t.totalMinutes >= budget
    val frac by animateFloatAsState(
        if (budget > 0) (t.totalMinutes.toFloat() / budget).coerceIn(0f, 1f) else 0f,
        spring(dampingRatio = 0.85f, stiffness = 260f), label = "budget"
    )

    Txt("Mindful today", 11f, fg2)
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
        Txt("${t.totalMinutes}", 40f, if (over) accent else fg, FontWeight.Light)
        Txt(if (budget > 0) "  / $budget min per app" else "  min", 12f, fg2, modifier = Modifier.padding(bottom = 9.dp))
    }
    if (budget > 0) {
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(fg.copy(alpha = 0.12f))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(if (over) accent else fg))
        }
    }
    Spacer(Modifier.height(10.dp))
    val top = t.minutes.entries.sortedByDescending { it.value }.take(3)
    if (top.isEmpty()) {
        Txt("Nothing yet. Nice.", 13f, fg2)
    }
    top.forEach { (pkg, m) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Txt(vm.appFor(pkg)?.label ?: pkg, 13f, fg, modifier = Modifier.weight(1f))
            Txt("$m min", 12f, fg2)
        }
    }
    Spacer(Modifier.height(8.dp))
    Txt(
        buildString {
            append("Said not now ${t.refusals}x")
            if (t.balance > 0) append("  -  free pass ${t.balance} min")
        },
        12f, fg2, maxLines = 2
    )
    when {
        e.bedtimeActive -> Txt("Bedtime mode is on", 12f, accent, modifier = Modifier.padding(top = 4.dp))
        e.studyHoursActive -> Txt("Study hours are on", 12f, accent, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Widget: seven days of minutes plus what saying "Not now" saved. Positive numbers only. */
@Composable
fun MindfulWeekContent(vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val e = vm.mindful
    val week = e.week()
    val budget = e.budgetMin()
    val peak = max(max(budget, week.maxOf { it.totalMinutes }), 1)
    val nos = week.sumOf { it.refusals }
    val saved = MindfulPolicy.savedMinutes(nos, e.averageSessionMin())
    val quiet = MindfulPolicy.underBudgetDays(week, budget)

    Txt("Mindful week", 11f, fg2)
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.fillMaxWidth().height(84.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEachIndexed { i, d ->
            val h by animateFloatAsState(
                (d.totalMinutes.toFloat() / peak).coerceIn(0.04f, 1f),
                spring(dampingRatio = 0.7f, stiffness = 300f), label = "bar"
            )
            val isToday = i == week.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(64.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.size(width = 14.dp, height = (64f * h).dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isToday) accent else fg.copy(alpha = 0.55f))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Txt(LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), 11f, fg2)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Txt("Said not now $nos times, about $saved min saved", 12f, fg, maxLines = 2)
    if (budget > 0) Txt("Under budget on $quiet of 7 days", 12f, fg2, modifier = Modifier.padding(top = 2.dp))
}
