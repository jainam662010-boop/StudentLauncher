package com.studentlauncher.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(val pkg: String, val label: String, val component: ComponentName)

/** [mono] is the themed (monochrome) layer when the app ships one (Android 13+). */
class AppIcons(val original: ImageBitmap, val mono: ImageBitmap?)

enum class ThemeMode { Light, Dark, System }
enum class IconShape { None, Circle, Squircle }
enum class IconStyle { Mono, Original }
enum class HomeLayout { List, Grid }
enum class AlphaSide { Right, Left, Off }
enum class DockStyle { Bottom, Rail, Off }
enum class ViewMode { Home, All }
enum class MenuType { None, Focus, Widgets, Style }
enum class Chip { All, Study }
