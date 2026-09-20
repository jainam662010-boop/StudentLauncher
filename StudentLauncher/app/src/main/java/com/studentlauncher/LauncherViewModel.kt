package com.studentlauncher

import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studentlauncher.data.*
import com.studentlauncher.ui.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val repo = AppRepository(ctx)
    private val prefs = ctx.getSharedPreferences("student_launcher", Context.MODE_PRIVATE)
    val hostMgr = WidgetHostManager(ctx)

    // ---- data ----
    var apps by mutableStateOf<List<AppInfo>>(emptyList()); private set
    private var byPkg by mutableStateOf<Map<String, AppInfo>>(emptyMap())
    val icons = mutableStateMapOf<String, AppIcons>()

    // ---- settings (persisted) ----
    var themeMode by mutableStateOf(enumPref("theme", ThemeMode.Light)); private set
    var wallpaper by mutableStateOf(prefs.getString("wallpaper", "dune") ?: "dune"); private set
    var wallpaperVersion by mutableStateOf(0); private set
    var wpBlur by mutableStateOf(prefs.getFloat("wpBlur", 0f)); private set
    var wpDim by mutableStateOf(prefs.getFloat("wpDim", 0f)); private set
    var wpZoom by mutableStateOf(prefs.getFloat("wpZoom", 1f)); private set
    var wpPanX by mutableStateOf(prefs.getFloat("wpPanX", 0f)); private set
    var wpPanY by mutableStateOf(prefs.getFloat("wpPanY", 0f)); private set
    var iconStyle by mutableStateOf(enumPref("iconStyle", IconStyle.Mono)); private set
    var iconShape by mutableStateOf(enumPref("iconShape", IconShape.Squircle)); private set
    var layout by mutableStateOf(enumPref("layout", HomeLayout.List)); private set
    var alphaSide by mutableStateOf(enumPref("alphaSide", AlphaSide.Right)); private set
    var dockStyle by mutableStateOf(enumPref("dockStyle", DockStyle.Bottom)); private set
    var showNames by mutableStateOf(prefs.getBoolean("showNames", true)); private set
    var studyMode by mutableStateOf(prefs.getBoolean("studyMode", false)); private set
    var pauseSec by mutableStateOf(prefs.getInt("pauseSec", 5)); private set
    var reminderMin by mutableStateOf(prefs.getInt("reminderMin", 10)); private set

    val pinned = mutableStateListOf<String>().apply { addAll(csv("pinned")) }
    val dock = mutableStateListOf<String>().apply { addAll(csv("dock")) }
    var flagged by mutableStateOf(csv("flagged").toSet()); private set
    var studyApps by mutableStateOf(csv("study").toSet()); private set
    val widgets = mutableStateListOf<WidgetItem>().apply {
        addAll(parseWidgets(prefs.getString("widgets_v2", null)) ?: defaultWidgets())
    }
    var pendingWidgetId = -1

    // ---- transient UI state ----
    var view by mutableStateOf(ViewMode.Home)
    var letter by mutableStateOf('A')
    var letterDir by mutableStateOf(1)
    var query by mutableStateOf("")
    var chip by mutableStateOf(Chip.All)
    var menu by mutableStateOf(MenuType.None)
    var ctxApp by mutableStateOf<AppInfo?>(null)
    var ctxRect by mutableStateOf(Rect.Zero)
    var pauseApp by mutableStateOf<AppInfo?>(null)
    var pauseRect by mutableStateOf(Rect.Zero)
    var toast by mutableStateOf<String?>(null)
    var widgetCenter by mutableStateOf(false)
    var widgetTab by mutableStateOf(0)
    var editWidgetId by mutableStateOf<String?>(null)
    var wallpaperEditor by mutableStateOf(false)
    var aboutOpen by mutableStateOf(false)

    init { refresh() }

    // ---- loading ----
    fun refresh() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = repo.loadApps()
            val installed = list.map { it.pkg }.toSet()
            withContext(Dispatchers.Main) {
                apps = list
                byPkg = list.associateBy { it.pkg }
                if (!prefs.contains("pinned")) { pinned.clear(); pinned.addAll(repo.defaultHome(installed)); saveCsv("pinned", pinned) }
                if (!prefs.contains("dock")) { dock.clear(); dock.addAll(repo.defaultDock(installed)); saveCsv("dock", dock) }
                if (!prefs.contains("flagged")) {
                    flagged = AppRepository.DISTRACTING_DEFAULTS.filter { it in installed }.toSet(); saveCsv("flagged", flagged)
                }
                if (!prefs.contains("study")) {
                    studyApps = AppRepository.STUDY_DEFAULTS.filter { it in installed }.toSet(); saveCsv("study", studyApps)
                }
                pinned.removeAll { it !in installed }
                dock.removeAll { it !in installed }
                icons.keys.filter { it !in installed }.forEach { icons.remove(it) }
            }
            val px = (ctx.resources.displayMetrics.density * 56f).toInt().coerceAtLeast(96)
            list.forEach { a ->
                if (icons[a.pkg] == null) repo.loadIcon(a, px)?.let { icons[a.pkg] = it }
            }
        }
    }

    fun appFor(pkg: String): AppInfo? = byPkg[pkg]
    fun isVisible(a: AppInfo) = !(studyMode && a.pkg in flagged)

    // ---- launcher panel data ----
    fun letterOf(label: String): Char {
        val c = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
        return if (c in 'A'..'Z') c else '#'
    }

    private fun pool(): List<AppInfo> = apps.filter {
        it.pkg !in pinned && isVisible(it) && (chip != Chip.Study || it.pkg in studyApps)
    }

    fun availableLetters(): Set<Char> = pool().map { letterOf(it.label) }.toSet()

    /** key is a letter ("A") or "q" for search results. */
    fun appsForKey(key: String): List<AppInfo> {
        val q = query.trim().lowercase()
        return if (key == "q") pool().filter { it.label.lowercase().contains(q) }
        else pool().filter { letterOf(it.label) == key[0] }
    }

    // ---- navigation ----
    fun onScrub(i: Int) {
        when (i) {
            0 -> goHome()
            27 -> Unit
            else -> {
                val l = 'A' + (i - 1)
                if (view != ViewMode.All || l != letter || query.isNotEmpty()) {
                    letterDir = if (l >= letter) 1 else -1
                    letter = l
                    query = ""
                    view = ViewMode.All
                }
            }
        }
    }

    fun onScrubRelease(i: Int) { if (i == 27) menu = MenuType.Style }

    fun goHome() { view = ViewMode.Home; query = ""; menu = MenuType.None; ctxApp = null }

    fun onHomePressed() {
        goHome()
        pauseApp = null
        editWidgetId = null
        widgetCenter = false
        aboutOpen = false
        wallpaperEditor = false
    }

    fun back() {
        when {
            wallpaperEditor -> wallpaperEditor = false
            aboutOpen -> aboutOpen = false
            editWidgetId != null -> editWidgetId = null
            widgetCenter -> widgetCenter = false
            ctxApp != null -> ctxApp = null
            pauseApp != null -> pauseApp = null
            menu != MenuType.None -> menu = MenuType.None
            view == ViewMode.All -> goHome()
        }
    }

    fun toggleMenu(m: MenuType) { menu = if (menu == m) MenuType.None else m }

    // ---- app actions ----
    fun togglePin(pkg: String) {
        if (pkg in pinned) pinned.remove(pkg)
        else if (pinned.size >= 8) toast = "Home holds 8 apps"
        else pinned.add(pkg)
        saveCsv("pinned", pinned)
    }

    fun toggleDock(pkg: String) {
        if (pkg in dock) dock.remove(pkg)
        else if (dock.size >= 5) toast = "Dock holds 5 apps"
        else dock.add(pkg)
        saveCsv("dock", dock)
    }

    fun toggleFlag(pkg: String) {
        flagged = if (pkg in flagged) flagged - pkg else flagged + pkg
        saveCsv("flagged", flagged)
        toast = if (pkg in flagged) "Marked as distracting" else "Removed from distracting"
    }

    fun toggleStudyApp(pkg: String) {
        studyApps = if (pkg in studyApps) studyApps - pkg else studyApps + pkg
        saveCsv("study", studyApps)
        toast = if (pkg in studyApps) "Marked as study app" else "Removed from study apps"
    }

    fun startApp(pkg: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (pkg in flagged && reminderMin > 0) {
            FocusReminders.start(ctx, pkg, appFor(pkg)?.label ?: "this app", reminderMin)
        }
        val o = ActivityOptions.makeCustomAnimation(ctx, R.anim.app_in, R.anim.hold)
        ctx.startActivity(i, o.toBundle())
    }

    fun openAppInfo(pkg: String) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    // ---- widgets ----
    private fun saveWidgets() = putS("widgets_v2", widgets.toList().toJson())

    fun addWidget(type: String) {
        widgets.add(newWidget(type))
        saveWidgets()
        toast = "${WT.title(type)} added"
    }

    fun commitAndroidWidget(appId: Int) {
        widgets.add(WidgetItem(UUID.randomUUID().toString(), WT.ANDROID, cfg = mapOf("appWidgetId" to appId.toString())))
        saveWidgets()
        pendingWidgetId = -1
        widgetCenter = false
    }

    fun cancelPendingWidget() {
        if (pendingWidgetId >= 0) hostMgr.release(pendingWidgetId)
        pendingWidgetId = -1
    }

    fun removeWidget(id: String) {
        val w = widgets.firstOrNull { it.id == id } ?: return
        if (w.type == WT.ANDROID) w.get("appWidgetId").toIntOrNull()?.let { hostMgr.release(it) }
        widgets.remove(w)
        if (editWidgetId == id) editWidgetId = null
        saveWidgets()
    }

    fun moveWidget(id: String, delta: Int) {
        val i = widgets.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in widgets.indices) return
        val w = widgets.removeAt(i)
        widgets.add(j, w)
        saveWidgets()
    }

    fun updateWidget(id: String, f: (WidgetItem) -> WidgetItem) {
        val i = widgets.indexOfFirst { it.id == id }
        if (i >= 0) {
            widgets[i] = f(widgets[i])
            saveWidgets()
        }
    }

    fun setCfg(id: String, key: String, value: String) = updateWidget(id) { it.put(key, value) }

    // ---- wallpaper ----
    fun saveWallpaperAdjust(blur: Float, dim: Float, zoom: Float, panX: Float, panY: Float) {
        wpBlur = blur; wpDim = dim; wpZoom = zoom; wpPanX = panX; wpPanY = panY
        prefs.edit().putFloat("wpBlur", blur).putFloat("wpDim", dim).putFloat("wpZoom", zoom)
            .putFloat("wpPanX", panX).putFloat("wpPanY", panY).apply()
        wallpaperVersion++
    }

    fun setPhotoWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = Wallpapers.savePhoto(ctx, uri)
            withContext(Dispatchers.Main) {
                if (ok) {
                    wallpaperVersion++
                    applyWallpaper("photo")
                    wpZoom = 1f; wpPanX = 0f; wpPanY = 0f
                    wallpaperEditor = true
                } else toast = "Couldn't load that photo"
            }
        }
    }

    // ---- settings setters ----
    fun applyTheme(v: ThemeMode) { themeMode = v; putS("theme", v.name) }
    fun applyWallpaper(id: String) { wallpaper = id; putS("wallpaper", id) }
    fun applyIconStyle(v: IconStyle) { iconStyle = v; putS("iconStyle", v.name) }
    fun applyIconShape(v: IconShape) { iconShape = v; putS("iconShape", v.name) }
    fun applyLayout(v: HomeLayout) { layout = v; putS("layout", v.name) }
    fun applyAlphaSide(v: AlphaSide) { alphaSide = v; putS("alphaSide", v.name) }
    fun applyDockStyle(v: DockStyle) { dockStyle = v; putS("dockStyle", v.name) }
    fun applyShowNames(v: Boolean) { showNames = v; putB("showNames", v) }
    fun applyStudyMode(v: Boolean) { studyMode = v; putB("studyMode", v) }
    fun applyPauseSec(v: Int) { pauseSec = v; prefs.edit().putInt("pauseSec", v).apply() }
    fun applyReminderMin(v: Int) {
        reminderMin = v
        prefs.edit().putInt("reminderMin", v).apply()
    }

    // ---- prefs helpers ----
    private inline fun <reified E : Enum<E>> enumPref(key: String, def: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, def.name) ?: def.name) }.getOrDefault(def)

    private fun putS(k: String, v: String) = prefs.edit().putString(k, v).apply()
    private fun putB(k: String, v: Boolean) = prefs.edit().putBoolean(k, v).apply()
    private fun csv(k: String): List<String> =
        (prefs.getString(k, "") ?: "").split(",").filter { it.isNotBlank() }
    private fun saveCsv(k: String, l: Collection<String>) =
        prefs.edit().putString(k, l.joinToString(",")).apply()
}
