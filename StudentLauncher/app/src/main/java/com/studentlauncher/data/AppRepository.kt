package com.studentlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.graphics.asImageBitmap

class AppRepository(private val ctx: Context) {
    private val pm: PackageManager = ctx.packageManager

    fun loadApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map {
                AppInfo(
                    it.activityInfo.packageName,
                    it.loadLabel(pm).toString(),
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                )
            }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    fun loadIcon(app: AppInfo, px: Int): AppIcons? {
        val d = runCatching { pm.getActivityIcon(app.component) }.getOrNull() ?: return null
        val original = d.render(px, 1f)
        var mono: Bitmap? = null
        if (Build.VERSION.SDK_INT >= 33 && d is AdaptiveIconDrawable) {
            val m = d.monochrome
            if (m != null) mono = m.render(px, 1.5f)
        }
        return AppIcons(original.asImageBitmap(), mono?.asImageBitmap())
    }

    /** Adaptive icon layers are 108dp with a 72dp safe zone, so the mono layer is drawn 1.5x. */
    private fun Drawable.render(size: Int, scale: Float): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pad = ((scale - 1f) * size / 2f).toInt()
        setBounds(-pad, -pad, size + pad, size + pad)
        draw(Canvas(bmp))
        return bmp
    }

    private fun resolve(intent: Intent): String? =
        pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName

    fun defaultDock(installed: Set<String>): List<String> = listOfNotNull(
        resolve(Intent(Intent.ACTION_DIAL)),
        resolve(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
        resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    ).filter { it in installed }.distinct()

    fun defaultHome(installed: Set<String>): List<String> {
        val study = STUDY_DEFAULTS.filter { it in installed }
        val more = listOfNotNull(
            resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
            resolve(Intent(Settings.ACTION_SETTINGS))
        ).filter { it in installed }
        return (study + more).distinct().take(6)
    }

    companion object {
        val STUDY_DEFAULTS = listOf(
            "com.google.android.keep", "com.microsoft.office.onenote", "org.khanacademy.android",
            "com.google.android.calculator", "com.notion.id", "com.duolingo",
            "com.google.android.apps.docs", "com.google.android.apps.classroom"
        )
        val DISTRACTING_DEFAULTS = listOf(
            "com.instagram.android", "com.google.android.youtube", "com.facebook.katana",
            "com.snapchat.android", "com.twitter.android", "com.zhiliaoapp.musically",
            "com.reddit.frontpage", "com.discord", "com.netflix.mediaclient"
        )
    }
}
