package com.studentlauncher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/** Thin wrapper around AppWidgetHost so the launcher can show regular Android widgets. */
class WidgetHostManager(private val ctx: Context) {
    val host = AppWidgetHost(ctx, HOST_ID)
    val mgr: AppWidgetManager = AppWidgetManager.getInstance(ctx)

    fun start() { runCatching { host.startListening() } }
    fun stop() { runCatching { host.stopListening() } }
    fun release(id: Int) { runCatching { host.deleteAppWidgetId(id) } }
    fun info(id: Int): AppWidgetProviderInfo? = mgr.getAppWidgetInfo(id)

    fun providers(): List<AppWidgetProviderInfo> =
        mgr.installedProviders.sortedBy { it.loadLabel(ctx.packageManager).lowercase() }

    companion object { const val HOST_ID = 1024 }
}
