package com.studentlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AlphaSide
import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.HomeLayout
import com.studentlauncher.data.IconShape
import com.studentlauncher.data.IconStyle
import com.studentlauncher.data.MenuType
import com.studentlauncher.data.ThemeMode
import java.time.format.DateTimeFormatter
import java.util.Locale

val MENU_H = 36.dp

/** macOS-style menu bar: glass strip under the status icons with three pull-down menus. */
@Composable
fun MenuBar(vm: LauncherViewModel, onAnchor: (MenuType, Float) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val now = rememberNow()
    Box(modifier.fillMaxWidth().glass(0.dp)) {
        Row(
            Modifier.statusBarsPadding().height(MENU_H).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(c.accent))
            Spacer(Modifier.width(18.dp))
            MenuTab("Focus", vm.menu == MenuType.Focus, { vm.toggleMenu(MenuType.Focus) }) { onAnchor(MenuType.Focus, it) }
            Spacer(Modifier.width(8.dp))
            MenuTab("Widgets", vm.menu == MenuType.Widgets, { vm.toggleMenu(MenuType.Widgets) }) { onAnchor(MenuType.Widgets, it) }
            Spacer(Modifier.width(8.dp))
            MenuTab("Style", vm.menu == MenuType.Style, { vm.toggleMenu(MenuType.Style) }) { onAnchor(MenuType.Style, it) }
            Spacer(Modifier.weight(1f))
            if (vm.studyMode) {
                Txt("Study on", 11f, c.accent)
                Spacer(Modifier.width(10.dp))
            }
            Txt(now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())), 12f, c.fg2)
        }
    }
}

@Composable
private fun MenuTab(label: String, selected: Boolean, onClick: () -> Unit, onX: (Float) -> Unit) {
    val c = LocalColors.current
    val bg by animateColorAsState(if (selected) c.tile else Color.Transparent, spring(stiffness = 500f), label = "tab")
    Box(
        Modifier
            .onGloballyPositioned { onX(it.positionInRoot().x) }
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .tap(onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) { Txt(label, 13f, c.fg, FontWeight.Medium) }
}

@Composable
fun Dropdown(
    visible: Boolean,
    x: Float,
    screenW: Dp,
    maxH: Dp,
    top: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val w = minOf(288.dp, screenW - 16.dp)
    val xDp = with(density) { x.toDp() }.coerceIn(8.dp, screenW - w - 8.dp)
    val origin = TransformOrigin(0.12f, 0f)
    AnimatedVisibility(
        visible,
        Modifier.offset(x = xDp, y = top),
        enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = 420f), initialScale = 0.82f, transformOrigin = origin) + fadeIn(tween(160)),
        exit = scaleOut(tween(150), targetScale = 0.9f, transformOrigin = origin) + fadeOut(tween(120))
    ) {
        Column(
            Modifier.width(w).heightIn(max = maxH).glass(24.dp).verticalScroll(rememberScrollState()).padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun SwitchRow(title: String, sub: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(title, 14f, c.fg)
            if (sub != null) Txt(sub, 11f, c.fg2, maxLines = 2)
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(checked, onChange)
    }
}

@Composable
private fun Label(text: String) {
    Txt(text, 11f, LocalColors.current.fg2, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(c.track)
            .tap(onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Txt(text, 14f, c.fg) }
}

@Composable
fun FocusMenu(vm: LauncherViewModel, askNotifications: () -> Unit) {
    val c = LocalColors.current
    SwitchRow("Study mode", "Hides distracting apps everywhere", vm.studyMode) { vm.applyStudyMode(it) }
    Label("Pause before opening a distracting app")
    Segmented(listOf(0 to "Off", 5 to "5 s", 10 to "10 s"), vm.pauseSec, { vm.applyPauseSec(it) })
    Label("Reminder while you are in one")
    Segmented(
        listOf(0 to "Off", 5 to "5 min", 10 to "10 min", 15 to "15 min"), vm.reminderMin,
        { vm.applyReminderMin(it); if (it > 0) askNotifications() }
    )
    Txt(
        "You get a notification every few minutes until you come back here. No special permissions needed.",
        11f, c.fg2, maxLines = 4, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun WidgetsMenu(vm: LauncherViewModel) {
    val c = LocalColors.current
    Txt("${vm.widgets.size} on your home screen", 12f, c.fg2, modifier = Modifier.padding(bottom = 4.dp))
    MenuButton("Add a widget") { vm.menu = MenuType.None; vm.widgetTab = 0; vm.widgetCenter = true }
    MenuButton("Arrange and edit") { vm.menu = MenuType.None; vm.widgetTab = 1; vm.widgetCenter = true }
    Txt("Tip: long-press any widget on the home screen to edit it.", 11f, c.fg2, maxLines = 2, modifier = Modifier.padding(top = 10.dp))
}

@Composable
fun StyleMenu(vm: LauncherViewModel, pickPhoto: () -> Unit) {
    val c = LocalColors.current
    Label("Theme")
    Segmented(listOf(ThemeMode.Light to "Light", ThemeMode.Dark to "Dark", ThemeMode.System to "Auto"), vm.themeMode, { vm.applyTheme(it) })
    Label("Wallpaper")
    val previews = remember(c.dark) {
        Wallpapers.presets.map { it to Wallpapers.preset(it, c.dark, 96, 96).asImageBitmap() }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        previews.forEach { (id, bmp) ->
            val on = vm.wallpaper == id
            Image(
                bmp, id, Modifier.size(34.dp).clip(CircleShape)
                    .border(if (on) 2.dp else 0.8.dp, if (on) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                    .tap { vm.applyWallpaper(id) },
                contentScale = ContentScale.Crop
            )
        }
        val photo = vm.wallpaper == "photo"
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(c.track)
                .border(if (photo) 2.dp else 0.8.dp, if (photo) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                .tap { pickPhoto() },
            contentAlignment = Alignment.Center
        ) { Txt("+", 18f, c.fg) }
    }
    MenuButton("Adjust wallpaper") { vm.menu = MenuType.None; vm.wallpaperEditor = true }
    Label("Icons")
    Segmented(listOf(IconStyle.Mono to "Mono", IconStyle.Original to "Original"), vm.iconStyle, { vm.applyIconStyle(it) })
    Label("Icon shape")
    Segmented(listOf(IconShape.None to "None", IconShape.Circle to "Circle", IconShape.Squircle to "Squircle"), vm.iconShape, { vm.applyIconShape(it) })
    Label("Home layout")
    Segmented(listOf(HomeLayout.List to "List", HomeLayout.Grid to "Grid"), vm.layout, { vm.applyLayout(it) })
    Label("Alphabet")
    Segmented(listOf(AlphaSide.Right to "Right", AlphaSide.Left to "Left", AlphaSide.Off to "Off"), vm.alphaSide, { vm.applyAlphaSide(it) })
    Label("Dock")
    Segmented(listOf(DockStyle.Bottom to "Bottom", DockStyle.Rail to "Side rail", DockStyle.Off to "Off"), vm.dockStyle, { vm.applyDockStyle(it) })
    SwitchRow("App names", null, vm.showNames) { vm.applyShowNames(it) }
    Box(Modifier.fillMaxWidth().tap { vm.menu = MenuType.None; vm.aboutOpen = true }.padding(top = 6.dp, bottom = 2.dp)) {
        Txt("About  -  Developed by thats.jainam", 12f, c.fg2)
    }
}
